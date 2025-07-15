package slideshow.elements;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class TextElement extends SlideElement {
    private String text;
    private double fontSize;
    private Color color;
    private FontWeight fontWeight;
    private boolean italic;
    //private double x;
    //private double y;
    private double width;
    private double height;
    //private boolean selected;
    
    private static final double HANDLE_SIZE = 8; // 控制点大小
    private static final double HANDLE_OFFSET = HANDLE_SIZE / 2;
    
    private void calculateTextBounds() {
        // 分割文本为多行
        String[] lines = text.split("\n");
        double maxWidth = 0;
        double totalHeight = 0;
        
        Text textNode = new Text();
        textNode.setFont(Font.font("Arial", fontWeight, 
                        italic ? FontPosture.ITALIC : FontPosture.REGULAR, 
                        fontSize));
        
        for (String line : lines) {
            textNode.setText(line);
            double lineWidth = textNode.getLayoutBounds().getWidth();
            double lineHeight = textNode.getLayoutBounds().getHeight();
            
            maxWidth = Math.max(maxWidth, lineWidth);
            totalHeight += lineHeight;
        }
        
        width = maxWidth;
        height = totalHeight;
    }
    
    public TextElement(double x, double y, String text, 
                      double fontSize, Color color, 
                      FontWeight fontWeight, boolean italic) {
        super(x, y);
        this.text = text;
        this.fontSize = fontSize;
        this.color = color;
        this.fontWeight = fontWeight;
        this.italic = italic;
        this.x = x;
        this.y = y;
        calculateTextBounds();
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        
        // 设置文本样式
        gc.setFill(color);
        gc.setFont(Font.font("Arial", fontWeight, 
                italic ? FontPosture.ITALIC : FontPosture.REGULAR, 
                fontSize));
        
        // 如果被选中，先绘制边框
        if (selected) {
            gc.setStroke(Color.BLUE);
            gc.setLineDashes(5);
            gc.strokeRect(x - 2, y - height, width + 4, height + 4);
            gc.setLineDashes(null);
            
            // 绘制控制点
            drawResizeHandles(gc);
        }
        
        // 绘制多行文本
        String[] lines = text.split("\n");
        double currentY = y;
        
        for (String line : lines) {
            gc.fillText(line, x, currentY);
            // 计算下一行的Y位置
            Text textNode = new Text(line);
            textNode.setFont(Font.font("Arial", fontWeight, 
                    italic ? FontPosture.ITALIC : FontPosture.REGULAR, 
                    fontSize));
            currentY += textNode.getLayoutBounds().getHeight();
        }
        
        gc.restore();
    }
    
    private void drawResizeHandles(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        
        // 绘制四角的控制点
        drawHandle(gc, x - HANDLE_OFFSET, y - height - HANDLE_OFFSET);           // NW
        drawHandle(gc, x + width - HANDLE_OFFSET, y - height - HANDLE_OFFSET);   // NE
        drawHandle(gc, x - HANDLE_OFFSET, y + HANDLE_OFFSET);                    // SW
        drawHandle(gc, x + width - HANDLE_OFFSET, y + HANDLE_OFFSET);            // SE
        
        // 绘制边缘中点的控制点
        drawHandle(gc, x + width/2 - HANDLE_OFFSET, y - height - HANDLE_OFFSET); // N
        drawHandle(gc, x + width/2 - HANDLE_OFFSET, y + HANDLE_OFFSET);          // S
        drawHandle(gc, x - HANDLE_OFFSET, y - height/2);                         // W
        drawHandle(gc, x + width - HANDLE_OFFSET, y - height/2);                 // E
    }
    
    private void drawHandle(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
        gc.strokeRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
    }
    
    @Override
    public ResizeHandle getResizeHandle(double px, double py) {
        if (!selected) return ResizeHandle.NONE;
        
        // 检查各个控制点
        if (isInHandle(px, py, x, y - height)) return ResizeHandle.NW;
        if (isInHandle(px, py, x + width, y - height)) return ResizeHandle.NE;
        if (isInHandle(px, py, x, y)) return ResizeHandle.SW;
        if (isInHandle(px, py, x + width, y)) return ResizeHandle.SE;
        if (isInHandle(px, py, x + width/2, y - height)) return ResizeHandle.N;
        if (isInHandle(px, py, x + width/2, y)) return ResizeHandle.S;
        if (isInHandle(px, py, x, y - height/2)) return ResizeHandle.W;
        if (isInHandle(px, py, x + width, y - height/2)) return ResizeHandle.E;
        
        return ResizeHandle.NONE;
    }
    
    private boolean isInHandle(double px, double py, double hx, double hy) {
        return px >= hx - HANDLE_OFFSET && px <= hx + HANDLE_OFFSET &&
               py >= hy - HANDLE_OFFSET && py <= hy + HANDLE_OFFSET;
    }
    
    public void resize(double deltaX, double deltaY, ResizeHandle handle) {
        double minSize = 12; // 最小字体大小
        double maxSize = 144; // 最大字体大小
        
        // 计算新的字体大小
        double scaleFactor = 1.0;
        switch (handle) {
            case SE: case NE: case SW: case NW:
                // 对角线调整，使用较大的变化量
                double delta = Math.max(Math.abs(deltaX), Math.abs(deltaY));
                if (delta > 0) {
                    scaleFactor = deltaX > 0 || deltaY > 0 ? 1.05 : 0.95;
                }
                break;
            case N: case S:
                // 垂直调整
                if (Math.abs(deltaY) > 0) {
                    scaleFactor = deltaY > 0 ? 0.95 : 1.05;
                }
                break;
            case E: case W:
                // 水平调整
                if (Math.abs(deltaX) > 0) {
                    scaleFactor = deltaX > 0 ? 1.05 : 0.95;
                }
                break;
        }
        
        // 应用新的字体大小
        double newFontSize = Math.min(maxSize, Math.max(minSize, fontSize * scaleFactor));
        if (Math.abs(newFontSize - fontSize) >= 0.5) {  // 添加一个阈值
            fontSize = newFontSize;
            calculateTextBounds(); // 重新计算文本边界
        }
    }
    
    public void setResizeHandle(ResizeHandle handle) {
        this.currentHandle = handle;
    }
    
    public ResizeHandle getCurrentHandle() {
        return currentHandle;
    }
    
    @Override
    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width &&
               py >= y - height && py <= y;
    }
    
    public void setText(String text) {
        this.text = text;
        calculateTextBounds();
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
        calculateTextBounds();
    }
    
    public void setFontStyle(FontWeight weight, boolean italic) {
        this.fontWeight = weight;
        this.italic = italic;
        calculateTextBounds();
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    @Override
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        calculateTextBounds();
    }
    
    @Override
    public void move(double deltaX, double deltaY) {
        setPosition(x + deltaX, y + deltaY);
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public void setWidth(double width) {
        this.width = width;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public String getText() {
        return text;
    }
    
    public Color getColor() {
        return color;
    }
    
    public double getFontSize() {
        return fontSize;
    }
    
    public FontWeight getFontWeight() {
        return fontWeight;
    }
    
    public boolean isItalic() {
        return italic;
    }

    @Override
    public SlideElement deepClone() {
        TextElement clone = new TextElement(
            this.x,
            this.y,
            this.text,
            this.fontSize,
            this.color,
            this.fontWeight,
            this.italic
        );
        clone.setSelected(this.selected);
        return clone;
    }
} 