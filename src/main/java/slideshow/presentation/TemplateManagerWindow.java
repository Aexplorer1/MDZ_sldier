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
    private ComboBox<TemplateCategory> categoryFilter;
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
        stage.setTitle("Prompt Template Management");
        stage.setWidth(1000);
        stage.setHeight(700);

        // Main layout
        BorderPane mainLayout = new BorderPane();

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

        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("Search templates...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchTemplates(newValue);
        });

        // 分类过滤器
        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll(TemplateCategory.values());
        categoryFilter.getItems().add(0, null);
        categoryFilter.setValue(null);
        categoryFilter.setPromptText("Select Category");
        categoryFilter.setOnAction(e -> filterByCategory());

        // 按钮
        Button newButton = new Button("New Template");
        newButton.setOnAction(e -> createNewTemplate());

        Button importButton = new Button("Import");
        importButton.setOnAction(e -> importTemplates());

        Button exportButton = new Button("Export");
        exportButton.setOnAction(e -> exportTemplates());

        Button backupButton = new Button("Backup");
        backupButton.setOnAction(e -> backupTemplates());

        toolbar.getChildren().addAll(
                new Label("Search:"), searchField,
                new Label("Category:"), categoryFilter,
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
        Label listLabel = new Label("Template List");
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

        // 模板详情区域
        VBox detailsPanel = createDetailsPanel();
        VBox.setVgrow(detailsPanel, Priority.ALWAYS);

        // 操作按钮
        HBox buttonPanel = createButtonPanel();

        rightPanel.getChildren().addAll(detailsPanel, buttonPanel);

        return rightPanel;
    }

    private VBox createDetailsPanel() {
        VBox detailsPanel = new VBox(10);
        detailsPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");
        detailsPanel.setPadding(new Insets(15));

        // 模板名称
        Label nameLabel = new Label("Template Name:");
        templateNameField = new TextField();
        templateNameField.setPromptText("Enter template name");

        // 模板描述
        Label descLabel = new Label("Template Description:");
        templateDescriptionArea = new TextArea();
        templateDescriptionArea.setPromptText("Enter template description");
        templateDescriptionArea.setPrefRowCount(3);

        // 模板分类
        Label categoryLabel = new Label("Template Category:");
        templateCategoryCombo = new ComboBox<>();
        templateCategoryCombo.getItems().addAll(TemplateCategory.values());

        // 模板内容
        Label contentLabel = new Label("Template Content:");
        templateContentArea = new TextArea();
        templateContentArea.setPromptText("Enter template content, use {0}, {1} as placeholders");
        templateContentArea.setPrefRowCount(10);

        // 标签
        Label tagLabel = new Label("Tags:");
        tagField = new TextField();
        tagField.setPromptText("Enter tags, separated by commas");

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

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveTemplate());

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> deleteTemplate());

        Button duplicateButton = new Button("Duplicate");
        duplicateButton.setOnAction(e -> duplicateTemplate());

        Button rateButton = new Button("Rate");
        rateButton.setOnAction(e -> rateTemplate());

        Button favoriteButton = new Button("Favorite");
        favoriteButton.setOnAction(e -> toggleFavorite());

        Button clearButton = new Button("Clear");
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

    private void filterByCategory() {
        TemplateCategory category = categoryFilter.getValue();
        if (category == null) {
            loadTemplates();
        } else {
            List<PromptTemplate> results = templateManager.getTemplatesByCategory(category);
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

            // 设置标签
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
            showAlert("Error", "Template name cannot be empty", Alert.AlertType.ERROR);
            return;
        }

        if (content.isEmpty()) {
            showAlert("Error", "Template content cannot be empty", Alert.AlertType.ERROR);
            return;
        }

        if (category == null) {
            showAlert("Error", "Please select a template category", Alert.AlertType.ERROR);
            return;
        }

        // 处理标签
        String[] tags = tagField.getText().split(",");

        // 检查是否是编辑现有模板
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();

        boolean success;
        if (selectedTemplate != null) {
            // 更新现有模板
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
            // 创建新模板
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
            showAlert("Success", "Template saved successfully", Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("Error", "Failed to save template", Alert.AlertType.ERROR);
        }
    }

    private void deleteTemplate() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("Error", "Please select a template to delete", Alert.AlertType.ERROR);
            return;
        }

        Alert alert;
        if (selectedTemplate.isDefault()) {
            // For default templates, show special confirmation dialog
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete Default Template");
            alert.setHeaderText("Delete Default Template");
            alert.setContentText("You are trying to delete the default template \"" + selectedTemplate.getName()
                    + "\".\n\n" +
                    "Default templates are important system presets. Deleting them may affect system functionality.\n" +
                    "Are you sure you want to delete this default template?");
        } else {
            // For regular templates, show standard confirmation dialog
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Delete Template");
            alert.setContentText("Are you sure you want to delete template \"" + selectedTemplate.getName() + "\"?");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = templateManager.deleteTemplate(selectedTemplate.getId());
            if (success) {
                showAlert("Success", "Template deleted successfully", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
                clearForm();
            } else {
                showAlert("Error", "Failed to delete template", Alert.AlertType.ERROR);
            }
        }
    }

    private void duplicateTemplate() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("Error", "Please select a template to duplicate", Alert.AlertType.ERROR);
            return;
        }

        // 创建副本
        PromptTemplate duplicate = new PromptTemplate(
                selectedTemplate.getName() + " (Copy)",
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
            showAlert("Success", "Template duplicated successfully", Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("Error", "Failed to duplicate template", Alert.AlertType.ERROR);
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
            showAlert("Error", "Please select a template to rate", Alert.AlertType.ERROR);
            return;
        }

        // 创建评分对话框
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Rate Template");
        dialog.setHeaderText("Rate template: " + selectedTemplate.getName());
        dialog.setContentText("Please select a rating (1-5 stars):");

        // 设置按钮
        ButtonType rateButtonType = new ButtonType("Rate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rateButtonType, ButtonType.CANCEL);

        // 创建评分选择器
        ComboBox<Double> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);
        ratingCombo.setValue(5.0);
        ratingCombo.setEditable(false);

        // 创建星级显示
        Label starLabel = new Label("*****");
        starLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: gold; -fx-font-weight: bold;");

        // 星级变化监听
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
        content.getChildren().addAll(new Label("Rating:"), ratingCombo, starLabel);
        dialog.getDialogPane().setContent(content);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == rateButtonType) {
                return ratingCombo.getValue();
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(rating -> {
            boolean success = templateManager.rateTemplate(selectedTemplate.getId(), rating);
            if (success) {
                showAlert("Success", "Template rated successfully: " + rating + " stars", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
            } else {
                showAlert("Error", "Failed to rate template", Alert.AlertType.ERROR);
            }
        });
    }

    private void toggleFavorite() {
        PromptTemplate selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert("Error", "Please select a template to toggle favorite", Alert.AlertType.ERROR);
            return;
        }

        boolean success = templateManager.toggleFavorite(selectedTemplate.getId());
        if (success) {
            boolean isFavorite = selectedTemplate.getMetadata().isFavorite();
            String message = isFavorite ? "Template added to favorites" : "Template removed from favorites";
            showAlert("Success", message, Alert.AlertType.INFORMATION);
            loadTemplates();
            updateStatistics();
        } else {
            showAlert("Error", "Failed to toggle favorite status", Alert.AlertType.ERROR);
        }
    }

    private void importTemplates() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select template file to import");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            boolean success = templateManager.restoreTemplates(file.getAbsolutePath());
            if (success) {
                showAlert("Success", "Templates imported successfully", Alert.AlertType.INFORMATION);
                loadTemplates();
                updateStatistics();
            } else {
                showAlert("Error", "Failed to import templates", Alert.AlertType.ERROR);
            }
        }
    }

    private void exportTemplates() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select export location");
        fileChooser.setInitialFileName("templates_export.json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // 直接使用JsonTemplateStorage的功能
                List<PromptTemplate> allTemplates = templateManager.getAllTemplates();
                if (allTemplates.isEmpty()) {
                    showAlert("Warning", "No templates to export", Alert.AlertType.WARNING);
                    return;
                }

                String json = new GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .create()
                        .toJson(allTemplates);

                java.nio.file.Path filePath = java.nio.file.Paths.get(file.getAbsolutePath());
                java.nio.file.Files.write(filePath, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                showAlert("Success", "Templates exported successfully to: " + file.getName(),
                        Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Error", "Failed to export templates: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void backupTemplates() {
        boolean success = templateManager.backupTemplates("backups");
        if (success) {
            showAlert("Success", "Templates backed up successfully", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Error", "Failed to backup templates", Alert.AlertType.ERROR);
        }
    }

    private void updateStatistics() {
        TemplateStatistics stats = templateManager.getStatistics();
        String statsText = String.format(
                "Total: %d | Default: %d | Favorite: %d | Avg Rating: %.1f | Total Usage: %d",
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

    public void show() {
        stage.show();
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
                        "Usage: %d | Rating: %.1f",
                        item.getMetadata().getUseCount(),
                        item.getMetadata().getAverageRating()));
                statsLabel.setFont(Font.font("System", 9));
                statsLabel.setStyle("-fx-text-fill: #999;");

                // 添加收藏状态显示
                HBox statusBox = new HBox(5);
                if (item.getMetadata().isFavorite()) {
                    Label favoriteLabel = new Label("[FAV]");
                    favoriteLabel.setStyle("-fx-text-fill: gold; -fx-font-weight: bold; -fx-font-size: 9px;");
                    statusBox.getChildren().add(favoriteLabel);
                }

                // 添加评分星级显示
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