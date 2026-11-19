package org.jeecg.modules.alarmStatistics.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.img.gif.NeuQuant;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.entity.AlarmDetailQueryVo;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.alarmStatistics.entity.*;
import org.jeecg.modules.alarmStatistics.service.IAlarmStatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;

import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

 /**
 * @Description: 统计分析表
 * @Author: jeecg-boot
 * @Date:   2022-08-12
 * @Version: V1.0
 */
@Slf4j
@Api(tags="统计分析表")
@RestController
@RequestMapping("/alarmStatistics/alarmStatistics")
public class AlarmStatisticsController extends JeecgController<AlarmStatistics, IAlarmStatisticsService> {
	@Autowired
	private IAlarmStatisticsService alarmStatisticsService;
	 @Autowired
	 private IAlarmDetailService alarmDetailService;
	 @Autowired
	 private ICameraManagerService cameraManagerService;
	 @Autowired
	 private IAlgorithmManagerService algorithmManagerService;
	 /**
	 * 分页列表查询
	 *
	 * @param alarmStatistics
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "统计分析表-分页列表查询")
	@ApiOperation(value="统计分析表-分页列表查询", notes="统计分析表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(AlarmStatistics alarmStatistics,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<AlarmStatistics> queryWrapper = QueryGenerator.initQueryWrapper(alarmStatistics, req.getParameterMap());
		Page<AlarmStatistics> page = new Page<AlarmStatistics>(pageNo, pageSize);
		IPage<AlarmStatistics> pageList = alarmStatisticsService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	 @AutoLog(value = "Part1-各类型总数")
	 @ApiOperation(value="Part1-各类型总数", notes="Part1-各类型总数")
	 @GetMapping(value = "/statisticsMainCount")
	 public Result<?> statisticsMainCount(@RequestParam(name = "cameraNames",required = false) List<String> cameraIds,
										  @RequestParam(name = "categorys",required = false) List<String> categorys,
										  @RequestParam(name = "startTime",required = false) String startTime,
										  @RequestParam(name = "endTime",required = false) String endTime
	 ) throws ParseException {
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date start = sdf.parse(startTime);
		 Date end = sdf.parse(endTime);
		 //需要计算上一周期区间，获得两日期之前相隔天数，并共同减去此天数
		 long nd = 1000 * 24 * 60 * 60;
		 long nh = 1000 * 60 * 60;
		 long nm = 1000 * 60;
		 long diff = end.getTime() - start.getTime();
		 long day = diff / nd;
		 long hour = diff % nd / nh ;
		 long min = diff % nd % nh / nm;
		 //若两日期都为当天则差前一天
		 if (day==0){
			 day=1;
		 } else if (hour>0||min>0){
//			 System.out.println("准确相差："+day+"天"+hour+"小时"+min+"分钟");
			 day+=1;
		 }
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(start);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastStartTime = sdf.format(cal.getTime());
		 cal.setTime(end);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastEndTime = sdf.format(cal.getTime());
//		 System.out.println("天数："+day);
//		 System.out.println("本周期开始："+startTime+","+"本周期结束："+endTime);
//		 System.out.println("上周期开始："+lastStartTime+","+"上周期结束："+lastEndTime);
		 //上个周期的告警数
		 int lastAllCount = alarmStatisticsService.getAllCount(cameraIds,categorys,lastStartTime,lastEndTime);
		 //告警数量
		 MainCountVo mainCountVo = alarmStatisticsService.getMainCount(cameraIds,categorys,startTime,endTime);

		 //计算当前周期比上一个周期的增长率
		 String rate = "";
		 //如果上一周期数量为0，增长率100%、若都为0增长率为0%
		 if (lastAllCount==0&&mainCountVo.getAllCount()!=0){
			 rate = "100.00%";
		 }else if(lastAllCount==0&&mainCountVo.getAllCount()==0){
			 rate = "00.00%";
		 }else {
			 rate = getAnalysisData(mainCountVo.getAllCount(), lastAllCount);
		 }
		 //相机总数
		 List<CameraManager> cameraManagerList = cameraManagerService.list();
		 Integer cameraAmount = cameraManagerList.size();
		 //算法数
		 Integer categoryAmount = algorithmManagerService.getAllAlgorithmCount(cameraIds,categorys);

		 Map<String,Object> map = new HashMap();
		 map.put("rate",rate);							  //增长率
		 map.put("allCount",mainCountVo.getAllCount());                    //告警总数
		 map.put("remindCount",mainCountVo.getRemindCount());			  //提醒告警数
		 map.put("commonCount",mainCountVo.getCommonCount());			  //一般告警数
		 map.put("warningCount",mainCountVo.getWarningCount());			  //警告告警数
		 map.put("seriousCount",mainCountVo.getSeriousCount());			  //严重告警数
		 map.put("cameraAmount",cameraAmount);			 //所有相机数量
		 map.put("categoryAmount",categoryAmount);			 //所有告警类型数量
		 return Result.OK(map);
	 }

	 @AutoLog(value = "Part2-告警统计（相机）/（区域）")
	 @ApiOperation(value="Part2-告警统计（相机）/（区域）", notes="Part2-告警统计（相机）/（区域）")
	 @GetMapping(value = "/statisticsByCamera")
	 public Result<?> statisticsByCamera(@RequestParam(name = "cameraNames",required = false) List<String> cameraIds,
										 @RequestParam(name = "categorys",required = false) List<String> categorys,
										 @RequestParam(name = "startTime",required = false) String startTime,
										 @RequestParam(name = "endTime",required = false) String endTime
	 ){
		 //告警统计图（相机）
		 List<CountVo> cameraCountList = alarmStatisticsService.getCameraCountList(cameraIds,categorys,startTime,endTime);
		 //避免查出来的数据name为null，根据相机名再查一次过滤掉告警等级为0，即正常的告警，不参与统计。
//		 for (CountVo camera : cameraCountList){
//			 List<AlarmDetail> alarmDetailList = alarmDetailService.list(new QueryWrapper<AlarmDetail>()
//					 .lambda()
//					 .eq(AlarmDetail::getCameraName,camera.getName())
//					 .eq(AlarmDetail::getAlarmLevel,0));
//			 if (camera.getValue()-alarmDetailList.size()<0){
//				 camera.setValue(0);
//			 }else{
//				 camera.setValue(camera.getValue()-alarmDetailList.size());
//			 }
//		 }
		 //告警统计图（区域）
		 List<CountVo> locationCountList = alarmStatisticsService.getLocationCountList(cameraIds,categorys,startTime,endTime);
//		 for (CountVo location : locationCountList){
//			 List<AlarmDetail> alarmDetailList = alarmDetailService.list(new QueryWrapper<AlarmDetail>()
//					 .lambda()
//					 .eq(AlarmDetail::getAlarmArea,location.getAreaId())
//					 .eq(AlarmDetail::getAlarmLevel,0));
//			 if (location.getValue()-alarmDetailList.size()<0){
//				 location.setValue(0);
//			 }else{
//				 location.setValue(location.getValue()-alarmDetailList.size());
//			 }
//		 }
		 Map<String,Object> map = new HashMap();
		 map.put("cameraCountList",cameraCountList.stream().sorted(Comparator.comparing(CountVo::getValue).reversed()).collect(Collectors.toList()));	 //告警统计图（相机）
		 map.put("locationCountList",locationCountList);	 //告警统计图（区域）
		 return Result.OK(map);
	 }

	 @AutoLog(value = "Part3-告警统计（类型）")
	 @ApiOperation(value="Part3-告警统计（类型）", notes="Part3-告警统计（类型）")
	 @GetMapping(value = "/statisticsByType")
	 public Result<?> statisticsByType(@RequestParam(name = "cameraNames",required = false) List<String> cameraIds,
									   @RequestParam(name = "categorys",required = false) List<String> categorys,
									   @RequestParam(name = "startTime",required = false) String startTime,
									   @RequestParam(name = "endTime",required = false) String endTime
	 ){
		 List<CountVo> typeCountList = alarmStatisticsService.getTypeCountList(cameraIds,categorys,startTime,endTime);
		 Map<String,Object> map = new HashMap();
		 map.put("typeCountList",typeCountList);
		 return Result.OK(map);
	 }

	 @AutoLog(value = "Part4-告警统计（日期）")
	 @ApiOperation(value="Part4-告警统计（日期）", notes="Part4-告警统计（日期）")
	 @GetMapping(value = "/statisticsByDate")
	 public Result<?> statisticsByDate(@RequestParam(name = "cameraNames",required = false) List<String> cameraIds,
									   @RequestParam(name = "categorys",required = false) List<String> categorys,
									   @RequestParam(name = "startTime",required = false) String startTime,
									   @RequestParam(name = "endTime",required = false) String endTime
	 )throws ParseException {
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date start = sdf.parse(startTime);
		 Date end = sdf.parse(endTime);
		 //需要计算上一周期区间，获得两日期之前相隔天数，并共同减去此天数
		 long nd = 1000 * 24 * 60 * 60;
		 long nh = 1000 * 60 * 60;
		 long nm = 1000 * 60;
		 long diff = end.getTime() - start.getTime();
		 long day = diff / nd;
		 long hour = diff % nd / nh ;
		 long min = diff % nd % nh / nm;
		 //若两日期都为当天则差前一天
		 if (day==0){
			 day=1;
		 } else if (hour>0||min>0){
//			 System.out.println("准确相差："+day+"天"+hour+"小时"+min+"分钟");
			 day+=1;
		 }
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(start);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastStartTime = sdf.format(cal.getTime());
		 cal.setTime(end);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastEndTime = sdf.format(cal.getTime());
		 //告警统计（日期）
		 List<DateCountVo> dateCountList = alarmStatisticsService.getDateCountList(cameraIds,categorys,startTime,endTime);
		 //告警统计（日期）上一周期
		 List<DateCountVo> lastDateCountList = alarmStatisticsService.getDateCountList(cameraIds,categorys,lastStartTime,lastEndTime);
		 //取所选日期区间的日期
		 List<String> days = this.getDays(startTime,endTime);
		 List<DateCountVo> dateNewList = new ArrayList<>();
		 for(String daysList:days) {
			 //为了把value为空的日期补0 并返回到前端
			 DateCountVo dateCountVo = new DateCountVo();
			 dateCountVo.setDate(daysList);
			 dateCountVo.setValue(0);
			 dateCountVo.setRemindValue(0);
			 dateCountVo.setSeriousValue(0);
			 dateCountVo.setCommonValue(0);
			 dateCountVo.setWarningValue(0);
			 //按日期统计各类别告警
			 for (DateCountVo dateAccount : dateCountList) {
				 if (dateAccount.getDate().equals(daysList)) {
					 dateCountVo.setValue(dateAccount.getValue());
					 dateCountVo.setSeriousValue(dateAccount.getSeriousValue());
					 dateCountVo.setCommonValue(dateAccount.getCommonValue());
					 dateCountVo.setWarningValue(dateAccount.getWarningValue());
					 dateCountVo.setRemindValue(dateAccount.getRemindValue());
				 }
			 }
			 dateNewList.add(dateCountVo);
		 }

		 //取所选日期区间的上一周期时间
		 List<String> lastDays = this.getDays(lastStartTime,lastEndTime);
		 List<DateCountVo> lastDateList = new ArrayList<>();
		 for(String daysList:lastDays) {
			 //为了把value为空的日期补0 并返回到前端
			 DateCountVo lastDateVo = new DateCountVo();
			 lastDateVo.setDate(daysList);
			 lastDateVo.setValue(0);
			 lastDateVo.setRemindValue(0);
			 lastDateVo.setSeriousValue(0);
			 lastDateVo.setCommonValue(0);
			 lastDateVo.setWarningValue(0);
			 //按日期统计各类别告警
			 for (DateCountVo lastDateAccount : lastDateCountList) {
				 if (lastDateAccount.getDate().equals(daysList)) {
					 lastDateVo.setValue(lastDateAccount.getValue());
					 lastDateVo.setSeriousValue(lastDateAccount.getSeriousValue());
					 lastDateVo.setCommonValue(lastDateAccount.getCommonValue());
					 lastDateVo.setWarningValue(lastDateAccount.getWarningValue());
					 lastDateVo.setRemindValue(lastDateAccount.getRemindValue());
				 }
			 }
			 lastDateList.add(lastDateVo);
		 }
		 Map<String,Object> map = new HashMap();
		 map.put("dateCountList",dateNewList);			 //告警统计（日期）
		 map.put("lastDateCountList",lastDateList);			 //告警统计（日期）上一周期
		 return Result.OK(map);
	 }

	 @AutoLog(value = "Part5-告警统计（24小时）")
	 @ApiOperation(value="Part5-告警统计（24小时）", notes="Part5-告警统计（24小时）")
	 @GetMapping(value = "/statisticsByHour")
	 public Result<?> statisticsByHour(@RequestParam(name = "cameraNames",required = false) List<String> cameraIds,
									   @RequestParam(name = "categorys",required = false) List<String> categorys,
									   @RequestParam(name = "startTime",required = false) String startTime,
									   @RequestParam(name = "endTime",required = false) String endTime
	 )throws ParseException {
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date start = sdf.parse(startTime);
		 Date end = sdf.parse(endTime);
		 //需要计算上一周期区间，获得两日期之前相隔天数，并共同减去此天数
		 long nd = 1000 * 24 * 60 * 60;
		 long nh = 1000 * 60 * 60;
		 long nm = 1000 * 60;
		 long diff = end.getTime() - start.getTime();
		 long day = diff / nd;
		 long hour = diff % nd / nh ;
		 long min = diff % nd % nh / nm;
		 //若两日期都为当天则差前一天
		 if (day==0){
			 day=1;
		 } else if (hour>0||min>0){
//			 System.out.println("准确相差："+day+"天"+hour+"小时"+min+"分钟");
			 day+=1;
		 }
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(start);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastStartTime = sdf.format(cal.getTime());
		 cal.setTime(end);
		 cal.add(Calendar.DATE, (int) -day);
		 String lastEndTime = sdf.format(cal.getTime());

		 //24小时告警态势图
		 List<HourCountVo> hourCountList = alarmStatisticsService.getHourCountList(cameraIds,categorys,startTime,endTime);
		 //24小时告警态势图 上一周期
		 List<HourCountVo> lastHourCountList = alarmStatisticsService.getHourCountList(cameraIds,categorys,lastStartTime,lastEndTime);

		 Map<String,Object> map = new HashMap();
		 map.put("hourCountList",hourCountList);		 //24小时告警态势图
		 map.put("lastHourCountList",lastHourCountList);		 //24小时告警态势图 上一周期
		 return Result.OK(map);
	 }


	 //方法用于取所选日期之间的所有日期
	 public static List<String> getDays(String startTime, String endTime) {
		 // 返回的日期集合
		 List<String> days = new ArrayList<String>();
		 DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		 try {
			 Date start = dateFormat.parse(startTime);
			 Date end = dateFormat.parse(endTime);
			 Calendar tempStart = Calendar.getInstance();
			 tempStart.setTime(start);
			 Calendar tempEnd = Calendar.getInstance();
			 tempEnd.setTime(end);
			 tempEnd.add(Calendar.DATE, +1);// 日期加1(包含结束)
			 while (tempStart.before(tempEnd)) {
				 days.add(dateFormat.format(tempStart.getTime()));
				 tempStart.add(Calendar.DAY_OF_YEAR, 1);
			 }
		 } catch (ParseException e) {
			 e.printStackTrace();
		 }
		 return days;
	 }
	 //方法用于计算增长率
	 public static String getAnalysisData(float sameMonth, float lastMonth) {
		 if (lastMonth < sameMonth) {
			 //如果下个数大于上个数，则增长率 为 正
			 float result = (sameMonth - lastMonth) / lastMonth * 100;
			 return  String.format("%10.2f%%", result).trim();
		 } else if (lastMonth > sameMonth) {
			 //如果下个数小于上个数，则增长率 为 负
			 float result = (sameMonth - lastMonth) / lastMonth * 100;
			 return  String.format("%10.2f%%", result).trim();
		 } else {
			 //如果相等，增长率为 0
			 return "0.00%";
		 }
	 }


	/**
	 * 添加
	 *
	 * @param alarmStatistics
	 * @return
	 */
	@AutoLog(value = "统计分析表-添加")
	@ApiOperation(value="统计分析表-添加", notes="统计分析表-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody AlarmStatistics alarmStatistics) {
		alarmStatisticsService.save(alarmStatistics);
		return Result.OK("添加成功！");
	}
	
	/**
	 * 编辑
	 *
	 * @param alarmStatistics
	 * @return
	 */
	@AutoLog(value = "统计分析表-编辑")
	@ApiOperation(value="统计分析表-编辑", notes="统计分析表-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody AlarmStatistics alarmStatistics) {
		alarmStatisticsService.updateById(alarmStatistics);
		return Result.OK("编辑成功!");
	}
	
	/**
	 * 通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "统计分析表-通过id删除")
	@ApiOperation(value="统计分析表-通过id删除", notes="统计分析表-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		alarmStatisticsService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "统计分析表-批量删除")
	@ApiOperation(value="统计分析表-批量删除", notes="统计分析表-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.alarmStatisticsService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "统计分析表-通过id查询")
	@ApiOperation(value="统计分析表-通过id查询", notes="统计分析表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		AlarmStatistics alarmStatistics = alarmStatisticsService.getById(id);
		return Result.OK(alarmStatistics);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param alarmStatistics
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, AlarmStatistics alarmStatistics) {
      return super.exportXls(request, alarmStatistics, AlarmStatistics.class, "统计分析表");
  }

  /**
   * 通过excel导入数据
   *
   * @param request
   * @param response
   * @return
   */
  @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
  public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
      return super.importExcel(request, response, AlarmStatistics.class);
  }

}
