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
import slideshow.presentation.PresentationWindow;
import slideshow.elements.DrawElement;
import dev.langchain4j.model.openai.OpenAiChatModel;

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
    public void     start(Stage primaryStage) {
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
                 .baseUrl("https://api.deepseek.com")  // ⚠️ DeepSeek 的 baseUrl
                 .modelName("deepseek-chat")
                 .temperature(0.5)
                 .logRequests(true)
                 .logResponses(true)
                 .build();

//        // 本地部署模型调用
//        aiModel = OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .baseUrl("http://localhost:11434/v1")  // ⚠️ DeepSeek 的 baseUrl
//                .modelName("deepseek-r1:7b")
//                .temperature(0.5)
//                .logRequests(true)
//                .logResponses(true)
//                .build();

        logger.info("AI Model initialized: " + (aiModel != null ? "Success" : "Failure"));
        
        // 初始化AIAgent
        aiAgent = new AIAgent(aiModel);
        logger.info("AIAgent initialized: " + (aiAgent != null ? "Success" : "Failure"));
        
//        testAIMessage();
        logger.info("Application startup completed");
    }


//    private void testAIMessage() {
//        String testPrompt = "Hello, can you respond?";
//        try {
//            String response = aiModel.chat(testPrompt);
//            logger.info("Test message response: " + response);
//            Platform.runLater(() -> showInfo("AI Test Successful", "Response: " + response));
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Failed to send test message", e);
//            Platform.runLater(() -> showError("AI Test Failed", "Error: " + e.getMessage()));
//        }
//    }


    private String getApiKey() {
        // Retrieve from environment variable first
        String key = System.getenv("DEEPSEEK_API_KEY");
//        logger.info("API Key: " + key);
        if (key != null) {
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
                    lineWidthComboBox.getValue()
            );
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
                case NW: case SE: return Cursor.NW_RESIZE;
                case NE: case SW: return Cursor.NE_RESIZE;
                case N: case S: return Cursor.V_RESIZE;
                case E: case W: return Cursor.H_RESIZE;
                default: return Cursor.DEFAULT;
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
                FontWeight weight = fontStyleCombo.getValue().equals("Bold") ?
                        FontWeight.BOLD : FontWeight.NORMAL;
                textElement.setFontStyle(weight, italic);
                refreshCanvas();
            }
        });

        // Sync styles when selected element changes
        canvas.setOnMouseClicked(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                colorPicker.setValue(textElement.getColor());
                fontSizeCombo.setValue((int)textElement.getFontSize());
                String style = textElement.getFontWeight() == FontWeight.BOLD ? "Bold" :
                        textElement.isItalic() ? "Italic" : "Regular";
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
                speechGenBtn
        );
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
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

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
                        (canvas.getWidth() - scaledWidth) / 2,  // Consider scaled width
                        (canvas.getHeight() - scaledHeight) / 2, // Consider scaled height
                        image
                );

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
        menuBar.setUseSystemMenuBar(true);  // Use system menu bar on macOS

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
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz")
        );

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
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz")
        );

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
                new FileChooser.ExtensionFilter("Presentation File", "*.mdz")
        );

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
     * @param speech 演讲稿内容
     */
    private void showSpeechDialog(String speech) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("生成的演讲稿");
        dialog.setHeaderText("根据当前幻灯片内容生成的演讲稿");
        
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButtonType = new ButtonType("复制到剪贴板", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType, copyButtonType);
        
        TextArea speechArea = new TextArea(speech);
        speechArea.setPrefRowCount(15);
        speechArea.setPrefColumnCount(60);
        speechArea.setWrapText(true);
        speechArea.setEditable(false);
        
        dialog.getDialogPane().setContent(speechArea);
        
        // 复制按钮事件
        Button copyButton = (Button) dialog.getDialogPane().lookupButton(copyButtonType);
        copyButton.setOnAction(e -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(speech);
            clipboard.setContent(content);
            showInfo("复制成功", "演讲稿已复制到剪贴板");
        });
        
        dialog.showAndWait();
    }

    private void showAIChatDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI智能生成PPT");
        dialog.setHeaderText("请输入你的PPT需求，点击*生成建议*后可查看AI建议、PPT命令和演讲稿，编辑命令后点击*生成PPT*生成幻灯片");

        ButtonType generateBtnType = new ButtonType("生成建议", ButtonBar.ButtonData.LEFT);
        ButtonType confirmBtnType = new ButtonType("生成PPT", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(generateBtnType, confirmBtnType, cancelBtnType);

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
            new Label("PPT需求："), inputArea,
            new Label("AI建议与反馈（只读）："), adviceArea,
            new Label("PPT命令与大纲（可编辑）："), suggestionArea
        );
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
            adviceArea.setDisable(false);
            suggestionArea.setDisable(false);
            adviceArea.setText("AI正在思考并生成建议，请稍候...");
            suggestionArea.setText("");
            // 调用AI生成建议、命令
            new Thread(() -> {
                String aiPrompt = "你是PPT助手，请根据用户输入做如下两步：\n" +
                        "1. 先用自然语言给出你的建议、思考或直接回答用户问题（如'今天星期三'），如果用户需求与PPT无关请直接回复建议或答案。\n" +
                        "2. 如果用户需求与PPT制作有关，再用严格的PPT命令格式输出大纲，格式要求如下：\n" +
                        "---PPT命令---\n" +
                        "Page 1:\nTitle: ...\nSubtitle: ...\nBullet: ...\nDraw: ...\nPage 2: ...\n（每个命令单独一行，所有命令都在---PPT命令---下方，若无PPT需求则此部分可为空）\n" +
                        "请严格用'---PPT命令---'分隔建议和命令部分。\n用户输入：" + userPrompt;
                try {
                    String aiResult = aiModel.chat(aiPrompt);
                    Platform.runLater(() -> {
                        String[] parts = aiResult.split("---PPT命令---");
                        String advice = parts.length > 0 ? parts[0].trim() : "";
                        String pptCmd = parts.length > 1 ? parts[1].trim() : "";
                        adviceArea.setText(advice);
                        suggestionArea.setText(pptCmd);
                        adviceArea.setDisable(false);
                        suggestionArea.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
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
            // 关闭对话框后生成PPT
            Platform.runLater(() -> {
                parseAndCreateSlides(suggestion);
                showInfo("AI生成完成", "PPT已根据建议生成，可继续编辑完善。");
            });
        });

        // 初始时禁用"生成PPT"按钮
        confirmBtn.setDisable(true);
        suggestionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            confirmBtn.setDisable(newVal.trim().isEmpty() || adviceArea.getText().startsWith("AI正在思考"));
        });

        dialog.showAndWait();
    }

    private void parseAndCreateSlides(String aiResult) {
        slides.clear();
        currentSlideIndex = -1;

        String[] pages = aiResult.split("Page\\s*\\d+[:：]");
        for (String page : pages) {
            String content = page.trim();
            if (content.isEmpty()) continue;

            createNewSlide();

            double slideWidth = canvas.getWidth();
            double y = 60;
            double lineSpacing = 8;

            // 用正则提取所有Title, Subtitle, Bullet, Draw指令
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(Title:([^;\\n]+))|(Subtitle:([^;\\n]+))|(Bullet:([^;\\n]+))|(Draw:\\s*(Line|Rectangle|Circle|Arrow)\\([^)]*\\))"
            );
            java.util.regex.Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                if (matcher.group(1) != null) { // Title
                    String text = matcher.group(2).trim();
                    TextElement titleElem = new TextElement(slideWidth/2, y, text, 28, Color.DARKBLUE, FontWeight.BOLD, false);
                    titleElem.setPosition(slideWidth/2 - titleElem.getWidth()/2, y + titleElem.getHeight());
                    currentSlide.addElement(titleElem);
                    y += titleElem.getHeight() + lineSpacing + 4;
                } else if (matcher.group(3) != null) { // Subtitle
                    String text = matcher.group(4).trim();
                    TextElement subElem = new TextElement(slideWidth/2, y, text, 20, Color.DARKGRAY, FontWeight.NORMAL, false);
                    subElem.setPosition(slideWidth/2 - subElem.getWidth()/2, y + subElem.getHeight());
                    currentSlide.addElement(subElem);
                    y += subElem.getHeight() + lineSpacing;
                } else if (matcher.group(5) != null) { // Bullet
                    String text = matcher.group(6).trim();
                    TextElement bulletElem = new TextElement(slideWidth/2, y, text, 18, Color.BLACK, FontWeight.NORMAL, false);
                    bulletElem.setPosition(slideWidth/2 - bulletElem.getWidth()/2, y + bulletElem.getHeight());
                    currentSlide.addElement(bulletElem);
                    y += bulletElem.getHeight() + lineSpacing;
                } else if (matcher.group(7) != null) { // Draw
                    // 只做绘图，不生成文本框
                    String drawCmd = matcher.group(7);
                    java.util.regex.Pattern drawPattern = java.util.regex.Pattern.compile("Draw:\\s*(Line|Rectangle|Circle|Arrow)\\(([^)]*)\\)");
                    java.util.regex.Matcher drawMatcher = drawPattern.matcher(drawCmd);
                    if (drawMatcher.find()) {
                        String shapeType = drawMatcher.group(1);
                        String params = drawMatcher.group(2);
                        try {
                            switch (shapeType) {
                                case "Line": {
                                    String[] xy = params.split(",");
                                    if (xy.length == 4) {
                                        double x1 = Double.parseDouble(xy[0].trim());
                                        double y1 = Double.parseDouble(xy[1].trim());
                                        double x2 = Double.parseDouble(xy[2].trim());
                                        double y2 = Double.parseDouble(xy[3].trim());
                                        DrawElement line = new DrawElement(x1, y1, DrawElement.ShapeType.LINE, Color.BLACK, 2.0);
                                        line.updateEndPoint(x2, y2);
                                        currentSlide.addElement(line);
                                    }
                                    break;
                                }
                                case "Rectangle": {
                                    String[] xy = params.split(",");
                                    if (xy.length == 4) {
                                        double x1 = Double.parseDouble(xy[0].trim());
                                        double y1 = Double.parseDouble(xy[1].trim());
                                        double x2 = Double.parseDouble(xy[2].trim());
                                        double y2 = Double.parseDouble(xy[3].trim());
                                        DrawElement rect = new DrawElement(x1, y1, DrawElement.ShapeType.RECTANGLE, Color.ORANGE, 2.0);
                                        rect.updateEndPoint(x2, y2);
                                        currentSlide.addElement(rect);
                                    }
                                    break;
                                }
                                case "Circle": {
                                    String[] xy = params.split(",");
                                    if (xy.length == 3) {
                                        double centerX = Double.parseDouble(xy[0].trim());
                                        double centerY = Double.parseDouble(xy[1].trim());
                                        double radius = Double.parseDouble(xy[2].trim());
                                        DrawElement circle = new DrawElement(centerX - radius, centerY, DrawElement.ShapeType.CIRCLE, Color.GREEN, 2.0);
                                        circle.updateEndPoint(centerX + radius, centerY);
                                        currentSlide.addElement(circle);
                                    }
                                    break;
                                }
                                case "Arrow": {
                                    String[] xy = params.split(",");
                                    if (xy.length == 4) {
                                        double x1 = Double.parseDouble(xy[0].trim());
                                        double y1 = Double.parseDouble(xy[1].trim());
                                        double x2 = Double.parseDouble(xy[2].trim());
                                        double y2 = Double.parseDouble(xy[3].trim());
                                        DrawElement arrow = new DrawElement(x1, y1, DrawElement.ShapeType.ARROW, Color.RED, 2.0);
                                        arrow.updateEndPoint(x2, y2);
                                        currentSlide.addElement(arrow);
                                    }
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    }
                }
            }
            refreshCanvas();
        }
        updateSlideControls();
        if (!slides.isEmpty()) {
            currentSlideIndex = 0;
            currentSlide = slides.get(0);
            refreshCanvas();
            updateSlideControls();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}