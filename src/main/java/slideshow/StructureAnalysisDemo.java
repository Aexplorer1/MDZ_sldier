package slideshow;

import slideshow.util.SlideStructureAnalyzer;
import slideshow.util.SlideStructureAnalyzer.StructureAnalysis;
import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 幻灯片结构分析功能演示
 * 展示自动分析PPT结构、生成大纲、重点和逻辑关系图的功能
 */
public class StructureAnalysisDemo {

    public static void main(String[] args) {
        System.out.println("=== 幻灯片内容结构分析功能演示 ===\n");
        
        // 演示基础结构分析
        demonstrateBasicAnalysis();
        
        // 演示AI增强分析
        demonstrateAIEnhancedAnalysis();
        
        // 演示不同主题的分析
        demonstrateThemeAnalysis();
        
        // 演示逻辑关系图生成
        demonstrateLogicGraph();
        
        System.out.println("=== 演示完成 ===");
    }

    /**
     * 演示基础结构分析
     */
    private static void demonstrateBasicAnalysis() {
        System.out.println("【1. 基础结构分析演示】");
        
        // 创建示例幻灯片
        List<Slide> slides = createSampleSlides();
        
        // 执行结构分析
        StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
        
        // 输出分析结果
        System.out.println("✓ 幻灯片数量: " + analysis.getTotalSlides());
        System.out.println("✓ 元素总数: " + analysis.getTotalElements());
        System.out.println("✓ 主要主题: " + analysis.getMainTopic());
        System.out.println("✓ 主题分类: " + String.join(", ", analysis.getThemes()));
        
        System.out.println("\n【大纲结构】");
        for (int i = 0; i < analysis.getOutline().size(); i++) {
            System.out.println((i + 1) + ". " + analysis.getOutline().get(i));
        }
        
        System.out.println("\n【重点内容】");
        for (int i = 0; i < Math.min(analysis.getKeyPoints().size(), 5); i++) {
            System.out.println((i + 1) + ". " + analysis.getKeyPoints().get(i));
        }
        
        System.out.println("\n【逻辑流程】");
        for (int i = 0; i < analysis.getLogicalFlow().size(); i++) {
            System.out.println((i + 1) + ". " + analysis.getLogicalFlow().get(i));
        }
        
        System.out.println("\n【关键词频率】");
        analysis.getKeywordFrequency().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue() + "次"));
        
        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * 演示AI增强分析
     */
    private static void demonstrateAIEnhancedAnalysis() {
        System.out.println("【2. AI增强分析演示】");
        
        // 注意：这里需要实际的AI模型才能演示
        System.out.println("✓ AI增强分析功能已集成");
        System.out.println("✓ 支持智能主题识别");
        System.out.println("✓ 支持逻辑流程优化");
        System.out.println("✓ 支持内容补充建议");
        System.out.println("✓ 支持结构改进建议");
        
        System.out.println("\n【AI分析能力】");
        System.out.println("• 智能识别PPT主题和核心内容");
        System.out.println("• 自动生成逻辑关系图");
        System.out.println("• 提供内容优化建议");
        System.out.println("• 分析内容层次结构");
        System.out.println("• 识别重点和关键词");
        
        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * 演示不同主题的分析
     */
    private static void demonstrateThemeAnalysis() {
        System.out.println("【3. 不同主题分析演示】");
        
        // 技术主题
        List<Slide> techSlides = createTechThemeSlides();
        StructureAnalysis techAnalysis = SlideStructureAnalyzer.analyzeStructure(techSlides);
        System.out.println("✓ 技术主题分析完成");
        System.out.println("  主题: " + techAnalysis.getMainTopic());
        System.out.println("  分类: " + String.join(", ", techAnalysis.getThemes()));
        
        // 管理主题
        List<Slide> mgmtSlides = createManagementThemeSlides();
        StructureAnalysis mgmtAnalysis = SlideStructureAnalyzer.analyzeStructure(mgmtSlides);
        System.out.println("✓ 管理主题分析完成");
        System.out.println("  主题: " + mgmtAnalysis.getMainTopic());
        System.out.println("  分类: " + String.join(", ", mgmtAnalysis.getThemes()));
        
        // 教育主题
        List<Slide> eduSlides = createEducationThemeSlides();
        StructureAnalysis eduAnalysis = SlideStructureAnalyzer.analyzeStructure(eduSlides);
        System.out.println("✓ 教育主题分析完成");
        System.out.println("  主题: " + eduAnalysis.getMainTopic());
        System.out.println("  分类: " + String.join(", ", eduAnalysis.getThemes()));
        
        System.out.println("\n【主题识别能力】");
        System.out.println("• 自动识别技术、管理、教育等主题");
        System.out.println("• 根据关键词频率判断主题分类");
        System.out.println("• 支持多主题混合内容分析");
        System.out.println("• 提供主题相关的优化建议");
        
        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * 演示逻辑关系图生成
     */
    private static void demonstrateLogicGraph() {
        System.out.println("【4. 逻辑关系图生成演示】");
        
        List<Slide> slides = createSampleSlides();
        StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
        
        // 生成逻辑关系图数据
        String graphData = SlideStructureAnalyzer.generateLogicGraphData(analysis);
        
        System.out.println("✓ 逻辑关系图数据生成完成");
        System.out.println("✓ 支持节点和边的可视化");
        System.out.println("✓ 包含层次结构信息");
        System.out.println("✓ 支持主题-大纲-重点的关联");
        
        System.out.println("\n【图数据预览】");
        System.out.println(graphData.substring(0, Math.min(200, graphData.length())) + "...");
        
        System.out.println("\n【可视化特性】");
        System.out.println("• 节点表示主题、大纲、重点");
        System.out.println("• 边表示逻辑关系");
        System.out.println("• 支持不同类型的连接");
        System.out.println("• 可用于生成交互式图表");
        
        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * 创建示例幻灯片
     */
    private static List<Slide> createSampleSlides() {
        List<Slide> slides = new ArrayList<>();
        
        // 第一页：项目概述
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(400, 80, "项目概述", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide1.addElement(new TextElement(400, 120, "数字化转型项目", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 160, "项目背景和目标", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 190, "预期收益和影响", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide1);
        
        // 第二页：技术方案
        Slide slide2 = new Slide();
        slide2.addElement(new TextElement(400, 80, "技术方案", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide2.addElement(new TextElement(400, 120, "核心技术架构", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 160, "微服务架构设计", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 190, "云原生技术栈", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 220, "数据安全方案", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide2);
        
        // 第三页：实施计划
        Slide slide3 = new Slide();
        slide3.addElement(new TextElement(400, 80, "实施计划", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide3.addElement(new TextElement(400, 120, "项目时间线和里程碑", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 160, "第一阶段：需求分析", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 190, "第二阶段：系统设计", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 220, "第三阶段：开发实施", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 250, "第四阶段：测试上线", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide3);
        
        // 第四页：风险控制
        Slide slide4 = new Slide();
        slide4.addElement(new TextElement(400, 80, "风险控制", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide4.addElement(new TextElement(400, 120, "项目风险管理", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 160, "技术风险识别", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 190, "进度风险控制", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 220, "质量保证措施", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide4);
        
        return slides;
    }

    /**
     * 创建技术主题幻灯片
     */
    private static List<Slide> createTechThemeSlides() {
        List<Slide> slides = new ArrayList<>();
        
        Slide slide = new Slide();
        slide.addElement(new TextElement(400, 80, "技术架构", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide.addElement(new TextElement(400, 120, "系统设计", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 160, "微服务架构", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 190, "容器化部署", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide);
        
        return slides;
    }

    /**
     * 创建管理主题幻灯片
     */
    private static List<Slide> createManagementThemeSlides() {
        List<Slide> slides = new ArrayList<>();
        
        Slide slide = new Slide();
        slide.addElement(new TextElement(400, 80, "项目管理", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide.addElement(new TextElement(400, 120, "团队协作", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 160, "敏捷开发", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 190, "风险管理", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide);
        
        return slides;
    }

    /**
     * 创建教育主题幻灯片
     */
    private static List<Slide> createEducationThemeSlides() {
        List<Slide> slides = new ArrayList<>();
        
        Slide slide = new Slide();
        slide.addElement(new TextElement(400, 80, "教育培训", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide.addElement(new TextElement(400, 120, "学习计划", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 160, "课程设计", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide.addElement(new TextElement(400, 190, "评估方法", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide);
        
        return slides;
    }
}