package slideshow.presentation;

// Ensure file uses UTF-8 encoding

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import slideshow.model.PromptTemplate;
import slideshow.model.TemplateCategory;
import slideshow.util.TemplateManager;
import slideshow.util.TemplateManager.TemplateStatistics;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.google.gson.GsonBuilder;

/**
 * Template Management Window
 * Provides template CRUD operations, search, import/export functionality
 */
public class TemplateManagerWindow {
    private static final Logger logger = Logger.getLogger(TemplateManagerWindow.class.getName());

    private Stage stage;
    private TemplateManager templateManager;
    private ObservableList<PromptTemplate> templateList;
    private ListView<PromptTemplate> templateListView;
    private TextField searchField;
    // 删除与PPT大纲相关的分类、过滤、显示等所有代码
    private TextArea templateContentArea;
    private TextField templateNameField;
    private TextArea templateDescriptionArea;
    private ComboBox<TemplateCategory> templateCategoryCombo;
    private TextField tagField;
    private Label statisticsLabel;

    public TemplateManagerWindow() {
        this.templateManager = new TemplateManager();
        this.templateList = FXCollections.observableArrayList();
        initializeUI();
        loadTemplates();
        updateStatistics();
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("本地提示词模板管理");
        stage.setWidth(1000);
        stage.setHeight(700);

        // Main layout
        BorderPane mainLayout = new BorderPane();

        // 插入模板管理系统使用说明
        Label usageGuide = new Label("使用说明：\n1. 可新建、导入、导出和备份提示词模板。\n2. 点击左侧模板列表可查看和编辑模板内容。\n3. 支持按名称搜索模板。\n4. 编辑完成后请点击保存。\n5. 删除模板请谨慎操作，删除后无法恢复。\n6. 模板内容字段请遵循Title、Subtitle、Bullet、Text、Draw等英文格式，便于正则分析。");
        usageGuide.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-padding: 10 0 10 0;");
        mainLayout.setTop(usageGuide);

        // Top toolbar
        HBox toolbar = createToolbar();
        mainLayout.setTop(toolbar);

        // Left template list
        VBox leftPanel = createLeftPanel();
        mainLayout.setLeft(leftPanel);

        // Right edit area
        VBox rightPanel = createRightPanel();
        mainLayout.setCenter(rightPanel);

        // Bottom status bar
        HBox statusBar = createStatusBar();
        mainLayout.setBottom(statusBar);

        Scene scene = new Scene(mainLayout);
        stage.setScene(scene);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #f0f0f0;");

        // 搜索
        searchField = new TextField();
        searchField.setPromptText("搜索模板...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchTemplates(newValue);
        });

        // 新建
        Button newButton = new Button("新建模板");
        newButton.setOnAction(e -> createNewTemplate());

        Button importButton = new Button("导入");
        importButton.setOnAction(e -> importTemplates());

        Button exportButton = new Button("导出");
        exportButton.setOnAction(e -> exportTemplates());

        Button backupButton = new Button("备份");
        backupButton.setOnAction(e -> backupTemplates());

        // 添加“使用说明”按钮
        Button helpButton = new Button("使用说明");
        helpButton.setOnAction(e -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("模板管理系统使用说明");
            dialog.setHeaderText(null);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

            Label title = new Label("模板管理系统使用说明");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a2a2a; -fx-padding: 0 0 10 0;");

            Label content = new Label(
                "提示词模板是用户可自定义的用于生成PPT大纲的提示词。用户可在“AI功能”中的“AI智能生成PPT”窗口中使用自定义的提示词模板替代程序的内置提示词模板\n\n" +
                "1. 用户可新建、导入、导出和备份提示词模板，并对模板进行收藏，打分等操作。\n" +
                "2. 点击左侧模板列表可查看和编辑模板内容。\n" +
                "3. 支持按名称搜索模板。\n" +
                "4. 编辑完成后请点击保存。\n" +
                "5. 删除模板请谨慎操作，删除后无法恢复。\n" +
                "6. 模板内容字段请遵循Title、Subtitle、Bullet、Text、Draw等英文格式，便于正则分析。具体模板定义格式请参考程序给出的参考模板。"
            );
            content.setStyle("-fx-font-size: 15px; -fx-text-fill: #333; -fx-padding: 0 0 0 0;");
            content.setWrapText(true);

            VBox vbox = new VBox(10, title, content);
            vbox.setPadding(new Insets(20, 30, 20, 30));
            vbox.setPrefWidth(480);
            dialog.getDialogPane().setContent(vbox);
            dialog.showAndWait();
        });
        // 将按钮加入工具栏
        toolbar.getChildren().add(helpButton);

        toolbar.getChildren().addAll(
                new Label("搜索:"), searchField,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                newButton, importButton, exportButton, backupButton);

        return toolbar;
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(300);
        leftPanel.setStyle("-fx-background-color: #f8f8f8;");

        // 模板列表
        Label listLabel = new Label("提示词模板列表");
        listLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        templateListView = new ListView<>();
        templateListView.setItems(templateList);
        templateListView.setCellFactory(param -> new TemplateListCell());
        templateListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadTemplateDetails(newValue);
                    }
                });

        VBox.setVgrow(templateListView, Priority.ALWAYS);
        leftPanel.getChildren().addAll(listLabel, templateListView);

        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(10));

        // 模板详情
        VBox detailsPanel = createDetailsPanel();
        VBox.setVgrow(detailsPanel, Priority.ALWAYS);

        // 按钮面板
        HBox buttonPanel = createButtonPanel();

        rightPanel.getChildren().addAll(detailsPanel, buttonPanel);

        return rightPanel;
    }

    private VBox createDetailsPanel() {
        VBox detailsPanel = new VBox(10);
        detailsPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");
        detailsPanel.setPadding(new Insets(15));

        // 模板名称
        Label nameLabel = new Label("提示词模板名称:");
        templateNameField = new TextField();
        templateNameField.setPromptText("请输入提示词模板名称");

        // 模板描述
        Label descLabel = new Label("提示词模板描述:");
        templateDescriptionArea = new TextArea();
        templateDescriptionArea.setPromptText("请输入提示词模板描述");
        templateDescriptionArea.setPrefRowCount(3);

        // 模板分类
        Label categoryLabel = new Label("提示词模板分类:");
        templateCategoryCombo = new ComboBox<>();
        templateCategoryCombo.getItems().addAll(TemplateCategory.values());

        // 模板内容
        Label contentLabel = new Label("提示词模板内容:");
        templateContentArea = new TextArea();
        templateContentArea.setPromptText("请输入提示词模板内容，可用{0}、{1}等占位符");
        templateContentArea.setPrefRowCount(10);

        // 标签
        Label tagLabel = new Label("提示词模板标签:");
        tagField = new TextField();
        tagField.setPromptText("请输入提示词模板标签，用逗号分隔");

        detailsPanel.getChildren().addAll(
                nameLabel, templateNameField,
                descLabel, templateDescriptionArea,
                categoryLabel, templateCategoryCombo,
                contentLabel, templateContentArea,
                tagLabel, tagField);

        return detailsPanel;
    }

    private HBox createButtonPanel() {
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("保存");
        saveButton.setOnAction(e -> saveTemplate());

        Button deleteButton = new Button("删除");
        deleteButton.setOnAction(e -> deleteTemplate());

        Button duplicateButton = new Button("复制");
        duplicateButton.setOnAction(e -> duplicateTemplate());

        Button rateButton = new Button("评分");
        rateButton.setOnAction(e -> rateTemplate());

        Button favoriteButton = new Button("收藏");
        favoriteButton.setOnAction(e -> toggleFavorite());

        Button clearButton = new Button("清空");
        clearButton.setOnAction(e -> clearForm());

        buttonPanel.getChildren().addAll(saveButton, deleteButton, duplicateButton, rateButton, favoriteButton,
                clearButton);

        return buttonPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");

        statisticsLabel = new Label();
        statusBar.getChildren().add(statisticsLabel);

        return statusBar;
    }

    private void loadTemplates() {
        List<PromptTemplate> templates = templateManager.getAllTemplates();
        Platform.runLater(() -> {
            templateList.clear();
            templateList.addAll(templates);
        });
    }

    private void searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            loadTemplates();
        } else {
            List<PromptTemplate> results = templateManager.searchTemplates(keyword);
            Platform.runLater(() -> {
                templateList.clear();
                templateList.addAll(results);
            });
        }
    }

    private void loadTemplateDetails(PromptTemplate template) {
        if (template != null) {
            templateNameField.setText(template.getName());
            templateDescriptionArea.setText(template.getDescription());
            templateCategoryCombo.setValue(template.getCategory());
            templateContentArea.setText(template.getContent());

            // 标签
            String tags = String.join(", ", template.getTags());
            tagField.setText(tags);
        }
    }

    private void createNewTemplate() {
        clearForm();
        templateNameField.requestFocus();
    }

    private void saveTemplate() {
        String name = templateNameField.getText().trim();
        String description = templateDescriptionArea.getText().trim();
        String content = templateContentArea.getText().trim();
        TemplateCategory category = templateCategoryCombo.getValue();

        if (name.isEmpty()) {
            showAlert("错误", "模板名称不能为空", Alert.AlertType.ERROR);
            return;
        }

        if (content.isEmpty()) {
            showAlert("错误", "模板内容不能为空", Alert.AlertType.ERROR);
            return;
        }

        if (category == null) {
            showAlert("错误", "请选择模板分类", Alert.AlertType.ERROR);
            return;
        }

        // 标签
        String[] tags = tagField.getText().split(",");

        // 是否为编辑模板
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();

        boolean success;
        if (selectedTemplate != null) {
            // 更新模板
            success = templateManager.updateTemplate(
                    selectedTemplate.getId(), name, description, content, category);

            // 更新标签
            if (success) {
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        templateManager.addTag(selectedTemplate.getId(), trimmedTag);
                    }
                }
            }
        } else {
            // 新建模板
            success = templateManager.createTemplate(name, description, content, category);

            // 添加标签
            if (success) {
                Optional<PromptTemplate> newTemplate = templateManager.getTemplateByName(name);
                if (newTemplate.isPresent()) {
                    for (String tag : tags) {
                        String trimmedTag = tag.trim();
                        if (!trimmedTag.isEmpty()) {
                            templateManager.addTag(newTemplate.get().getId(), trimmedTag);
                        }
                    }
                }
            }
        }

        if (success) {
            showAlert("成功", "模板保存成功", Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("错误", "模板保存失败", Alert.AlertType.ERROR);
        }
    }

    private void deleteTemplate() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("错误", "请选择要删除的模板", Alert.AlertType.ERROR);
            return;
        }

        Alert alert;
        if (selectedTemplate.isDefault()) {
            // 对于默认模板，显示特殊确认对话框
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除默认模板");
            alert.setHeaderText("删除默认模板");
            alert.setContentText("您正在尝试删除默认模板 \"" + selectedTemplate.getName()
                    + "\"。\n\n" +
                    "默认模板是系统预设的重要组成部分。删除它们可能会影响系统功能。\n" +
                    "您确定要删除此默认模板吗？");
        } else {
            // 对于普通模板，显示标准确认对话框
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除模板");
            alert.setContentText("您确定要删除模板 \"" + selectedTemplate.getName() + "\"?");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = templateManager.deleteTemplate(selectedTemplate.getId());
            if (success) {
                showAlert("成功", "模板删除成功", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
                clearForm();
            } else {
                showAlert("错误", "模板删除失败", Alert.AlertType.ERROR);
            }
        }
    }

    private void duplicateTemplate() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("错误", "请选择要复制的模板", Alert.AlertType.ERROR);
            return;
        }

        // 复制模板
        PromptTemplate duplicate = new PromptTemplate(
                selectedTemplate.getName() + " (复制)",
                selectedTemplate.getDescription(),
                selectedTemplate.getContent(),
                selectedTemplate.getCategory());

        // 复制标签
        for (String tag : selectedTemplate.getTags()) {
            duplicate.addTag(tag);
        }

        boolean success = templateManager.createTemplate(
                duplicate.getName(),
                duplicate.getDescription(),
                duplicate.getContent(),
                duplicate.getCategory());
        if (success) {
            showAlert("成功", "模板复制成功", Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("错误", "模板复制失败", Alert.AlertType.ERROR);
        }
    }

    private void clearForm() {
        templateNameField.clear();
        templateDescriptionArea.clear();
        templateCategoryCombo.setValue(null);
        templateContentArea.clear();
        tagField.clear();
        templateListView.getSelectionModel().clearSelection();
    }

    private void rateTemplate() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("错误", "请选择要评分的模板", Alert.AlertType.ERROR);
            return;
        }

        // 评分对话框
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("评分模板");
        dialog.setHeaderText("评分模板: " + selectedTemplate.getName());
        dialog.setContentText("请选择评分 (1-5星):");

        // 确定按钮
        ButtonType rateButtonType = new ButtonType("评分", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rateButtonType, ButtonType.CANCEL);

        // 评分选择器
        ComboBox<Double> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);
        ratingCombo.setValue(5.0);
        ratingCombo.setEditable(false);

        // 星星显示
        Label starLabel = new Label("*****");
        starLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: gold; -fx-font-weight: bold;");

        // 评分变化监听
        ratingCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int stars = (int) Math.round(newVal);
                StringBuilder starText = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i < stars) {
                        starText.append("*");
                    } else {
                        starText.append("-");
                    }
                }
                starLabel.setText(starText.toString());
            }
        });

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("评分:"), ratingCombo, starLabel);
        dialog.getDialogPane().setContent(content);

        // 结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == rateButtonType) {
                return ratingCombo.getValue();
            }
            return null;
        });

        // 显示评分对话框
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(rating -> {
            boolean success = templateManager.rateTemplate(selectedTemplate.getId(), rating);
            if (success) {
                showAlert("成功", "模板评分成功: " + rating + "星", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
            } else {
                showAlert("错误", "模板评分失败", Alert.AlertType.ERROR);
            }
        });
    }

    private void toggleFavorite() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("错误", "请选择要切换收藏状态的模板", Alert.AlertType.ERROR);
            return;
        }

        boolean success = templateManager.toggleFavorite(selectedTemplate.getId());
        if (success) {
            boolean isFavorite = selectedTemplate.getMetadata().isFavorite();
            String message = isFavorite ? "模板已添加到收藏" : "模板已从收藏移除";
            showAlert("成功", message, Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("错误", "切换收藏状态失败", Alert.AlertType.ERROR);
        }
    }

    private void importTemplates() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要导入的模板文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件", "*.json"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            boolean success = templateManager.restoreTemplates(file.getAbsolutePath());
            if (success) {
                showAlert("成功", "模板导入成功", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
            } else {
                showAlert("错误", "模板导入失败", Alert.AlertType.ERROR);
            }
        }
    }

    private void exportTemplates() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择导出位置");
        fileChooser.setInitialFileName("templates_export.json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件", "*.json"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // 直接使用JsonTemplateStorage的导出功能
                List<PromptTemplate> allTemplates = templateManager.getAllTemplates();
                if (allTemplates.isEmpty()) {
                    showAlert("警告", "没有模板可导出", Alert.AlertType.WARNING);
                    return;
                }

                String json = new GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .create()
                        .toJson(allTemplates);

                java.nio.file.Path filePath = java.nio.file.Paths.get(file.getAbsolutePath());
                java.nio.file.Files.write(filePath, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                showAlert("成功", "模板导出成功到: " + file.getName(),
                        Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("错误", "模板导出失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void backupTemplates() {
        boolean success = templateManager.backupTemplates("backups");
        if (success) {
            showAlert("成功", "模板备份成功", Alert.AlertType.INFORMATION);
        } else {
            showAlert("错误", "模板备份失败", Alert.AlertType.ERROR);
        }
    }

    private void updateStatistics() {
        TemplateStatistics stats = templateManager.getStatistics();
        String statsText = String.format(
                "总数: %d | 默认: %d | 收藏: %d | 平均评分: %.1f | 总使用: %d",
                stats.getTotalCount(), stats.getDefaultCount(), stats.getFavoriteCount(),
                stats.getAverageRating(), stats.getTotalUseCount());
        statisticsLabel.setText(statsText);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void pasteContentToNewTemplate(String content) {
        createNewTemplate();
        if (content != null) {
            templateContentArea.setText(content);
        }
    }

    public void show() {
        stage.show();
        // 检查是否有待粘贴内容
        String pending = slideshow.PromptGeneratorDialog.getPendingTemplateContent();
        if (pending != null && !pending.trim().isEmpty()) {
            pasteContentToNewTemplate(pending);
            slideshow.PromptGeneratorDialog.clearPendingTemplateContent();
        }
    }

    /**
     * Custom list cell
     */
    private class TemplateListCell extends ListCell<PromptTemplate> {
        @Override
        protected void updateItem(PromptTemplate item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox cellContent = new VBox(2);

                Label nameLabel = new Label(item.getName());
                nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

                Label categoryLabel = new Label(item.getCategory().getDisplayName());
                categoryLabel.setFont(Font.font("System", 10));
                categoryLabel.setStyle("-fx-text-fill: #666;");

                Label statsLabel = new Label(String.format(
                        "使用: %d | 评分: %.1f",
                        item.getMetadata().getUseCount(),
                        item.getMetadata().getAverageRating()));
                statsLabel.setFont(Font.font("System", 9));
                statsLabel.setStyle("-fx-text-fill: #999;");

                // 状态显示
                HBox statusBox = new HBox(5);
                if (item.getMetadata().isFavorite()) {
                    Label favoriteLabel = new Label("[收藏]");
                    favoriteLabel.setStyle("-fx-text-fill: gold; -fx-font-weight: bold; -fx-font-size: 9px;");
                    statusBox.getChildren().add(favoriteLabel);
                }

                // 评分显示
                if (item.getMetadata().getRatingCount() > 0) {
                    double rating = item.getMetadata().getAverageRating();
                    int stars = (int) Math.round(rating);
                    StringBuilder starText = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        if (i < stars) {
                            starText.append("*");
                        } else {
                            starText.append("-");
                        }
                    }
                    Label ratingLabel = new Label(starText.toString());
                    ratingLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 10px; -fx-font-weight: bold;");
                    statusBox.getChildren().add(ratingLabel);
                }

                cellContent.getChildren().addAll(nameLabel, categoryLabel, statsLabel, statusBox);
                setGraphic(cellContent);
            }
        }
    }
}