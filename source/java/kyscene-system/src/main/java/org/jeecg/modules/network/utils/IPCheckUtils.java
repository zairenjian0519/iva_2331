package org.jeecg.modules.network.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/6/14 10:46
 **/
public class IPCheckUtils {


    private static final String IPV4_PATTERN =
            "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";

    public static boolean isValidIPv4(String ip) {
        if (!ip.matches(IPV4_PATTERN)) {
            return false;
        }

        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.getHostAddress().equals(ip) && ip.contains(".");
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isValidSubnetMask(String subnet) {
        if (!isValidIPv4(subnet)) {
            return false;
        }

        String[] parts = subnet.split("\\.");
        int mask = 0;
        for (String part : parts) {
            mask = (mask << 8) + Integer.parseInt(part);
        }

        boolean foundZero = false;
        for (int i = 0; i < 32; i++) {
            if ((mask & (1 << (31 - i))) == 0) {
                foundZero = true;
            } else if (foundZero) {
                return false;
            }
        }
        return true;
    }


}
