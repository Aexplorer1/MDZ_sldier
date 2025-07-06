package slideshow;

import slideshow.util.SlideParser;
import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;

import java.util.List;

/**
 * SlideParser测试类
 * 用于验证AI生成PPT的分页逻辑
 */
public class SlideParserTest {

    public static void main(String[] args) {
        testSlideParser();
    }

    /**
     * 测试SlideParser的各种功能
     */
    public static void testSlideParser() {
        System.out.println("=== SlideParser 测试开始 ===");
        
        // 测试用例1：基本PPT命令格式
        String testCommand1 = 
            "Page 1:\n" +
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
        
        testParseCommand("测试用例1：基本PPT命令", testCommand1);
        
        // 测试用例2：包含绘图命令
        String testCommand2 = 
            "Page 1:\n" +
            "Title: 数据可视化\n" +
            "Draw: Line(50,50,200,150)\n" +
            "Draw: Circle(300,100,50)\n" +
            "Draw: Arrow(400,200,500,300)\n" +
            "Page 2:\n" +
            "Title: 图表分析\n" +
            "Draw: Rectangle(100,100,400,300)";
        
        testParseCommand("测试用例2：包含绘图命令", testCommand2);
        
        // 测试用例3：空内容
        String testCommand3 = "";
        testParseCommand("测试用例3：空内容", testCommand3);
        
        // 测试用例4：无效格式
        String testCommand4 = "这是无效的PPT命令格式";
        testParseCommand("测试用例4：无效格式", testCommand4);
        
        // 测试验证方法
        testValidationMethods();
        
        System.out.println("=== SlideParser 测试完成 ===");
    }

    /**
     * 测试解析命令
     */
    private static void testParseCommand(String testName, String command) {
        System.out.println("\n--- " + testName + " ---");
        System.out.println("输入命令：");
        System.out.println(command);
        
        try {
            List<Slide> slides = SlideParser.parseAndCreateSlides(command, 800.0);
            System.out.println("解析结果：");
            System.out.println("幻灯片数量: " + slides.size());
            
            for (int i = 0; i < slides.size(); i++) {
                Slide slide = slides.get(i);
                System.out.println("  幻灯片 " + (i + 1) + ":");
                System.out.println("    元素数量: " + slide.getElements().size());
                
                for (int j = 0; j < slide.getElements().size(); j++) {
                    var element = slide.getElements().get(j);
                    if (element instanceof TextElement) {
                        TextElement textElement = (TextElement) element;
                        System.out.println("    文本元素 " + (j + 1) + ": " + textElement.getText());
                    } else if (element instanceof DrawElement) {
                        DrawElement drawElement = (DrawElement) element;
                        System.out.println("    绘图元素 " + (j + 1) + ": DrawElement");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试验证方法
     */
    private static void testValidationMethods() {
        System.out.println("\n--- 验证方法测试 ---");

        String validCommand = "Page 1:\nTitle: 测试\nPage 2:\nTitle: 测试2";
        String invalidCommand = "这是无效的命令";

        System.out.println("有效命令验证: " + SlideParser.isValidPPTCommand(validCommand));
        System.out.println("无效命令验证: " + SlideParser.isValidPPTCommand(invalidCommand));
        System.out.println("空命令验证: " + SlideParser.isValidPPTCommand(""));

        System.out.println("有效命令页面数: " + SlideParser.getPageCount(validCommand));
        System.out.println("无效命令页面数: " + SlideParser.getPageCount(invalidCommand));
        System.out.println("空命令页面数: " + SlideParser.getPageCount(""));
    }
}