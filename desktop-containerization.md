# 桌面应用容器化方案（不推荐）

## 技术方案

### 1. 使用 X11 转发
```dockerfile
FROM ubuntu:20.04

# 安装 Java 和 X11 相关包
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    xvfb \
    x11vnc \
    fluxbox \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY build/libs/*.jar app.jar
COPY lib/ lib/

# 启动脚本
COPY start.sh /start.sh
RUN chmod +x /start.sh

EXPOSE 5900
CMD ["/start.sh"]
```

### 2. 启动脚本
```bash
#!/bin/bash
# start.sh

# 启动虚拟显示
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99

# 启动窗口管理器
fluxbox &

# 启动 VNC 服务器
x11vnc -display :99 -nopw -listen localhost -xkb -ncache 10 -ncache_cr -forever &

# 启动 Java 应用
java -jar app.jar
```

### 3. docker-compose.yml
```yaml
version: '3.8'
services:
  mdz-slider:
    build: .
    ports:
      - "5900:5900"
    environment:
      - DISPLAY=:99
    volumes:
      - ./data:/app/data
```

## 使用方式

1. **构建并运行容器**
```bash
docker-compose up -d
```

2. **使用 VNC 客户端连接**
- 地址: localhost:5900
- 无需密码

## 限制和问题

### 技术限制
- **性能问题**: 图形渲染性能差
- **用户体验**: 需要 VNC 客户端
- **交互延迟**: 网络传输延迟
- **兼容性**: 不同系统 VNC 客户端差异

### 实际应用场景
- 仅适用于**远程演示**或**测试环境**
- 不适合**生产环境**或**日常使用**
- 主要用于**自动化测试**或**演示目的**

## 推荐方案

对于桌面应用，推荐以下部署方式：

### 1. 传统分发
- 打包成 Windows 可执行文件（.exe）
- 使用 MSI 安装包
- 通过官网或应用商店分发

### 2. 现代化分发
- 使用 jpackage 生成安装包
- 支持自动更新机制
- 集成到系统包管理器

### 3. 云端部署（如果必须）
- 转换为 Web 应用（推荐）
- 使用远程桌面解决方案
- 考虑 SaaS 模式

## 结论

**不建议**将桌面应用容器化，因为：
1. 用户体验差
2. 性能损失严重
3. 维护成本高
4. 不符合桌面应用的使用场景

**建议**：
- 保持桌面应用的本质
- 使用传统的分发方式
- 如需网络功能，考虑 Web 版本 