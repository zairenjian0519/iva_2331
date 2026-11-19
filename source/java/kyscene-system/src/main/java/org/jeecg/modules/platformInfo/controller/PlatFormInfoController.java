package org.jeecg.modules.platformInfo.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.config.result.Result;
import org.jeecg.modules.deviceinfo.entity.Deviceinfo;
import org.jeecg.modules.deviceinfo.service.DeviceinfoService;
import org.jeecg.modules.platformInfo.entity.Platforminfo;
import org.jeecg.modules.platformInfo.entity.PlatforminfoVO;
import org.jeecg.modules.platformInfo.service.IPlatforminfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/platforminfo")
public class PlatFormInfoController {
    @Autowired
    private IPlatforminfoService iPlatforminfoService;
    @Autowired
    private DeviceinfoService deviceinfoService;
    private String Path = "platforminfo.txt";

    //添加
    @PostMapping(value = "/save")
     public Result save(@RequestBody Platforminfo pfi) {
        //判断平台地址是否存在
        String address = pfi.getAddress();
        List<Platforminfo> platforminfoList = iPlatforminfoService.list();
        for (Platforminfo platforminfo : platforminfoList) {
            if (platforminfo.getAddress().equals(pfi.getAddress())) {
                return Result.error("该平台的ip已存在");
            }
            if (platforminfo.getName().equals(pfi.getName())) {
                return Result.error("该平台名称已存在");
            }
        }
        Platforminfo platforminfoVO = new Platforminfo();
        platforminfoVO.setAddress(pfi.getAddress());
        platforminfoVO.setName(pfi.getName());
        platforminfoVO.setPassword(Base64Encoder(pfi.getPassword()));
        platforminfoVO.setPort(pfi.getPort());
        platforminfoVO.setSipid(pfi.getSipid());
        platforminfoVO.setType(pfi.getType());
        log.info("数据 : " + platforminfoVO.toString());
        iPlatforminfoService.save(platforminfoVO);
        writePlatforminfoToFile();
        return Result.success();
    }

    //查询所有
    @GetMapping("/findAll")
    public Result findAll() {
        //获取所有平台的列表
        List<Platforminfo> platforminfoList = iPlatforminfoService.list();
        for (Platforminfo platforminfo : platforminfoList) {
            platforminfo.setPassword(Base64Decoder(platforminfo.getPassword()));
        }
        writePlatforminfoToFile();
        return Result.success(platforminfoList);
    }

    //查询id
    @GetMapping("/getById")
    public Result getid(@RequestParam(name = "id") String id) {
        Platforminfo platforminfo = iPlatforminfoService.getById(id);
        if (null == platforminfo) {
            return Result.error(0, "该平台不存在");
        }
        String s = Base64Decoder(platforminfo.getPassword());
        platforminfo.setPassword(s);
        return Result.success(platforminfo);
    }

    //删除
    @DeleteMapping("/deleteId")
    public Result deleteid(@RequestParam(name = "id") String id) {
        Platforminfo platforminfo = iPlatforminfoService.getById(id);
        if (null == platforminfo) {
            return Result.error("该平台已删除");
        }
        iPlatforminfoService.removeById(id);
        //获取平台国际编码流查询所有的相机
        String sipid = platforminfo.getSipid();
        List<Deviceinfo> deviceinfoList = deviceinfoService.getFindAll(sipid);
        ArrayList<Object> devId = new ArrayList<>();
        //遍历获取到的所有的相机
        for (Deviceinfo deviceinfo : deviceinfoList) {
            devId.add(sipid);

        }
        deviceinfoService.remove(new LambdaQueryWrapper<Deviceinfo>().eq(Deviceinfo::getPlatformid, sipid));
        writePlatforminfoToFile();

        return Result.success();
    }

    //编辑
    @PostMapping("/edit")
    public Result edis(@RequestBody Platforminfo pif) {
        //通过主键查询
        Platforminfo platforminfo = iPlatforminfoService.getById(pif.getId());
        //查询数据库是否存在
        Platforminfo platforminfo1 = iPlatforminfoService.getOne(new LambdaQueryWrapper<Platforminfo>().eq(Platforminfo::getAddress, pif.getAddress()));
        //判断
        if (null != platforminfo1 && !(platforminfo1.getId().equals(pif.getId()))) {
            return Result.error("该平台ip已存在");
        }
        Platforminfo platforminfo2 = iPlatforminfoService.getOne(new LambdaQueryWrapper<Platforminfo>().eq(Platforminfo::getName, pif.getName()));
        if (null != platforminfo2 && !(platforminfo2.getId().equals(pif.getId()))) {
            return Result.error("该平台名称已存在");
        }
        pif.setPassword(Base64Encoder(pif.getPassword()));
        iPlatforminfoService.updateById(pif);
        writePlatforminfoToFile();
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
     * 平台写入文件,只使用于平台,另一个平台不是IVA
     */
    public  void writePlatforminfoToFile() {
        //获取所有的相机列表
        List<Deviceinfo> deviceinfoList = deviceinfoService.list();
        List<Platforminfo> list = iPlatforminfoService.list();
        List<PlatforminfoVO> platforminfoFile = new ArrayList<>();
        PlatforminfoVO platforminfoVO = new PlatforminfoVO();
        for (Platforminfo platforminfo : list) {
            platforminfoVO.setAddress(platforminfo.getAddress());
            platforminfoVO.setName(platforminfo.getName());
            platforminfoVO.setPassword(platforminfo.getPassword());
            platforminfoVO.setPort(platforminfo.getPort());
            platforminfoVO.setType(platforminfo.getType());
            platforminfoVO.setSipid(platforminfo.getSipid());
        }
        for (Deviceinfo deviceinfo : deviceinfoList) {
            String platformid = deviceinfo.getPlatformid();
            platforminfoVO.devicedListAdd(deviceinfo.getPlatformid());

        }
        platforminfoFile.add(platforminfoVO);
        System.out.println(platforminfoFile.toString());

        // 使用 Jackson 库将集合转为 JSON 格式
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将集合转为 JSON 字符串
            String jsonString = objectMapper.writeValueAsString(platforminfoFile);

            // 将 JSON 字符串写入文件
            objectMapper.writeValue(new File(Path), platforminfoFile);

            System.out.println("Objects have been written to the file: " + Path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
