package slideshow.elements;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class TextElement extends SlideElement {
    private String text;
    
    public TextElement(double x, double y, String text) {
        super(x, y);
        this.text = text;
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillText(text, x, y);
        
        if (selected) {
            gc.setStroke(Color.BLUE);
            gc.strokeRect(x - 2, y - 12, gc.getFont().getSize() * text.length() * 0.6, 16);
        }
    }
    
    @Override
    public boolean containsPoint(double px, double py) {
        double width = text.length() * 10;  // 简单估算文本宽度
        double height = 16;  // 简单估算文本高度
        return px >= x && px <= x + width && py >= y - height && py <= y;
    }
} 