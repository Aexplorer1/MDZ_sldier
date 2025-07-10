package slideshow;

import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;
import slideshow.util.IntelligentLayoutEngine;
import slideshow.util.MultilingualSupport;
import slideshow.util.MultilingualSupport.SupportedLanguage;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.ArrayList;

/**
 * 增强功能测试类
 * 演示AI智能排版优化和多语言幻灯片生成功能
 */
public class EnhancedFeaturesTest {
    
    /**
     * 测试智能排版优化功能
     */
    public static void testIntelligentLayoutOptimization() {
        System.out.println("=== 测试智能排版优化功能 ===");
        
        try {
            // 创建测试幻灯片
            Slide testSlide = createTestSlide();
            
            System.out.println("原始幻灯片元素数量: " + testSlide.getElements().size());
            
            // 测试不同的布局类型
            IntelligentLayoutEngine.LayoutType[] layoutTypes = {
                IntelligentLayoutEngine.LayoutType.CENTERED,
                IntelligentLayoutEngine.LayoutType.LEFT_ALIGNED,
                IntelligentLayoutEngine.LayoutType.GRID,
                IntelligentLayoutEngine.LayoutType.FLOW,
                IntelligentLayoutEngine.LayoutType.COMPACT
            };
            
            for (IntelligentLayoutEngine.LayoutType layoutType : layoutTypes) {
                System.out.println("\n测试布局类型: " + layoutType.getDisplayName());
                
                // 创建幻灯片副本进行测试
                Slide slideCopy = createTestSlide();
                
                // 应用智能布局优化
                IntelligentLayoutEngine.optimizeLayout(slideCopy, 800.0, 600.0, layoutType);
                
                System.out.println("优化后元素数量: " + slideCopy.getElements().size());
                
                // 显示元素位置信息
                for (int i = 0; i < slideCopy.getElements().size(); i++) {
                    SlideElement element = slideCopy.getElements().get(i);
                    System.out.println("  元素 " + (i + 1) + ": 位置(" + element.getX() + ", " + element.getY() + 
                                     "), 尺寸(" + element.getWidth() + "x" + element.getHeight() + ")");
                }
            }
            
            System.out.println("\n智能排版优化测试完成");
            
        } catch (Exception e) {
            System.err.println("智能排版优化测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试多语言支持功能
     */
    public static void testMultilingualSupport() {
        System.out.println("\n=== 测试多语言支持功能 ===");
        
        try {
            // 测试原始内容
            String originalContent = "人工智能简介\n" +
                                   "• 什么是人工智能\n" +
                                   "• AI的发展历程\n" +
                                   "• AI的应用领域";
            
            System.out.println("原始内容:\n" + originalContent);
            
            // 测试不同语言的翻译
            SupportedLanguage[] languages = {
                SupportedLanguage.ENGLISH,
                SupportedLanguage.JAPANESE,
                SupportedLanguage.KOREAN
            };
            
            for (SupportedLanguage language : languages) {
                System.out.println("\n翻译为 " + language.getDisplayName() + ":");
                
                String translatedContent = MultilingualSupport.generateMultilingualContent(originalContent, language);
                System.out.println(translatedContent);
            }
            
            // 测试PPT命令翻译
            String originalPPTCommands = "Page 1:\n" +
                                        "Title: 人工智能简介\n" +
                                        "Subtitle: 探索AI的未来\n" +
                                        "Bullet: 什么是人工智能\n" +
                                        "Bullet: AI的发展历程\n" +
                                        "Bullet: AI的应用领域\n" +
                                        "Page 2:\n" +
                                        "Title: AI技术分类\n" +
                                        "Bullet: 机器学习\n" +
                                        "Bullet: 深度学习\n" +
                                        "Bullet: 自然语言处理\n" +
                                        "Draw: Rectangle(100,100,300,200)";
            
            System.out.println("\n原始PPT命令:\n" + originalPPTCommands);
            
            for (SupportedLanguage language : languages) {
                System.out.println("\nPPT命令翻译为 " + language.getDisplayName() + ":");
                
                String translatedCommands = MultilingualSupport.generateMultilingualPPTCommands(originalPPTCommands, language);
                System.out.println(translatedCommands);
            }
            
            System.out.println("\n多语言支持测试完成");
            
        } catch (Exception e) {
            System.err.println("多语言支持测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试语言切换功能
     */
    public static void testLanguageSwitching() {
        System.out.println("\n=== 测试语言切换功能 ===");
        
        try {
            // 显示支持的语言列表
            System.out.println("支持的语言:");
            for (SupportedLanguage language : MultilingualSupport.getSupportedLanguages()) {
                System.out.println("  " + language.getDisplayName() + " (" + language.getLanguageCode() + ")");
            }
            
            // 测试语言切换
            SupportedLanguage[] testLanguages = {
                SupportedLanguage.CHINESE,
                SupportedLanguage.ENGLISH,
                SupportedLanguage.JAPANESE
            };
            
            for (SupportedLanguage language : testLanguages) {
                System.out.println("\n切换到 " + language.getDisplayName() + ":");
                MultilingualSupport.switchLanguage(language);
                
                // 测试本地化字符串
                String[] testKeys = {"ui.new_slide", "ui.add_text", "ui.save", "dialog.ok"};
                for (String key : testKeys) {
                    String localizedString = MultilingualSupport.getString(key);
                    System.out.println("  " + key + ": " + localizedString);
                }
            }
            
            // 恢复为中文
            MultilingualSupport.switchLanguage(SupportedLanguage.CHINESE);
            System.out.println("\n已恢复为中文");
            
            System.out.println("语言切换测试完成");
            
        } catch (Exception e) {
            System.err.println("语言切换测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试响应式布局调整
     */
    public static void testResponsiveLayout() {
        System.out.println("\n=== 测试响应式布局调整 ===");
        
        try {
            // 创建测试幻灯片
            Slide testSlide = createTestSlide();
            
            System.out.println("原始尺寸: " + testSlide.getWidth() + "x" + testSlide.getHeight());
            
            // 测试不同的尺寸
            double[][] testSizes = {
                {1024, 768},  // 大屏幕
                {800, 600},   // 中等屏幕
                {640, 480},   // 小屏幕
                {1920, 1080}  // 高清屏幕
            };
            
            for (double[] size : testSizes) {
                System.out.println("\n调整到尺寸: " + size[0] + "x" + size[1]);
                
                // 创建幻灯片副本
                Slide slideCopy = createTestSlide();
                
                // 应用响应式调整
                IntelligentLayoutEngine.responsiveAdjust(slideCopy, size[0], size[1]);
                
                System.out.println("调整后尺寸: " + slideCopy.getWidth() + "x" + slideCopy.getHeight());
                
                // 显示元素位置信息
                for (int i = 0; i < slideCopy.getElements().size(); i++) {
                    SlideElement element = slideCopy.getElements().get(i);
                    System.out.println("  元素 " + (i + 1) + ": 位置(" + element.getX() + ", " + element.getY() + ")");
                }
            }
            
            System.out.println("响应式布局调整测试完成");
            
        } catch (Exception e) {
            System.err.println("响应式布局调整测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试自动文本大小调整
     */
    public static void testAutoTextSizeAdjustment() {
        System.out.println("\n=== 测试自动文本大小调整 ===");
        
        try {
            // 创建测试文本元素
            TextElement textElement = new TextElement(100, 100, "这是一个测试文本，用于测试自动大小调整功能", 
                                                   24.0, Color.BLACK, FontWeight.NORMAL, false);
            
            System.out.println("原始文本: " + textElement.getText());
            System.out.println("原始字体大小: " + textElement.getFontSize());
            System.out.println("原始尺寸: " + textElement.getWidth() + "x" + textElement.getHeight());
            
            // 测试不同的容器尺寸
            double[][] containerSizes = {
                {200, 50},   // 小容器
                {400, 100},  // 中等容器
                {600, 150},  // 大容器
                {100, 30}    // 极小容器
            };
            
            for (double[] size : containerSizes) {
                System.out.println("\n容器尺寸: " + size[0] + "x" + size[1]);
                
                // 创建文本元素副本
                TextElement textCopy = new TextElement(100, 100, textElement.getText(), 
                                                     textElement.getFontSize(), textElement.getColor(), 
                                                     textElement.getFontWeight(), textElement.isItalic());
                
                // 应用自动大小调整
                IntelligentLayoutEngine.autoAdjustTextSize(textCopy, size[0], size[1]);
                
                System.out.println("调整后字体大小: " + textCopy.getFontSize());
                System.out.println("调整后尺寸: " + textCopy.getWidth() + "x" + textCopy.getHeight());
            }
            
            System.out.println("自动文本大小调整测试完成");
            
        } catch (Exception e) {
            System.err.println("自动文本大小调整测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试幻灯片
     */
    private static Slide createTestSlide() {
        Slide slide = new Slide();
        
        // 添加文本元素
        TextElement titleElement = new TextElement(100, 50, "测试标题", 28.0, Color.BLUE, FontWeight.BOLD, false);
        TextElement subtitleElement = new TextElement(100, 100, "测试副标题", 20.0, Color.GRAY, FontWeight.NORMAL, false);
        TextElement bullet1 = new TextElement(100, 150, "• 测试要点1", 18.0, Color.BLACK, FontWeight.NORMAL, false);
        TextElement bullet2 = new TextElement(100, 180, "• 测试要点2", 18.0, Color.BLACK, FontWeight.NORMAL, false);
        TextElement bullet3 = new TextElement(100, 210, "• 测试要点3", 18.0, Color.BLACK, FontWeight.NORMAL, false);
        
        // 添加绘图元素
        DrawElement rectangle = new DrawElement(400, 100, DrawElement.ShapeType.RECTANGLE, Color.ORANGE, 2.0);
        rectangle.updateEndPoint(600, 200);
        
        DrawElement circle = new DrawElement(450, 250, DrawElement.ShapeType.CIRCLE, Color.GREEN, 2.0);
        circle.updateEndPoint(550, 350);
        
        // 添加元素到幻灯片
        slide.addElement(titleElement);
        slide.addElement(subtitleElement);
        slide.addElement(bullet1);
        slide.addElement(bullet2);
        slide.addElement(bullet3);
        slide.addElement(rectangle);
        slide.addElement(circle);
        
        return slide;
    }
    
    /**
     * 运行所有测试
     */
    public static void runAllTests() {
        System.out.println("开始运行增强功能测试...\n");
        
        testIntelligentLayoutOptimization();
        testMultilingualSupport();
        testLanguageSwitching();
        testResponsiveLayout();
        testAutoTextSizeAdjustment();
        
        System.out.println("\n所有增强功能测试完成！");
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        runAllTests();
    }
} 