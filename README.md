# MDZ Slider

一个简单的幻灯片编辑器，使用 JavaFX 构建。

## 功能特性

- 创建和编辑幻灯片
- 添加和编辑文本元素
- 拖拽元素改变位置

## 环境要求

- Java 17 或更高版本
- JavaFX 21.0.1

## 构建和运行

1. 下载 JavaFX SDK 21.0.1

```bash
mkdir -p lib
cd lib
curl -L -o javafx-sdk.zip https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_osx-aarch64_bin-sdk.zip
unzip javafx-sdk.zip
mv javafx-sdk-21.0.1 javafx-sdk-21.0.1
cd ..
```

2. 使用 Gradle 构建和运行

```bash
./gradlew clean build
./gradlew run
```

## 使用说明

1. 点击"新建幻灯片"按钮创建新的幻灯片
2. 点击"添加文本"按钮添加文本元素
3. 使用鼠标拖动文本元素改变位置

## 开发计划

- [ ] 完善文本编辑功能
- [ ] 添加图片支持
- [ ] 实现多页幻灯片管理
- [ ] 添加保存/加载功能
- [ ] 添加幻灯片播放功能

## 许可证

MIT License 