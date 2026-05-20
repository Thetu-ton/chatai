# Memory Module (记忆模块)

## 概述

Memory 模块是 AI 聊天应用的**记忆中枢**，负责：

- **接收 & 存储** UI 发来的聊天消息（持久化到本地 Room 数据库）
- **长期记忆** 通过 [Mem0](https://mem0.ai) 向量记忆服务，提取并存储用户偏好、事实等持久信息
- **上下文装配** 将近期对话 + 关键词关联的历史消息 + 长期记忆组装为 LLM 上下文
- **记忆搜索** FTS4 全文搜索（本地）+ 向量搜索（Mem0 云端）

### 架构

```
┌─────────────────────────────────────────────────────┐
│                    MemoryManager                     │
│                  (公共 API 单例)                      │
├──────────────┬──────────────────┬───────────────────┤
│  MemoryStore │   Mem0Client     │  MessageConverter │
│  (Room/SQLite)│  (Mem0 REST API) │  (IMessage↔AiMsg) │
│  本地消息存储  │  云端长期记忆      │  格式转换          │
└──────────────┴──────────────────┴───────────────────┘
```

- **MemoryStore** — Room 数据库（SQLite + FTS4），存储所有对话消息，本地全文搜索
- **Mem0Client** — 调用 Mem0 REST API，存储/检索长期记忆（向量语义搜索）
- **MessageConverter** — IMessage ↔ AiMessage 格式转换 + AI 响应的 UI 展示适配

### 依赖

| 库 | 用途 | 许可证 |
|---|---|---|
| AndroidX Room 2.5.2 | 本地消息持久化 + FTS4 搜索 | Apache 2.0 |
| Mem0 (REST API) | 云端长期记忆向量存储 | 开源 / MIT |
| Gson 2.10.1 | JSON 序列化 | Apache 2.0 |
| aiinteract (本地模块) | AiMessage 类型 | — |
| messagelist (compileOnly) | IMessage 接口 | — |

---

## 快速接入

### 1. Gradle 配置

在 `settings.gradle` 中注册模块：

```groovy
include ':memory'
project(':memory').projectDir = new File(rootProject.projectDir, '../memory')
include ':aiinteract'
project(':aiinteract').projectDir = new File(rootProject.projectDir, '../aiinteract')
```

在你的 app 模块 `build.gradle` 中添加依赖：

```groovy
implementation project(':memory')
implementation project(':aiinteract')
```

### 2. 初始化

在 `Application.onCreate()` 或首个 `Activity.onCreate()` 中：

```java
import com.chatai.memory.MemoryManager;
import com.chatai.memory.MemoryConfig;
import com.chatai.aiinteract.AiInteract;
import com.chatai.aiinteract.AiConfig;

// 1. 初始化 Memory 模块
MemoryConfig memoryConfig = new MemoryConfig.Builder()
    .mem0Endpoint("https://your-mem0-server.com")  // Mem0 服务地址
    .mem0ApiKey("your-mem0-api-key")                // Mem0 API Key
    .mem0UserId("user_123")                         // 当前用户 ID
    .maxContextMessages(50)                         // 近期消息窗口大小
    .maxMemoryEntries(5)                            // 每次注入 LLM 的长期记忆条数
    .build();
MemoryManager.init(context, memoryConfig);

// 2. 初始化 AI 模块
AiConfig aiConfig = new AiConfig.Builder(
    "https://api.openai.com/v1/chat/completions",
    "sk-your-api-key"
).build();
AiInteract.init(aiConfig);

// 3. 恢复上次会话
MemoryManager.getInstance().restoreConversationToAiInteract();
```

### 3. 用户发送消息时

```java
// 在 MessageListActivity.onSendTextMessage() 中：

public boolean onSendTextMessage(CharSequence input) {
    String text = input.toString();

    // (a) 创建 UI 消息并显示
    MyMessage message = new MyMessage(text, IMessage.MessageType.SEND_TEXT.ordinal());
    message.setUserInfo(currentUser);
    message.setTimeString(getCurrentTime());
    message.setMessageStatus(IMessage.MessageStatus.SEND_GOING);
    mAdapter.addToStart(message, true);

    // (b) 存入 Memory（持久化）
    MemoryManager.getInstance().onUserMessage(message, currentUser.getId());

    // (c) 同步长期记忆到 Mem0（自动提取关键信息）
    MemoryManager.getInstance().memorize(text);

    // (d) 发送给 AI
    AiInteract.getInstance().sendTextMessage(text, createAiCallback());

    return true;
}
```

### 4. 接收 AI 响应时

```java
private AiCallback createAiCallback() {
    IUser aiUser = new DefaultUser("ai", "AI Assistant", "R.drawable.ai_avatar");

    return new AiCallback() {
        @Override
        public void onResponse(AiMessage aiMessage) {
            // (a) 存入 Memory
            MemoryManager.getInstance().onAiResponse(aiMessage);

            // (b) 转为 UI 消息并显示
            IMessage displayMsg = AiMessageBridge.forReceived(aiMessage, aiUser);
            mAdapter.addToStart(displayMsg, true);
        }

        @Override
        public void onStreamChunk(String chunk) { /* 流式更新 UI */ }

        @Override
        public void onStreamComplete() {}

        @Override
        public void onError(int code, String msg) {
            Toast.makeText(activity, "AI Error: " + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileDownloadProgress(int progress) {}
    };
}
```

### 5. 长期记忆操作

```java
// 手动存储一条长期记忆
MemoryManager.getInstance().remember(
    "user_name",              // key
    "User's name is Honor",   // value (事实描述)
    "User Name",              // summary (简短标签)
    8                         // importance (0-10)
);

// 搜索长期记忆（Mem0 向量搜索，语义匹配）
MemoryManager.getInstance().searchLongTermMemory("What does Honor like?",
    results -> {
        for (MemoryEntry entry : results) {
            Log.d("Memory", entry.getMemoryValue());
        }
    });

// 获取所有长期记忆
MemoryManager.getInstance().getAllMemories(entries -> { ... });

// 删除一条记忆
MemoryManager.getInstance().forget("user_name");
```

---

## 公共 API 参考

### MemoryManager（单例）

#### 初始化

| 方法 | 说明 |
|---|---|
| `MemoryManager.init(Context, MemoryConfig)` | 初始化模块，必须最先调用 |
| `MemoryManager.getInstance()` | 获取单例 |

#### 消息存取

| 方法 | 说明 |
|---|---|
| `onUserMessage(IMessage, String senderId)` | 存储用户发送的消息（持久化到 Room） |
| `onAiResponse(AiMessage)` | 存储 AI 回复的消息（持久化到 Room） |

#### 上下文装配（给 LLM 用）

| 方法 | 说明 |
|---|---|
| `getContextForLLM(String userInput)` | 组装完整上下文：近期消息 + 关键词关联消息 + 长期记忆 system prompt |
| `getRecentContext()` | 仅获取近期消息上下文 |
| `buildSystemPromptWithMemories()` | 构建包含长期记忆的 system prompt |
| `restoreConversationToAiInteract()` | 恢复上次持久化的会话到 AiInteract |

#### 长期记忆（Mem0 云端）

| 方法 | 说明 |
|---|---|
| `remember(key, value, summary, importance)` | 存储一条长期记忆 → Mem0 |
| `memorize(String text)` | 将用户消息自动存入 Mem0（异步） |
| `searchLongTermMemory(String query, Mem0Callback)` | 语义搜索长期记忆 → Mem0 API |
| `getAllMemories(Mem0Callback)` | 获取所有长期记忆 |
| `forget(String key)` | 删除指定记忆 |
| `getMemoryCount()` | 长期记忆总数 |

#### 本地消息搜索（Room FTS4）

| 方法 | 说明 |
|---|---|
| `searchMessages(String query)` | 全文搜索历史消息，返回 `List<AiMessage>` |
| `searchMessagesForDisplay(String query, IUser)` | 全文搜索，返回 `List<IMessage>` 可直接显示 |
| `getMessageCount()` | 已存储的消息总数 |

#### 会话管理

| 方法 | 说明 |
|---|---|
| `clearAll()` | 清空所有数据（本地 + 云端） |
| `shutdown()` | 释放资源 |

---

### MemoryConfig（配置）

```java
new MemoryConfig.Builder()
    .mem0Endpoint("https://your-mem0-server.com")  // Mem0 服务地址
    .mem0ApiKey("your-api-key")                     // Mem0 API Key
    .mem0UserId("user_123")                         // 用户标识
    .maxContextMessages(50)                         // 近期消息窗口 (default: 50)
    .maxSearchResults(10)                           // 关键词搜索结果数 (default: 10)
    .maxMemoryEntries(5)                            // 注入 LLM 的长期记忆数 (default: 5)
    .persistEnabled(true)                           // 是否持久化 (default: true)
    .systemPrompt("You are...")                     // 自定义 system prompt
    .aiUserId("ai_user")                            // AI 用户 ID
    .aiUserName("AI Assistant")                     // AI 显示名称
    .userDisplayName("User")                        // 用户显示名称
    .build();
```

---

### Mem0 服务

本模块依赖一个运行的 [Mem0](https://github.com/mem0ai/mem0) 服务实例。Mem0 负责：

- 向量嵌入存储（使用 embedding 模型将记忆转为向量）
- 语义相似度搜索（找到与当前话题最相关的历史记忆）
- 记忆去重和合并

**部署 Mem0 服务**（自行托管或使用 Mem0 Cloud）：

```bash
# 方式 1: 使用 Mem0 Python 包自建服务
pip install mem0ai
mem0 server start --host 0.0.0.0 --port 8000

# 方式 2: Docker
docker run -p 8000:8000 mem0/mem0-server
```

然后在 `MemoryConfig` 中配置 endpoint 指向该服务。

---

## 数据流图

```
用户输入 "我叫 Honor，喜欢 Python"
        │
        ▼
MessageListActivity.onSendTextMessage()
        │
        ├──► MyMessage → mAdapter.addToStart()       [UI 显示]
        │
        ├──► MemoryManager.onUserMessage()            [Room 持久化]
        │
        ├──► MemoryManager.memorize()                 [Mem0 长期记忆]
        │     └── POST /v1/memories/
        │         { "messages": ["我叫 Honor，喜欢 Python"],
        │           "user_id": "user_123" }
        │
        └──► AiInteract.sendTextMessage()             [发送给 LLM]
                │
                ▼
        AiCallback.onResponse(aiMsg)
                │
                ├──► MemoryManager.onAiResponse()     [Room 持久化]
                │
                └──► AiMessageBridge → mAdapter       [UI 显示]

--- 下次用户问 "我喜欢什么编程语言？" ---

MemoryManager.getContextForLLM("我喜欢什么编程语言？")
        │
        ├──► Room: 获取最近 50 条消息
        ├──► Room FTS4: 搜索匹配 "编程语言" 的历史消息
        └──► Mem0 API: 语义搜索 "我喜欢什么编程语言？"
             └── 返回: "User's name is Honor, likes Python"
                    ↓
              注入到 system prompt → LLM 知道用户叫 Honor、喜欢 Python
```

---

## 项目文件结构

```
memory/
  build.gradle                              — Gradle 构建配置
  proguard-rules.pro                        — 混淆规则
  README.md                                 — 本文件
  INTEGRATION.md                            — 集成指南（给修改其他文件的 agent）
  src/main/
    AndroidManifest.xml
    java/com/chatai/memory/
      MemoryConfig.java                     — 配置类 (Builder 模式)
      MemoryManager.java                    — 核心编排器 (公共 API)
      MemoryStore.java                      — Room 本地存储 (消息持久化)
      MessageConverter.java                 — IMessage ↔ AiMessage 转换
      storage/
        MemoryDatabase.java                 — Room 数据库单例
        StoredMessageEntity.java            — 消息实体
        MessageFts.java                     — FTS4 全文搜索虚拟表
        StoredMessageDao.java               — 消息 DAO
        MemoryEntryEntity.java              — 长期记忆实体
        MemoryEntryDao.java                 — 长期记忆 DAO
      mem0/
        Mem0Client.java                     — Mem0 REST API 客户端
        Mem0Models.java                     — Mem0 请求/响应模型
```
