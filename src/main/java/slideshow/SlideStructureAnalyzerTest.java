package slideshow;

import slideshow.util.SlideStructureAnalyzer;
import slideshow.util.SlideStructureAnalyzer.StructureAnalysis;
import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;

import java.util.List;
import java.util.ArrayList;

/**
 * 幻灯片结构分析器测试类
 * 用于验证幻灯片内容结构分析功能
 */
public class SlideStructureAnalyzerTest {

    public static void main(String[] args) {
        testSlideStructureAnalyzer();
    }

    /**
     * 测试幻灯片结构分析器
     */
    public static void testSlideStructureAnalyzer() {
        System.out.println("=== 幻灯片结构分析器测试开始 ===");
        
        // 创建测试幻灯片
        List<Slide> testSlides = createTestSlides();
        
        // 执行结构分析
        StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(testSlides);
        
        // 输出分析结果
        System.out.println("分析结果：");
        System.out.println(analysis.toString());
        
        // 生成分析报告
        String report = SlideStructureAnalyzer.generateAnalysisReport(analysis);
        System.out.println("\n=== 详细分析报告 ===");
        System.out.println(report);
        
        // 生成逻辑关系图数据
        String graphData = SlideStructureAnalyzer.generateLogicGraphData(analysis);
        System.out.println("\n=== 逻辑关系图数据 ===");
        System.out.println(graphData);
        
        System.out.println("=== 幻灯片结构分析器测试完成 ===");
    }

    /**
     * 创建测试幻灯片
     */
    private static List<Slide> createTestSlides() {
        List<Slide> slides = new ArrayList<>();
        
        // 第一页：人工智能简介
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(400, 80, "人工智能简介", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide1.addElement(new TextElement(400, 120, "探索AI的未来发展", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 160, "什么是人工智能", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 190, "AI的发展历程", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 220, "AI的应用领域", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide1);
        
        // 第二页：AI技术分类
        Slide slide2 = new Slide();
        slide2.addElement(new TextElement(400, 80, "AI技术分类", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide2.addElement(new TextElement(400, 120, "主要技术分支", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 160, "机器学习", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 190, "深度学习", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 220, "自然语言处理", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(400, 250, "计算机视觉", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide2);
        
        // 第三页：AI应用案例
        Slide slide3 = new Slide();
        slide3.addElement(new TextElement(400, 80, "AI应用案例", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide3.addElement(new TextElement(400, 120, "实际应用场景", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 160, "智能助手", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 190, "自动驾驶", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 220, "医疗诊断", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(400, 250, "金融风控", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide3);
        
        // 第四页：未来展望
        Slide slide4 = new Slide();
        slide4.addElement(new TextElement(400, 80, "未来展望", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide4.addElement(new TextElement(400, 120, "AI发展趋势", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 160, "通用人工智能", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 190, "人机协作", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 220, "伦理与安全", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide4.addElement(new TextElement(400, 250, "社会影响", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide4);
        
        return slides;
    }

    /**
     * 测试不同主题的幻灯片
     */
    public static void testDifferentThemes() {
        System.out.println("\n=== 测试不同主题的幻灯片 ===");
        
        // 创建技术主题幻灯片
        List<Slide> techSlides = createTechThemeSlides();
        StructureAnalysis techAnalysis = SlideStructureAnalyzer.analyzeStructure(techSlides);
        System.out.println("技术主题分析：");
        System.out.println("主要主题: " + techAnalysis.getMainTopic());
        System.out.println("主题分类: " + String.join(", ", techAnalysis.getThemes()));
        
        // 创建管理主题幻灯片
        List<Slide> mgmtSlides = createManagementThemeSlides();
        StructureAnalysis mgmtAnalysis = SlideStructureAnalyzer.analyzeStructure(mgmtSlides);
        System.out.println("\n管理主题分析：");
        System.out.println("主要主题: " + mgmtAnalysis.getMainTopic());
        System.out.println("主题分类: " + String.join(", ", mgmtAnalysis.getThemes()));
    }

    /**
     * 创建技术主题幻灯片
     */
    private static List<Slide> createTechThemeSlides() {
        List<Slide> slides = new ArrayList<>();
        
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(400, 80, "技术架构设计", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide1.addElement(new TextElement(400, 120, "系统架构概述", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 160, "微服务架构", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 190, "容器化部署", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide1);
        
        return slides;
    }

    /**
     * 创建管理主题幻灯片
     */
    private static List<Slide> createManagementThemeSlides() {
        List<Slide> slides = new ArrayList<>();
        
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(400, 80, "项目管理方法", 28, javafx.scene.paint.Color.DARKBLUE, javafx.scene.text.FontWeight.BOLD, false));
        slide1.addElement(new TextElement(400, 120, "敏捷开发实践", 20, javafx.scene.paint.Color.DARKGRAY, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 160, "团队协作", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(400, 190, "风险管理", 18, javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false));
        slides.add(slide1);
        
        return slides;
    }
}