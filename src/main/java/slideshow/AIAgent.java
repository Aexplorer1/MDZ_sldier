package slideshow;

import dev.langchain4j.model.openai.OpenAiChatModel;
import slideshow.model.Slide;
import slideshow.model.PromptTemplate;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.util.TemplateManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AI代理类，负责处理与AI模型的交互
 * 包括生成演讲稿、PPT内容、关键词提取等功能
 */
public class AIAgent {
    private static final Logger logger = Logger.getLogger(AIAgent.class.getName());

    private OpenAiChatModel aiModel;
    private TemplateManager templateManager;

    /**
     * 幻灯片分析结果类
     */
    public static class SlideAnalysis {
        private List<String> keywords;
        private String mainTopic;
        private String summary;
        private Map<String, Integer> keywordFrequency;
        private List<String> themes;
        private int totalSlides;
        private int totalWords;

        public SlideAnalysis() {
            this.keywords = new ArrayList<>();
            this.keywordFrequency = new HashMap<>();
            this.themes = new ArrayList<>();
        }

        // Getters and Setters
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        
        public String getMainTopic() { return mainTopic; }
        public void setMainTopic(String mainTopic) { this.mainTopic = mainTopic; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public Map<String, Integer> getKeywordFrequency() { return keywordFrequency; }
        public void setKeywordFrequency(Map<String, Integer> keywordFrequency) { this.keywordFrequency = keywordFrequency; }
        
        public List<String> getThemes() { return themes; }
        public void setThemes(List<String> themes) { this.themes = themes; }
        
        public int getTotalSlides() { return totalSlides; }
        public void setTotalSlides(int totalSlides) { this.totalSlides = totalSlides; }
        
        public int getTotalWords() { return totalWords; }
        public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("幻灯片分析结果:\n");
            sb.append("主要主题: ").append(mainTopic).append("\n");
            sb.append("关键词: ").append(String.join(", ", keywords)).append("\n");
            sb.append("主题数量: ").append(themes.size()).append("\n");
            sb.append("幻灯片总数: ").append(totalSlides).append("\n");
            sb.append("总字数: ").append(totalWords).append("\n");
            sb.append("摘要: ").append(summary).append("\n");
            return sb.toString();
        }
    }

    /**
     * 构造函数
     * 
     * @param aiModel AI模型实例
     */
    public AIAgent(OpenAiChatModel aiModel) {
        if (aiModel == null) {
            throw new IllegalArgumentException("AI模型不能为空");
        }
        this.aiModel = aiModel;
        this.templateManager = new TemplateManager();
        logger.info("AIAgent初始化成功");
    }

    /**
     * 根据幻灯片内容生成演讲稿
     * 
     * @param slides 幻灯片列表
     * @return 生成的演讲稿文本
     * @throws AIException              当AI调用失败时抛出
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
     * 
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
     * 
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
     * 
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
     * 
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
     * 根据主题生成演讲稿结构
     * 
     * @param topic    演讲主题
     * @param duration 演讲时长（分钟）
     * @param audience 目标听众
     * @return 生成的演讲稿结构
     * @throws AIException              当AI调用失败时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public String generateSlidesByTopic(String topic, int duration, String audience)
            throws AIException, IllegalArgumentException {
        // 参数验证
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("演讲主题不能为空");
        }

        if (duration <= 0 || duration > 120) {
            throw new IllegalArgumentException("演讲时长必须在1-120分钟之间");
        }

        if (audience == null || audience.trim().isEmpty()) {
            audience = "一般听众";
        }

        try {
            logger.info("开始生成演讲稿结构，主题: " + topic + ", 时长: " + duration + "分钟");

            // 构建提示词
            String prompt = String.format(
                    "请为以下演讲设计一个结构化的演讲稿大纲：\n" +
                            "主题：%s\n" +
                            "时长：%d分钟\n" +
                            "听众：%s\n\n" +
                            "要求：\n" +
                            "1. 包含开场白、主要内容、结尾\n" +
                            "2. 每个部分标明时长\n" +
                            "3. 主要内容分层次，逻辑清晰\n" +
                            "4. 适合目标听众\n" +
                            "5. 使用中文输出\n\n" +
                            "请生成演讲稿结构：",
                    topic, duration, audience);

            // 调用AI模型
            String response = callAIModel(prompt);

            logger.info("演讲稿结构生成成功");
            return response.trim();

        } catch (AIException e) {
            logger.log(Level.SEVERE, "AI调用失败", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "参数验证失败", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成演讲稿结构时发生未知错误", e);
            throw new AIException("生成演讲稿结构时发生未知错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用模板生成内容
     * 
     * @param templateId 模板ID
     * @param args       模板参数
     * @return 生成的内容
     * @throws AIException              当AI调用失败时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public String generateWithTemplate(String templateId, Object... args) throws AIException, IllegalArgumentException {
        try {
            Optional<PromptTemplate> template = templateManager.getTemplate(templateId);
            if (!template.isPresent()) {
                throw new IllegalArgumentException("模板不存在: " + templateId);
            }

            PromptTemplate promptTemplate = template.get();

            // 格式化模板内容
            String formattedPrompt = promptTemplate.formatContent(args);

            // 调用AI模型
            String response = callAIModel(formattedPrompt);

            // 记录模板使用次数
            templateManager.useTemplate(templateId);

            logger.info("使用模板生成内容成功: " + promptTemplate.getName());
            return response.trim();

        } catch (AIException e) {
            logger.log(Level.SEVERE, "使用模板生成内容失败", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "模板参数验证失败", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "使用模板生成内容时发生未知错误", e);
            throw new AIException("使用模板生成内容时发生未知错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用模板名称生成内容
     * 
     * @param templateName 模板名称
     * @param args         模板参数
     * @return 生成的内容
     * @throws AIException              当AI调用失败时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public String generateWithTemplateByName(String templateName, Object... args)
            throws AIException, IllegalArgumentException {
        try {
            Optional<PromptTemplate> template = templateManager.getTemplateByName(templateName);
            if (!template.isPresent()) {
                throw new IllegalArgumentException("模板不存在: " + templateName);
            }

            return generateWithTemplate(template.get().getId(), args);

        } catch (AIException e) {
            logger.log(Level.SEVERE, "使用模板名称生成内容失败", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "模板名称验证失败", e);
            throw e;
        }
    }

    /**
     * 获取模板管理器
     * 
     * @return 模板管理器实例
     */
    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    /**
     * 解析幻灯片内容，提取关键词和分析主题
     * 
     * @param slides 幻灯片列表
     * @return 幻灯片分析结果
     * @throws AIException              当AI调用失败时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public SlideAnalysis parseSlides(List<Slide> slides) throws AIException, IllegalArgumentException {
        // 参数验证
        if (slides == null) {
            throw new IllegalArgumentException("幻灯片列表不能为空");
        }

        if (slides.isEmpty()) {
            throw new IllegalArgumentException("幻灯片列表不能为空");
        }

        try {
            logger.info("开始解析幻灯片内容，幻灯片数量: " + slides.size());

            // 提取幻灯片内容
            String slideContent = extractSlideContent(slides);

            if (slideContent.trim().isEmpty()) {
                logger.warning("提取的幻灯片内容为空");
                throw new AIException("无法从幻灯片中提取有效内容");
            }

            // 创建分析结果对象
            SlideAnalysis analysis = new SlideAnalysis();
            analysis.setTotalSlides(slides.size());
            analysis.setTotalWords(countWords(slideContent));

            // 使用AI进行智能分析
            performAIAnalysis(slideContent, analysis);

            // 进行本地关键词提取
            performLocalKeywordExtraction(slideContent, analysis);

            // 合并和去重关键词
            mergeAndDeduplicateKeywords(analysis);

            logger.info("幻灯片解析完成，提取到 " + analysis.getKeywords().size() + " 个关键词");
            return analysis;

        } catch (AIException e) {
            logger.log(Level.SEVERE, "AI调用失败", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "参数验证失败", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "解析幻灯片时发生未知错误", e);
            throw new AIException("解析幻灯片时发生未知错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用AI进行智能分析
     * 
     * @param slideContent 幻灯片内容
     * @param analysis 分析结果对象
     * @throws AIException 当AI调用失败时抛出
     */
    private void performAIAnalysis(String slideContent, SlideAnalysis analysis) throws AIException {
        try {
            String prompt = buildAnalysisPrompt(slideContent);
            String aiResponse = callAIModel(prompt);
            parseAIAnalysisResponse(aiResponse, analysis);
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI分析失败，将使用本地分析", e);
            // 如果AI分析失败，使用本地分析作为备选
            performLocalAnalysis(slideContent, analysis);
        }
    }

    /**
     * 构建AI分析提示词
     * 
     * @param slideContent 幻灯片内容
     * @return 构建的提示词
     */
    private String buildAnalysisPrompt(String slideContent) {
        return "请分析以下PPT内容，并按照以下格式返回分析结果：\n" +
                "---分析结果---\n" +
                "主要主题：[用一句话概括PPT的主要主题]\n" +
                "关键词：[提取5-10个最重要的关键词，用逗号分隔]\n" +
                "主题分类：[列出2-4个主要主题分类]\n" +
                "内容摘要：[用100字左右概括PPT的主要内容]\n" +
                "---分析结果---\n\n" +
                "PPT内容：\n" + slideContent + "\n\n" +
                "请严格按照上述格式返回分析结果：";
    }

    /**
     * 解析AI分析响应
     * 
     * @param aiResponse AI响应文本
     * @param analysis 分析结果对象
     */
    private void parseAIAnalysisResponse(String aiResponse, SlideAnalysis analysis) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return;
        }

        String content = aiResponse.trim();
        
        // 提取主要主题
        Pattern topicPattern = Pattern.compile("主要主题[：:](.+)");
        Matcher topicMatcher = topicPattern.matcher(content);
        if (topicMatcher.find()) {
            analysis.setMainTopic(topicMatcher.group(1).trim());
        }

        // 提取关键词
        Pattern keywordPattern = Pattern.compile("关键词[：:](.+)");
        Matcher keywordMatcher = keywordPattern.matcher(content);
        if (keywordMatcher.find()) {
            String keywordsStr = keywordMatcher.group(1).trim();
            String[] keywords = keywordsStr.split("[,，]");
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty()) {
                    analysis.getKeywords().add(trimmed);
                }
            }
        }

        // 提取主题分类
        Pattern themePattern = Pattern.compile("主题分类[：:](.+)");
        Matcher themeMatcher = themePattern.matcher(content);
        if (themeMatcher.find()) {
            String themesStr = themeMatcher.group(1).trim();
            String[] themes = themesStr.split("[,，]");
            for (String theme : themes) {
                String trimmed = theme.trim();
                if (!trimmed.isEmpty()) {
                    analysis.getThemes().add(trimmed);
                }
            }
        }

        // 提取内容摘要
        Pattern summaryPattern = Pattern.compile("内容摘要[：:](.+)");
        Matcher summaryMatcher = summaryPattern.matcher(content);
        if (summaryMatcher.find()) {
            analysis.setSummary(summaryMatcher.group(1).trim());
        }
    }

    /**
     * 进行本地关键词提取
     * 
     * @param slideContent 幻灯片内容
     * @param analysis 分析结果对象
     */
    private void performLocalKeywordExtraction(String slideContent, SlideAnalysis analysis) {
        // 停用词列表
        Set<String> stopWords = new HashSet<>();
        stopWords.addAll(List.of("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"));

        // 分词并统计频率
        String[] words = slideContent.split("[\\s\\p{Punct}]+");
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.length() > 1 && !stopWords.contains(trimmed)) {
                wordFrequency.put(trimmed, wordFrequency.getOrDefault(trimmed, 0) + 1);
            }
        }

        // 按频率排序，取前10个作为关键词
        wordFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .forEach(entry -> {
                    analysis.getKeywords().add(entry.getKey());
                    analysis.getKeywordFrequency().put(entry.getKey(), entry.getValue());
                });
    }

    /**
     * 进行本地分析（AI分析失败时的备选方案）
     * 
     * @param slideContent 幻灯片内容
     * @param analysis 分析结果对象
     */
    private void performLocalAnalysis(String slideContent, SlideAnalysis analysis) {
        // 简单的本地分析逻辑
        String[] sentences = slideContent.split("[。！？]");
        if (sentences.length > 0) {
            analysis.setMainTopic(sentences[0].substring(0, Math.min(50, sentences[0].length())));
        }

        if (analysis.getSummary().isEmpty()) {
            analysis.setSummary("基于幻灯片内容生成的摘要，包含主要信息和关键要点。");
        }
    }

    /**
     * 合并和去重关键词
     * 
     * @param analysis 分析结果对象
     */
    private void mergeAndDeduplicateKeywords(SlideAnalysis analysis) {
        Set<String> uniqueKeywords = new HashSet<>(analysis.getKeywords());
        analysis.setKeywords(new ArrayList<>(uniqueKeywords));
    }

    /**
     * 统计文本中的字数
     * 
     * @param text 文本内容
     * @return 字数统计
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
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