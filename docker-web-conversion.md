# 将 SlideMind 转换为 Web 应用并容器化部署

## 转换方案

### 1. 技术栈选择
- **前端**: React/Vue.js + Canvas API
- **后端**: Spring Boot + REST API
- **容器**: Docker + Docker Compose

### 2. 架构转换

#### 前端改造
```javascript
// 使用 Canvas API 替代 JavaFX Canvas
const canvas = document.getElementById('slideCanvas');
const ctx = canvas.getContext('2d');

// 实现相同的绘图功能
class SlideElement {
    draw(ctx) {
        // 绘制逻辑
    }
}
```

#### 后端 API 设计
```java
@RestController
@RequestMapping("/api/slides")
public class SlideController {
    
    @PostMapping("/create")
    public Slide createSlide(@RequestBody SlideRequest request) {
        // 创建幻灯片
    }
    
    @PostMapping("/{id}/elements")
    public void addElement(@PathVariable String id, @RequestBody ElementRequest request) {
        // 添加元素
    }
    
    @GetMapping("/{id}")
    public Slide getSlide(@PathVariable String id) {
        // 获取幻灯片
    }
}
```

### 3. Docker 配置

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制应用文件
COPY build/libs/*.jar app.jar
COPY lib/ lib/

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### docker-compose.yml
```yaml
version: '3.8'
services:
  mdz-slider:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
    volumes:
      - ./data:/app/data
```

### 4. 部署步骤

1. **构建 Web 应用**
```bash
./gradlew build
```

2. **构建 Docker 镜像**
```bash
docker build -t mdz-slider:latest .
```

3. **运行容器**
```bash
docker-compose up -d
```

4. **访问应用**
```
http://localhost:8080
```

## 优势
- 跨平台访问
- 易于部署和扩展
- 支持多用户协作
- 数据集中管理

## 挑战
- 需要重写大量前端代码
- Canvas API 功能可能不如 JavaFX 丰富
- 需要处理实时协作
- 性能可能不如桌面应用 