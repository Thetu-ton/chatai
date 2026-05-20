# Memory Module — Integration Guide

This document specifies the changes needed in files **outside** `memory/` to integrate the memory system into the app. Each section targets a specific file.

---

## 1. Register modules in Gradle

### File: `Android/settings.gradle`

```groovy
include ':memory'
project(':memory').projectDir = new File(rootProject.projectDir, '../memory')

include ':aiinteract'
project(':aiinteract').projectDir = new File(rootProject.projectDir, '../aiinteract')
```

### File: `Android/sample/exampleui/build.gradle`

```groovy
dependencies {
    // ... existing deps ...
    implementation project(':memory')
    implementation project(':aiinteract')
}
```

---

## 2. Wire MemoryManager into the chat Activity

### File: `Android/sample/exampleui/src/main/java/imui/jiguang/cn/imuisample/messages/MessageListActivity.java`

#### 2a. Add imports:

```java
import com.chatai.memory.MemoryManager;
import com.chatai.memory.MemoryConfig;
import com.chatai.memory.MessageConverter;
import com.chatai.aiinteract.AiInteract;
import com.chatai.aiinteract.ApiPreset;
import com.chatai.aiinteract.callback.AiCallback;
import com.chatai.aiinteract.models.AiMessage;
import com.chatai.aiinteract.bridge.AiMessageBridge;
```

#### 2b. In `onCreate()`, after `mChatView.initModule()`:

```java
// --- Initialize Memory + AI in one call ---
MemoryConfig memoryConfig = MemoryConfig.fromPreset(
    ApiPreset.GROK,          // AI 提供商
    "xai-your-api-key",      // AI API Key
    "user_123"               // Mem0 用户 ID
)
    .mem0("https://api.mem0.ai", "mem0-api-key")
    .userDisplayName("Ironman")
    .maxContextMessages(50)
    .maxMemoryEntries(5)
    .build();

MemoryManager.init(this, memoryConfig);
// AiInteract is auto-initialized — no separate AiInteract.init() needed

// Restore last session (includes long-term memories in system prompt)
MemoryManager.getInstance().restoreConversationToAiInteract();
```

#### 2c. Replace `onSendTextMessage()`:

```java
@Override
public boolean onSendTextMessage(CharSequence input) {
    if (input.length() == 0) return false;

    String text = input.toString();

    // 1. Display user message in UI
    MyMessage message = new MyMessage(text, IMessage.MessageType.SEND_TEXT.ordinal());
    message.setUserInfo(new DefaultUser("1", "Ironman", "R.drawable.ironman"));
    message.setTimeString(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    message.setMessageStatus(IMessage.MessageStatus.SEND_GOING);
    mAdapter.addToStart(message, true);

    // 2. Persist to memory (Room) + extract long-term memories (Mem0, async)
    MemoryManager.getInstance().onUserMessage(message, "1");
    MemoryManager.getInstance().memorize(text);

    // 3. Apply updated system prompt (with fresh Mem0 memories) to AiInteract
    MemoryManager.getInstance().applySystemPromptToAi();

    // 4. Show typing indicator
    IUser aiUser = new DefaultUser("0", "AI", "R.drawable.deadpool");
    IMessage typingMsg = MessageConverter.createTypingIndicator(aiUser);
    mAdapter.addToStart(typingMsg, true);
    String typingMsgId = typingMsg.getMsgId();

    // 5. Send to AI
    AiInteract.getInstance().sendTextMessage(text, new AiCallback() {
        @Override
        public void onResponse(AiMessage aiMessage) {
            mAdapter.deleteById(typingMsgId);
            MemoryManager.getInstance().onAiResponse(aiMessage);
            IMessage displayMsg = AiMessageBridge.forReceived(aiMessage, aiUser);
            mAdapter.addToStart(displayMsg, true);
        }

        @Override
        public void onStreamChunk(String chunk) { /* update streaming bubble */ }

        @Override
        public void onStreamComplete() {}

        @Override
        public void onError(int code, String msg) {
            mAdapter.deleteById(typingMsgId);
            Toast.makeText(MessageListActivity.this,
                "AI Error: " + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileDownloadProgress(int progress) {}
    });

    return true;
}
```

#### 2d. Provider switching (for Settings screen):

```java
// User picks a different AI provider in settings:
MemoryManager.getInstance().switchAiPreset(ApiPreset.CLAUDE, "claude-api-key");

// Get available presets for the settings UI:
List<ApiPreset> presets = MemoryManager.getInstance().getAvailablePresets();
// Each preset has: getId(), getDisplayName(), getDescription(), getModels(), getApiKeyUrl()
```

#### 2e. In `onDestroy()`:

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mReceiver);
    mSensorManager.unregisterListener(this);
    MemoryManager.getInstance().shutdown();
    AiInteract.getInstance().shutdown();
}
```

---

## 3. Remove mock data (for production)

In `initMsgAdapter()`, remove or comment out the hardcoded sample messages (~lines 611-634 in MessageListActivity). Also remove `mAdapter.addToEndChronologically(mData)`.

---

## 4. Mem0 Server Setup

The memory module's long-term memory depends on a running Mem0 instance.

**Self-hosted:**
```bash
pip install mem0ai
mem0 server start --host 0.0.0.0 --port 8000
```

**Docker:**
```bash
docker run -p 8000:8000 -e MEM0_API_KEY=your-key mem0/mem0-server
```

**Mem0 Cloud:** Use `https://api.mem0.ai` with your API key.

Configure via `MemoryConfig.fromPreset(...).mem0(endpoint, apiKey)`.

---

## Summary of Files to Change

| File | Change |
|---|---|
| `Android/settings.gradle` | Add `:memory` and `:aiinteract` module includes |
| `Android/sample/exampleui/build.gradle` | Add `implementation project(':memory')` and `:aiinteract` |
| `Android/.../MessageListActivity.java` | Wire MemoryManager (auto-inits AiInteract), remove mock data |
| (none) | No changes needed to AiApiClient — `setSystemPrompt()` already supported |

---

## Final Architecture

```
User types in ChatInputView
        │
        ▼
MessageListActivity.onSendTextMessage()
        │
        ├──► MyMessage → mAdapter.addToStart()        [UI display]
        │
        ├──► MemoryManager.onUserMessage()             [Room 持久化]
        │
        ├──► MemoryManager.memorize()                  [Mem0 长期记忆]
        │     └── Mem0Client → POST /v1/memories/
        │
        ├──► MemoryManager.applySystemPromptToAi()     [注入长期记忆到 system prompt]
        │     └── AiInteract.setSystemPrompt()
        │
        └──► AiInteract.sendTextMessage()              [发送给 LLM]
                │
                ▼
        AiCallback.onResponse(aiMsg)
                │
                ├──► MemoryManager.onAiResponse()      [Room 持久化]
                │
                └──► AiMessageBridge → mAdapter        [UI display]

--- Provider Switching ---

Settings Activity
        │
        ├──► MemoryManager.getAvailablePresets()       [获取提供商列表]
        │     └── ApiPreset.getDisplayName() / getDescription() / getModels()
        │
        └──► MemoryManager.switchAiPreset(preset, key) [切换提供商]
              └── AiInteract.switchToPreset() + applySystemPromptToAi()
```
