package org.jeecg.modules.deviceinfo.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.config.result.Result;
import org.jeecg.modules.deviceinfo.entity.Deviceinfo;
import org.jeecg.modules.deviceinfo.entity.DeviceinfoVO;
import org.jeecg.modules.deviceinfo.service.DeviceinfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author
 * @since 2023-12-07
 */
@RestController
@RequestMapping("deviceinfo")
public class DeviceinfoController {

    private String Path = "deviceinfo.txt";
    @Autowired
    private DeviceinfoService deviceinfoService;

    //添加
    @PostMapping("/save")
    public Result deviceinfoSave(@RequestBody Deviceinfo deviceinfo) {
        List<Deviceinfo> deviceinfoList = deviceinfoService.list();
        for (Deviceinfo deviceinfo1 : deviceinfoList) {
            if (deviceinfo1.getSipid().equals(deviceinfo.getSipid())) {
                return Result.error("相机SIPID已存在,请重新输入");
            }
        }
        Deviceinfo deviceinfoVo = new Deviceinfo();
        deviceinfoVo.setAddress(deviceinfo.getAddress());
        deviceinfoVo.setName(deviceinfo.getName());
        deviceinfoVo.setPassword(Base64Encoder(deviceinfo.getPassword()));
        deviceinfoVo.setPort(deviceinfo.getPort());
        deviceinfoVo.setSipid(deviceinfo.getSipid());
        deviceinfoVo.setPlatformid(deviceinfo.getPlatformid());
        deviceinfoVo.setType(deviceinfo.getType());
        deviceinfoService.save(deviceinfoVo);
        writeDeviceinfoToFile();
        return Result.success();
    }

    //查询所有
    @GetMapping("/findAll")
    public Result deviceinfoFindAll(@RequestParam(name = "sipid") String sipid) {
        List<Deviceinfo> deviceinfoList = deviceinfoService.getFindAll(sipid);
        //遍历所有的相机
        for (Deviceinfo deviceinfo : deviceinfoList) {
            deviceinfo.setPassword(Base64Decoder(deviceinfo.getPassword()));
        }
        writeDeviceinfoToFile();
        return Result.success(deviceinfoList);
    }

    //根据id查询
    @GetMapping("/getById")
    public Result getById(@RequestParam(name = "id") String id) {
        Deviceinfo deviceinfo = deviceinfoService.getById(id);
        deviceinfo.setPassword(Base64Decoder(deviceinfo.getPassword()));
        return Result.success(deviceinfo);
    }

    //根据id删除
    @DeleteMapping("/deleteId")
    public Result deleteId(@RequestParam(name = "id") String id) {
        deviceinfoService.removeById(id);
        writeDeviceinfoToFile();
        return Result.success();
    }

    //编辑
    @PostMapping("/edit")
    public Result edis(@RequestBody Deviceinfo deviceinfo) {
        String sipid = deviceinfo.getSipid();
        //根据sipid查询所有的相机
        List<Deviceinfo> list = deviceinfoService.list();

        for (Deviceinfo dev : list) {
            if (dev.getSipid().equals(sipid)) {
                return Result.error("相机SIPID已存在,请重新输入");
            }
        }
        deviceinfo.setPassword(Base64Encoder(deviceinfo.getPassword()));
        deviceinfoService.updateById(deviceinfo);
        writeDeviceinfoToFile();
        return Result.success();
    }

    /**
     * 使用Base64加密
     */
    public String Base64Encoder(String s) {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encode = encoder.encode(s.getBytes());
        return new String(encode);
    }
    /**
     * 使用Base64解密
     */
    public String Base64Decoder(String s) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decode = decoder.decode(s);
        return new String(decode);
    }

    /**
     * 相机写入文件,只适用于相机,另一个平台不是IVA
     */
    public  void writeDeviceinfoToFile() {
        //写入文件
        List<Deviceinfo> deviceinfoList = deviceinfoService.list();
        List<DeviceinfoVO> deviceinfoVOList = new ArrayList<>();
        //遍历所有文件
        for (Deviceinfo deviceinfo : deviceinfoList) {
            DeviceinfoVO deviceinfoVO = new DeviceinfoVO();
            deviceinfoVO.setAddress(deviceinfo.getAddress());
            deviceinfoVO.setName(deviceinfo.getName());
            deviceinfoVO.setPassword(Base64Decoder(deviceinfo.getPassword()));
            deviceinfoVO.setPort(deviceinfo.getPort());
            deviceinfoVO.setSipid(deviceinfo.getSipid());
            deviceinfoVO.setPlatformid(deviceinfo.getPlatformid());
            deviceinfoVO.setType(deviceinfo.getType());
            deviceinfoVOList.add(deviceinfoVO);
        }
        // 使用 Jackson 库将集合转为 JSON 格式
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将集合转为 JSON 字符串
            String jsonString = objectMapper.writeValueAsString(deviceinfoVOList);

            // 将 JSON 字符串写入文件
            objectMapper.writeValue(new File(Path), deviceinfoVOList);

            System.out.println("Objects have been written to the file: " + Path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
