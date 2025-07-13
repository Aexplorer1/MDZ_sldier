package slideshow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class FeedbackDialog extends Stage {
    public FeedbackDialog() {
        setTitle("用户反馈");
        setMinWidth(400);
        setMinHeight(320);
        initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        Label descLabel = new Label("问题描述（必填）:");
        TextArea descArea = new TextArea();
        descArea.setPromptText("请详细描述你遇到的问题或建议...");
        descArea.setPrefRowCount(6);

        Label contactLabel = new Label("联系方式（选填）:");
        TextField contactField = new TextField();
        contactField.setPromptText("如邮箱/微信号/QQ号");

        Button submitBtn = new Button("提交反馈");
        Button closeBtn = new Button("关闭");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #d9534f;");

        HBox btnBox = new HBox(10, submitBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(descLabel, descArea, contactLabel, contactField, btnBox, statusLabel);

        Scene scene = new Scene(root);
        setScene(scene);

        submitBtn.setOnAction(e -> {
            String desc = descArea.getText().trim();
            String contact = contactField.getText().trim();
            if (desc.isEmpty()) {
                statusLabel.setText("请填写问题描述！");
                return;
            }
            statusLabel.setText("正在发送，请稍候...");
            submitBtn.setDisable(true);

            new Thread(() -> {
                try {
                    sendFeedbackMail(desc, contact);
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: #218838;");
                        statusLabel.setText("反馈已成功发送，感谢您的支持！");
                        submitBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: #d9534f;");
                        statusLabel.setText("发送失败：" + ex.getMessage());
                        submitBtn.setDisable(false);
                    });
                }
            }).start();
        });

        closeBtn.setOnAction(e -> close());
    }

    // 邮件发送逻辑
    private void sendFeedbackMail(String desc, String contact) throws Exception {
        String host = "smtp.qq.com";
        String from = "1770453335@qq.com"; // 换成你的发件邮箱
        String authCode = "uunjsshnssfgbici";    // 换成你的SMTP授权码
        String to = "1770453335@qq.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, authCode);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("【用户反馈】PPT生成器");
        message.setText("问题描述：\n" + desc + "\n\n联系方式：" + contact);

        Transport.send(message);
    }
} 