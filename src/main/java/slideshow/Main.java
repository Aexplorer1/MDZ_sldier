package slideshow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.FontWeight;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ButtonBar;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import slideshow.util.Constants;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.ImageElement;
import slideshow.util.UIStrings;
import slideshow.util.SlideSerializer;
import slideshow.util.SlideParser;
import slideshow.presentation.PresentationWindow;
import slideshow.presentation.SpeakerViewWindow;
import slideshow.elements.DrawElement;
import dev.langchain4j.model.openai.OpenAiChatModel;
import slideshow.model.PromptTemplate;
import slideshow.util.IntelligentLayoutEngine;
import slideshow.util.MultilingualSupport;
import slideshow.AIEnhancedAgent;
import slideshow.util.SlideStructureAnalyzer;
import slideshow.util.SlideStructureAnalyzer.StructureAnalysis;
import slideshow.util.SpeechManager;
import slideshow.util.LogicGraphRenderer;
import slideshow.PromptGeneratorDialog;
import slideshow.FeedbackDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Stack;

/**
 * SlideMind main application class
 * Responsible for managing the user interface and slide editing functionality
 */
public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private Canvas canvas;
    private GraphicsContext graphicsContext;
    private Slide currentSlide;
    private SlideElement selectedElement;
    private double lastMouseX;
    private double lastMouseY;
    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = -1;
    private Button previousSlideButton;
    private Button nextSlideButton;
    private Label slideCountLabel;
    private SlideElement.ResizeHandle currentResizeHandle = SlideElement.ResizeHandle.NONE;
    private DrawElement.ShapeType currentShape = null;
    private DrawElement currentDrawing = null;
    private ColorPicker drawColorPicker;
    private ComboBox<Double> lineWidthComboBox;
    private ToggleGroup drawGroup;
    private OpenAiChatModel aiModel;
    private AIAgent aiAgent;

    // 主题色变量
    private String mainBgColor = "#a3d3b2";
    private String borderColor = "#7fcfa0";
    private String canvasBgColor = "#e6f4ea";
    private boolean isDarkTheme = false;

    // 主题相关控件声明
    private VBox topBox;
    private ToolBar toolBar;
    private VBox sidebar;
    private BorderPane canvasHolder;

    // ========== 撤销/重做/剪切/复制/粘贴相关成员 ==========
    // 快照式撤销/重做
    private static class SlidesSnapshot {
        List<slideshow.model.Slide> slides;
        int currentSlideIndex;
        SlidesSnapshot(List<slideshow.model.Slide> slides, int currentSlideIndex) {
            this.slides = new ArrayList<>();
            for (slideshow.model.Slide s : slides) {
                this.slides.add(s.deepClone());
            }
            this.currentSlideIndex = currentSlideIndex;
        }
    }
    private final Stack<SlidesSnapshot> undoStack = new Stack<>();
    private final Stack<SlidesSnapshot> redoStack = new Stack<>();
    private SlideElement clipboardElement = null;
    // 多元素剪贴板
    private List<SlideElement> clipboardElements = new ArrayList<>();

    // 1. 命令模式接口和实现
    private interface Command {
        void execute();
        void undo();
    }
    private class AddElementCommand implements Command {
        private Slide slide;
        private SlideElement element;
        public AddElementCommand(Slide slide, SlideElement element) {
            this.slide = slide;
            this.element = element;
        }
        public void execute() { slide.addElement(element); refreshCanvas(); }
        public void undo() { slide.removeElement(element); refreshCanvas(); }
    }
    private class RemoveElementCommand implements Command {
        private Slide slide;
        private SlideElement element;
        public RemoveElementCommand(Slide slide, SlideElement element) {
            this.slide = slide;
            this.element = element;
        }
        public void execute() { slide.removeElement(element); refreshCanvas(); }
        public void undo() { slide.addElement(element); refreshCanvas(); }
    }
    private class MoveElementCommand implements Command {
        private SlideElement element;
        private double oldX, oldY, newX, newY;
        public MoveElementCommand(SlideElement element, double oldX, double oldY, double newX, double newY) {
            this.element = element;
            this.oldX = oldX; this.oldY = oldY; this.newX = newX; this.newY = newY;
        }
        public void execute() { element.setPosition(newX, newY); refreshCanvas(); }
        public void undo() { element.setPosition(oldX, oldY); refreshCanvas(); }
    }
    private class EditTextCommand implements Command {
        private TextElement element;
        private String oldText, newText;
        public EditTextCommand(TextElement element, String oldText, String newText) {
            this.element = element;
            this.oldText = oldText; this.newText = newText;
        }
        public void execute() { element.setText(newText); refreshCanvas(); }
        public void undo() { element.setText(oldText); refreshCanvas(); }
    }
    private class RemoveElementsCommand implements Command {
        private Slide slide;
        private List<SlideElement> elements;
        public RemoveElementsCommand(Slide slide, List<SlideElement> elements) {
            this.slide = slide;
            this.elements = new ArrayList<>(elements);
        }
        public void execute() {
            for (SlideElement elem : elements) {
                slide.removeElement(elem);
            }
            refreshCanvas();
        }
        public void undo() {
            for (SlideElement elem : elements) {
                slide.addElement(elem);
            }
            refreshCanvas();
        }
    }
    // 2. 命令栈替换快照栈
    private final Stack<Command> commandUndoStack = new Stack<>();
    private final Stack<Command> commandRedoStack = new Stack<>();
    private void pushCommand(Command cmd) {
        cmd.execute();
        commandUndoStack.push(cmd);
        commandRedoStack.clear();
    }
    private void commandUndo() {
        if (!commandUndoStack.isEmpty()) {
            Command cmd = commandUndoStack.pop();
            cmd.undo();
            commandRedoStack.push(cmd);
        }
    }
    private void commandRedo() {
        if (!commandRedoStack.isEmpty()) {
            Command cmd = commandRedoStack.pop();
            cmd.execute();
            commandUndoStack.push(cmd);
        }
    }
    // 3. 粘贴图片/文本功能
    private void handlePasteFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasImage()) {
            Image img = clipboard.getImage();
            if (img != null && currentSlide != null) {
                ImageElement imgElem = new ImageElement(canvas.getWidth()/2 - img.getWidth()/2, canvas.getHeight()/2 - img.getHeight()/2, img);
                pushCommand(new AddElementCommand(currentSlide, imgElem));
                selectedElement = imgElem;
                refreshCanvas();
            }
        } else if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text != null && !text.trim().isEmpty() && currentSlide != null) {
                TextElement txtElem = new TextElement(canvas.getWidth()/2, canvas.getHeight()/2, text, 20, Color.BLACK, FontWeight.NORMAL, false);
                pushCommand(new AddElementCommand(currentSlide, txtElem));
                selectedElement = txtElem;
                refreshCanvas();
            }
        }
    }

    // 在Main类成员变量区添加
    private ContextMenu canvasContextMenu;

    // 框选多目标相关变量
    private boolean isSelecting = false;
    private double selectionStartX, selectionStartY;
    private double selectionEndX, selectionEndY;
    private List<SlideElement> selectedElements = new ArrayList<>();
    // 长按判定
    private PauseTransition longPressTimer;
    private boolean longPressTriggered = false;
    // 框选拖动相关变量
    private boolean isGroupDragging = false;
    private double groupDragLastX, groupDragLastY;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Application starting...");
        BorderPane root = new BorderPane();

        // ========== 幻灯片切换控件初始化 ==========
        previousSlideButton = new Button("上一页");
        nextSlideButton = new Button("下一页");
        slideCountLabel = new Label("1/1");
        slideCountLabel.setStyle("-fx-font-size:14;-fx-padding:6 0;-fx-text-fill:#666;");
        previousSlideButton.getStyleClass().add("button");
        nextSlideButton.getStyleClass().add("button");
        previousSlideButton.setOnAction(e -> previousSlide());
        nextSlideButton.setOnAction(e -> nextSlide());
        VBox navBox = new VBox(8, previousSlideButton, slideCountLabel, nextSlideButton);
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new Insets(40, 0, 0, 0));

        // Create canvas
        double newCanvasWidth = 1600;
        double newCanvasHeight = 1000;
        canvas = new Canvas(newCanvasWidth, newCanvasHeight);
        graphicsContext = canvas.getGraphicsContext2D();

        // Add mouse event handling
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        canvas.setOnMouseClicked(this::handleMouseClicked);

        // ========== 顶部主标题 ==========
        Label mainTitle = new Label("SlideMind");
        mainTitle.setStyle(
                "-fx-font-size:22;-fx-font-weight:bold;-fx-padding:12 0 12 24;-fx-text-fill:#222;font-family:'PingFang SC','Microsoft YaHei','Arial';");
        topBox = new VBox();
        // 主题色 setStyle
        topBox.setStyle("-fx-background-color:" + mainBgColor + ";-fx-border-width:0 0 1 0;-fx-border-color:" + borderColor + ";");
        topBox.getChildren().add(mainTitle);
        // ========== 恢复顶部操作栏 ==========
        toolBar = createToolBar();
        toolBar.setStyle(
                "-fx-background-color:" + mainBgColor + ";-fx-border-radius:16;-fx-background-radius:16;-fx-padding:8 15;-fx-spacing:8;-fx-border-width:0 0 1 0;-fx-border-color:" + borderColor + ";");
        topBox.getChildren().add(toolBar);
        root.setTop(topBox);
        // ========== 画布区域 ==========
        canvasHolder = new BorderPane(canvas);
        canvasHolder.setStyle("-fx-background-color:" + canvasBgColor + ";-fx-border-radius:24;-fx-background-radius:24;");
        canvasHolder.getStyleClass().add("canvas-holder");
        root.setCenter(canvasHolder);

        // 监听canvasPane大小变化，动态重绘并等比缩放内容
        canvasHolder.widthProperty().addListener((obs, oldVal, newVal) -> refreshCanvas());
        canvasHolder.heightProperty().addListener((obs, oldVal, newVal) -> refreshCanvas());

        // ========== 新增：左侧苹果风格绿色侧边栏 ==========
        sidebar = new VBox(18);
        sidebar.setPadding(new Insets(24, 8, 24, 8));
        sidebar.setStyle(
                "-fx-background-color:" + mainBgColor + ";-fx-border-width:0 1 0 0;-fx-border-color:" + borderColor + ";-fx-border-radius:16;-fx-background-radius:16;");
        sidebar.setPrefWidth(160); // 调整侧栏宽度，确保按钮完全显示
        // File 菜单
        Button fileBtn = new Button("文件操作");
        Label fileIcon = new Label("\uD83D\uDCC1");
        fileIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        fileBtn.setGraphic(fileIcon);
        fileBtn.setContentDisplay(ContentDisplay.LEFT);
        fileBtn.setGraphicTextGap(10);
        fileBtn.getStyleClass().add("menu-button");
        fileBtn.setMaxWidth(Double.MAX_VALUE);
        fileBtn.setOnAction(e -> {
            MenuItem newItem = new MenuItem("新建");
            MenuItem openItem = new MenuItem("打开...");
            MenuItem saveItem = new MenuItem("保存");
            MenuItem saveAsItem = new MenuItem("另存为...");
            MenuItem exitItem = new MenuItem("退出");
            newItem.setOnAction(ev -> createNewPresentation());
            openItem.setOnAction(ev -> openPresentation());
            saveItem.setOnAction(ev -> savePresentation());
            saveAsItem.setOnAction(ev -> saveAsPresentation());
            exitItem.setOnAction(ev -> Platform.exit());
            ContextMenu menu = new ContextMenu(
                    newItem,
                    openItem,
                    saveItem,
                    saveAsItem,
                    new SeparatorMenuItem(),
                    exitItem);
            menu.show(fileBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // Edit 菜单
        Button editBtn = new Button("编辑");
        Label editIcon = new Label("\u270E");
        editIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        editBtn.setGraphic(editIcon);
        editBtn.setContentDisplay(ContentDisplay.LEFT);
        editBtn.setGraphicTextGap(10);
        editBtn.getStyleClass().add("menu-button");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnAction(e -> {
            MenuItem undoItem = new MenuItem("撤销");
            MenuItem redoItem = new MenuItem("重做");
            MenuItem cutItem = new MenuItem("剪切");
            MenuItem copyItem = new MenuItem("复制");
            MenuItem pasteItem = new MenuItem("粘贴");
            // 绑定功能
            undoItem.setOnAction(ev -> undo());
            redoItem.setOnAction(ev -> redo());
            cutItem.setOnAction(ev -> cutSelectedElement());
            copyItem.setOnAction(ev -> copySelectedElement());
            pasteItem.setOnAction(ev -> pasteClipboardElement());
            ContextMenu menu = new ContextMenu(
                    undoItem,
                    redoItem,
                    new SeparatorMenuItem(),
                    cutItem,
                    copyItem,
                    pasteItem);
            menu.show(editBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // 智能排版
        Button layoutBtn = new Button("智能排版");
        Label layoutIcon = new Label("\uD83D\uDCC4");
        layoutIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        layoutBtn.setGraphic(layoutIcon);
        layoutBtn.setContentDisplay(ContentDisplay.LEFT);
        layoutBtn.setGraphicTextGap(10);
        layoutBtn.getStyleClass().add("menu-button");
        layoutBtn.setMaxWidth(Double.MAX_VALUE);
        layoutBtn.setOnAction(e -> {
            MenuItem optimizeItem = new MenuItem("优化布局");
            MenuItem responsiveItem = new MenuItem("响应式调整");
            MenuItem autoTextItem = new MenuItem("自动文本调整");
            optimizeItem.setOnAction(ev -> optimizeCurrentSlideLayout());
            responsiveItem.setOnAction(ev -> responsiveAdjustCurrentSlide());
            autoTextItem.setOnAction(ev -> autoAdjustTextSize());
            ContextMenu menu = new ContextMenu(
                    optimizeItem,
                    responsiveItem,
                    autoTextItem);
            menu.show(layoutBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // 结构分析
        Button structureBtn = new Button("结构分析");
        Label structureIcon = new Label("\uD83D\uDCC8");
        structureIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        structureBtn.setGraphic(structureIcon);
        structureBtn.setContentDisplay(ContentDisplay.LEFT);
        structureBtn.setGraphicTextGap(10);
        structureBtn.getStyleClass().add("menu-button");
        structureBtn.setMaxWidth(Double.MAX_VALUE);
        structureBtn.setOnAction(e -> {
            MenuItem analyzeItem = new MenuItem("分析幻灯片结构");
            MenuItem outlineItem = new MenuItem("生成智能大纲");
            MenuItem keypointsItem = new MenuItem("重点内容分析");
            MenuItem logicGraphItem = new MenuItem("生成逻辑关系图");
            MenuItem reportItem = new MenuItem("完整分析报告");
            analyzeItem.setOnAction(ev -> analyzeSlideStructure());
            outlineItem.setOnAction(ev -> generateSmartOutline());
            keypointsItem.setOnAction(ev -> analyzeKeyPoints());
            logicGraphItem.setOnAction(ev -> generateLogicGraph());
            reportItem.setOnAction(ev -> generateCompleteReport());
            ContextMenu menu = new ContextMenu(
                    analyzeItem,
                    outlineItem,
                    keypointsItem,
                    logicGraphItem,
                    reportItem);
            menu.show(structureBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // 多语言
        Button languageBtn = new Button("多语言");
        Label languageIcon = new Label("\uD83C\uDF10");
        languageIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        languageBtn.setGraphic(languageIcon);
        languageBtn.setContentDisplay(ContentDisplay.LEFT);
        languageBtn.setGraphicTextGap(10);
        languageBtn.getStyleClass().add("menu-button");
        languageBtn.setMaxWidth(Double.MAX_VALUE);
        languageBtn.setOnAction(e -> {
            MenuItem translateOneItem = new MenuItem("一键翻译当前幻灯片");
            MenuItem translateAllItem = new MenuItem("批量翻译所有幻灯片");
            MenuItem genMultiItem = new MenuItem("生成多语言PPT");
            MenuItem switchLangItem = new MenuItem("切换语言");
            translateOneItem.setOnAction(ev -> translateCurrentContent());
            translateAllItem.setOnAction(ev -> translateAllContent());
            genMultiItem.setOnAction(ev -> generateMultilingualPPT());
            switchLangItem.setOnAction(ev -> showLanguageSelectionDialog());
            ContextMenu menu = new ContextMenu(
                    translateOneItem,
                    translateAllItem,
                    genMultiItem,
                    switchLangItem);
            menu.show(languageBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // 放映功能
        Button presentationBtn = new Button("放映");
        Label presentationIcon = new Label("\uD83C\uDFA5");
        presentationIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        presentationBtn.setGraphic(presentationIcon);
        presentationBtn.setContentDisplay(ContentDisplay.LEFT);
        presentationBtn.setGraphicTextGap(10);
        presentationBtn.getStyleClass().add("menu-button");
        presentationBtn.setMaxWidth(Double.MAX_VALUE);
        presentationBtn.setOnAction(e -> {
            MenuItem startPresentationItem = new MenuItem("开始放映");
            MenuItem speakerViewItem = new MenuItem("演讲者视图");
            MenuItem presentationSettingsItem = new MenuItem("放映设置");
            startPresentationItem.setOnAction(ev -> startPresentation());
            speakerViewItem.setOnAction(ev -> startSpeakerView());
            presentationSettingsItem.setOnAction(ev -> showPresentationSettings());
            ContextMenu menu = new ContextMenu(
                    startPresentationItem,
                    speakerViewItem,
                    presentationSettingsItem);
            menu.show(presentationBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        
        // AI功能
        Button aiBtn = new Button("AI功能");
        Label aiIcon = new Label("\uD83E\uDD16");
        aiIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;"); // 深绿色
        aiBtn.setGraphic(aiIcon);
        aiBtn.setContentDisplay(ContentDisplay.LEFT);
        aiBtn.setGraphicTextGap(10);
        aiBtn.getStyleClass().add("menu-button");
        aiBtn.setMaxWidth(Double.MAX_VALUE);
        aiBtn.setOnAction(e -> {
            MenuItem aiGenItem = new MenuItem("AI智能生成PPT");
            MenuItem speechGenItem = new MenuItem("生成演讲稿");
            MenuItem speechStructureItem = new MenuItem("演讲稿结构");
            MenuItem keywordAnalysisItem = new MenuItem("关键词分析");
            MenuItem aiQAItem = new MenuItem("AI问答");
            MenuItem templateManageItem = new MenuItem("本地提示词模板管理");
            // 新增：智能生成提示词
            MenuItem promptGenItem = new MenuItem("智能生成提示词");
            promptGenItem.setOnAction(ev -> showPromptGeneratorDialog());
            aiGenItem.setOnAction(ev -> showAIChatDialog());
            speechGenItem.setOnAction(ev -> generateSpeechFromSlides());
            speechStructureItem.setOnAction(ev -> showSpeechStructureDialog());
            keywordAnalysisItem.setOnAction(ev -> performKeywordAnalysis());
            aiQAItem.setOnAction(ev -> showAIDialog());
            templateManageItem.setOnAction(ev -> openTemplateManager());
            ContextMenu menu = new ContextMenu(
                aiGenItem,
                speechGenItem,
                speechStructureItem,
                keywordAnalysisItem,
                aiQAItem,
                promptGenItem,
                new SeparatorMenuItem(),
                templateManageItem);
            menu.show(aiBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // 用户反馈按钮
        Button feedbackBtn = new Button("用户反馈");
        Label feedbackIcon = new Label("\uD83D\uDCE9");
        feedbackIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #218838;");
        feedbackBtn.setGraphic(feedbackIcon);
        feedbackBtn.setContentDisplay(ContentDisplay.LEFT);
        feedbackBtn.setGraphicTextGap(10);
        feedbackBtn.getStyleClass().add("menu-button");
        feedbackBtn.setMaxWidth(Double.MAX_VALUE);
        feedbackBtn.setOnAction(e -> new FeedbackDialog().show());
        // 分组美化
        Separator sep1 = new Separator();
        sep1.setPrefWidth(80);
        Separator sep2 = new Separator();
        sep2.setPrefWidth(80);
        Separator sep3 = new Separator();
        sep3.setPrefWidth(80);
        Separator sep4 = new Separator();
        sep4.setPrefWidth(80);
        // ========== 新增：切换主题按钮 ==========
        Button themeSwitchBtn = new Button("切换主题");
        themeSwitchBtn.setMaxWidth(Double.MAX_VALUE);
        themeSwitchBtn.setOnAction(e -> {
            MenuItem lightItem = new MenuItem("浅色主题");
            MenuItem darkItem = new MenuItem("黑夜主题");
            lightItem.setOnAction(ev -> {
                mainBgColor = "#a3d3b2";
                borderColor = "#7fcfa0";
                canvasBgColor = "#e6f4ea";
                isDarkTheme = false;
                applyTheme();
            });
            darkItem.setOnAction(ev -> {
                mainBgColor = "#23272e";
                borderColor = "#444";
                canvasBgColor = "#181a20";
                isDarkTheme = true;
                applyTheme();
            });
            ContextMenu menu = new ContextMenu(lightItem, darkItem);
            menu.show(themeSwitchBtn, javafx.geometry.Side.RIGHT, 0, 0);
        });
        // ========== 侧边栏控件顺序调整，底部加切换主题按钮 ==========
        sidebar.getChildren().setAll(fileBtn, editBtn, sep1, presentationBtn, sep2, layoutBtn, structureBtn, sep3, languageBtn, aiBtn, feedbackBtn, sep4, themeSwitchBtn);
        root.setLeft(sidebar);

        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);

        // 设置合理的窗口初始大小，确保所有按钮都能显示但不会铺满屏幕
        primaryStage.setWidth(1400); // 调整为更合理的宽度
        primaryStage.setHeight(900); // 调整为更合理的高度

        // Load CSS styles
        try {
            String cssPath = getClass().getResource("/styles/theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            logger.info("CSS styles loaded successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load CSS file", e);
        }

        primaryStage.setTitle("SlideMind");
        primaryStage.setScene(scene);

        // 确保主窗口可以正常控制
        primaryStage.setResizable(true);
        primaryStage.setFullScreen(false);
        primaryStage.setMaximized(false); // 不强制最大化
        
        // 设置窗口最小尺寸，确保功能正常使用
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);

        primaryStage.show();

        // Create initial slide
        createNewSlide();

        // Add keyboard event listener
        scene.setOnKeyPressed(this::handleKeyPressed);

        String apiKey = getApiKey(); // Retrieve from secure source

        aiModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com") // ⚠️ DeepSeek 的 baseUrl
                .modelName("deepseek-chat")
                .temperature(0.5)
                .logRequests(true)
                .logResponses(true)
                .build();

        // // 本地部署模型调用
        // aiModel = OpenAiChatModel.builder()
        // .apiKey(apiKey)
        // .baseUrl("http://localhost:11434/v1") // ⚠️ DeepSeek 的 baseUrl
        // .modelName("deepseek-r1:7b")
        // .temperature(0.5)
        // .logRequests(true)
        // .logResponses(true)
        // .build();

        logger.info("AI Model initialized: " + (aiModel != null ? "Success" : "Failure"));

        // 初始化AIAgent
        aiAgent = new AIAgent(aiModel);
        logger.info("AIAgent initialized: " + (aiAgent != null ? "Success" : "Failure"));

        // testAIMessage();
        logger.info("Application startup completed");

        // 初始化主题
        applyTheme();

        // 粘贴快捷键
        canvas.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                handlePasteFromClipboard();
            }
        });
        // 拖拽图片/文件
        canvas.setOnDragOver(event -> {
            if (event.getDragboard().hasImage() || event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });
        canvas.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            if (db.hasImage()) {
                Image img = db.getImage();
                if (img != null && currentSlide != null) {
                    ImageElement imgElem = new ImageElement(canvas.getWidth()/2 - img.getWidth()/2, canvas.getHeight()/2 - img.getHeight()/2, img);
                    pushCommand(new AddElementCommand(currentSlide, imgElem));
                    selectedElement = imgElem;
                    refreshCanvas();
                }
            } else if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    if (file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp)")) {
                        Image img = new Image(file.toURI().toString());
                        ImageElement imgElem = new ImageElement(canvas.getWidth()/2 - img.getWidth()/2, canvas.getHeight()/2 - img.getHeight()/2, img);
                        pushCommand(new AddElementCommand(currentSlide, imgElem));
                        selectedElement = imgElem;
                        refreshCanvas();
                    }
                    // 可扩展支持文档
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
        // 右键菜单粘贴
        canvas.setOnContextMenuRequested(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem pasteItem = new MenuItem("粘贴");
            pasteItem.setOnAction(ev -> handlePasteFromClipboard());
            menu.getItems().add(pasteItem);
            menu.show(canvas, e.getScreenX(), e.getScreenY());
        });

        // 右键菜单初始化（只创建一次）
        canvasContextMenu = new ContextMenu();
        MenuItem pasteItem = new MenuItem("粘贴");
        pasteItem.setOnAction(ev -> handlePasteFromClipboard());
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(ev -> {
            clipboardElements.clear();
            if (!selectedElements.isEmpty()) {
                for (SlideElement elem : selectedElements) {
                    clipboardElements.add(elem.deepClone());
                }
            } else if (selectedElement != null) {
                clipboardElements.add(selectedElement.deepClone());
            }
        });
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(ev -> {
            if (!selectedElements.isEmpty() && currentSlide != null) {
                pushCommand(new RemoveElementsCommand(currentSlide, selectedElements));
                selectedElements.clear();
                selectedElement = null;
                refreshCanvas();
            } else if (selectedElement != null && currentSlide != null) {
                pushCommand(new RemoveElementCommand(currentSlide, selectedElement));
                selectedElement = null;
                refreshCanvas();
            }
        });
        MenuItem undoItem = new MenuItem("撤销");
        undoItem.setOnAction(ev -> commandUndo());
        MenuItem redoItem = new MenuItem("重做");
        redoItem.setOnAction(ev -> commandRedo());
        canvasContextMenu.getItems().setAll(pasteItem, copyItem, deleteItem, new SeparatorMenuItem(), undoItem, redoItem);

        // 右键弹出菜单
        canvas.setOnContextMenuRequested(e -> {
            // 动态enable/disable
            pasteItem.setDisable(false); // 粘贴始终可用
            copyItem.setDisable(selectedElements.isEmpty() && selectedElement == null);
            deleteItem.setDisable(selectedElements.isEmpty() && selectedElement == null);
            undoItem.setDisable(commandUndoStack.isEmpty());
            redoItem.setDisable(commandRedoStack.isEmpty());
            // 先关闭已有菜单
            canvasContextMenu.hide();
            canvasContextMenu.show(canvas, e.getScreenX(), e.getScreenY());
        });
        // 左键点击时关闭菜单，并执行原有逻辑
        canvas.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && canvasContextMenu.isShowing()) {
                canvasContextMenu.hide();
            }
            handleMousePressed(e);
        });
    } // <-- 补充闭合start方法体

   

    private String getApiKey() {
        // 直接返回用户提供的 API Key
        return "sk-a62c9faf266a4c74b63ecbe57c11ca1d";
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            longPressTriggered = false;
            double px = event.getX();
            double py = event.getY();
            SlideElement clickedElement = currentSlide != null ? currentSlide.findElementAt(px, py) : null;
            // 如果在多选状态下，且点在已选元素上，准备整体拖动
            if (!selectedElements.isEmpty() && clickedElement != null && selectedElements.contains(clickedElement)) {
                isGroupDragging = true;
                groupDragLastX = px;
                groupDragLastY = py;
                return;
            }
            // 启动长按定时器
            if (longPressTimer != null) longPressTimer.stop();
            longPressTimer = new PauseTransition(Duration.millis(400));
            longPressTimer.setOnFinished(e -> {
                if (clickedElement == null) {
                    // 空白区域长按，进入框选
                    isSelecting = true;
                    selectionStartX = px;
                    selectionStartY = py;
                    selectionEndX = px;
                    selectionEndY = py;
                    selectedElements.clear();
                    longPressTriggered = true;
                    refreshCanvas();
                } else {
                    // 元素上长按，只选中该元素
                    selectedElements.clear();
                    selectedElement = clickedElement;
                    selectedElement.setSelected(true);
                    longPressTriggered = true;
                    refreshCanvas();
                }
            });
            longPressTimer.play();
        }
        if (currentShape != null) {
            // Start drawing
            currentDrawing = new DrawElement(
                    event.getX(), event.getY(),
                    currentShape,
                    drawColorPicker.getValue(),
                    lineWidthComboBox.getValue());
            currentSlide.addElement(currentDrawing);
            return;
        }
        if (currentSlide != null) {
            if (selectedElement != null) {
                currentResizeHandle = selectedElement.getResizeHandle(event.getX(), event.getY());
                if (currentResizeHandle != SlideElement.ResizeHandle.NONE) {
                    return;
                }
            }

            SlideElement clickedElement = currentSlide.findElementAt(event.getX(), event.getY());

            // If right-click, show context menu
            if (event.isSecondaryButtonDown() && clickedElement != null) {
                showContextMenu(clickedElement, event.getScreenX(), event.getScreenY());
                return;
            }

            // If clicked on blank area, clear selection
            if (clickedElement == null) {
                clearSelection();
                return;
            }

            // If clicked on new element, update selection
            if (selectedElement != clickedElement) {
                if (selectedElement != null) {
                    selectedElement.setSelected(false);
                }
                selectedElement = clickedElement;
                selectedElement.setSelected(true);
            }

            lastMouseX = event.getX();
            lastMouseY = event.getY();
            System.out.println("==="+lastMouseX+"    " + lastMouseY);
            refreshCanvas();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isGroupDragging) {
            double dx = event.getX() - groupDragLastX;
            double dy = event.getY() - groupDragLastY;
            for (SlideElement elem : selectedElements) {
                elem.move(dx, dy);
            }
            groupDragLastX = event.getX();
            groupDragLastY = event.getY();
            refreshCanvas();
            return;
        }
        if (isSelecting) {
            selectionEndX = event.getX();
            selectionEndY = event.getY();
            // 实时高亮选中元素
            selectedElements.clear();
            if (currentSlide != null) {
                double x1 = Math.min(selectionStartX, selectionEndX);
                double y1 = Math.min(selectionStartY, selectionEndY);
                double x2 = Math.max(selectionStartX, selectionEndX);
                double y2 = Math.max(selectionStartY, selectionEndY);
                for (SlideElement elem : currentSlide.getElements()) {
                    if (elem.getBoundingBox().intersects(x1, y1, x2 - x1, y2 - y1)) {
                        selectedElements.add(elem);
                    }
                }
            }
            refreshCanvas();
            return;
        }
        if (currentDrawing != null) {

            // Update drawing
            currentDrawing.updateEndPoint(event.getX(), event.getY());
            System.out.printf("[调试] 绘图元素终点更新为：(%.2f, %.2f)%n", event.getX(), event.getY());
            refreshCanvas();
            return;
        }
        if (selectedElement != null && currentResizeHandle != SlideElement.ResizeHandle.NONE) {

            System.out.println("============="+lastMouseX+"   "+lastMouseY);
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
            if(deltaX>5||deltaY>5){
                deltaX=0;
                deltaY=0;
            }
            System.out.println("======++++++=="+deltaX+"    " + deltaY);
//            selectedElement.resize(deltaX, deltaY, currentResizeHandle);
            selectedElement.resize(deltaX, deltaY, currentResizeHandle);
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            refreshCanvas();
            return;
        }

        if (selectedElement != null) {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
            selectedElement.move(deltaX, deltaY);
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            refreshCanvas();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (longPressTimer != null) longPressTimer.stop();
        if (isGroupDragging) {
            isGroupDragging = false;
            refreshCanvas();
            return;
        }
        if (isSelecting) {
            isSelecting = false;
            // 框选结束，最终确定选中元素
            // 先全部取消选中
            List<SlideElement> allElements = (currentSlide != null) ? currentSlide.getElements() : new ArrayList<SlideElement>();
            for (SlideElement elem : allElements) {
                elem.setSelected(false);
            }
            selectedElements.clear();
            if (currentSlide != null) {
                double x1 = Math.min(selectionStartX, selectionEndX);
                double y1 = Math.min(selectionStartY, selectionEndY);
                double x2 = Math.max(selectionStartX, selectionEndX);
                double y2 = Math.max(selectionStartY, selectionEndY);
                for (SlideElement elem : currentSlide.getElements()) {
                    if (elem.getBoundingBox().intersects(x1, y1, x2 - x1, y2 - y1)) {
                        selectedElements.add(elem);
                        elem.setSelected(true);
                    }
                }
            }
            selectedElement = selectedElements.isEmpty() ? null : selectedElements.get(0);
            refreshCanvas();
            return;
        }
        // 如果不是长按，执行单选/取消选中逻辑
        if (!longPressTriggered && event.isPrimaryButtonDown()) {
            double px = event.getX();
            double py = event.getY();
            SlideElement clickedElement = currentSlide != null ? currentSlide.findElementAt(px, py) : null;
            // 先全部取消选中
            List<SlideElement> allElements = (currentSlide != null) ? currentSlide.getElements() : new ArrayList<SlideElement>();
            for (SlideElement elem : allElements) {
                elem.setSelected(false);
            }
            selectedElements.clear();
            if (clickedElement != null) {
                selectedElements.add(clickedElement);
                clickedElement.setSelected(true);
                selectedElement = clickedElement;
            } else {
                selectedElement = null;
            }
            refreshCanvas();
        }
        if (currentDrawing != null) {
            // Complete drawing
            currentDrawing.updateEndPoint(event.getX(), event.getY());
            currentDrawing = null;
            // Clear current drawing state
            currentShape = null;
            // Clear selection of all drawing buttons
            for (Toggle toggle : drawGroup.getToggles()) {
                toggle.setSelected(false);
            }
            refreshCanvas();
            return;
        }
        currentResizeHandle = SlideElement.ResizeHandle.NONE;
        lastMouseX = 0;
        lastMouseY = 0;
        refreshCanvas();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (currentSlide != null) {
            if (selectedElement != null) {
                Cursor cursor = Cursor.DEFAULT;

                if (selectedElement instanceof ImageElement) {
                    ImageElement imageElement = (ImageElement) selectedElement;
                    ImageElement.ResizeHandle handle = imageElement.getResizeHandle(event.getX(), event.getY());
                    cursor = getResizeCursor(handle);
                } else if (selectedElement instanceof TextElement) {
                    TextElement textElement = (TextElement) selectedElement;
                    TextElement.ResizeHandle handle = textElement.getResizeHandle(event.getX(), event.getY());
                    cursor = getResizeCursor(handle);
                }

                if (cursor == Cursor.DEFAULT && selectedElement.containsPoint(event.getX(), event.getY())) {
                    cursor = Cursor.HAND;
                }

                canvas.setCursor(cursor);
                return;
            }

            SlideElement element = currentSlide.findElementAt(event.getX(), event.getY());
            if (element != null) {
                element.setHoverCursor(canvas);
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        }
    }

    private Cursor getResizeCursor(Object handle) {
        if (handle instanceof SlideElement.ResizeHandle) {
            SlideElement.ResizeHandle h = (SlideElement.ResizeHandle) handle;
            switch (h) {
                case NW:
                case SE:
                    return Cursor.NW_RESIZE;
                case NE:
                case SW:
                    return Cursor.NE_RESIZE;
                case N:
                case S:
                    return Cursor.V_RESIZE;
                case E:
                case W:
                    return Cursor.H_RESIZE;
                default:
                    return Cursor.DEFAULT;
            }
        }
        return Cursor.DEFAULT;
    }

    private void adjustCanvasSize() {
        if (canvas != null && canvas.getParent() != null) {
            // 获取容器可用空间
            double containerWidth = ((BorderPane) canvas.getParent()).getWidth() - 40; // 减去内边距
            double containerHeight = ((BorderPane) canvas.getParent()).getHeight() - 40;
            
            // 计算保持16:9比例的最大尺寸
            double maxWidth = containerWidth;
            double maxHeight = containerHeight;
            
            // 保持16:9比例
            double aspectRatio = 16.0 / 9.0;
            
            if (maxWidth / maxHeight > aspectRatio) {
                // 高度限制
                maxWidth = maxHeight * aspectRatio;
            } else {
                // 宽度限制
                maxHeight = maxWidth / aspectRatio;
            }
            
            // 设置最小尺寸
            double minWidth = 800;
            double minHeight = 450;
            
            // 确保不小于最小尺寸
            if (maxWidth < minWidth) {
                maxWidth = minWidth;
                maxHeight = maxWidth / aspectRatio;
            }
            if (maxHeight < minHeight) {
                maxHeight = minHeight;
                maxWidth = maxHeight * aspectRatio;
            }
            
            // 调整画布尺寸
            canvas.setWidth(maxWidth);
            canvas.setHeight(maxHeight);
            
            // 重新获取GraphicsContext
            graphicsContext = canvas.getGraphicsContext2D();
        }
    }

    private void refreshCanvas() {
        // Clear canvas
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // Redraw all elements
        if (currentSlide != null) {
            currentSlide.draw(graphicsContext);
            // 高亮多选元素
            graphicsContext.setStroke(Color.BLUE);
            graphicsContext.setLineDashes(6);
            for (SlideElement elem : selectedElements) {
                var bbox = elem.getBoundingBox();
                graphicsContext.strokeRect(bbox.getMinX(), bbox.getMinY(), bbox.getWidth(), bbox.getHeight());
            }
            graphicsContext.setLineDashes(0);
        }
        // 绘制选区矩形
        if (isSelecting) {
            graphicsContext.setStroke(Color.LIGHTBLUE);
            graphicsContext.setLineDashes(4);
            double x = Math.min(selectionStartX, selectionEndX);
            double y = Math.min(selectionStartY, selectionEndY);
            double w = Math.abs(selectionEndX - selectionStartX);
            double h = Math.abs(selectionEndY - selectionStartY);
            graphicsContext.strokeRect(x, y, w, h);
            graphicsContext.setLineDashes(0);
        }
    }

    private ToolBar createToolBar() {
        Button newSlideBtn = new Button(UIStrings.NEW_SLIDE);
        Button addTextBtn = new Button(UIStrings.ADD_TEXT);
        Button addImageBtn = new Button(UIStrings.ADD_IMAGE);
        Button deleteSlideBtn = new Button("删除当前幻灯片");
        deleteSlideBtn.getStyleClass().add("button");
        deleteSlideBtn.setOnAction(e -> deleteCurrentSlide());

        // Add style class to all buttons
        newSlideBtn.getStyleClass().add("button");
        addTextBtn.getStyleClass().add("button");
        addImageBtn.getStyleClass().add("button");

        // Add text style control components
        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        ComboBox<Integer> fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll(12, 14, 16, 18, 20, 24, 28, 32, 36, 48);
        fontSizeCombo.setValue(20);

        ComboBox<String> fontStyleCombo = new ComboBox<>();
        fontStyleCombo.getItems().addAll("Regular", "Bold", "Italic");
        fontStyleCombo.setValue("Regular");

        // Add style change listeners
        colorPicker.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                textElement.setColor(colorPicker.getValue());
                refreshCanvas();
            }
        });

        fontSizeCombo.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                textElement.setFontSize(fontSizeCombo.getValue());
                refreshCanvas();
            }
        });

        fontStyleCombo.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                boolean italic = fontStyleCombo.getValue().equals("Italic");
                FontWeight weight = fontStyleCombo.getValue().equals("Bold") ? FontWeight.BOLD : FontWeight.NORMAL;
                textElement.setFontStyle(weight, italic);
                refreshCanvas();
            }
        });

        // Sync styles when selected element changes - 这个功能已经合并到handleMouseClicked中

        // Initialize class member variables instead of creating new local variables
        previousSlideButton = new Button("Previous Slide");
        nextSlideButton = new Button("Next Slide");
        slideCountLabel = new Label("1/1"); // Display current slide number

        previousSlideButton.getStyleClass().add("button");
        nextSlideButton.getStyleClass().add("button");

        previousSlideButton.setOnAction(e -> previousSlide());
        nextSlideButton.setOnAction(e -> nextSlide());

        newSlideBtn.setOnAction(e -> createNewSlide());
        addTextBtn.setOnAction(e -> addText());
        addImageBtn.setOnAction(e -> addImage());

        // Add drawing tool buttons
        drawGroup = new ToggleGroup();

        ToggleButton rectBtn = new ToggleButton("Rectangle");
        ToggleButton circleBtn = new ToggleButton("Circle");
        ToggleButton lineBtn = new ToggleButton("Line");
        ToggleButton arrowBtn = new ToggleButton("Arrow");

        // Add the same style class as other buttons
        rectBtn.getStyleClass().add("button");
        circleBtn.getStyleClass().add("button");
        lineBtn.getStyleClass().add("button");
        arrowBtn.getStyleClass().add("button");

        rectBtn.setToggleGroup(drawGroup);
        circleBtn.setToggleGroup(drawGroup);
        lineBtn.setToggleGroup(drawGroup);
        arrowBtn.setToggleGroup(drawGroup);

        rectBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.RECTANGLE);
        circleBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.CIRCLE);
        lineBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.LINE);
        arrowBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.ARROW);

        // Add color and line width controls
        drawColorPicker = new ColorPicker(Color.BLACK);
        lineWidthComboBox = new ComboBox<>();
        lineWidthComboBox.getItems().addAll(1.0, 2.0, 3.0, 4.0, 5.0);
        lineWidthComboBox.setValue(2.0);

        // 简化工具栏，只保留基本功能，移除AI功能按钮
        return new ToolBar(
                newSlideBtn,
                deleteSlideBtn,
                new Separator(),
                previousSlideButton,
                slideCountLabel,
                nextSlideButton,
                new Separator(),
                addTextBtn,
                addImageBtn,
                new Separator(),
                colorPicker,
                fontSizeCombo,
                fontStyleCombo,
                new Separator(),
                rectBtn, circleBtn, lineBtn, arrowBtn,
                drawColorPicker,
                lineWidthComboBox);
    }

    private void createNewSlide() {
        Slide newSlide = new Slide();
        slides.add(newSlide);
        currentSlideIndex = slides.size() - 1;
        currentSlide = newSlide;
        refreshCanvas();
        updateSlideControls();
        pushUndoSnapshot(); // 新建后入栈
    }

    private void addText() {
        pushUndoSnapshot();
        // 创建支持多行输入的对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("添加文本");
        dialog.setHeaderText("请输入文本内容：");
        dialog.setResizable(true);

        // 设置按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        // 创建多行文本输入区域
        TextArea textArea = new TextArea();
        textArea.setPromptText("在此输入文本内容...\n支持换行，按Enter键换行");
        textArea.setPrefRowCount(5);
        textArea.setPrefColumnCount(40);
        textArea.setWrapText(true);

        // 设置对话框内容
        dialog.getDialogPane().setContent(textArea);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return textArea.getText();
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(text -> {
            if (!text.trim().isEmpty()) {
            TextElement textElement = new TextElement(
                    canvas.getWidth() / 2,
                    canvas.getHeight() / 2,
                    text,
                    20, // Default font size
                    Color.BLACK, // Default color
                    FontWeight.NORMAL, // Default weight
                    false // Default non-italic
            );
            currentSlide.addElement(textElement);
            refreshCanvas();
                pushUndoSnapshot(); // 变更后入栈
            }
        });
    }

    private void addImage() {
        pushUndoSnapshot();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                System.out.println("Image loaded successfully: " + image.getWidth() + "x" + image.getHeight());

                // Calculate centered position (considering scaled dimensions)
                double scale = Math.min(800 / image.getWidth(), 600 / image.getHeight());
                double scaledWidth = image.getWidth() * scale;
                double scaledHeight = image.getHeight() * scale;

                ImageElement imageElement = new ImageElement(
                        (canvas.getWidth() - scaledWidth) / 2, // Consider scaled width
                        (canvas.getHeight() - scaledHeight) / 2, // Consider scaled height
                        image);

                // Ensure current slide exists
                if (currentSlide == null) {
                    System.out.println("Error: Current slide is null");
                    return;
                }

                currentSlide.addElement(imageElement);
                System.out.println("Image element added to slide");

                refreshCanvas();
                pushUndoSnapshot(); // 变更后入栈
            } catch (Exception e) {
                e.printStackTrace(); // Print detailed error information
                showError("Failed to Load Image", "Ensure a valid image file is selected. Error: " + e.getMessage());
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Add a new method to clear selection
    private void clearSelection() {
        if (selectedElement != null) {
            selectedElement.setSelected(false);
            selectedElement = null;
            refreshCanvas();
        }
    }

    private void previousSlide() {
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
            currentSlide = slides.get(currentSlideIndex);
            refreshCanvas();
            updateSlideControls();
        }
    }

    private void nextSlide() {
        if (currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
            currentSlide = slides.get(currentSlideIndex);
            refreshCanvas();
            updateSlideControls();
        }
    }

    private void updateSlideControls() {
        // Update page number display
        slideCountLabel.setText(String.format("%d/%d",
                currentSlideIndex + 1, slides.size()));

        // Update button states
        previousSlideButton.setDisable(currentSlideIndex <= 0);
        nextSlideButton.setDisable(currentSlideIndex >= slides.size() - 1);
    }

    private void showContextMenu(SlideElement element, double x, double y) {
        // 只做选中和编辑，不再弹出ContextMenu
        if (element instanceof TextElement) {
            selectedElement = element;
            // 可选：支持双击或其它方式编辑文本
            // editSelectedText();
        }
        // 不再弹出任何菜单
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            commandUndo();
        } else if (event.isControlDown() && event.getCode() == KeyCode.Y) {
            commandRedo();
        } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if (!selectedElements.isEmpty() && currentSlide != null) {
                pushCommand(new RemoveElementsCommand(currentSlide, selectedElements));
                selectedElements.clear();
                selectedElement = null;
                refreshCanvas();
                return;
            } else if (selectedElement != null && currentSlide != null) {
                pushCommand(new RemoveElementCommand(currentSlide, selectedElement));
                selectedElement = null;
                refreshCanvas();
                return;
            }
        } else if (event.getCode() == KeyCode.ENTER && selectedElement instanceof TextElement) {
            // 当选中文本元素时，按Enter键可以编辑文本
            editSelectedText();
        } else if (event.isControlDown() && event.getCode() == KeyCode.C) {
            clipboardElements.clear();
            if (!selectedElements.isEmpty()) {
                for (SlideElement elem : selectedElements) {
                    clipboardElements.add(elem.deepClone());
                }
            } else if (selectedElement != null) {
                clipboardElements.add(selectedElement.deepClone());
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.V) {
            if (!clipboardElements.isEmpty() && currentSlide != null) {
                for (SlideElement elem : clipboardElements) {
                    SlideElement pasted = elem.deepClone();
                    pasted.setPosition(canvas.getWidth()/2, canvas.getHeight()/2);
                    currentSlide.addElement(pasted);
                    selectedElement = pasted;
                }
                selectedElements.clear();
                selectedElements.addAll(clipboardElements);
                refreshCanvas();
            } else {
                handlePasteFromClipboard();
            }
        }
    }

    private void editSelectedText() {
        pushUndoSnapshot();
        if (selectedElement instanceof TextElement) {
            TextElement textElement = (TextElement) selectedElement;
            
            // 创建编辑对话框
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("编辑文本");
            dialog.setHeaderText("编辑文本内容（支持换行）：");
            dialog.setResizable(true);

            // 设置按钮
            ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

            // 创建多行文本输入区域
            TextArea textArea = new TextArea(textElement.getText());
            textArea.setPromptText("在此编辑文本内容...\n支持换行，按Enter键换行");
            textArea.setPrefRowCount(5);
            textArea.setPrefColumnCount(40);
            textArea.setWrapText(true);

            // 设置对话框内容
            dialog.getDialogPane().setContent(textArea);

            // 设置结果转换器
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return textArea.getText();
                }
                return null;
            });

            // 显示对话框并处理结果
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> {
                if (!text.trim().isEmpty()) {
                    textElement.setText(text);
                    refreshCanvas();
                    pushUndoSnapshot(); // 编辑后入栈
                }
            });
        }
    }

    private void deleteElement(SlideElement element) {
        pushUndoSnapshot();
        if (currentSlide != null) {
            currentSlide.removeElement(element);
            if (element == selectedElement) {
                selectedElement = null;
            }
            refreshCanvas();
            pushUndoSnapshot(); // 删除后入栈
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true); // Use system menu bar on macOS

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New");
        MenuItem openItem = new MenuItem("Open...");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem saveAsItem = new MenuItem("Save As...");
        MenuItem exitItem = new MenuItem("Exit");

        // Add shortcuts
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));

        fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(), exitItem);

        // Edit menu
        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        MenuItem redoItem = new MenuItem("Redo");
        MenuItem cutItem = new MenuItem("Cut");
        MenuItem copyItem = new MenuItem("Copy");
        MenuItem pasteItem = new MenuItem("Paste");

        editMenu.getItems().addAll(undoItem, redoItem,
                new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem);

        // Play menu
        Menu playMenu = new Menu("Play");
        MenuItem startItem = new MenuItem("Start Presentation");
        MenuItem settingsItem = new MenuItem("Presentation Settings");

        playMenu.getItems().addAll(startItem, settingsItem);
        
        // 智能排版菜单
        Menu layoutMenu = new Menu("智能排版");
        MenuItem optimizeLayoutItem = new MenuItem("优化布局");
        MenuItem responsiveLayoutItem = new MenuItem("响应式调整");
        MenuItem autoTextSizeItem = new MenuItem("自动文本调整");
        layoutMenu.getItems().addAll(optimizeLayoutItem, responsiveLayoutItem, autoTextSizeItem);
        
        // 结构分析菜单
        Menu structureMenu = new Menu("结构分析");
        MenuItem analyzeStructureItem = new MenuItem("分析幻灯片结构");
        MenuItem generateOutlineItem = new MenuItem("生成智能大纲");
        MenuItem analyzeKeyPointsItem = new MenuItem("重点内容分析");
        MenuItem generateLogicGraphItem = new MenuItem("生成逻辑关系图");
        MenuItem completeReportItem = new MenuItem("完整分析报告");
        structureMenu.getItems().addAll(analyzeStructureItem, generateOutlineItem, analyzeKeyPointsItem,
                generateLogicGraphItem, completeReportItem);

        // 多语言菜单
        Menu languageMenu = new Menu("多语言");
        MenuItem translateContentItem = new MenuItem("一键翻译当前幻灯片");
        MenuItem translateAllItem = new MenuItem("批量翻译所有幻灯片");
        MenuItem generateMultilingualItem = new MenuItem("生成多语言PPT");
        MenuItem switchLanguageItem = new MenuItem("切换语言");
        languageMenu.getItems().addAll(translateContentItem, translateAllItem, generateMultilingualItem,
                switchLanguageItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, playMenu, layoutMenu, structureMenu, languageMenu);

        // Add event handling
        newItem.setOnAction(e -> createNewPresentation());
        openItem.setOnAction(e -> openPresentation());
        saveItem.setOnAction(e -> savePresentation());
        saveAsItem.setOnAction(e -> saveAsPresentation());
        exitItem.setOnAction(e -> Platform.exit());
        startItem.setOnAction(e -> startPresentation());
        
        // 智能排版功能事件处理
        optimizeLayoutItem.setOnAction(e -> optimizeCurrentSlideLayout());
        responsiveLayoutItem.setOnAction(e -> responsiveAdjustCurrentSlide());
        autoTextSizeItem.setOnAction(e -> autoAdjustTextSize());
        
        // 结构分析功能事件处理
        analyzeStructureItem.setOnAction(e -> analyzeSlideStructure());
        generateOutlineItem.setOnAction(e -> generateSmartOutline());
        analyzeKeyPointsItem.setOnAction(e -> analyzeKeyPoints());
        generateLogicGraphItem.setOnAction(e -> generateLogicGraph());
        completeReportItem.setOnAction(e -> generateCompleteReport());
        
        // 多语言功能事件处理
        translateContentItem.setOnAction(e -> translateCurrentContent());
        translateAllItem.setOnAction(e -> translateAllContent());
        generateMultilingualItem.setOnAction(e -> generateMultilingualPPT());
        switchLanguageItem.setOnAction(e -> showLanguageSelectionDialog());

        return menuBar;
    }

    private void createNewPresentation() {
        // Prompt to save current file
        if (!slides.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("New Presentation");
            alert.setHeaderText("Save current presentation?");
            alert.setContentText("If not saved, current changes will be lost.");

            ButtonType saveButton = new ButtonType("Save");
            ButtonType noSaveButton = new ButtonType("Don't Save");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, noSaveButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == saveButton) {
                savePresentation();
            } else if (result.get() == cancelButton) {
                return;
            }
        }

        // Clear current slides
        slides.clear();
        currentSlideIndex = -1;
        createNewSlide();
    }

    private void saveAsPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz"));

        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        try {
            SlideSerializer.savePresentation(slides, file.getPath());
            showInfo("Save Successful", "Presentation saved to: " + file.getPath());
        } catch (IOException e) {
            showError("Save Failed", "Unable to save file: " + e.getMessage());
        }

    }

    private void startPresentation() {
        if (slides.isEmpty()) {
            showError("放映失败", "当前没有幻灯片内容，无法开始放映");
            return;
        }
        PresentationWindow presentation = new PresentationWindow(slides);
        presentation.start();
    }
    
    private void startSpeakerView() {
        if (slides.isEmpty()) {
            showError("演讲者视图失败", "当前没有幻灯片内容，无法启动演讲者视图");
            return;
        }
        
        // 检查是否有演讲稿文件
        if (!SpeechManager.hasSpeechFile()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("演讲者视图");
            alert.setHeaderText("未找到演讲稿文件");
            alert.setContentText("演讲者视图需要演讲稿文件才能显示演讲稿内容。\n是否先生成演讲稿？");
            
            ButtonType generateButton = new ButtonType("生成演讲稿");
            ButtonType continueButton = new ButtonType("继续启动");
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(generateButton, continueButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == generateButton) {
                    generateSpeechFromSlides();
                    return;
                } else if (result.get() == cancelButton) {
                    return;
                }
            }
        }
        
        SpeakerViewWindow speakerView = new SpeakerViewWindow(slides);
        speakerView.start();
    }
    
    private void showPresentationSettings() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("放映设置");
        dialog.setHeaderText("放映功能说明");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label titleLabel = new Label("放映功能说明");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        TextArea infoArea = new TextArea(
            "放映功能说明：\n\n" +
            "1. 开始放映：\n" +
            "   - 点击'开始放映'按钮\n" +
            "   - 放映窗口将全屏显示\n" +
            "   - 支持键盘控制\n\n" +
            "2. 键盘控制：\n" +
            "   - 右箭头键或空格键：下一张幻灯片\n" +
            "   - 左箭头键：上一张幻灯片\n" +
            "   - ESC键：退出放映\n\n" +
            "3. 放映特性：\n" +
            "   - 全屏显示模式\n" +
            "   - 自动适应屏幕尺寸\n" +
            "   - 保持幻灯片原有样式\n\n" +
            "4. 注意事项：\n" +
            "   - 确保有幻灯片内容再开始放映\n" +
            "   - 放映时请确保显示器支持全屏模式\n" +
            "   - 按ESC键可随时退出放映"
        );
        infoArea.setPrefRowCount(20);
        infoArea.setPrefColumnCount(50);
        infoArea.setWrapText(true);
        infoArea.setEditable(false);
        
        content.getChildren().addAll(titleLabel, infoArea);
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    private void savePresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Presentation");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz"));

        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                // Use SlideSerializer to save slide list
                SlideSerializer.savePresentation(slides, file.getPath());
                showInfo("Save Successful", "Presentation saved to: " + file.getPath());
            } catch (IOException e) {
                showError("Save Failed", "Unable to save file: " + e.getMessage());
            }
        }
    }

    private void openPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Presentation");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz"));

        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                // Use SlideSerializer to load slide list
                slides = SlideSerializer.loadPresentation(file.getPath());
                currentSlideIndex = 0;
                currentSlide = slides.get(0);
                refreshCanvas();
                updateSlideControls();
                showInfo("Open Successful", "Loaded presentation: " + file.getName());
            } catch (IOException e) {
                showError("Open Failed", "Unable to open file: " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 使用AIAgent生成演讲稿
     * 演示如何使用generateSpeechBySlides方法
     */
    private void generateSpeechFromSlides() {
        if (slides.isEmpty()) {
            showError("生成演讲稿失败", "当前没有幻灯片内容");
            return;
        }
        // 新增：判断所有幻灯片内容是否都为空
        boolean allEmpty = true;
        for (Slide slide : slides) {
            if (slide.getTextContent() != null && !slide.getTextContent().isEmpty()) {
                allEmpty = false;
                break;
            }
        }
        if (allEmpty) {
            showError("生成演讲稿失败", "当前没有可用的PPT内容，无法生成演讲稿");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("生成演讲稿");
        progressAlert.setHeaderText("正在根据幻灯片内容生成演讲稿...");
        progressAlert.setContentText("请稍候，这可能需要几秒钟时间");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行AI调用
        new Thread(() -> {
            try {
                String speech = aiAgent.generateSpeechBySlides(slides);

                Platform.runLater(() -> {
                    progressAlert.close();
                    showSpeechDialog(speech);
                });

            } catch (AIAgent.AIException e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("AI调用失败", "生成演讲稿时发生错误: " + e.getMessage());
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("参数错误", "参数验证失败: " + e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("未知错误", "生成演讲稿时发生未知错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示演讲稿对话框
     * 
     * @param speech 演讲稿内容
     */
    private void showSpeechDialog(String speech) {
        // 使用Alert而不是Dialog，这样更简单且不会有关闭问题
        Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
        resultDialog.setTitle("生成的演讲稿");
        resultDialog.setHeaderText("根据当前幻灯片内容生成的演讲稿");

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButtonType = new ButtonType("复制到剪贴板", ButtonBar.ButtonData.OTHER);
        ButtonType saveButtonType = new ButtonType("保存到文件", ButtonBar.ButtonData.OTHER);
        resultDialog.getButtonTypes().setAll(closeButtonType, copyButtonType, saveButtonType);

        TextArea speechArea = new TextArea(speech);
        speechArea.setPrefRowCount(15);
        speechArea.setPrefColumnCount(60);
        speechArea.setWrapText(true);
        speechArea.setEditable(false);

        resultDialog.getDialogPane().setContent(speechArea);

        // 显示对话框并处理结果
        Optional<ButtonType> result = resultDialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == copyButtonType) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(speech);
            clipboard.setContent(content);
            showInfo("复制成功", "演讲稿已复制到剪贴板");
            } else if (result.get() == saveButtonType) {
                saveSpeechToFile(speech);
            }
        }
    }
    
    /**
     * 保存演讲稿到文件
     * 
     * @param speech 演讲稿内容
     */
    private void saveSpeechToFile(String speech) {
        String presentationName = "演示文稿";
        if (!slides.isEmpty()) {
            // 尝试从第一张幻灯片获取标题作为演示文稿名称
            List<String> textContent = slides.get(0).getTextContent();
            if (textContent != null && !textContent.isEmpty()) {
                presentationName = textContent.get(0).substring(0, Math.min(20, textContent.get(0).length()));
            }
        }
        
        String filePath = SpeechManager.saveSpeechToFile(speech, presentationName);
        if (filePath != null) {
            showInfo("保存成功", "演讲稿已保存到文件:\n" + filePath);
        } else {
            showError("保存失败", "无法保存演讲稿到文件");
        }
    }
    
    /**
     * 自动生成并保存演讲稿（带界面显示）
     */
    private void generateAndSaveSpeechWithDisplay(TextArea speechDisplayArea) {
        if (slides.isEmpty()) {
            showError("生成失败", "当前没有幻灯片内容，无法生成演讲稿");
            return;
        }
        
        // 在演讲稿区域显示生成状态
        if (speechDisplayArea != null) {
            speechDisplayArea.setDisable(false);
            speechDisplayArea.setText("正在生成演讲稿...");
        }
        
        // 创建时间更新器
        final long startTime = System.currentTimeMillis();
        final javafx.animation.Timeline timeTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1000), e -> {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long minutes = elapsed / 60;
                    long seconds = elapsed % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);
                    
                    if (speechDisplayArea != null) {
                        speechDisplayArea.setText("正在生成演讲稿... (" + timeStr + ")");
                    }
                }));
        timeTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeTimeline.play();

        // 在新线程中执行AI调用
        new Thread(() -> {
            try {
                String speech = aiAgent.generateSpeechBySlides(slides);
                
                // 自动保存演讲稿
                String presentationName = "演示文稿";
                if (!slides.isEmpty()) {
                    List<String> textContent = slides.get(0).getTextContent();
                    if (textContent != null && !textContent.isEmpty()) {
                        presentationName = textContent.get(0).substring(0, Math.min(20, textContent.get(0).length()));
                    }
                }
                
                String filePath = SpeechManager.saveSpeechToFile(speech, presentationName);

                Platform.runLater(() -> {
                    // 停止时间更新器
                    timeTimeline.stop();
                    
                    // 显示演讲稿在界面上
                    if (speechDisplayArea != null) {
                        speechDisplayArea.setText(speech);
                        speechDisplayArea.setDisable(false);
                    }
                    
                    if (filePath != null) {
                        showInfo("生成成功", "演讲稿已生成并保存到文件:\n" + filePath);
                    } else {
                        showError("保存失败", "演讲稿生成成功，但保存到文件失败");
                    }
                });

            } catch (AIAgent.AIException e) {
                Platform.runLater(() -> {
                    timeTimeline.stop();
                    if (speechDisplayArea != null) {
                        speechDisplayArea.setText("生成演讲稿失败: " + e.getMessage());
                    }
                    showError("AI调用失败", "生成演讲稿时发生错误: " + e.getMessage());
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    timeTimeline.stop();
                    if (speechDisplayArea != null) {
                        speechDisplayArea.setText("参数错误: " + e.getMessage());
                    }
                    showError("参数错误", "参数验证失败: " + e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    timeTimeline.stop();
                    if (speechDisplayArea != null) {
                        speechDisplayArea.setText("生成演讲稿时发生未知错误: " + e.getMessage());
                    }
                    showError("未知错误", "生成演讲稿时发生未知错误: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 自动生成并保存演讲稿
     */
    private void generateAndSaveSpeechAutomatically() {
        // 调用带显示的方法，传入null表示不需要显示在界面上
        generateAndSaveSpeechWithDisplay(null);
    }

    private void showAIChatDialog() {
        // 创建一个独立的Stage而不是Dialog，这样可以有完整的窗口控制
        Stage aiStage = new Stage();
        aiStage.setTitle("AI智能生成PPT");
        aiStage.setMinWidth(800);
        aiStage.setMinHeight(600);
        
        // 设置窗口图标（如果有的话）
        // aiStage.getIcons().add(new
        // Image(getClass().getResourceAsStream("/icon.png")));
        
        // 创建主容器
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // 创建标题
        Label titleLabel = new Label(
                "选择模板并输入需求，点击*生成建议*后可查看AI建议、PPT命令和演讲稿，编辑命令后点击*生成PPT并保持窗口*生成幻灯片");
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 添加模板选择功能
        ComboBox<PromptTemplate> templateCombo = new ComboBox<>();
        templateCombo.setPromptText("选择提示词模板（可选）");
        templateCombo.setPrefWidth(300);

        // 设置自定义单元格工厂来格式化显示
        templateCombo.setCellFactory(param -> new ListCell<PromptTemplate>() {
            @Override
            protected void updateItem(PromptTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if ("no-template".equals(item.getId())) {
                        setText("程序默认提示词");
                    } else {
                        setText(item.getName() + " (" + item.getCategory().getDisplayName() + ")");
                    }
                }
            }
        });

        // 设置按钮单元格工厂
        templateCombo.setButtonCell(templateCombo.getCellFactory().call(null));

        // 加载所有模板
        List<PromptTemplate> allTemplates = aiAgent.getTemplateManager().getAllTemplates();
        templateCombo.getItems().addAll(allTemplates);

        // 添加一个"不使用模板"选项
        PromptTemplate noTemplate = new PromptTemplate("程序默认提示词", "直接使用默认提示词", "",
                slideshow.model.TemplateCategory.OTHER);
        noTemplate.setId("no-template");
        templateCombo.getItems().add(0, noTemplate);
        templateCombo.setValue(noTemplate);

        // 显示当前选择的模板信息
        Label templateInfoLabel = new Label("当前模板: 程序默认提示词");
        templateInfoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        // 模板选择监听器
        templateCombo.setOnAction(e -> {
            PromptTemplate selected = templateCombo.getValue();
            if (selected != null) {
                if ("no-template".equals(selected.getId())) {
                    templateInfoLabel.setText("当前模板: 程序默认提示词");
                } else {
                    String info = "当前模板: " + selected.getName() +
                            " (" + selected.getCategory().getDisplayName() + ")";

                    // 添加使用次数信息
                    if (selected.getMetadata() != null && selected.getMetadata().getUseCount() > 0) {
                        info += " | 使用次数: " + selected.getMetadata().getUseCount();
                    }

                    // 添加评分信息
                    if (selected.getMetadata() != null && selected.getMetadata().getAverageRating() > 0) {
                        info += " | 评分: " + String.format("%.1f", selected.getMetadata().getAverageRating());
                    }

                    templateInfoLabel.setText(info);
                }
            }
        });

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("请输入你的PPT需求，例如：生成一个关于人工智能的PPT介绍，或直接提问如'今天星期几'");
        inputArea.setPrefRowCount(3);
        inputArea.setWrapText(true);

        TextArea adviceArea = new TextArea();
        adviceArea.setPromptText("AI建议与思考过程将在这里展示（只读）");
        adviceArea.setPrefRowCount(5);
        adviceArea.setWrapText(true);
        adviceArea.setEditable(false);
        adviceArea.setDisable(true);

        TextArea suggestionArea = new TextArea();
        suggestionArea.setPromptText("AI生成的PPT命令将在这里展示，可手动修改后再生成PPT");
        suggestionArea.setPrefRowCount(8);
        suggestionArea.setWrapText(true);
        suggestionArea.setEditable(true);
        suggestionArea.setDisable(true);
        
        // 添加演讲稿显示区域
        TextArea speechArea = new TextArea();
        speechArea.setPromptText("生成的演讲稿将在这里显示，可编辑");
        speechArea.setPrefRowCount(8);
        speechArea.setWrapText(true);
        speechArea.setEditable(true); // 允许编辑
        speechArea.setDisable(true);

        // 创建选项区域（暂时保留，但为空）
        HBox optionsBox = new HBox(20);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        // 创建按钮容器
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button generateBtn = new Button("生成建议");
        Button confirmBtn = new Button("生成PPT并保持窗口");
        Button generateSpeechBtn = new Button("生成演讲稿"); // 新增
        Button saveSpeechBtn = new Button("保存演讲稿"); // 修改
        Button closeBtn = new Button("关闭");
        
        // 设置按钮样式
        generateBtn.getStyleClass().add("button");
        confirmBtn.getStyleClass().add("button");
        generateSpeechBtn.getStyleClass().add("button");
        saveSpeechBtn.getStyleClass().add("button");
        closeBtn.getStyleClass().add("button");
        
        // 初始时禁用生成/保存演讲稿按钮
        generateSpeechBtn.setDisable(true);
        saveSpeechBtn.setDisable(true);

        buttonBox.getChildren().addAll(generateBtn, confirmBtn, generateSpeechBtn, saveSpeechBtn, closeBtn);

        // ========== 20250712新增：AI思考链可视化相关 ==========
        ListView<AIChainStep> aiChainListView = new ListView<>();
        ObservableList<AIChainStep> aiChainSteps = FXCollections.observableArrayList();
        aiChainListView.setItems(aiChainSteps);
        aiChainListView.setPrefHeight(120);
        aiChainListView.setCellFactory(list -> new ListCell<AIChainStep>() {
            @Override
            protected void updateItem(AIChainStep step, boolean empty) {
                super.updateItem(step, empty);
                if (empty || step == null) {
                    setText(null);
                } else {
                    setText(step.getTitle() + " [" + step.getStatus() + "]\n" + step.getDetail());
                }
            }
        });
        // ========== 新增结束 ==========
        
        // 添加所有组件到主容器
        root.getChildren().addAll(
            titleLabel,
            new Label("选择模板："), templateCombo, templateInfoLabel,
            new Label("PPT需求："), inputArea,
                // 新增：AI思考链可视化区域
                new Label("AI思考链（可视化）："), aiChainListView,
            new Label("AI建议与反馈（只读）："), adviceArea,
            new Label("PPT命令与大纲（可编辑）："), suggestionArea,
                new Label("演讲稿内容（可编辑）："), speechArea,
                optionsBox,
                buttonBox);

        // 创建Scene并设置到Stage
        Scene scene = new Scene(root);
        aiStage.setScene(scene);
        
        // 生成建议按钮逻辑
        generateBtn.setOnAction(event -> {
            String userPrompt = inputArea.getText().trim();
            if (userPrompt.isEmpty()) {
                adviceArea.setText("请先输入PPT需求！");
                suggestionArea.setText("");
                return;
            }

            // 获取选择的模板
            PromptTemplate selectedTemplate = templateCombo.getValue();

            adviceArea.setDisable(false);
            suggestionArea.setDisable(false);
            adviceArea.setText("AI正在思考并生成建议，请稍候...");
            suggestionArea.setText("");

            // ========== 新增：AI思考链步骤初始化 ==========
            aiChainSteps.clear();
            aiChainSteps.add(
                    new AIChainStep("1. 构建提示词", "正在根据用户输入和模板构建AI提示词...", AIChainStep.StepStatus.RUNNING));
            aiChainListView.refresh();
            // ========== 新增结束 ==========

            // 创建时间更新器
            final long startTime = System.currentTimeMillis();

            final javafx.animation.Timeline timeTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), e -> {
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        adviceArea.setText("AI正在思考中... (已用时: " + elapsed + "秒)");
                    }));
            timeTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            timeTimeline.play();

            // 调用AI生成建议、命令
            new Thread(() -> {
                String aiPrompt;

                // 根据是否选择模板来构建不同的提示词
                if (selectedTemplate != null && !"no-template".equals(selectedTemplate.getId())) {
                    // 使用选择的模板
                    try {
                        String formattedPrompt = selectedTemplate.formatContent(userPrompt);

                        // 确保模板输出标准PPT格式
                        aiPrompt = formattedPrompt +
                                "\n\n重要要求：请根据以上模板要求处理用户输入，但必须严格按照以下标准PPT格式输出：" +
                                "\n---PPT命令---" +
                                "\nPage 1:" +
                                "\nTitle: [页面标题]" +
                                "\nSubtitle: [页面副标题]" +
                                "\nBullet: [项目符号内容]" +
                                "\nText: [小标题下具体自然段文本,PPT的正文内容]" +
                                "\nDraw: [绘图描述]" +
                                "\nPage 2:" +
                                "\nTitle: [页面标题]" +
                                "\nSubtitle: [页面副标题]" +
                                "\nBullet: [项目符号内容]" +
                                "\nText: [小标题下具体自然段文本，PPT的正文内容]" +
                                "\nDraw: [绘图描述]" +
                                "\n（继续更多页面...）" +
                                "\n\n1.请确保使用'---PPT命令---'分隔符，并严格按照Page X:格式分页。" +
                                "\n2.对Text下大的段落酌情设置标准长度进行分行。当用户提出的要求中标注了具体生成语言时(如英语)，请使用英语输出。否则默认为中文。" +
                                "\n3.Text为自然段，请在内容尽量丰富的情况下，酌情设置标准长度进行分行。内容可以不局限于一句，但请注意不要出现过大段落。" +
                                "\n用户输入：" + userPrompt;

                        // 记录模板使用次数
                        aiAgent.getTemplateManager().useTemplate(selectedTemplate.getId());

                    } catch (Exception e) {
                        // 如果模板格式化失败，使用默认提示词
                        aiPrompt = buildDefaultPrompt(userPrompt);
                    }
                } else {
                    // 使用默认提示词
                    aiPrompt = buildDefaultPrompt(userPrompt);
                }

                // ========== 新增：AI思考链步骤 ==========
                Platform.runLater(() -> {
                    aiChainSteps.get(0).setStatus(AIChainStep.StepStatus.DONE);
                    aiChainSteps
                            .add(new AIChainStep("2. 调用AI模型", "正在请求AI生成建议...", AIChainStep.StepStatus.RUNNING));
                    aiChainListView.refresh();
                });
                // ========== 新增结束 ==========

                try {
                    String aiResult = aiModel.chat(aiPrompt);
                    Platform.runLater(() -> {
                        // 停止时间更新器
                        timeTimeline.stop();

                        // ========== 新增：AI思考链步骤 ==========
                        aiChainSteps.get(1).setStatus(AIChainStep.StepStatus.DONE);
                        aiChainSteps.add(
                                new AIChainStep("3. 解析AI响应", "正在解析AI返回内容...", AIChainStep.StepStatus.RUNNING));
                        aiChainListView.refresh();
                        // ========== 新增结束 ==========

                        // 智能解析AI响应
                        String advice = "";
                        String pptCmd = "";

                        if (selectedTemplate != null && !"no-template".equals(selectedTemplate.getId())) {
                            // 使用自定义模板时的处理逻辑
                            if (aiResult.contains("---PPT命令---")) {
                                // 如果包含分隔符，按原逻辑处理
                                String[] parts = aiResult.split("---PPT命令---");
                                advice = parts.length > 0 ? parts[0].trim() : "";
                                pptCmd = parts.length > 1 ? parts[1].trim() : "";
                            } else {
                                // 如果不包含分隔符，尝试智能解析并转换为标准格式
                                String[] lines = aiResult.split("\n");
                                boolean foundPptContent = false;
                                StringBuilder pptBuilder = new StringBuilder();
                                StringBuilder adviceBuilder = new StringBuilder();
                                int pageNumber = 1;

                                for (String line : lines) {
                                    line = line.trim();
                                    if (line.isEmpty())
                                        continue;

                                    // 检查是否是PPT命令格式
                                    if (line.startsWith("Page ") ||
                                            line.startsWith("Title:") ||
                                            line.startsWith("Subtitle:") ||
                                            line.startsWith("Bullet:") ||
                                            line.startsWith("Draw:") ||
                                            line.startsWith("Image:") ||
                                            line.matches("^\\d+\\..*")) {
                                        foundPptContent = true;

                                        // 如果是Page开头，确保格式正确
                                        if (line.startsWith("Page ")) {
                                            pptBuilder.append("Page ").append(pageNumber).append(":\n");
                                            pageNumber++;
                                        } else {
                                            pptBuilder.append(line).append("\n");
                                        }
                                    } else if (foundPptContent) {
                                        // 如果已经找到PPT内容，后续内容也归为PPT
                                        pptBuilder.append(line).append("\n");
                                    } else {
                                        // 否则归为建议
                                        adviceBuilder.append(line).append("\n");
                                    }
                                }

                                advice = adviceBuilder.toString().trim();
                                pptCmd = pptBuilder.toString().trim();

                                // 如果没有找到PPT内容，尝试将整个响应转换为标准格式
                                if (pptCmd.isEmpty()) {
                                    advice = aiResult.trim();
                                    // 尝试将响应内容转换为标准PPT格式
                                    pptCmd = convertToStandardPPTFormat(aiResult.trim());
                                }
                            }
                        } else {
                            // 使用默认模板时的处理逻辑
                            String[] parts = aiResult.split("---PPT命令---");
                            advice = parts.length > 0 ? parts[0].trim() : "";
                            pptCmd = parts.length > 1 ? parts[1].trim() : "";
                        }

                        adviceArea.setText(advice);
                        suggestionArea.setText(pptCmd);
                        adviceArea.setDisable(false);
                        suggestionArea.setDisable(false);
                        
                        // 启用生成PPT和生成演讲稿按钮（有PPT命令时）
                        boolean pptReady = !pptCmd.isEmpty();
                        confirmBtn.setDisable(!pptReady);
                        generateSpeechBtn.setDisable(!pptReady);
                        // 生成后演讲稿区域可编辑但内容为空
                        speechArea.setText("");
                        speechArea.setDisable(false);
                        speechArea.setEditable(true);
                        saveSpeechBtn.setDisable(true);
                        
                        // ========== 新增：AI思考链步骤 ==========
                        aiChainSteps.get(2).setStatus(AIChainStep.StepStatus.DONE);
                        aiChainListView.refresh();
                        // ========== 新增结束 ==========
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        // 停止时间更新器
                        timeTimeline.stop();

                        adviceArea.setText("AI生成失败：" + e.getMessage());
                        suggestionArea.setText("");
                        adviceArea.setDisable(false);
                        suggestionArea.setDisable(false);
                        // ========== 新增：AI思考链步骤 ==========
                        if (aiChainSteps.size() > 0)
                            aiChainSteps.get(aiChainSteps.size() - 1).setStatus(AIChainStep.StepStatus.DONE);
                        aiChainSteps
                                .add(new AIChainStep("异常", "AI生成失败：" + e.getMessage(), AIChainStep.StepStatus.DONE));
                        aiChainListView.refresh();
                        // ========== 新增结束 ==========
                    });
                }
            }).start();
        });

        // 生成PPT按钮逻辑
        confirmBtn.setOnAction(event -> {
            String suggestion = suggestionArea.getText().trim();
            if (suggestion.isEmpty() || adviceArea.getText().startsWith("AI正在思考")) {
                suggestionArea.setText("请先生成并确认PPT命令！");
                return;
            }

            System.out.println("Main: 开始生成PPT，PPT命令内容:");
            System.out.println(suggestion);
            System.out.println("Main: PPT命令内容长度: " + suggestion.length());

            // 生成PPT但不关闭窗口
            Platform.runLater(() -> {
                parseAndCreateSlides(suggestion);
                
                // 在窗口内显示成功信息
                adviceArea.setText("✓ PPT已成功生成！您可以继续查看和编辑AI建议，或关闭窗口。");
                
                // 生成PPT后也允许生成演讲稿
                generateSpeechBtn.setDisable(false);
            });
        });
        
        // 生成演讲稿按钮逻辑（只生成并显示，不保存）
        generateSpeechBtn.setOnAction(event -> {
            if (slides.isEmpty()) {
                showError("生成失败", "当前没有幻灯片内容，无法生成演讲稿");
                return;
            }
            speechArea.setDisable(false);
            speechArea.setEditable(false);
            speechArea.setText("正在生成演讲稿...");
            saveSpeechBtn.setDisable(true);
            // 创建时间更新器
            final long startTime = System.currentTimeMillis();
            final javafx.animation.Timeline timeTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(1000), e -> {
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        long minutes = elapsed / 60;
                        long seconds = elapsed % 60;
                        String timeStr = String.format("%02d:%02d", minutes, seconds);
                        speechArea.setText("正在生成演讲稿... (" + timeStr + ")");
                    }));
            timeTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            timeTimeline.play();
            new Thread(() -> {
                try {
                    String speech = aiAgent.generateSpeechBySlides(slides);
                    Platform.runLater(() -> {
                        timeTimeline.stop();
                        speechArea.setText(speech);
                        speechArea.setEditable(true);
                        speechArea.requestFocus();
                        saveSpeechBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        timeTimeline.stop();
                        speechArea.setText("生成演讲稿失败: " + e.getMessage());
                        speechArea.setEditable(false);
                        saveSpeechBtn.setDisable(true);
                    });
                }
            }).start();
        });

        // 保存演讲稿按钮逻辑（保存当前内容）
        saveSpeechBtn.setOnAction(event -> {
            String speech = speechArea.getText();
            if (speech == null || speech.trim().isEmpty()) {
                showError("保存失败", "演讲稿内容为空，无法保存");
                return;
            }
            // 弹出输入框让用户输入文件名
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("保存演讲稿");
            dialog.setHeaderText("请输入演讲稿文件名（不含扩展名）");
            String defaultName = "演讲稿";
            if (!slides.isEmpty()) {
                List<String> textContent = slides.get(0).getTextContent();
                if (textContent != null && !textContent.isEmpty()) {
                    defaultName = textContent.get(0).substring(0, Math.min(20, textContent.get(0).length()));
                }
            }
            dialog.getEditor().setText(defaultName);
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String fileName = result.get().trim();
                String filePath = slideshow.util.SpeechManager.saveSpeechToFile(speech, fileName);
                if (filePath != null) {
                    showInfo("保存成功", "演讲稿已保存到文件:\n" + filePath);
                } else {
                    showError("保存失败", "无法保存演讲稿到文件");
                }
            } else {
                showError("保存失败", "文件名不能为空，已取消保存");
            }
        });

        // 关闭按钮逻辑
        closeBtn.setOnAction(event -> aiStage.close());

        // 初始时禁用"生成PPT"、"生成/保存演讲稿"按钮
        confirmBtn.setDisable(true);
        generateSpeechBtn.setDisable(true);
        saveSpeechBtn.setDisable(true);
        suggestionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasContent = !newVal.trim().isEmpty() && !adviceArea.getText().startsWith("AI正在思考");
            confirmBtn.setDisable(!hasContent);
            generateSpeechBtn.setDisable(!hasContent);
            // 只有生成了演讲稿内容后才能保存
            if (speechArea.getText().trim().isEmpty()) {
                saveSpeechBtn.setDisable(true);
            }
        });
        speechArea.textProperty().addListener((obs, oldVal, newVal) -> {
            // 只要演讲稿区域有内容且非只读，允许保存
            saveSpeechBtn.setDisable(newVal.trim().isEmpty() || !speechArea.isEditable());
        });

        // 显示窗口
        aiStage.show();
    }

    private void parseAndCreateSlides(String aiResult) {
        pushUndoSnapshot();
        System.out.println("Main: parseAndCreateSlides 开始");
        System.out.println("Main: 输入内容长度: " + aiResult.length());

        // 使用SlideParser解析AI生成的PPT命令
        slides = SlideParser.parseAndCreateSlides(aiResult, canvas.getWidth());

        System.out.println("Main: 解析完成，创建了 " + slides.size() + " 个幻灯片");

        // 更新当前幻灯片索引和显示
        currentSlideIndex = slides.isEmpty() ? -1 : 0;
        currentSlide = slides.isEmpty() ? null : slides.get(0);

        System.out.println("Main: 当前幻灯片索引: " + currentSlideIndex);
        if (currentSlide != null) {
            System.out.println("Main: 当前幻灯片元素数量: " + currentSlide.getElements().size());
        }

        // 刷新画布和控件状态
        refreshCanvas();
        updateSlideControls();
        pushUndoSnapshot(); // 解析后入栈

        System.out.println("Main: parseAndCreateSlides 完成");
    }

    /**
     * 构建默认的AI提示词
     */
    private String buildDefaultPrompt(String userPrompt) {
        return "你是PPT助手，请根据用户输入做如下两步：\n" +
                "1. 先用自然语言给出你的建议、思考或直接回答用户问题（如'今天星期三'），如果用户需求与PPT无关请直接回复建议或答案。\n" +
                "2. 如果用户需求与PPT制作有关，再用严格的PPT命令格式输出大纲，格式要求如下：\n" +
                "---PPT命令---\n" +
                "Page 1:\nTitle: ...\nSubtitle: ...\nBullet: ...\nText: ...\nDraw: ...\nPage 2: ...\n（每个命令单独一行，所有命令都在---PPT命令---下方，若无PPT需求则此部分可为空）\n" +
                "请严格用'---PPT命令---'分隔建议和命令部分。\n" +
                "重要要求：\n" +
                "- Text字段下请对本页标题或分点内容进行详细扩展说明，生成多句自然段长文本，不要只写一句话。\n" +
                "- Text内容应包含背景、解释、案例、分析等丰富信息，便于观众理解。\n" +
                "- Bullet仅为要点，Text为正文扩展说明。\n" +
                "- 如有多条Bullet，可在Text中分别对每条进行详细阐述。\n" +
                "- 内容可适当分行，但每页Text应尽量充实。\n" +
                "用户输入：" + userPrompt;
    }

    /**
     * 将任意内容转换为标准PPT格式
     */
    private String convertToStandardPPTFormat(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        int pageNumber = 1;
        boolean hasContent = false;
        boolean inPage = false;
        boolean textWritten = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // 检测新页面开始
            if (line.matches("^Page\\s*\\d+[:：]?.*")) {
                if (hasContent) {
                    result.append("\n");
                }
                result.append(line.contains(":") ? line : (line + ":")).append("\n");
                inPage = true;
                textWritten = false;
                hasContent = true;
                continue;
            }

            // 标题、副标题
            if (line.contains("标题") || line.contains("Title") || line.contains("主题")) {
                result.append("Title: ").append(line.replaceAll(".*[标题|Title|主题][：:]*\\s*", "")).append("\n");
            } else if (line.contains("副标题") || line.contains("Subtitle")) {
                result.append("Subtitle: ").append(line.replaceAll(".*[副标题|Subtitle][：:]*\\s*", "")).append("\n");
            } else if ((line.startsWith("Text:") || line.startsWith("Text：")) && inPage && !textWritten) {
                // 专门处理Text:开头的行，去掉前缀
                String text = line.substring(line.indexOf(':') + 1).trim();
                if (!text.isEmpty()) {
                    result.append("Text: ").append(text).append("\n");
                    textWritten = true;
                }
            } else if (line.contains("要点") || line.contains("Bullet") || line.contains("•") || line.contains("-")) {
                String bulletContent = line.replaceAll(".*[要点|Bullet|•|-][：:]*\\s*", "");
                if (!bulletContent.isEmpty()) {
                    result.append("Bullet: ").append(bulletContent).append("\n");
                }
            } else if (line.matches("^\\d+\\..*")) {
                // 编号列表
                String listContent = line.substring(line.indexOf('.') + 1).trim();
                if (!listContent.isEmpty()) {
                    result.append("Bullet: ").append(listContent).append("\n");
                }
            } else if (line.contains("图片") || line.contains("Image") || line.contains("图表")) {
                result.append("Draw: ").append(line.replaceAll(".*[图片|Image|图表][：:]*\\s*", "")).append("\n");
            } else if (inPage && !textWritten && (line.length() > 10 && line.length() < 300)
                        && !(line.startsWith("Text:") || line.startsWith("Text："))) {
                // 优先将较长的普通文本归为Text:，每页只输出一次，且不是Text:开头
                result.append("Text: ").append(line).append("\n");
                textWritten = true;
            } else if (!line.startsWith("Page") && !line.startsWith("---")) {
                // 其他内容作为Bullet
                result.append("Bullet: ").append(line).append("\n");
            }
        }

        // 如果没有找到任何结构化的内容，创建一个简单的页面
        if (!hasContent) {
            result.append("Page 1:\n");
            result.append("Title: ").append(content.substring(0, Math.min(50, content.length()))).append("\n");
            result.append("Subtitle: 内容概述\n");
            result.append("Text: ").append(content).append("\n");
        }

        return result.toString().trim();
    }

    /**
     * 显示演讲稿结构生成对话框
     */
    private void showSpeechStructureDialog() {
        // 创建输入对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("生成演讲稿结构");
        dialog.setHeaderText("请输入演讲信息");

        ButtonType generateButtonType = new ButtonType("生成", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, cancelButtonType);

        // 创建输入控件
        TextField topicField = new TextField();
        topicField.setPromptText("演讲主题");

        ComboBox<Integer> durationCombo = new ComboBox<>();
        durationCombo.getItems().addAll(5, 10, 15, 20, 30, 45, 60);
        durationCombo.setValue(15);

        ComboBox<String> audienceCombo = new ComboBox<>();
        audienceCombo.getItems().addAll("一般听众", "学生", "专业人士", "管理层");
        audienceCombo.setValue("一般听众");

        VBox inputBox = new VBox(10);
        inputBox.getChildren().addAll(
                new Label("主题："), topicField,
                new Label("时长（分钟）："), durationCombo,
                new Label("听众："), audienceCombo);
        inputBox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(inputBox);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButtonType) {
                String topic = topicField.getText().trim();
                if (topic.isEmpty()) {
                    showError("输入错误", "请输入演讲主题");
                    return null;
                }
                return topic + "|" + durationCombo.getValue() + "|" + audienceCombo.getValue();
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String[] parts = input.split("\\|");
            if (parts.length == 3) {
                generateSpeechStructure(parts[0], Integer.parseInt(parts[1]), parts[2]);
            }
        });
    }

    /**
     * 生成演讲稿结构
     */
    private void generateSpeechStructure(String topic, int duration, String audience) {
        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("生成中");
        progressAlert.setHeaderText("正在生成演讲稿结构...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行AI调用
        new Thread(() -> {
            try {
                String speechStructure = aiAgent.generateSlidesByTopic(topic, duration, audience);

                Platform.runLater(() -> {
                    progressAlert.close();
                    showSpeechStructureResult(speechStructure);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("生成失败", "生成演讲稿结构时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示演讲稿结构结果
     */
    private void showSpeechStructureResult(String speechStructure) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("演讲稿结构");
        dialog.setHeaderText("生成的演讲稿结构");

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);

        // 创建文本区域
        TextArea textArea = new TextArea(speechStructure);
        textArea.setPrefRowCount(15);
        textArea.setEditable(false);
        textArea.setPromptText("分析结果...");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("详细分析结果:"), textArea);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    /**
     * 执行关键词分析
     */
    private void performKeywordAnalysis() {
        if (slides.isEmpty()) {
            showError("关键词分析失败", "当前没有幻灯片内容");
            return;
        }

        // 检查是否有内容
        boolean hasContent = false;
        for (Slide slide : slides) {
            if (slide.getTextContent() != null && !slide.getTextContent().isEmpty()) {
                hasContent = true;
                break;
            }
        }

        if (!hasContent) {
            showError("关键词分析失败", "当前幻灯片没有可分析的文本内容");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("关键词分析");
        progressAlert.setHeaderText("正在分析幻灯片内容...");
        progressAlert.setContentText("请稍候，这可能需要几秒钟时间");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行AI调用
        new Thread(() -> {
            try {
                AIAgent.SlideAnalysis analysis = aiAgent.parseSlides(slides);

                Platform.runLater(() -> {
                    progressAlert.close();
                    showKeywordAnalysisResult(analysis);
                });

            } catch (AIAgent.AIException e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("AI调用失败", "关键词分析时发生错误: " + e.getMessage());
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("参数错误", "参数验证失败: " + e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("未知错误", "关键词分析时发生未知错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示关键词分析结果
     * 
     * @param analysis 分析结果
     */
    private void showKeywordAnalysisResult(AIAgent.SlideAnalysis analysis) {
        // 创建详细的分析结果文本
        StringBuilder resultText = new StringBuilder();
        resultText.append("=== 幻灯片分析报告 ===\n\n");
        
        resultText.append("📊 基本信息:\n");
        resultText.append("• 幻灯片总数: ").append(analysis.getTotalSlides()).append("\n");
        resultText.append("• 总字数: ").append(analysis.getTotalWords()).append("\n\n");
        
        resultText.append("🎯 主要主题:\n");
        resultText.append(analysis.getMainTopic() != null ? analysis.getMainTopic() : "未识别").append("\n\n");
        
        resultText.append("🔑 关键词 (共").append(analysis.getKeywords().size()).append("个):\n");
        for (int i = 0; i < analysis.getKeywords().size(); i++) {
            String keyword = analysis.getKeywords().get(i);
            Integer frequency = analysis.getKeywordFrequency().get(keyword);
            resultText.append(i + 1).append(". ").append(keyword);
            if (frequency != null) {
                resultText.append(" (出现").append(frequency).append("次)");
            }
            resultText.append("\n");
        }
        resultText.append("\n");
        
        if (!analysis.getThemes().isEmpty()) {
            resultText.append("📂 主题分类:\n");
            for (int i = 0; i < analysis.getThemes().size(); i++) {
                resultText.append(i + 1).append(". ").append(analysis.getThemes().get(i)).append("\n");
            }
            resultText.append("\n");
        }
        
        resultText.append("📝 内容摘要:\n");
        resultText.append(analysis.getSummary() != null ? analysis.getSummary() : "未生成摘要").append("\n\n");
        
        resultText.append("=== 分析完成 ===");

        // 使用Alert而不是Dialog，这样更简单且不会有关闭问题
        Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
        resultDialog.setTitle("关键词分析结果");
        resultDialog.setHeaderText("幻灯片内容分析");

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButtonType = new ButtonType("复制结果", ButtonBar.ButtonData.OTHER);
        resultDialog.getButtonTypes().setAll(closeButtonType, copyButtonType);

        // 创建文本区域
        TextArea resultArea = new TextArea(resultText.toString());
        resultArea.setPrefRowCount(20);
        resultArea.setPrefColumnCount(60);
        resultArea.setWrapText(true);
        resultArea.setEditable(false);

        resultDialog.getDialogPane().setContent(resultArea);

        // 显示对话框并处理结果
        Optional<ButtonType> result = resultDialog.showAndWait();
        if (result.isPresent() && result.get() == copyButtonType) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(resultText.toString());
            clipboard.setContent(content);
            showInfo("复制成功", "分析结果已复制到剪贴板");
        }
    }

    /**
     * 显示AI问答对话框
     */
    private void showAIDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI智能问答");
        dialog.setHeaderText("向AI提问，获取智能回答");

        ButtonType askButtonType = new ButtonType("提问", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(askButtonType, cancelButtonType);

        // 创建输入控件
        TextArea questionArea = new TextArea();
        questionArea.setPromptText("请输入您的问题，例如：\n• 什么是人工智能？\n• 如何制作PPT？\n• 今天天气怎么样？");
        questionArea.setPrefRowCount(4);
        questionArea.setWrapText(true);

        TextArea answerArea = new TextArea();
        answerArea.setPromptText("AI回答将在这里显示");
        answerArea.setPrefRowCount(8);
        answerArea.setWrapText(true);
        answerArea.setEditable(false);

        VBox inputBox = new VBox(10);
        inputBox.getChildren().addAll(
                new Label("您的问题："), questionArea,
                new Label("AI回答："), answerArea);
        inputBox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(inputBox);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == askButtonType) {
                String question = questionArea.getText().trim();
                if (question.isEmpty()) {
                    showError("输入错误", "请输入您的问题");
                    return null;
                }
                return question;
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(question -> {
            // 显示进度提示
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("AI思考中");
            progressAlert.setHeaderText("正在思考您的问题...");
            progressAlert.setContentText("请稍候，这可能需要几秒钟时间");
            progressAlert.setResizable(false);
            progressAlert.show();

            // 在新线程中执行AI调用
            new Thread(() -> {
                try {
                    String answer = aiAgent.askAI(question);

                    Platform.runLater(() -> {
                        progressAlert.close();
                        showAIAnswerDialog(question, answer);
                    });

                } catch (AIAgent.AIException e) {
                    Platform.runLater(() -> {
                        progressAlert.close();
                        showError("AI调用失败", "AI问答时发生错误: " + e.getMessage());
                    });
                } catch (IllegalArgumentException e) {
                    Platform.runLater(() -> {
                        progressAlert.close();
                        showError("参数错误", "参数验证失败: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressAlert.close();
                        showError("未知错误", "AI问答时发生未知错误: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    /**
     * 显示AI回答对话框
     * 
     * @param question 用户问题
     * @param answer   AI回答
     */
    private void showAIAnswerDialog(String question, String answer) {
        Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
        resultDialog.setTitle("AI回答");
        resultDialog.setHeaderText("AI智能回答");

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButtonType = new ButtonType("复制回答", ButtonBar.ButtonData.OTHER);
        resultDialog.getButtonTypes().setAll(closeButtonType, copyButtonType);

        // 创建格式化的显示内容
        StringBuilder displayContent = new StringBuilder();
        displayContent.append("❓ 您的问题：\n");
        displayContent.append(question).append("\n\n");
        displayContent.append("🤖 AI回答：\n");
        displayContent.append(answer);

        TextArea resultArea = new TextArea(displayContent.toString());
        resultArea.setPrefRowCount(15);
        resultArea.setPrefColumnCount(60);
        resultArea.setWrapText(true);
        resultArea.setEditable(false);

        resultDialog.getDialogPane().setContent(resultArea);

        // 显示对话框并处理结果
        Optional<ButtonType> result = resultDialog.showAndWait();
        if (result.isPresent() && result.get() == copyButtonType) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(answer);
            clipboard.setContent(content);
            showInfo("复制成功", "AI回答已复制到剪贴板");
        }
    }

    /**
     * 打开模板管理窗口
     */
    public static void openTemplateManager() {
        try {
            slideshow.presentation.TemplateManagerWindow templateWindow = new slideshow.presentation.TemplateManagerWindow();
            templateWindow.show();
            // logger.info("Template manager window opened successfully");
        } catch (Exception e) {
            // logger.log(Level.SEVERE, "Failed to open template manager window", e);
            // showError("错误", "无法打开模板管理窗口: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        Application.launch(args);
    }
    
    // ==================== 新增功能方法实现 ====================
    
    /**
     * 优化当前幻灯片布局
     */
    private void optimizeCurrentSlideLayout() {
        if (currentSlide == null) {
            showError("优化失败", "当前没有选中的幻灯片");
            return;
        }
        
        try {
            // 创建增强的AI代理
            AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
            
            // 应用智能布局优化
            enhancedAgent.optimizeSlideLayout(currentSlide, canvas.getWidth(), canvas.getHeight(), 
                                           IntelligentLayoutEngine.LayoutType.CENTERED);
            
            refreshCanvas();
            showInfo("布局优化", "当前幻灯片布局已优化");
            
        } catch (Exception e) {
            showError("优化失败", "布局优化时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 响应式调整当前幻灯片
     */
    private void responsiveAdjustCurrentSlide() {
        if (currentSlide == null) {
            showError("调整失败", "当前没有选中的幻灯片");
            return;
        }
        
        try {
            // 创建增强的AI代理
            AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
            
            // 获取当前画布尺寸
            double newWidth = canvas.getWidth();
            double newHeight = canvas.getHeight();
            
            // 应用响应式调整
            enhancedAgent.responsiveAdjustLayout(currentSlide, newWidth, newHeight);
            
            refreshCanvas();
            showInfo("响应式调整", "幻灯片已根据当前尺寸调整");
            
        } catch (Exception e) {
            showError("调整失败", "响应式调整时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 自动调整文本大小
     */
    private void autoAdjustTextSize() {
        if (selectedElement == null || !(selectedElement instanceof TextElement)) {
            showError("调整失败", "请先选择一个文本元素");
            return;
        }
        
        try {
            TextElement textElement = (TextElement) selectedElement;
            
            // 创建增强的AI代理
            AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
            
            // 自动调整文本大小
            enhancedAgent.autoAdjustTextSize(textElement, 400.0, 100.0);
            
            refreshCanvas();
            showInfo("文本调整", "文本大小已自动调整");
            
        } catch (Exception e) {
            showError("调整失败", "自动调整文本大小时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 翻译当前内容
     */
    private void translateCurrentContent() {
        if (currentSlide == null) {
            showError("翻译失败", "当前没有选中的幻灯片");
            return;
        }
        
        // 先弹出语言选择对话框
        Dialog<MultilingualSupport.SupportedLanguage> dialog = new Dialog<>();
        dialog.setTitle("选择翻译语言");
        dialog.setHeaderText("请选择要翻译成的语言");
        ButtonType confirmButtonType = new ButtonType("翻译", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        ComboBox<MultilingualSupport.SupportedLanguage> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(MultilingualSupport.getSupportedLanguages());
        languageCombo.setCellFactory(param -> new ListCell<MultilingualSupport.SupportedLanguage>() {
            @Override
            protected void updateItem(MultilingualSupport.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        languageCombo.setButtonCell(languageCombo.getCellFactory().call(null));
        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("目标语言:"), languageCombo);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return languageCombo.getValue();
            }
            return null;
        });
        Optional<MultilingualSupport.SupportedLanguage> result = dialog.showAndWait();
        if (result.isPresent()) {
            // 只有用户选择后才弹出进度提示
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("翻译中");
            progressAlert.setHeaderText("正在翻译当前幻灯片...");
            progressAlert.setContentText("请稍候，这可能需要几秒钟时间");
            progressAlert.setResizable(false);
            progressAlert.show();

            MultilingualSupport.SupportedLanguage targetLanguage = result.get();
            // 启动翻译任务
            Task<StringBuilder> translationTask = new Task<>() {
                @Override
                protected StringBuilder call() {
                    StringBuilder translationLog = new StringBuilder();
                    int count = 0;
                    List<SlideElement> elements = currentSlide.getElements();
                    List<TextElement> textElements = new ArrayList<>();
                    for (SlideElement element : elements) {
                        if (element instanceof TextElement) {
                            textElements.add((TextElement) element);
                        }
                    }
                    for (TextElement textElement : textElements) {
                        String originalText = textElement.getText();
                        if (originalText != null && !originalText.trim().isEmpty()) {
                            String translatedText = MultilingualSupport.generateMultilingualContent(originalText,
                                    targetLanguage);
                            if (translatedText.equals(originalText)) {
                                String prompt = String.format(
                                        "请将下列内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Text、Page X:等），保持原有排版。只输出翻译结果本身，不要任何注释、说明、Note、括号内容、示例、解释等。如果遇到占位符（如[你的姓名/职位]），请原样保留，不要输出任何说明。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                                        targetLanguage.getDisplayName(), originalText);
                                try {
                                    translatedText = aiModel.chat(prompt).trim();
                                } catch (Exception ex) {
                                    translatedText = "[AI翻译失败] " + originalText;
                                }
                            }
                            String cleanOriginal = stripPPTStructureFields(originalText);
                            String cleanTranslated = stripPPTStructureFields(translatedText);
                            translationLog.append(String.format("原文: %s\n译文: %s\n", cleanOriginal, cleanTranslated));
                            textElement.setText(translatedText);
                            count++;
                        }
                    }
                    translationLog.insert(0,
                            String.format("已翻译 %d 个文本元素为: %s\n\n", count, targetLanguage.getDisplayName()));
                    return translationLog;
                }
            };
            translationTask.setOnSucceeded(e -> {
                progressAlert.close();
                try {
                    AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
                    enhancedAgent.optimizeSlideLayout(currentSlide, canvas.getWidth(), canvas.getHeight(),
                            IntelligentLayoutEngine.LayoutType.CENTERED);
                } catch (Exception ex) {
                    logger.warning("翻译后自动优化布局失败: " + ex.getMessage());
                }
                refreshCanvas();
                showTranslationResultDialog(translationTask.getValue().toString(), currentSlide.getElements().size(),
                        targetLanguage);
            });
            translationTask.setOnFailed(e -> {
                progressAlert.close();
                showError("翻译失败", "翻译过程中发生错误: " + translationTask.getException().getMessage());
            });
            new Thread(translationTask).start();
        }
    }

    private void translateAllContent() {
        if (slides.isEmpty()) {
            showError("翻译失败", "没有可翻译的幻灯片");
            return;
        }
        // 新增：显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("批量翻译中");
        progressAlert.setHeaderText("正在批量翻译所有幻灯片...");
        progressAlert.setContentText("请稍候，这可能需要几秒钟时间");
        progressAlert.setResizable(false);
        progressAlert.show();
        // 显示语言选择对话框
        Dialog<MultilingualSupport.SupportedLanguage> dialog = new Dialog<>();
        dialog.setTitle("选择批量翻译语言");
        dialog.setHeaderText("请选择要翻译成的语言（将翻译所有幻灯片）");
        ButtonType confirmButtonType = new ButtonType("批量翻译", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        ComboBox<MultilingualSupport.SupportedLanguage> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(MultilingualSupport.getSupportedLanguages());
        languageCombo.setCellFactory(param -> new ListCell<MultilingualSupport.SupportedLanguage>() {
            @Override
            protected void updateItem(MultilingualSupport.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        languageCombo.setButtonCell(languageCombo.getCellFactory().call(null));
        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("目标语言:"), languageCombo);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return languageCombo.getValue();
            }
            return null;
        });
        Optional<MultilingualSupport.SupportedLanguage> result = dialog.showAndWait();
        if (result.isPresent()) {
            MultilingualSupport.SupportedLanguage targetLanguage = result.get();
            Task<StringBuilder> translationTask = new Task<>() {
                @Override
                protected StringBuilder call() {
                    int translatedSlides = 0;
                    int translatedElements = 0;
                    StringBuilder translationLog = new StringBuilder();
                    for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
                        Slide slide = slides.get(slideIndex);
                        List<SlideElement> elements = slide.getElements();
                        boolean slideTranslated = false;
                        translationLog.append(String.format("\n=== 幻灯片 %d ===\n", slideIndex + 1));
                        for (SlideElement element : elements) {
                            if (element instanceof TextElement) {
                                TextElement textElement = (TextElement) element;
                                String originalText = textElement.getText();
                                if (originalText != null && !originalText.trim().isEmpty()) {
                                    String translatedText = MultilingualSupport
                                            .generateMultilingualContent(originalText, targetLanguage);
                                    if (translatedText.equals(originalText)) {
                                        String prompt = String.format(
                                                "请将下列内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Page X:等），保持原有排版。只输出翻译结果本身，不要任何注释、说明、Note、括号内容、示例、解释等。如果遇到占位符（如[你的姓名/职位]），请原样保留，不要输出任何说明。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                                                targetLanguage.getDisplayName(), originalText);
                                        try {
                                            translatedText = aiModel.chat(prompt).trim();
                                        } catch (Exception ex) {
                                            translatedText = "[AI翻译失败] " + originalText;
                                        }
                                    }
                                    String cleanOriginal = stripPPTStructureFields(originalText);
                                    String cleanTranslated = stripPPTStructureFields(translatedText);
                                    translationLog
                                            .append(String.format("原文: %s\n译文: %s\n", cleanOriginal, cleanTranslated));
                                    textElement.setText(translatedText);
                                    translatedElements++;
                                    slideTranslated = true;
                                }
                            }
                        }
                        if (slideTranslated) {
                            translatedSlides++;
                        }
                    }
                    translationLog.insert(0, String.format("已翻译 %d 个幻灯片，共 %d 个文本元素为: %s\n\n", translatedSlides,
                            translatedElements, targetLanguage.getDisplayName()));
                    return translationLog;
                }
            };
            translationTask.setOnSucceeded(e -> {
                progressAlert.close();
                try {
                    AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
                    for (Slide slide : slides) {
                        enhancedAgent.optimizeSlideLayout(slide, canvas.getWidth(), canvas.getHeight(),
                                IntelligentLayoutEngine.LayoutType.CENTERED);
                    }
                } catch (Exception ex) {
                    logger.warning("批量翻译后自动优化布局失败: " + ex.getMessage());
                }
                if (currentSlide != null)
                    refreshCanvas();
                // 统计信息
                String translationLog = translationTask.getValue().toString();
                String[] lines = translationLog.split("\n");
                int actualTranslatedSlides = 0;
                int actualTranslatedElements = 0;
                if (lines.length > 0 && lines[0].contains("已翻译")) {
                    String firstLine = lines[0];
                    if (firstLine.contains("个幻灯片，共") && firstLine.contains("个文本元素")) {
                        try {
                            String parts = firstLine.split("已翻译 ")[1].split(" 个幻灯片，共 ")[0];
                            actualTranslatedSlides = Integer.parseInt(parts);
                            String elementsPart = firstLine.split(" 个幻灯片，共 ")[1].split(" 个文本元素")[0];
                            actualTranslatedElements = Integer.parseInt(elementsPart);
                        } catch (Exception ex) {
                            actualTranslatedSlides = 0;
                            actualTranslatedElements = 0;
                        }
                    }
                }
                showBatchTranslationResultDialog(translationLog, actualTranslatedSlides, actualTranslatedElements,
                        targetLanguage);
            });
            translationTask.setOnFailed(e -> {
                progressAlert.close();
                showError("批量翻译失败", "翻译过程中发生错误: " + translationTask.getException().getMessage());
            });
            new Thread(translationTask).start();
        } else {
            progressAlert.close();
        }
    }
    
    /**
     * 生成多语言PPT
     */
    private void generateMultilingualPPT() {
        // 显示主题输入对话框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("生成多语言PPT");
        dialog.setHeaderText("请输入PPT主题");
        dialog.setContentText("主题:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String topic = result.get().trim();
            
            try {
                // 创建增强的AI代理
                AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
                
                // 生成多语言PPT（默认英文）
                String pptCommands = enhancedAgent.generateIntelligentMultilingualPPT(
                    topic, 
                    MultilingualSupport.SupportedLanguage.ENGLISH,
                    IntelligentLayoutEngine.LayoutType.CENTERED);
                
                // 解析并创建幻灯片
                parseAndCreateSlides(pptCommands);
                
                showInfo("生成成功", "多语言PPT已生成");
                
            } catch (Exception e) {
                showError("生成失败", "生成多语言PPT时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 显示语言选择对话框
     */
    private void showLanguageSelectionDialog() {
        Dialog<MultilingualSupport.SupportedLanguage> dialog = new Dialog<>();
        dialog.setTitle("选择语言");
        dialog.setHeaderText("请选择要切换的语言");
        
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // 创建语言选择列表
        ComboBox<MultilingualSupport.SupportedLanguage> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(MultilingualSupport.getSupportedLanguages());
        languageCombo.setCellFactory(param -> new ListCell<MultilingualSupport.SupportedLanguage>() {
            @Override
            protected void updateItem(MultilingualSupport.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        languageCombo.setButtonCell(languageCombo.getCellFactory().call(null));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("语言:"), languageCombo);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return languageCombo.getValue();
            }
            return null;
        });
        
        // 显示对话框并处理结果
        Optional<MultilingualSupport.SupportedLanguage> result = dialog.showAndWait();
        result.ifPresent(language -> {
            try {
                MultilingualSupport.switchLanguage(language);
                showInfo("语言切换", "语言已切换到: " + language.getDisplayName());
            } catch (Exception e) {
                showError("切换失败", "切换语言时发生错误: " + e.getMessage());
            }
        });
    }
    
    /**
     * 显示翻译语言选择对话框
     */
    private void showLanguageSelectionDialogForTranslation() {
        Dialog<MultilingualSupport.SupportedLanguage> dialog = new Dialog<>();
        dialog.setTitle("选择翻译语言");
        dialog.setHeaderText("请选择要翻译成的语言");
        
        ButtonType confirmButtonType = new ButtonType("翻译", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // 创建语言选择列表
        ComboBox<MultilingualSupport.SupportedLanguage> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(MultilingualSupport.getSupportedLanguages());
        languageCombo.setCellFactory(param -> new ListCell<MultilingualSupport.SupportedLanguage>() {
            @Override
            protected void updateItem(MultilingualSupport.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        languageCombo.setButtonCell(languageCombo.getCellFactory().call(null));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("目标语言:"), languageCombo);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return languageCombo.getValue();
            }
            return null;
        });
        
        // 显示对话框并处理结果
        Optional<MultilingualSupport.SupportedLanguage> result = dialog.showAndWait();
        result.ifPresent(language -> {
            // 执行一键翻译功能
            translateCurrentSlideContent(language);
        });
    }
    
    /**
     * 显示批量翻译语言选择对话框
     */
    private void showLanguageSelectionDialogForBatchTranslation() {
        Dialog<MultilingualSupport.SupportedLanguage> dialog = new Dialog<>();
        dialog.setTitle("选择批量翻译语言");
        dialog.setHeaderText("请选择要翻译成的语言（将翻译所有幻灯片）");
        
        ButtonType confirmButtonType = new ButtonType("批量翻译", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // 创建语言选择列表
        ComboBox<MultilingualSupport.SupportedLanguage> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(MultilingualSupport.getSupportedLanguages());
        languageCombo.setCellFactory(param -> new ListCell<MultilingualSupport.SupportedLanguage>() {
            @Override
            protected void updateItem(MultilingualSupport.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        languageCombo.setButtonCell(languageCombo.getCellFactory().call(null));
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("目标语言:"), 
            languageCombo,
                new Label("注意: 此操作将翻译所有幻灯片的内容"));
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return languageCombo.getValue();
            }
            return null;
        });
        
        // 显示对话框并处理结果
        Optional<MultilingualSupport.SupportedLanguage> result = dialog.showAndWait();
        result.ifPresent(language -> {
            // 执行批量翻译功能
            translateAllSlidesContent(language);
        });
    }
    
    /**
     * 显示翻译结果
     */
    private void showTranslationResult(String originalContent, String translatedContent, 
                                     MultilingualSupport.SupportedLanguage language) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("翻译结果");
        dialog.setHeaderText("翻译为: " + language.getDisplayName());
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        // 使用stripPPTStructureFields处理，确保只显示内容本身
        String cleanOriginal = stripPPTStructureFields(originalContent);
        String cleanTranslated = stripPPTStructureFields(translatedContent);
        
        Label originalLabel = new Label("原文:");
        TextArea originalArea = new TextArea(cleanOriginal);
        originalArea.setPrefRowCount(3);
        originalArea.setEditable(false);
        
        Label translatedLabel = new Label("译文:");
        TextArea translatedArea = new TextArea(cleanTranslated);
        translatedArea.setPrefRowCount(3);
        translatedArea.setEditable(false);
        
        content.getChildren().addAll(originalLabel, originalArea, translatedLabel, translatedArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }
    
    /**
     * 一键翻译当前幻灯片内容（异步，无等待窗口，AI提示词极致收紧）
     */
    private void translateCurrentSlideContent(MultilingualSupport.SupportedLanguage targetLanguage) {
        if (currentSlide == null) {
            showError("翻译失败", "当前没有选中的幻灯片");
            return;
        }
        List<SlideElement> elements = currentSlide.getElements();
        List<TextElement> textElements = new ArrayList<>();
        for (SlideElement element : elements) {
            if (element instanceof TextElement) {
                textElements.add((TextElement) element);
            }
        }
        if (textElements.isEmpty()) {
            showError("翻译失败", "当前幻灯片没有可翻译的文本内容");
            return;
        }
        Task<StringBuilder> translationTask = new Task<>() {
            @Override
            protected StringBuilder call() {
                StringBuilder translationLog = new StringBuilder();
                int count = 0;
                for (TextElement textElement : textElements) {
                    String originalText = textElement.getText();
                    if (originalText != null && !originalText.trim().isEmpty()) {
                        String translatedText = MultilingualSupport.generateMultilingualContent(originalText,
                                targetLanguage);
                        if (translatedText.equals(originalText)) {
                            // 极致收紧AI提示词，明确禁止输出结构字段
                            String prompt = String.format(
                                    "请将下列内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Text、Page X:等），保持原有排版。只输出翻译结果本身，不要任何注释、说明、Note、括号内容、示例、解释等。如果遇到占位符（如[你的姓名/职位]），请原样保留，不要输出任何说明。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                                    targetLanguage.getDisplayName(), originalText);
                            try {
                                translatedText = aiModel.chat(prompt).trim();
                            } catch (Exception ex) {
                                translatedText = "[AI翻译失败] " + originalText;
                            }
                        }
                        // 使用stripPPTStructureFields处理，确保统计和日志只显示内容本身
                        String cleanOriginal = stripPPTStructureFields(originalText);
                        String cleanTranslated = stripPPTStructureFields(translatedText);
                        translationLog.append(String.format("原文: %s\n译文: %s\n", cleanOriginal, cleanTranslated));
                        textElement.setText(translatedText);
                        count++;
                    }
                }
                translationLog.insert(0,
                        String.format("已翻译 %d 个文本元素为: %s\n\n", count, targetLanguage.getDisplayName()));
                return translationLog;
            }
        };
        translationTask.setOnSucceeded(e -> {
            // 翻译后自动优化布局
            try {
                AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
                enhancedAgent.optimizeSlideLayout(currentSlide, canvas.getWidth(), canvas.getHeight(),
                    IntelligentLayoutEngine.LayoutType.CENTERED);
            } catch (Exception ex) {
                logger.warning("翻译后自动优化布局失败: " + ex.getMessage());
            }
            refreshCanvas();
            showTranslationResultDialog(translationTask.getValue().toString(), textElements.size(), targetLanguage);
        });
        translationTask.setOnFailed(e -> {
            showError("翻译失败", "翻译过程中发生错误: " + translationTask.getException().getMessage());
        });
        new Thread(translationTask).start();
    }

    /**
     * 批量翻译所有幻灯片内容（异步，无等待窗口，AI提示词极致收紧）
     */
    private void translateAllSlidesContent(MultilingualSupport.SupportedLanguage targetLanguage) {
        if (slides.isEmpty()) {
            showError("翻译失败", "没有可翻译的幻灯片");
            return;
        }
        Task<StringBuilder> translationTask = new Task<>() {
            @Override
            protected StringBuilder call() {
                int translatedSlides = 0;
                int translatedElements = 0;
                StringBuilder translationLog = new StringBuilder();
                for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
                    Slide slide = slides.get(slideIndex);
                    List<SlideElement> elements = slide.getElements();
                    boolean slideTranslated = false;
                    translationLog.append(String.format("\n=== 幻灯片 %d ===\n", slideIndex + 1));
                    for (SlideElement element : elements) {
                        if (element instanceof TextElement) {
                            TextElement textElement = (TextElement) element;
                            String originalText = textElement.getText();
                            if (originalText != null && !originalText.trim().isEmpty()) {
                                String translatedText = MultilingualSupport.generateMultilingualContent(originalText,
                                        targetLanguage);
                                if (translatedText.equals(originalText)) {
                                    String prompt = String.format(
                                            "请将下列内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Text、Page X:等），保持原有排版。只输出翻译结果本身，不要任何注释、说明、Note、括号内容、示例、解释等。如果遇到占位符（如[你的姓名/职位]），请原样保留，不要输出任何说明。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                                            targetLanguage.getDisplayName(), originalText);
                                    try {
                                        translatedText = aiModel.chat(prompt).trim();
                                    } catch (Exception ex) {
                                        translatedText = "[AI翻译失败] " + originalText;
                                    }
                                }
                                // 使用stripPPTStructureFields处理，确保统计和日志只显示内容本身
                                String cleanOriginal = stripPPTStructureFields(originalText);
                                String cleanTranslated = stripPPTStructureFields(translatedText);
                                translationLog
                                        .append(String.format("原文: %s\n译文: %s\n", cleanOriginal, cleanTranslated));
                                textElement.setText(translatedText);
                                translatedElements++;
                                slideTranslated = true;
                            }
                        }
                    }
                    if (slideTranslated) {
                        translatedSlides++;
                    }
                }
                translationLog.insert(0, String.format("已翻译 %d 个幻灯片，共 %d 个文本元素为: %s\n\n", translatedSlides,
                        translatedElements, targetLanguage.getDisplayName()));
                return translationLog;
            }
        };
        translationTask.setOnSucceeded(e -> {
            // 批量翻译后自动优化所有幻灯片布局
            try {
                AIEnhancedAgent enhancedAgent = new AIEnhancedAgent(aiModel);
                for (Slide slide : slides) {
                    enhancedAgent.optimizeSlideLayout(slide, canvas.getWidth(), canvas.getHeight(),
                        IntelligentLayoutEngine.LayoutType.CENTERED);
                }
            } catch (Exception ex) {
                logger.warning("批量翻译后自动优化布局失败: " + ex.getMessage());
            }
            if (currentSlide != null)
                refreshCanvas();
            // 从translationLog中提取实际的翻译统计信息
            String translationLog = translationTask.getValue().toString();
            String[] lines = translationLog.split("\n");
            int actualTranslatedSlides = 0;
            int actualTranslatedElements = 0;
            // 从第一行提取统计信息
            if (lines.length > 0 && lines[0].contains("已翻译")) {
                String firstLine = lines[0];
                if (firstLine.contains("个幻灯片，共") && firstLine.contains("个文本元素")) {
                    try {
                        String parts = firstLine.split("已翻译 ")[1].split(" 个幻灯片，共 ")[0];
                        actualTranslatedSlides = Integer.parseInt(parts);
                        String elementsPart = firstLine.split(" 个幻灯片，共 ")[1].split(" 个文本元素")[0];
                        actualTranslatedElements = Integer.parseInt(elementsPart);
                    } catch (Exception ex) {
                        actualTranslatedSlides = 0;
                        actualTranslatedElements = 0;
                    }
                }
            }
            showBatchTranslationResultDialog(translationLog, actualTranslatedSlides, actualTranslatedElements,
                    targetLanguage);
        });
        translationTask.setOnFailed(e -> {
            showError("批量翻译失败", "翻译过程中发生错误: " + translationTask.getException().getMessage());
        });
        new Thread(translationTask).start();
    }
    
    /**
     * 显示翻译结果对话框
     */
    private void showTranslationResultDialog(String translationLog, int translatedCount, 
                                          MultilingualSupport.SupportedLanguage targetLanguage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("翻译完成");
        dialog.setHeaderText(String.format("已翻译 %d 个文本元素为: %s", translatedCount, targetLanguage.getDisplayName()));
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        Label summaryLabel = new Label(String.format("翻译统计: %d 个文本元素", translatedCount));
        summaryLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea logArea = new TextArea(translationLog);
        logArea.setPrefRowCount(10);
        logArea.setEditable(false);
        logArea.setPromptText("翻译详情...");
        
        content.getChildren().addAll(summaryLabel, new Label("翻译详情:"), logArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }
    
    /**
     * 显示批量翻译结果对话框
     */
    private void showBatchTranslationResultDialog(String translationLog, int translatedSlides, int translatedElements,
                                               MultilingualSupport.SupportedLanguage targetLanguage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("批量翻译完成");
        dialog.setHeaderText(String.format("已翻译 %d 个幻灯片，共 %d 个文本元素为: %s", 
            translatedSlides, translatedElements, targetLanguage.getDisplayName()));
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        Label summaryLabel = new Label(
                String.format("翻译统计: %d 个幻灯片，%d 个文本元素", translatedSlides, translatedElements));
        summaryLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea logArea = new TextArea(translationLog);
        logArea.setPrefRowCount(15);
        logArea.setEditable(false);
        logArea.setPromptText("翻译详情...");
        
        content.getChildren().addAll(summaryLabel, new Label("翻译详情:"), logArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }
    
    /**
     * 批量翻译所有内容
     */

    /**
     * 翻译PPT命令文本，仅翻译内容部分，保留格式字段和排版
     */
    private String translatePPTCommandsWithAI(String pptCommandText,
            MultilingualSupport.SupportedLanguage targetLanguage) {
        // 优化AI提示词，要求仅翻译冒号后的内容，保留格式字段和排版，不要添加任何解释
        String prompt = String.format(
                "请将下列PPT命令内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Text、Page X:等），保持原有排版，不要添加任何解释、说明或多余内容。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                targetLanguage.getDisplayName(), pptCommandText);
        try {
            String translated = aiModel.chat(prompt).trim();
            return translated;
        } catch (Exception ex) {
            return "[AI翻译失败] " + pptCommandText;
        }
    }

    // 使用示例：
    // String translatedCommands = translatePPTCommandsWithAI(originalCommands,
    // MultilingualSupport.SupportedLanguage.ENGLISH);
    // parseAndCreateSlides(translatedCommands);

    /**
     * 去除PPT命令结构字段，仅保留内容（终极版：连续结构字段行后只保留第一个非结构字段内容行，其余全部丢弃）
     */
    private String stripPPTStructureFields(String pptCommandText) {
        if (pptCommandText == null || pptCommandText.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String[] lines = pptCommandText.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // 如果是结构字段行（包括Title:、Subtitle:、Bullet:、Draw:、Page X:等）
            if (line.matches("^(Title|Subtitle|Bullet|Draw|Page|Text ?\\d*|Page X|Note|Comment|说明|注释):\\s*(.*)")) {
                String content = line
                        .replaceFirst("^(Title|Subtitle|Bullet|Draw|Page|Text ?\\d*|Page X|Note|Comment|说明|注释):\\s*", "");
                if (!content.isEmpty()) {
                    sb.append(content).append('\n');
                } else {
                    // 跳过连续结构字段行，直到遇到内容行
                    while (i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (nextLine
                                .matches("^(Title|Subtitle|Bullet|Draw|Page|Text ?\\d*|Page X|Note|Comment|说明|注释):\\s*.*")
                                || nextLine.isEmpty()) {
                            i++;
                        } else {
                            sb.append(nextLine).append('\n');
                            i++;
                            break;
                        }
                    }
                }
            } else if (!line.isEmpty() && !line.matches("^---.*---$")) {
                // 不是结构字段，也不是分隔符，直接保留
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    /**
     * 翻译并渲染PPT命令文本（整体翻译后自动渲染，仅保留内容，自动智能排版）
     */
    private void translateAndRenderPPTCommands(String pptCommandText,
            MultilingualSupport.SupportedLanguage targetLanguage) {
        String prompt = String.format(
                "请将下列PPT命令内容翻译为%s，仅翻译每一行冒号后的内容，保留格式字段（如Title、Subtitle、Bullet、Draw、Text、Page X:等），保持原有排版。只输出翻译结果本身，不要任何注释、说明、Note、括号内容、示例、解释等。重要：不要输出任何结构字段如'Title:'、'Subtitle:'等，只输出内容本身：\n%s",
                targetLanguage.getDisplayName(), pptCommandText);
        Task<String> translationTask = new Task<>() {
            @Override
            protected String call() {
                try {
                    String translated = aiModel.chat(prompt).trim();
                    return translated;
                } catch (Exception ex) {
                    return "[AI翻译失败] " + pptCommandText;
                }
            }
        };
        translationTask.setOnSucceeded(e -> {
            String translatedCommands = translationTask.getValue();
            // 去除结构字段，只保留内容
            String contentOnly = stripPPTStructureFields(translatedCommands);
            parseAndCreateSlides(contentOnly);
            // 自动智能排版
            if (!slides.isEmpty()) {
                for (Slide slide : slides) {
                    IntelligentLayoutEngine.optimizeLayout(
                        slide,
                        canvas.getWidth(),
                        canvas.getHeight(),
                            IntelligentLayoutEngine.LayoutType.CENTERED);
                }
                refreshCanvas();
            }
            // 使用stripPPTStructureFields处理，确保提示信息也只显示内容本身
            String cleanContent = stripPPTStructureFields(contentOnly);
            showInfo("PPT翻译并渲染完成", "已将PPT命令翻译为 " + targetLanguage.getDisplayName() + " 并自动排版渲染，仅保留内容");
        });
        translationTask.setOnFailed(e -> {
            showError("PPT翻译失败", "翻译过程中发生错误: " + translationTask.getException().getMessage());
        });
        new Thread(translationTask).start();
    }

    // 使用示例：
    // translateAndRenderPPTCommands(originalCommands,
    // MultilingualSupport.SupportedLanguage.JAPANESE);

    // ==================== 结构分析功能 ====================

    /**
     * 分析幻灯片结构
     */
    private void analyzeSlideStructure() {
        if (slides.isEmpty()) {
            showError("分析失败", "没有可分析的幻灯片");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("分析中");
        progressAlert.setHeaderText("正在分析幻灯片结构...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行分析
        new Thread(() -> {
            try {
                StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    showStructureAnalysisResult(analysis);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("分析失败", "分析幻灯片结构时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示结构分析结果
     */
    private void showStructureAnalysisResult(StructureAnalysis analysis) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("幻灯片结构分析结果");
        dialog.setHeaderText("分析完成");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        Label summaryLabel = new Label(String.format("幻灯片数量: %d, 元素总数: %d", 
            analysis.getTotalSlides(), analysis.getTotalElements()));
        summaryLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea resultArea = new TextArea(analysis.toString());
        resultArea.setPrefRowCount(20);
        resultArea.setEditable(false);
        resultArea.setPromptText("分析结果...");
        
        content.getChildren().addAll(summaryLabel, new Label("详细分析结果:"), resultArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    /**
     * 生成智能大纲
     */
    private void generateSmartOutline() {
        if (slides.isEmpty()) {
            showError("生成失败", "没有可生成大纲的幻灯片");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("生成中");
        progressAlert.setHeaderText("正在生成智能大纲...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行生成
        new Thread(() -> {
            try {
                String outline = SlideStructureAnalyzer.generateAnalysisReport(
                        SlideStructureAnalyzer.analyzeStructure(slides));
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    showSmartOutlineResult(outline);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("生成失败", "生成智能大纲时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示智能大纲结果
     */
    private void showSmartOutlineResult(String outline) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("智能大纲生成结果");
        dialog.setHeaderText("大纲生成完成");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        TextArea outlineArea = new TextArea(outline);
        outlineArea.setPrefRowCount(25);
        outlineArea.setEditable(false);
        outlineArea.setPromptText("智能大纲...");
        
        content.getChildren().addAll(new Label("生成的智能大纲:"), outlineArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    /**
     * 重点内容分析
     */
    private void analyzeKeyPoints() {
        if (slides.isEmpty()) {
            showError("分析失败", "没有可分析重点的幻灯片");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("分析中");
        progressAlert.setHeaderText("正在分析重点内容...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行分析
        new Thread(() -> {
            try {
                StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
                StringBuilder keyPointsText = new StringBuilder();
                keyPointsText.append("=== 重点内容分析 ===\n\n");
                
                keyPointsText.append("【核心重点】\n");
                for (int i = 0; i < Math.min(analysis.getKeyPoints().size(), 10); i++) {
                    keyPointsText.append(i + 1).append(". ").append(analysis.getKeyPoints().get(i)).append("\n");
                }
                
                keyPointsText.append("\n【关键词统计】\n");
                analysis.getKeywordFrequency().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(8)
                        .forEach(entry -> keyPointsText.append(entry.getKey()).append(": ").append(entry.getValue())
                                .append("次\n"));
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    showKeyPointsResult(keyPointsText.toString());
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("分析失败", "分析重点内容时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示重点内容分析结果
     */
    private void showKeyPointsResult(String keyPointsText) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("重点内容分析结果");
        dialog.setHeaderText("分析完成");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        TextArea resultArea = new TextArea(keyPointsText);
        resultArea.setPrefRowCount(20);
        resultArea.setEditable(false);
        resultArea.setPromptText("重点内容分析...");
        
        content.getChildren().addAll(new Label("重点内容分析:"), resultArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    /**
     * 生成逻辑关系图
     */
    private void generateLogicGraph() {
        if (slides.isEmpty()) {
            showError("生成失败", "没有可生成关系图的幻灯片");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("生成中");
        progressAlert.setHeaderText("正在生成逻辑关系图...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行生成
        new Thread(() -> {
            try {
                StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
                String graphData = SlideStructureAnalyzer.generateLogicGraphData(analysis);
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    showLogicGraphResult(graphData);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("生成失败", "生成逻辑关系图时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示逻辑关系图结果
     */
    private void showLogicGraphResult(String graphData) {
        // 创建选项对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("逻辑关系图");
        alert.setHeaderText("选择显示方式");
        alert.setContentText("请选择如何显示逻辑关系图：");
        
        ButtonType showVisualButton = new ButtonType("可视化显示");
        ButtonType showDataButton = new ButtonType("显示数据");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(showVisualButton, showDataButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == showVisualButton) {
                // 显示可视化图形
                showVisualLogicGraph(graphData);
            } else if (result.get() == showDataButton) {
                // 显示原始数据
                showLogicGraphData(graphData);
            }
        }
    }
    
    /**
     * 显示可视化逻辑关系图
     */
    private void showVisualLogicGraph(String graphData) {
        try {
            // 创建新窗口显示可视化图形
            LogicGraphRenderer.showLogicGraph(graphData);
        } catch (Exception e) {
            showError("可视化失败", "无法显示可视化图形: " + e.getMessage());
            // 如果可视化失败，回退到显示数据
            showLogicGraphData(graphData);
        }
    }
    
    /**
     * 显示逻辑关系图数据
     */
    private void showLogicGraphData(String graphData) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("逻辑关系图数据");
        dialog.setHeaderText("生成完成");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType visualizeButton = new ButtonType("可视化显示");
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType, visualizeButton);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        Label infoLabel = new Label("逻辑关系图数据已生成，可用于可视化展示");
        infoLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea graphArea = new TextArea(graphData);
        graphArea.setPrefRowCount(15);
        graphArea.setEditable(false);
        graphArea.setPromptText("逻辑关系图数据...");
        
        content.getChildren().addAll(infoLabel, new Label("图数据:"), graphArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        // 添加可视化按钮事件
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == visualizeButton) {
                showVisualLogicGraph(graphData);
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    /**
     * 生成完整分析报告
     */
    private void generateCompleteReport() {
        if (slides.isEmpty()) {
            showError("生成失败", "没有可生成报告的幻灯片");
            return;
        }

        // 显示进度提示
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("生成中");
        progressAlert.setHeaderText("正在生成完整分析报告...");
        progressAlert.setContentText("请稍候");
        progressAlert.setResizable(false);
        progressAlert.show();

        // 在新线程中执行生成
        new Thread(() -> {
            try {
                StructureAnalysis analysis = SlideStructureAnalyzer.analyzeStructure(slides);
                String completeReport = SlideStructureAnalyzer.generateAnalysisReport(analysis);
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    showCompleteReportResult(completeReport);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showError("生成失败", "生成完整分析报告时发生错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示完整分析报告结果
     */
    private void showCompleteReportResult(String completeReport) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("完整分析报告");
        dialog.setHeaderText("报告生成完成");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 创建内容显示区域
        VBox content = new VBox(10);
        
        TextArea reportArea = new TextArea(completeReport);
        reportArea.setPrefRowCount(30);
        reportArea.setEditable(false);
        reportArea.setPromptText("完整分析报告...");
        
        content.getChildren().addAll(new Label("完整分析报告:"), reportArea);
        content.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
    }

    // AI思考链可视化步骤
    public static class AIChainStep {
        private String title;
        private String detail;
        private StepStatus status;

        public enum StepStatus {
            WAITING, RUNNING, DONE
        }

        public AIChainStep(String title, String detail, StepStatus status) {
            this.title = title;
            this.detail = detail;
            this.status = status;
        }

        // getter/setter略
        public String getTitle() {
            return title;
        }

        public String getDetail() {
            return detail;
        }

        public StepStatus getStatus() {
            return status;
        }

        public void setStatus(StepStatus status) {
            this.status = status;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        // 处理双击事件
        if (event.getClickCount() == 2) {
            if (currentSlide != null) {
                SlideElement clickedElement = currentSlide.findElementAt(event.getX(), event.getY());
                if (clickedElement instanceof TextElement) {
                    // 双击文本元素，进入编辑模式
                    selectedElement = clickedElement;
                    selectedElement.setSelected(true);
                    refreshCanvas();
                    // 使用与右键编辑相同的对话框
                    editSelectedText();
                }
            }
        } else if (event.getClickCount() == 1) {
            // 单击时同步样式到工具栏
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                // 这里需要访问工具栏的控件，暂时注释掉，后续可以通过其他方式实现
                // colorPicker.setValue(textElement.getColor());
                // fontSizeCombo.setValue((int) textElement.getFontSize());
                // String style = textElement.getFontWeight() == FontWeight.BOLD ? "Bold"
                //         : textElement.isItalic() ? "Italic" : "Regular";
                // fontStyleCombo.setValue(style);
            }
        }
    }

    // 内联编辑功能已移除，现在统一使用标准对话框
    
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    // 新增方法：弹出智能生成提示词窗口
    private void showPromptGeneratorDialog() {
        PromptGeneratorDialog dialog = new PromptGeneratorDialog(aiAgent);
        dialog.show();
    }

    // 新增：统一刷新主题色方法
    private void applyTheme() {
        // 顶部栏
        if (topBox != null) topBox.setStyle("-fx-background-color:" + mainBgColor + ";-fx-border-width:0 0 1 0;-fx-border-color:" + borderColor + ";");
        // 工具栏
        if (toolBar != null) toolBar.setStyle("-fx-background-color:" + mainBgColor + ";-fx-border-radius:16;-fx-background-radius:16;-fx-padding:8 15;-fx-spacing:8;-fx-border-width:0 0 1 0;-fx-border-color:" + borderColor + ";");
        // 侧边栏
        if (sidebar != null) sidebar.setStyle("-fx-background-color:" + mainBgColor + ";-fx-border-width:0 1 0 0;-fx-border-color:" + borderColor + ";-fx-border-radius:16;-fx-background-radius:16;");
        // 画布
        if (canvasHolder != null) canvasHolder.setStyle("-fx-background-color:" + canvasBgColor + ";-fx-border-radius:24;-fx-background-radius:24;");
    }

    /**
     * 保存当前幻灯片元素快照到撤销栈
     */
    private void pushUndoSnapshot() {
        undoStack.push(new SlidesSnapshot(slides, currentSlideIndex));
        redoStack.clear();
    }

    /**
     * 撤销上一步操作（恢复整个幻灯片列表和当前页）
     */
    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new SlidesSnapshot(slides, currentSlideIndex));
            SlidesSnapshot prev = undoStack.pop();
            restoreSnapshot(prev);
        }
    }

    /**
     * 重做（恢复上一次撤销的内容）
     */
    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new SlidesSnapshot(slides, currentSlideIndex));
            SlidesSnapshot next = redoStack.pop();
            restoreSnapshot(next);
        }
    }

    /**
     * 恢复快照内容到当前状态
     */
    private void restoreSnapshot(SlidesSnapshot snapshot) {
        slides = new ArrayList<>();
        for (slideshow.model.Slide s : snapshot.slides) {
            slides.add(s.deepClone());
        }
        currentSlideIndex = snapshot.currentSlideIndex;
        currentSlide = slides.isEmpty() ? null : slides.get(currentSlideIndex);
        selectedElement = null;
        refreshCanvas();
        updateSlideControls();
    }

    /**
     * 剪切选中元素
     */
    private void cutSelectedElement() {
        if (selectedElement != null && currentSlide != null) {
            pushUndoSnapshot();
            clipboardElement = selectedElement.deepClone();
            currentSlide.removeElement(selectedElement);
            selectedElement = null;
            refreshCanvas();
            pushUndoSnapshot(); // 剪切后入栈
        }
    }

    /**
     * 复制选中元素
     */
    private void copySelectedElement() {
        if (selectedElement != null) {
            clipboardElement = selectedElement.deepClone();
        }
    }

    /**
     * 粘贴剪贴板元素到当前幻灯片中央
     */
    private void pasteClipboardElement() {
        if (clipboardElement != null && currentSlide != null) {
            pushUndoSnapshot();
            SlideElement pasted = clipboardElement.deepClone();
            // 粘贴到画布中央
            double centerX = canvas.getWidth() / 2;
            double centerY = canvas.getHeight() / 2;
            pasted.setPosition(centerX, centerY);
            currentSlide.addElement(pasted);
            selectedElement = pasted;
            refreshCanvas();
            pushUndoSnapshot(); // 粘贴后入栈
        }
    }

    /**
     * 删除当前幻灯片
     */
    private void deleteCurrentSlide() {
        if (slides.isEmpty() || currentSlideIndex < 0) return;
        slides.remove(currentSlideIndex);
        if (slides.isEmpty()) {
            createNewSlide();
        } else {
            if (currentSlideIndex >= slides.size()) {
                currentSlideIndex = slides.size() - 1;
            }
            currentSlide = slides.get(currentSlideIndex);
        }
        refreshCanvas();
        updateSlideControls();
    }
}