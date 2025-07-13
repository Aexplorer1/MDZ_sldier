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

public class PromptGeneratorDialog extends Stage {
    private final AIAgent aiAgent;

    public PromptGeneratorDialog(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
        setTitle("智能生成提示词");
        setMinWidth(500);
        setMinHeight(400);
        initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

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

        HBox btnBox = new HBox(10, generateBtn, copyBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
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
            String sceneVal = sceneCombo.getValue();
            String styleVal = styleCombo.getValue();
            String densityVal = densityCombo.getValue();
            if (sceneVal == null || styleVal == null || densityVal == null) {
                resultArea.setText("请先选择所有必选项！");
                copyBtn.setDisable(true);
                return;
            }
            resultArea.setText("AI正在生成提示词，请稍候...");
            copyBtn.setDisable(true);
            new Thread(() -> {
                String prompt = String.format(
                        "请为如下需求生成适合大模型的PPT提示词：\n" +
                        "1. 使用场景：%s\n" +
                        "2. 风格：%s\n" +
                        "3. 文本密度：%s\n" +
                        "要求：提示词应详细描述PPT的结构、内容、风格和文本详略，便于AI生成高质量PPT命令。输出内容应适合直接作为AI大模型输入。",
                        sceneVal, styleVal, densityVal
                );
                try {
                    String aiResult = aiAgent.askAI(prompt);
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText(aiResult);
                        copyBtn.setDisable(aiResult == null || aiResult.trim().isEmpty());
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText("AI生成失败：" + ex.getMessage());
                        copyBtn.setDisable(true);
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

        // 关闭按钮逻辑
        closeBtn.setOnAction(e -> close());
    }
} 