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
    private double x;
    private double y;
    private double width;
    private double height;
    private boolean selected;
    
    private void calculateTextBounds() {
        Text textNode = new Text(text);
        textNode.setFont(Font.font("Arial", fontWeight, 
                        italic ? FontPosture.ITALIC : FontPosture.REGULAR, 
                        fontSize));
        width = textNode.getLayoutBounds().getWidth();
        height = textNode.getLayoutBounds().getHeight();
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
        }
        
        // 绘制文本
        gc.fillText(text, x, y);
        
        gc.restore();
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
} 