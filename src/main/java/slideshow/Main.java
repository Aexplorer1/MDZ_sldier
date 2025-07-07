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

import slideshow.util.Constants;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.ImageElement;
import slideshow.util.UIStrings;
import slideshow.util.SlideSerializer;
import slideshow.util.SlideParser;
import slideshow.presentation.PresentationWindow;
import slideshow.elements.DrawElement;
import dev.langchain4j.model.openai.OpenAiChatModel;
import slideshow.model.PromptTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * MDZ_Slider main application class
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

    @Override
    public void start(Stage primaryStage) {
        logger.info("Application starting...");
        BorderPane root = new BorderPane();

        // Create canvas
        canvas = new Canvas(Constants.DEFAULT_SLIDE_WIDTH, Constants.DEFAULT_SLIDE_HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();

        // Add mouse event handling
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);

        // Create menu bar and tool bar
        MenuBar menuBar = createMenuBar();
        ToolBar toolBar = createToolBar();

        // Create top container
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        root.setTop(topContainer);

        // Set canvas container
        BorderPane canvasHolder = new BorderPane(canvas);
        canvasHolder.getStyleClass().add("canvas-holder");
        root.setCenter(canvasHolder);

        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);

        // Load CSS styles
        try {
            String cssPath = getClass().getResource("/styles/theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            logger.info("CSS styles loaded successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load CSS file", e);
        }

        primaryStage.setTitle("MDZ_Slider");
        primaryStage.setScene(scene);
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
    }

    // private void testAIMessage() {
    // String testPrompt = "Hello, can you respond?";
    // try {
    // String response = aiModel.chat(testPrompt);
    // logger.info("Test message response: " + response);
    // Platform.runLater(() -> showInfo("AI Test Successful", "Response: " +
    // response));
    // } catch (Exception e) {
    // logger.log(Level.SEVERE, "Failed to send test message", e);
    // Platform.runLater(() -> showError("AI Test Failed", "Error: " +
    // e.getMessage()));
    // }
    // }

    private String getApiKey() {
        // Retrieve from environment variable first
        String key = System.getenv("DEEPSEEK_API_KEY");
        // logger.info("API Key: " + key);
        if (key == null) {
            throw new RuntimeException("API Key not configured");
        }
        return key;
    }

    private void handleMousePressed(MouseEvent event) {
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
            refreshCanvas();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (currentDrawing != null) {

            // Update drawing
            currentDrawing.updateEndPoint(event.getX(), event.getY());
            refreshCanvas();
            return;
        }
        if (selectedElement != null && currentResizeHandle != SlideElement.ResizeHandle.NONE) {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
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

    private void refreshCanvas() {
        // Clear canvas
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Redraw all elements
        if (currentSlide != null) {
            System.out.println("Refreshing canvas");
            currentSlide.draw(graphicsContext);
        }
    }

    private ToolBar createToolBar() {
        Button newSlideBtn = new Button(UIStrings.NEW_SLIDE);
        Button addTextBtn = new Button(UIStrings.ADD_TEXT);
        Button addImageBtn = new Button(UIStrings.ADD_IMAGE);

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

        // Sync styles when selected element changes
        canvas.setOnMouseClicked(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                colorPicker.setValue(textElement.getColor());
                fontSizeCombo.setValue((int) textElement.getFontSize());
                String style = textElement.getFontWeight() == FontWeight.BOLD ? "Bold"
                        : textElement.isItalic() ? "Italic" : "Regular";
                fontStyleCombo.setValue(style);
            }
        });

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

        Button aiGenBtn = new Button("AI智能生成PPT");
        aiGenBtn.getStyleClass().add("button");
        aiGenBtn.setOnAction(e -> showAIChatDialog());

        Button speechGenBtn = new Button("生成演讲稿");
        speechGenBtn.getStyleClass().add("button");
        speechGenBtn.setOnAction(e -> generateSpeechFromSlides());

        Button speechStructureBtn = new Button("演讲稿结构");
        speechStructureBtn.getStyleClass().add("button");
        speechStructureBtn.setOnAction(e -> showSpeechStructureDialog());

        // Add keyword analysis button
        Button keywordAnalysisBtn = new Button("关键词分析");
        keywordAnalysisBtn.getStyleClass().add("button");
        keywordAnalysisBtn.setOnAction(e -> performKeywordAnalysis());

        // Add template management button
        Button templateManageBtn = new Button("Template Manager");
        templateManageBtn.getStyleClass().add("button");
        templateManageBtn.setOnAction(e -> openTemplateManager());

        return new ToolBar(
                newSlideBtn,
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
                rectBtn, circleBtn, lineBtn, arrowBtn,
                drawColorPicker,
                lineWidthComboBox,
                new Separator(),
                aiGenBtn,
                speechGenBtn,
                speechStructureBtn,
                keywordAnalysisBtn,
                new Separator(),
                templateManageBtn);
    }

    private void createNewSlide() {
        Slide newSlide = new Slide();
        slides.add(newSlide);
        currentSlideIndex = slides.size() - 1;
        currentSlide = newSlide;
        refreshCanvas();
        updateSlideControls(); // Update slide control button states
    }

    private void addText() {
        TextInputDialog dialog = new TextInputDialog("New Text");
        dialog.setTitle("Add Text");
        dialog.setHeaderText("Enter text content:");
        dialog.setContentText("Text:");

        dialog.showAndWait().ifPresent(text -> {
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
        });
    }

    private void addImage() {
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
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteElement(element));
        contextMenu.getItems().add(deleteItem);
        contextMenu.show(canvas, x, y);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.BACK_SPACE && selectedElement != null) {
            deleteElement(selectedElement);
        }
    }

    private void deleteElement(SlideElement element) {
        if (currentSlide != null) {
            currentSlide.removeElement(element);
            if (element == selectedElement) {
                selectedElement = null;
            }
            refreshCanvas();
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

        menuBar.getMenus().addAll(fileMenu, editMenu, playMenu);

        // Add event handling
        newItem.setOnAction(e -> createNewPresentation());
        openItem.setOnAction(e -> openPresentation());
        saveItem.setOnAction(e -> savePresentation());
        saveAsItem.setOnAction(e -> saveAsPresentation());
        exitItem.setOnAction(e -> Platform.exit());
        startItem.setOnAction(e -> startPresentation());

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
        PresentationWindow presentation = new PresentationWindow(slides);
        presentation.start();
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
        resultDialog.getButtonTypes().setAll(closeButtonType, copyButtonType);

        TextArea speechArea = new TextArea(speech);
        speechArea.setPrefRowCount(15);
        speechArea.setPrefColumnCount(60);
        speechArea.setWrapText(true);
        speechArea.setEditable(false);

        resultDialog.getDialogPane().setContent(speechArea);

        // 显示对话框并处理结果
        Optional<ButtonType> result = resultDialog.showAndWait();
        if (result.isPresent() && result.get() == copyButtonType) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(speech);
            clipboard.setContent(content);
            showInfo("复制成功", "演讲稿已复制到剪贴板");
        }
    }

    private void showAIChatDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI智能生成PPT");
        dialog.setHeaderText("选择模板并输入需求，点击*生成建议*后可查看AI建议、PPT命令和演讲稿，编辑命令后点击*生成PPT并保持窗口*生成幻灯片（窗口不会自动关闭）");

        ButtonType generateBtnType = new ButtonType("生成建议", ButtonBar.ButtonData.LEFT);
        ButtonType confirmBtnType = new ButtonType("生成PPT并保持窗口", ButtonBar.ButtonData.OTHER);
        ButtonType cancelBtnType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(generateBtnType, confirmBtnType, cancelBtnType);

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
                        setText("不使用模板");
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
        PromptTemplate noTemplate = new PromptTemplate("不使用模板", "直接使用默认提示词", "",
                slideshow.model.TemplateCategory.CUSTOM);
        noTemplate.setId("no-template");
        templateCombo.getItems().add(0, noTemplate);
        templateCombo.setValue(noTemplate);

        // 显示当前选择的模板信息
        Label templateInfoLabel = new Label("当前模板: 不使用模板");
        templateInfoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        // 模板选择监听器
        templateCombo.setOnAction(e -> {
            PromptTemplate selected = templateCombo.getValue();
            if (selected != null) {
                if ("no-template".equals(selected.getId())) {
                    templateInfoLabel.setText("当前模板: 不使用模板 (使用默认提示词)");
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
        suggestionArea.setPrefRowCount(10);
        suggestionArea.setWrapText(true);
        suggestionArea.setEditable(true);
        suggestionArea.setDisable(true);

        VBox vbox = new VBox(10,
                new Label("选择模板："), templateCombo, templateInfoLabel,
                new Label("PPT需求："), inputArea,
                new Label("AI建议与反馈（只读）："), adviceArea,
                new Label("PPT命令与大纲（可编辑）："), suggestionArea);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        // 生成建议按钮逻辑
        Button generateBtn = (Button) dialog.getDialogPane().lookupButton(generateBtnType);
        generateBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // 阻止关闭对话框
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

            // 创建AI思考提示
            final long startTime = System.currentTimeMillis();

            // 创建时间更新器
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
                                "\nDraw: [绘图描述]" +
                                "\nPage 2:" +
                                "\nTitle: [页面标题]" +
                                "\nSubtitle: [页面副标题]" +
                                "\nBullet: [项目符号内容]" +
                                "\nDraw: [绘图描述]" +
                                "\n（继续更多页面...）" +
                                "\n\n请确保使用'---PPT命令---'分隔符，并严格按照Page X:格式分页。" +
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

                try {
                    String aiResult = aiModel.chat(aiPrompt);
                    Platform.runLater(() -> {
                        // 停止时间更新器
                        timeTimeline.stop();

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
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        // 停止时间更新器
                        timeTimeline.stop();

                        adviceArea.setText("AI生成失败：" + e.getMessage());
                        suggestionArea.setText("");
                        adviceArea.setDisable(false);
                        suggestionArea.setDisable(false);
                    });
                }
            }).start();
        });

        // 生成PPT按钮逻辑
        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmBtnType);
        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String suggestion = suggestionArea.getText().trim();
            if (suggestion.isEmpty() || adviceArea.getText().startsWith("AI正在思考")) {
                event.consume();
                suggestionArea.setText("请先生成并确认PPT命令！");
                return;
            }

            System.out.println("Main: 开始生成PPT，PPT命令内容:");
            System.out.println(suggestion);
            System.out.println("Main: PPT命令内容长度: " + suggestion.length());

            // 生成PPT但不关闭对话框
            Platform.runLater(() -> {
                parseAndCreateSlides(suggestion);
                // 在对话框内显示成功信息，而不是弹出新窗口
                adviceArea.setText("✓ PPT已成功生成！您可以继续查看和编辑AI建议，或关闭窗口。");
            });
            
            // 阻止对话框关闭
            event.consume();
        });

        // 初始时禁用"生成PPT"按钮
        confirmBtn.setDisable(true);
        suggestionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            confirmBtn.setDisable(newVal.trim().isEmpty() || adviceArea.getText().startsWith("AI正在思考"));
        });

        dialog.showAndWait();
    }

    private void parseAndCreateSlides(String aiResult) {
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
                "Page 1:\nTitle: ...\nSubtitle: ...\nBullet: ...\nDraw: ...\nPage 2: ...\n（每个命令单独一行，所有命令都在---PPT命令---下方，若无PPT需求则此部分可为空）\n"
                +
                "请严格用'---PPT命令---'分隔建议和命令部分。\n用户输入：" + userPrompt;
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

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // 如果内容看起来像PPT结构，尝试转换为标准格式
            if (line.contains("标题") || line.contains("Title") || line.contains("主题")) {
                if (hasContent) {
                    result.append("\n");
                }
                result.append("Page ").append(pageNumber).append(":\n");
                result.append("Title: ").append(line.replaceAll(".*[标题|Title|主题][：:]*\\s*", "")).append("\n");
                pageNumber++;
                hasContent = true;
            } else if (line.contains("副标题") || line.contains("Subtitle")) {
                result.append("Subtitle: ").append(line.replaceAll(".*[副标题|Subtitle][：:]*\\s*", "")).append("\n");
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
            } else if (!line.startsWith("Page") && !line.startsWith("---")) {
                // 其他内容作为普通文本
                result.append("Bullet: ").append(line).append("\n");
            }
        }

        // 如果没有找到任何结构化的内容，创建一个简单的页面
        if (!hasContent) {
            result.append("Page 1:\n");
            result.append("Title: ").append(content.substring(0, Math.min(50, content.length()))).append("\n");
            result.append("Subtitle: 内容概述\n");
            result.append("Bullet: ").append(content).append("\n");
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
        Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
        resultDialog.setTitle("演讲稿结构");
        resultDialog.setHeaderText("生成的演讲稿结构");

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButtonType = new ButtonType("复制", ButtonBar.ButtonData.OTHER);
        resultDialog.getButtonTypes().setAll(closeButtonType, copyButtonType);

        // 创建文本区域
        TextArea textArea = new TextArea(speechStructure);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(60);
        textArea.setWrapText(true);
        textArea.setEditable(false);

        resultDialog.getDialogPane().setContent(textArea);

        // 显示对话框并处理结果
        Optional<ButtonType> result = resultDialog.showAndWait();
        if (result.isPresent() && result.get() == copyButtonType) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(speechStructure);
            clipboard.setContent(content);
            showInfo("复制成功", "演讲稿结构已复制到剪贴板");
        }
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
     * 打开模板管理窗口
     */
    private void openTemplateManager() {
        try {
            slideshow.presentation.TemplateManagerWindow templateWindow = new slideshow.presentation.TemplateManagerWindow();
            templateWindow.show();
            logger.info("Template manager window opened successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open template manager window", e);
            showError("错误", "无法打开模板管理窗口: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}