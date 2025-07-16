# IntelliJ IDEA 一键打包指南

## 方式一：使用 IDEA Artifacts（推荐）

### 1. 配置 Artifacts

1. **打开项目结构**
   - 菜单：`File` → `Project Structure` (快捷键 `Ctrl+Alt+Shift+S`)

2. **创建 JAR Artifact**
   - 选择 `Artifacts` 标签
   - 点击 `+` 号 → `JAR` → `From modules with dependencies`
   - 选择主模块和主类：`slideshow.Main`

3. **配置 JAR 设置**
   ```
   Name: MDZ_Slider
   Type: JAR
   Main Class: slideshow.Main
   ```

4. **添加 JavaFX 依赖**
   - 在 `Output Layout` 中，确保包含所有 JavaFX JAR 文件
   - 添加 `lib/javafx-sdk-21.0.1/lib/` 下的所有 JAR

### 2. 构建 JAR

1. **构建 Artifact**
   - 菜单：`Build` → `Build Artifacts` → `MDZ_Slider` → `Build`

2. **查找输出文件**
   - 生成的 JAR 在：`out/artifacts/MDZ_Slider/MDZ_Slider.jar`

### 3. 运行 JAR

```bash
# 使用 JavaFX 模块运行
java --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics \
     -jar out/artifacts/MDZ_Slider/MDZ_Slider.jar
```

## 方式二：使用 Gradle 集成

### 1. 配置 build.gradle

在 `build.gradle` 中添加 fat jar 任务：

```groovy
// 添加 fat jar 任务
task fatJar(type: Jar) {
    manifest {
        attributes 'Main-Class': 'slideshow.Main'
    }
    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
    with jar
    archiveBaseName = 'MDZ_Slider'
    archiveVersion = '1.0'
}

// 添加 JavaFX 启动脚本任务
task createStartScripts(type: CreateStartScripts) {
    outputDir = file('build/scripts')
    applicationName = 'MDZ_Slider'
    mainClass = 'slideshow.Main'
    defaultJvmOpts = [
        '--module-path', 'lib/javafx-sdk-21.0.1/lib',
        '--add-modules', 'javafx.controls,javafx.fxml,javafx.base,javafx.graphics',
        '-Dfile.encoding=UTF-8',
        '-Duser.language=zh',
        '-Duser.country=CN'
    ]
}
```

### 2. 在 IDEA 中运行 Gradle 任务

1. **打开 Gradle 工具窗口**
   - 菜单：`View` → `Tool Windows` → `Gradle`

2. **运行打包任务**
   - 展开 `Tasks` → `build`
   - 双击 `fatJar` 任务

3. **查找输出文件**
   - 生成的 JAR 在：`build/libs/MDZ_Slider-1.0.jar`

## 方式三：使用 IDEA 运行配置

### 1. 创建运行配置

1. **创建 Application 配置**
   - 菜单：`Run` → `Edit Configurations`
   - 点击 `+` → `Application`

2. **配置主类**
   ```
   Name: MDZ_Slider
   Main class: slideshow.Main
   Module: MDZ_sldier.main
   ```

3. **配置 VM 选项**
   ```
   VM options:
   --module-path lib/javafx-sdk-21.0.1/lib
   --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics
   -Dfile.encoding=UTF-8
   -Duser.language=zh
   -Duser.country=CN
   ```

### 2. 一键运行

- 点击运行按钮或按 `Shift+F10`

## 方式四：使用 IDEA 的 Build 工具

### 1. 配置 Build 配置

1. **创建 Build 配置**
   - 菜单：`Build` → `Edit Configurations`
   - 添加新的 `Build` 配置

2. **配置构建步骤**
   ```
   Name: Build MDZ_Slider
   Target: fatJar (Gradle task)
   ```

### 2. 一键构建

- 菜单：`Build` → `Build MDZ_Slider`

## 推荐的完整流程

### 1. 使用 Gradle 方式（最推荐）

```bash
# 在 IDEA 终端中执行
./gradlew fatJar
```

### 2. 创建启动脚本

创建 `run.bat` (Windows):
```batch
@echo off
java --module-path lib/javafx-sdk-21.0.1/lib ^
     --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics ^
     -Dfile.encoding=UTF-8 ^
     -Duser.language=zh ^
     -Duser.country=CN ^
     -jar build/libs/MDZ_Slider-1.0.jar
pause
```

创建 `run.sh` (Linux/Mac):
```bash
#!/bin/bash
java --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics \
     -Dfile.encoding=UTF-8 \
     -Duser.language=zh \
     -Duser.country=CN \
     -jar build/libs/MDZ_Slider-1.0.jar
```

### 3. 在 IDEA 中设置快捷键

1. **打开设置**
   - 菜单：`File` → `Settings` (快捷键 `Ctrl+Alt+S`)

2. **配置快捷键**
   - `Keymap` → 搜索 `Gradle` → `fatJar`
   - 设置快捷键，如 `Ctrl+Shift+B`

## 常见问题解决

### 1. JavaFX 模块找不到
- 确保 `lib/javafx-sdk-21.0.1/lib/` 目录存在
- 检查 JAR 文件是否完整

### 2. 编码问题
- 确保 VM 选项包含 `-Dfile.encoding=UTF-8`
- 检查 IDEA 的文件编码设置

### 3. 依赖问题
- 确保所有依赖都在 classpath 中
- 使用 fat jar 包含所有依赖

## 最佳实践

1. **使用 Gradle 方式**：最稳定，支持增量构建
2. **创建启动脚本**：方便用户运行
3. **设置快捷键**：提高开发效率
4. **版本管理**：在 build.gradle 中管理版本号 