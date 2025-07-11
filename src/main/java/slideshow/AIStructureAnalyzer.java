package slideshow;

import slideshow.util.SlideStructureAnalyzer;
import slideshow.util.SlideStructureAnalyzer.StructureAnalysis;
import slideshow.model.Slide;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AI增强的幻灯片结构分析器
 * 结合AI能力进行更智能的幻灯片结构分析
 */
public class AIStructureAnalyzer {
    private static final Logger logger = Logger.getLogger(AIStructureAnalyzer.class.getName());
    
    private OpenAiChatModel aiModel;
    private SlideStructureAnalyzer structureAnalyzer;

    /**
     * 构造函数
     * 
     * @param aiModel AI模型实例
     */
    public AIStructureAnalyzer(OpenAiChatModel aiModel) {
        this.aiModel = aiModel;
        this.structureAnalyzer = new SlideStructureAnalyzer();
    }

    /**
     * AI增强的幻灯片结构分析
     * 
     * @param slides 幻灯片列表
     * @return 增强的结构分析结果
     */
    public StructureAnalysis analyzeWithAI(List<Slide> slides) {
        logger.info("开始AI增强的幻灯片结构分析");
        
        try {
            // 1. 基础结构分析
            StructureAnalysis baseAnalysis = SlideStructureAnalyzer.analyzeStructure(slides);
            
            // 2. AI增强分析
            StructureAnalysis enhancedAnalysis = enhanceWithAI(baseAnalysis, slides);
            
            logger.info("AI增强分析完成");
            return enhancedAnalysis;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "AI增强分析失败", e);
            // 如果AI分析失败，返回基础分析结果
            return SlideStructureAnalyzer.analyzeStructure(slides);
        }
    }

    /**
     * 使用AI增强分析结果
     */
    private StructureAnalysis enhanceWithAI(StructureAnalysis baseAnalysis, List<Slide> slides) {
        try {
            // 构建AI提示词
            String prompt = buildEnhancementPrompt(baseAnalysis, slides);
            
            // 调用AI模型
            String aiResponse = aiModel.chat(prompt);
            
            // 解析AI响应并更新分析结果
            parseAIEnhancement(aiResponse, baseAnalysis);
            
            return baseAnalysis;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI增强失败，使用基础分析结果", e);
            return baseAnalysis;
        }
    }

    /**
     * 构建AI增强提示词
     */
    private String buildEnhancementPrompt(StructureAnalysis analysis, List<Slide> slides) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对以下PPT内容进行深度结构分析，并提供改进建议：\n\n");
        
        prompt.append("当前分析结果：\n");
        prompt.append("主要主题: ").append(analysis.getMainTopic()).append("\n");
        prompt.append("幻灯片数量: ").append(analysis.getTotalSlides()).append("\n");
        prompt.append("主题分类: ").append(String.join(", ", analysis.getThemes())).append("\n\n");
        
        prompt.append("大纲结构：\n");
        for (int i = 0; i < analysis.getOutline().size(); i++) {
            prompt.append(i + 1).append(". ").append(analysis.getOutline().get(i)).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("重点内容：\n");
        for (int i = 0; i < analysis.getKeyPoints().size(); i++) {
            prompt.append(i + 1).append(". ").append(analysis.getKeyPoints().get(i)).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("请按照以下格式返回增强分析结果：\n");
        prompt.append("---增强分析---\n");
        prompt.append("改进主题: [更准确的主题描述]\n");
        prompt.append("逻辑优化: [逻辑流程优化建议]\n");
        prompt.append("内容补充: [需要补充的内容建议]\n");
        prompt.append("结构建议: [结构改进建议]\n");
        prompt.append("重点突出: [重点内容突出建议]\n");
        prompt.append("---增强分析---\n");
        
        return prompt.toString();
    }

    /**
     * 解析AI增强响应
     */
    private void parseAIEnhancement(String aiResponse, StructureAnalysis analysis) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return;
        }

        String content = aiResponse.trim();
        
        // 提取改进主题
        if (content.contains("改进主题:")) {
            String improvedTopic = extractField(content, "改进主题:");
            if (improvedTopic != null && !improvedTopic.isEmpty()) {
                analysis.setMainTopic(improvedTopic);
            }
        }
        
        // 提取逻辑优化建议
        if (content.contains("逻辑优化:")) {
            String logicOptimization = extractField(content, "逻辑优化:");
            if (logicOptimization != null && !logicOptimization.isEmpty()) {
                analysis.getLogicalFlow().add("AI建议: " + logicOptimization);
            }
        }
        
        // 提取内容补充建议
        if (content.contains("内容补充:")) {
            String contentSuggestion = extractField(content, "内容补充:");
            if (contentSuggestion != null && !contentSuggestion.isEmpty()) {
                analysis.getKeyPoints().add("建议补充: " + contentSuggestion);
            }
        }
    }

    /**
     * 提取字段值
     */
    private String extractField(String content, String fieldName) {
        int startIndex = content.indexOf(fieldName);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += fieldName.length();
        int endIndex = content.indexOf("\n", startIndex);
        if (endIndex == -1) {
            endIndex = content.length();
        }
        
        return content.substring(startIndex, endIndex).trim();
    }

    /**
     * 生成智能大纲建议
     * 
     * @param slides 幻灯片列表
     * @return 智能大纲建议
     */
    public String generateSmartOutline(List<Slide> slides) {
        try {
            StructureAnalysis analysis = analyzeWithAI(slides);
            
            StringBuilder outline = new StringBuilder();
            outline.append("=== 智能大纲建议 ===\n\n");
            
            outline.append("【当前结构】\n");
            for (int i = 0; i < analysis.getOutline().size(); i++) {
                outline.append(i + 1).append(". ").append(analysis.getOutline().get(i)).append("\n");
            }
            
            outline.append("\n【优化建议】\n");
            outline.append("1. 确保开场有明确的主题介绍\n");
            outline.append("2. 主要内容按逻辑顺序排列\n");
            outline.append("3. 结尾包含总结和行动建议\n");
            outline.append("4. 每页内容控制在3-5个要点\n");
            
            outline.append("\n【逻辑流程】\n");
            for (int i = 0; i < analysis.getLogicalFlow().size(); i++) {
                outline.append(i + 1).append(". ").append(analysis.getLogicalFlow().get(i)).append("\n");
            }
            
            return outline.toString();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成智能大纲失败", e);
            return "生成智能大纲时发生错误: " + e.getMessage();
        }
    }

    /**
     * 生成重点内容分析
     * 
     * @param slides 幻灯片列表
     * @return 重点内容分析
     */
    public String generateKeyPointsAnalysis(List<Slide> slides) {
        try {
            StructureAnalysis analysis = analyzeWithAI(slides);
            
            StringBuilder analysisText = new StringBuilder();
            analysisText.append("=== 重点内容分析 ===\n\n");
            
            analysisText.append("【核心重点】\n");
            for (int i = 0; i < Math.min(analysis.getKeyPoints().size(), 10); i++) {
                analysisText.append(i + 1).append(". ").append(analysis.getKeyPoints().get(i)).append("\n");
            }
            
            analysisText.append("\n【关键词统计】\n");
            analysis.getKeywordFrequency().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> analysisText.append(entry.getKey()).append(": ").append(entry.getValue()).append("次\n"));
            
            analysisText.append("\n【重点突出建议】\n");
            analysisText.append("1. 使用视觉元素突出关键信息\n");
            analysisText.append("2. 采用对比色强调重要内容\n");
            analysisText.append("3. 添加图表和图形说明\n");
            analysisText.append("4. 确保每页有明确的重点\n");
            
            return analysisText.toString();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成重点内容分析失败", e);
            return "生成重点内容分析时发生错误: " + e.getMessage();
        }
    }

    /**
     * 生成逻辑关系图
     * 
     * @param slides 幻灯片列表
     * @return 逻辑关系图数据
     */
    public String generateLogicGraph(List<Slide> slides) {
        try {
            StructureAnalysis analysis = analyzeWithAI(slides);
            return SlideStructureAnalyzer.generateLogicGraphData(analysis);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成逻辑关系图失败", e);
            return "{\"error\": \"生成逻辑关系图时发生错误: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成完整的结构分析报告
     * 
     * @param slides 幻灯片列表
     * @return 完整的分析报告
     */
    public String generateCompleteReport(List<Slide> slides) {
        try {
            StructureAnalysis analysis = analyzeWithAI(slides);
            
            StringBuilder report = new StringBuilder();
            report.append("=== 幻灯片结构分析完整报告 ===\n\n");
            
            // 基础分析报告
            report.append(SlideStructureAnalyzer.generateAnalysisReport(analysis));
            
            // AI增强建议
            report.append("\n=== AI智能建议 ===\n");
            report.append("基于AI分析，建议：\n");
            report.append("1. 优化内容逻辑流程\n");
            report.append("2. 突出核心重点内容\n");
            report.append("3. 增强视觉表达效果\n");
            report.append("4. 完善结构层次关系\n");
            
            // 改进建议
            report.append("\n=== 具体改进建议 ===\n");
            if (analysis.getTotalSlides() < 3) {
                report.append("- 建议增加更多内容页面\n");
            }
            if (analysis.getKeyPoints().size() < 5) {
                report.append("- 建议丰富重点内容\n");
            }
            if (analysis.getThemes().size() < 2) {
                report.append("- 建议扩展主题范围\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成完整报告失败", e);
            return "生成完整报告时发生错误: " + e.getMessage();
        }
    }
}