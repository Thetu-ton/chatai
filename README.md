# ChatAI - AI Chat App

一款支持文字、语音、视频三种消息形式的 Android AI 聊天应用。使用 Grok / OpenAI / Claude API，可自由切换 AI 提供商。

## 下载 APK

### 方法一：直接下载（推荐）

点击下载 → [**chatai-app.apk**](chatai-app.apk) ← 右键另存到手机安装

> 需要 Android 4.4+ | 8.5MB | Debug 版本

### 方法二：自己构建

```bash
cd Android
set JAVA_HOME=C:\path\to\jdk17
set ANDROID_HOME=C:\path\to\android-sdk
gradlew :exampleui:assembleDebug
```

构建后 APK 路径：`Android/sample/exampleui/build/outputs/apk/debug/exampleui-debug.apk`

---

## 快速使用

1. 安装 APK → 打开 App → 进入聊天界面
2. 点击右上角 **齿轮图标** 进入设置
3. 选择 AI 提供商：
   - **Grok (xAI)** — `https://api.x.ai/v1/chat/completions`
   - **OpenAI** — `https://api.openai.com/v1/chat/completions`
   - **Claude** — `https://api.anthropic.com/v1/messages`
   - **自定义** — 任意兼容接口
4. 输入 **API Key** → 点击 **Save & Connect**
5. 返回聊天界面，发送消息即可与 AI 对话

---

## 项目结构

```
chatai/
├── Android/              ← Android 工程（UI + 输入组件）
│   ├── sample/exampleui/ ← 主应用 (聊天界面、设置界面)
│   ├── chatinput/        ← 输入组件库 (文字、语音、拍照、相册)
│   └── messagelist/      ← 消息列表库 (头像、气泡、播放)
├── aiinteract/           ← AI 接口层
│   └── src/.../ApiPreset.java  ← Grok/OpenAI/Claude 预设
│   └── src/.../AiInteract.java ← 主入口 (发送、接收、会话管理)
│   └── src/.../AiApiClient.java← HTTP 客户端
│   └── read.md            ← 模块文档 & 调用示例
├── memory/               ← 记忆模块 (Room 数据库，历史消息存储)
├── chatai-app.apk        ← 可安装的 APK 文件
└── plan.md               ← 构建状态 & 变更记录
```

## 模块功能

| 模块 | 功能 | 技术栈 |
|---|---|---|
| `aiinteract` | AI 接口：消息发送、回复接收、预设切换 | HttpURLConnection, Gson, ExecutorService |
| `memory` | 对话记忆：历史存储、全文搜索、长期记忆 | Room, SQLite |
| `chatinput` | 输入组件：文字输入、录音、拍照、相册 | AndroidX, RecyclerView |
| `messagelist` | 消息列表：文字气泡、语音播放、视频播放 | RecyclerView, Glide, MediaPlayer |
| `exampleui` | 主界面：聊天页 + 设置页 | Activity, Adapter, MultiDex |

## 支持的 API

| 提供商 | 预设 | 默认模型 |
|---|---|---|
| **Grok (xAI)** | `ApiPreset.GROK` | `grok-3` |
| **OpenAI** | `ApiPreset.OPENAI` | `gpt-4o` |
| **Claude (Anthropic)** | `ApiPreset.CLAUDE` | `claude-opus-4-7` |
| **自定义** | `ApiPreset.CUSTOM` | 手动输入 |

## 技术支持

- 消息类型：文字、语音、视频
- 流式回复：打字机效果，逐字显示
- 媒体下载：AI 返回语音/视频时自动下载
- 对话历史：Room 持久化，跨重启保留
- 预设切换：运行时切换 AI 提供商，无需重启
