@echo off
echo ========================================
echo MDZ_Slider 一键打包工具
echo ========================================
echo.

echo 检查 Java 版本...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo 错误：未找到 Java，请安装 JDK 17 或更高版本
    pause
    exit /b 1
)

echo.
echo 开始构建 MDZ_Slider...
echo.

echo 1. 构建 JAR 文件...
call gradlew.bat fatJar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ JAR 构建失败！
    echo 请检查项目配置和依赖
    pause
    exit /b 1
)
echo ✅ JAR 构建成功

echo.
echo 2. 生成 Windows 安装包...
echo 注意：跳过安装包生成（需要 WiX Toolset）
echo 直接使用 JAR 文件即可
echo ✅ 构建完成

echo.
echo ========================================
echo 🎉 构建完成！
echo ========================================
echo.
echo 生成的文件位置：
echo 📦 JAR 文件: build/libs/MDZ_Slider-1.0.jar
echo.
echo 使用方法：
echo 1. 运行 JAR: java -jar build/libs/MDZ_Slider-1.0.jar
echo 2. 创建快捷方式: 右键 JAR 文件 → 发送到 → 桌面快捷方式
echo.
pause 