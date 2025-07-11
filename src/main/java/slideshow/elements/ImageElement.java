package slideshow.elements;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ImageElement extends SlideElement {
    private transient Image image;      // 不参与序列化
    private String imageUrl;            // 参与序列化
    private double width;
    private double height;

    private static final double HANDLE_SIZE = 8;
    private static final double HANDLE_OFFSET = HANDLE_SIZE / 2;

    public ImageElement(double x, double y, Image image) {
        super(x, y);
        this.image = image;
        this.imageUrl = image.getUrl();

        // 获取原始尺寸
        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();

        // 设置最大显示尺寸
        double maxWidth = 800;
        double maxHeight = 600;

        // 计算缩放比例
        double scale = 1.0;
        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            double scaleX = maxWidth / originalWidth;
            double scaleY = maxHeight / originalHeight;
            scale = Math.min(scaleX, scaleY);
        }

        // 设置显示尺寸
        this.width = originalWidth * scale;
        this.height = originalHeight * scale;
    }

    // 用于反序列化构建
    public ImageElement(double x, double y, String imageUrl, double width, double height) {
        super(x, y);
        this.imageUrl = imageUrl;
        this.width = width;
        this.height = height;
        this.image = new Image(imageUrl);
    }

    public Image getImage() {
        if (image == null && imageUrl != null) {
            image = new Image(imageUrl);
        }
        return image;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.save();

        // 绘制图片
        gc.drawImage(getImage(), x, y, width, height);

        // 如果被选中，绘制边框和控制点
        if (selected) {
            gc.setStroke(Color.BLUE);
            gc.setLineDashes(5);
            gc.strokeRect(x - 2, y - 2, width + 4, height + 4);
            gc.setLineDashes(null);
            drawResizeHandles(gc);
        }

        gc.restore();
    }

    private void drawResizeHandles(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);

        // 四角控制点
        drawHandle(gc, x - HANDLE_OFFSET, y - HANDLE_OFFSET);                 // NW
        drawHandle(gc, x + width - HANDLE_OFFSET, y - HANDLE_OFFSET);         // NE
        drawHandle(gc, x - HANDLE_OFFSET, y + height - HANDLE_OFFSET);        // SW
        drawHandle(gc, x + width - HANDLE_OFFSET, y + height - HANDLE_OFFSET);// SE

        // 中点控制点
        drawHandle(gc, x + width / 2 - HANDLE_OFFSET, y - HANDLE_OFFSET);             // N
        drawHandle(gc, x + width / 2 - HANDLE_OFFSET, y + height - HANDLE_OFFSET);    // S
        drawHandle(gc, x - HANDLE_OFFSET, y + height / 2 - HANDLE_OFFSET);            // W
        drawHandle(gc, x + width - HANDLE_OFFSET, y + height / 2 - HANDLE_OFFSET);    // E
    }

    private void drawHandle(GraphicsContext gc, double x, double y) {
        gc.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
        gc.strokeRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
    }

    @Override
    public ResizeHandle getResizeHandle(double px, double py) {
        if (!selected) return ResizeHandle.NONE;

        if (isInHandle(px, py, x, y)) return ResizeHandle.NW;
        if (isInHandle(px, py, x + width, y)) return ResizeHandle.NE;
        if (isInHandle(px, py, x, y + height)) return ResizeHandle.SW;
        if (isInHandle(px, py, x + width, y + height)) return ResizeHandle.SE;
        if (isInHandle(px, py, x + width / 2, y)) return ResizeHandle.N;
        if (isInHandle(px, py, x + width / 2, y + height)) return ResizeHandle.S;
        if (isInHandle(px, py, x, y + height / 2)) return ResizeHandle.W;
        if (isInHandle(px, py, x + width, y + height / 2)) return ResizeHandle.E;

        return ResizeHandle.NONE;
    }

    private boolean isInHandle(double px, double py, double hx, double hy) {
        return px >= hx - HANDLE_OFFSET && px <= hx + HANDLE_OFFSET &&
                py >= hy - HANDLE_OFFSET && py <= hy + HANDLE_OFFSET;
    }

    public void resize(double deltaX, double deltaY, ResizeHandle handle) {
        double minSize = 50;

        switch (handle) {
            case SE:
                width = Math.max(minSize, width + deltaX);
                height = Math.max(minSize, height + deltaY);
                break;

            case NW:
                double newWidth = Math.max(minSize, width - deltaX);
                double newHeight = Math.max(minSize, height - deltaY);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;

            case NE:
                width = Math.max(minSize, width + deltaX);
                newHeight = Math.max(minSize, height - deltaY);
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;

            case SW:
                newWidth = Math.max(minSize, width - deltaX);
                height = Math.max(minSize, height + deltaY);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                break;

            case N:
                newHeight = Math.max(minSize, height - deltaY);
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;

            case S:
                height = Math.max(minSize, height + deltaY);
                break;

            case W:
                newWidth = Math.max(minSize, width - deltaX);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                break;

            case E:
                width = Math.max(minSize, width + deltaX);
                break;
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
                py >= y && py <= y + height;
    }

    @Override
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
}