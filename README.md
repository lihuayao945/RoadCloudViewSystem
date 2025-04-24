# 🚗 智能网联汽车云控系统

<div align="center">
  <img src="https://github.com/user-attachments/assets/0c7531f8-5c91-415e-8d3e-0c9fd918622c" alt="系统首页预览" width="800"/>
  <p><em>系统首页预览</em></p>
</div>

> 一个基于Vue.js和Spring Boot的智能网联汽车云控系统，提供实时数据可视化、3D场景展示和数据分析功能。

## ✨ 功能特点

<div align="center">
  <table>
    <tr>
      <td align="center">🌐</td>
      <td>实时数据监控与可视化</td>
      <td align="center">🎮</td>
      <td>3D场景交互展示</td>
    </tr>
    <tr>
      <td align="center">📊</td>
      <td>数据分析与报表</td>
      <td align="center">🔄</td>
      <td>车云数据交互</td>
    </tr>
    <tr>
      <td align="center">🛣️</td>
      <td>路云数据交互</td>
      <td align="center">📱</td>
      <td>响应式设计</td>
    </tr>
  </table>
</div>

## 🛠️ 技术栈

### 前端 (RoadCloud)
<div align="center">
  <img src="https://img.shields.io/badge/Vue.js-3-4FC08D?style=flat&logo=vue.js" alt="Vue.js 3"/>
  <img src="https://img.shields.io/badge/Vite-4.0-646CFF?style=flat&logo=vite" alt="Vite"/>
  <img src="https://img.shields.io/badge/Element Plus-2.0-409EFF?style=flat&logo=element" alt="Element Plus"/>
  <img src="https://img.shields.io/badge/Three.js-0.150-000000?style=flat&logo=three.js" alt="Three.js"/>
  <img src="https://img.shields.io/badge/ECharts-5.0-AA344D?style=flat&logo=apache" alt="ECharts"/>
</div>

### 后端 (RoadCloudVisualizationSystem)
<div align="center">
  <img src="https://img.shields.io/badge/Spring Boot-3.0-6DB33F?style=flat&logo=spring" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring Security-6.0-6DB33F?style=flat&logo=spring" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/MyBatis-3.5-000000?style=flat&logo=mybatis" alt="MyBatis"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Redis-7.0-DC382D?style=flat&logo=redis" alt="Redis"/>
  <img src="https://img.shields.io/badge/MQTT-5.0-660066?style=flat&logo=mqtt" alt="MQTT"/>
</div>

### 3D建模
<div align="center">
  <img src="https://img.shields.io/badge/Blender-3.0-E87D0D?style=flat&logo=blender" alt="Blender"/>
  <img src="https://img.shields.io/badge/实时渲染引擎-自定义-000000?style=flat" alt="实时渲染引擎"/>
</div>

## 📁 项目结构

```
RoadCloudVisualizationSystem/
├── RoadCloud/                    # 前端项目目录
│   ├── src/                      # 源代码
│   ├── public/                   # 静态资源
│   └── package.json              # 前端依赖配置
│
├── RoadCloudVisualizationSystem/ # 后端项目目录
│   ├── src/                      # 源代码
│   ├── resources/                # 配置文件
│   └── pom.xml                   # 后端依赖配置
│
└── 3D-Models/                    # 3D建模文件目录
    ├── blender/                  # Blender源文件
    └── exports/                  # 导出模型文件
```

## 📦 环境要求

- Node.js >= 16.0.0
- JDK >= 17
- MySQL >= 8.0
- Redis >= 7.0
- MQTT Broker

## 🚀 快速开始

1. 克隆项目
```bash
git clone https://github.com/your-username/RoadCloudVisualizationSystem.git
```

2. 安装依赖
```bash
# 前端依赖
cd RoadCloud
npm install

# 后端依赖
cd ../RoadCloudVisualizationSystem
mvn install
```

3. 启动服务
```bash
# 启动前端
cd RoadCloud
npm run dev

# 启动后端
cd ../RoadCloudVisualizationSystem
mvn spring-boot:run
```

## 📝 许可证

[MIT License](LICENSE)

---
