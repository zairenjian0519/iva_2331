package org.jeecg.modules.alarmDetail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.entity.AlarmDetailQueryVo;
import org.jeecg.modules.alarmDetail.entity.AlarmSearchVo;

import java.util.Date;
import java.util.List;

/**
 * @Description: 告警检测表
 * @Author: jeecg-boot
 * @Date: 2022-08-15
 * @Version: V1.0
 */
@Mapper
public interface AlarmDetailMapper extends BaseMapper<AlarmDetail> {
    List<AlarmDetail> todayList();

    List<AlarmDetail> seriousList();

    List<AlarmDetail> allList();

    int seriousListByName(String cameraName);

    int allListByName(String cameraName);

    int alarmAreaList(String alarmArea);

    int alarmDeviceList(String alarmDevice);

    IPage<AlarmDetail> selectDetailList(IPage page,@Param("alarmSearchVo") AlarmSearchVo alarmSearchVo);

    IPage<AlarmDetail> queryDetailList(IPage<AlarmDetail> page, AlarmSearchVo alarmSearchVo);

    int allCount(AlarmSearchVo alarmSearchVo);

    int remindCount(AlarmSearchVo alarmSearchVo);

    int commonCount(AlarmSearchVo alarmSearchVo);

    int warningCount(AlarmSearchVo alarmSearchVo);

    int seriousCount(AlarmSearchVo alarmSearchVo);


    IPage<AlarmDetail> detailByCamera(IPage page, @Param("cameraId") String cameraId);

    int getAlarmCount();

    AlarmDetailQueryVo getCountFunction(AlarmSearchVo alarmSearchVo);

    int todayCount();

    int getCountByCameraIdAndAlgCode(@Param("cameraId") String cameraId, @Param("algorithmCode") String algorithmCode,
                                     @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    void truncateTable();
}
