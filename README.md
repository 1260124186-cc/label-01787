# 小安的书店

## How to Run

### 方式一：Docker 一键启动（推荐）

**环境要求：** Docker & Docker Compose、微信开发者工具

```bash
# 克隆项目
git clone <repo-url>
cd label-01787

# 一键启动所有服务（后端 + 管理后台 + MySQL）
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

启动后访问：
- 管理后台：http://localhost:8081
- 后端 API：http://localhost:8080
- 小程序端：使用微信开发者工具打开 `frontend-mp/` 目录

停止服务：
```bash
docker-compose down         # 停止并移除容器
docker-compose down -v      # 停止并移除容器和数据卷（会清空数据库）
```

### 方式二：本地开发启动

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0 |
| 微信开发者工具 | 最新版 |

**1. 启动 MySQL**

使用 Docker：`docker-compose up -d --build mysql`（端口 3307，后端需设置 `MYSQL_PORT=3307`）

或使用本地 MySQL（默认 3306）：
```sql
CREATE DATABASE IF NOT EXISTS xiaoan_bookstore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
```bash
mysql -u root -p xiaoan_bookstore < backend/src/main/resources/schema.sql
```

**2. 启动后端**
```bash
cd backend
mvn spring-boot:run
```
后端 API：http://localhost:9090

**3. 启动管理后台**
```bash
cd frontend-admin
npm install
npm run dev
```
管理后台：http://localhost:3000

**4. 启动微信小程序**

打开微信开发者工具，导入 `frontend-mp/` 目录，确认 `app.js` 中 `baseUrl` 指向后端地址，编译运行。

## Services

| 服务 | Docker 端口 | 本地开发端口 | 说明 |
|------|-------------|-------------|------|
| backend | 8080 | 9090 | 后端 API（Java 17 + Spring Boot 3 + MyBatis-Plus） |
| frontend-admin | 8081 | 3000 | 管理后台（Vue 3 + Vite + Element Plus） |
| mysql | 3307 | 3306 | 数据库（MySQL 8.0） |
| frontend-mp | - | - | 微信小程序（微信开发者工具运行） |

## 测试账号

| 平台 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理后台 | admin | admin123 | 超级管理员，可访问用户管理、书籍管理、操作日志等 |
| 微信小程序 | - | - | 通过微信开发者工具自动静默登录，无需手动输入账号密码 |

## 题目内容

我想做一个微信小程序，名字叫"小安的书店"，用户可以自由上传pdf文件，文件大小1-150mb范围内即可上传，在阅读pdf时可以看目录，翻页（上下滚动、左右翻页）可以选中某句话添加评语、记笔记、可以设置文件类型区别放置，可以有不同主题颜色（护眼绿、夜空黑、白色）并且有阅读时长记录，能够做周期性总结（周、月、年）

## 项目简介

"小安的书店"是一款面向阅读爱好者的微信小程序，集成了 PDF 上传与管理、在线阅读、批注笔记、分类管理、主题切换和阅读统计等功能，帮助用户构建个人数字书架，养成良好的阅读习惯。

### 核心功能

- PDF 上传与管理：支持 1-150MB 的 PDF 文件上传，自动解析页数和目录
- 在线阅读器：支持上下滚动和左右翻页两种阅读模式
- 目录导航：自动提取 PDF 目录，支持快速跳转
- 批注与笔记：选中文本添加评语，自由记录读书笔记
- 分类管理：自定义书籍分类，有序管理个人书架
- 主题切换：护眼绿、夜空黑、白色三种阅读主题
- 阅读统计：记录阅读时长，周/月/年维度数据总结
- 管理后台：用户管理、书籍管理、操作日志等

### 技术栈

- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8.0
- 管理后台：Vue 3 + Vite + Element Plus + Pinia + Scss
- 小程序：原生微信小程序
- 基础设施：Docker + Docker Compose

## 项目结构

```
label-01787/
├── backend/                          # 后端服务（Spring Boot）
│   ├── Dockerfile
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/main/
│       ├── java/com/xiaoan/bookstore/
│       │   ├── BookstoreApplication.java   # 启动入口
│       │   ├── annotation/           # 自定义注解（操作日志）
│       │   ├── aspect/               # AOP 切面
│       │   ├── common/               # 公共类（常量、统一响应）
│       │   ├── config/               # 配置类（MyBatis-Plus、WebMvc）
│       │   ├── controller/           # 控制器层
│       │   ├── dto/                  # 数据传输对象
│       │   ├── entity/               # 数据库实体
│       │   ├── exception/            # 异常处理
│       │   ├── interceptor/          # JWT 拦截器
│       │   ├── mapper/               # MyBatis Mapper
│       │   ├── service/              # 业务逻辑层
│       │   └── util/                 # 工具类（JWT、微信）
│       └── resources/
│           ├── application.yml       # 应用配置
│           └── schema.sql            # 数据库初始化脚本
├── frontend-admin/                   # 管理后台（Vue 3）
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/admin.js              # API 请求封装
│       ├── router/index.js           # 路由配置
│       ├── store/user.js             # Pinia 状态管理
│       ├── styles/                   # 全局样式
│       ├── utils/request.js          # Axios 请求封装
│       └── views/                    # 页面组件
├── frontend-mp/                      # 微信小程序
│   ├── app.js / app.json / app.wxss
│   ├── assets/                       # 静态资源
│   ├── pages/                        # 页面目录
│   │   ├── bookshelf/                # 书架页
│   │   ├── reader/                   # 阅读器页
│   │   ├── notes/                    # 笔记页
│   │   ├── summary/                  # 阅读统计页
│   │   ├── profile/                  # 个人中心页
│   │   └── category/                 # 分类管理页
│   └── utils/                        # 工具函数
├── docs/project_design.md
├── docker-compose.yml
└── README.md
```
