<div align="center">

<img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white" alt="Java 21">
<img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen?logo=springboot&logoColor=white" alt="Spring Boot">
<img src="https://img.shields.io/badge/Spring%20Cloud-2021.0.9-blue?logo=spring&logoColor=white" alt="Spring Cloud">
<img src="https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql&logoColor=white" alt="MySQL">
<img src="https://img.shields.io/badge/Maven-3.8+-c71a36?logo=apachemaven&logoColor=white" alt="Maven">
<img src="https://img.shields.io/badge/license-MIT-green" alt="License">

</div>

<br>

# ShareSystem

> 基于 Spring Cloud 微服务架构的个人学习资料分享平台 —— 轻量、安全、开箱即用

**ShareSystem** 是一个面向个人 / 教育场景的私有网盘系统，提供文件上传、在线预览、安全分享与后台管理等功能。采用前后端分离 + 微服务架构设计，适合课程设计、个人部署与二次开发。

<p align="center">
  <em>Apple 风格扁平化 UI · 分片上传 & 秒传 · JWT 无状态认证 · 在线预览</em>
</p>

---

## ✨ 功能特性

<table>
  <tr>
    <td width="50%">
      <h4>📁 文件管理</h4>
      <ul>
        <li>文件 / 文件夹 CRUD（新建、重命名、移动、删除）</li>
        <li>回收站（还原 / 彻底删除 / 清空）</li>
        <li>网格 & 列表双视图切换</li>
        <li>全文搜索</li>
      </ul>
    </td>
    <td width="50%">
      <h4>⚡ 智能上传</h4>
      <ul>
        <li>大文件分片上传（默认 1MB 分片）</li>
        <li>MD5 秒传（重复文件去重）</li>
        <li>断点续传（进度可查询 & 恢复）</li>
        <li>Range 断点下载</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>
      <h4>👁️ 在线预览</h4>
      <ul>
        <li>图片 / 视频 / 音频内嵌播放</li>
        <li>PDF 文档在线浏览</li>
        <li>Office 文档预览（Word / Excel / PPT）</li>
        <li>纯文本 & 代码高亮</li>
      </ul>
    </td>
    <td>
      <h4>🔗 安全分享</h4>
      <ul>
        <li>分享码 + 提取码双重保护</li>
        <li>可设置有效期</li>
        <li>浏览 & 下载次数统计</li>
        <li>随时取消分享</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>
      <h4>🔐 用户认证</h4>
      <ul>
        <li>JWT 无状态 Token 鉴权</li>
        <li>Gateway 全局过滤器统一校验</li>
        <li>QQ OAuth 2.0 第三方登录（预留）</li>
        <li>角色权限控制（用户 / 管理员）</li>
      </ul>
    </td>
    <td>
      <h4>🛡️ 管理后台</h4>
      <ul>
        <li>用户管理（启停、配额分配）</li>
        <li>文件审计（查看所有文件）</li>
        <li>分享监控（查看 & 取消分享）</li>
        <li>操作日志追踪</li>
      </ul>
    </td>
  </tr>
</table>

---

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────┐
│                   Browser / Client                    │
│              HTML5 + CSS3 + JavaScript                │
│               Apple 简约扁平化设计风格                    │
└─────────────────────┬────────────────────────────────┘
                      │  HTTP :8180
┌─────────────────────▼────────────────────────────────┐
│             API Gateway (share-gateway)                │
│     Spring Cloud Gateway + JWT Auth + CORS             │
│     Port: 8180  |  前端静态资源托管                      │
└────┬──────────────┬──────────────────┬────────────────┘
     │              │                  │
┌────▼──────┐ ┌─────▼──────┐ ┌───────▼──────────┐
│  Nacos    │ │  User Svc  │ │   File Svc        │
│  (可选)    │ │  :8181     │ │   :8182           │
│           │ │            │ │                   │
│ Discovery │ │ 注册/登录   │ │ 文件 CRUD          │
│ & Config  │ │ JWT 签发   │ │ 分片上传 & 秒传     │
│ :8848     │ │ 用户管理   │ │ 在线预览            │
│           │ │ QQ OAuth   │ │ 分享管理            │
└───────────┘ └─────┬──────┘ └────────┬──────────┘
                    │                 │
             ┌──────▼─────────────────▼──────┐
             │          MySQL 8.0             │
             │       share_system 数据库        │
             └────────────────────────────────┘
```

| 服务 | 端口 | 技术栈 | 职责 |
|------|------|--------|------|
| **share-gateway** | 8180 | Spring Cloud Gateway | API 网关、JWT 鉴权、CORS、静态资源 |
| **share-user-service** | 8181 | Spring Boot + MyBatis-Plus | 注册登录、JWT 签发、用户管理 |
| **share-file-service** | 8182 | Spring Boot + MyBatis-Plus | 文件管理、分片上传、预览、分享 |
| **share-common** | - | 公共 JAR | 实体类、DTO、工具类（JWT / MD5） |

---

## 🚀 快速开始

### 环境要求

- **JDK** 21+
- **Maven** 3.8+
- **MySQL** 8.0+
- **Nacos** 2.x（可选，已默认直连模式）

### 1. 克隆仓库

```bash
git clone https://github.com/Seredipited/ShareSystem.git
cd ShareSystem
```

### 2. 初始化数据库

**方式一：使用脚本（推荐）**

```bash
# Windows 双击运行
init-db.bat
```

**方式二：手动执行**

```bash
mysql -u root -p < sql/init.sql
```

> 默认数据库连接：`root / 112233`，如需修改请编辑各服务 `application.yml` 中的 `spring.datasource` 配置。

### 3. 编译项目

```bash
mvn clean package -DskipTests
```

### 4. 启动服务

**方式一：使用启动脚本**

```bash
# Windows 双击运行
_launch.bat
```

**方式二：手动启动**

```bash
# 按顺序启动（每个在新终端窗口中）
java -jar share-user-service/target/share-user-service-1.0.0.jar
java -jar share-file-service/target/share-file-service-1.0.0.jar
java -jar share-gateway/target/share-gateway-1.0.0.jar
```

### 5. 访问系统

| 入口 | 地址 |
|------|------|
| 🏠 **用户首页** | http://localhost:8180 |
| 🔧 **管理后台** | http://localhost:8180/pages/admin.html |

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 普通用户 | `test` | `test123` |

---

## 📦 项目结构

```
ShareSystem/
├── pom.xml                          # 父 POM（依赖管理 & 模块聚合）
├── _launch.bat                      # 一键启动脚本
├── init-db.bat                      # 数据库初始化脚本
├── JarPatcher.java                  # Gateway JAR 修补工具
├── gateway-lib/                     # JDK 21 JAXB 兼容依赖
│   ├── jakarta.activation-1.2.2.jar
│   ├── jakarta.xml.bind-api-2.3.3.jar
│   └── jaxb-runtime-2.3.9.jar
├── sql/
│   └── init.sql                     # 数据库建表 & 种子数据
│
├── share-common/                    # 公共模块
│   └── src/main/java/.../common/
│       ├── entity/                  # 实体类：User, FileItem, Share...
│       ├── dto/                     # DTO：Result, LoginDTO, R...
│       └── util/                    # 工具类：JwtUtil, MD5Util, FileUtil...
│
├── share-gateway/                   # API 网关（8180）
│   └── src/main/
│       ├── java/.../gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/CorsConfig.java
│       │   └── filter/AuthGlobalFilter.java   # JWT 全局鉴权
│       └── resources/
│           ├── application.yml               # 路由 & Nacos 配置
│           └── static/                        # 🎨 前端静态资源
│               ├── index.html
│               ├── css/style.css
│               ├── js/{api,main,admin}.js
│               └── pages/{login,register,main,preview,share,admin}.html
│
├── share-user-service/              # 用户服务（8181）
│   └── src/main/
│       ├── java/.../user/
│       │   ├── controller/UserController.java
│       │   ├── controller/AdminUserController.java
│       │   ├── service/impl/UserServiceImpl.java
│       │   └── mapper/{User,UserOauth}Mapper.java
│       └── resources/application.yml
│
├── share-file-service/              # 文件服务（8182）
│   └── src/main/
│       ├── java/.../file/
│       │   ├── controller/{File,Share,AdminFile}Controller.java
│       │   ├── service/impl/{File,Share}ServiceImpl.java
│       │   ├── feign/UserFeignClient.java      # 跨服务调用
│       │   └── mapper/{FileItem,FileChunk,Share,OperationLog}Mapper.java
│       └── resources/application.yml
│
└── src/                             # 旧版单体 SSM 架构代码（参考）
    └── main/
        ├── java/.../  # Spring MVC Controller / Service / Mapper
        └── webapp/    # JSP 前端页面
```

---

## 🔑 核心设计

### JWT 认证流程

```
Client                          Gateway                       Microservice
  │                                │                               │
  │  POST /api/user/login          │                               │
  │ ──────────────────────────────►│                               │
  │                                │  Forward                      │
  │                                │ ─────────────────────────────►│
  │                                │                               │ Validate
  │                                │         { token: "..." }      │
  │                                │ ◄─────────────────────────────│
  │        { token: "..." }        │                               │
  │ ◄──────────────────────────────│                               │
  │                                │                               │
  │  GET /api/file/list            │                               │
  │  Authorization: Bearer <token> │                               │
  │ ──────────────────────────────►│                               │
  │                                │  AuthGlobalFilter             │
  │                                │  ┌─ 白名单? → 放行             │
  │                                │  ├─ Token 有效? → X-User-Token │
  │                                │  └─ 无效? → 401               │
  │                                │  Forward + X-User-Token       │
  │                                │ ─────────────────────────────►│
  │                                │                               │ Parse Token
  │                                │         { data: [...] }       │
  │                                │ ◄─────────────────────────────│
  │        { data: [...] }         │                               │
  │ ◄──────────────────────────────│                               │
```

### 分片上传 & 秒传

```
Client                                              File Service
  │                                                       │
  │  1. 计算文件 MD5                                        │
  │  2. POST /api/file/checkInstant { md5 }               │
  │ ─────────────────────────────────────────────────────►│
  │                      秒传成功 ← MD5 已存在               │
  │ ◄─────────────────────────────────────────────────────│
  │                                                       │
  │  (MD5 不存在 → 分片上传)                                 │
  │  3. GET /api/file/chunkProgress?md5=xxx               │
  │ ─────────────────────────────────────────────────────►│
  │              { uploaded: [0,1,2] }  ← 已上传分片索引      │
  │ ◄─────────────────────────────────────────────────────│
  │                                                       │
  │  4. POST /api/file/chunk (逐片上传，可并发)               │
  │     { md5, chunkIndex, totalChunks, file }            │
  │ ─────────────────────────────────────────────────────►│
  │                                                       │
  │  5. POST /api/file/merge { md5, fileName, parentId }  │
  │ ─────────────────────────────────────────────────────►│
  │              合并完成，文件可用                            │
  │ ◄─────────────────────────────────────────────────────│
```

### API 接口一览

#### 🔓 公开接口（无需 Token）

| Method | Path | 描述 |
|--------|------|------|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录 |
| GET | `/api/share/info/{code}` | 获取分享信息 |
| POST | `/api/share/verify` | 验证提取码 |
| GET | `/api/file/download/{id}` | 文件下载 |
| GET | `/api/file/preview/{id}` | 文件预览 |

#### 🔒 需认证接口（Bearer Token）

| Method | Path | 描述 |
|--------|------|------|
| GET | `/api/user/current` | 获取当前用户信息 |
| GET | `/api/file/list` | 文件列表 |
| POST | `/api/file/upload` | 文件上传 |
| POST | `/api/file/chunk` | 分片上传 |
| POST | `/api/file/merge` | 合并分片 |
| POST | `/api/file/mkdir` | 新建文件夹 |
| PUT | `/api/file/rename` | 重命名 |
| DELETE | `/api/file/delete` | 删除（移入回收站） |
| POST | `/api/share/create` | 创建分享 |
| DELETE | `/api/share/cancel` | 取消分享 |
| GET | `/api/admin/users` | 管理员 - 用户列表 |
| GET | `/api/admin/files` | 管理员 - 文件列表 |

> 统一响应格式：`{ "code": 200, "message": "操作成功", "data": { ... } }`

---

## 🗄️ 数据库设计

<details>
<summary>点击展开 ER 图（6 张表）</summary>

```
┌──────────────┐     ┌──────────────┐
│     user     │     │  user_oauth  │
│──────────────│     │──────────────│
│ id (PK)      │◄────│ user_id (FK) │
│ username     │     │ platform     │
│ password     │     │ open_id      │
│ email        │     │ access_token │
│ nickname     │     └──────────────┘
│ role         │
│ storage_used │     ┌──────────────┐
│ storage_max  │     │  file_item   │
│ status       │     │──────────────│
└──────────────┘     │ id (PK)      │
                     │ user_id (FK) │
┌──────────────┐     │ parent_id    │
│   share      │     │ file_name    │
│──────────────│     │ file_path    │
│ id (PK)      │     │ file_size    │
│ file_id (FK) │────►│ file_md5     │
│ user_id (FK) │     │ mime_type    │
│ share_code   │     │ is_folder    │
│ share_pwd    │     │ is_deleted   │
│ expire_time  │     └──────────────┘
│ view_count   │
│ download_cnt │     ┌──────────────┐     ┌────────────────┐
└──────────────┘     │  file_chunk  │     │ operation_log  │
                     │──────────────│     │────────────────│
                     │ id (PK)      │     │ id (PK)        │
                     │ file_md5     │     │ user_id        │
                     │ chunk_index  │     │ operation      │
                     │ chunk_path   │     │ target         │
                     │ user_id      │     │ create_time    │
                     └──────────────┘     └────────────────┘
```
</details>

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 2.7.18 |
| 微服务 | Spring Cloud | 2021.0.9 |
| 注册 / 配置 | Nacos | 2.x（可选） |
| API 网关 | Spring Cloud Gateway | 3.1.9 |
| 服务调用 | Spring Cloud OpenFeign | 3.1.9 |
| ORM | MyBatis-Plus | 3.5.5 |
| 连接池 | Druid | 1.2.20 |
| 数据库 | MySQL | 8.0.33 |
| 认证 | JWT（自实现 HMAC-SHA256） | - |
| 文档处理 | Apache POI | 5.2.5 |
| JSON | FastJSON | 1.2.83 |
| 构建 | Maven | 3.8+ |
| 语言 | Java | 21 |

---

## 🔧 JDK 21 兼容性说明

本项目使用 **JDK 21** 开发，针对以下兼容性问题做了适配：

- **JWT 工具类**：`share-common` 中的 `JwtUtil` 为自实现（HMAC-SHA256），不依赖 `io.jsonwebtoken:jjwt:0.9.1`（该版本依赖 JDK 9+ 已移除的 `javax.xml.bind`）
- **JAXB 依赖**：`gateway-lib/` 目录提供了 Gateway 模块所需的 JAXB 运行时依赖，使用 `JarPatcher.java` 可将它们注入到 Gateway 可执行 JAR 中

```bash
# 使用 JarPatcher 修补 Gateway JAR
javac JarPatcher.java
java JarPatcher share-gateway-1.0.0.jar share-common-1.0.0.jar share-gateway-patched.jar
```

---

## 📝 常见问题

<details>
<summary><b>启动报错 "Communications link failure"？</b></summary>
请确保 MySQL 服务已启动，并检查各服务 <code>application.yml</code> 中的数据库连接配置是否正确。
</details>

<details>
<summary><b>端口被占用？</b></summary>
默认端口：Gateway=8180, User=8181, File=8182。可在各服务的 <code>application.yml</code> 中修改 <code>server.port</code>。
</details>

<details>
<summary><b>Nacos 连接失败？</b></summary>
项目默认已禁用 Nacos（<code>enabled: false</code>），使用直连模式。如需启用，将 <code>enabled</code> 改为 <code>true</code> 并配置 <code>server-addr</code>。
</details>

<details>
<summary><b>分片上传文件保存在哪里？</b></summary>
默认路径：<code>D:\IDEA\ssm\ShareSystem\storage\files</code>，可在 <code>share-file-service/application.yml</code> 的 <code>file.upload.base-path</code> 中修改。
</details>

<details>
<summary><b>如何修改 JWT 密钥？</b></summary>
编辑 <code>share-common/src/main/java/.../util/JwtUtil.java</code> 中的 <code>SECRET</code> 常量，修改后需要重新编译 common 模块并重新打包各服务。
</details>

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feat/amazing-feature`
3. 提交更改：`git commit -m 'feat: add amazing feature'`
4. 推送到分支：`git push origin feat/amazing-feature`
5. 发起 Pull Request

---

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。

---

<div align="center">

**Made with ❤️ by [Seredipited](https://github.com/Seredipited)**

如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！

</div>
