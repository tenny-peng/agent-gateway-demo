# agent-gateway-demo

练习用 **LLM 网关 / Agent**：Spring Boot 2 + Java 8，对接 **腾讯云混元 OpenAI 兼容 API**（可换其他兼容端点）。  
公开仓库：**[tenny-peng/agent-gateway-demo](https://github.com/tenny-peng/agent-gateway-demo)**（若地址有变以你实际仓库为准）。

---

## 功能概览

| 能力 | 说明 |
|------|------|
| **通用对话** | 无工具，多轮会话 + 流式 SSE；`POST /api/generic/chat`、`/api/generic/chat/stream` |
| **物流业务 Agent** | `query_waybill` 工具循环 + 多轮 + 流式；`POST /api/logistics/agent/chat`、`/chat/stream` |
| **RAG（最小实现）** | classpath Markdown 分块、本地关键词/字元检索、`top-k` 注入 system；落库前剥离检索段 |
| **评测** | `eval/cases.json` + `scripts/eval-run.ps1` 冒烟（需本机已启动应用并配置 Key） |
| **静态页** | `http://localhost:8080/` 简易流式测试（通用 / 物流两个 Tab）；流式结束后用 **marked + DOMPurify** 渲染 Markdown（粗体、列表等） |
| **登录 / 注册** | opaque **UUID** 存 **Redis**（会话），用户与统计在 **MySQL**（**MyBatis-Plus**）；前端 `localStorage` 仅存令牌字符串 |
| **管理后台** | `/admin.html`，接口 `GET /api/admin/stats`（需 `ADMIN` 角色） |

---

## 技术栈与运行

- JDK 8、Spring Boot 2.7、Jackson、`SseEmitter`  
- **MyBatis-Plus** + **MySQL 8**；**Spring Data Redis**（Lettuce），会话键 `agw:auth:session:{token}`  
- 配置：`src/main/resources/application.yml`；混元 Key 建议环境变量 `HUNYUAN_API_KEY`  
- 运行前：执行 **`src/main/resources/schema-mysql.sql`** 建库建表，并启动本机 **Redis**（默认 `127.0.0.1:6379`）

```bash
mvn spring-boot:run
```

浏览器打开 **http://localhost:8080/**；评测见下文「评测脚本」。

---

## 认证与会话（UUID + Redis）

**登录后发 **随机 UUID**，会话 payload 与 TTL 放在 **Redis**；请求头 `Authorization: Bearer <uuid>`；**`POST /api/auth/logout` 删 Redis 键**即可立即使服务端会话失效（多实例共享同一 Redis/集群即可）。

公开接口（无需 Bearer）：`POST /api/auth/register`、`POST /api/auth/login`。其余 `/api/generic/**`、`/api/logistics/**`、`/api/auth/me`、`/api/auth/logout`、`/api/admin/**` 需带 Bearer。可选环境变量：`MYSQL_USER`、`MYSQL_PASSWORD`、`REDIS_*`、`ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD`（若设置且库中无同名用户则插入管理员）。

> 默认**不会**自动创建管理员账号：只有在启动时同时提供 `ADMIN_BOOTSTRAP_USERNAME` 与 `ADMIN_BOOTSTRAP_PASSWORD`，且数据库中不存在同名用户时，才会插入首个管理员。

---

## HTTP 路径（当前约定）

- **通用**：`/api/generic/chat`、`/api/generic/chat/stream`（需登录）  
- **物流 Agent**：`/api/logistics/agent/chat`、`/api/logistics/agent/chat/stream`（需登录）  
- **认证**：`/api/auth/register`、`/api/auth/login`、`/api/auth/me`、`/api/auth/logout`  
- **管理**：`/api/admin/stats`（`ADMIN`）  
- 全局：`IllegalStateException` → 400；`UnauthorizedException` → 401；`ForbiddenException` → 403（`ApiExceptionHandler`）

---

## 目录结构（按职责分包）

```
org.tenny
├── admin/               # 管理端 Controller / DTO
├── auth/                # 登录注册、Redis 会话、拦截器、实体与 Mapper（MyBatis-Plus）
├── client/              # LLM HTTP / SSE 客户端
├── common/session/      # 内存多轮会话（chat / logistics 两套 map）
├── config/              # llm、agent、rag、MyBatis、安全 TTL 等
├── dto/                 # ChatRequest 等
├── generic/             # 通用问答：service + web
├── logistics/           # 物流 Agent：service + web + tool（运单演示）
├── rag/                 # RAG 加载与注入
└── web/                 # 全局异常处理
```

语料示例：`src/main/resources/rag/*.md`。

---

## 评测脚本

1. 启动应用并配置 `HUNYUAN_API_KEY`  
2. 项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/eval-run.ps1
```

若已启用登录保护，先调用 `/api/auth/login` 取得响应里的 **`token`**，再带上 `-ApiToken`：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/eval-run.ps1 -ApiToken "<token>"
```

用例定义在 **`eval/cases.json`**；脚本正文为 **ASCII**，避免 Windows PowerShell 5.1 在 UTF-8 无 BOM 下解析中文脚本报错。

---

## 学习与踩坑记录

> 详细实现以代码为准；此处只记 **问题是什么、大致怎么解**。

### 1. 流式输出：从「整段再等」到「逐字/逐段」

- **现象**：若用默认 `RestTemplate` 读响应体，往往会 **等服务器关闭连接才把 body 一次读完**，前端只能最后一下显示整段。  
- **思路**：对 **OpenAI 兼容的 `stream=true` SSE**，用 **`HttpURLConnection`（或同类流式 API）按行读 `data:`**，每读到一块 **`delta.content`** 就回调/写入 `SseEmitter`，实现边下边推。  
- **前端**：`EventSource` 不便带 POST body，故用 **`fetch` + `ReadableStream`** 解析 SSE；注意 **空行分隔事件**、同一事件内多行 `data:` 应用换行拼接，否则 Markdown 表格等会挤成一行。

### 2. Agent 流式 + Tool（`stream=true` + `tools`）

- **难点**：流式里 **`delta.tool_calls` 是分片到的**，`function.arguments` 可能多包才拼成合法 JSON。  
- **思路**：按 **`index` 合并** 各段，凑齐后再 `executeTool`，与非流式 `tool_calls` 一样接 **多轮 messages**；正文仍走 `delta.content` 回调。

### 3. RAG（检索增强）

- **思路**：启动时读 md → **分段** → 用当前 **用户本轮问题** 做简单打分取 **top-k** → 拼进 **第一条 system**（与「人设」区分可用固定分隔符）；**持久化会话前去掉检索段**，避免多轮把旧检索越堆越长；每轮请求 **重新检索**。

### 4. Tool 调用（与非流式 Agent 循环）

- **思路**：请求里带 **`tools` + `tool_choice`**；模型返回 **`assistant` + `tool_calls`** 时落盘到 messages，执行工具得到 **`role=tool`** 消息，再调模型直到出现最终 **`content`**。与「联网搜索」「查 TMS」同一类 **「模型决策 + 服务端执行」** 模式。

### 5. 通用 vs 物流：后端分包与路由

- **思路**：**`generic`** 与 **`logistics`** 分包、分 Controller 路径，共用 `ConversationStore` 里不同 map；工具定义与 `WaybillQueryTool` 放在 **logistics**，避免 `LlmClient` 依赖业务。

### 6. 评测脚本与编码（Windows）

- **现象**：`eval-run.ps1` 内写中文时，在 **UTF-8 无 BOM** 下可能被 PowerShell 5.1 **按系统编码误读**，引号断裂报「表达式包含意外标记」。  
- **思路**：脚本里 **错误提示等用英文**，`cases.json` 仍用 UTF-8 由 `Get-Content -Encoding UTF8` 读取。

### 7. Markdown 在网页上如何显示（粗体、标题等）

- **原因**：后端返回的是 **纯文本**；模型常用 Markdown 排版（如 `**粗体**`）。若前端只用 `textContent` 拼接，浏览器会 **原样显示 `**`**，不是 bug。  
- **做法**（本仓库 `static/index.html`）：流式过程中仍用纯文本展示「正在输出」；**整段接收完毕后** 用 **[marked](https://github.com/markedjs/marked)** 转成 HTML，再用 **[DOMPurify](https://github.com/cure53/DOMPurify)** 做 **XSS 消毒** 后写入 `innerHTML`。CDN 加载失败时回退为纯文本。  
- **注意**：边下边渲染 Markdown 会遇到「半个 `**`」等不完整语法，故采用 **结束后一次性渲染**；若以后要「打字机 + 实时排版」，需更复杂的增量解析或仅对闭合块渲染。

### 8. 其它（可选扩展）

- **Embedding / 向量库**：检索从「字匹配」升级为语义相似度时再引入；不必与首版 RAG 绑死。  
- **Skill / 用户说明书**：本质是 **按用户或租户拼 system**（上传 md、在线编辑），与 RAG「按问题检索」可并存，注意顺序与长度。  
- **联网**：要么 **先搜再注入上下文**，要么 **`web_search` 型 Tool** 走与 `query_waybill` 相同的多轮协议；结果都要 **top-k + 截断 + 总 token 预算**，避免整页 HTML 进模型。

---

## License

练习项目；使用第三方 API 时请遵守对应服务商条款与计费说明。
