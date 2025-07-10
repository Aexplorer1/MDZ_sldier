package slideshow;

import dev.langchain4j.model.openai.OpenAiChatModel;
import slideshow.model.Slide;
import slideshow.model.PromptTemplate;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.util.TemplateManager;
import slideshow.util.IntelligentLayoutEngine;
import slideshow.util.MultilingualSupport;
import slideshow.util.MultilingualSupport.SupportedLanguage;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 增强的AI代理类
 * 集成智能排版优化和多语言幻灯片生成功能
 */
public class AIEnhancedAgent extends AIAgent {
    private static final Logger logger = Logger.getLogger(AIEnhancedAgent.class.getName());
    
    public AIEnhancedAgent(OpenAiChatModel aiModel) {
        super(aiModel);
    }
    
    /**
     * 生成多语言PPT内容
     * 
     * @param originalContent 原始内容
     * @param targetLanguage 目标语言
     * @return 翻译后的内容
     */
    public String generateMultilingualContent(String originalContent, SupportedLanguage targetLanguage) {
        try {
            logger.info("开始生成多语言内容，目标语言: " + targetLanguage.getDisplayName());
            
            String translatedContent = MultilingualSupport.generateMultilingualContent(originalContent, targetLanguage);
            
            logger.info("多语言内容生成完成");
            return translatedContent;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成多语言内容失败", e);
            return originalContent;
        }
    }
    
    /**
     * 生成多语言PPT命令
     * 
     * @param originalCommands 原始PPT命令
     * @param targetLanguage 目标语言
     * @return 翻译后的PPT命令
     */
    public String generateMultilingualPPTCommands(String originalCommands, SupportedLanguage targetLanguage) {
        try {
            logger.info("开始生成多语言PPT命令，目标语言: " + targetLanguage.getDisplayName());
            
            String translatedCommands = MultilingualSupport.generateMultilingualPPTCommands(originalCommands, targetLanguage);
            
            logger.info("多语言PPT命令生成完成");
            return translatedCommands;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成多语言PPT命令失败", e);
            return originalCommands;
        }
    }
    
    /**
     * 智能优化幻灯片布局
     * 
     * @param slide 要优化的幻灯片
     * @param slideWidth 幻灯片宽度
     * @param slideHeight 幻灯片高度
     * @param layoutType 布局类型
     */
    public void optimizeSlideLayout(Slide slide, double slideWidth, double slideHeight, 
                                  IntelligentLayoutEngine.LayoutType layoutType) {
        try {
            logger.info("开始智能优化幻灯片布局，布局类型: " + layoutType.getDisplayName());
            
            IntelligentLayoutEngine.optimizeLayout(slide, slideWidth, slideHeight, layoutType);
            
            logger.info("幻灯片布局优化完成");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "智能优化幻灯片布局失败", e);
        }
    }
    
    /**
     * 自动调整文本大小以适应容器
     * 
     * @param textElement 文本元素
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     */
    public void autoAdjustTextSize(TextElement textElement, double maxWidth, double maxHeight) {
        try {
            logger.info("开始自动调整文本大小");
            
            IntelligentLayoutEngine.autoAdjustTextSize(textElement, maxWidth, maxHeight);
            
            logger.info("文本大小自动调整完成");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "自动调整文本大小失败", e);
        }
    }
    
    /**
     * 响应式调整布局
     * 
     * @param slide 幻灯片
     * @param newWidth 新宽度
     * @param newHeight 新高度
     */
    public void responsiveAdjustLayout(Slide slide, double newWidth, double newHeight) {
        try {
            logger.info("开始响应式调整布局，新尺寸: " + newWidth + "x" + newHeight);
            
            IntelligentLayoutEngine.responsiveAdjust(slide, newWidth, newHeight);
            
            logger.info("响应式调整完成");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "响应式调整布局失败", e);
        }
    }
    
    /**
     * 生成多语言演讲稿
     * 
     * @param slides 幻灯片列表
     * @param targetLanguage 目标语言
     * @return 翻译后的演讲稿
     */
    public String generateMultilingualSpeech(List<Slide> slides, SupportedLanguage targetLanguage) {
        try {
            logger.info("开始生成多语言演讲稿，目标语言: " + targetLanguage.getDisplayName());
            
            // 先生成中文演讲稿
            String originalSpeech = generateSpeechBySlides(slides);
            
            // 翻译演讲稿
            String translatedSpeech = MultilingualSupport.generateMultilingualContent(originalSpeech, targetLanguage);
            
            logger.info("多语言演讲稿生成完成");
            return translatedSpeech;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成多语言演讲稿失败", e);
            return "无法生成多语言演讲稿";
        }
    }
    
    /**
     * 智能生成多语言PPT
     * 
     * @param topic 主题
     * @param targetLanguage 目标语言
     * @param layoutType 布局类型
     * @return 生成的PPT命令
     */
    public String generateIntelligentMultilingualPPT(String topic, SupportedLanguage targetLanguage, 
                                                   IntelligentLayoutEngine.LayoutType layoutType) {
        try {
            logger.info("开始智能生成多语言PPT，主题: " + topic + ", 目标语言: " + targetLanguage.getDisplayName());
            
            // 构建多语言提示词
            String prompt = buildMultilingualPrompt(topic, targetLanguage);
            
            // 使用公共方法调用AI模型
            String aiResponse = askAI(prompt);
            
            // 解析AI响应
            String pptCommands = parsePPTCommands(aiResponse);
            
            // 应用智能布局优化
            List<Slide> slides = parseAndCreateSlides(pptCommands, 800.0);
            for (Slide slide : slides) {
                optimizeSlideLayout(slide, 800.0, 600.0, layoutType);
            }
            
            logger.info("智能多语言PPT生成完成");
            return pptCommands;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "智能生成多语言PPT失败", e);
            return "无法生成多语言PPT";
        }
    }
    
    /**
     * 构建多语言提示词
     */
    private String buildMultilingualPrompt(String topic, SupportedLanguage targetLanguage) {
        String languageName = targetLanguage.getDisplayName();
        
        return String.format(
            "你是一个专业的PPT助手，请为以下主题生成一个完整的PPT大纲。\n" +
            "要求：\n" +
            "1. 使用%s语言生成内容\n" +
            "2. 内容要专业、准确、易懂\n" +
            "3. 结构清晰，逻辑连贯\n" +
            "4. 包含标题、副标题、要点等元素\n" +
            "5. 适合目标语言的文化背景\n\n" +
            "主题：%s\n\n" +
            "请严格按照以下格式输出：\n" +
            "---PPT命令---\n" +
            "Page 1:\n" +
            "Title: [页面标题]\n" +
            "Subtitle: [页面副标题]\n" +
            "Bullet: [项目符号内容]\n" +
            "Draw: [绘图描述]\n" +
            "Page 2:\n" +
            "Title: [页面标题]\n" +
            "Subtitle: [页面副标题]\n" +
            "Bullet: [项目符号内容]\n" +
            "Draw: [绘图描述]\n" +
            "（继续更多页面...）\n\n" +
            "请生成PPT大纲：",
            languageName, topic
        );
    }
    
    /**
     * 解析PPT命令
     */
    private String parsePPTCommands(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "";
        }
        
        // 查找PPT命令部分
        if (aiResponse.contains("---PPT命令---")) {
            String[] parts = aiResponse.split("---PPT命令---");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        // 如果没有找到分隔符，尝试智能解析
        return aiResponse.trim();
    }
    
    /**
     * 解析并创建幻灯片
     */
    private List<Slide> parseAndCreateSlides(String pptCommands, double slideWidth) {
        // 这里应该调用SlideParser来解析PPT命令
        // 为了简化，这里返回空列表
        return new ArrayList<>();
    }
    
    /**
     * 获取支持的语言列表
     */
    public List<SupportedLanguage> getSupportedLanguages() {
        return MultilingualSupport.getSupportedLanguages();
    }
    
    /**
     * 切换语言
     */
    public void switchLanguage(SupportedLanguage language) {
        MultilingualSupport.switchLanguage(language);
    }
    
    /**
     * 获取当前语言
     */
    public SupportedLanguage getCurrentLanguage() {
        return MultilingualSupport.getCurrentLanguage();
    }
    
    /**
     * 获取本地化字符串
     */
    public String getLocalizedString(String key) {
        return MultilingualSupport.getString(key);
    }
    
    /**
     * 获取带参数的本地化字符串
     */
    public String getLocalizedString(String key, Object... args) {
        return MultilingualSupport.getString(key, args);
    }
} 