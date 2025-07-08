package slideshow;

import java.util.List;
import java.util.ArrayList;

/**
 * AI问答功能测试类
 */
public class AIQATest {
    
    public static void main(String[] args) {
        System.out.println("开始AI问答功能测试...");
        
        // 模拟测试问题
        List<String> testQuestions = new ArrayList<>();
        testQuestions.add("什么是人工智能？");
        testQuestions.add("如何制作一个好的PPT？");
        testQuestions.add("今天天气怎么样？");
        testQuestions.add("Java和Python有什么区别？");
        
        System.out.println("=== 测试问题列表 ===");
        for (int i = 0; i < testQuestions.size(); i++) {
            System.out.println((i + 1) + ". " + testQuestions.get(i));
        }
        System.out.println();
        
        // 模拟AI回答
        System.out.println("=== 模拟AI回答 ===");
        simulateAIAnswers(testQuestions);
        
        System.out.println("AI问答功能测试完成！");
    }
    
    /**
     * 模拟AI回答
     */
    private static void simulateAIAnswers(List<String> questions) {
        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            String answer = generateMockAnswer(question);
            
            System.out.println("❓ 问题 " + (i + 1) + "：" + question);
            System.out.println("🤖 AI回答：");
            System.out.println(answer);
            System.out.println("---");
            System.out.println();
        }
    }
    
    /**
     * 生成模拟回答
     */
    private static String generateMockAnswer(String question) {
        if (question.contains("人工智能")) {
            return "人工智能（Artificial Intelligence，AI）是计算机科学的一个分支，旨在创建能够执行通常需要人类智能的任务的系统。\n\n" +
                   "主要特点：\n" +
                   "• 机器学习：从数据中学习模式\n" +
                   "• 深度学习：使用神经网络处理复杂数据\n" +
                   "• 自然语言处理：理解和生成人类语言\n" +
                   "• 计算机视觉：识别和理解图像\n\n" +
                   "应用领域：医疗诊断、自动驾驶、智能客服、金融风控等。";
        } else if (question.contains("PPT") || question.contains("ppt")) {
            return "制作一个好的PPT需要遵循以下原则：\n\n" +
                   "📋 内容设计：\n" +
                   "• 明确主题和目标受众\n" +
                   "• 结构清晰，逻辑连贯\n" +
                   "• 内容简洁，避免文字过多\n\n" +
                   "🎨 视觉设计：\n" +
                   "• 使用统一的配色方案\n" +
                   "• 选择合适的字体和字号\n" +
                   "• 添加适当的图片和图表\n\n" +
                   "💡 演示技巧：\n" +
                   "• 控制每页内容量\n" +
                   "• 使用动画效果适度\n" +
                   "• 准备演讲稿和备注";
        } else if (question.contains("天气")) {
            return "抱歉，我无法获取实时天气信息。建议您：\n\n" +
                   "🌤️ 查看天气的方式：\n" +
                   "• 使用天气APP\n" +
                   "• 访问天气网站\n" +
                   "• 查看手机系统天气\n\n" +
                   "📱 推荐天气应用：\n" +
                   "• 墨迹天气\n" +
                   "• 天气通\n" +
                   "• 系统自带天气";
        } else if (question.contains("Java") || question.contains("Python")) {
            return "Java和Python的主要区别：\n\n" +
                   "🔧 语法特点：\n" +
                   "• Java：强类型，需要显式声明变量类型\n" +
                   "• Python：动态类型，代码更简洁\n\n" +
                   "⚡ 性能对比：\n" +
                   "• Java：编译型语言，执行速度快\n" +
                   "• Python：解释型语言，开发效率高\n\n" +
                   "🎯 应用领域：\n" +
                   "• Java：企业级应用、Android开发\n" +
                   "• Python：数据科学、AI、Web开发\n\n" +
                   "📚 学习建议：\n" +
                   "• 初学者推荐Python\n" +
                   "• 企业开发推荐Java";
        } else {
            return "这是一个很好的问题！\n\n" +
                   "💭 我的回答：\n" +
                   "作为AI助手，我会尽力为您提供准确、有用的信息。\n" +
                   "如果您有具体的技术问题或需要帮助，请随时告诉我。\n\n" +
                   "🔍 建议：\n" +
                   "• 提供更多具体细节\n" +
                   "• 说明您的具体需求\n" +
                   "• 我会根据您的问题提供针对性回答";
        }
    }
} 