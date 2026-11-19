package org.jeecg.modules.alarmDetail.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmAudit.mapper.AlarmAuditMapper;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.entity.AlarmDetailQueryVo;
import org.jeecg.modules.alarmDetail.entity.AlarmSearchVo;
import org.jeecg.modules.alarmDetail.mapper.AlarmDetailMapper;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.StorageUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.retry.mapper.AlarmRetryMapper;
import org.jeecg.modules.thirdApi.retry.service.AlarmRetryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 告警检测表
 * @Author: jeecg-boot
 * @Date: 2022-08-15
 * @Version: V1.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AlarmDetailServiceImpl extends ServiceImpl<AlarmDetailMapper, AlarmDetail> implements IAlarmDetailService {
    private static final String ALARM_STORAGE_PATH_ENV = "KYSCENE_ALARM_STORAGE_PATH";
    private static final int CLEAN_BATCH_SIZE = 200;
    private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final AlarmDetailMapper alarmDetailMapper;
    private final AtomicBoolean alarmCleanupRunning = new AtomicBoolean(false);

    @Override
    public List<AlarmDetail> getTodayList() {
        return alarmDetailMapper.todayList();
    }

    @Override
    public int getTodayCount() {
        return alarmDetailMapper.todayCount();
    }

    @Override
    public List<AlarmDetail> getSeriousList() {
        return alarmDetailMapper.seriousList();
    }

    @Override
    public List<AlarmDetail> getAllList() {
        return alarmDetailMapper.allList();
    }

    @Override
    public int getAlarmAreaList(String alarmArea) {
        return alarmDetailMapper.alarmAreaList(alarmArea);
    }

    @Override
    public int getAlarmDeviceList(String alarmDevice) {
        return alarmDetailMapper.alarmDeviceList(alarmDevice);
    }

    //根据相机名获取对应的告警总数和严重告警数
    @Override
    public int getSeriousList(String cameraName) {
        return alarmDetailMapper.seriousListByName(cameraName);
    }

    @Override
    public int getAllList(String cameraName) {
        return alarmDetailMapper.allListByName(cameraName);
    }

    @Override
    public IPage<AlarmDetail> selectDetailList(IPage page, AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.queryDetailList(page,alarmSearchVo);
    }

    @Override
    public IPage<AlarmDetail> getSelectDetailList(IPage page, AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.selectDetailList(page, alarmSearchVo);
    }

    @Override
    public int getAllCount(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.allCount(alarmSearchVo);
    }

    @Override
    public int getRemindCount(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.remindCount(alarmSearchVo);
    }

    @Override
    public int getCommonCount(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.commonCount(alarmSearchVo);
    }

    @Override
    public int getWarningCount(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.warningCount(alarmSearchVo);
    }

    @Override
    public int getSeriousCount(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.seriousCount(alarmSearchVo);
    }

    @Override
    public IPage<AlarmDetail> getDetailByCamera(IPage page, String cameraId) {
        return alarmDetailMapper.detailByCamera(page, cameraId);
    }

    @Override
    public int getAlarmCount() {
        return alarmDetailMapper.getAlarmCount();
    }


    @Override
    public AlarmDetailQueryVo getCountFunction(AlarmSearchVo alarmSearchVo) {
        return alarmDetailMapper.getCountFunction(alarmSearchVo);
    }

    @Override
    public int getCountByCameraIdAndAlgCode(String cameraId, String algorithmCode, int timeSegment) {
        Date endDate = new Date();
        long startTime = endDate.getTime() - timeSegment * 1000L;
        Date startDate = new Date(startTime);
        return this.baseMapper.getCountByCameraIdAndAlgCode(cameraId, algorithmCode, startDate, endDate);
    }

    @Override
    public boolean removeByIdWithImage(String id) {
        AlarmDetail alarm = getById(id);
        if (alarm != null) {
            deleteAlarmImages(alarm);
        }
        return removeById(id);
    }

    @Override
    public boolean removeByIdsWithImages(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<AlarmDetail> alarms = listByIds(ids);
        for (AlarmDetail alarm : alarms) {
            deleteAlarmImages(alarm);
        }
        return removeByIds(ids);
    }

    // 一键清楚
    @Override
    public Result<?> clearAlarmByManual() {
        try {
            int deletedFiles = StorageUtil.clearAlarmImages();
            log.info("一键清除告警图片完成，删除文件数: {}", deletedFiles);
        } catch (Exception e) {
            log.error("一键清除告警图片失败", e);
            return Result.error("清除告警图片失败：" + e.getMessage());
        }

        // 清空 alarm_details 和 alarm_audit 还有 alarm_retry_data 三张表
        alarmDetailMapper.truncateTable();

        AlarmAuditMapper alarmAuditMapper = SpringContextUtils.getBean(AlarmAuditMapper.class);
        alarmAuditMapper.truncateTable();

        AlarmRetryMapper alarmRetryMapper = SpringContextUtils.getBean(AlarmRetryMapper.class);
        alarmRetryMapper.truncateTable();

        return Result.ok("一键清除成功");
    }

    @Override
    public void clearAlarmDaysAgo(Integer days) {
        if (days == null || days <= 0) {
            log.warn("清理天数不合法，跳过清理");
            return;
        }
        long timeThreshold = days * 24 * 60 * 60 * 1000L;
        cleanByTime(timeThreshold);
    }

    @Override
    public void clearAlarm() {
        if (!alarmCleanupRunning.compareAndSet(false, true)) {
            log.warn("告警清理任务仍在执行，跳过本次触发");
            return;
        }
        try {
            SysParameter sysParameter = SysParameterUtil.getSysParameter("AlarmClearRule");
            if (sysParameter == null) {
                log.warn("未找到告警清理规则配置");
                return;
            }
            String value = sysParameter.getParamVal();
            JSONObject json = JSON.parseObject(value);
            // {"usableSpace":1,"fileRetentionDays":7}
            int fileRetentionDays = json.getIntValue("fileRetentionDays");
            int freeSpace = json.getIntValue("usableSpace");
            if (fileRetentionDays <= 0) {
                log.error("告警保留天数必须大于 0，跳过清理: {}", fileRetentionDays);
                return;
            }
            if (freeSpace < 0) {
                log.error("告警预留空间不能小于 0，跳过清理: {}", freeSpace);
                return;
            }
            long timeThreshold = fileRetentionDays * 24L * 60L * 60L * 1000L;
            clearStorageRule(freeSpace, timeThreshold);
        } catch (Exception e) {
            log.error("告警清理失败: {}", e.getLocalizedMessage());
        } finally {
            alarmCleanupRunning.set(false);
        }
    }

    /**
     * freeSpace: 预留空间(GB)
     * timeThreshold: 时间阈值(毫秒) —— 只删除早于此时间的文件
     */
    public void clearStorageRule(int freeSpace, long timeThreshold) {
        // Step 1: 按时间清理
        cleanByTime(timeThreshold);

        // usableSpace=0 表示仅按保留天数清理，不启用容量清理。
        if (freeSpace == 0) {
            log.info("告警容量清理未启用，仅执行按时间清理");
            return;
        }

        String storagePath = System.getenv(ALARM_STORAGE_PATH_ENV);
        if (storagePath == null || storagePath.trim().isEmpty()) {
            log.error("未配置 {}，为保护告警数据跳过容量清理", ALARM_STORAGE_PATH_ENV);
            return;
        }

        long reservedSpaceBytes = Math.multiplyExact((long) freeSpace, BYTES_PER_GB);
        long usableSpaceBytes;
        try {
            usableSpaceBytes = StorageUtil.getUsableSpaceBytes(storagePath);
        } catch (RuntimeException e) {
            log.error("无法读取告警存储目录容量，为保护告警数据跳过容量清理: path={}, error={}",
                    storagePath, e.getMessage());
            return;
        }

        log.info("告警存储容量检查: path={}, usable={}MB, reserved={}MB",
                storagePath, usableSpaceBytes / BYTES_PER_MB, reservedSpaceBytes / BYTES_PER_MB);

        // Step 2: 如果告警存储分区仍不足，则从最旧告警开始清理。
        if (usableSpaceBytes < reservedSpaceBytes) {
            cleanByCapacity(storagePath, reservedSpaceBytes);
        }

        log.info("告警清理结束: path={}, usable={}MB",
                storagePath, StorageUtil.getUsableSpace(storagePath));
    }

    /**
     * 删除所有超过时间阈值的记录（最安全）
     */
    private void cleanByTime(long timeThreshold) {

        long current = System.currentTimeMillis();

        while (true) {
            // 删除后结果集会前移，必须始终读取第一页，否则会跳过记录。
            Page<AlarmDetail> page = new Page<>(1, CLEAN_BATCH_SIZE);
            IPage<AlarmDetail> result = this.baseMapper.selectPage(
                    page,
                    Wrappers.lambdaQuery(AlarmDetail.class)
                            .lt(AlarmDetail::getCreateTime, new Date(current - timeThreshold))
                            .orderByAsc(AlarmDetail::getCreateTime)
            );

            if (result.getRecords().isEmpty()) break;

            deleteRecords(result.getRecords());
        }

        log.info("按时间清理结束");
    }

    /**
     * 强制按容量清理，从最旧的开始删
     */
    private void cleanByCapacity(String storagePath, long reservedSpaceBytes) {

        while (true) {
            long usableBefore = StorageUtil.getUsableSpaceBytes(storagePath);
            if (usableBefore >= reservedSpaceBytes) {
                break;
            }

            Page<AlarmDetail> page = new Page<>(1, CLEAN_BATCH_SIZE);
            IPage<AlarmDetail> result = this.baseMapper.selectPage(
                    page,
                    Wrappers.lambdaQuery(AlarmDetail.class)
                            .orderByAsc(AlarmDetail::getCreateTime)
            );

            if (result.getRecords().isEmpty()) {
                log.error("已经没有可删文件了，但磁盘仍不足！可能需要增加存储容量！");
                break;
            }

            deleteRecords(result.getRecords());

            long usableAfter = StorageUtil.getUsableSpaceBytes(storagePath);
            if (usableAfter <= usableBefore) {
                log.error("删除最旧告警后存储空间未增加，停止容量清理以保护数据库记录: " +
                                "path={}, usable={}MB, batchSize={}",
                        storagePath, usableAfter / BYTES_PER_MB, result.getRecords().size());
                break;
            }
        }

        log.info("按容量清理结束");
    }

    private void deleteRecords(List<AlarmDetail> records) {

        List<String> ids = new ArrayList<>();
        List<String> cameraIds = new ArrayList<>();

        for (AlarmDetail alarm : records) {

            deleteAlarmImages(alarm);

            ids.add(alarm.getId());
            cameraIds.add(alarm.getCameraId());
        }

        // 删除数据库
        if (!ids.isEmpty()) {
            this.removeByIds(ids);
            SpringContextUtils.getBean(AlarmRetryService.class).removeByCameraIds(cameraIds);
            SpringContextUtils.getBean(IAlarmAuditService.class).removeByAlarmIds(ids);
        }
    }

    private void deleteAlarmImages(AlarmDetail alarm) {
        if (alarm == null || alarm.getPicPath() == null) {
            return;
        }
        StorageUtil.deleteAlarmImagePair(alarm.getPicPath());
    }
}
