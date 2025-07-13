package slideshow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;

public class PromptGeneratorDialog extends Stage {
    private final AIAgent aiAgent;
    private static String pendingTemplateContent = null; // 用于跨窗口传递内容

    public static String getPendingTemplateContent() {
        return pendingTemplateContent;
    }
    public static void clearPendingTemplateContent() {
        pendingTemplateContent = null;
    }

    public PromptGeneratorDialog(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
        setTitle("智能生成提示词");
        setMinWidth(500);
        setMinHeight(480);
        initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        // 具体主题输入
        Label topicLabel = new Label("具体主题（必填）:");
        TextField topicField = new TextField();
        topicField.setPromptText("请输入PPT具体主题，如：人工智能在教育中的应用");

        // PPT使用场景
        Label sceneLabel = new Label("PPT使用场景（必选）:");
        ComboBox<String> sceneCombo = new ComboBox<>();
        sceneCombo.getItems().addAll("学术汇报", "商务路演", "教学课件", "产品发布", "年终总结");
        sceneCombo.setPromptText("请选择PPT使用场景");

        // PPT风格
        Label styleLabel = new Label("PPT风格（必选）:");
        ComboBox<String> styleCombo = new ComboBox<>();
        styleCombo.getItems().addAll("正式", "活泼", "极简", "创意", "科技感");
        styleCombo.setPromptText("请选择PPT风格");

        // PPT文本密度
        Label densityLabel = new Label("PPT文本密度（必选）:");
        ComboBox<String> densityCombo = new ComboBox<>();
        densityCombo.getItems().addAll("简略", "适中", "详细");
        densityCombo.setPromptText("请选择文本密度");

        // 生成按钮
        Button generateBtn = new Button("生成提示词");
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        // 保存至本地按钮
        Button saveBtn = new Button("保存至本地模板库");
        saveBtn.setDisable(true);

        // 结果展示
        TextArea resultArea = new TextArea();
        resultArea.setPromptText("AI生成的提示词将在这里显示，可复制");
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setPrefRowCount(7);

        // 复制按钮
        Button copyBtn = new Button("复制");
        copyBtn.setDisable(true);

        // 关闭按钮
        Button closeBtn = new Button("关闭");

        HBox btnBox = new HBox(10, generateBtn, saveBtn, copyBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
                topicLabel, topicField,
                sceneLabel, sceneCombo,
                styleLabel, styleCombo,
                densityLabel, densityCombo,
                btnBox,
                resultArea
        );

        Scene scene = new Scene(root);
        setScene(scene);

        // 生成按钮逻辑
        generateBtn.setOnAction(e -> {
            String topicVal = topicField.getText();
            String sceneVal = sceneCombo.getValue();
            String styleVal = styleCombo.getValue();
            String densityVal = densityCombo.getValue();
            if (topicVal == null || topicVal.trim().isEmpty() || sceneVal == null || styleVal == null || densityVal == null) {
                resultArea.setText("请先填写所有必填项！");
                copyBtn.setDisable(true);
                saveBtn.setDisable(true);
                return;
            }
            resultArea.setText("AI正在生成提示词，请稍候...");
            copyBtn.setDisable(true);
            saveBtn.setDisable(true);
            new Thread(() -> {
                String prompt = String.format(
                        "请为如下需求生成适合大模型的PPT提示词：\n" +
                        "1. 主题：%s\n" +
                        "2. 使用场景：%s\n" +
                        "3. 风格：%s\n" +
                        "4. 文本密度：%s\n" +
                        "要求：提示词应详细描述PPT的结构、内容、风格和文本详略，便于AI生成高质量PPT命令。不要输出无关内容，仅输出生成的提示词。",
                        topicVal, sceneVal, styleVal, densityVal
                );
                try {
                    String aiResult = aiAgent.askAI(prompt);
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText(aiResult);
                        boolean enable = aiResult != null && !aiResult.trim().isEmpty();
                        copyBtn.setDisable(!enable);
                        saveBtn.setDisable(!enable);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText("AI生成失败：" + ex.getMessage());
                        copyBtn.setDisable(true);
                        saveBtn.setDisable(true);
                    });
                }
            }).start();
        });

        // 复制按钮逻辑
        copyBtn.setOnAction(e -> {
            String text = resultArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        // 保存至本地按钮逻辑
        saveBtn.setOnAction(e -> {
            String text = resultArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                // 1. 复制到剪贴板
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
                // 2. 设置静态变量，供模板管理窗口粘贴
                pendingTemplateContent = text;
                // 3. 打开模板管理窗口
                Main.openTemplateManager();
                // 4. 关闭本窗口
                close();
            }
        });

        // 关闭按钮逻辑
        closeBtn.setOnAction(e -> close());
    }
} 