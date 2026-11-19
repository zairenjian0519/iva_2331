#include <pybind11/numpy.h>
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

#include <atomic>
#include <algorithm>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <utility>
#include <vector>

extern "C" {
#include <libavcodec/bsf.h>
#include <libavformat/avformat.h>
#include <libavutil/error.h>
#include <libavutil/rational.h>
#include <mpp_buffer.h>
#include <mpp_frame.h>
#include <mpp_packet.h>
#include <rk_mpi.h>
#include <rk_venc_cfg.h>
#include <rk_venc_rc.h>
}

#include <im2d.hpp>

namespace py = pybind11;
using namespace std::chrono_literals;

namespace {

constexpr int kModelWidth = 640;
constexpr int kModelHeight = 640;

std::string ff_error(int error) {
  char buffer[AV_ERROR_MAX_STRING_SIZE] = {};
  av_strerror(error, buffer, sizeof(buffer));
  return buffer;
}

void require_mpp(MPP_RET result, const char* operation) {
  if (result != MPP_OK) {
    throw std::runtime_error(std::string(operation) + " failed with MPP error " +
                             std::to_string(result));
  }
}

bool rga_ok(IM_STATUS status) {
  return status == IM_STATUS_SUCCESS || status == IM_STATUS_NOERROR;
}

int round_even(double value) {
  int rounded = static_cast<int>(value + 0.5);
  rounded = std::max(2, rounded & ~1);
  return rounded;
}

struct CameraConfig {
  int camera_id = 0;
  std::string rtsp_url;
  std::string codec = "auto";
  bool enable_preview = false;
  int reconnect_delay_ms = 5000;
  int timeout_ms = 5000;
};

struct LetterboxInfo {
  float scale = 1.0f;
  int pad_left = 0;
  int pad_top = 0;
  int resized_width = 0;
  int resized_height = 0;
};

struct StreamStats {
  std::uint64_t packets = 0;
  std::uint64_t packet_bytes = 0;
  std::uint64_t decoded_frames = 0;
  std::uint64_t delivered_frames = 0;
  std::uint64_t consumed_frames = 0;
  std::uint64_t dropped_frames = 0;
  std::uint64_t errors = 0;
  std::uint64_t reconnects = 0;
  std::uint32_t frame_buffer_capacity = 2;
  std::uint32_t pending_frames = 0;
  std::uint32_t in_flight_frames = 0;
  std::int64_t last_published_pts_us = 0;
  std::int64_t last_consumed_pts_us = 0;
  std::string state = "stopped";
  std::string last_error;
};

struct FrameStorage {
  ~FrameStorage() {
    if (yuv_synced && yuv_buffer != nullptr) {
      mpp_buffer_sync_ro_end(yuv_buffer);
    }
    if (rgb_synced && rgb_buffer != nullptr) {
      mpp_buffer_sync_ro_end(rgb_buffer);
    }
    if (preview_synced && preview_buffer != nullptr) {
      mpp_buffer_sync_ro_end(preview_buffer);
    }
    if (yuv_buffer != nullptr) {
      mpp_buffer_put(yuv_buffer);
    }
    if (rgb_buffer != nullptr) {
      mpp_buffer_put(rgb_buffer);
    }
    if (preview_buffer != nullptr) {
      mpp_buffer_put(preview_buffer);
    }
  }

  MppBuffer yuv_buffer = nullptr;
  MppBuffer rgb_buffer = nullptr;
  MppBuffer preview_buffer = nullptr;
  bool yuv_synced = false;
  bool rgb_synced = false;
  bool preview_synced = false;
  int camera_id = 0;
  int width = 0;
  int height = 0;
  int hor_stride = 0;
  int ver_stride = 0;
  std::int64_t pts_us = 0;
  std::string pixel_format;
  LetterboxInfo letterbox;
};

// Two ownership slots with latest-frame semantics. The producer only replaces
// pending_latest_; it never mutates the frame currently held by the consumer.
class LatestFrameDoubleBuffer {
 public:
  static constexpr std::uint32_t kCapacity = 2;

  bool publish(std::shared_ptr<FrameStorage> frame) {
    const bool replaced = pending_latest_ != nullptr;
    pending_latest_ = std::move(frame);
    return replaced;
  }

  std::shared_ptr<FrameStorage> consume_latest() {
    if (pending_latest_ == nullptr) {
      return nullptr;
    }
    in_flight_ = std::move(pending_latest_);
    pending_latest_.reset();
    return in_flight_;
  }

  bool has_pending() const { return pending_latest_ != nullptr; }
  std::uint32_t pending_count() const { return has_pending() ? 1U : 0U; }
  std::uint32_t in_flight_count() const { return in_flight_ != nullptr ? 1U : 0U; }

  void clear() {
    pending_latest_.reset();
    in_flight_.reset();
  }

 private:
  std::shared_ptr<FrameStorage> in_flight_;
  std::shared_ptr<FrameStorage> pending_latest_;
};

struct RgaRgbStorage {
  ~RgaRgbStorage() {
    if (synced && buffer != nullptr) {
      mpp_buffer_sync_ro_end(buffer);
    }
    if (buffer != nullptr) {
      mpp_buffer_put(buffer);
    }
    if (group != nullptr) {
      mpp_buffer_group_put(group);
    }
  }

  MppBufferGroup group = nullptr;
  MppBuffer buffer = nullptr;
  bool synced = false;
  int width = 0;
  int height = 0;
  LetterboxInfo letterbox;
};

class RgaImage {
 public:
  explicit RgaImage(std::shared_ptr<RgaRgbStorage> storage) : storage_(std::move(storage)) {}

  int width() const { return storage_->width; }
  int height() const { return storage_->height; }
  LetterboxInfo letterbox() const { return storage_->letterbox; }

  py::array rgb() const {
    auto* ptr = static_cast<std::uint8_t*>(mpp_buffer_get_ptr(storage_->buffer));
    if (ptr == nullptr) {
      throw std::runtime_error("RGA output buffer is not CPU mappable");
    }
    auto* owner = new std::shared_ptr<RgaRgbStorage>(storage_);
    py::capsule base(owner, [](void* value) {
      delete static_cast<std::shared_ptr<RgaRgbStorage>*>(value);
    });
    py::array array(py::dtype::of<std::uint8_t>(),
                    {storage_->height, storage_->width, 3},
                    {storage_->width * 3, 3, 1}, ptr, base);
    array.attr("setflags")(false);
    return array;
  }

 private:
  std::shared_ptr<RgaRgbStorage> storage_;
};

class MppJpegEncoder {
 public:
  ~MppJpegEncoder() { destroy(); }

  py::bytes encode(const std::shared_ptr<FrameStorage>& storage, int quality) {
    std::lock_guard<std::mutex> lock(mutex_);
    const auto format = storage->pixel_format == "NV12" ? MPP_FMT_YUV420SP :
                        storage->pixel_format == "NV21" ? MPP_FMT_YUV420SP_VU : MPP_FMT_BUTT;
    if (format == MPP_FMT_BUTT) {
      throw std::runtime_error("unsupported pixel format for MPP JPEG encode");
    }
    quality = std::max(1, std::min(99, quality));
    if (!matches(*storage, format, quality)) {
      initialize(*storage, format, quality);
    }

    MppFrame frame = nullptr;
    MppPacket packet = nullptr;
    auto cleanup = [&] {
      if (packet != nullptr) mpp_packet_deinit(&packet);
      if (frame != nullptr) mpp_frame_deinit(&frame);
    };
    try {
      require_mpp(mpp_frame_init(&frame), "mpp_frame_init(jpeg)");
      mpp_frame_set_width(frame, storage->width);
      mpp_frame_set_height(frame, storage->height);
      mpp_frame_set_hor_stride(frame, storage->hor_stride);
      mpp_frame_set_ver_stride(frame, storage->ver_stride);
      mpp_frame_set_fmt(frame, format);
      mpp_frame_set_buffer(frame, storage->yuv_buffer);
      require_mpp(api_->encode_put_frame(context_, frame), "encode_put_frame(jpeg)");
      for (int attempt = 0; attempt < 100; ++attempt) {
        const MPP_RET result = api_->encode_get_packet(context_, &packet);
        if (result == MPP_ERR_TIMEOUT) {
          std::this_thread::sleep_for(1ms);
          continue;
        }
        require_mpp(result, "encode_get_packet(jpeg)");
        if (packet != nullptr) break;
      }
      if (packet == nullptr) {
        throw std::runtime_error("MPP JPEG encode produced no packet");
      }
      const auto* data = static_cast<const char*>(mpp_packet_get_pos(packet));
      const auto size = mpp_packet_get_length(packet);
      if (data == nullptr || size == 0) {
        throw std::runtime_error("MPP JPEG packet is empty");
      }
      py::bytes output(data, size);
      cleanup();
      return output;
    } catch (...) {
      cleanup();
      throw;
    }
  }

 private:
  bool matches(const FrameStorage& storage, MppFrameFormat format, int quality) const {
    return context_ != nullptr && width_ == storage.width && height_ == storage.height &&
           hor_stride_ == storage.hor_stride && ver_stride_ == storage.ver_stride &&
           format_ == format && quality_ == quality;
  }

  void initialize(const FrameStorage& storage, MppFrameFormat format, int quality) {
    destroy();
    MppEncCfg config = nullptr;
    try {
      require_mpp(mpp_create(&context_, &api_), "mpp_create(jpeg)");
      require_mpp(mpp_init(context_, MPP_CTX_ENC, MPP_VIDEO_CodingMJPEG), "mpp_init(jpeg)");
      require_mpp(mpp_enc_cfg_init(&config), "mpp_enc_cfg_init(jpeg)");
      require_mpp(api_->control(context_, MPP_ENC_GET_CFG, config), "MPP_ENC_GET_CFG(jpeg)");
      require_mpp(mpp_enc_cfg_set_s32(config, "prep:width", storage.width), "jpeg prep width");
      require_mpp(mpp_enc_cfg_set_s32(config, "prep:height", storage.height), "jpeg prep height");
      require_mpp(mpp_enc_cfg_set_s32(config, "prep:hor_stride", storage.hor_stride), "jpeg prep hor stride");
      require_mpp(mpp_enc_cfg_set_s32(config, "prep:ver_stride", storage.ver_stride), "jpeg prep ver stride");
      require_mpp(mpp_enc_cfg_set_s32(config, "prep:format", format), "jpeg prep format");
      require_mpp(mpp_enc_cfg_set_s32(config, "codec:type", MPP_VIDEO_CodingMJPEG), "jpeg codec type");
      require_mpp(mpp_enc_cfg_set_s32(config, "rc:mode", MPP_ENC_RC_MODE_FIXQP), "jpeg rc mode");
      require_mpp(mpp_enc_cfg_set_s32(config, "jpeg:q_factor", quality), "jpeg quality");
      require_mpp(mpp_enc_cfg_set_s32(config, "jpeg:qf_min", 1), "jpeg minimum quality");
      require_mpp(mpp_enc_cfg_set_s32(config, "jpeg:qf_max", 99), "jpeg maximum quality");
      require_mpp(api_->control(context_, MPP_ENC_SET_CFG, config), "MPP_ENC_SET_CFG(jpeg)");
      mpp_enc_cfg_deinit(config);
      config = nullptr;
      width_ = storage.width;
      height_ = storage.height;
      hor_stride_ = storage.hor_stride;
      ver_stride_ = storage.ver_stride;
      format_ = format;
      quality_ = quality;
    } catch (...) {
      if (config != nullptr) mpp_enc_cfg_deinit(config);
      destroy();
      throw;
    }
  }

  void destroy() noexcept {
    if (context_ != nullptr) {
      mpp_destroy(context_);
      context_ = nullptr;
      api_ = nullptr;
    }
    width_ = height_ = hor_stride_ = ver_stride_ = quality_ = 0;
    format_ = MPP_FMT_BUTT;
  }

  std::mutex mutex_;
  MppCtx context_ = nullptr;
  MppApi* api_ = nullptr;
  int width_ = 0;
  int height_ = 0;
  int hor_stride_ = 0;
  int ver_stride_ = 0;
  int quality_ = 0;
  MppFrameFormat format_ = MPP_FMT_BUTT;
};

class Frame {
 public:
  explicit Frame(std::shared_ptr<FrameStorage> storage) : storage_(std::move(storage)) {}

  int camera_id() const { return storage_->camera_id; }
  int width() const { return storage_->width; }
  int height() const { return storage_->height; }
  int hor_stride() const { return storage_->hor_stride; }
  int ver_stride() const { return storage_->ver_stride; }
  std::int64_t pts_us() const { return storage_->pts_us; }
  const std::string& pixel_format() const { return storage_->pixel_format; }
  LetterboxInfo letterbox() const { return storage_->letterbox; }

  py::array yuv() const {
    const auto rows = storage_->ver_stride + storage_->ver_stride / 2;
    return make_array(storage_->yuv_buffer, {rows, storage_->hor_stride},
                      {storage_->hor_stride, 1}, 0);
  }

  py::tuple yuv_planes() const {
    const auto y = make_array(storage_->yuv_buffer, {storage_->height, storage_->width},
                              {storage_->hor_stride, 1}, 0);
    const auto uv_offset = static_cast<py::ssize_t>(storage_->ver_stride) * storage_->hor_stride;
    const auto uv = make_array(storage_->yuv_buffer, {storage_->height / 2, storage_->width},
                               {storage_->hor_stride, 1}, uv_offset);
    return py::make_tuple(y, uv);
  }

  py::array rgb() const {
    return make_array(storage_->rgb_buffer, {kModelHeight, kModelWidth, 3},
                      {kModelWidth * 3, 3, 1}, 0);
  }

  py::array preview_rgb() const {
    if (storage_->preview_buffer == nullptr) {
      throw std::runtime_error("original-resolution preview was not enabled for this camera");
    }
    return make_array(storage_->preview_buffer, {storage_->height, storage_->width, 3},
                      {storage_->width * 3, 3, 1}, 0);
  }

  RgaImage rga_rgb(int output_width, int output_height, int x, int y,
                   int roi_width, int roi_height, bool letterbox) const {
    if (output_width <= 0 || output_height <= 0 || roi_width <= 0 || roi_height <= 0 ||
        x < 0 || y < 0 || x + roi_width > storage_->width || y + roi_height > storage_->height) {
      throw std::invalid_argument("invalid RGA crop or output dimensions");
    }
    const int source_format = storage_->pixel_format == "NV12" ? RK_FORMAT_YCbCr_420_SP :
                              storage_->pixel_format == "NV21" ? RK_FORMAT_YCrCb_420_SP : 0;
    if (source_format == 0) {
      throw std::runtime_error("unsupported source pixel format for RGA crop");
    }
    const int source_fd = mpp_buffer_get_fd(storage_->yuv_buffer);
    if (source_fd < 0) {
      throw std::runtime_error("MPP source buffer has no DMA-BUF fd");
    }

    auto result = std::make_shared<RgaRgbStorage>();
    result->width = output_width;
    result->height = output_height;
    require_mpp(mpp_buffer_group_get_internal(&result->group, MPP_BUFFER_TYPE_ION),
                "mpp_buffer_group_get_internal(roi)");
    try {
      require_mpp(mpp_buffer_get(result->group, &result->buffer,
                                 static_cast<size_t>(output_width) * output_height * 3),
                  "mpp_buffer_get(roi)");
      const int destination_fd = mpp_buffer_get_fd(result->buffer);
      if (destination_fd < 0) {
        throw std::runtime_error("RGA crop destination has no DMA-BUF fd");
      }
      im_handle_param_t source_param{};
      source_param.width = storage_->width;
      source_param.height = storage_->height;
      source_param.format = source_format;
      im_handle_param_t destination_param{};
      destination_param.width = output_width;
      destination_param.height = output_height;
      destination_param.format = RK_FORMAT_RGB_888;
      const rga_buffer_handle_t source_handle = importbuffer_fd(source_fd, &source_param);
      const rga_buffer_handle_t destination_handle = importbuffer_fd(destination_fd, &destination_param);
      if (source_handle <= 0 || destination_handle <= 0) {
        if (source_handle > 0) releasebuffer_handle(source_handle);
        if (destination_handle > 0) releasebuffer_handle(destination_handle);
        throw std::runtime_error("RGA importbuffer_fd failed for crop transform");
      }
      auto release_handles = [&] {
        releasebuffer_handle(source_handle);
        releasebuffer_handle(destination_handle);
      };
      rga_buffer_t source = wrapbuffer_handle(source_handle, storage_->width, storage_->height,
                                               source_format, storage_->hor_stride, storage_->ver_stride);
      rga_buffer_t destination = wrapbuffer_handle(destination_handle, output_width, output_height,
                                                    RK_FORMAT_RGB_888, output_width, output_height);
      const float scale = letterbox ? std::min(static_cast<float>(output_width) / roi_width,
                                                static_cast<float>(output_height) / roi_height)
                                    : std::min(static_cast<float>(output_width) / roi_width,
                                               static_cast<float>(output_height) / roi_height);
      const int resized_width = letterbox ? std::min(output_width, round_even(roi_width * scale)) : output_width;
      const int resized_height = letterbox ? std::min(output_height, round_even(roi_height * scale)) : output_height;
      const int pad_left = letterbox ? (output_width - resized_width) / 2 : 0;
      const int pad_top = letterbox ? (output_height - resized_height) / 2 : 0;
      if (letterbox && (resized_width != output_width || resized_height != output_height)) {
        auto* ptr = static_cast<std::uint8_t*>(mpp_buffer_get_ptr(result->buffer));
        if (ptr == nullptr) {
          release_handles();
          throw std::runtime_error("RGA crop destination is not CPU mappable");
        }
        std::memset(ptr, 0, static_cast<size_t>(output_width) * output_height * 3);
      }
      const im_rect source_rect = {x, y, roi_width, roi_height};
      const im_rect destination_rect = {pad_left, pad_top, resized_width, resized_height};
      rga_buffer_t empty{};
      const im_rect empty_rect{};
      auto rga_error = [&](const char* stage, IM_STATUS status) {
        std::ostringstream message;
        message << stage << " failed: source=" << storage_->width << "x" << storage_->height
                << " stride=" << storage_->hor_stride << "x" << storage_->ver_stride
                << " crop=" << x << "," << y << "," << roi_width << "x" << roi_height
                << " destination=" << pad_left << "," << pad_top << ","
                << resized_width << "x" << resized_height
                << " output=" << output_width << "x" << output_height
                << " scale=" << scale << " status=" << static_cast<int>(status)
                << " error=" << imStrError_t(status);
        return message.str();
      };
      const IM_STATUS check_status = imcheck_t(source, destination, empty, source_rect,
                                               destination_rect, empty_rect, IM_SYNC);
      if (!rga_ok(check_status)) {
        const auto message = rga_error("RGA crop imcheck", check_status);
        release_handles();
        throw std::runtime_error(message);
      }
      const IM_STATUS status = improcess(source, destination, empty, source_rect,
                                         destination_rect, empty_rect, -1, nullptr, nullptr, IM_SYNC);
      if (!rga_ok(status)) {
        const auto message = rga_error("RGA crop transform", status);
        release_handles();
        throw std::runtime_error(message);
      }
      release_handles();
      result->letterbox = {scale, pad_left, pad_top, resized_width, resized_height};
      require_mpp(mpp_buffer_sync_ro_begin(result->buffer), "mpp_buffer_sync_ro_begin(roi)");
      result->synced = true;
      return RgaImage(std::move(result));
    } catch (...) {
      throw;
    }
  }

  py::bytes jpeg(int quality = 80) const {
    static MppJpegEncoder encoder;
    return encoder.encode(storage_, quality);
  }

 private:
  py::array make_array(MppBuffer buffer, std::vector<py::ssize_t> shape,
                       std::vector<py::ssize_t> strides, py::ssize_t offset) const {
    auto* ptr = static_cast<std::uint8_t*>(mpp_buffer_get_ptr(buffer));
    if (ptr == nullptr) {
      throw std::runtime_error("MPP buffer is not CPU mappable");
    }
    auto* owner = new std::shared_ptr<FrameStorage>(storage_);
    py::capsule base(owner, [](void* value) {
      delete static_cast<std::shared_ptr<FrameStorage>*>(value);
    });
    py::array array(py::dtype::of<std::uint8_t>(), std::move(shape), std::move(strides),
                    ptr + offset, base);
    array.attr("setflags")(false);
    return array;
  }

  std::shared_ptr<FrameStorage> storage_;
};

class MppDecoder {
 public:
  MppDecoder(int camera_id, bool enable_preview)
      : camera_id_(camera_id), enable_preview_(enable_preview) {
    require_mpp(mpp_create(&ctx_, &mpi_), "mpp_create");
  }

  void initialize(MppCodingType codec) {
    if (initialized_) {
      if (codec != initialized_codec_) {
        throw std::runtime_error("RTSP reconnect changed the video codec");
      }
      return;
    }
    try {
      require_mpp(mpp_init(ctx_, MPP_CTX_DEC, codec), "mpp_init");
      require_mpp(mpp_buffer_group_get_internal(&rgb_group_, MPP_BUFFER_TYPE_ION),
                  "mpp_buffer_group_get_internal(rgb)");
      initialized_codec_ = codec;
      initialized_ = true;
    } catch (...) {
      destroy();
      throw;
    }
  }

  ~MppDecoder() { destroy(); }

  MppDecoder(const MppDecoder&) = delete;
  MppDecoder& operator=(const MppDecoder&) = delete;

  void reset() {
    if (initialized_ && ctx_ != nullptr && mpi_ != nullptr) {
      mpi_->reset(ctx_);
    }
  }

  std::vector<std::shared_ptr<FrameStorage>> submit(const std::uint8_t* data, size_t size,
                                                     std::int64_t pts_us,
                                                     const std::atomic<bool>& stopping) {
    if (!initialized_) {
      throw std::logic_error("MPP decoder used before initialization");
    }
    MppPacket packet = nullptr;
    require_mpp(mpp_packet_init(&packet, const_cast<std::uint8_t*>(data), size), "mpp_packet_init");
    try {
      mpp_packet_set_length(packet, size);
      mpp_packet_set_pts(packet, pts_us);

      MPP_RET result = MPP_NOK;
      for (int attempt = 0; attempt < 30 && !stopping.load(); ++attempt) {
        result = mpi_->decode_put_packet(ctx_, packet);
        if (result == MPP_OK) {
          break;
        }
        (void)drain();
        std::this_thread::sleep_for(2ms);
      }
      mpp_packet_deinit(&packet);
      require_mpp(result, "decode_put_packet");
      return drain();
    } catch (...) {
      mpp_packet_deinit(&packet);
      throw;
    }
  }

  std::vector<std::shared_ptr<FrameStorage>> drain() {
    std::vector<std::shared_ptr<FrameStorage>> frames;
    while (true) {
      MppFrame frame = nullptr;
      const MPP_RET result = mpi_->decode_get_frame(ctx_, &frame);
      if (result == MPP_ERR_TIMEOUT || frame == nullptr) {
        break;
      }
      require_mpp(result, "decode_get_frame");
      try {
        if (mpp_frame_get_info_change(frame)) {
          configure_output_group(frame);
        } else if (!(mpp_frame_get_errinfo(frame) || mpp_frame_get_discard(frame))) {
          frames.push_back(make_frame(frame));
        }
        mpp_frame_deinit(&frame);
      } catch (...) {
        mpp_frame_deinit(&frame);
        throw;
      }
    }
    return frames;
  }

 private:
  void configure_output_group(MppFrame frame) {
    const auto buffer_size = mpp_frame_get_buf_size(frame);
    if (frame_group_ == nullptr) {
      require_mpp(mpp_buffer_group_get_internal(&frame_group_, MPP_BUFFER_TYPE_DRM),
                  "mpp_buffer_group_get_internal(frame)");
      require_mpp(mpi_->control(ctx_, MPP_DEC_SET_EXT_BUF_GROUP, frame_group_),
                  "MPP_DEC_SET_EXT_BUF_GROUP");
    } else {
      require_mpp(mpp_buffer_group_clear(frame_group_), "mpp_buffer_group_clear");
    }
    require_mpp(mpp_buffer_group_limit_config(frame_group_, buffer_size, 8),
                "mpp_buffer_group_limit_config");
    require_mpp(mpi_->control(ctx_, MPP_DEC_SET_INFO_CHANGE_READY, nullptr),
                "MPP_DEC_SET_INFO_CHANGE_READY");
  }

  std::shared_ptr<FrameStorage> make_frame(MppFrame frame) {
    const auto raw_format = static_cast<MppFrameFormat>(mpp_frame_get_fmt(frame) & MPP_FRAME_FMT_MASK);
    int rga_format = 0;
    std::string format;
    if (raw_format == MPP_FMT_YUV420SP) {
      rga_format = RK_FORMAT_YCbCr_420_SP;
      format = "NV12";
    } else if (raw_format == MPP_FMT_YUV420SP_VU) {
      rga_format = RK_FORMAT_YCrCb_420_SP;
      format = "NV21";
    } else {
      throw std::runtime_error("unsupported decoded MPP frame format " +
                               std::to_string(raw_format));
    }

    const int width = mpp_frame_get_width(frame);
    const int height = mpp_frame_get_height(frame);
    const int hor_stride = mpp_frame_get_hor_stride(frame);
    const int ver_stride = mpp_frame_get_ver_stride(frame);
    MppBuffer source = mpp_frame_get_buffer(frame);
    if (source == nullptr || mpp_buffer_get_fd(source) < 0) {
      throw std::runtime_error("MPP frame has no DMA-BUF fd");
    }

    auto storage = std::make_shared<FrameStorage>();
    require_mpp(mpp_buffer_inc_ref(source), "mpp_buffer_inc_ref(frame)");
    storage->yuv_buffer = source;
    storage->camera_id = camera_id_;
    storage->width = width;
    storage->height = height;
    storage->hor_stride = hor_stride;
    storage->ver_stride = ver_stride;
    storage->pts_us = mpp_frame_get_pts(frame);
    storage->pixel_format = format;

    try {
      require_mpp(mpp_buffer_get(rgb_group_, &storage->rgb_buffer,
                                 kModelWidth * kModelHeight * 3),
                  "mpp_buffer_get(rgb)");
      if (enable_preview_) {
        require_mpp(mpp_buffer_get(rgb_group_, &storage->preview_buffer,
                                   static_cast<size_t>(width) * height * 3),
                    "mpp_buffer_get(preview)");
      }
      convert_with_rga(*storage, rga_format);
      require_mpp(mpp_buffer_sync_ro_begin(storage->yuv_buffer), "mpp_buffer_sync_ro_begin(yuv)");
      storage->yuv_synced = true;
      require_mpp(mpp_buffer_sync_ro_begin(storage->rgb_buffer), "mpp_buffer_sync_ro_begin(rgb)");
      storage->rgb_synced = true;
      if (storage->preview_buffer != nullptr) {
        require_mpp(mpp_buffer_sync_ro_begin(storage->preview_buffer),
                    "mpp_buffer_sync_ro_begin(preview)");
        storage->preview_synced = true;
      }
      return storage;
    } catch (...) {
      throw;
    }
  }

  void convert_with_rga(FrameStorage& storage, int source_format) {
    const int source_fd = mpp_buffer_get_fd(storage.yuv_buffer);
    const int destination_fd = mpp_buffer_get_fd(storage.rgb_buffer);
    const int preview_fd = storage.preview_buffer == nullptr ? -1 : mpp_buffer_get_fd(storage.preview_buffer);
    if (destination_fd < 0) {
      throw std::runtime_error("RGA destination MPP buffer has no DMA-BUF fd");
    }
    if (storage.preview_buffer != nullptr && preview_fd < 0) {
      throw std::runtime_error("RGA preview MPP buffer has no DMA-BUF fd");
    }

    im_handle_param_t source_param{};
    source_param.width = storage.width;
    source_param.height = storage.height;
    source_param.format = source_format;
    im_handle_param_t destination_param{};
    destination_param.width = kModelWidth;
    destination_param.height = kModelHeight;
    destination_param.format = RK_FORMAT_RGB_888;
    im_handle_param_t preview_param{};
    preview_param.width = storage.width;
    preview_param.height = storage.height;
    preview_param.format = RK_FORMAT_RGB_888;

    const rga_buffer_handle_t source_handle = importbuffer_fd(source_fd, &source_param);
    const rga_buffer_handle_t destination_handle = importbuffer_fd(destination_fd, &destination_param);
    const rga_buffer_handle_t preview_handle =
        storage.preview_buffer == nullptr ? 0 : importbuffer_fd(preview_fd, &preview_param);
    if (source_handle <= 0 || destination_handle <= 0 ||
        (storage.preview_buffer != nullptr && preview_handle <= 0)) {
      if (source_handle > 0) {
        releasebuffer_handle(source_handle);
      }
      if (destination_handle > 0) {
        releasebuffer_handle(destination_handle);
      }
      if (preview_handle > 0) {
        releasebuffer_handle(preview_handle);
      }
      throw std::runtime_error("RGA importbuffer_fd failed for an MPP DMA-BUF");
    }
    auto release_handles = [&] {
      releasebuffer_handle(source_handle);
      releasebuffer_handle(destination_handle);
      if (preview_handle > 0) {
        releasebuffer_handle(preview_handle);
      }
    };
    rga_buffer_t source = wrapbuffer_handle(source_handle, storage.width, storage.height, source_format,
                                             storage.hor_stride, storage.ver_stride);
    rga_buffer_t destination = wrapbuffer_handle(destination_handle, kModelWidth, kModelHeight,
                                                  RK_FORMAT_RGB_888, kModelWidth, kModelHeight);
    rga_buffer_t preview{};
    if (preview_handle > 0) {
      preview = wrapbuffer_handle(preview_handle, storage.width, storage.height,
                                  RK_FORMAT_RGB_888, storage.width, storage.height);
    }
    const float scale = std::min(static_cast<float>(kModelWidth) / storage.width,
                                 static_cast<float>(kModelHeight) / storage.height);
    const int resized_width = std::min(kModelWidth, round_even(storage.width * scale));
    const int resized_height = std::min(kModelHeight, round_even(storage.height * scale));
    const int pad_left = (kModelWidth - resized_width) / 2;
    const int pad_top = (kModelHeight - resized_height) / 2;
    if (resized_width != kModelWidth || resized_height != kModelHeight) {
      // This RGA driver rejects imfill on RGB888 DMA-BUFs. Filling the newly
      // allocated destination mapping does not copy decoded pixels through CPU;
      // RGA still performs the YUV conversion and scaled write into the ROI.
      auto* destination_ptr = static_cast<std::uint8_t*>(mpp_buffer_get_ptr(storage.rgb_buffer));
      if (destination_ptr == nullptr) {
        release_handles();
        throw std::runtime_error("RGA destination MPP buffer is not CPU mappable");
      }
      std::memset(destination_ptr, 0, kModelWidth * kModelHeight * 3);
    }
    const im_rect source_rect = {0, 0, storage.width, storage.height};
    const im_rect destination_rect = {pad_left, pad_top, resized_width, resized_height};
    rga_buffer_t empty_buffer{};
    const im_rect empty_rect{};
    if (preview_handle > 0) {
      const im_rect preview_rect = {0, 0, storage.width, storage.height};
      const IM_STATUS preview_result = improcess(source, preview, empty_buffer, source_rect,
                                                  preview_rect, empty_rect, -1, nullptr, nullptr, IM_SYNC);
      if (!rga_ok(preview_result)) {
        release_handles();
        throw std::runtime_error("RGA original-resolution preview conversion failed with status " +
                                 std::to_string(preview_result));
      }
    }
    const IM_STATUS result = improcess(source, destination, empty_buffer, source_rect,
                                       destination_rect, empty_rect, -1, nullptr, nullptr, IM_SYNC);
    if (!rga_ok(result)) {
      release_handles();
      throw std::runtime_error("RGA improcess failed with status " + std::to_string(result));
    }
    release_handles();
    storage.letterbox = {scale, pad_left, pad_top, resized_width, resized_height};
  }

  void destroy() noexcept {
    if (ctx_ != nullptr && mpi_ != nullptr) {
      mpi_->reset(ctx_);
    }
    if (ctx_ != nullptr) {
      mpp_destroy(ctx_);
      ctx_ = nullptr;
      mpi_ = nullptr;
    }
    initialized_ = false;
    if (rgb_group_ != nullptr) {
      mpp_buffer_group_put(rgb_group_);
      rgb_group_ = nullptr;
    }
    if (frame_group_ != nullptr) {
      mpp_buffer_group_put(frame_group_);
      frame_group_ = nullptr;
    }
  }

  int camera_id_;
  bool enable_preview_ = false;
  MppCtx ctx_ = nullptr;
  MppApi* mpi_ = nullptr;
  MppBufferGroup rgb_group_ = nullptr;
  MppBufferGroup frame_group_ = nullptr;
  MppCodingType initialized_codec_{};
  bool initialized_ = false;
};

class StreamWorker : public std::enable_shared_from_this<StreamWorker> {
 public:
  explicit StreamWorker(CameraConfig config) : config_(std::move(config)) {
    if (config_.camera_id < 0 || config_.rtsp_url.empty()) {
      throw std::invalid_argument("camera_id must be non-negative and rtsp_url must not be empty");
    }
  }

  ~StreamWorker() { stop(); }

  void start() {
    if (thread_.joinable()) {
      return;
    }
    stopping_.store(false);
    set_state("initializing");
    thread_ = std::thread([this] { run(); });
  }

  void stop() {
    stopping_.store(true);
    condition_.notify_all();
    if (thread_.joinable()) {
      thread_.join();
    }
    {
      std::lock_guard<std::mutex> lock(mutex_);
      frames_.clear();
      stats_.pending_frames = 0;
      stats_.in_flight_frames = 0;
      stats_.state = "stopped";
    }
    decoder_.reset();
  }

  std::shared_ptr<FrameStorage> read_latest(int timeout_ms) {
    std::unique_lock<std::mutex> lock(mutex_);
    if (!condition_.wait_for(lock, std::chrono::milliseconds(timeout_ms), [this] {
          return frames_.has_pending() || stopping_.load() || stats_.state == "error";
        })) {
      return nullptr;
    }
    auto output = frames_.consume_latest();
    if (output != nullptr) {
      stats_.consumed_frames++;
      stats_.last_consumed_pts_us = output->pts_us;
    }
    stats_.pending_frames = frames_.pending_count();
    stats_.in_flight_frames = frames_.in_flight_count();
    return output;
  }

  StreamStats stats() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return stats_;
  }

 private:
  static int interrupt_callback(void* opaque) {
    return static_cast<StreamWorker*>(opaque)->stopping_.load() ? 1 : 0;
  }

  static MppCodingType select_codec(AVCodecID codec_id, const std::string& configured_codec) {
    if (configured_codec == "h264") {
      return MPP_VIDEO_CodingAVC;
    }
    if (configured_codec == "h265" || configured_codec == "hevc") {
      return MPP_VIDEO_CodingHEVC;
    }
    if (codec_id == AV_CODEC_ID_H264) {
      return MPP_VIDEO_CodingAVC;
    }
    if (codec_id == AV_CODEC_ID_HEVC) {
      return MPP_VIDEO_CodingHEVC;
    }
    throw std::runtime_error("only H.264 and H.265 RTSP streams are supported");
  }

  void run() {
    avformat_network_init();
    while (!stopping_.load()) {
      try {
        set_state("connecting");
        process_session();
      } catch (const std::exception& error) {
        std::lock_guard<std::mutex> lock(mutex_);
        stats_.errors++;
        stats_.last_error = error.what();
      }
      if (!stopping_.load()) {
        std::unique_lock<std::mutex> lock(mutex_);
        stats_.reconnects++;
        stats_.state = "backoff";
        condition_.wait_for(lock, std::chrono::milliseconds(config_.reconnect_delay_ms),
                            [this] { return stopping_.load(); });
      }
    }
  }

  void process_session() {
    AVFormatContext* format = avformat_alloc_context();
    if (format == nullptr) {
      throw std::runtime_error("avformat_alloc_context failed");
    }
    format->interrupt_callback = {interrupt_callback, this};
    AVDictionary* options = nullptr;
    av_dict_set(&options, "rtsp_transport", "tcp", 0);
    av_dict_set_int(&options, "rw_timeout", static_cast<int64_t>(config_.timeout_ms) * 1000, 0);
    int result = avformat_open_input(&format, config_.rtsp_url.c_str(), nullptr, &options);
    av_dict_free(&options);
    if (result < 0) {
      avformat_free_context(format);
      throw std::runtime_error("avformat_open_input: " + ff_error(result));
    }
    auto close_format = [&format] { avformat_close_input(&format); };

    try {
      result = avformat_find_stream_info(format, nullptr);
      if (result < 0) {
        throw std::runtime_error("avformat_find_stream_info: " + ff_error(result));
      }
      const int stream_index = av_find_best_stream(format, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
      if (stream_index < 0) {
        throw std::runtime_error("RTSP source contains no video stream");
      }
      AVStream* stream = format->streams[stream_index];
      const auto stream_codec = select_codec(stream->codecpar->codec_id, "auto");
      if (config_.codec != "auto") {
        const auto configured_codec = select_codec(AV_CODEC_ID_NONE, config_.codec);
        if (stream_codec != configured_codec) {
          throw std::runtime_error("RTSP session codec differs from the configured codec");
        }
      }
      if (decoder_ == nullptr || stream_codec != expected_codec_) {
        if (decoder_ != nullptr) {
          decoder_->reset();
          decoder_.reset();
        }
        auto decoder = std::make_unique<MppDecoder>(config_.camera_id, config_.enable_preview);
        decoder->initialize(stream_codec);
        decoder_ = std::move(decoder);
        expected_codec_ = stream_codec;
      }
      AVBSFContext* bsf = create_annex_b_filter(stream);
      {
        std::lock_guard<std::mutex> lock(mutex_);
        stats_.state = "running";
        stats_.last_error.clear();
      }
      AVPacket* packet = av_packet_alloc();
      if (packet == nullptr) {
        av_bsf_free(&bsf);
        throw std::runtime_error("av_packet_alloc failed");
      }
      while (!stopping_.load() && (result = av_read_frame(format, packet)) >= 0) {
        if (packet->stream_index == stream_index) {
          {
            std::lock_guard<std::mutex> lock(mutex_);
            stats_.packets++;
            stats_.packet_bytes += static_cast<std::uint64_t>(std::max(packet->size, 0));
          }
          send_packet_to_decoder(bsf, *decoder_, packet, stream->time_base);
        }
        av_packet_unref(packet);
      }
      av_packet_free(&packet);
      av_bsf_free(&bsf);
      decoder_->reset();
      if (!stopping_.load() && result < 0 && result != AVERROR_EOF) {
        throw std::runtime_error("av_read_frame: " + ff_error(result));
      }
    } catch (...) {
      if (decoder_ != nullptr) {
        decoder_->reset();
      }
      close_format();
      throw;
    }
    close_format();
  }

  AVBSFContext* create_annex_b_filter(AVStream* stream) {
    const char* name = stream->codecpar->codec_id == AV_CODEC_ID_H264 ? "h264_mp4toannexb" : "hevc_mp4toannexb";
    const AVBitStreamFilter* filter = av_bsf_get_by_name(name);
    if (filter == nullptr) {
      throw std::runtime_error(std::string("FFmpeg bitstream filter unavailable: ") + name);
    }
    AVBSFContext* bsf = nullptr;
    int result = av_bsf_alloc(filter, &bsf);
    if (result < 0) {
      throw std::runtime_error("av_bsf_alloc: " + ff_error(result));
    }
    result = avcodec_parameters_copy(bsf->par_in, stream->codecpar);
    if (result >= 0) {
      bsf->time_base_in = stream->time_base;
      result = av_bsf_init(bsf);
    }
    if (result < 0) {
      av_bsf_free(&bsf);
      throw std::runtime_error("av_bsf_init: " + ff_error(result));
    }
    return bsf;
  }

  void send_packet_to_decoder(AVBSFContext* bsf, MppDecoder& decoder, AVPacket* packet,
                              AVRational time_base) {
    int result = av_bsf_send_packet(bsf, packet);
    if (result < 0) {
      throw std::runtime_error("av_bsf_send_packet: " + ff_error(result));
    }
    AVPacket* filtered = av_packet_alloc();
    if (filtered == nullptr) {
      throw std::runtime_error("av_packet_alloc(filter) failed");
    }
    while ((result = av_bsf_receive_packet(bsf, filtered)) == 0) {
      const int64_t pts = filtered->pts == AV_NOPTS_VALUE
                              ? 0
                              : av_rescale_q(filtered->pts, time_base, AVRational{1, 1000000});
      auto frames = decoder.submit(filtered->data, filtered->size, pts, stopping_);
      for (auto& frame : frames) {
        publish(std::move(frame));
      }
      av_packet_unref(filtered);
    }
    av_packet_free(&filtered);
    if (result != AVERROR(EAGAIN) && result != AVERROR_EOF) {
      throw std::runtime_error("av_bsf_receive_packet: " + ff_error(result));
    }
  }

  void publish(std::shared_ptr<FrameStorage> frame) {
    std::lock_guard<std::mutex> lock(mutex_);
    stats_.decoded_frames++;
    stats_.last_published_pts_us = frame == nullptr ? 0 : frame->pts_us;
    if (frames_.publish(std::move(frame))) {
      stats_.dropped_frames++;
    }
    stats_.delivered_frames++;
    stats_.pending_frames = frames_.pending_count();
    stats_.in_flight_frames = frames_.in_flight_count();
    condition_.notify_one();
  }

  void set_state(std::string value) {
    std::lock_guard<std::mutex> lock(mutex_);
    stats_.state = std::move(value);
  }

  CameraConfig config_;
  std::atomic<bool> stopping_{false};
  std::thread thread_;
  mutable std::mutex mutex_;
  std::condition_variable condition_;
  LatestFrameDoubleBuffer frames_;
  std::unique_ptr<MppDecoder> decoder_;
  MppCodingType expected_codec_{};
  StreamStats stats_;
};

class MppRtspManager {
 public:
  explicit MppRtspManager(const std::vector<CameraConfig>& configs) {
    for (const auto& config : configs) {
      if (workers_.find(config.camera_id) != workers_.end()) {
        throw std::invalid_argument("duplicate camera_id " + std::to_string(config.camera_id));
      }
      workers_.emplace(config.camera_id, std::make_shared<StreamWorker>(config));
    }
  }

  ~MppRtspManager() { stop(); }

  void start() {
    for (const auto& [_, worker] : workers_) {
      worker->start();
    }
  }

  void stop() {
    for (const auto& [_, worker] : workers_) {
      worker->stop();
    }
  }

  py::object read_latest(int camera_id, int timeout_ms = 1000) {
    const auto iterator = workers_.find(camera_id);
    if (iterator == workers_.end()) {
      throw std::out_of_range("unknown camera_id " + std::to_string(camera_id));
    }
    std::shared_ptr<FrameStorage> storage;
    {
      py::gil_scoped_release release;
      storage = iterator->second->read_latest(timeout_ms);
    }
    if (storage == nullptr) {
      return py::none();
    }
    return py::cast(Frame(std::move(storage)));
  }

  StreamStats stream_stats(int camera_id) const {
    const auto iterator = workers_.find(camera_id);
    if (iterator == workers_.end()) {
      throw std::out_of_range("unknown camera_id " + std::to_string(camera_id));
    }
    return iterator->second->stats();
  }

  std::vector<int> camera_ids() const {
    std::vector<int> ids;
    ids.reserve(workers_.size());
    for (const auto& [camera_id, _] : workers_) {
      ids.push_back(camera_id);
    }
    return ids;
  }

 private:
  std::map<int, std::shared_ptr<StreamWorker>> workers_;
};

}  // namespace

PYBIND11_MODULE(rk_mpp_pipeline, module) {
  module.doc() = "MPP hardware decode and RGA-to-NumPy views for RK3588";
  module.def("mpp_probe", [] {
    MppCtx context = nullptr;
    MppApi* api = nullptr;
    require_mpp(mpp_create(&context, &api), "mpp_create");
    if (context == nullptr || api == nullptr) {
      throw std::runtime_error("mpp_create returned an empty context or API");
    }
    mpp_destroy(context);
    return true;
  });
  module.def("latest_frame_buffer_probe", [] {
    LatestFrameDoubleBuffer buffer;
    std::uint64_t dropped = 0;
    auto make = [](std::int64_t pts_us) {
      auto frame = std::make_shared<FrameStorage>();
      frame->pts_us = pts_us;
      return frame;
    };
    dropped += buffer.publish(make(1)) ? 1 : 0;
    dropped += buffer.publish(make(2)) ? 1 : 0;
    dropped += buffer.publish(make(3)) ? 1 : 0;
    const auto first = buffer.consume_latest();
    dropped += buffer.publish(make(4)) ? 1 : 0;
    const auto second = buffer.consume_latest();
    py::dict result;
    result["capacity"] = LatestFrameDoubleBuffer::kCapacity;
    result["dropped"] = dropped;
    result["first_consumed_pts_us"] = first == nullptr ? 0 : first->pts_us;
    result["second_consumed_pts_us"] = second == nullptr ? 0 : second->pts_us;
    result["pending_frames"] = buffer.pending_count();
    result["in_flight_frames"] = buffer.in_flight_count();
    return result;
  });

  py::class_<CameraConfig>(module, "CameraConfig")
      .def(py::init<>())
      .def_readwrite("camera_id", &CameraConfig::camera_id)
      .def_readwrite("rtsp_url", &CameraConfig::rtsp_url)
      .def_readwrite("codec", &CameraConfig::codec)
      .def_readwrite("enable_preview", &CameraConfig::enable_preview)
      .def_readwrite("reconnect_delay_ms", &CameraConfig::reconnect_delay_ms)
      .def_readwrite("timeout_ms", &CameraConfig::timeout_ms);

  py::class_<LetterboxInfo>(module, "LetterboxInfo")
      .def_readonly("scale", &LetterboxInfo::scale)
      .def_readonly("pad_left", &LetterboxInfo::pad_left)
      .def_readonly("pad_top", &LetterboxInfo::pad_top)
      .def_readonly("resized_width", &LetterboxInfo::resized_width)
      .def_readonly("resized_height", &LetterboxInfo::resized_height);

  py::class_<StreamStats>(module, "StreamStats")
      .def_readonly("packets", &StreamStats::packets)
      .def_readonly("packet_bytes", &StreamStats::packet_bytes)
      .def_readonly("decoded_frames", &StreamStats::decoded_frames)
      .def_readonly("delivered_frames", &StreamStats::delivered_frames)
      .def_readonly("consumed_frames", &StreamStats::consumed_frames)
      .def_readonly("dropped_frames", &StreamStats::dropped_frames)
      .def_readonly("errors", &StreamStats::errors)
      .def_readonly("reconnects", &StreamStats::reconnects)
      .def_readonly("frame_buffer_capacity", &StreamStats::frame_buffer_capacity)
      .def_readonly("pending_frames", &StreamStats::pending_frames)
      .def_readonly("in_flight_frames", &StreamStats::in_flight_frames)
      .def_readonly("last_published_pts_us", &StreamStats::last_published_pts_us)
      .def_readonly("last_consumed_pts_us", &StreamStats::last_consumed_pts_us)
      .def_readonly("state", &StreamStats::state)
      .def_readonly("last_error", &StreamStats::last_error);

  py::class_<Frame>(module, "Frame")
      .def_property_readonly("camera_id", &Frame::camera_id)
      .def_property_readonly("width", &Frame::width)
      .def_property_readonly("height", &Frame::height)
      .def_property_readonly("hor_stride", &Frame::hor_stride)
      .def_property_readonly("ver_stride", &Frame::ver_stride)
      .def_property_readonly("pts_us", &Frame::pts_us)
      .def_property_readonly("pixel_format", &Frame::pixel_format)
      .def_property_readonly("letterbox", &Frame::letterbox)
      .def_property_readonly("yuv", &Frame::yuv)
      .def("yuv_planes", &Frame::yuv_planes)
      .def_property_readonly("rgb", &Frame::rgb)
      .def_property_readonly("preview_rgb", &Frame::preview_rgb)
      .def("rga_rgb", [](const Frame& frame, int output_width, int output_height,
                          int x, int y, int roi_width, int roi_height, bool letterbox) {
            if (roi_width < 0) roi_width = frame.width() - x;
            if (roi_height < 0) roi_height = frame.height() - y;
            return frame.rga_rgb(output_width, output_height, x, y, roi_width, roi_height, letterbox);
          }, py::arg("output_width"), py::arg("output_height"), py::arg("x") = 0,
             py::arg("y") = 0, py::arg("roi_width") = -1, py::arg("roi_height") = -1,
             py::arg("letterbox") = true)
      .def("jpeg", &Frame::jpeg, py::arg("quality") = 80);

  py::class_<RgaImage>(module, "RgaImage")
      .def_property_readonly("width", &RgaImage::width)
      .def_property_readonly("height", &RgaImage::height)
      .def_property_readonly("letterbox", &RgaImage::letterbox)
      .def_property_readonly("rgb", &RgaImage::rgb);

  py::class_<MppRtspManager>(module, "MppRtspManager")
      .def(py::init<const std::vector<CameraConfig>&>())
      .def("start", &MppRtspManager::start)
      .def("stop", &MppRtspManager::stop)
      .def("read_latest", &MppRtspManager::read_latest, py::arg("camera_id"),
           py::arg("timeout_ms") = 1000)
      .def("stream_stats", &MppRtspManager::stream_stats)
      .def_property_readonly("camera_ids", &MppRtspManager::camera_ids);
}
