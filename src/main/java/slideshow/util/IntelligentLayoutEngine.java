package slideshow.util;

import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;
import slideshow.elements.ImageElement;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AI智能排版引擎
 * 提供动态布局、内容自适应排版、智能空间分配和响应式设计功能
 */
public class IntelligentLayoutEngine {
    private static final Logger logger = Logger.getLogger(IntelligentLayoutEngine.class.getName());
    
    // 布局配置
    private static final double MIN_ELEMENT_SPACING = 10.0;
    private static final double MAX_TITLE_HEIGHT = 80.0;
    private static final double MAX_SUBTITLE_HEIGHT = 60.0;
    private static final double MAX_BULLET_HEIGHT = 40.0;
    private static final double SIDE_MARGIN = 50.0;
    private static final double TOP_MARGIN = 40.0;
    private static final double BOTTOM_MARGIN = 40.0;
    
    /**
     * 布局类型枚举
     */
    public enum LayoutType {
        CENTERED("居中布局"),
        LEFT_ALIGNED("左对齐布局"),
        GRID("网格布局"),
        FLOW("流式布局"),
        COMPACT("紧凑布局");
        
        private final String displayName;
        
        LayoutType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 智能优化幻灯片布局
     * 
     * @param slide 要优化的幻灯片
     * @param slideWidth 幻灯片宽度
     * @param slideHeight 幻灯片高度
     * @param layoutType 布局类型
     */
    public static void optimizeLayout(Slide slide, double slideWidth, double slideHeight, LayoutType layoutType) {
        try {
            logger.info("开始智能优化布局，布局类型: " + layoutType.getDisplayName());
            
            // 分析幻灯片内容
            LayoutAnalysis analysis = analyzeSlideContent(slide);
            
            // 根据内容类型和数量选择合适的布局策略
            LayoutStrategy strategy = selectLayoutStrategy(analysis, layoutType);
            
            // 应用布局策略
            applyLayoutStrategy(slide, strategy, slideWidth, slideHeight);
            
            logger.info("布局优化完成");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "布局优化失败", e);
        }
    }
    
    /**
     * 分析幻灯片内容
     */
    private static LayoutAnalysis analyzeSlideContent(Slide slide) {
        LayoutAnalysis analysis = new LayoutAnalysis();
        
        List<SlideElement> elements = slide.getElements();
        
        for (SlideElement element : elements) {
            if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                analysis.addTextElement(textElement);
            } else if (element instanceof DrawElement) {
                DrawElement drawElement = (DrawElement) element;
                analysis.addDrawElement(drawElement);
            } else if (element instanceof ImageElement) {
                ImageElement imageElement = (ImageElement) element;
                analysis.addImageElement(imageElement);
            }
        }
        
        return analysis;
    }
    
    /**
     * 选择布局策略
     */
    private static LayoutStrategy selectLayoutStrategy(LayoutAnalysis analysis, LayoutType layoutType) {
        LayoutStrategy strategy = new LayoutStrategy();
        strategy.setLayoutType(layoutType);
        
        // 根据内容类型调整策略
        if (analysis.getTextElements().size() > 5) {
            strategy.setCompactMode(true);
        }
        
        if (analysis.getDrawElements().size() > 3) {
            strategy.setGridMode(true);
        }
        
        if (analysis.getImageElements().size() > 0) {
            strategy.setImageOptimized(true);
        }
        
        return strategy;
    }
    
    /**
     * 应用布局策略
     */
    private static void applyLayoutStrategy(Slide slide, LayoutStrategy strategy, double slideWidth, double slideHeight) {
        List<SlideElement> elements = slide.getElements();
        
        // 清除现有位置
        slide.clearElements();
        
        switch (strategy.getLayoutType()) {
            case CENTERED:
                applyCenteredLayout(slide, elements, slideWidth, slideHeight, strategy);
                break;
            case LEFT_ALIGNED:
                applyLeftAlignedLayout(slide, elements, slideWidth, slideHeight, strategy);
                break;
            case GRID:
                applyGridLayout(slide, elements, slideWidth, slideHeight, strategy);
                break;
            case FLOW:
                applyFlowLayout(slide, elements, slideWidth, slideHeight, strategy);
                break;
            case COMPACT:
                applyCompactLayout(slide, elements, slideWidth, slideHeight, strategy);
                break;
        }
    }
    
    /**
     * 应用居中布局
     */
    private static void applyCenteredLayout(Slide slide, List<SlideElement> elements, 
                                          double slideWidth, double slideHeight, LayoutStrategy strategy) {
        double currentY = TOP_MARGIN;
        double availableWidth = slideWidth - 2 * SIDE_MARGIN;
        
        // 按类型分组元素
        List<TextElement> textElements = new ArrayList<>();
        List<DrawElement> drawElements = new ArrayList<>();
        List<ImageElement> imageElements = new ArrayList<>();
        
        for (SlideElement element : elements) {
            if (element instanceof TextElement) {
                textElements.add((TextElement) element);
            } else if (element instanceof DrawElement) {
                drawElements.add((DrawElement) element);
            } else if (element instanceof ImageElement) {
                imageElements.add((ImageElement) element);
            }
        }
        
        // 布局文本元素
        for (TextElement textElement : textElements) {
            double elementWidth = Math.min(textElement.getWidth(), availableWidth);
            double x = (slideWidth - elementWidth) / 2;
            
            textElement.setX(x);
            textElement.setY(currentY);
            
            slide.addElement(textElement);
            currentY += textElement.getHeight() + MIN_ELEMENT_SPACING;
        }
        
        // 布局绘图元素
        for (DrawElement drawElement : drawElements) {
            double x = (slideWidth - drawElement.getWidth()) / 2;
            drawElement.setX(x);
            drawElement.setY(currentY);
            
            slide.addElement(drawElement);
            currentY += drawElement.getHeight() + MIN_ELEMENT_SPACING;
        }
        
        // 布局图片元素
        for (ImageElement imageElement : imageElements) {
            double x = (slideWidth - imageElement.getWidth()) / 2;
            imageElement.setX(x);
            imageElement.setY(currentY);
            
            slide.addElement(imageElement);
            currentY += imageElement.getHeight() + MIN_ELEMENT_SPACING;
        }
    }
    
    /**
     * 应用左对齐布局
     */
    private static void applyLeftAlignedLayout(Slide slide, List<SlideElement> elements, 
                                             double slideWidth, double slideHeight, LayoutStrategy strategy) {
        double currentY = TOP_MARGIN;
        double x = SIDE_MARGIN;
        
        for (SlideElement element : elements) {
            element.setX(x);
            element.setY(currentY);
            
            slide.addElement(element);
            currentY += element.getHeight() + MIN_ELEMENT_SPACING;
        }
    }
    
    /**
     * 应用网格布局
     */
    private static void applyGridLayout(Slide slide, List<SlideElement> elements, 
                                      double slideWidth, double slideHeight, LayoutStrategy strategy) {
        int columns = calculateOptimalColumns(elements.size(), slideWidth);
        int rows = (int) Math.ceil((double) elements.size() / columns);
        
        double cellWidth = (slideWidth - 2 * SIDE_MARGIN) / columns;
        double cellHeight = (slideHeight - TOP_MARGIN - BOTTOM_MARGIN) / rows;
        
        int elementIndex = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns && elementIndex < elements.size(); col++) {
                SlideElement element = elements.get(elementIndex);
                
                double x = SIDE_MARGIN + col * cellWidth + (cellWidth - element.getWidth()) / 2;
                double y = TOP_MARGIN + row * cellHeight + (cellHeight - element.getHeight()) / 2;
                
                element.setX(x);
                element.setY(y);
                
                slide.addElement(element);
                elementIndex++;
            }
        }
    }
    
    /**
     * 应用流式布局
     */
    private static void applyFlowLayout(Slide slide, List<SlideElement> elements, 
                                      double slideWidth, double slideHeight, LayoutStrategy strategy) {
        double currentX = SIDE_MARGIN;
        double currentY = TOP_MARGIN;
        double maxHeightInRow = 0;
        
        for (SlideElement element : elements) {
            // 检查是否需要换行
            if (currentX + element.getWidth() > slideWidth - SIDE_MARGIN) {
                currentX = SIDE_MARGIN;
                currentY += maxHeightInRow + MIN_ELEMENT_SPACING;
                maxHeightInRow = 0;
            }
            
            element.setX(currentX);
            element.setY(currentY);
            
            slide.addElement(element);
            
            currentX += element.getWidth() + MIN_ELEMENT_SPACING;
            maxHeightInRow = Math.max(maxHeightInRow, element.getHeight());
        }
    }
    
    /**
     * 应用紧凑布局
     */
    private static void applyCompactLayout(Slide slide, List<SlideElement> elements, 
                                         double slideWidth, double slideHeight, LayoutStrategy strategy) {
        double currentY = TOP_MARGIN;
        double spacing = MIN_ELEMENT_SPACING / 2; // 减少间距
        
        for (SlideElement element : elements) {
            double x = (slideWidth - element.getWidth()) / 2;
            element.setX(x);
            element.setY(currentY);
            
            slide.addElement(element);
            currentY += element.getHeight() + spacing;
        }
    }
    
    /**
     * 计算最优列数
     */
    private static int calculateOptimalColumns(int elementCount, double slideWidth) {
        if (elementCount <= 1) return 1;
        if (elementCount <= 4) return 2;
        if (elementCount <= 9) return 3;
        return 4;
    }
    
    /**
     * 响应式调整布局
     */
    public static void responsiveAdjust(Slide slide, double newWidth, double newHeight) {
        logger.info("开始响应式调整布局，新尺寸: " + newWidth + "x" + newHeight);
        
        // 重新计算所有元素的位置和大小
        List<SlideElement> elements = slide.getElements();
        double scaleX = newWidth / slide.getWidth();
        double scaleY = newHeight / slide.getHeight();
        
        for (SlideElement element : elements) {
            // 调整位置
            element.setX(element.getX() * scaleX);
            element.setY(element.getY() * scaleY);
            
            // 调整大小（如果元素支持）
            if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                textElement.setWidth(textElement.getWidth() * scaleX);
                textElement.setHeight(textElement.getHeight() * scaleY);
            }
        }
        
        slide.setWidth(newWidth);
        slide.setHeight(newHeight);
        
        logger.info("响应式调整完成");
    }
    
    /**
     * 自动调整文本大小以适应容器
     */
    public static void autoAdjustTextSize(TextElement textElement, double maxWidth, double maxHeight) {
        double fontSize = textElement.getFontSize();
        double currentWidth = textElement.getWidth();
        double currentHeight = textElement.getHeight();
        
        // 如果文本太大，逐步减小字体大小
        while ((currentWidth > maxWidth || currentHeight > maxHeight) && fontSize > 8) {
            fontSize -= 1;
            textElement.setFontSize(fontSize);
            currentWidth = textElement.getWidth();
            currentHeight = textElement.getHeight();
        }
        
        // 如果文本太小，逐步增大字体大小
        while (currentWidth < maxWidth * 0.8 && currentHeight < maxHeight * 0.8 && fontSize < 48) {
            fontSize += 1;
            textElement.setFontSize(fontSize);
            currentWidth = textElement.getWidth();
            currentHeight = textElement.getHeight();
        }
    }
    
    /**
     * 布局分析类
     */
    private static class LayoutAnalysis {
        private List<TextElement> textElements = new ArrayList<>();
        private List<DrawElement> drawElements = new ArrayList<>();
        private List<ImageElement> imageElements = new ArrayList<>();
        
        public void addTextElement(TextElement element) {
            textElements.add(element);
        }
        
        public void addDrawElement(DrawElement element) {
            drawElements.add(element);
        }
        
        public void addImageElement(ImageElement element) {
            imageElements.add(element);
        }
        
        public List<TextElement> getTextElements() {
            return textElements;
        }
        
        public List<DrawElement> getDrawElements() {
            return drawElements;
        }
        
        public List<ImageElement> getImageElements() {
            return imageElements;
        }
    }
    
    /**
     * 布局策略类
     */
    private static class LayoutStrategy {
        private LayoutType layoutType = LayoutType.CENTERED;
        private boolean compactMode = false;
        private boolean gridMode = false;
        private boolean imageOptimized = false;
        
        public LayoutType getLayoutType() {
            return layoutType;
        }
        
        public void setLayoutType(LayoutType layoutType) {
            this.layoutType = layoutType;
        }
        
        public boolean isCompactMode() {
            return compactMode;
        }
        
        public void setCompactMode(boolean compactMode) {
            this.compactMode = compactMode;
        }
        
        public boolean isGridMode() {
            return gridMode;
        }
        
        public void setGridMode(boolean gridMode) {
            this.gridMode = gridMode;
        }
        
        public boolean isImageOptimized() {
            return imageOptimized;
        }
        
        public void setImageOptimized(boolean imageOptimized) {
            this.imageOptimized = imageOptimized;
        }
    }
} 