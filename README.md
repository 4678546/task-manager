# 校园任务管理系统后端接口与用途说明

本文档根据当前 Spring Boot 后端代码、`资料/task_manager.sql` 建表脚本和 `资料` 目录中的 Postman 测试集合整理。

## 项目用途

`task-manager` 是一个面向校园社团或学习小组场景的任务管理系统后端。用户注册登录后，可以创建自己的任务，查看任务列表，查询任务详情，修改任务内容或状态，删除任务，并按任务状态、截止时间等条件分页查询。

项目主要实现以下能力：

- 用户注册、登录、退出登录、获取用户信息、修改密码
- JWT 登录认证与 Redis token 有效性校验
- 个人任务新增、分页查询、详情查询、更新、删除
- 按任务状态和截止时间筛选任务列表
- 文件上传与上传文件静态访问
- MySQL 持久化、MyBatis 数据访问、PageHelper 分页
- 参数校验、统一响应格式、全局异常处理

系统中的任务归属于用户。除注册、登录和访问上传文件外，其他接口都需要在请求头中携带 JWT token。

## 功能完成概览

| 模块         | 当前状态 | 说明                                                                    |
| ------------ | -------- | ----------------------------------------------------------------------- |
| 项目结构     | 已完成   | 包含 Controller、Service、Mapper、Entity、Config、Utils、Exception 分层 |
| 用户注册     | 已完成   | 支持用户名重复校验、用户名和密码格式校验、密码 BCrypt 加密存储          |
| 用户登录     | 已完成   | 校验用户名和密码，登录成功返回 JWT                                      |
| 登录态管理   | 已完成   | token 写入 Redis，并设置 1 小时过期时间                                 |
| 用户信息     | 已完成   | 查询当前登录用户信息，响应中不返回密码字段                              |
| 修改密码     | 已完成   | 校验旧密码、新密码一致性，修改成功后删除当前 token                      |
| 退出登录     | 已完成   | 删除 Redis 中当前 token                                                 |
| 新增任务     | 已完成   | 登录用户可新增自己的任务                                                |
| 分页查询任务 | 已完成   | 使用 PageHelper 分页，支持状态和截止时间条件                            |
| 查询任务详情 | 已完成   | `GET /task/search?id={id}`，并校验任务归属                              |
| 更新任务     | 已完成   | 支持按任务 ID 更新标题、内容、状态、截止时间                            |
| 删除任务     | 已完成   | 支持按任务 ID 删除任务，并校验任务归属                                  |
| 文件上传     | 已完成   | 支持 multipart 文件上传，最大 1MB                                       |
| 接口测试集合 | 已提供   | `资料` 目录包含登录、任务、文件上传 Postman collection                  |
| 数据库脚本   | 已提供   | `资料/task_manager.sql` 包含建库、建表和初始化用户                      |

## 技术栈

- Java 17
- Spring Boot 3.5.14
- Spring Web
- Spring Security
- Spring Validation
- MyBatis
- MySQL
- Redis
- JWT: `java-jwt`
- PageHelper
- Lombok

## 运行依赖

默认配置位于 `src/main/resources/application.yml`：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 1MB
      max-request-size: 1MB
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/task-manager
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379

mybatis:
  configuration:
    map-underscore-to-camel-case: true
    mapper-locations: classpath:com/sakura/taskmanager/mapper/*.xml

file:
  upload:
    path: uploads
```

启动前需要准备：

- MySQL 数据库服务
- Redis 服务：`localhost:6379`
- 数据库和数据表：执行 `资料/task_manager.sql`

默认服务地址：

```text
http://localhost:8080
```

## 启动步骤

1. 启动 MySQL，并确认账号密码与 `application.yml` 一致。
2. 执行数据库脚本：

```sql
source 资料/task_manager.sql;
```

也可以在数据库客户端中直接运行 `资料/task_manager.sql`。

3. 启动 Redis：

```text
localhost:6379
```

4. 编译项目：

```bash
mvn -DskipTests compile
```

5. 启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

6. 使用 Postman 导入 `资料` 目录中的测试集合。先调用登录接口获取 token，再访问需要登录的接口。

## 测试账号

`资料/task_manager.sql` 会初始化一个用户：

| username | password | 说明                 |
| -------- | -------- | -------------------- |
| `sakura` | `123456` | Postman 登录示例账号 |

也可以通过注册接口创建新用户。Postman 注册示例：

| username | password |
| -------- | -------- |
| `22222`  | `123456` |

## Redis 使用说明

项目中 Redis 主要用于登录态管理：

- 登录成功后，将 JWT token 作为 key 写入 Redis，value 也是 token。
- token 在 Redis 中的过期时间为 1 小时。
- JWT 自身过期时间也是 1 小时。
- 认证过滤器会先解析 `Authorization: Bearer <token>`，再检查 Redis 中是否存在该 token。
- 退出登录时删除当前 token。
- 修改密码成功后删除当前 token，用户需要重新登录。
- 登录时还会写入一个 `username` key，`GET /user/info` 当前通过该 key 查询用户信息。

当前实现没有使用 Redis 缓存任务详情或用户信息数据，也没有实现 refresh token、限流、分布式锁等能力。

## 接口总览

| 模块 | 接口用途     | 请求方法 | 路径                  |
| ---- | ------------ | -------- | --------------------- |
| 用户 | 注册         | POST     | `/user/register`      |
| 用户 | 登录         | POST     | `/user/login`         |
| 用户 | 获取用户信息 | GET      | `/user/info`          |
| 用户 | 修改密码     | PATCH    | `/user/updatePwd`     |
| 用户 | 退出登录     | POST     | `/user/logout`        |
| 任务 | 新增任务     | POST     | `/task`               |
| 任务 | 分页查询任务 | GET      | `/task`               |
| 任务 | 查询任务详情 | GET      | `/task/info?id={id}`  |
| 任务 | 更新任务     | PUT      | `/task`               |
| 任务 | 删除任务     | DELETE   | `/task?id={id}`       |
| 文件 | 上传文件     | POST     | `/upload`             |
| 文件 | 访问上传文件 | GET      | `/uploads/{filename}` |

## 数据库表

### user 用户表

| 字段        | 类型         | 说明                |
| ----------- | ------------ | ------------------- |
| id          | int unsigned | 用户主键，自增      |
| username    | varchar(16)  | 用户名              |
| password    | varchar(100) | BCrypt 加密后的密码 |
| create_time | datetime     | 创建时间            |
| update_time | datetime     | 更新时间            |

建表脚本中包含初始化用户 `sakura`。

### task 任务表

| 字段        | 类型         | 说明                            |
| ----------- | ------------ | ------------------------------- |
| id          | int unsigned | 任务主键，自增                  |
| title       | varchar(20)  | 任务标题                        |
| content     | varchar(100) | 任务内容                        |
| status      | varchar(5)   | 任务状态，数据库默认值为 `TODO` |
| deadline    | datetime     | 截止时间                        |
| user_id     | int unsigned | 所属用户 ID                     |
| create_time | datetime     | 创建时间                        |
| update_time | datetime     | 更新时间                        |

`task.user_id` 外键关联 `user.id`，用户更新或删除时会级联影响任务数据。

## 统一响应格式

业务接口统一返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

字段说明：

| 字段    | 类型   | 说明                                   |
| ------- | ------ | -------------------------------------- |
| code    | number | 业务状态码，`0` 表示成功，`1` 表示失败 |
| message | string | 响应提示                               |
| data    | any    | 响应数据                               |

失败示例：

```json
{
  "code": 1,
  "message": "密码错误",
  "data": null
}
```

认证失败由 Spring Security 过滤器直接返回 HTTP 401：

```json
{
  "error": "Missing authentication token"
}
```

或：

```json
{
  "error": "Invalid or expired token"
}
```

## 鉴权说明

放行接口：

- `POST /user/register`
- `POST /user/login`
- `GET /uploads/**`

其他接口都需要携带 JWT token：

```http
Authorization: Bearer <token>
```

登录成功后，后端会生成 JWT，payload 中包含 `id` 和 `username`，并将 token 存入 Redis。退出登录或修改密码后，当前 token 会从 Redis 删除，后续请求需要重新登录。

## 用户接口

### 注册

用于创建新用户。

```http
POST /user/register
Content-Type: application/x-www-form-urlencoded
```

请求参数：

| 参数     | 类型   | 必填 | 说明                           |
| -------- | ------ | ---- | ------------------------------ |
| username | string | 是   | 用户名，必须是 5-16 位非空字符 |
| password | string | 是   | 密码，必须是 5-16 位非空字符   |

请求示例：

```text
username=22222&password=123456
```

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

失败场景：

- 用户名或密码格式不符合规则
- 用户已存在

### 登录

用于校验用户名和密码，并返回 JWT token。

```http
POST /user/login
Content-Type: application/x-www-form-urlencoded
```

请求参数：

| 参数     | 类型   | 必填 | 说明                           |
| -------- | ------ | ---- | ------------------------------ |
| username | string | 是   | 用户名，必须是 5-16 位非空字符 |
| password | string | 是   | 密码，必须是 5-16 位非空字符   |

请求示例：

```text
username=sakura&password=123456
```

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "JWT_TOKEN"
}
```

失败场景：

- 用户不存在
- 密码错误
- 用户名或密码格式不符合规则

### 获取用户信息

用于获取当前登录用户信息。

```http
GET /user/info
Authorization: Bearer <token>
```

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "sakura",
    "createTime": "2026-06-15 22:25:34",
    "updateTime": "2026-06-15 22:25:34"
  }
}
```

说明：

- `password` 字段使用 `@JsonIgnore`，不会返回给前端。
- 当前实现通过 Redis 中的 `username` key 查询用户信息。

### 修改密码

用于当前登录用户修改密码。修改成功后，当前 token 会失效，需要重新登录。

```http
PATCH /user/updatePwd
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "old_pwd": "123456",
  "new_pwd": "1234567",
  "re_pwd": "1234567"
}
```

请求参数：

| 参数    | 类型   | 必填 | 说明       |
| ------- | ------ | ---- | ---------- |
| old_pwd | string | 是   | 旧密码     |
| new_pwd | string | 是   | 新密码     |
| re_pwd  | string | 是   | 确认新密码 |

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

失败场景：

- 参数为空
- 旧密码错误
- 两次密码不一致
- 新密码与旧密码相同

### 退出登录

用于退出当前账号。退出后，当前 token 会从 Redis 删除。

```http
POST /user/logout
Authorization: Bearer <token>
```

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "退出成功!"
}
```

## 任务接口

任务接口都需要登录后访问。后端会根据 token 中的用户 ID 控制任务归属，用户只能查看、更新和删除自己的任务。

### 新增任务

用于给当前登录用户新增任务。

```http
POST /task
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "title": "习概",
  "content": "坚持党的领导",
  "status": "TODO",
  "deadline": "2026-01-01 11:11:11"
}
```

请求参数：

| 参数     | 类型   | 必填 | 说明                                               |
| -------- | ------ | ---- | -------------------------------------------------- |
| title    | string | 是   | 任务标题，必须是 1-10 位非空字符，不能包含空白字符 |
| content  | string | 是   | 任务内容，不能为 null                              |
| status   | string | 否   | 任务状态，例如 `TODO`、`DOING`、`DONE`             |
| deadline | string | 否   | 截止时间，格式 `yyyy-MM-dd HH:mm:ss`               |

说明：

- `userId` 不需要前端传入，后端会从 token 中读取当前用户 ID。
- `createTime` 和 `updateTime` 由后端自动设置。
- 数据库字段 `status` 默认值为 `TODO`，但当前新增 SQL 会写入请求中的 `status` 值；建议前端显式传入任务状态。

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

失败场景：

- 标题为空或格式不符合规则
- 内容为 null
- 未携带 token 或 token 失效

### 分页查询任务

用于分页查询当前登录用户的任务列表，支持按状态和截止时间过滤。

```http
GET /task?pageNum=1&pageSize=3
Authorization: Bearer <token>
```

查询参数：

| 参数     | 类型   | 必填 | 说明                                       |
| -------- | ------ | ---- | ------------------------------------------ |
| pageNum  | number | 是   | 页码，从 1 开始                            |
| pageSize | number | 是   | 每页条数                                   |
| status   | string | 否   | 任务状态，例如 `TODO`、`DOING`、`DONE`     |
| deadline | string | 否   | 截止时间，按数据库中的 `deadline` 精确匹配 |

请求示例：

```http
GET /task?pageNum=1&pageSize=3&status=DONE
Authorization: Bearer <token>
```

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "total": 1,
    "items": [
      {
        "id": 7,
        "title": "习概",
        "content": "坚持党的领导",
        "status": "DONE",
        "userId": 1,
        "deadline": "2026-01-01 11:11:11",
        "createTime": "2026-06-15 10:00:00",
        "updateTime": "2026-06-15 10:00:00"
      }
    ]
  }
}
```

### 查询任务详情

用于根据任务 ID 查询单个任务详情。

```http
GET /task/search?id=7
Authorization: Bearer <token>
```

查询参数：

| 参数 | 类型   | 必填 | 说明    |
| ---- | ------ | ---- | ------- |
| id   | number | 是   | 任务 ID |

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 7,
    "title": "习概",
    "content": "坚持党的领导",
    "status": "DOING",
    "userId": 1,
    "deadline": "2026-01-01 11:11:11",
    "createTime": "2026-06-15 10:00:00",
    "updateTime": "2026-06-15 10:00:00"
  }
}
```

失败场景：

- 任务 ID 为空
- 任务不存在
- 当前登录用户无权查看该任务

### 更新任务

用于更新指定任务。后端只更新请求体中非 null 的字段。

```http
PUT /task
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "id": 7,
  "title": "习概",
  "content": "坚持党的领导",
  "status": "DOING",
  "deadline": "2026-01-01 11:11:11"
}
```

请求参数：

| 参数     | 类型   | 必填 | 说明                                 |
| -------- | ------ | ---- | ------------------------------------ |
| id       | number | 是   | 任务 ID                              |
| title    | string | 否   | 任务标题                             |
| content  | string | 否   | 任务内容                             |
| status   | string | 否   | 任务状态                             |
| deadline | string | 否   | 截止时间，格式 `yyyy-MM-dd HH:mm:ss` |

说明：

- `id` 不能为空。
- 后端会检查任务是否存在，并校验任务是否属于当前登录用户。
- `updateTime` 由后端自动更新。

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

失败场景：

- 任务 ID 为空
- 任务不存在
- 当前登录用户无权更新该任务

### 删除任务

用于删除指定任务。

推荐请求方式：

```http
DELETE /task?id=8
Authorization: Bearer <token>
```

请求参数：

| 参数 | 类型   | 必填 | 说明    |
| ---- | ------ | ---- | ------- |
| id   | number | 是   | 任务 ID |

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

失败场景：

- 任务不存在
- 当前登录用户无权删除该任务

## 文件接口

### 上传文件

用于上传文件到本地 `uploads` 目录。

```http
POST /upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

请求参数：

| 参数 | 类型 | 必填 | 说明               |
| ---- | ---- | ---- | ------------------ |
| file | file | 是   | 上传文件，最大 1MB |

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "/uploads/625c9338-e266-4b64-ad9e-873e6d30b226.jpg"
}
```

失败场景：

- 未选择文件
- 文件超过 1MB
- 文件名不合法
- 文件保存失败

### 访问上传文件

用于通过 URL 访问上传后的文件。

```http
GET /uploads/{filename}
```

示例：

```text
http://localhost:8080/uploads/625c9338-e266-4b64-ad9e-873e6d30b226.jpg
```

说明：

- `/uploads/**` 是公开访问路径，不需要 token。
- 文件实际保存目录由 `file.upload.path` 配置，默认是项目根目录下的 `uploads`。

## Postman 测试集合

项目中的 `资料` 目录包含以下测试资源：

| 文件                                   | 说明                                             |
| -------------------------------------- | ------------------------------------------------ |
| `登录接口测试.postman_collection.json` | 注册、登录、用户信息、退出登录、修改密码相关请求 |
| `任务接口测试.postman_collection.json` | 任务分页查询、更新、删除相关请求                 |
| `文件上传测试.postman_collection.json` | 文件上传请求                                     |
| `task_manager.sql`                     | MySQL 建库建表脚本                               |

### 登录接口测试集合

| 请求名称 | 方法  | Postman URL                            | 对应后端接口            | 说明                                                                        |
| -------- | ----- | -------------------------------------- | ----------------------- | --------------------------------------------------------------------------- |
| 注册     | POST  | `http://localhost:8080/user/register`  | `POST /user/register`   | 可直接使用                                                                  |
| 登录     | POST  | `http://localhost:8080/user/login`     | `POST /user/login`      | 可直接使用                                                                  |
| 任务列表 | GET   | `http://localhost:8080/task/list`      | `GET /task`             | 集合中的 `/task/list` 不是当前后端路径，应改为 `/task?pageNum=1&pageSize=3` |
| 用户信息 | GET   | `http://localhost:8080/user/info`      | `GET /user/info`        | 需要 Bearer token                                                           |
| 登出     | POST  | `http://localhost:8080/user/logout`    | `POST /user/logout`     | 需要 Bearer token                                                           |
| 更新密码 | PATCH | `http://localhost:8080/user/updatePwd` | `PATCH /user/updatePwd` | 需要 Bearer token                                                           |

### 任务接口测试集合

| 请求名称           | 方法   | Postman URL                                       | 对应后端接口        | 说明                                             |
| ------------------ | ------ | ------------------------------------------------- | ------------------- | ------------------------------------------------ |
| 新增任务           | GET    | 未配置 URL                                        | `POST /task`        | 集合中该请求需要改为 POST 并补充 JSON 请求体     |
| 任务列表(条件分页) | GET    | `http://localhost:8080/task?pageNum=1&pageSize=3` | `GET /task`         | 可直接使用，可按需启用 `status`、`deadline` 参数 |
| 更新任务           | PUT    | `http://localhost:8080/task`                      | `PUT /task`         | 可直接使用，需确保任务 ID 存在且属于当前用户     |
| 删除任务           | DELETE | `http://localhost:8080/task`                      | `DELETE /task?id=8` | 建议把 `id` 放到 query 参数中                    |

新增任务的 Postman 请求体示例：

```json
{
  "title": "习概",
  "content": "坚持党的领导",
  "status": "TODO",
  "deadline": "2026-01-01 11:11:11"
}
```

查询任务详情的 Postman 示例：

```http
GET http://localhost:8080/task/info?id=7
Authorization: Bearer <token>
```

### 文件上传测试集合

| 请求名称 | 方法 | Postman URL                    | 对应后端接口   | 说明                                                      |
| -------- | ---- | ------------------------------ | -------------- | --------------------------------------------------------- |
| 文件上传 | POST | `http://localhost:8080/upload` | `POST /upload` | 需要 Bearer token，Body 使用 `form-data`，字段名为 `file` |

## 项目资料对应关系

| 内容             | 当前项目中的文件或位置                                                                                                                |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 项目源码         | 当前仓库根目录                                                                                                                        |
| 数据库建表 SQL   | `资料/task_manager.sql`                                                                                                               |
| 项目说明         | `README.md`、`接口与用途说明.md`                                                                                                      |
| 接口说明         | `接口与用途说明.md`                                                                                                                   |
| Postman 测试集合 | `资料/登录接口测试.postman_collection.json`、`资料/任务接口测试.postman_collection.json`、`资料/文件上传测试.postman_collection.json` |
| 数据库配置       | `src/main/resources/application.yml`                                                                                                  |
| Redis 配置       | `src/main/resources/application.yml`                                                                                                  |
| 文件上传目录配置 | `src/main/resources/application.yml` 中的 `file.upload.path`                                                                          |

## 当前实现注意事项

- 需要重新reload项目
- 除注册、登录、上传文件访问路径外，其他接口都需要 `Authorization: Bearer <token>`。
- token 同时受 JWT 过期时间和 Redis 存储状态控制。
- 修改密码和退出登录都会使当前 token 失效。
- `GET /task/info?id={id}` 已实现，可查询当前用户自己的任务详情。
- 删除任务接口建议使用 query 参数传递 `id`，例如 `DELETE /task?id=8`。
- 任务查询中的 `deadline` 当前是精确匹配，不是按日期范围查询。
- 当前后端没有 `GET /task/list`，分页查询接口是 `GET /task`。
- 当前实现主要用 Redis 管理 token，没有实现任务详情缓存、用户信息缓存、refresh token、接口限流或分布式锁。
- `GET /user/info` 当前通过 Redis 中的全局 `username` key 获取用户信息，多用户同时登录场景下建议改为根据 token 中的用户 ID 查询。

## AI使用情况

使用ai模型为deepseekV4Flash与ChatGPT5.5 High

- 本文主要由ChatGPT5.5生成。
- 测试用html文件由Codex生成。
- 最终提交ChatGPT5.5测试是否还有bug。
- 在SecurutyConfig.java中，适配jwt工具类获取用户信息，并设置用户权限。
- 对于SecurityConfig.java及其继承类的认识、理解。
- WebCofig.java配置中对上传路径的再次优化。
- FileUploadController.java中，创建文件上传的目录与multipart提交模块。
