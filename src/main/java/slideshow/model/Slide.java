package slideshow.model;

import javafx.scene.canvas.GraphicsContext;
import slideshow.elements.SlideElement;

import java.util.ArrayList;
import java.util.List;

public class Slide {
    private List<SlideElement> elements = new ArrayList<>();
    
    public void addElement(SlideElement element) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        elements.add(element);
    }
    
    public void removeElement(SlideElement element) {
        elements.remove(element);
    }
    
    /**
     * 获取幻灯片中的所有元素
     * @return 元素列表的副本
     */
    public List<SlideElement> getElements() {
        return new ArrayList<>(elements);
    }
    
    /**
     * 获取幻灯片中文本元素的内容
     * @return 文本内容列表
     */
    public List<String> getTextContent() {
        List<String> textContent = new ArrayList<>();
        for (SlideElement element : elements) {
            if (element instanceof slideshow.elements.TextElement) {
                slideshow.elements.TextElement textElement = (slideshow.elements.TextElement) element;
                String text = textElement.getText();
                if (text != null && !text.trim().isEmpty()) {
                    textContent.add(text.trim());
                }
            }
        }
        return textContent;
    }
    
    public void draw(GraphicsContext gc) {
        // 添加调试信息
        System.out.println("绘制幻灯片，元素数量：" + (elements != null ? elements.size() : 0));
        
        if (elements != null) {
            for (SlideElement element : elements) {
                element.draw(gc);
            }
        }
    }
    
    public SlideElement findElementAt(double x, double y) {
        // 从后往前遍历，这样可以选中最上层的元素
        for (int i = elements.size() - 1; i >= 0; i--) {
            SlideElement element = elements.get(i);
            if (element.containsPoint(x, y)) {
                return element;
            }
        }
        return null;
    }
} 