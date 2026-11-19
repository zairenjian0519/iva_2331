package org.jeecg.modules.system.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.zlmediakit.StreamMediaConst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * 执行系统命令的工具
 */
@Slf4j
public class CmdUti {
    private static final Map<String, Process> ROCKCHIP_STREAM_PROCESSES = new ConcurrentHashMap<>();
    private static final Pattern RTSP_CREDENTIALS = Pattern.compile("(?i)(rtsp://)[^\\s@]+@");

    /**
     * 实时输出脚本执行结果
     */
    public static int execCmdWithRealTimeOutput(String[] command) {
        int exitCode = -1;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);

            // 合并标准错误和标准输出
            builder.redirectErrorStream(true);

            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("shell输出： {}", line);
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("执行命令：{}, 异常：{}", String.join(" ", command), e.getMessage());
        }
        return exitCode;
    }

    /**
     * 执行命令并返回控制台输出
     *
     * @param command 命令数组
     * @return 命令输出结果
     */
    public static String executeCommand(String[] command) throws IOException, InterruptedException {
        CommandResult result = runCommand(command);
        return result.getOutput().trim();
    }

    /**
     * 执行命令并返回退出码
     *
     * @param command 命令数组
     * @return 命令退出码
     */
    public static int executeCmd(String[] command) throws IOException, InterruptedException {
        CommandResult result = runCommand(command);
        return result.getExitCode();
    }

    /**
     * 执行命令并返回命令结果
     *
     * @param command 命令数组
     * @return 命令结果
     * @throws IOException
     * @throws InterruptedException
     */
    public static CommandResult executeCmd2(String[] command) throws IOException, InterruptedException {
        return runCommand(command);
    }


    public static void startStream(String streamUrl, String mediaUrl, String streamFormat) {
        String command = "sudo -E LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/lynxi/lynffmpeg/lib " +
                "/opt/lynxi/lynffmpeg/bin/ffmpeg -re -rtsp_transport tcp -an -c:v hevc_lyndec -vsync 0 " +
                "-i '" + streamUrl + "' " +
                "-c:v h264_lynenc -bitrate 4096000 -f rtsp -rtsp_transport tcp " + mediaUrl;
        if (StreamMediaConst.STREAM_FORMAT_H264.equalsIgnoreCase(streamFormat)) {
            command = "sudo -E LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/lynxi/lynffmpeg/lib " +
                    "/opt/lynxi/lynffmpeg/bin/ffmpeg -re -rtsp_transport tcp -an -c:v h264_lyndec -vsync 0 " +
                    "-i '" + streamUrl + "' " +
                    "-c:v h264_lynenc -bitrate 4096000 -f rtsp -rtsp_transport tcp " + mediaUrl;
        } else if (StreamMediaConst.STREAM_FORMAT_MJPEG.equalsIgnoreCase(streamFormat)) {
            command = "sudo -E LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/lynxi/lynffmpeg/lib " +
                    "/opt/lynxi/lynffmpeg/bin/ffmpeg -re -rtsp_transport tcp -an -c:v mjpeg_lyndec -vsync 0 " +
                    "-i '" + streamUrl + "' " +
                    "-c:v h264_lynenc -bitrate 4096000 -f rtsp -rtsp_transport tcp " + mediaUrl;
        }
        execPushStream(command);
    }

    public static void startRockchipStream(String streamId, String sourceUrl, String targetUrl, String streamFormat) {
        String decoder = getRockchipDecoder(streamFormat);
        List<String> command = Arrays.asList(
                "/usr/bin/ffmpeg",
                "-nostdin",
                "-hide_banner",
                "-loglevel", "warning",
                "-nostats",
                "-fflags", "+genpts",
                "-rtsp_transport", "tcp",
                "-c:v", decoder,
                "-i", sourceUrl,
                "-an",
                "-c:v", "h264_rkmpp",
                "-profile:v", "baseline",
                "-level", "3.1",
                "-g", "50",
                "-keyint_min", "50",
                "-bf", "0",
                "-flags", "+low_delay",
                "-b:v", "4M",
                "-maxrate", "4M",
                "-bufsize", "8M",
                "-f", "flv",
                targetUrl
        );

        Process process = null;
        try {
            log.info("启动 RKMPP 转码，streamId: {}, decoder: {}, encoder: h264_rkmpp, source: {}, target: {}",
                    streamId, decoder, maskStreamUrl(sourceUrl), targetUrl);
            synchronized (ROCKCHIP_STREAM_PROCESSES) {
                Process current = ROCKCHIP_STREAM_PROCESSES.get(streamId);
                if (current != null && current.isAlive()) {
                    log.info("RKMPP 转码进程已经运行，streamId: {}", streamId);
                    return;
                }
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                process = builder.start();
                ROCKCHIP_STREAM_PROCESSES.put(streamId, process);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[RKMPP-FFmpeg][{}] {}", streamId, maskStreamUrl(line));
                }
            }

            int exitCode = process.waitFor();
            log.warn("RKMPP FFmpeg 退出，streamId: {}, 状态码: {}", streamId, exitCode);
        } catch (IOException e) {
            log.error("RKMPP FFmpeg 启动失败，streamId: {}, 原因: {}", streamId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("RKMPP FFmpeg 线程被中断，streamId: {}", streamId);
        } finally {
            if (process != null) {
                ROCKCHIP_STREAM_PROCESSES.remove(streamId, process);
            }
        }
    }

    public static void stopRockchipStream(String streamId) {
        Process process = ROCKCHIP_STREAM_PROCESSES.remove(streamId);
        if (process == null || !process.isAlive()) return;

        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    public static void stopAllRockchipStreams() {
        for (String streamId : new ArrayList<>(ROCKCHIP_STREAM_PROCESSES.keySet())) {
            stopRockchipStream(streamId);
        }
    }

    public static String maskStreamUrl(String text) {
        if (text == null) return null;
        return RTSP_CREDENTIALS.matcher(text).replaceAll("$1***@");
    }

    private static String getRockchipDecoder(String streamFormat) {
        if (StreamMediaConst.STREAM_FORMAT_H264.equalsIgnoreCase(streamFormat)) return "h264_rkmpp";
        if (StreamMediaConst.STREAM_FORMAT_MJPEG.equalsIgnoreCase(streamFormat)) return "mjpeg_rkmpp";
        return "hevc_rkmpp";
    }

    public static void execPushStream(String command) {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 输出 FFmpeg 日志
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg] {}", maskStreamUrl(line));
            }

            int exitCode = process.waitFor();
            log.error("FFmpeg 退出，状态码：{}", exitCode);
        } catch (IOException | InterruptedException e) {
            log.error("[FFmpeg] 命令执行失败：{}", e.getMessage());
        }
    }

    /**
     * 执行bash脚本
     *
     * @param shellPath shell 路径
     * @return 0 表示成功，其他值表示失败
     */
    public static int execByShell(String shellPath) {
        String[] command = {"bash", shellPath};
        int exitCode = -1;
        try {
            exitCode = executeCmd(command);
        } catch (IOException | InterruptedException e) {
            log.error("执行脚本异常: {}", e.getMessage());
        }
        if (exitCode != 0) {
            log.error("执行脚本失败");
        } else {
            log.info("执行脚本成功");
        }
        return exitCode;
    }

    /**
     * 执行bash脚本，脚本后面可以跟多个参数
     * 返回执行脚本输出的结果
     */
    public static CommandResult execByShell(String shellPath, String... args) throws InterruptedException, ExecutionException, TimeoutException {
        // 构建命令：bash shellPath arg1 arg2 ...
        String[] command = new String[args.length + 2];
        command[0] = "bash";
        command[1] = shellPath;
        System.arraycopy(args, 0, command, 2, args.length);

        // 使用线程池执行命令
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CommandResult> future = executor.submit(() -> {
            CommandResult result = runCommand(command);
            if (result.getExitCode() != 0) {
                log.error("执行脚本失败，退出码: {}，脚本输出：{}", result.getExitCode(), result.getOutput());
            }
            return result;
        });

        try {
            // 设置超时时间，如 2 分钟
            return future.get(2, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("执行脚本超时：{}, {}", String.join(" ", command), e.getMessage());
            future.cancel(true); // 强制中断任务
            throw e;
        } finally {
            executor.shutdownNow(); // 关闭线程池
        }
    }

    /**
     * 重启AI服务
     *
     * @return 0 表示成功，其他值表示失败
     */
    public static int restartAiServer() {
        int i = execByShell("/home/super/web/ai_restart.sh");
        if (i == 0) {
            log.info("重启 AI 服务成功");
        } else {
            log.error("重启 AI 服务失败");
        }
        return i;
    }

    /**
     * 重启AI引擎
     *
     * @return 0 表示成功，其他值表示失败
     */
    public static int restartAiEngine() {
        int i = execByShell("/home/super/web/engine_restart.sh");
        if (i == 0) {
            log.info("重启 AI 引擎成功");
        } else {
            log.error("重启 AI 引擎失败");
        }
        return i;
    }

    /**
     * 重启java，启动时间长，使用独立线程执行脚本
     */
    public static void restartJava() {
        new Thread(() -> {
            log.info("正在重启 Java，请稍等...");
            execByShell("/home/super/web/startKyscene.sh");
        }).start();
    }

    /**
     * 封装执行命令逻辑
     *
     * @param command 命令数组
     * @return CommandResult 包含输出结果与退出码
     */
    private static CommandResult runCommand(String[] command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        return new CommandResult(output.toString(), exitCode);
    }

}
