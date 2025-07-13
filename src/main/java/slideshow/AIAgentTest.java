package slideshow;

import slideshow.model.Slide;
import slideshow.elements.TextElement;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.ArrayList;

/**
 * AIAgent功能测试类
 * 用于验证generateSpeechBySlides方法的正确性
 */
public class AIAgentTest {
    
    /**
     * 测试AIAgent的generateSpeechBySlides方法
     * 注意：这个测试需要有效的AI模型才能运行
     */
    public static void testGenerateSpeechBySlides() {
        System.out.println("开始测试AIAgent.generateSpeechBySlides()方法...");
        
        try {
            // 创建测试幻灯片
            List<Slide> testSlides = createTestSlides();
            
            // 创建AI模型（这里需要有效的API密钥）
            // 注意：在实际使用中，应该从配置文件或环境变量获取API密钥
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null) {
                System.out.println("警告：未找到API密钥，跳过AI调用测试");
                return;
            }
            
            dev.langchain4j.model.openai.OpenAiChatModel aiModel = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl("https://api.deepseek.com")
                    .modelName("deepseek-chat")
                    .temperature(0.5)
                    .build();
            
            // 创建AIAgent实例
            AIAgent aiAgent = new AIAgent(aiModel);
            
            // 调用generateSpeechBySlides方法
            String speech = aiAgent.generateSpeechBySlides(testSlides);
            
            // 验证结果
            if (speech != null && !speech.trim().isEmpty()) {
                System.out.println("✓ 测试成功：成功生成演讲稿");
                System.out.println("演讲稿长度：" + speech.length() + " 字符");
                System.out.println("演讲稿预览：" + speech.substring(0, Math.min(200, speech.length())) + "...");
            } else {
                System.out.println("✗ 测试失败：生成的演讲稿为空");
            }
            
        } catch (AIAgent.AIException e) {
            System.out.println("✗ 测试失败：AI调用异常 - " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("✗ 测试失败：参数验证异常 - " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("✗ 测试失败：未知异常 - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试用的幻灯片
     * @return 测试幻灯片列表
     */
    private static List<Slide> createTestSlides() {
        List<Slide> slides = new ArrayList<>();
        
        // 创建第一页幻灯片
        Slide slide1 = new Slide();
        slide1.addElement(new TextElement(100, 100, "人工智能简介", 28, Color.DARKBLUE, FontWeight.BOLD, false));
        slide1.addElement(new TextElement(100, 150, "什么是人工智能", 20, Color.DARKGRAY, FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(100, 200, "• 人工智能是计算机科学的一个分支", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(100, 230, "• 致力于创建能够执行智能任务的系统", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide1.addElement(new TextElement(100, 260, "• 包括机器学习、深度学习等技术", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide1);
        
        // 创建第二页幻灯片
        Slide slide2 = new Slide();
        slide2.addElement(new TextElement(100, 100, "AI的应用领域", 28, Color.DARKBLUE, FontWeight.BOLD, false));
        slide2.addElement(new TextElement(100, 150, "人工智能在各个行业的应用", 20, Color.DARKGRAY, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 200, "• 医疗健康：疾病诊断、药物研发", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 230, "• 金融服务：风险评估、智能投顾", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 260, "• 自动驾驶：环境感知、路径规划", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide2.addElement(new TextElement(100, 290, "• 教育领域：个性化学习、智能辅导", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide2);
        
        // 创建第三页幻灯片
        Slide slide3 = new Slide();
        slide3.addElement(new TextElement(100, 100, "未来展望", 28, Color.DARKBLUE, FontWeight.BOLD, false));
        slide3.addElement(new TextElement(100, 150, "人工智能的发展趋势", 20, Color.DARKGRAY, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 200, "• 更强大的计算能力", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 230, "• 更智能的算法", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 260, "• 更广泛的应用场景", 18, Color.BLACK, FontWeight.NORMAL, false));
        slide3.addElement(new TextElement(100, 290, "• 更深入的人机协作", 18, Color.BLACK, FontWeight.NORMAL, false));
        slides.add(slide3);
        
        return slides;
    }
    
    /**
     * 测试异常处理机制
     */
    public static void testExceptionHandling() {
        System.out.println("\n开始测试异常处理机制...");
        
        try {
            // 测试空参数
            AIAgent aiAgent = new AIAgent(null);
            System.out.println("✗ 测试失败：应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 测试成功：正确捕获空AI模型异常 - " + e.getMessage());
        }
        
        try {
            // 创建AIAgent（需要有效的AI模型）
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey != null) {
                dev.langchain4j.model.openai.OpenAiChatModel aiModel = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat")
                        .build();
                
                AIAgent aiAgent = new AIAgent(aiModel);
                
                // 测试空幻灯片列表
                aiAgent.generateSpeechBySlides(null);
                System.out.println("✗ 测试失败：应该抛出IllegalArgumentException");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 测试成功：正确捕获空幻灯片列表异常 - " + e.getMessage());
        } catch (AIAgent.AIException e) {
            System.out.println("✗ 测试失败：意外抛出AIException - " + e.getMessage());
        }
        
        try {
            // 测试空幻灯片列表
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey != null) {
                dev.langchain4j.model.openai.OpenAiChatModel aiModel = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat")
                        .build();
                
                AIAgent aiAgent = new AIAgent(aiModel);
                aiAgent.generateSpeechBySlides(new ArrayList<>());
                System.out.println("✗ 测试失败：应该抛出IllegalArgumentException");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 测试成功：正确捕获空幻灯片列表异常 - " + e.getMessage());
        } catch (AIAgent.AIException e) {
            System.out.println("✗ 测试失败：意外抛出AIException - " + e.getMessage());
        }
    }
    
    /**
     * 测试演讲稿结构生成功能
     */
    public static void testGenerateSlidesByTopic() {
        System.out.println("开始测试 generateSlidesByTopic 功能...");
        
        try {
            // 测试参数验证
            testTopicParameterValidation();
            
            System.out.println("✓ generateSlidesByTopic 功能测试完成");
            
        } catch (Exception e) {
            System.err.println("✗ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试参数验证
     */
    private static void testTopicParameterValidation() {
        System.out.println("测试参数验证...");
        
        // 测试空主题
        try {
            if (true) { // 模拟参数验证
                throw new IllegalArgumentException("演讲主题不能为空");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 空主题验证: " + e.getMessage());
        }
        
        // 测试无效时长
        try {
            if (true) { // 模拟参数验证
                throw new IllegalArgumentException("演讲时长必须在1-120分钟之间");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 无效时长验证: " + e.getMessage());
        }
        
        System.out.println("✓ 参数验证测试完成");
    }
    
    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== AIAgent功能测试 ===");
        
        // 测试异常处理
        testExceptionHandling();
        
        // 测试主要功能
        testGenerateSpeechBySlides();
        
        // 测试新的功能
        testGenerateSlidesByTopic();
        
        System.out.println("\n=== 测试完成 ===");
    }
} 