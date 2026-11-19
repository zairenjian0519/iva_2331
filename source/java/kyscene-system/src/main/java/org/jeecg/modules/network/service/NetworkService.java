package org.jeecg.modules.network.service;

import org.jeecg.modules.network.vo.NetworkInfoVo;

import java.util.List;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/5/17 14:42
 **/
public interface NetworkService {

    List<NetworkInfoVo> getNetworkInfoVo();

    int updateNetworkInfo(NetworkInfoVo networkInfo);

    boolean checkIPAvailable(String ip, String networkCard);

    boolean checkIpReachable(String ipAddress);
}
