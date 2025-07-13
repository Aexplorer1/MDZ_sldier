package slideshow.presentation;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import slideshow.model.Slide;
import slideshow.util.SpeechManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 演讲者视图窗口
 * 提供演讲者专用的界面，包含时间显示、当前幻灯片预览和演讲稿内容
 */
public class SpeakerViewWindow {
    private Stage stage;
    private Canvas previewCanvas;
    private TextArea speechArea;
    private Label totalTimeLabel;
    private Label slideTimeLabel;
    private Label slideInfoLabel;
    private List<Slide> slides;
    private int currentIndex = 0;
    
    // 时间相关
    private LocalDateTime startTime;
    private LocalDateTime slideStartTime;
    private Timeline timeUpdateTimer;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // 演讲稿相关
    private String speechContent;
    private String[] speechLines;
    private int currentSpeechLine = 0;
    
    public SpeakerViewWindow(List<Slide> slides) {
        this.slides = slides;
        this.startTime = LocalDateTime.now();
        this.slideStartTime = LocalDateTime.now();
        
        // 加载演讲稿
        loadSpeechContent();
        
        stage = new Stage();
        initializeUI();
        setupEventHandlers();
    }
    
    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        BorderPane root = new BorderPane();
        
        // 顶部：时间信息
        VBox topBox = createTopPanel();
        
        // 左侧：幻灯片预览
        VBox leftBox = createLeftPanel();
        
        // 右侧：演讲稿内容
        VBox rightBox = createRightPanel();
        
        // 设置布局
        root.setTop(topBox);
        root.setLeft(leftBox);
        root.setCenter(rightBox);
        
        // 创建场景
        Scene scene = new Scene(root, 1400, 900);
        stage.setScene(scene);
        stage.setTitle("演讲者视图");
        stage.setX(100);
        stage.setY(100);
        
        // 设置窗口属性
        stage.setResizable(true);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
    }
    
    /**
     * 创建顶部面板（时间信息）
     */
    private VBox createTopPanel() {
        VBox topBox = new VBox(5);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        
        // 总时间标签
        totalTimeLabel = new Label("总时间: 00:00:00");
        totalTimeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // 当前幻灯片时间标签
        slideTimeLabel = new Label("本页时间: 00:00:00");
        slideTimeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        // 幻灯片信息标签
        slideInfoLabel = new Label("第 1 张 / 共 " + slides.size() + " 张");
        slideInfoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        topBox.getChildren().addAll(totalTimeLabel, slideTimeLabel, slideInfoLabel);
        topBox.setAlignment(Pos.CENTER);
        
        return topBox;
    }
    
    /**
     * 创建左侧面板（幻灯片预览）
     */
    private VBox createLeftPanel() {
        VBox leftBox = new VBox(10);
        leftBox.setPadding(new Insets(10));
        leftBox.setPrefWidth(400);
        leftBox.setStyle("-fx-background-color: #fafafa; -fx-border-color: #ccc; -fx-border-width: 0 1 0 0;");
        
        // 预览标题
        Label previewTitle = new Label("幻灯片预览");
        previewTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // 预览画布
        previewCanvas = new Canvas(380, 250);
        previewCanvas.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-background-color: white;");
        
        leftBox.getChildren().addAll(previewTitle, previewCanvas);
        
        return leftBox;
    }
    
    /**
     * 创建右侧面板（演讲稿内容）
     */
    private VBox createRightPanel() {
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(10));
        
        // 演讲稿标题
        Label speechTitle = new Label("演讲稿内容");
        speechTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // 演讲稿文本区域
        speechArea = new TextArea();
        speechArea.setEditable(false);
        speechArea.setWrapText(true);
        speechArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Microsoft YaHei', 'SimSun';");
        
        // 滚动面板
        ScrollPane scrollPane = new ScrollPane(speechArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        rightBox.getChildren().addAll(speechTitle, scrollPane);
        
        return rightBox;
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        Scene scene = stage.getScene();
        
        // 键盘事件处理
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT:
                case SPACE:
                    nextSlide();
                    break;
                case LEFT:
                    previousSlide();
                    break;
                case ESCAPE:
                    stage.close();
                    break;
            }
        });
        
        // 鼠标点击事件处理
        scene.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                nextSlide();
            } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                previousSlide();
            }
        });
    }
    
    /**
     * 加载演讲稿内容
     */
    private void loadSpeechContent() {
        String latestSpeechFile = SpeechManager.getLatestSpeechFile();
        if (latestSpeechFile != null) {
            speechContent = SpeechManager.loadSpeechFromFile(latestSpeechFile);
            if (speechContent != null) {
                // 提取纯演讲稿内容（去除头部信息）
                String[] lines = speechContent.split("\n");
                StringBuilder contentBuilder = new StringBuilder();
                boolean contentStarted = false;
                
                for (String line : lines) {
                    if (line.startsWith("=")) {
                        contentStarted = true;
                        continue;
                    }
                    if (contentStarted) {
                        contentBuilder.append(line).append("\n");
                    }
                }
                
                speechContent = contentBuilder.toString().trim();
                speechLines = speechContent.split("\n");
            }
        }
        
        if (speechContent == null || speechContent.isEmpty()) {
            speechContent = "未找到演讲稿文件，请先生成演讲稿。";
            speechLines = new String[]{speechContent};
        }
    }
    
    /**
     * 启动时间更新定时器
     */
    private void startTimeUpdateTimer() {
        timeUpdateTimer = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> updateTimeDisplay())
        );
        timeUpdateTimer.setCycleCount(Timeline.INDEFINITE);
        timeUpdateTimer.play();
    }
    
    /**
     * 更新时间显示
     */
    private void updateTimeDisplay() {
        LocalDateTime now = LocalDateTime.now();
        
        // 更新总时间
        long totalSeconds = java.time.Duration.between(startTime, now).getSeconds();
        String totalTime = String.format("%02d:%02d:%02d", 
            totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
        totalTimeLabel.setText("总时间: " + totalTime);
        
        // 更新当前幻灯片时间
        long slideSeconds = java.time.Duration.between(slideStartTime, now).getSeconds();
        String slideTime = String.format("%02d:%02d:%02d", 
            slideSeconds / 3600, (slideSeconds % 3600) / 60, slideSeconds % 60);
        slideTimeLabel.setText("本页时间: " + slideTime);
    }
    
    /**
     * 显示当前幻灯片
     */
    private void showCurrentSlide() {
        if (currentIndex >= 0 && currentIndex < slides.size()) {
            // 更新幻灯片预览
            previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
            previewCanvas.getGraphicsContext2D().setFill(javafx.scene.paint.Color.WHITE);
            previewCanvas.getGraphicsContext2D().fillRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
            
            // 缩放绘制幻灯片
            double scaleX = previewCanvas.getWidth() / 1600.0;
            double scaleY = previewCanvas.getHeight() / 1000.0;
            double scale = Math.min(scaleX, scaleY);
            
            previewCanvas.getGraphicsContext2D().save();
            previewCanvas.getGraphicsContext2D().scale(scale, scale);
            slides.get(currentIndex).draw(previewCanvas.getGraphicsContext2D());
            previewCanvas.getGraphicsContext2D().restore();
            
            // 更新幻灯片信息
            slideInfoLabel.setText(String.format("第 %d 张 / 共 %d 张", currentIndex + 1, slides.size()));
            
            // 更新演讲稿内容
            updateSpeechContent();
            
            // 重置幻灯片开始时间
            slideStartTime = LocalDateTime.now();
        }
    }
    
    /**
     * 更新演讲稿内容显示
     */
    private void updateSpeechContent() {
        if (speechLines != null && speechLines.length > 0) {
            // 根据当前幻灯片索引计算显示的演讲稿内容
            int startLine = Math.min(currentIndex * 3, speechLines.length - 1);
            int endLine = Math.min(startLine + 20, speechLines.length);
            
            StringBuilder displayContent = new StringBuilder();
            for (int i = startLine; i < endLine; i++) {
                displayContent.append(speechLines[i]).append("\n");
            }
            
            speechArea.setText(displayContent.toString());
            
            // 滚动到顶部
            speechArea.setScrollTop(0);
        }
    }
    
    /**
     * 切换到下一张幻灯片
     */
    private void nextSlide() {
        if (currentIndex < slides.size() - 1) {
            currentIndex++;
            showCurrentSlide();
        }
    }
    
    /**
     * 切换到上一张幻灯片
     */
    private void previousSlide() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentSlide();
        }
    }
    
    /**
     * 启动演讲者视图
     */
    public void start() {
        stage.show();
        showCurrentSlide();
        startTimeUpdateTimer();
        
        System.out.println("演讲者视图已启动");
        System.out.println("- 总时间: 实时更新");
        System.out.println("- 本页时间: 实时更新");
        System.out.println("- 幻灯片预览: 左侧显示");
        System.out.println("- 演讲稿内容: 右侧显示");
        System.out.println("- 控制方式: 键盘箭头键、空格键或鼠标点击");
    }
    
    /**
     * 关闭演讲者视图
     */
    public void close() {
        if (timeUpdateTimer != null) {
            timeUpdateTimer.stop();
        }
        stage.close();
    }
} 