package slideshow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FeedbackDialog extends Stage {
    private final List<File> attachments = new ArrayList<>();
    private final ObservableList<AttachmentInfo> attachmentInfos = FXCollections.observableArrayList();
    private static class AttachmentInfo {
        File file;
        String name;
        String time;
        String size;
        AttachmentInfo(File file) {
            this.file = file;
            this.name = file.getName();
            this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.size = readableFileSize(file.length());
        }
        static String readableFileSize(long size) {
            if (size <= 0) return "0 B";
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }
        @Override
        public String toString() {
            return name + " | " + size + " | " + time;
        }
    }
    public FeedbackDialog() {
        setTitle("用户反馈");
        setMinWidth(400);
        setMinHeight(380);
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

        // 附件相关
        Button attachBtn = new Button("添加附件");
        Button removeBtn = new Button("删除附件");
        ListView<AttachmentInfo> attachList = new ListView<>(attachmentInfos);
        attachList.setPrefHeight(80);
        attachList.setPlaceholder(new Label("未添加附件"));

        attachBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择附件");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("视频", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.mkv"),
                new FileChooser.ExtensionFilter("文本", "*.txt", "*.md", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
            );
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            List<File> files = fileChooser.showOpenMultipleDialog(this);
            if (files != null && !files.isEmpty()) {
                for (File f : files) {
                    boolean exists = false;
                    for (AttachmentInfo info : attachmentInfos) {
                        if (info.file.equals(f)) { exists = true; break; }
                    }
                    if (!exists) {
                        attachments.add(f);
                        attachmentInfos.add(new AttachmentInfo(f));
                    }
                }
            }
        });
        removeBtn.setOnAction(e -> {
            AttachmentInfo selected = attachList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                attachments.remove(selected.file);
                attachmentInfos.remove(selected);
            }
        });

        Button submitBtn = new Button("提交反馈");
        Button closeBtn = new Button("关闭");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #d9534f;");

        HBox btnBox = new HBox(10, submitBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        HBox attachBtnBox = new HBox(10, attachBtn, removeBtn);
        attachBtnBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(descLabel, descArea, contactLabel, contactField, attachBtnBox, attachList, btnBox, statusLabel);

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
                    sendFeedbackMail(desc, contact, attachments);
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

    // 邮件发送逻辑，支持附件
    private void sendFeedbackMail(String desc, String contact, List<File> attachments) throws Exception {
        String host = "smtp.qq.com";
        String from = "1770453335@qq.com"; // 发件邮箱
        String authCode = "fwibqwzyenxqgfja";    // MTP授权码
        String to = "1770453335@qq.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.fallback", "false");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(from, authCode);
            }
        });

        javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
        message.setFrom(new javax.mail.internet.InternetAddress(from));
        message.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(to));
        message.setSubject("【用户反馈】PPT生成器");

        // 邮件正文
        javax.mail.internet.MimeBodyPart textPart = new javax.mail.internet.MimeBodyPart();
        textPart.setText("问题描述：\n" + desc + "\n\n联系方式：" + contact);

        // 附件部分
        javax.mail.internet.MimeMultipart multipart = new javax.mail.internet.MimeMultipart();
        multipart.addBodyPart(textPart);
        if (attachments != null) {
            for (File file : attachments) {
                javax.mail.internet.MimeBodyPart attachPart = new javax.mail.internet.MimeBodyPart();
                attachPart.attachFile(file);
                multipart.addBodyPart(attachPart);
            }
        }
        message.setContent(multipart);

        javax.mail.Transport.send(message);
    }
} 