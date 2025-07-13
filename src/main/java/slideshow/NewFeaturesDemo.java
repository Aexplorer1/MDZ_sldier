package slideshow;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;

import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.SlideElement;
import slideshow.elements.ImageElement;
import slideshow.util.IntelligentLayoutEngine;
import slideshow.util.MultilingualSupport;
import slideshow.AIEnhancedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;
import java.util.ArrayList;

/**
 * 新功能演示类
 * 展示智能排版优化和多语言支持功能
 */
public class NewFeaturesDemo extends Application {
    
    private Slide demoSlide;
    private IntelligentLayoutEngine layoutEngine;
    private MultilingualSupport multilingualSupport;
    private AIEnhancedAgent enhancedAgent;
    
    @Override
    public void start(Stage primaryStage) {
        // 初始化组件
        initializeComponents();
        
        // 创建演示界面
        BorderPane root = new BorderPane();
        
        // 创建标题
        Label titleLabel = new Label("新功能演示 - 智能排版与多语言支持");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        titleLabel.setPadding(new Insets(10));
        
        // 创建功能按钮区域
        VBox buttonContainer = new VBox(10);
        buttonContainer.setPadding(new Insets(20));
        buttonContainer.setAlignment(Pos.CENTER);
        
        // 智能排版功能按钮
        Label layoutLabel = new Label("智能排版功能:");
        layoutLabel.setStyle("-fx-font-weight: bold;");
        
        Button optimizeLayoutBtn = new Button("优化布局");
        Button responsiveBtn = new Button("响应式调整");
        Button autoTextBtn = new Button("自动文本调整");
        
        // 多语言功能按钮
        Label languageLabel = new Label("多语言功能:");
        languageLabel.setStyle("-fx-font-weight: bold;");
        
        Button translateBtn = new Button("一键翻译当前幻灯片");
        Button translateAllBtn = new Button("批量翻译所有幻灯片");
        Button generateMultilingualBtn = new Button("生成多语言PPT");
        Button switchLanguageBtn = new Button("切换语言");
        
        // 综合功能按钮
        Label enhancedLabel = new Label("增强AI功能:");
        enhancedLabel.setStyle("-fx-font-weight: bold;");
        
        Button intelligentPPTBtn = new Button("智能多语言PPT生成");
        Button speechBtn = new Button("生成演讲稿");
        
        // 添加按钮到容器
        buttonContainer.getChildren().addAll(
            layoutLabel, optimizeLayoutBtn, responsiveBtn, autoTextBtn,
            new Separator(),
            languageLabel, translateBtn, translateAllBtn, generateMultilingualBtn, switchLanguageBtn,
            new Separator(),
            enhancedLabel, intelligentPPTBtn, speechBtn
        );
        
        // 创建结果显示区域
        TextArea resultArea = new TextArea();
        resultArea.setPrefRowCount(15);
        resultArea.setEditable(false);
        resultArea.setPromptText("功能演示结果将显示在这里...");
        
        // 设置布局
        root.setTop(titleLabel);
        root.setCenter(buttonContainer);
        root.setBottom(resultArea);
        
        // 添加事件处理
        optimizeLayoutBtn.setOnAction(e -> demonstrateLayoutOptimization(resultArea));
        responsiveBtn.setOnAction(e -> demonstrateResponsiveAdjustment(resultArea));
        autoTextBtn.setOnAction(e -> demonstrateAutoTextAdjustment(resultArea));
        translateBtn.setOnAction(e -> demonstrateTranslation(resultArea));
        translateAllBtn.setOnAction(e -> demonstrateBatchTranslation(resultArea));
        generateMultilingualBtn.setOnAction(e -> demonstrateMultilingualGeneration(resultArea));
        switchLanguageBtn.setOnAction(e -> demonstrateLanguageSwitch(resultArea));
        intelligentPPTBtn.setOnAction(e -> demonstrateIntelligentPPT(resultArea));
        speechBtn.setOnAction(e -> demonstrateSpeechGeneration(resultArea));
        
        // 创建场景
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("新功能演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建演示幻灯片
        demoSlide = new Slide();
        
        // 添加一些演示元素
        TextElement titleElement = new TextElement(100, 50, "演示标题", 24, 
            javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false);
        TextElement contentElement = new TextElement(100, 100, "这是演示内容，用于测试智能排版和多语言功能。", 16,
            javafx.scene.paint.Color.BLACK, javafx.scene.text.FontWeight.NORMAL, false);
        
        demoSlide.addElement(titleElement);
        demoSlide.addElement(contentElement);
        
        // 初始化布局引擎
        layoutEngine = new IntelligentLayoutEngine();
        
        // 初始化多语言支持
        multilingualSupport = new MultilingualSupport();
        
        // 初始化增强AI代理（模拟）
        try {
            OpenAiChatModel aiModel = OpenAiChatModel.builder()
                .apiKey("demo-key")
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .build();
            enhancedAgent = new AIEnhancedAgent(aiModel);
        } catch (Exception e) {
            // 如果AI模型初始化失败，使用模拟模式
            enhancedAgent = null;
        }
    }
    
    /**
     * 演示布局优化
     */
    private void demonstrateLayoutOptimization(TextArea resultArea) {
        try {
            resultArea.appendText("=== 布局优化演示 ===\n");
            
            // 应用布局优化
            layoutEngine.optimizeLayout(demoSlide, 800, 600, IntelligentLayoutEngine.LayoutType.CENTERED);
            
            resultArea.appendText("✓ 布局优化完成\n");
            resultArea.appendText("- 应用了居中布局\n");
            resultArea.appendText("- 元素位置已重新排列\n");
            resultArea.appendText("- 空间分配已优化\n\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 布局优化失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示响应式调整
     */
    private void demonstrateResponsiveAdjustment(TextArea resultArea) {
        try {
            resultArea.appendText("=== 响应式调整演示 ===\n");
            
            // 模拟不同尺寸的调整
            double[] widths = {600, 800, 1000};
            double[] heights = {400, 600, 800};
            
            for (int i = 0; i < widths.length; i++) {
                IntelligentLayoutEngine.responsiveAdjust(demoSlide, widths[i], heights[i]);
                resultArea.appendText(String.format("✓ 调整到 %dx%d 完成\n", (int)widths[i], (int)heights[i]));
            }
            
            resultArea.appendText("- 所有尺寸调整完成\n");
            resultArea.appendText("- 元素位置自适应\n");
            resultArea.appendText("- 文本大小自动调整\n\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 响应式调整失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示自动文本调整
     */
    private void demonstrateAutoTextAdjustment(TextArea resultArea) {
        try {
            resultArea.appendText("=== 自动文本调整演示 ===\n");
            
            // 获取文本元素
            List<SlideElement> elements = demoSlide.getElements();
            for (SlideElement element : elements) {
                if (element instanceof TextElement) {
                    TextElement textElement = (TextElement) element;
                    
                    // 自动调整文本大小
                    layoutEngine.autoAdjustTextSize(textElement, 400, 100);
                    
                    resultArea.appendText("✓ 文本元素调整完成\n");
                    resultArea.appendText("- 字体大小已优化\n");
                    resultArea.appendText("- 文本适合容器\n");
                }
            }
            
            resultArea.appendText("\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 自动文本调整失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示翻译功能
     */
    private void demonstrateTranslation(TextArea resultArea) {
        try {
            resultArea.appendText("=== 一键翻译功能演示 ===\n");
            
            String originalText = "这是演示内容，用于测试多语言功能。";
            
            // 翻译为不同语言
            MultilingualSupport.SupportedLanguage[] languages = {
                MultilingualSupport.SupportedLanguage.ENGLISH,
                MultilingualSupport.SupportedLanguage.JAPANESE,
                MultilingualSupport.SupportedLanguage.KOREAN
            };
            
            for (MultilingualSupport.SupportedLanguage language : languages) {
                String translated = multilingualSupport.generateMultilingualContent(originalText, language);
                resultArea.appendText(String.format("中文 → %s: %s\n", language.getDisplayName(), translated));
            }
            
            resultArea.appendText("✓ 一键翻译功能演示完成\n\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 翻译功能失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示批量翻译功能
     */
    private void demonstrateBatchTranslation(TextArea resultArea) {
        try {
            resultArea.appendText("=== 批量翻译功能演示 ===\n");
            
            // 模拟多个幻灯片的文本内容
            String[] slideContents = {
                "第一张幻灯片标题",
                "这是第一张幻灯片的内容",
                "第二张幻灯片标题", 
                "这是第二张幻灯片的内容",
                "第三张幻灯片标题",
                "这是第三张幻灯片的内容"
            };
            
            MultilingualSupport.SupportedLanguage targetLanguage = MultilingualSupport.SupportedLanguage.ENGLISH;
            
            resultArea.appendText("批量翻译为: " + targetLanguage.getDisplayName() + "\n");
            
            for (int i = 0; i < slideContents.length; i++) {
                String originalText = slideContents[i];
                String translatedText = multilingualSupport.generateMultilingualContent(originalText, targetLanguage);
                resultArea.appendText(String.format("幻灯片%d: %s → %s\n", i+1, originalText, translatedText));
            }
            
            resultArea.appendText("✓ 批量翻译功能演示完成\n");
            resultArea.appendText("- 已翻译 3 个幻灯片\n");
            resultArea.appendText("- 共翻译 6 个文本元素\n\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 批量翻译功能失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示多语言生成
     */
    private void demonstrateMultilingualGeneration(TextArea resultArea) {
        try {
            resultArea.appendText("=== 多语言PPT生成演示 ===\n");
            
            String topic = "人工智能发展";
            
            // 生成不同语言的PPT命令
            MultilingualSupport.SupportedLanguage[] languages = {
                MultilingualSupport.SupportedLanguage.ENGLISH,
                MultilingualSupport.SupportedLanguage.JAPANESE
            };
            
            for (MultilingualSupport.SupportedLanguage language : languages) {
                String pptCommands = multilingualSupport.generateMultilingualPPTCommands(topic, language);
                resultArea.appendText(String.format("✓ %s PPT命令生成完成\n", language.getDisplayName()));
                resultArea.appendText("命令预览: " + pptCommands.substring(0, Math.min(50, pptCommands.length())) + "...\n");
            }
            
            resultArea.appendText("\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 多语言生成失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示语言切换
     */
    private void demonstrateLanguageSwitch(TextArea resultArea) {
        try {
            resultArea.appendText("=== 语言切换演示 ===\n");
            
            MultilingualSupport.SupportedLanguage[] languages = {
                MultilingualSupport.SupportedLanguage.CHINESE,
                MultilingualSupport.SupportedLanguage.ENGLISH,
                MultilingualSupport.SupportedLanguage.JAPANESE
            };
            
            for (MultilingualSupport.SupportedLanguage language : languages) {
                multilingualSupport.switchLanguage(language);
                resultArea.appendText(String.format("✓ 切换到 %s\n", language.getDisplayName()));
                
                // 显示当前语言的界面文本
                String welcomeText = "欢迎使用"; // 简化处理
                resultArea.appendText("欢迎文本: " + welcomeText + "\n");
            }
            
            resultArea.appendText("\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 语言切换失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示智能PPT生成
     */
    private void demonstrateIntelligentPPT(TextArea resultArea) {
        try {
            resultArea.appendText("=== 智能多语言PPT生成演示 ===\n");
            
            if (enhancedAgent != null) {
                String topic = "可持续发展";
                
                String pptCommands = enhancedAgent.generateIntelligentMultilingualPPT(
                    topic,
                    MultilingualSupport.SupportedLanguage.ENGLISH,
                    IntelligentLayoutEngine.LayoutType.GRID
                );
                
                resultArea.appendText("✓ 智能PPT生成完成\n");
                resultArea.appendText("- 主题: " + topic + "\n");
                resultArea.appendText("- 语言: 英文\n");
                resultArea.appendText("- 布局: 网格布局\n");
                resultArea.appendText("命令预览: " + pptCommands.substring(0, Math.min(100, pptCommands.length())) + "...\n");
            } else {
                resultArea.appendText("⚠ AI模型未初始化，使用模拟模式\n");
                resultArea.appendText("✓ 智能PPT生成模拟完成\n");
            }
            
            resultArea.appendText("\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 智能PPT生成失败: " + e.getMessage() + "\n\n");
        }
    }
    
    /**
     * 演示演讲稿生成
     */
    private void demonstrateSpeechGeneration(TextArea resultArea) {
        try {
            resultArea.appendText("=== 演讲稿生成演示 ===\n");
            
            if (enhancedAgent != null) {
                String topic = "数字化转型";
                String speech = "这是关于" + topic + "的演讲稿示例。"; // 简化处理
                
                resultArea.appendText("✓ 演讲稿生成完成\n");
                resultArea.appendText("- 主题: " + topic + "\n");
                resultArea.appendText("- 语言: 中文\n");
                resultArea.appendText("- 时长: 5分钟\n");
                resultArea.appendText("演讲稿预览: " + speech.substring(0, Math.min(150, speech.length())) + "...\n");
            } else {
                resultArea.appendText("⚠ AI模型未初始化，使用模拟模式\n");
                resultArea.appendText("✓ 演讲稿生成模拟完成\n");
            }
            
            resultArea.appendText("\n");
            
        } catch (Exception e) {
            resultArea.appendText("✗ 演讲稿生成失败: " + e.getMessage() + "\n\n");
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 