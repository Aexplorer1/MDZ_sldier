@echo off
echo 启动 MDZ_Slider 应用...
echo.

REM 检查 JAR 文件是否存在
if not exist "build\libs\MDZ_Slider-1.0.jar" (
    echo 错误：找不到 JAR 文件，请先运行 ./gradlew fatJar
    pause
    exit /b 1
)

REM 检查 JavaFX 库是否存在
if not exist "lib\javafx-sdk-21.0.1\lib\" (
    echo 错误：找不到 JavaFX 库文件
    pause
    exit /b 1
)

echo 正在启动应用...
java --module-path lib/javafx-sdk-21.0.1/lib ^
     --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics ^
     -Dfile.encoding=UTF-8 ^
     -Duser.language=zh ^
     -Duser.country=CN ^
     -jar build/libs/MDZ_Slider-1.0.jar

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 应用启动失败，错误代码：%ERRORLEVEL%
    echo 请检查 Java 版本和 JavaFX 库是否正确安装
)

pause 