package org.jeecg.modules.network.service.Impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jeecg.modules.network.service.NetworkService;
import org.jeecg.modules.network.vo.NetworkInfoVo;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/5/17 14:43
 **/
@Service
@Slf4j
public class NetworkServiceImpl implements NetworkService {

    @Override
    public List<NetworkInfoVo> getNetworkInfoVo() {
        try {
            return getNetworkInterfaces();
        } catch (IOException | InterruptedException e) {
            log.error("获取网卡信息异常：{}", e.getMessage());
        }
        return null;
    }

    @Override
    public int updateNetworkInfo(NetworkInfoVo networkInfo) {
        int result = 1;
        if (Objects.equals(networkInfo.getIpMethod(), "manual")) {
            String subnetMask = "";
            try {
                subnetMask = subnetMaskToCIDR(networkInfo.getSubnetMask());
            } catch (Exception e) {
                log.error("转子网掩码CIDR失败");
            }
            String dns = "";
            if (CollectionUtils.isNotEmpty(networkInfo.getDns())) {
                dns = String.join(",", networkInfo.getDns());
            }
            result = setStaticIp(networkInfo.getName(), networkInfo.getIpAddress(), subnetMask, networkInfo.getGateway(), dns);
        } else {
            result = setDynamicIp(networkInfo.getName());
        }

        if (result == 1) return result;

        // 确保此连接是自动连接的（开机自动启用）
        String[] cmd = {"nmcli", "connection", "modify", networkInfo.getName(), "connection.autoconnect", "yes"};
        try {
            CommandResult commandResult = CmdUti.executeCmd2(cmd);
            if (commandResult.getExitCode() != 0) {
                log.error("设置自动连接失败：{}", commandResult.getOutput());
                result = 1;
                return result;
            }
            result = 0;
        }catch (Exception e) {
            log.error("设置自动连接失败, {}", e.getMessage());
            result = 1;
        }
        return result;
    }

    @Override
    public boolean checkIPAvailable(String ipAddress, String networkCard) {
        List<NetworkInfoVo> networkInfoVos = getNetworkInfoVo();
        Optional<NetworkInfoVo> networkInfoVoOptional = networkInfoVos.stream()
                .filter(networkInfoVo -> networkInfoVo.getIpAddress().equals(ipAddress))
                .findFirst();
        if (networkInfoVoOptional.isPresent()) {
            NetworkInfoVo networkInfoVo = networkInfoVoOptional.get();
            // 如果检测的是自身网卡的ip，那此ip是可用的
            if (networkInfoVo.getNetworkCard().equals(networkCard))
                return false;
        }
        return isIPAvailable(ipAddress);
    }

    @Override
    public boolean checkIpReachable(String ipAddress) {
        return isIPAvailable(ipAddress);
    }

    public List<NetworkInfoVo> getNetworkInterfaces() throws IOException, InterruptedException {
        List<NetworkInfoVo> interfaces = new ArrayList<>();
        // 使用nmcli命令获取所有网络连接
        String[] getDeviceCommand = {"nmcli", "-t", "-f", "DEVICE,TYPE,STATE,CONNECTION", "device"};
        String deviceInfo = executeCommand(getDeviceCommand);
        if (StringUtils.isNotBlank(deviceInfo)) {
            String[] deviceInfoLines = deviceInfo.split("\n");
            outer:
            for (String line : deviceInfoLines) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && Objects.equals(parts[1], "ethernet")) {
                    String device = parts[0];
                    String connection = "";
                    //获取到的信息长度大于3 也就是能获取到connection名称说明网口是连接状态其他需要获取他是关闭状态的话需要启动
                    if (parts.length > 3) {
                        connection = parts[3];
                    } else {
                        String[] getStateCommand = {"nmcli", "-g", "GENERAL.STATE", "device", "show", device};
                        String deviceStateInfo = executeCommand(getStateCommand);
                        if (StringUtils.isNotBlank(deviceStateInfo)) {
                            String[] deviceStateInfoLine = deviceStateInfo.split("\n");
                            for (String deviceStateLine : deviceStateInfoLine) {
                                if (deviceStateLine.contains("30")) {
                                    String[] openNetworkCard = {"nmcli", "device", "connect", device};
                                    executeCommand(openNetworkCard);
                                } else {
                                    continue outer;
                                }
                            }
                        }
                    }
                    // 获取IP配置信息
                    String[] getIpCommand = {"nmcli", "-t", "-f", "GENERAL.STATE,IP4.ADDRESS,IP4.GATEWAY,IP4.DNS,DHCP4", "device", "show", device};
                    String ipDeviceInfo = executeCommand(getIpCommand);
                    if (StringUtils.isNotBlank(ipDeviceInfo)) {
                        String[] ipDeviceLine = ipDeviceInfo.split("\n");
                        NetworkInfoVo networkInterface = new NetworkInfoVo();
                        networkInterface.setName(connection);
                        networkInterface.setNetworkCard(device);
                        List<String> dnsList = Lists.newArrayList();
                        for (String ipLine : ipDeviceLine) {
                            log.info("获取网口信息切割------{}", ipLine);
                            networkInterface.setIpMethod("manual");
                            String[] ipParts = ipLine.split(":");
                            if (ipParts.length > 1 && !Objects.equals("--", ipParts[1])) {
                                if (ipParts[0].startsWith("IP4.ADDRESS")) {
                                    networkInterface.setIpAddress(ipParts[1].split("/")[0]);
                                    //计算子网掩码
                                    networkInterface.setSubnetMask(calculateSubnetMask(ipParts[1].split("/")[1]));
                                } else if (ipParts[0].startsWith("IP4.GATEWAY")) {
                                    networkInterface.setGateway(ipParts[1]);
                                } else if (ipParts[0].startsWith("IP4.DNS")) {
                                    dnsList.add(ipParts[1]);
                                } else if (ipParts[0].startsWith("DHCP4")) {
                                    networkInterface.setIpMethod("auto");
                                }
                                log.info("网口{},信息为{}", device, networkInterface);
                            }
                        }
                        networkInterface.setDns(dnsList);
                        interfaces.add(networkInterface);
                    }
                }
            }
        }
        interfaces.sort((p1, p2) -> {
            if (p1.getNetworkCard().contains("eth1")) {
                return -1;
            }
            if (p2.getNetworkCard().contains("eth1")) {
                return 1;
            }
            return 0;
        });
        return interfaces;
    }

    public int setDynamicIp(String connectionName) {
        try {
            Process process = new ProcessBuilder("nmcli", "connection", "modify", connectionName, "ipv4.method", "auto").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.error("设置动态IP出错 {}", line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.info("配置静态ip指令执行的exitCode: {}", exitCode);
                return 1;
            }
            boolean restarted = restartNetwork(connectionName);
            if (!restarted) return 1;
        } catch (IOException | InterruptedException e) {
            log.error("设置动态IP异常：{}", e.getMessage());
            return 1;
        }
        return 0;
    }

    public int setStaticIp(String connectionName, String ipAddress, String subnetMask, String gateway, String dns) {
        log.info("需要设置的网卡信息为 connectionName：{}, ipAddress: {}, subnetMask：{}, gateway：{}, dns：{}", connectionName, ipAddress, subnetMask, gateway, dns);
        try {
            String addresses = ipAddress + "/" + subnetMask;
            Process process = new ProcessBuilder("nmcli", "connection", "modify", connectionName, "ipv4.method", "manual", "ipv4.addresses", addresses, "ipv4.gateway", gateway, "ipv4.dns", dns).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.error("设置网口信息出错 {}", line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.info("配置静态ip指令执行的exitCode: {}", exitCode);
                return 1;
            }
            boolean restarted = restartNetwork(connectionName);
            if (!restarted) return 1;
        } catch (Exception e) {
            log.error("设置网口信息出错：{}", e.getMessage());
            return 1;
        }
        return 0;
    }

    public boolean restartNetwork(String connectionName) throws IOException, InterruptedException {
        //关闭网口
        String[] closeConnectionCommand = {"nmcli", "connection", "down", connectionName};
        String closeLine = executeCommand(closeConnectionCommand);
        if (StringUtils.isNotBlank(closeLine)) {
            if (closeLine.contains("错误") || closeLine.contains("error")) {
                log.error("关闭connection失败{}", closeLine);
                return false;
            }
        }
        log.info("重启网卡操作: connection down");
        //开启网口
        String[] openConnectionCommand = {"nmcli", "connection", "up", connectionName};
        String openLine = executeCommand(openConnectionCommand);
        if (StringUtils.isNotBlank(openLine)) {
            if (openLine.contains("错误") || openLine.contains("error")) {
                log.error("启动connection失败{}", openLine);
                return false;
            }
        }
        log.info("重启网卡操作：connection up");
        return true;
    }


    public String calculateSubnetMask(String cidr) {
        int prefixLength = Integer.parseInt(cidr);
        // 计算子网掩码
        int mask = 0xffffffff << (32 - prefixLength);
        // 将子网掩码转换为点分十进制格式
        return String.format("%d.%d.%d.%d",
                (mask >> 24 & 0xff),
                (mask >> 16 & 0xff),
                (mask >> 8 & 0xff),
                (mask & 0xff));
    }

    public String subnetMaskToCIDR(String subnetMask) {
        // 将子网掩码转换为二进制字符串
        String[] maskParts = subnetMask.split("\\.");
        StringBuilder binaryString = new StringBuilder();
        for (String part : maskParts) {
            int maskPart = Integer.parseInt(part);
            String binaryPart = Integer.toBinaryString(maskPart);
            while (binaryPart.length() < 8) {
                binaryPart = "0" + binaryPart;
            }
            binaryString.append(binaryPart);
        }
        // 计算前缀长度
        int prefixLength = binaryString.toString().replaceAll("0*$", "").length();
        log.info("子网掩码{},CIDR表示法{}", subnetMask, prefixLength);
        return String.valueOf(prefixLength);
    }


    public boolean isIPAvailable(String ipAddress) {
        try {
            Process process = new ProcessBuilder("ping", "-c", "1", ipAddress).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 如果 ping 命令成功，则说明 IP 可用
                return true;
            }
        } catch (IOException | InterruptedException e) {
            log.error("ping {} 异常，异常信息：{}", ipAddress, e.getMessage());
        }

        // 如果 ping 命令失败或者 IP 不可达，则说明 IP 不可用
        return false;
    }

    public String executeCommand(String[] command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        return output.toString().trim();
    }
}