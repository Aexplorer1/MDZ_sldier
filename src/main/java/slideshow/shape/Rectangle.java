package slideshow.shape;
import javafx.scene.canvas.GraphicsContext;

public class Rectangle extends Shape {
    public Rectangle(double startX, double startY) {
        super(startX, startY);
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        
        gc.strokeRect(x, y, width, height);
    }
    
    @Override
    public boolean contains(double x, double y) {
        double left = Math.min(startX, endX);
        double right = Math.max(startX, endX);
        double top = Math.min(startY, endY);
        double bottom = Math.max(startY, endY);
        
        return x >= left && x <= right && y >= top && y <= bottom;
    }
} 