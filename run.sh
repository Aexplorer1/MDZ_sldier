#!/bin/bash

echo "启动 MDZ_Slider 应用..."
echo

# 检查 JAR 文件是否存在
if [ ! -f "build/libs/MDZ_Slider-1.0.jar" ]; then
    echo "错误：找不到 JAR 文件，请先运行 ./gradlew fatJar"
    exit 1
fi

# 检查 JavaFX 库是否存在
if [ ! -d "lib/javafx-sdk-21.0.1/lib" ]; then
    echo "错误：找不到 JavaFX 库文件"
    exit 1
fi

echo "正在启动应用..."
java --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics \
     -Dfile.encoding=UTF-8 \
     -Duser.language=zh \
     -Duser.country=CN \
     -jar build/libs/MDZ_Slider-1.0.jar

if [ $? -ne 0 ]; then
    echo
    echo "应用启动失败，错误代码：$?"
    echo "请检查 Java 版本和 JavaFX 库是否正确安装"
fi 