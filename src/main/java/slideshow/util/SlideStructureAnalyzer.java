package slideshow.util;

import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;
import slideshow.elements.ImageElement;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 幻灯片内容结构分析器
 * 负责自动分析PPT结构，生成大纲、重点和逻辑关系图
 */
public class SlideStructureAnalyzer {
    private static final Logger logger = Logger.getLogger(SlideStructureAnalyzer.class.getName());

    /**
     * 幻灯片结构分析结果
     */
    public static class StructureAnalysis {
        private String mainTopic;                    // 主要主题
        private List<String> outline;                // 大纲结构
        private List<String> keyPoints;              // 重点内容
        private Map<String, List<String>> hierarchy; // 层次结构
        private List<String> themes;                 // 主题分类
        private Map<String, Integer> keywordFrequency; // 关键词频率
        private List<String> logicalFlow;            // 逻辑流程
        private int totalSlides;                     // 幻灯片总数
        private int totalElements;                   // 元素总数
        private Map<String, Integer> elementTypes;   // 元素类型统计

        public StructureAnalysis() {
            this.outline = new ArrayList<>();
            this.keyPoints = new ArrayList<>();
            this.hierarchy = new LinkedHashMap<>();
            this.themes = new ArrayList<>();
            this.keywordFrequency = new HashMap<>();
            this.logicalFlow = new ArrayList<>();
            this.elementTypes = new HashMap<>();
        }

        // Getters and Setters
        public String getMainTopic() { return mainTopic; }
        public void setMainTopic(String mainTopic) { this.mainTopic = mainTopic; }
        
        public List<String> getOutline() { return outline; }
        public void setOutline(List<String> outline) { this.outline = outline; }
        
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
        
        public Map<String, List<String>> getHierarchy() { return hierarchy; }
        public void setHierarchy(Map<String, List<String>> hierarchy) { this.hierarchy = hierarchy; }
        
        public List<String> getThemes() { return themes; }
        public void setThemes(List<String> themes) { this.themes = themes; }
        
        public Map<String, Integer> getKeywordFrequency() { return keywordFrequency; }
        public void setKeywordFrequency(Map<String, Integer> keywordFrequency) { this.keywordFrequency = keywordFrequency; }
        
        public List<String> getLogicalFlow() { return logicalFlow; }
        public void setLogicalFlow(List<String> logicalFlow) { this.logicalFlow = logicalFlow; }
        
        public int getTotalSlides() { return totalSlides; }
        public void setTotalSlides(int totalSlides) { this.totalSlides = totalSlides; }
        
        public int getTotalElements() { return totalElements; }
        public void setTotalElements(int totalElements) { this.totalElements = totalElements; }
        
        public Map<String, Integer> getElementTypes() { return elementTypes; }
        public void setElementTypes(Map<String, Integer> elementTypes) { this.elementTypes = elementTypes; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 幻灯片结构分析结果 ===\n");
            sb.append("主要主题: ").append(mainTopic).append("\n");
            sb.append("幻灯片总数: ").append(totalSlides).append("\n");
            sb.append("元素总数: ").append(totalElements).append("\n");
            sb.append("主题分类: ").append(String.join(", ", themes)).append("\n");
            sb.append("\n=== 大纲结构 ===\n");
            for (int i = 0; i < outline.size(); i++) {
                sb.append(i + 1).append(". ").append(outline.get(i)).append("\n");
            }
            sb.append("\n=== 重点内容 ===\n");
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append(i + 1).append(". ").append(keyPoints.get(i)).append("\n");
            }
            sb.append("\n=== 逻辑流程 ===\n");
            for (int i = 0; i < logicalFlow.size(); i++) {
                sb.append(i + 1).append(". ").append(logicalFlow.get(i)).append("\n");
            }
            sb.append("\n=== 关键词频率 ===\n");
            keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
            return sb.toString();
        }
    }

    /**
     * 分析幻灯片结构
     * 
     * @param slides 幻灯片列表
     * @return 结构分析结果
     */
    public static StructureAnalysis analyzeStructure(List<Slide> slides) {
        logger.info("开始分析幻灯片结构，幻灯片数量: " + slides.size());
        
        StructureAnalysis analysis = new StructureAnalysis();
        analysis.setTotalSlides(slides.size());
        
        try {
            // 1. 提取所有文本内容
            String allContent = extractAllContent(slides);
            
            // 2. 分析元素类型
            analyzeElementTypes(slides, analysis);
            
            // 3. 提取关键词和频率
            extractKeywords(allContent, analysis);
            
            // 4. 生成大纲结构
            generateOutline(slides, analysis);
            
            // 5. 提取重点内容
            extractKeyPoints(slides, analysis);
            
            // 6. 分析层次结构
            analyzeHierarchy(slides, analysis);
            
            // 7. 识别主题分类
            identifyThemes(analysis);
            
            // 8. 生成逻辑流程
            generateLogicalFlow(slides, analysis);
            
            // 9. 确定主要主题
            determineMainTopic(analysis);
            
            logger.info("幻灯片结构分析完成");
            return analysis;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "幻灯片结构分析失败", e);
            throw new RuntimeException("幻灯片结构分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取所有文本内容
     */
    private static String extractAllContent(List<Slide> slides) {
        StringBuilder content = new StringBuilder();
        
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            content.append("第").append(i + 1).append("页：\n");
            
            List<String> textContent = slide.getTextContent();
            for (String text : textContent) {
                content.append(text).append("\n");
            }
            content.append("\n");
        }
        
        return content.toString();
    }

    /**
     * 分析元素类型
     */
    private static void analyzeElementTypes(List<Slide> slides, StructureAnalysis analysis) {
        Map<String, Integer> elementTypes = new HashMap<>();
        int totalElements = 0;
        
        for (Slide slide : slides) {
            for (SlideElement element : slide.getElements()) {
                String elementType = element.getClass().getSimpleName();
                elementTypes.put(elementType, elementTypes.getOrDefault(elementType, 0) + 1);
                totalElements++;
            }
        }
        
        analysis.setElementTypes(elementTypes);
        analysis.setTotalElements(totalElements);
    }

    /**
     * 提取关键词和频率
     */
    private static void extractKeywords(String content, StructureAnalysis analysis) {
        Map<String, Integer> keywordFrequency = new HashMap<>();
        
        // 分词并统计频率
        String[] words = content.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            String cleanWord = word.trim().toLowerCase();
            if (cleanWord.length() > 1 && !isStopWord(cleanWord)) {
                keywordFrequency.put(cleanWord, keywordFrequency.getOrDefault(cleanWord, 0) + 1);
            }
        }
        
        analysis.setKeywordFrequency(keywordFrequency);
    }

    /**
     * 判断是否为停用词
     */
    private static boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"
        ));
        return stopWords.contains(word);
    }

    /**
     * 生成大纲结构
     */
    private static void generateOutline(List<Slide> slides, StructureAnalysis analysis) {
        List<String> outline = new ArrayList<>();
        
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            String slideTitle = extractSlideTitle(slide);
            if (slideTitle != null && !slideTitle.isEmpty()) {
                outline.add(slideTitle);
            } else {
                outline.add("第" + (i + 1) + "页");
            }
        }
        
        analysis.setOutline(outline);
    }

    /**
     * 提取幻灯片标题
     */
    private static String extractSlideTitle(Slide slide) {
        for (SlideElement element : slide.getElements()) {
            if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                String text = textElement.getText();
                if (text != null && text.length() > 0 && text.length() < 50) {
                    // 假设第一个较短的文本元素是标题
                    return text;
                }
            }
        }
        return null;
    }

    /**
     * 提取重点内容
     */
    private static void extractKeyPoints(List<Slide> slides, StructureAnalysis analysis) {
        List<String> keyPoints = new ArrayList<>();
        
        for (Slide slide : slides) {
            for (SlideElement element : slide.getElements()) {
                if (element instanceof TextElement) {
                    TextElement textElement = (TextElement) element;
                    String text = textElement.getText();
                    if (text != null && text.length() > 5 && text.length() < 200) {
                        // 提取中等长度的文本作为重点
                        keyPoints.add(text);
                    }
                }
            }
        }
        
        // 去重并限制数量
        Set<String> uniquePoints = new LinkedHashSet<>(keyPoints);
        analysis.setKeyPoints(new ArrayList<>(uniquePoints).subList(0, Math.min(uniquePoints.size(), 20)));
    }

    /**
     * 分析层次结构
     */
    private static void analyzeHierarchy(List<Slide> slides, StructureAnalysis analysis) {
        Map<String, List<String>> hierarchy = new LinkedHashMap<>();
        
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            String slideTitle = extractSlideTitle(slide) != null ? extractSlideTitle(slide) : "第" + (i + 1) + "页";
            List<String> slideContent = new ArrayList<>();
            
            for (SlideElement element : slide.getElements()) {
                if (element instanceof TextElement) {
                    TextElement textElement = (TextElement) element;
                    String text = textElement.getText();
                    if (text != null && !text.equals(slideTitle)) {
                        slideContent.add(text);
                    }
                }
            }
            
            hierarchy.put(slideTitle, slideContent);
        }
        
        analysis.setHierarchy(hierarchy);
    }

    /**
     * 识别主题分类
     */
    private static void identifyThemes(StructureAnalysis analysis) {
        List<String> themes = new ArrayList<>();
        Map<String, Integer> keywordFreq = analysis.getKeywordFrequency();
        
        // 基于关键词频率识别主题
        if (keywordFreq.containsKey("技术") || keywordFreq.containsKey("科技")) {
            themes.add("技术科技");
        }
        if (keywordFreq.containsKey("管理") || keywordFreq.containsKey("领导")) {
            themes.add("管理领导");
        }
        if (keywordFreq.containsKey("市场") || keywordFreq.containsKey("营销")) {
            themes.add("市场营销");
        }
        if (keywordFreq.containsKey("教育") || keywordFreq.containsKey("学习")) {
            themes.add("教育培训");
        }
        if (keywordFreq.containsKey("产品") || keywordFreq.containsKey("服务")) {
            themes.add("产品服务");
        }
        
        if (themes.isEmpty()) {
            themes.add("通用主题");
        }
        
        analysis.setThemes(themes);
    }

    /**
     * 生成逻辑流程
     */
    private static void generateLogicalFlow(List<Slide> slides, StructureAnalysis analysis) {
        List<String> logicalFlow = new ArrayList<>();
        
        if (slides.size() >= 3) {
            logicalFlow.add("开场介绍 - 建立背景和目的");
            logicalFlow.add("主要内容 - 核心信息和要点");
            logicalFlow.add("总结结论 - 回顾要点和行动建议");
        } else if (slides.size() >= 2) {
            logicalFlow.add("问题提出 - 明确要解决的问题");
            logicalFlow.add("解决方案 - 提供具体的解决方案");
        } else {
            logicalFlow.add("信息展示 - 展示关键信息");
        }
        
        analysis.setLogicalFlow(logicalFlow);
    }

    /**
     * 确定主要主题
     */
    private static void determineMainTopic(StructureAnalysis analysis) {
        // 基于关键词频率确定主要主题
        Map<String, Integer> keywordFreq = analysis.getKeywordFrequency();
        String mainTopic = keywordFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("未识别主题");
        
        analysis.setMainTopic(mainTopic);
    }

    /**
     * 生成结构分析报告
     * 
     * @param analysis 结构分析结果
     * @return 格式化的报告文本
     */
    public static String generateAnalysisReport(StructureAnalysis analysis) {
        StringBuilder report = new StringBuilder();
        report.append("=== 幻灯片结构分析报告 ===\n\n");
        
        // 基本信息
        report.append("【基本信息】\n");
        report.append("主要主题: ").append(analysis.getMainTopic()).append("\n");
        report.append("幻灯片数量: ").append(analysis.getTotalSlides()).append("\n");
        report.append("元素总数: ").append(analysis.getTotalElements()).append("\n");
        report.append("主题分类: ").append(String.join(", ", analysis.getThemes())).append("\n\n");
        
        // 大纲结构
        report.append("【大纲结构】\n");
        for (int i = 0; i < analysis.getOutline().size(); i++) {
            report.append(i + 1).append(". ").append(analysis.getOutline().get(i)).append("\n");
        }
        report.append("\n");
        
        // 重点内容
        report.append("【重点内容】\n");
        for (int i = 0; i < analysis.getKeyPoints().size(); i++) {
            report.append(i + 1).append(". ").append(analysis.getKeyPoints().get(i)).append("\n");
        }
        report.append("\n");
        
        // 逻辑流程
        report.append("【逻辑流程】\n");
        for (int i = 0; i < analysis.getLogicalFlow().size(); i++) {
            report.append(i + 1).append(". ").append(analysis.getLogicalFlow().get(i)).append("\n");
        }
        report.append("\n");
        
        // 关键词统计
        report.append("【关键词统计】\n");
        analysis.getKeywordFrequency().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> report.append(entry.getKey()).append(": ").append(entry.getValue()).append("次\n"));
        report.append("\n");
        
        // 元素类型统计
        report.append("【元素类型统计】\n");
        analysis.getElementTypes().forEach((type, count) -> 
            report.append(type).append(": ").append(count).append("个\n"));
        
        return report.toString();
    }

    /**
     * 生成逻辑关系图数据
     * 
     * @param analysis 结构分析结果
     * @return 逻辑关系图的JSON格式数据
     */
    public static String generateLogicGraphData(StructureAnalysis analysis) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"nodes\": [\n");
        
        // 添加主题节点
        json.append("    {\"id\": \"main\", \"label\": \"").append(analysis.getMainTopic()).append("\", \"type\": \"main\"},\n");
        
        // 添加大纲节点
        for (int i = 0; i < analysis.getOutline().size(); i++) {
            json.append("    {\"id\": \"outline_").append(i).append("\", \"label\": \"").append(analysis.getOutline().get(i)).append("\", \"type\": \"outline\"},\n");
        }
        
        // 添加重点节点
        for (int i = 0; i < analysis.getKeyPoints().size(); i++) {
            json.append("    {\"id\": \"point_").append(i).append("\", \"label\": \"").append(analysis.getKeyPoints().get(i)).append("\", \"type\": \"keypoint\"},\n");
        }
        
        json.append("  ],\n");
        json.append("  \"edges\": [\n");
        
        // 添加主题到大纲的连接
        for (int i = 0; i < analysis.getOutline().size(); i++) {
            json.append("    {\"source\": \"main\", \"target\": \"outline_").append(i).append("\", \"type\": \"hierarchy\"},\n");
        }
        
        // 添加大纲到重点的连接
        for (int i = 0; i < Math.min(analysis.getOutline().size(), analysis.getKeyPoints().size()); i++) {
            json.append("    {\"source\": \"outline_").append(i).append("\", \"target\": \"point_").append(i).append("\", \"type\": \"detail\"},\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
}