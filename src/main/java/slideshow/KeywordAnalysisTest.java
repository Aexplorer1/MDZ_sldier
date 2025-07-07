package slideshow;

import slideshow.model.Slide;
import slideshow.elements.TextElement;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.ArrayList;

/**
 * 关键词分析功能测试类
 */
public class KeywordAnalysisTest {
    
    public static void main(String[] args) {
        System.out.println("开始关键词分析测试...");
        
        // 创建测试幻灯片
        List<Slide> testSlides = createTestSlides();
        
        // 模拟AI模型（这里只是打印内容，实际使用时需要真实的AI模型）
        System.out.println("=== 测试幻灯片内容 ===");
        for (int i = 0; i < testSlides.size(); i++) {
            Slide slide = testSlides.get(i);
            System.out.println("第" + (i + 1) + "页:");
            List<String> textContent = slide.getTextContent();
            for (String text : textContent) {
                System.out.println("  " + text);
            }
            System.out.println();
        }
        
        // 模拟关键词提取结果
        System.out.println("=== 模拟关键词分析结果 ===");
        simulateKeywordAnalysis(testSlides);
        
        System.out.println("关键词分析测试完成！");
    }
    
    /**
     * 创建测试幻灯片
     */
    private static List<Slide> createTestSlides() {
        List<Slide> slides = new ArrayList<>();
        
        // 第一页：人工智能介绍
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(100, 100, "人工智能技术发展", 24, Color.BLACK, FontWeight.BOLD, false));
        slide1.addElement(new TextElement(100, 150, "• 机器学习算法", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(100, 180, "• 深度学习技术", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(100, 210, "• 自然语言处理", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide1);
        
        // 第二页：应用领域
        Slide slide2 = new Slide();
        slide2.addElement(new TextElement(100, 100, "AI应用领域", 24, Color.BLACK, FontWeight.BOLD, false));
        slide2.addElement(new TextElement(100, 150, "• 医疗诊断", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 180, "• 自动驾驶", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 210, "• 智能客服", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 240, "• 金融风控", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide2);
        
        // 第三页：发展趋势
        Slide slide3 = new Slide();
        slide3.addElement(new TextElement(100, 100, "未来发展趋势", 24, Color.BLACK, FontWeight.BOLD, false));
        slide3.addElement(new TextElement(100, 150, "• 边缘计算", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 180, "• 联邦学习", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 210, "• 可解释AI", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide3);
        
        return slides;
    }
    
    /**
     * 模拟关键词分析
     */
    private static void simulateKeywordAnalysis(List<Slide> slides) {
        // 模拟AI分析结果
        System.out.println("📊 基本信息:");
        System.out.println("• 幻灯片总数: " + slides.size());
        System.out.println("• 总字数: " + countTotalWords(slides));
        System.out.println();
        
        System.out.println("🎯 主要主题:");
        System.out.println("人工智能技术发展与应用");
        System.out.println();
        
        System.out.println("🔑 关键词 (共8个):");
        System.out.println("1. 人工智能 (出现3次)");
        System.out.println("2. 技术 (出现2次)");
        System.out.println("3. 发展 (出现2次)");
        System.out.println("4. 机器学习 (出现1次)");
        System.out.println("5. 深度学习 (出现1次)");
        System.out.println("6. 应用 (出现1次)");
        System.out.println("7. 医疗 (出现1次)");
        System.out.println("8. 自动驾驶 (出现1次)");
        System.out.println();
        
        System.out.println("📂 主题分类:");
        System.out.println("1. 技术基础");
        System.out.println("2. 应用领域");
        System.out.println("3. 发展趋势");
        System.out.println();
        
        System.out.println("📝 内容摘要:");
        System.out.println("本PPT介绍了人工智能技术的发展历程，包括机器学习、深度学习等核心技术，");
        System.out.println("以及在医疗、交通、金融等领域的应用，最后展望了边缘计算、联邦学习等未来发展趋势。");
        System.out.println();
        
        System.out.println("=== 分析完成 ===");
    }
    
    /**
     * 统计总字数
     */
    private static int countTotalWords(List<Slide> slides) {
        int totalWords = 0;
        for (Slide slide : slides) {
            List<String> textContent = slide.getTextContent();
            for (String text : textContent) {
                totalWords += text.replaceAll("\\s+", "").length();
            }
        }
        return totalWords;
    }
} 