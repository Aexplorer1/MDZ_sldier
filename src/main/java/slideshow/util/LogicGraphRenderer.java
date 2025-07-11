package slideshow.util;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.geometry.Point2D;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 逻辑关系图渲染器
 * 用于可视化展示幻灯片结构分析生成的逻辑关系图
 */
public class LogicGraphRenderer extends Application {
    
    private Pane graphPane;
    private Map<String, GraphNode> nodes = new HashMap<>();
    private List<GraphEdge> edges = new ArrayList<>();
    private double dragStartX, dragStartY;
    private GraphNode selectedNode = null;
    
    /**
     * 图节点类
     */
    public static class GraphNode {
        private String id;
        private String label;
        private String type;
        private Circle circle;
        private Text text;
        private double x, y;
        
        public GraphNode(String id, String label, String type, double x, double y) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.x = x;
            this.y = y;
            
            // 根据类型设置不同的颜色和大小
            double radius = getRadiusByType(type);
            Color color = getColorByType(type);
            
            this.circle = new Circle(x, y, radius, color);
            this.circle.setStroke(Color.BLACK);
            this.circle.setStrokeWidth(2);
            
            this.text = new Text(x, y, label);
            this.text.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            this.text.setFill(Color.WHITE);
            
            // 文本居中
            this.text.setX(x - this.text.getLayoutBounds().getWidth() / 2);
            this.text.setY(y + this.text.getLayoutBounds().getHeight() / 4);
        }
        
        private double getRadiusByType(String type) {
            switch (type) {
                case "main": return 40;
                case "outline": return 30;
                case "keypoint": return 25;
                default: return 20;
            }
        }
        
        private Color getColorByType(String type) {
            switch (type) {
                case "main": return Color.DARKBLUE;
                case "outline": return Color.ORANGE;
                case "keypoint": return Color.GREEN;
                default: return Color.GRAY;
            }
        }
        
        // Getters
        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getType() { return type; }
        public Circle getCircle() { return circle; }
        public Text getText() { return text; }
        public double getX() { return x; }
        public double getY() { return y; }
        
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
            this.circle.setCenterX(x);
            this.circle.setCenterY(y);
            this.text.setX(x - this.text.getLayoutBounds().getWidth() / 2);
            this.text.setY(y + this.text.getLayoutBounds().getHeight() / 4);
        }
        
        public boolean contains(double x, double y) {
            double distance = Math.sqrt((x - this.x) * (x - this.x) + (y - this.y) * (y - this.y));
            return distance <= this.circle.getRadius();
        }
    }
    
    /**
     * 图边类
     */
    public static class GraphEdge {
        private GraphNode source;
        private GraphNode target;
        private String type;
        private Line line;
        private Text label;
        
        public GraphEdge(GraphNode source, GraphNode target, String type) {
            this.source = source;
            this.target = target;
            this.type = type;
            
            this.line = new Line();
            this.line.setStroke(getColorByType(type));
            this.line.setStrokeWidth(2);
            
            this.label = new Text();
            this.label.setFont(Font.font("Arial", 10));
            this.label.setFill(getColorByType(type));
            
            updatePosition();
        }
        
        private Color getColorByType(String type) {
            switch (type) {
                case "hierarchy": return Color.BLUE;
                case "detail": return Color.RED;
                case "association": return Color.GREEN;
                default: return Color.BLACK;
            }
        }
        
        public void updatePosition() {
            // 计算边的位置
            double startX = source.getX();
            double startY = source.getY();
            double endX = target.getX();
            double endY = target.getY();
            
            // 计算方向向量
            double dx = endX - startX;
            double dy = endY - startY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                // 归一化方向向量
                dx /= distance;
                dy /= distance;
                
                // 调整起点和终点，避免与节点重叠
                double sourceRadius = source.getCircle().getRadius();
                double targetRadius = target.getCircle().getRadius();
                
                double adjustedStartX = startX + dx * sourceRadius;
                double adjustedStartY = startY + dy * sourceRadius;
                double adjustedEndX = endX - dx * targetRadius;
                double adjustedEndY = endY - dy * targetRadius;
                
                this.line.setStartX(adjustedStartX);
                this.line.setStartY(adjustedStartY);
                this.line.setEndX(adjustedEndX);
                this.line.setEndY(adjustedEndY);
                
                // 设置标签位置
                this.label.setX((adjustedStartX + adjustedEndX) / 2);
                this.label.setY((adjustedStartY + adjustedEndY) / 2 - 5);
                this.label.setText(type);
            }
        }
        
        // Getters
        public GraphNode getSource() { return source; }
        public GraphNode getTarget() { return target; }
        public String getType() { return type; }
        public Line getLine() { return line; }
        public Text getLabel() { return label; }
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("逻辑关系图可视化");
        
        // 创建图形面板
        graphPane = new Pane();
        graphPane.setStyle("-fx-background-color: white;");
        
        // 设置拖拽事件
        graphPane.setOnMousePressed(this::handleMousePressed);
        graphPane.setOnMouseDragged(this::handleMouseDragged);
        graphPane.setOnMouseReleased(this::handleMouseReleased);
        
        // 创建场景
        Scene scene = new Scene(graphPane, 1200, 800);
        primaryStage.setScene(scene);
        
        // 显示窗口
        primaryStage.show();
    }
    
    /**
     * 解析JSON数据并渲染图形
     */
    public void renderGraph(String jsonData) {
        try {
            // 解析JSON数据
            Map<String, Object> graphData = parseJsonData(jsonData);
            
            // 清空现有图形
            graphPane.getChildren().clear();
            nodes.clear();
            edges.clear();
            
            // 创建节点
            createNodes(graphData);
            
            // 创建边
            createEdges(graphData);
            
            // 自动布局
            autoLayout();
            
            // 渲染到面板
            renderToPane();
            
        } catch (Exception e) {
            System.err.println("渲染图形失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 解析JSON数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonData(String jsonData) {
        Map<String, Object> result = new HashMap<>();
        
        // 解析节点
        Pattern nodePattern = Pattern.compile("\"id\":\\s*\"([^\"]+)\",\\s*\"label\":\\s*\"([^\"]+)\",\\s*\"type\":\\s*\"([^\"]+)\"");
        Matcher nodeMatcher = nodePattern.matcher(jsonData);
        List<Map<String, String>> nodes = new ArrayList<>();
        while (nodeMatcher.find()) {
            Map<String, String> node = new HashMap<>();
            node.put("id", nodeMatcher.group(1));
            node.put("label", nodeMatcher.group(2));
            node.put("type", nodeMatcher.group(3));
            nodes.add(node);
        }
        result.put("nodes", nodes);
        
        // 解析边
        Pattern edgePattern = Pattern.compile("\"source\":\\s*\"([^\"]+)\",\\s*\"target\":\\s*\"([^\"]+)\",\\s*\"type\":\\s*\"([^\"]+)\"");
        Matcher edgeMatcher = edgePattern.matcher(jsonData);
        List<Map<String, String>> edges = new ArrayList<>();
        while (edgeMatcher.find()) {
            Map<String, String> edge = new HashMap<>();
            edge.put("source", edgeMatcher.group(1));
            edge.put("target", edgeMatcher.group(2));
            edge.put("type", edgeMatcher.group(3));
            edges.add(edge);
        }
        result.put("edges", edges);
        
        return result;
    }
    
    /**
     * 创建节点
     */
    @SuppressWarnings("unchecked")
    private void createNodes(Map<String, Object> graphData) {
        List<Map<String, String>> nodesData = (List<Map<String, String>>) graphData.get("nodes");
        
        for (Map<String, String> nodeData : nodesData) {
            String id = nodeData.get("id");
            String label = nodeData.get("label");
            String type = nodeData.get("type");
            
            // 临时位置，稍后会通过自动布局调整
            GraphNode node = new GraphNode(id, label, type, 0, 0);
            nodes.put(id, node);
        }
    }
    
    /**
     * 创建边
     */
    @SuppressWarnings("unchecked")
    private void createEdges(Map<String, Object> graphData) {
        List<Map<String, String>> edgesData = (List<Map<String, String>>) graphData.get("edges");
        
        for (Map<String, String> edgeData : edgesData) {
            String sourceId = edgeData.get("source");
            String targetId = edgeData.get("target");
            String type = edgeData.get("type");
            
            GraphNode source = nodes.get(sourceId);
            GraphNode target = nodes.get(targetId);
            
            if (source != null && target != null) {
                GraphEdge edge = new GraphEdge(source, target, type);
                edges.add(edge);
            }
        }
    }
    
    /**
     * 自动布局算法
     */
    private void autoLayout() {
        if (nodes.isEmpty()) return;
        
        // 分层布局
        Map<String, List<GraphNode>> layers = new HashMap<>();
        
        // 找到主节点
        GraphNode mainNode = null;
        for (GraphNode node : nodes.values()) {
            if ("main".equals(node.getType())) {
                mainNode = node;
                break;
            }
        }
        
        if (mainNode == null) {
            // 如果没有主节点，选择第一个节点
            mainNode = nodes.values().iterator().next();
        }
        
        // 设置主节点位置
        mainNode.setPosition(600, 100);
        
        // 分层布局
        layoutNodes(mainNode, 0, 600, 100);
    }
    
    /**
     * 递归布局节点
     */
    private void layoutNodes(GraphNode parent, int level, double centerX, double centerY) {
        List<GraphNode> children = new ArrayList<>();
        
        // 找到所有连接到当前节点的子节点
        for (GraphEdge edge : edges) {
            if (edge.getSource().getId().equals(parent.getId())) {
                children.add(edge.getTarget());
            }
        }
        
        if (children.isEmpty()) return;
        
        // 计算子节点位置
        double spacing = 150;
        double totalWidth = (children.size() - 1) * spacing;
        double startX = centerX - totalWidth / 2;
        double childY = centerY + 120;
        
        for (int i = 0; i < children.size(); i++) {
            GraphNode child = children.get(i);
            double childX = startX + i * spacing;
            child.setPosition(childX, childY);
            
            // 递归布局子节点
            layoutNodes(child, level + 1, childX, childY);
        }
    }
    
    /**
     * 渲染到面板
     */
    private void renderToPane() {
        // 添加边
        for (GraphEdge edge : edges) {
            graphPane.getChildren().add(edge.getLine());
            graphPane.getChildren().add(edge.getLabel());
        }
        
        // 添加节点
        for (GraphNode node : nodes.values()) {
            graphPane.getChildren().add(node.getCircle());
            graphPane.getChildren().add(node.getText());
            
            // 添加拖拽事件
            node.getCircle().setOnMousePressed(e -> handleNodeMousePressed(e, node));
            node.getText().setOnMousePressed(e -> handleNodeMousePressed(e, node));
        }
    }
    
    /**
     * 处理节点鼠标按下事件
     */
    private void handleNodeMousePressed(MouseEvent event, GraphNode node) {
        selectedNode = node;
        dragStartX = event.getSceneX() - node.getX();
        dragStartY = event.getSceneY() - node.getY();
        event.consume();
    }
    
    /**
     * 处理鼠标按下事件
     */
    private void handleMousePressed(MouseEvent event) {
        dragStartX = event.getSceneX();
        dragStartY = event.getSceneY();
    }
    
    /**
     * 处理鼠标拖拽事件
     */
    private void handleMouseDragged(MouseEvent event) {
        if (selectedNode != null) {
            // 拖拽节点
            double newX = event.getSceneX() - dragStartX;
            double newY = event.getSceneY() - dragStartY;
            selectedNode.setPosition(newX, newY);
            
            // 更新连接的边
            updateConnectedEdges(selectedNode);
        } else {
            // 拖拽整个图形
            double deltaX = event.getSceneX() - dragStartX;
            double deltaY = event.getSceneY() - dragStartY;
            
            for (GraphNode node : nodes.values()) {
                node.setPosition(node.getX() + deltaX, node.getY() + deltaY);
            }
            
            // 更新所有边
            for (GraphEdge edge : edges) {
                edge.updatePosition();
            }
            
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        }
    }
    
    /**
     * 处理鼠标释放事件
     */
    private void handleMouseReleased(MouseEvent event) {
        selectedNode = null;
    }
    
    /**
     * 更新连接的边
     */
    private void updateConnectedEdges(GraphNode node) {
        for (GraphEdge edge : edges) {
            if (edge.getSource().getId().equals(node.getId()) || 
                edge.getTarget().getId().equals(node.getId())) {
                edge.updatePosition();
            }
        }
    }
    
    /**
     * 显示逻辑关系图
     */
    public static void showLogicGraph(String jsonData) {
        Platform.runLater(() -> {
            LogicGraphRenderer renderer = new LogicGraphRenderer();
            Stage stage = new Stage();
            renderer.start(stage);
            renderer.renderGraph(jsonData);
        });
    }
    
    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        // 测试数据
        String testJson = "{\n" +
            "  \"nodes\": [\n" +
            "    {\"id\": \"main\", \"label\": \"人工智能\", \"type\": \"main\"},\n" +
            "    {\"id\": \"outline_0\", \"label\": \"机器学习\", \"type\": \"outline\"},\n" +
            "    {\"id\": \"outline_1\", \"label\": \"深度学习\", \"type\": \"outline\"},\n" +
            "    {\"id\": \"point_0\", \"label\": \"监督学习\", \"type\": \"keypoint\"},\n" +
            "    {\"id\": \"point_1\", \"label\": \"无监督学习\", \"type\": \"keypoint\"}\n" +
            "  ],\n" +
            "  \"edges\": [\n" +
            "    {\"source\": \"main\", \"target\": \"outline_0\", \"type\": \"hierarchy\"},\n" +
            "    {\"source\": \"main\", \"target\": \"outline_1\", \"type\": \"hierarchy\"},\n" +
            "    {\"source\": \"outline_0\", \"target\": \"point_0\", \"type\": \"detail\"},\n" +
            "    {\"source\": \"outline_0\", \"target\": \"point_1\", \"type\": \"detail\"}\n" +
            "  ]\n" +
            "}";
        
        launch(args);
    }
}