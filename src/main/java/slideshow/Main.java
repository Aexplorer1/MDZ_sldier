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
                .baseUrl("https://api.deepseek.com")  // ⚠️ DeepSeek 的 baseUrl
                .modelName("deepseek-chat")
                .temperature(0.5)
                .logRequests(true)
                .logResponses(true)
                .build();


        logger.info("AI Model initialized: " + (aiModel != null ? "Success" : "Failure"));
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

        Button aiGenBtn = new Button("AI Generate PPT");
        aiGenBtn.getStyleClass().add("button");
        aiGenBtn.setOnAction(e -> showAIGenerateDialog());

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
                aiGenBtn
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
        if (file != null) {
            try {
                SlideSerializer.savePresentation(slides, file.getPath());
                showInfo("Save Successful", "Presentation saved to: " + file.getPath());
            } catch (IOException e) {
                showError("Save Failed", "Unable to save file: " + e.getMessage());
            }
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

    private void showAIGenerateDialog() {
        TextInputDialog dialog = new TextInputDialog("Please describe the PPT content you want, e.g., Generate a PPT introduction about artificial intelligence");
        dialog.setTitle("AI Generate PPT");
        dialog.setHeaderText("Enter your PPT requirements:");
        dialog.setContentText("Requirements:");
        dialog.showAndWait().ifPresent(prompt -> {
            generatePPTWithAI(prompt);
        });
    }

    private void generatePPTWithAI(String prompt) {
        // 优化AI提示词，支持多种图形
        String aiPrompt = "You are a PPT assistant, please generate a PPT outline based on user requirements. Requirements:\n" +
                "1. One sentence per page, suitable as a slide title or key point.\n" +
                "2. Return format must be strict: Page 1: xxx; Page 2: xxx; ..., no other content.\n" +
                "3. Do not output explanations.\n" +
                "4. If you need to draw a line, use: Draw: Line(x1,y1,x2,y2). If you need to draw a rectangle, use: Draw: Rectangle(x1,y1,x2,y2). If you need to draw a circle, use: Draw: Circle(centerX,centerY,radius). If you need to draw an arrow, use: Draw: Arrow(x1,y1,x2,y2). You can use multiple Draw instructions in one page.\n" +
                "5. If you need to add a title, use: Title: xxx. If you need to add a subtitle, use: Subtitle: xxx. If you need to add a bullet, use: Bullet: xxx. You can use multiple Title, Subtitle, and Bullet instructions in one page.\n" +
                "6. Each Title, Subtitle, Bullet, and Draw must be on its own line, do not merge multiple items into one line.\n" +
                "7. All content for the same page (Title, Subtitle, Bullet, Draw, etc.) must be under the same Page n:, do not create a new Page n: for each line.\n" +
                "User requirements: " + prompt;
        new Thread(() -> {
            try {
                String aiResult = aiModel.chat(aiPrompt);
                logger.info("aiResult: " + aiResult);
                Platform.runLater(() -> {
                    parseAndCreateSlides(aiResult);
                    showInfo("AI Generation Complete", "PPT outline generated based on AI suggestions.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("AI Generation Failed", e.getMessage()));
            }
        }).start();
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