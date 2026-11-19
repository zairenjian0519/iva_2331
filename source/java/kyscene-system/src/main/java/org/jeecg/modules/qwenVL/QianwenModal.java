package org.jeecg.modules.qwenVL;

import cn.hutool.core.io.FileUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class QianwenModal {

    private final Properties properties;

    // 初始化系统消息
    private MultiModalMessage initSystemMsg() {
        String templateMessage = """
                 你是一个高级算法工程师，对于图像的理解和识别都是专家级别的，现在你的工作就是图像识别。
                 请你按照用户的提问，以标准的 JSON 格式输出答案，严格按照以下格式输出：
                 {"result": true, "count": 1, "bbox_2d": []}
                
                 要求：
                 - result 只有两种值 true/false
                 - count 表示图中符合要求的计数
                 - bbox_2d 为数组，长度为 count
                 - bbox_2d 数组中的元素格式为 {"box":[x1, y1, x2, y2]}，坐标基于原始图片的像素位置
                
                 必须严格遵守以下规则：
                 1. 仅关注明显、肉眼可见的缺失区域，忽略细小缺口、临时遮挡或不影响整体结构的情况。
                 2. 绝对禁止输出任何 Markdown 标记，包括 ```json、``` 等。
                 3. 不要输出任何解释、文字说明或额外字符，只输出 JSON。
                 4. 输出必须是有效 JSON，对象键和字符串使用双引号。
                 5. 确保输出可以被编程语言直接解析，不包含多余的空格或换行。
                
                 输出示例：
                 {"result": true, "count": 1, "bbox_2d": [{"box": [1578, 436, 1630, 489]}]}
                """;

        return MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(List.of(Collections.singletonMap("text", templateMessage))).build();
    }

    // 初始化用户消息
    private MultiModalMessage initUserMsg(String sourcePath, String targetPath) {
        String userMsg = """
                这是工地施工现场，我传给你的图片是监控视频中的部分切图画面，请判断图片是否存在围栏结构不连续即缺失，仅关注较为明显的区域，
                对每个缺失区域，输出一个基于原图像像素坐标的 bbox。
                
                输出要求：
                1. 输出标准 JSON 格式，不要输出任何 Markdown 代码块或多余文字。
                2. 输出格式为：
                   {"result": true/false, "count": 数量, "bbox_2d": [{"box":[x1,y1,x2,y2]}, ...]}
                3. result 为 true 表示存在缺失，false 表示不存在。
                4. count 为缺失区域数量。
                5. bbox_2d 数组长度等于 count，每个元素为 {"box":[左上x, 左上y, 右下x, 右下y]}。
                """;
        if (enableBaseImg()) {
            userMsg = """
                    这是一个工地场景，我上传了两张图片：
                    1. 第一张为参考的工地围栏图片。
                    2. 第二张为待检测图片。
                    
                    任务：
                    根据第一张图片的围栏特征，在第二张图片中寻找特征相似的围栏，并判断围栏结构是否连续。
                    如果围栏存在缺失，请对每个缺失区域输出一个基于第二张原始图像像素坐标的 bbox（[左上x, 左上y, 右下x, 右下y]，单位为像素）。
                    
                    输出要求：
                    1. 输出严格的标准 JSON 格式，不要包含任何 Markdown 代码块、换行符、注释或多余文字。
                    2. 格式：
                       {"result": true/false, "count": 数量, "bbox_2d": [{"box":[x1,y1,x2,y2]}, ...]}
                    3. result：true 表示存在缺失，false 表示不存在。
                    4. count：缺失区域数量。
                    5. bbox_2d 数组长度等于 count，每个元素为 {"box":[左上x, 左上y, 右下x, 右下y]}。
                    """;
        }
        List<Map<String, Object>> content = new ArrayList<>();
        if (enableBaseImg()) {
            content.add(Collections.singletonMap("image", getFilePath(sourcePath)));
        }
        content.add(Collections.singletonMap("image", getFilePath(targetPath)));
        content.add(Collections.singletonMap("text", userMsg));
        return MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(content).build();
    }

    // 初始化多模态对话参数
    private MultiModalConversationParam initParams(MultiModalMessage systemMsg, MultiModalMessage userMsg) {
        log.info("模型名称：{}", getModelName());
        return MultiModalConversationParam.builder()
                .apiKey(properties.getApiKey())
                .model(getModelName())
                .messages(Arrays.asList(systemMsg, userMsg))
                .vlHighResolutionImages(true)
                .build();
    }

    // 多模态对话调用
    // 提示词：图片是否存在围栏缺失或破损，仅关注肉眼可见、较为明显的区域。
    public JSONObject MultiModalConversationCall(String sourcePath, String targetPath, String savePath) {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam params = initParams(initSystemMsg(), initUserMsg(sourcePath, targetPath));
        log.info("多模态对话参数，资源路径：{}，检测路径：{}，保存路径：{}", sourcePath, targetPath, savePath);
        MultiModalConversationResult result = null;
        try {
            result = conv.call(params);
            // todo 删除filePath
        } catch (NoApiKeyException e) {
            log.error("没有找到apiKey");
        } catch (UploadFileException e) {
            log.error("上传文件失败");
        }

        if (result != null) {
            List<Map<String, Object>> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
            Map<String, Object> contentMap = contentList.get(0);
            Object text = contentMap.get("text");

            if (text instanceof String textStr) {
                log.info("MultiModal原始输出结果：{}", textStr);
                try {
                    JSONObject jsonObject = cleanResult(textStr);
                    if (jsonObject == null) return null;

                    boolean res = jsonObject.getBoolean("result");
                    JSONArray bboxArray = jsonObject.getJSONArray("bbox_2d");
                    log.info("解析后的JSON结果：{}", jsonObject);

                    if (res && bboxArray != null && !bboxArray.isEmpty()) {
                        // drawRedBoundingBoxOnImage(savePath, savePath, bboxArray);
                        String shellPath = properties.getScriptPath() + "draw_bbox";
                        log.info("执行画框脚本路径: {}", shellPath);
                        CommandResult commandResult = CmdUti.executeCmd2(new String[]{shellPath, savePath, bboxArray.toJSONString()});
                        log.info("画框脚本执行结果：{}", commandResult.getOutput());
                        if (commandResult.getExitCode() == 0) {
                            log.info("画框成功, 图片保存路径：{}", savePath);
                        } else {
                            log.error("画框失败");
                            return null;
                        }
                        JSONObject resultData = new JSONObject();
                        resultData.put("result", jsonObject.getBooleanValue("result"));
                        resultData.put("count", jsonObject.getIntValue("count"));
                        return resultData;
                    }

                } catch (Exception e) {
                    log.error("解析JSON异常", e);
                }
            }
        }
        return null;
    }

    // 画框
    /*private void drawRedBoundingBoxOnImage(String imagePath, String savePath, JSONArray bboxCoords) {
        Mat originalImage = null;
        try {
            log.info("正在读取图像文件: {}", imagePath);
            originalImage = opencv_imgcodecs.imread(imagePath);

            if (originalImage.empty()) {
                log.error("无法读取图像文件: {}", imagePath);
                return;
            }

            if (bboxCoords != null && !bboxCoords.isEmpty()) {
                for (int i = 0; i < bboxCoords.size(); i++) {
                    JSONObject temp = bboxCoords.getJSONObject(i);
                    JSONArray boxPoints = temp.getJSONArray("box");
                    int x1 = boxPoints.getIntValue(0);
                    int y1 = boxPoints.getIntValue(1);
                    int x2 = boxPoints.getIntValue(2);
                    int y2 = boxPoints.getIntValue(3);

                    Scalar redColor = new Scalar(0, 0, 255, 0); // BGR 格式 + alpha
                    int thickness = 2;

                    opencv_imgproc.rectangle(originalImage,
                            new Point(x1, y1),
                            new Point(x2, y2),
                            redColor,
                            thickness,
                            opencv_imgproc.LINE_8,
                            0);
                }

            }
            initSavePath(savePath);
            opencv_imgcodecs.imwrite(savePath, originalImage);
            log.info("标记后的图像已保存到: {}", savePath);

        } catch (Exception e) {
            log.error("绘制标记框时发生错误", e);
        } finally {
            // 释放 Mat 对象
            if (originalImage != null && !originalImage.empty()) {
                originalImage.release();
            }
        }
    }*/

    // 根据系统参数获取文件路径
    private String getFilePath(String filePath) {
        String filePrefix = "file://";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            filePrefix = "file:///";
        }
        return filePrefix + filePath;
    }

    // 初始化保存路径
    private void initSavePath(String filePath) {
        String parent = FileUtil.getParent(filePath, 1);
        FileUtil.mkdir(parent);
    }

    // 返回结果清洗
    private JSONObject cleanResult(String rawOutput) {
        Pattern pattern = Pattern.compile("```(?:json)?\\s*|```");
        try {
            // 去掉 ```json 或 ``` 标记
            String cleanJson = pattern.matcher(rawOutput).replaceAll("").trim();
            return JSONObject.parseObject(cleanJson);
        } catch (Exception e) {
            log.error("返回结果清洗异常", e);
        }
        return null;
    }

    // 动态获取模型名称，方便切换
    private String getModelName() {
        String modelName = "qwen-vl-plus";
        SysParameter sysParameter = SysParameterUtil.getSysParameter("ModelSetting");
        if (sysParameter != null) {
            String settings = sysParameter.getParamVal();
            JSONObject jsonObject = JSONObject.parseObject(settings);
            modelName = jsonObject.getString("modelName");
        }
        return modelName;
    }

    // 动态获取模型参数，方便切换
    public boolean enableBaseImg() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("ModelSetting");
        if (sysParameter != null) {
            String settings = sysParameter.getParamVal();
            JSONObject jsonObject = JSONObject.parseObject(settings);
            return jsonObject.getBooleanValue("baseImage");
        }
        return false;
    }

    // public static void main(String[] args) {
    //     String imagePath = "D:/ShareFiles/IVA/8.jpeg";
    //     String parent = FileUtil.getParent(imagePath, 1);
    //     String savePath = parent + File.separator +  "Result" + File.separator + FileUtil.getName(imagePath);
    //     String userMsg = "请判断图片是否存在围栏缺失或破损，仅关注肉眼可见、较为明显的区域。";
    //     QianwenModal qianwenModal = new QianwenModal();
    //     JSONObject jsonObject = qianwenModal.MultiModalConversationCall(userMsg, imagePath, savePath);
    //     log.info("返回结果：{}", jsonObject);
    // }
}