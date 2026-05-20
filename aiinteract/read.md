# AI Interact 模块说明文档

## 模块职责

`aiinteract` 是聊天应用的 **AI 接口层**，负责用户消息发送和 AI 回复的接收与分发。消息支持文字、语音、视频三种形式。上层 UI 不直接访问网络，只通过本模块的接口完成所有 AI 交互。

---

## 快速开始

### 1. 初始化（Application 或首个 Activity 中调用一次）

```java
AiConfig config = new AiConfig.Builder(
    "https://api.openai.com/v1/chat/completions",  // AI API 地址
    "sk-your-api-key"                                // API 密钥
)
    .model("gpt-4")
    .voiceEnabled(true)
    .videoEnabled(false)
    .aiUserName("AI 助手")
    .aiUserAvatar("R.drawable.ai_avatar")
    .aiUserId("ai_001")
    .mediaDownloadDir(getExternalFilesDir("media").getAbsolutePath())
    .build();

AiInteract.init(config);
```

### 2. 使用预设（推荐）

内置了 **Grok**、**OpenAI**、**Claude** 三个预设，也支持自定义 API：

```java
// 使用 Grok 预设
AiConfig config = AiConfig.fromPreset(ApiPreset.GROK, "xai-your-api-key")
    .aiUserName("Grok")
    .build();
AiInteract.init(config);
```

### 3. 运行时切换预设

```java
// 用户在设置界面切换到 Grok
AiInteract.getInstance().switchToPreset(ApiPreset.GROK, "xai-new-key");
```

### 4. 获取所有可用预设（给 UI 展示列表用）

```java
List<ApiPreset> presets = AiInteract.getAvailablePresets();
// 返回: [GROK, OPENAI, CLAUDE]

for (ApiPreset p : presets) {
    p.getId();            // "grok"
    p.getDisplayName();   // "Grok (xAI)"
    p.getDescription();   // "xAI 开发的 Grok 模型，由 Elon Musk 团队打造..."
    p.getModels();        // ["grok-3", "grok-3-mini"]
    p.getDefaultModel();  // "grok-3"
    p.getEndpoint();      // "https://api.x.ai/v1/chat/completions"
    p.getApiKeyUrl();     // "https://console.x.ai" (获取密钥的链接)
}
```

### 5. 更新配置（手动设置）

```java
AiConfig newConfig = new AiConfig.Builder("https://new-api.example.com/v1/chat", "new-key")
    .model("claude-opus-4-7")
    .build();
AiInteract.getInstance().updateConfig(newConfig);
```

---

## 所有公开接口

### AiInteract — 主入口（单例）

| 方法 | 说明 |
|---|---|
| `AiInteract.init(AiConfig)` | 初始化，必须最先调用 |
| `AiInteract.getInstance()` | 获取单例 |
| `sendTextMessage(String text, AiCallback cb)` | 发送文字消息 |
| `sendTextMessageStreaming(String text, AiCallback cb)` | 发送文字消息（流式返回） |
| `sendVoiceMessage(String filePath, AiCallback cb)` | 发送语音消息 |
| `sendVideoMessage(String filePath, AiCallback cb)` | 发送视频消息 |
| `getConversationHistory()` | 获取当前对话历史 |
| `clearHistory()` | 清空对话历史 |
| `setSystemPrompt(String prompt)` | 设置系统提示词 |
| `updateConfig(AiConfig)` | 运行时更换 API 配置 |
| `switchToPreset(ApiPreset, String apiKey)` | 切换到预设 API 提供商 |
| `getAvailablePresets()` | 获取所有可用预设列表（静态方法） |
| `getConfig()` | 获取当前配置 |
| `shutdown()` | 释放资源 |

### AiConfig — API 配置（Builder 模式）

| 构建参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `apiEndpoint` (必填) | String | - | AI API 地址 |
| `apiKey` (必填) | String | - | API 密钥 |
| `model` | String | `"gpt-4"` | 模型名称 |
| `voiceEnabled` | boolean | `true` | 是否启用语音回复 |
| `videoEnabled` | boolean | `false` | 是否启用视频回复 |
| `maxTokens` | int | `2048` | 最大返回 token 数 |
| `temperature` | float | `0.7` | 回复随机性 |
| `aiUserName` | String | `"AI Assistant"` | AI 显示名称 |
| `aiUserAvatar` | String | `""` | AI 头像资源路径 |
| `aiUserId` | String | `"ai_user"` | AI 用户 ID |
| `mediaDownloadDir` | String | 默认下载目录 | 语音/视频文件保存目录 |

**静态工厂方法：**

```java
// 从预设创建（推荐）
AiConfig.fromPreset(ApiPreset.GROK, "api-key").build();

// 手动指定所有参数
new AiConfig.Builder("https://api.x.ai/v1/chat/completions", "api-key")
    .model("grok-3")
    .build();
```

### ApiPreset — API 预设提供商

| 枚举值 | 名称 | 默认模型 | API 地址 |
|---|---|---|---|
| `GROK` | Grok (xAI) | `grok-3` | `https://api.x.ai/v1/chat/completions` |
| `OPENAI` | OpenAI (ChatGPT) | `gpt-4o` | `https://api.openai.com/v1/chat/completions` |
| `CLAUDE` | Claude (Anthropic) | `claude-opus-4-7` | `https://api.anthropic.com/v1/messages` |
| `CUSTOM` | 自定义 API | (手动输入) | (手动输入) |

每个预设提供的方法：

| 方法 | 说明 |
|---|---|
| `getId()` | 预设唯一标识，如 `"grok"` |
| `getDisplayName()` | 显示名称，如 `"Grok (xAI)"` |
| `getDescription()` | 描述文字 |
| `getEndpoint()` | API 端点地址 |
| `getDefaultModel()` | 默认模型名 |
| `getModels()` | 可用模型列表，如 `["grok-3", "grok-3-mini"]` |
| `getApiKeyUrl()` | 获取 API 密钥的网页地址 |
| `createConfigBuilder(String apiKey)` | 快速创建对应的 AiConfig.Builder |

### AiCallback — UI 接收回调

```java
public interface AiCallback {
    void onResponse(AiMessage message);          // 收到完整 AI 回复
    void onStreamChunk(String chunk);            // 流式文字片段（逐字返回）
    void onStreamComplete();                     // 流式传输完成
    void onError(int code, String message);      // 出错
    void onFileDownloadProgress(int progress);   // 媒体文件下载进度 0-100
}
```

### AiMessageBridge — 消息格式转换

将内部 `AiMessage` 转换为 UI 层可用的 `IMessage`（适配现有 MsgListAdapter）：

```java
// 收到的 AI 消息
IMessage msg = AiMessageBridge.forReceived(aiMessage, aiUser);

// 发出的用户消息
IMessage msg = AiMessageBridge.forSending(aiMessage, user);
```

---

## 调用流程

### 发送文字消息

```
UI 层                              aiinteract 层                     AI API
│                                       │                              │
│  sendTextMessage("你好", callback)     │                              │
│ ──────────────────────────────────►   │                              │
│                                       │  POST /v1/chat/completions   │
│                                       │ ────────────────────────────►│
│                                       │                              │
│                                       │     JSON response            │
│                                       │ ◄────────────────────────────│
│                                       │                              │
│  callback.onResponse(aiMessage)       │                              │
│ ◄──────────────────────────────────   │                              │
│                                       │                              │
│  AiMessageBridge.forReceived()        │                              │
│  转换为 IMessage → 更新 RecyclerView   │                              │
```

### 发送语音/视频消息（流程同上，多了文件下载步骤）

```
callback.onFileDownloadProgress(50)
    → callback.onFileDownloadProgress(100)
    → callback.onResponse(aiMessage)   // localMediaPath 已填充
```

### 流式消息（打字机效果）

```
sendTextMessageStreaming("你好", cb)
    → cb.onStreamChunk("你")
    → cb.onStreamChunk("好")
    → cb.onStreamChunk("！")
    → cb.onStreamComplete()
    → cb.onResponse(完整消息)
```

---

## UI 层集成示例

```java
// ====== 在 MessageListActivity 中 ======

// 1. 用户点击发送按钮
@Override
public boolean onSendTextMessage(CharSequence input) {
    // 先显示用户发出的消息
    MyMessage userMsg = new MyMessage(input.toString(), IMessage.MessageType.SEND_TEXT.ordinal());
    userMsg.setUserInfo(currentUser);
    userMsg.setTimeString(getCurrentTime());
    userMsg.setMessageStatus(IMessage.MessageStatus.SEND_GOING);
    mAdapter.addToStart(userMsg, true);

    // 通过 aiinteract 发送
    AiInteract.getInstance().sendTextMessage(input.toString(), new AiCallback() {
        @Override
        public void onResponse(AiMessage aiMessage) {
            IUser aiUser = new DefaultUser("ai", "AI助手", "R.drawable.ai_avatar");
            IMessage aiMsg = AiMessageBridge.forReceived(aiMessage, aiUser);
            mAdapter.addToStart(aiMsg, true);
            // 更新用户消息状态为发送成功
            userMsg.setMessageStatus(IMessage.MessageStatus.SEND_SUCCEED);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onStreamChunk(String chunk) { /* 仅流式模式用到 */ }

        @Override
        public void onStreamComplete() { /* 仅流式模式用到 */ }

        @Override
        public void onError(int code, String message) {
            Toast.makeText(MessageListActivity.this, "AI 回复失败: " + message, Toast.LENGTH_SHORT).show();
            userMsg.setMessageStatus(IMessage.MessageStatus.SEND_FAILED);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFileDownloadProgress(int progress) {
            // 可显示下载进度条
        }
    });
    return true;
}

// 2. 语音录制完成后发送
@Override
public void onFinishRecord(File voiceFile, int duration) {
    AiInteract.getInstance().sendVoiceMessage(voiceFile.getPath(), callback);
}
```

---

## 自定义 API 适配

本模块默认发送 **OpenAI 兼容格式** 请求。如果使用其他 API 服务，需确保端点兼容以下格式：

**请求格式：**
```json
{
  "model": "gpt-4",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "用户消息"},
    {"role": "assistant", "content": "AI 历史回复"}
  ],
  "max_tokens": 2048,
  "temperature": 0.7,
  "stream": false
}
```

**回复格式（文字）：**
```json
{
  "choices": [{
    "message": { "content": "AI 的文字回复" }
  }]
}
```

**回复格式（语音/视频扩展字段）：**
```json
{
  "choices": [{
    "message": {
      "content": "附带的文字说明",
      "voice_url": "https://example.com/audio.mp3",
      "voice_duration": 5
    }
  }]
}
```

> 额外字段 `voice_url`、`video_url`、`voice_duration`、`video_duration` 放在 message 内部或顶层均可被解析。不兼容的 API 需要在 `AiApiClient.parseResponse()` 中适配。

---

## 依赖

| 依赖 | 用途 |
|---|---|
| `com.squareup.okhttp3:okhttp:4.12.0` | HTTP 网络请求 |
| `com.google.code.gson:gson:2.10.1` | JSON 解析 |
| `:messagelist` (compileOnly) | IMessage 接口（桥接层使用） |

---

## 文件索引

| 文件 | 职责 |
|---|---|
| `AiInteract.java` | 主入口，单例，消息发送、会话管理与预设切换 |
| `AiConfig.java` | API 配置，Builder 模式构建 |
| `ApiPreset.java` | 预设 API 提供商（Grok、OpenAI、Claude） |
| `AiCallback.java` | UI 回调接口定义 |
| `AiMessage.java` | 内部消息模型（文字/语音/视频） |
| `AiMessageType.java` | 消息类型枚举 TEXT / VOICE / VIDEO |
| `AiResponse.java` | AI API 回复解析模型 |
| `AiApiClient.java` | HTTP 客户端，请求/响应处理 |
| `AiFileDownloader.java` | 语音/视频文件下载 |
| `AiMessageBridge.java` | AiMessage → IMessage 格式转换 |
