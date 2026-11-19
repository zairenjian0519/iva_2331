package org.jeecg.modules.network.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.network.service.NetworkService;
import org.jeecg.modules.network.utils.IPCheckUtils;
import org.jeecg.modules.network.vo.NetworkInfoVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/5/20 16:35
 **/
@Api(tags = "网口信息")
@RestController
@RequestMapping("/network")
@Slf4j
public class NetworkController {

    @Resource
    private NetworkService networkService;


    @ApiOperation(value = "获取正在连接的网口列表", notes = "获取正在连接的网口列表")
    @GetMapping("/list")
    public Result<List<NetworkInfoVo>> queryNetworkList() {
        List<NetworkInfoVo> networkInfoVo = networkService.getNetworkInfoVo();
        return Result.OK(networkInfoVo);
    }

    @ApiOperation(value = "修改网口信息", notes = "修改网口信息")
    @PostMapping("/update")
    public Result<?> updateNetworkInfo(@RequestBody NetworkInfoVo networkInfoVo) {
        if (Objects.equals(networkInfoVo.getIpMethod(), "manual")) {
            if (StringUtils.isBlank(networkInfoVo.getIpAddress())) {
                return Result.error("IP地址不能为空");
            }
            if (!IPCheckUtils.isValidIPv4(networkInfoVo.getIpAddress())) {
                return Result.error("请输入合法的IP地址");
            }
            if (!IPCheckUtils.isValidSubnetMask(networkInfoVo.getSubnetMask())) {
                return Result.error("请输入合法的子网掩码");
            }
            if (StringUtils.isNotBlank(networkInfoVo.getGateway())){
                if (!IPCheckUtils.isValidIPv4(networkInfoVo.getGateway())) {
                    return Result.error("请输入合法的网关地址");
                }
            }
        } else {
            if (StrUtil.equals(networkInfoVo.getNetworkCard(), "eth1")) {
                return Result.error("Lan1口必须为静态IP");
            }
        }
        int i = networkService.updateNetworkInfo(networkInfoVo);
        if (i > 0) {
            return Result.error("修改失败");
        }
        log.info("edit ip result: {}", i);
        // 更新设备信息中网卡ip
        // IDeviceManagementService deviceManagementService = SpringContextUtils.getBean(IDeviceManagementService.class);
        // List<DeviceManagement> deviceManagementList = deviceManagementService.list();
        // if (ObjectUtil.isNotEmpty(deviceManagementList)) {
        //     DeviceManagement deviceManagement = deviceManagementList.get(0);
        //     deviceManagementService.updateIpById(deviceManagement.getId(), networkInfoVo.getIpAddress());
        // }
        return Result.OK("修改成功");
    }


    @ApiOperation(value = "检查IP是否可用", notes = "检查IP是否可用")
    @PostMapping("/checkIP/{ipAddress}/{networkCard}")
    public Result<?> checkIP(@PathVariable String ipAddress, @PathVariable String networkCard) {
        if (StringUtils.isBlank(ipAddress)) {
            return Result.error("IP地址不能为空");
        }
        if (!IPCheckUtils.isValidIPv4(ipAddress)) {
            return Result.error("请输入合法的IP地址");
        }
        if (networkService.checkIPAvailable(ipAddress, networkCard)) {
            return Result.error("IP已被占用");
        }
        return Result.OK("IP可用");
    }

    @ApiOperation(value = "检查IP是否可达", notes = "检查IP是否可达")
    @PostMapping("/checkIpReachable/{ipAddress}")
    public Result<?> checkIpReachable(@PathVariable String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return Result.error("IP地址不能为空");
        }
        if (!IPCheckUtils.isValidIPv4(ipAddress)) {
            return Result.error("请输入合法的IP地址");
        }
        return Result.OK(networkService.checkIpReachable(ipAddress));
    }
}
