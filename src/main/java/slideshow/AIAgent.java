package slideshow;

import dev.langchain4j.model.openai.OpenAiChatModel;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AI代理类，负责处理与AI模型的交互
 * 包括生成演讲稿、PPT内容等功能
 */
public class AIAgent {
    private static final Logger logger = Logger.getLogger(AIAgent.class.getName());
    
    private OpenAiChatModel aiModel;
    
    /**
     * 构造函数
     * @param aiModel AI模型实例
     */
    public AIAgent(OpenAiChatModel aiModel) {
        if (aiModel == null) {
            throw new IllegalArgumentException("AI模型不能为空");
        }
        this.aiModel = aiModel;
        logger.info("AIAgent初始化成功");
    }
    
    /**
     * 根据幻灯片内容生成演讲稿
     * @param slides 幻灯片列表
     * @return 生成的演讲稿文本
     * @throws AIException 当AI调用失败时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public String generateSpeechBySlides(List<Slide> slides) throws AIException, IllegalArgumentException {
        // 参数验证
        if (slides == null) {
            throw new IllegalArgumentException("幻灯片列表不能为空");
        }
        
        if (slides.isEmpty()) {
            throw new IllegalArgumentException("幻灯片列表不能为空");
        }
        
        try {
            logger.info("开始根据幻灯片生成演讲稿，幻灯片数量: " + slides.size());
            
            // 提取幻灯片内容
            String slideContent = extractSlideContent(slides);
            
            if (slideContent.trim().isEmpty()) {
                logger.warning("提取的幻灯片内容为空");
                throw new AIException("无法从幻灯片中提取有效内容");
            }
            
            // 构建AI提示词
            String prompt = buildSpeechPrompt(slideContent);
            
            // 调用AI模型
            String aiResponse = callAIModel(prompt);
            
            // 解析AI响应
            String speech = parseAIResponse(aiResponse);
            
            logger.info("演讲稿生成成功，长度: " + speech.length());
            return speech;
            
        } catch (AIException e) {
            logger.log(Level.SEVERE, "AI调用失败", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "参数验证失败", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成演讲稿时发生未知错误", e);
            throw new AIException("生成演讲稿时发生未知错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取幻灯片内容
     * @param slides 幻灯片列表
     * @return 提取的文本内容
     */
    private String extractSlideContent(List<Slide> slides) {
        StringBuilder content = new StringBuilder();
        
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            content.append("第").append(i + 1).append("页：\n");
            
            // 获取幻灯片中的文本内容
            List<String> textContent = slide.getTextContent();
            
            for (String text : textContent) {
                content.append(text).append("\n");
            }
            content.append("\n");
        }
        
        return content.toString();
    }
    
    /**
     * 构建演讲稿生成的提示词
     * @param slideContent 幻灯片内容
     * @return 构建的提示词
     */
    private String buildSpeechPrompt(String slideContent) {
        return "你是一个专业的演讲助手。请根据以下PPT内容生成一份完整的演讲稿。\n" +
               "要求：\n" +
               "1. 演讲稿要流畅自然，适合口头表达\n" +
               "2. 每页PPT对应一段演讲稿内容\n" +
               "3. 语言要生动有趣，吸引听众注意力\n" +
               "4. 适当添加过渡语句，使各页内容衔接自然\n" +
               "5. 演讲稿长度要适中，每页大约1-2分钟的演讲时间\n" +
               "6. 使用中文输出\n\n" +
               "PPT内容：\n" + slideContent + "\n\n" +
               "请生成演讲稿：";
    }
    
    /**
     * 调用AI模型
     * @param prompt 提示词
     * @return AI响应
     * @throws AIException 当AI调用失败时抛出
     */
    private String callAIModel(String prompt) throws AIException {
        try {
            logger.info("开始调用AI模型");
            
            if (aiModel == null) {
                throw new AIException("AI模型未初始化");
            }
            
            String response = aiModel.chat(prompt);
            
            if (response == null || response.trim().isEmpty()) {
                throw new AIException("AI返回的响应为空");
            }
            
            logger.info("AI模型调用成功，响应长度: " + response.length());
            return response;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "AI模型调用失败", e);
            throw new AIException("AI模型调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析AI响应
     * @param aiResponse AI响应文本
     * @return 解析后的演讲稿
     */
    private String parseAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "无法生成演讲稿，AI响应为空";
        }
        
        // 简单的响应处理，可以根据需要添加更复杂的解析逻辑
        return aiResponse.trim();
    }
    
    /**
     * 自定义AI异常类
     */
    public static class AIException extends Exception {
        public AIException(String message) {
            super(message);
        }
        
        public AIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 