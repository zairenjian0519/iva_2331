package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 存储清理工具类
 */
@Slf4j
public class StorageUtil {
    private static final long MB_UNIT = 1024L * 1024L;
    private static final String ALARM_ROOT_ENV = "KYSCENE_ALARM_ROOT";
    private static final String DEFAULT_ALARM_ROOT =
            "/userdata/sps_test/iva-kyscene/data/alarm_picture/download";

    /**
     * 获取磁盘总共可用存储空间
     */
    public static long getUsableSpace() {
        final String DATA_PATH = "/data";

        String deviceCategory = SysParameterUtil.getDeviceCategory();
        if ("N200".equalsIgnoreCase(deviceCategory)) {
            File file = new File(DATA_PATH);
            if (file.exists() && file.isDirectory()) {
                return file.getUsableSpace() / MB_UNIT;
            }
        }

        File[] roots = File.listRoots();
        long totalUsableSpace = 0L;

        if (roots != null) {
            for (File root : roots) {
                if (root != null && root.exists() && root.canRead()) {
                    long usableSpace = root.getUsableSpace();
                    if (usableSpace > 0) {
                        totalUsableSpace += usableSpace;
                    }
                }
            }
        }

        return totalUsableSpace / MB_UNIT;
    }

    /**
     * 获取指定目录所在文件系统的可用空间。
     */
    public static long getUsableSpace(String storagePath) {
        return getUsableSpaceBytes(storagePath) / MB_UNIT;
    }

    /**
     * 获取指定目录所在文件系统的可用字节数。
     */
    public static long getUsableSpaceBytes(String storagePath) {
        File storage = requireStorageDirectory(storagePath);
        return storage.getUsableSpace();
    }

    /**
     * 获取磁盘使用信息
     */
    public static JSONObject getDiskUsage() {
        String category = SysParameterUtil.getDeviceCategory();
        File fileToCheck = null;

        if ("N200".equalsIgnoreCase(category)) {
            File n200File = new File("/data");
            if (n200File.exists()) {
                fileToCheck = n200File;
            }
        }

        long totalSpace;
        long freeSpace;

        if (fileToCheck != null) {
            totalSpace = fileToCheck.getTotalSpace();
            freeSpace = fileToCheck.getFreeSpace();
        } else {
            File[] roots = File.listRoots();
            totalSpace = 0;
            freeSpace = 0;

            for (File root : roots) {
                totalSpace += root.getTotalSpace();
                freeSpace += root.getFreeSpace();
            }
        }

        long usedSpace = totalSpace - freeSpace;
        double usageRate = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
        // 保留两位小数
        usageRate = Math.round(usageRate * 100) / 100.0;

        JSONObject json = new JSONObject();
        json.put("diskTotal", totalSpace / MB_UNIT);
        json.put("diskUse", usedSpace / MB_UNIT);
        json.put("diskRate", usageRate);

        return json;
    }

    /**
     * 获取指定目录所在文件系统的容量信息。
     */
    public static JSONObject getDiskUsage(String storagePath) {
        File storage = requireStorageDirectory(storagePath);
        long totalSpace = storage.getTotalSpace();
        long freeSpace = storage.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double usageRate = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
        usageRate = Math.round(usageRate * 100) / 100.0;

        JSONObject json = new JSONObject();
        json.put("diskTotal", totalSpace / MB_UNIT);
        json.put("diskUse", usedSpace / MB_UNIT);
        json.put("diskRate", usageRate);
        return json;
    }

    private static File requireStorageDirectory(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            throw new IllegalArgumentException("存储目录不能为空");
        }
        File storage = new File(storagePath).getAbsoluteFile();
        if (!storage.exists() || !storage.isDirectory()) {
            throw new IllegalStateException("存储目录不存在或不是目录: " + storage.getPath());
        }
        return storage;
    }

    /**
     * 清理磁盘存储
     */
    public static void clearDiskStorage(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            log.info("路径列表为空，无需清理");
            return;
        }

        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                log.info("路径不存在，跳过清理: " + path);
                continue;
            }

            if (file.isDirectory()) {
                // 如果是目录，递归删除目录及其内容
                deleteDirectory(file);
            } else {
                // 如果是文件，直接删除
                boolean deleted = file.delete();
                if (!deleted) {
                    log.error("文件删除失败: " + path);
                }
            }
        }
    }

    /**
     * 递归删除目录及其内容
     *
     * @param directory 目录文件对象
     */
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // 递归删除子目录
                } else {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.error("文件删除失败: " + file.getAbsolutePath());
                    }
                }
            }
        }
        // 删除空目录
        boolean deleted = directory.delete();
        if (!deleted) {
            log.error("目录删除失败: " + directory.getAbsolutePath());
        }
    }

    /**
     * 获取创建时间超过指定时间的文件
     *
     * @param directoryPath 目标目录路径
     * @param timeThreshold 时间阈值（单位：毫秒，例如 7 天 = 7 * 24 * 60 * 60 * 1000）
     * @return 创建时间超过指定时间的文件列表
     */
    public static List<File> getFilesOlderThan(String directoryPath, long timeThreshold) {
        List<File> oldFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            log.info("指定路径不存在或不是目录: " + directoryPath);
            return oldFiles;
        }

        // 当前时间
        long currentTime = System.currentTimeMillis();

        // 遍历目录中的所有文件和子目录
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    long lastModifiedTime = file.lastModified(); // 文件最后修改时间
                    if (currentTime - lastModifiedTime > timeThreshold) {
                        oldFiles.add(file); // 添加到结果列表
                    }
                } else if (file.isDirectory()) {
                    // 如果是子目录，递归处理
                    oldFiles.addAll(getFilesOlderThan(file.getAbsolutePath(), timeThreshold));
                }
            }
        }

        return oldFiles;
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            if (!Files.exists(path)) {
                return false;
            }

            // 判断是否是符号链接
            if (Files.isSymbolicLink(path)) {
                Path targetPath = Files.readSymbolicLink(path);

                // 目标路径是相对路径时，要转成绝对路径
                Path realTargetPath = path.getParent().resolve(targetPath).normalize();

                if (Files.exists(realTargetPath)) {
                    Files.delete(realTargetPath);
                }
            }
            Files.delete(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 将数据库中的告警图片 URL 转换为当前安装目录下的安全文件路径。
     * 只允许 image 和 ori_image 两个历史图片目录，拒绝路径穿越和其它目录。
     */
    public static Path resolveAlarmImagePath(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return null;
        }
        Path root = alarmRoot();
        String value = storedPath.trim().replace('\\', '/');
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                value = new URI(value).getPath();
            }
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("告警图片路径无法解析: {}", storedPath);
            return null;
        }

        Path candidate;
        Path supplied = Paths.get(value).normalize();
        if (supplied.isAbsolute() && supplied.startsWith(root)) {
            candidate = supplied;
        } else {
            String relative = alarmRelativePath(value);
            if (relative == null) {
                return null;
            }
            candidate = root.resolve(relative).normalize();
        }
        Path relative = root.relativize(candidate);
        if (!isAlarmImageRelative(relative) || !candidate.startsWith(root)) {
            log.warn("拒绝删除告警目录之外的路径: {}", storedPath);
            return null;
        }
        return candidate;
    }

    /** 返回告警图片对应的原图路径。 */
    public static Path resolveAlarmOriginalImagePath(String storedPath) {
        Path image = resolveAlarmImagePath(storedPath);
        if (image == null) {
            return null;
        }
        Path root = alarmRoot();
        Path relative = root.relativize(image);
        if (relative.getNameCount() == 0) {
            return null;
        }
        if ("ori_image".equals(relative.getName(0).toString())) {
            return image;
        }
        if (!"image".equals(relative.getName(0).toString()) || relative.getNameCount() == 1) {
            return null;
        }
        return root.resolve("ori_image").resolve(relative.subpath(1, relative.getNameCount())).normalize();
    }

    /** 删除一条告警关联的缩略图和原图。 */
    public static void deleteAlarmImagePair(String storedPath) {
        Path image = resolveAlarmImagePath(storedPath);
        Path original = resolveAlarmOriginalImagePath(storedPath);
        deleteResolvedFile(image, storedPath);
        if (original != null && !original.equals(image)) {
            deleteResolvedFile(original, storedPath);
        }
    }

    /** 清空历史告警图片，保留 show_image 摄像头封面。 */
    public static int clearAlarmImages() throws IOException {
        Path root = alarmRoot();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("告警图片根目录不存在: " + root);
        }
        int deleted = clearDirectoryContents(root.resolve("image"));
        deleted += clearDirectoryContents(root.resolve("ori_image"));
        try (Stream<Path> children = Files.list(root)) {
            for (Path child : (Iterable<Path>) children::iterator) {
                if (Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)
                        && child.getFileName().toString().endsWith(".zip")) {
                    if (Files.deleteIfExists(child)) {
                        deleted++;
                    }
                }
            }
        }
        return deleted;
    }

    private static Path alarmRoot() {
        String configured = System.getenv(ALARM_ROOT_ENV);
        String root = configured == null || configured.trim().isEmpty()
                ? DEFAULT_ALARM_ROOT : configured.trim();
        return Paths.get(root).toAbsolutePath().normalize();
    }

    private static String alarmRelativePath(String value) {
        String normalized = value.replace('\\', '/');
        int downloadIndex = normalized.indexOf("/alarm_picture/download/");
        if (downloadIndex >= 0) {
            return normalized.substring(downloadIndex + "/alarm_picture/download/".length());
        }
        if (normalized.startsWith("/alarm_picture/")) {
            return normalized.substring("/alarm_picture/".length());
        }
        if (normalized.startsWith("/alarmImg/alarm_picture/")) {
            return normalized.substring("/alarmImg/alarm_picture/".length());
        }
        if (normalized.startsWith("/image/") || normalized.startsWith("/ori_image/")) {
            return normalized.substring(1);
        }
        return null;
    }

    private static boolean isAlarmImageRelative(Path relative) {
        if (relative == null || relative.getNameCount() < 2) {
            return false;
        }
        String first = relative.getName(0).toString();
        return "image".equals(first) || "ori_image".equals(first);
    }

    private static void deleteResolvedFile(Path path, String source) {
        if (path == null) {
            log.warn("告警图片路径无法映射到当前存储目录: {}", source);
            return;
        }
        try {
            if (Files.deleteIfExists(path)) {
                log.debug("删除告警图片: {}", path);
            }
        } catch (IOException e) {
            log.error("删除告警图片失败: {}", path, e);
        }
    }

    private static int clearDirectoryContents(Path directory) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }
        int deleted = 0;
        try (Stream<Path> children = Files.list(directory)) {
            for (Path child : (Iterable<Path>) children::iterator) {
                deleted += deleteTree(child);
            }
        }
        return deleted;
    }

    private static int deleteTree(Path path) throws IOException {
        int deleted = 0;
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> children = Files.list(path)) {
                for (Path child : (Iterable<Path>) children::iterator) {
                    deleted += deleteTree(child);
                }
            }
        }
        if (Files.deleteIfExists(path)) {
            deleted++;
        }
        return deleted;
    }

    /**
     * 格式化文件大小
     */
    private static String formatSize(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeInUnits = size;

        while (sizeInUnits >= 1024 && unitIndex < units.length - 1) {
            sizeInUnits /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", sizeInUnits, units[unitIndex]);
    }

}
