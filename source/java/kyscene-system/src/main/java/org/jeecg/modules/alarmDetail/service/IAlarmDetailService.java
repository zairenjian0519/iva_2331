package org.jeecg.modules.alarmDetail.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.entity.AlarmDetailQueryVo;
import org.jeecg.modules.alarmDetail.entity.AlarmSearchVo;

import java.util.List;

/**
 * @Description: 告警检测表
 * @Author: jeecg-boot
 * @Date: 2022-08-15
 * @Version: V1.0
 */
public interface IAlarmDetailService extends IService<AlarmDetail> {
    //最新告警列表（页面暂时不用）
    List<AlarmDetail> getTodayList();

    //最新告警数
    int getTodayCount();

    //相机对应的数量
    List<AlarmDetail> getSeriousList();

    List<AlarmDetail> getAllList();

    int getSeriousList(String cameraName);

    int getAllList(String cameraName);

    int getAlarmAreaList(String alarmArea);

    int getAlarmDeviceList(String alarmDevice);

    IPage<AlarmDetail> selectDetailList(IPage page, AlarmSearchVo alarmSearchVo);

    IPage<AlarmDetail> getSelectDetailList(IPage page, AlarmSearchVo alarmSearchVo);

    //分开获取影响性能 已被优化成下面的方法
    int getAllCount(AlarmSearchVo alarmSearchVo);

    int getRemindCount(AlarmSearchVo alarmSearchVo);

    int getCommonCount(AlarmSearchVo alarmSearchVo);

    int getWarningCount(AlarmSearchVo alarmSearchVo);

    int getSeriousCount(AlarmSearchVo alarmSearchVo);

    //一次获取各种类型的数量
    AlarmDetailQueryVo getCountFunction(AlarmSearchVo alarmSearchVo);

    IPage<AlarmDetail> getDetailByCamera(IPage page, String cameraId);

    int getAlarmCount();

    int getCountByCameraIdAndAlgCode(String cameraId, String algorithmCode, int timeSegment);

    boolean removeByIdWithImage(String id);

    boolean removeByIdsWithImages(List<String> ids);

    void clearAlarm();

    Result<?> clearAlarmByManual();

    void clearAlarmDaysAgo(Integer days);
}
