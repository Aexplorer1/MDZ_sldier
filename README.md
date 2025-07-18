

# Slide

SlideMind 是一款基于 JavaFX 的智能幻灯片制作工具，集成了 AI 内容生成、智能排版、多语言支持、结构分析、模板管理、演讲者视图等多项创新功能，适用于教育、演讲、企业培训等多种场景。

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [系统要求](#系统要求)
- [安装与运行](#安装与运行)
- [主要界面与操作](#主要界面与操作)
- [AI 智能与多语言功能](#ai-智能与多语言功能)
- [技术架构与核心类](#技术架构与核心类)
- [常见问题与技巧](#常见问题与技巧)
- [开发与扩展](#开发与扩展)
- [未来规划](#未来规划)
- [致谢](#致谢)

---

## 项目简介

SlideMind 致力于让幻灯片创作更高效、更智能、更国际化。它不仅支持传统的文本、图片、绘图等元素编辑，还集成了 AI 生成 PPT、演讲稿、结构分析、关键词提取、智能排版、多语言翻译、模板管理、主题切换、演讲者视图等功能。

---

## 核心特性

- **幻灯片编辑**：支持文本、图片、绘图（矩形、圆形、线、箭头）等多种元素，支持拖拽、缩放、样式调整。
- **AI 智能功能**：
  - 一键生成结构化 PPT 内容和演讲稿
  - AI 问答、关键词提取、内容优化建议
  - 智能排版与结构分析（大纲、重点、逻辑关系图）
- **多语言支持**：支持中、英、日、韩、法、德、西、俄等多语言内容生成与界面切换。
- **模板管理**：本地提示词模板的增删改查、导入导出、备份恢复。
- **主题切换**：浅色/深色主题一键切换。
- **放映与演讲者视图**：全屏放映、演讲者视图（同步显示演讲稿、计时、分步提示）。
- **结构分析与逻辑关系图**：自动生成内容大纲、重点、逻辑关系图，辅助内容梳理。
- **现代化 UI**：响应式布局、渐变主题、流畅交互体验。

---

## 系统要求

- **Java 版本**：Java 17 或更高
- **操作系统**：Windows 10/11（建议），理论支持主流桌面系统
- **依赖**：JavaFX 21.0.1

---

## 安装与运行

### 1. 安装依赖

- 安装 Java 17+
- 下载 JavaFX SDK 21.0.1 并解压到 `lib/` 目录

### 2. 构建项目

```bash
./gradlew clean build
```

### 3. 运行应用

- 方式一：使用 Gradle
  ```bash
  ./gradlew run
  ```
- 方式二：直接运行 JAR
  ```bash
  java -jar build/libs/SlideMind-1.0.jar
  ```

### 4. 可选：创建桌面快捷方式

---

## 主要界面与操作

- **左侧栏**：文件操作、编辑、放映、AI、结构分析、多语言、模板管理、主题切换等入口
- **顶部工具栏**：新建/切换/删除幻灯片、添加元素、样式调整、AI生成、结构分析等
- **主画布区**：幻灯片内容编辑与预览
- **放映模式**：全屏展示，支持键盘/鼠标切换
- **演讲者视图**：多窗口布局，显示演讲稿、计时、幻灯片预览

---

## AI 智能与多语言功能

### AI 智能功能

- **AI 生成 PPT/演讲稿**：输入主题，自动生成结构化内容和演讲稿
- **AI 问答/关键词分析**：对当前内容提问、提取关键词、结构分析
- **智能排版**：一键优化当前幻灯片布局，支持多种布局类型（居中、左对齐、网格、流式、紧凑）
- **结构分析**：自动生成大纲、重点、逻辑关系图

### 多语言支持

- 一键翻译当前或全部幻灯片内容
- 批量生成多语言 PPT
- 支持多语言界面切换
- 国际化资源文件位于 `src/main/resources/i18n/`

### 模板管理

- 本地提示词模板的增删改查、导入导出、备份恢复
- 模板文件为 `templates.json`

---

## 技术架构与核心类

### 架构设计

- **MVC 架构**：模型（Slide/SlideElement）、视图（JavaFX Canvas/控件）、控制（事件与交互逻辑）
- **设计模式**：抽象工厂、观察者、命令、序列化等

### 主要核心类

- `Main`：主应用入口与 UI 控制
- `Slide`/`SlideElement`/`TextElement`/`ImageElement`/`DrawElement`：幻灯片及元素模型
- `AIAgent`/`AIEnhancedAgent`：AI 交互与智能功能
- `IntelligentLayoutEngine`：智能排版引擎
- `MultilingualSupport`：多语言支持系统
- `TemplateManager`：模板管理
- `PresentationWindow`/`SpeakerViewWindow`：放映与演讲者视图
- `SlideStructureAnalyzer`：结构分析与逻辑关系图

---

## 常见问题与技巧

1. **AI 功能不可用/无响应**
   - 检查网络与 API Key（需设置 `DEEPSEEK_API_KEY` 或 OpenAI Key 环境变量）
2. **多语言内容显示异常**
   - 检查系统编码（建议 UTF-8），确认目标语言已支持
3. **模板导入导出/备份**
   - 在模板管理窗口操作，或手动备份 `templates.json`
4. **主题切换无效**
   - 升级到最新版本，部分老版本不支持动态主题切换
5. **界面显示异常**
   - 检查 JavaFX 依赖与系统编码

---

## 开发与扩展

- **添加新语言**：新增资源文件 `messages_xx_XX.properties`，在 `SupportedLanguage` 枚举注册
- **扩展布局类型**：在 `LayoutType` 枚举和 `IntelligentLayoutEngine` 中添加新策略
- **集成新 AI/翻译 API**：在 `AIAgent`/`MultilingualSupport` 中扩展接口
- **Web 化与容器化**：可参考 `docker-web-conversion.md`，支持 Spring Boot + React/Vue 前后端分离与 Docker 部署

---

## 未来规划

- 丰富动画与交互
- 提升 AI 生成质量
- 支持更多文件格式与导出方式
- 云端同步与多用户协作
- 高级翻译与专业内容定制

---

## 致谢

感谢所有开源依赖、AI API 服务商及社区贡献者。  
如需进一步细化某一功能的说明或代码示例，请查阅项目根目录下的相关文档，或联系开发团队。
邮箱号：109308072@qq.com
---
