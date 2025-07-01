# AIAgent 使用说明

## 概述

`AIAgent` 是一个专门用于处理AI模型交互的代理类，主要负责根据幻灯片内容生成演讲稿等功能。

## 主要功能

### generateSpeechBySlides(List<Slide> slides)

根据幻灯片内容生成演讲稿。

**参数：**
- `slides`: 幻灯片列表，不能为空

**返回值：**
- `String`: 生成的演讲稿文本

**异常：**
- `AIException`: AI调用失败时抛出
- `IllegalArgumentException`: 参数无效时抛出

## 使用方法

### 1. 基本使用

```java
// 创建AI模型
OpenAiChatModel aiModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .baseUrl("https://api.deepseek.com")
    .modelName("deepseek-chat")
    .build();

// 创建AIAgent实例
AIAgent aiAgent = new AIAgent(aiModel);

// 生成演讲稿
try {
    String speech = aiAgent.generateSpeechBySlides(slides);
    System.out.println("生成的演讲稿：" + speech);
} catch (AIAgent.AIException e) {
    System.err.println("AI调用失败：" + e.getMessage());
} catch (IllegalArgumentException e) {
    System.err.println("参数错误：" + e.getMessage());
}
```

### 2. 在JavaFX应用中使用

```java
// 在Main类中初始化
private AIAgent aiAgent;

@Override
public void start(Stage primaryStage) {
    // 初始化AI模型
    aiModel = OpenAiChatModel.builder()
        .apiKey(getApiKey())
        .baseUrl("https://api.deepseek.com")
        .modelName("deepseek-chat")
        .build();
    
    // 初始化AIAgent
    aiAgent = new AIAgent(aiModel);
}

// 生成演讲稿的方法
private void generateSpeechFromSlides() {
    if (slides.isEmpty()) {
        showError("生成演讲稿失败", "当前没有幻灯片内容");
        return;
    }
    
    // 在新线程中执行AI调用
    new Thread(() -> {
        try {
            String speech = aiAgent.generateSpeechBySlides(slides);
            
            Platform.runLater(() -> {
                showSpeechDialog(speech);
            });
            
        } catch (AIAgent.AIException e) {
            Platform.runLater(() -> {
                showError("AI调用失败", "生成演讲稿时发生错误: " + e.getMessage());
            });
        } catch (IllegalArgumentException e) {
            Platform.runLater(() -> {
                showError("参数错误", "参数验证失败: " + e.getMessage());
            });
        }
    }).start();
}
```

## 异常处理机制

### 1. 参数验证异常

- **空AI模型**: 构造函数中传入null时抛出`IllegalArgumentException`
- **空幻灯片列表**: 传入null或空列表时抛出`IllegalArgumentException`

### 2. AI调用异常

- **网络错误**: 网络连接失败时抛出`AIException`
- **API错误**: API调用失败时抛出`AIException`
- **响应解析错误**: AI响应为空或格式错误时抛出`AIException`

### 3. 异常处理示例

```java
try {
    String speech = aiAgent.generateSpeechBySlides(slides);
    // 处理成功结果
} catch (AIAgent.AIException e) {
    // 处理AI相关错误
    logger.log(Level.SEVERE, "AI调用失败", e);
    showError("AI调用失败", e.getMessage());
} catch (IllegalArgumentException e) {
    // 处理参数错误
    logger.log(Level.SEVERE, "参数验证失败", e);
    showError("参数错误", e.getMessage());
} catch (Exception e) {
    // 处理其他未知错误
    logger.log(Level.SEVERE, "未知错误", e);
    showError("未知错误", e.getMessage());
}
```

## 测试

### 运行测试

```bash
# 设置API密钥环境变量
export DEEPSEEK_API_KEY="your-api-key"

# 运行测试
java slideshow.AIAgentTest
```

### 测试内容

1. **异常处理测试**: 验证各种异常情况的处理
2. **功能测试**: 验证演讲稿生成功能
3. **参数验证测试**: 验证输入参数的验证逻辑

## 配置要求

### 环境变量

- `DEEPSEEK_API_KEY`: DeepSeek API密钥

### 依赖

- `dev.langchain4j:langchain4j-open-ai`: LangChain4j OpenAI集成
- `javafx`: JavaFX运行时

## 注意事项

1. **API密钥安全**: 不要在代码中硬编码API密钥，使用环境变量或配置文件
2. **网络连接**: 确保网络连接正常，AI调用需要访问外部API
3. **异步处理**: 在UI应用中，AI调用应该在后台线程中执行
4. **错误处理**: 始终包含适当的异常处理机制
5. **日志记录**: 使用日志记录AI调用的状态和错误信息

## 扩展功能

可以根据需要扩展AIAgent类，添加更多功能：

- 生成PPT大纲
- 优化幻灯片内容
- 生成演讲建议
- 多语言支持
- 自定义提示词模板 