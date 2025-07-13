# AI生成PPT分页逻辑说明

## 概述

本项目提取了AI生成PPT的分页逻辑，将其封装在`SlideParser`类中，实现了从AI返回的命令字符串到幻灯片对象的转换。

## 核心组件

### 1. SlideParser类 (`src/main/java/slideshow/util/SlideParser.java`)

这是核心的解析器类，负责：
- 解析AI返回的PPT命令格式
- 创建对应的幻灯片对象
- 处理文本和绘图元素

#### 主要方法

```java
// 解析AI生成的PPT命令并创建幻灯片列表
public static List<Slide> parseAndCreateSlides(String aiResult, double slideWidth)

// 验证AI返回的PPT命令格式是否正确
public static boolean isValidPPTCommand(String aiResult)

// 获取PPT命令中的页面数量
public static int getPageCount(String aiResult)
```

### 2. 分页逻辑

#### 页面分割
使用正则表达式 `Page\\s*\\d+[:：]` 来分割页面：
- 支持 "Page 1:" 和 "Page 1：" 两种格式
- 自动忽略空页面

#### 元素解析
支持以下PPT命令格式：

1. **标题 (Title)**
   ```
   Title: 标题内容
   ```

2. **副标题 (Subtitle)**
   ```
   Subtitle: 副标题内容
   ```

3. **要点 (Bullet)**
   ```
   Bullet: 要点内容
   ```

4. **绘图 (Draw)**
   ```
   Draw: Line(x1,y1,x2,y2)
   Draw: Rectangle(x1,y1,x2,y2)
   Draw: Circle(centerX,centerY,radius)
   Draw: Arrow(x1,y1,x2,y2)
   ```

### 3. 布局逻辑

#### 文本元素布局
- 起始Y坐标：60像素
- 行间距：8像素
- 标题字体大小：28px，深蓝色，粗体
- 副标题字体大小：20px，深灰色，正常
- 要点字体大小：18px，黑色，正常
- 所有文本元素水平居中对齐

#### 绘图元素布局
- 直线：黑色，线宽2.0
- 矩形：橙色，线宽2.0
- 圆形：绿色，线宽2.0
- 箭头：红色，线宽2.0

## 使用示例

### 在Main类中的集成

```java
private void parseAndCreateSlides(String aiResult) {
    // 使用SlideParser解析AI生成的PPT命令
    slides = SlideParser.parseAndCreateSlides(aiResult, canvas.getWidth());
    
    // 更新当前幻灯片索引和显示
    currentSlideIndex = slides.isEmpty() ? -1 : 0;
    currentSlide = slides.isEmpty() ? null : slides.get(0);
    
    // 刷新画布和控件状态
    refreshCanvas();
    updateSlideControls();
}
```

### AI命令格式示例

```
Page 1:
Title: 人工智能简介
Subtitle: 探索AI的未来
Bullet: 什么是人工智能
Bullet: AI的发展历程
Bullet: AI的应用领域

Page 2:
Title: AI技术分类
Bullet: 机器学习
Bullet: 深度学习
Bullet: 自然语言处理
Draw: Rectangle(100,100,300,200)
```

## 测试

### SlideParserTest类 (`src/main/java/slideshow/SlideParserTest.java`)

提供了完整的测试用例，包括：
- 基本PPT命令格式测试
- 包含绘图命令的测试
- 空内容和无效格式测试
- 验证方法测试

### 运行测试

```bash
java slideshow.SlideParserTest
```

## 错误处理

- 解析错误会被捕获并记录，但不会中断整个解析过程
- 无效的绘图参数会被忽略
- 空页面会被自动跳过
- 提供验证方法检查命令格式的有效性

## 扩展性

该解析器设计具有良好的扩展性：
- 可以轻松添加新的元素类型
- 支持自定义布局参数
- 可以扩展支持更多的绘图类型
- 支持国际化（中英文冒号）

## 注意事项

1. AI返回的命令必须严格按照指定格式
2. 绘图参数必须是有效的数字
3. 页面编号建议连续，但不强制要求
4. 解析器对大小写敏感
5. 建议在生成PPT前验证命令格式的有效性 