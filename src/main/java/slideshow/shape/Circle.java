package slideshow.shape;
import javafx.scene.canvas.GraphicsContext;

public class Circle extends Shape {
    public Circle(double startX, double startY) {
        super(startX, startY);
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        
        gc.strokeOval(x, y, width, height);
    }
    
    @Override
    public boolean contains(double x, double y) {
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;
        double radiusX = Math.abs(endX - startX) / 2;
        double radiusY = Math.abs(endY - startY) / 2;
        
        // 椭圆方程判断点是否在圆内
        return Math.pow((x - centerX) / radiusX, 2) + 
               Math.pow((y - centerY) / radiusY, 2) <= 1;
    }
}