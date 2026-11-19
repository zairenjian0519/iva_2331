package org.jeecg.modules.face.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmFace.AlarmFaceDo;
import org.jeecg.modules.alarmFace.AlarmFaceService;
import org.jeecg.modules.face.baidu.FaceUploadQueue;
import org.jeecg.modules.face.baidu.UserDeleteQueue;
import org.jeecg.modules.face.baidu.UserGroupService;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.entity.FaceQueryParams;
import org.jeecg.modules.face.mapper.FaceDetailMapper;
import org.jeecg.modules.face.properties.FaceConfigProperties;
import org.jeecg.modules.face.remoteSync.GlobalSyncStateCache;
import org.jeecg.modules.face.remoteSync.SyncState;
import org.jeecg.modules.face.service.FaceDetailService;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class FaceDetailServiceImpl extends ServiceImpl<FaceDetailMapper, FaceDetailDo> implements FaceDetailService {

    private final FaceGroupService faceGroupService;

    private final FaceConfigProperties faceConfigProperties;

    private final UserGroupService userGroupService;

    private final FaceUploadQueue faceUploadQueue;

    private final GlobalSyncStateCache globalSyncStateCache;

    private final UserDeleteQueue userDeleteQueue;

    @Override
    public FaceDetailDo getFaceDetail(String id) {
        return null;
    }

    @Override
    public void addFaceDetail(FaceDetailDo faceDetailDo, MultipartFile file) {
        // 通过手机号码或者证件号码判断是否已经存在
        if (StrUtil.isNotBlank(faceDetailDo.getFacePhone())) {
            LambdaQueryWrapper<FaceDetailDo> queryWrapper = Wrappers.lambdaQuery(FaceDetailDo.class);
            queryWrapper.eq(FaceDetailDo::getFacePhone, faceDetailDo.getFacePhone());
            if (this.count(queryWrapper) > 0) {
                throw new JeecgBootException("手机号码已经存在");
            }
        }
        if (StrUtil.isNotBlank(faceDetailDo.getFaceIdCard())) {
            LambdaQueryWrapper<FaceDetailDo> queryWrapper = Wrappers.lambdaQuery(FaceDetailDo.class);
            queryWrapper.eq(FaceDetailDo::getFaceIdCard, faceDetailDo.getFaceIdCard());
            if (this.count(queryWrapper) > 0) {
                throw new JeecgBootException("证件号码已经存在");
            }
        }

        // 单个人脸组人脸总数不能超过配置的条数
        long count = this.count(Wrappers.lambdaQuery(FaceDetailDo.class)
                .eq(FaceDetailDo::getFaceGroup, faceDetailDo.getFaceGroup()));
        if (count == faceConfigProperties.getGroupFaceCount()) {
            throw new JeecgBootException("单组人脸数不能超过" + faceConfigProperties.getGroupFaceCount() + "条");
        }

        // 获取group路径
        FaceGroupDo faceGroupDo = faceGroupService.getById(faceDetailDo.getFaceGroup());
        if (faceGroupDo == null) {
            throw new JeecgBootException("分组不存在");
        }
        String groupPath = faceGroupDo.getGroupPath();

        // 文件重命名未uuid
        String fileName = file.getOriginalFilename();
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        // 重新生成文件名，并且文件名作为 vectorId
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String newFileName = uuid + fileExt;
        String filePath = faceConfigProperties.getBaseFacePath() + groupPath + "/" + newFileName;
        log.info("文件保存路径：" + filePath);
        try {
            file.transferTo(new File(filePath));
            faceDetailDo.setImagePath(filePath);
            faceDetailDo.setImageOriginalName(fileName);
            faceDetailDo.setImageNewName(newFileName);
            faceDetailDo.setImageSize(file.getSize());
            faceDetailDo.setExtName(fileExt.replace(".", ""));
            faceDetailDo.setVectorId(uuid);
            this.save(faceDetailDo);
            // if (SysParameterUtil.isBaiduFaceEnabled()) {
            //     // 上传到百度人脸库
            //     faceUploadQueue.addEvent(faceDetailDo);
            // }
        } catch (IOException e) {
            throw new JeecgBootException("写入文件失败：" + e.getMessage());
        }

    }

    @Override
    public void updateFaceDetail(FaceDetailDo faceDetailDo, MultipartFile file) {
        // 通过手机号码或者证件号码判断是否已经存在
        if (StrUtil.isNotBlank(faceDetailDo.getFacePhone())) {
            LambdaQueryWrapper<FaceDetailDo> queryWrapper = Wrappers.lambdaQuery(FaceDetailDo.class);
            queryWrapper.eq(FaceDetailDo::getFacePhone, faceDetailDo.getFacePhone());
            queryWrapper.ne(FaceDetailDo::getId, faceDetailDo.getId());
            if (this.count(queryWrapper) > 0) {
                throw new JeecgBootException("手机号码被别人使用");
            }
        }
        if (StrUtil.isNotBlank(faceDetailDo.getFaceIdCard())) {
            LambdaQueryWrapper<FaceDetailDo> queryWrapper = Wrappers.lambdaQuery(FaceDetailDo.class);
            queryWrapper.eq(FaceDetailDo::getFaceIdCard, faceDetailDo.getFaceIdCard());
            queryWrapper.ne(FaceDetailDo::getId, faceDetailDo.getId());
            if (this.count(queryWrapper) > 0) {
                throw new JeecgBootException("证件号码被别人使用");
            }
        }

        if (file != null) {
            // 获取group路径
            FaceGroupDo faceGroupDo = faceGroupService.getById(faceDetailDo.getFaceGroup());
            if (faceGroupDo == null) {
                throw new JeecgBootException("分组不存在");
            }
            String groupPath = faceGroupDo.getGroupPath();

            // 文件重命名未uuid
            String fileName = file.getOriginalFilename();
            String fileExt = fileName.substring(fileName.lastIndexOf("."));
            // 重新生成文件名，并且文件名作为 vectorId
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String newFileName = uuid + fileExt;
            String filePath = faceConfigProperties.getBaseFacePath() + groupPath + File.separator + newFileName;
            try {
                file.transferTo(new File(filePath));
                faceDetailDo.setImagePath(filePath);
                faceDetailDo.setImageOriginalName(fileName);
                faceDetailDo.setImageNewName(newFileName);
                faceDetailDo.setImageSize(file.getSize());
                faceDetailDo.setExtName(fileExt.replace(".", ""));
                faceDetailDo.setVectorId(uuid);
            } catch (IOException e) {
                throw new JeecgBootException("写入文件失败");
            }
            // if (SysParameterUtil.isBaiduFaceEnabled()) {
            //     // 上传到百度人脸库
            //     faceUploadQueue.addEvent(faceDetailDo);
            // }
        }
        this.updateById(faceDetailDo);
    }

    @Override
    public IPage<FaceDetailDo> getByGroupId(FaceQueryParams faceQueryParams, int pageNo, int pageSize) {
        Map<String, String> faceGroupMap = faceGroupService.list().stream().collect(Collectors.toMap(FaceGroupDo::getId, FaceGroupDo::getGroupPath));

        Page<FaceDetailDo> page = new Page<>(pageNo, pageSize);
        Page<FaceDetailDo> faceDetailDoPage = this.page(page, Wrappers.lambdaQuery(FaceDetailDo.class)
                .eq(FaceDetailDo::getFaceGroup, faceQueryParams.getGroupId())
                .like(StrUtil.isNotBlank(faceQueryParams.getFaceName()), FaceDetailDo::getFaceName, faceQueryParams.getFaceName())
                .like(StrUtil.isNotBlank(faceQueryParams.getFacePhone()), FaceDetailDo::getFacePhone, faceQueryParams.getFacePhone())
                .like(StrUtil.isNotBlank(faceQueryParams.getFaceIdCard()), FaceDetailDo::getFaceIdCard, faceQueryParams.getFaceIdCard()));

        for (FaceDetailDo faceDetailDo : faceDetailDoPage.getRecords()) {
            String imgUrl = String.format("/face/%s/%s", faceGroupMap.get(faceDetailDo.getFaceGroup()), faceDetailDo.getImageNewName());
            faceDetailDo.setImgUrl(imgUrl);
        }
        return faceDetailDoPage;
    }

    @Override
    public void deleteFaceDetail(String id) {
        // 删除文件
        FaceDetailDo faceDetailDo = this.getById(id);
        if (faceDetailDo != null) {
            File file = new File(faceDetailDo.getImagePath());
            FileUtil.del(file);
            // 删除npy文件
            String groupPath = getAllFaceGroupMap().get(faceDetailDo.getFaceGroup()).getGroupPath();
            String npyFilePath = faceConfigProperties.getNpyFilePath() + groupPath + "/" + faceDetailDo.getVectorId() + ".npy";
            FileUtil.del(npyFilePath);
        }
        this.removeById(id);

        // 删除百度人脸
        // if (SysParameterUtil.isBaiduFaceEnabled()) {
        //     faceDeleteQueue.addEvent(faceDetailDo);
        // }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDeleteFaceDetail(String ids) {
        // 批量删除文件
        List<String> idList = StrUtil.split(ids, StrUtil.C_COMMA);
        List<FaceDetailDo> faceDetailDos = this.listByIds(idList);
        removeFiles(faceDetailDos);
        this.removeBatchByIds(idList);

        // 删除百度人脸
        // if (SysParameterUtil.isBaiduFaceEnabled()) {
        //     faceDetailDos.forEach(faceDeleteQueue::addEvent);
        // }
    }

    @Override
    public void deleteImg(String id) {
        FaceDetailDo faceDetailDo = this.getById(id);
        if (faceDetailDo != null) {
            File file = new File(faceDetailDo.getImagePath());
            FileUtil.del(file);
            // 删除npy文件
            String groupPath = getAllFaceGroupMap().get(faceDetailDo.getFaceGroup()).getGroupPath();
            String npyFilePath = faceConfigProperties.getNpyFilePath() + groupPath + "/" + faceDetailDo.getVectorId() + ".npy";
            FileUtil.del(npyFilePath);

            // 删除百度人脸库数据
            // if (SysParameterUtil.isBaiduFaceEnabled()) {
            //     faceDeleteQueue.addEvent(faceDetailDo);
            // }
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSingleFace(String alarmDetailId, FaceDetailDo faceDetailDo) {
        AlarmFaceService alarmFaceService = SpringContextUtils.getBean(AlarmFaceService.class);
        AlarmFaceDo alarmFaceDo = alarmFaceService.getById(alarmDetailId);
        if (alarmFaceDo == null) {
            throw new JeecgBootException("告警不存在");
        }

        String faceGroup = faceDetailDo.getFaceGroup();
        if (StrUtil.isBlank(faceGroup)) {
            throw new JeecgBootException("分组不存在");
        }

        // 每个分组不能大于5000条
        long count = this.count(Wrappers.lambdaQuery(FaceDetailDo.class).eq(FaceDetailDo::getFaceGroup, faceGroup));
        if (faceConfigProperties.getGroupFaceCount() == count) {
            throw new JeecgBootException("分组已满 " + faceConfigProperties.getGroupFaceCount() + " 条数据");
        }

        FaceGroupDo faceGroupDo = faceGroupService.getById(faceGroup);
        String groupPath = faceGroupDo.getGroupPath();

        // 获取告警图片地址
        String picPath = alarmFaceDo.getPicPath();

        // 通过地址获取faceDetailDo所需文件数据
        try {
            File file = new File(picPath);
            if (file.exists()) {
                byte[] bytes = FileUtils.readFileToByteArray(file);
                faceDetailDo.setImagePath(picPath);
                faceDetailDo.setImageOriginalName(file.getName());
                faceDetailDo.setImageSize((long) bytes.length);
                faceDetailDo.setExtName(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                String uid = UUID.randomUUID().toString().replaceAll("-", "");
                faceDetailDo.setVectorId(uid);
                String newFileName = uid + "." + faceDetailDo.getExtName();
                faceDetailDo.setImageNewName(newFileName);
                String filePath = faceConfigProperties.getBaseFacePath() + groupPath + File.separator + newFileName;
                faceDetailDo.setImagePath(filePath);
                FileUtils.writeByteArrayToFile(new File(filePath), bytes);
                this.save(faceDetailDo);

                // 百度人脸库添加人脸
                // if (SysParameterUtil.isBaiduFaceEnabled()) {
                //     faceUploadQueue.addEvent(faceDetailDo);
                // }

                // 保存后删除文件
                FileUtils.forceDelete(file);
                alarmFaceService.removeById(alarmDetailId);
            } else {
                throw new JeecgBootException("告警图片不存在");
            }
        } catch (IOException e) {
            throw new JeecgBootException("导入失败：" + e.getLocalizedMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchImportFace(String faceGroup, String ids) {
        AlarmFaceService alarmFaceService = SpringContextUtils.getBean(AlarmFaceService.class);
        List<AlarmFaceDo> alarmFaceDoList = alarmFaceService.listByIds(StrUtil.split(ids, StrUtil.C_COMMA));
        if (alarmFaceDoList.isEmpty()) {
            throw new JeecgBootException("告警不存在");
        }
        String baseString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (AlarmFaceDo alarmFaceDo : alarmFaceDoList) {
            try {
                FaceDetailDo faceDetailDo = new FaceDetailDo();
                String randomString = RandomUtil.randomString(baseString, 6);
                faceDetailDo.setFaceName(randomString);
                faceDetailDo.setFaceIdCard(RandomUtil.randomNumbers(18));
                faceDetailDo.setFacePhone(RandomUtil.randomNumbers(11));
                faceDetailDo.setRemark("批量导入");
                faceDetailDo.setFaceGroup(faceGroup);
                // 启用异步 async
                importSingleFace(alarmFaceDo.getId(), faceDetailDo);
            } catch (Exception e) {
                throw new JeecgBootException("导入失败：" + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void exportXls(Integer startNum, Integer exportNum, HttpServletResponse response) {
        if (startNum == null) {
            startNum = 0;
        }
        if (exportNum == null) {
            exportNum = 5000;
        }

        List<FaceDetailDo> faceDetails = this.list(Wrappers.lambdaQuery(FaceDetailDo.class).last("limit " + startNum + "," + exportNum));
        if (faceDetails.isEmpty()) {
            throw new JeecgBootException("没有数据");
        }

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=face_details.xlsx");
        response.setCharacterEncoding("UTF-8");

        // 创建 Excel
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        SXSSFSheet sheet = workbook.createSheet("人脸数据");

        // 设置列宽
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 12 * 256);

        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex()); // 设置字体颜色为白色
        headerStyle.setFont(headerFont);

        // 创建表头
        String[] headers = {"姓名", "证件号", "手机号", "备注", "照片"};
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25); // 设置行高

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 创建内容样式
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 处理数据
        int rowIdx = 1;
        for (FaceDetailDo detail : faceDetails) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(75); // 设置内容行高

            row.createCell(0).setCellValue(detail.getFaceName());
            row.createCell(1).setCellValue(detail.getFaceIdCard());
            row.createCell(2).setCellValue(detail.getFacePhone());
            row.createCell(3).setCellValue(detail.getRemark());

            for (int i = 0; i < 4; i++) {
                row.getCell(i).setCellStyle(contentStyle);
            }

            // 处理图片
            String imagePath = detail.getImagePath();
            if (imagePath != null) {
                insertImage(workbook, sheet, row, 4, imagePath);
            }
        }

        // 导出 Excel
        try (OutputStream out = response.getOutputStream()) {
            workbook.write(out);
        } catch (IOException e) {
            throw new JeecgBootException("导出 Excel 失败");
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void insertImage(SXSSFWorkbook workbook, SXSSFSheet sheet, Row row, int col, String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                return;
            }
            InputStream is = new FileInputStream(file);
            byte[] bytes = is.readAllBytes();
            int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
            is.close();

            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();

            // 计算单元格的精确位置（Excel 使用 1024 作为列宽单位，行高是 1/20 点）
            int colWidth = sheet.getColumnWidth(col); // 获取列宽（单位为 1/256 字符宽度）
            float colPixelWidth = colWidth / 256.0f * 7; // 转换为像素（大约每个字符宽度 7 像素）

            float rowHeight = row.getHeightInPoints(); // 获取行高（单位为点）
            float rowPixelHeight = rowHeight * 1.33f; // 转换为像素（Excel 1 pt ≈ 1.33 像素）

            // 计算图片缩放比例
            ImageIcon imageIcon = new ImageIcon(bytes);
            int imgWidth = imageIcon.getIconWidth();
            int imgHeight = imageIcon.getIconHeight();
            float scaleX = colPixelWidth / imgWidth;
            float scaleY = rowPixelHeight / imgHeight;

            // 设置图片的绝对位置，确保填充单元格
            anchor.setCol1(col);
            anchor.setRow1(row.getRowNum());
            anchor.setDx1(0);
            anchor.setDy1(0);
            anchor.setCol2(col + 1);
            anchor.setRow2(row.getRowNum() + 1);
            anchor.setDx2((int) (scaleX * 1024)); // 设置 X 方向缩放
            anchor.setDy2((int) (scaleY * 256 * 20)); // 设置 Y 方向缩放

            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception e) {
            throw new JeecgBootException("读取图片失败: 路径 " + imagePath);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<String> importExcel(String groupId, MultipartFile file) {
        final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100M
        long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2M

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new JeecgBootException("文件大小超过100M限制");
        }

        // 库内数量
        long count = this.count(Wrappers.lambdaQuery(FaceDetailDo.class).eq(FaceDetailDo::getFaceGroup, groupId));

        List<String> errorList = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows() - 1;
            if (totalRows > (faceConfigProperties.getGroupFaceCount() - count)) {
                throw new JeecgBootException("数据条数超过" + faceConfigProperties.getGroupFaceCount() + "限制");
            }

            // 解析 Excel 中的图片，如果图片格式不是jpg或者png，则抛出异常
            JSONObject imagesFromSheet = extractImagesFromSheet(sheet, workbook);
            imagesFromSheet.forEach((key, value) -> {
                if (value instanceof JSONObject imgInfo) {
                    String fileExtension = imgInfo.getString("fileExtension");
                    fileExtension = fileExtension.toLowerCase();
                    if (!fileExtension.equals("jpg") && !fileExtension.equals("jpeg") && !fileExtension.equals("png")) {
                        String[] rcArr = key.split("-");
                        throw new JeecgBootException("第 " + rcArr[0] + " 行: 图片格式错误，仅支持jpg, jpeg和png");
                    }
                }
            });

            // 获取groupPath
            String groupPath = faceGroupService.getById(groupId).getGroupPath();
            List<FaceDetailDo> faceRecords = new ArrayList<>();

            int rowCount = 0;
            for (Row row : sheet) {
                if (rowCount++ == 0) continue; // 跳过表头

                String name = row.getCell(0).getStringCellValue();

                // 电话号码cell，如果cell为number转为字符串
                String phone = row.getCell(1).getCellType() == CellType.NUMERIC ? String.valueOf(row.getCell(1).getNumericCellValue()) : row.getCell(1).getStringCellValue();

                // 身份证号码cell, 如果cell为number转为字符串
                String idCard = row.getCell(2).getCellType() == CellType.NUMERIC ? String.valueOf(row.getCell(2).getNumericCellValue()) : row.getCell(2).getStringCellValue();

                String remark = row.getCell(3).getStringCellValue();

                if (name == null || name.trim().isEmpty()) {
                    errorList.add("第 " + rowCount + " 行: 姓名缺失");
                    continue;
                }
                // 获取图片字节数组
                String key = rowCount + "-5";
                JSONObject imgInfo = imagesFromSheet.getJSONObject(key);
                if (imgInfo == null || !imgInfo.containsKey("imageData")) {
                    errorList.add("第 " + rowCount + " 行: 人脸图片缺失");
                    continue;
                }
                byte[] imageBytes = imgInfo.getBytes("imageData");
                if (imageBytes.length > MAX_IMAGE_SIZE) {
                    errorList.add("第 " + rowCount + " 行: 人脸图片大小超过2M限制");
                    continue;
                }

                // 检查手机号 和 证件号 是否重复
                if (this.getOne(new LambdaQueryWrapper<FaceDetailDo>()
                        .eq(StrUtil.isNotBlank(phone), FaceDetailDo::getFacePhone, phone)
                        .or()
                        .eq(StrUtil.isNotBlank(idCard), FaceDetailDo::getFaceIdCard, idCard)) != null) {
                    errorList.add("第 " + rowCount + " 行: 手机号或证件号重复");
                    continue;
                }

                // 保存图片到磁盘
                String imgExt = imgInfo.getString("fileExtension");
                JSONObject saveImageResult = saveImage(imageBytes, groupPath, imgExt);

                // 填充数据
                FaceDetailDo faceDetailDo = new FaceDetailDo();
                faceDetailDo.setFaceGroup(groupId);
                faceDetailDo.setFaceName(name);
                faceDetailDo.setExtName(imgExt);
                faceDetailDo.setImageSize((long) imageBytes.length);
                faceDetailDo.setVectorId(saveImageResult.getString("vectorId"));
                faceDetailDo.setImageOriginalName(saveImageResult.getString("imageOriginalName"));
                faceDetailDo.setImageNewName(saveImageResult.getString("imageNewName"));
                faceDetailDo.setImagePath(saveImageResult.getString("imagePath"));
                faceDetailDo.setFacePhone(phone);
                faceDetailDo.setFaceIdCard(idCard);
                faceDetailDo.setRemark(remark);
                faceRecords.add(faceDetailDo);
            }

            // 保存到数据库
            this.saveBatch(faceRecords);

            // 上传到百度人脸库
            // if (SysParameterUtil.isBaiduFaceEnabled()) {
            //     faceRecords.forEach(faceUploadQueue::addEvent);
            // }
        } catch (Exception e) {
            throw new JeecgBootException("文件解析失败:" + e.getMessage());
        }
        return errorList;
    }

    private JSONObject saveImage(byte[] imageBytes, String groupPath, String imgExt) {
        JSONObject result = new JSONObject();
        String simpleUid = UUID.randomUUID().toString().replace("-", "");
        result.put("vectorId", simpleUid);
        result.put("imageOriginalName", simpleUid + "." + imgExt);
        result.put("imageNewName", simpleUid + "." + imgExt);
        String imagePath = faceConfigProperties.getBaseFacePath() + groupPath + File.separator + simpleUid + "." + imgExt;
        result.put("imagePath", imagePath);
        try (FileOutputStream fos = new FileOutputStream(imagePath)) {
            fos.write(imageBytes);
        } catch (IOException e) {
            throw new JeecgBootException("图片保存失败: 路径 " + imagePath);
        }
        return result;
    }

    private JSONObject extractImagesFromSheet(Sheet sheet, Workbook workbook) {
        JSONObject imageJson = new JSONObject();

        if (workbook instanceof XSSFWorkbook) {
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            if (drawing != null) {
                for (Shape shape : drawing) {
                    if (shape instanceof XSSFPicture picture) {
                        ClientAnchor anchor = picture.getClientAnchor();

                        // 获取图片位置
                        int row = anchor.getRow1() + 1;
                        int col = anchor.getCol1() + 1;

                        // 获取图片数据
                        XSSFPictureData pictureData = picture.getPictureData();
                        byte[] imageData = pictureData.getData();
                        String fileExtension = pictureData.suggestFileExtension();
                        JSONObject imgInfoJson = new JSONObject();
                        imgInfoJson.put("fileExtension", fileExtension);
                        imgInfoJson.put("imageData", imageData);
                        // 将图片数据映射到单元格位置
                        imageJson.put(row + "-" + col, imgInfoJson);
                    }
                }
            }
        } else if (workbook instanceof HSSFWorkbook) {
            HSSFPatriarch patriarch = (HSSFPatriarch) sheet.createDrawingPatriarch();
            if (patriarch != null) {
                for (HSSFShape shape : patriarch.getChildren()) {
                    if (shape instanceof HSSFPicture picture) {
                        ClientAnchor anchor = picture.getClientAnchor();

                        // 获取图片位置
                        int row = anchor.getRow1() + 1;
                        int col = anchor.getCol1() + 1;

                        // 获取图片数据
                        HSSFPictureData pictureData = picture.getPictureData();
                        byte[] imageData = pictureData.getData();
                        String fileExtension = pictureData.suggestFileExtension();
                        JSONObject imgInfoJson = new JSONObject();
                        imgInfoJson.put("fileExtension", fileExtension);
                        imgInfoJson.put("imageData", imageData);
                        // 将图片数据映射到单元格位置
                        imageJson.put(row + "-" + col, imgInfoJson);
                    }
                }
            }
        }

        return imageJson;
    }

    private String getImgExtName(String targetKey, JSONObject searchData) {
        for (String key : searchData.keySet()) {
            if (key.equals(targetKey)) {
                JSONObject imgInfo = searchData.getJSONObject(key);
                String fileExtension = imgInfo.getString("fileExtension");
                return fileExtension.toLowerCase();
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchImportFromRemote(JSONArray faceData) {
        FaceGroupDo faceGroupDo = faceGroupService.getOrCreateDefaultGroup();
        String groupPath = faceGroupDo.getGroupPath();
        log.info("获取到groupPath信息：{}, {}", faceGroupDo.getGroupName(), faceGroupDo.getGroupPath());
        List<FaceDetailDo> faceRecords = new ArrayList<>();
        for (int i = 0; i < faceData.size(); i++) {
            JSONObject jsonObject = faceData.getJSONObject(i);
            FaceDetailDo faceDetailDo = new FaceDetailDo();
            faceDetailDo.setId(jsonObject.getString("id"));
            faceDetailDo.setFaceIdCard(jsonObject.getString("id"));
            faceDetailDo.setFaceGroup(faceGroupDo.getId());
            faceDetailDo.setFaceName(jsonObject.getString("name"));
            String photoBase64 = jsonObject.getString("photoBase64");
            // 提取文件扩展名
            String[] parts = photoBase64.split(",");
            if (parts.length != 2) {
                log.error("Base64 字符串格式不正确");
                continue;
            }
            // 提取图片类型 data:image/png;base64
            String mimeType = parts[0];
            String base64Data = parts[1];
            // 默认 png
            String fileExt = "png";
            if (mimeType.contains("/")) {
                fileExt = mimeType.substring(mimeType.indexOf("/") + 1, mimeType.indexOf(";"));
            }
            // 重新生成文件名，并且文件名作为 vectorId
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String newFileName = uuid + "." + fileExt;
            String filePath = faceConfigProperties.getBaseFacePath() + groupPath + "/" + newFileName;
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            try {
                FileUtils.writeByteArrayToFile(new File(filePath), imageBytes);
            } catch (IOException e) {
                log.error("图片保存失败: 路径 " + filePath);
                continue;
            }
            faceDetailDo.setImageNewName(newFileName);
            faceDetailDo.setImageOriginalName(newFileName);
            faceDetailDo.setImagePath(filePath);
            faceDetailDo.setVectorId(uuid);
            faceDetailDo.setExtName(fileExt);
            faceDetailDo.setImageSize((long) imageBytes.length);
            faceDetailDo.setRemark("从远程导入");
            faceRecords.add(faceDetailDo);
        }
        this.saveBatch(faceRecords);

        // 放入百度人脸上传队列
        // if (SysParameterUtil.isBaiduFaceEnabled()) {
        //     faceRecords.forEach(faceUploadQueue::addEvent);
        // }
    }

    @Override
    public void clearAllData() {
        // 获取所有记录
        List<FaceDetailDo> faceDetailDos = this.list();
        removeFiles(faceDetailDos);
        this.baseMapper.truncateTable();

        // 如果启用了百度人脸服务，删除百度人脸组
        if (SysParameterUtil.isBaiduFaceEnabled()) {
            try {
                List<String> faceGroupKeys = faceDetailDos.stream().map(FaceDetailDo::getFaceGroup).distinct().toList();
                List<String> faceGroupIds = faceGroupService.listByIds(faceGroupKeys).stream().map(FaceGroupDo::getGroupId).toList();
                faceGroupIds.forEach(userGroupService::deleteUserGroup);
            } catch (Exception e) {
                log.error("cloud face 人脸组删除失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public void syncBaiduYun(String groupKey) {
        if (!SysParameterUtil.isBaiduFaceEnabled()) {
            return;
        }
        if (globalSyncStateCache.getSyncState().isGlobalState()) {
            throw new JeecgBootException("后台正在同步数据，请稍后再试");
        }
        globalSyncStateCache.getSyncState().setGlobalState(true);


        try {
            List<FaceDetailDo> list = this.list();
            if (ObjectUtil.isEmpty(list)) return;

            FaceGroupDo faceGroupDo = faceGroupService.getById(groupKey);
            if (ObjectUtil.isEmpty(faceGroupDo)) {
                throw new JeecgBootException("人脸组不存在");
            }

            // 获取百度人脸组中的所有用户ID
            List<String> userIdList = new ArrayList<>();
            int start, length = 100, pageNum = 1;
            while (true) {
                start = length * (pageNum - 1);
                Result<JSONObject> result = userGroupService.getUserGroupList(faceGroupDo.getGroupId(), start, length);
                if (!result.isSuccess()) {
                    throw new JeecgBootException("操作失败，人脸组获取失败：" + result.getMessage());
                }
                JSONArray jsonArray = result.getResult().getJSONArray("user_id_list");
                if (jsonArray == null || jsonArray.isEmpty()) break;

                userIdList.addAll(jsonArray.toJavaList(String.class));
                log.info("获取人脸组用户列表，第{}页, 数据长度{}", pageNum, jsonArray.size());
                if (jsonArray.size() < length) break; // 已经取完
                pageNum++;
            }

            // 本地数据集转 Set，提高效率
            Set<String> localVectorIds = list.stream()
                    .map(FaceDetailDo::getVectorId)
                    .collect(Collectors.toSet());

            // 本地有，但百度没有 → 上传
            for (FaceDetailDo faceDetailDo : list) {
                if (!userIdList.contains(faceDetailDo.getVectorId())) {
                    faceUploadQueue.addEvent(faceDetailDo);
                }
            }

            // 百度有，但本地没有 → 删除
            for (String userId : userIdList) {
                if (!localVectorIds.contains(userId)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("userId", userId);
                    map.put("groupId", faceGroupDo.getGroupId());
                    userDeleteQueue.addEvent(map);
                }
            }
        } catch (Exception e) {
            log.error("操作失败：", e);
        }
    }

    // 获取所有的faceGroup数据，并按照id 组合为一个Map
    private Map<String, FaceGroupDo> getAllFaceGroupMap() {
        List<FaceGroupDo> faceGroupDos = faceGroupService.list();
        return faceGroupDos.stream()
                .collect(Collectors.toMap(FaceGroupDo::getId, faceGroupDo -> faceGroupDo));
    }

    private void removeFiles(List<FaceDetailDo> faceDetailDos) {
        for (FaceDetailDo faceDetailDo : faceDetailDos) {
            // 删除人员图片
            String faceImagePath = faceDetailDo.getImagePath();
            FileUtil.del(new File(faceImagePath));
            // 删除npy文件
            String groupPath = getAllFaceGroupMap().get(faceDetailDo.getFaceGroup()).getGroupPath();
            String npyPath = faceConfigProperties.getNpyFilePath() + groupPath + "/" + faceDetailDo.getVectorId() + ".npy";
            FileUtil.del(new File(npyPath));
        }
    }
}
