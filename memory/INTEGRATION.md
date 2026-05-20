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
import com.chatai.aiinteract.AiConfig;
import com.chatai.aiinteract.callback.AiCallback;
import com.chatai.aiinteract.models.AiMessage;
import com.chatai.aiinteract.bridge.AiMessageBridge;
```

#### 2b. In `onCreate()`, after `mChatView.initModule()`:

```java
// --- Initialize Memory Module ---
MemoryConfig memoryConfig = new MemoryConfig.Builder()
    .mem0("https://your-mem0-server.com", "mem0-api-key", "user_123")
    .userDisplayName("Ironman")
    .maxContextMessages(50)
    .maxMemoryEntries(5)
    .build();
MemoryManager.init(this, memoryConfig);

// --- Initialize AI Module ---
AiConfig aiConfig = new AiConfig.Builder(
    "https://api.openai.com/v1/chat/completions",
    "sk-your-api-key"
)
    .model("gpt-4")
    .aiUserName("AI Assistant")
    .build();
AiInteract.init(aiConfig);

// --- Set system prompt with memories ---
String prompt = MemoryManager.getInstance().buildSystemPromptWithMemories();
AiInteract.getInstance().setSystemPrompt(prompt);

// --- Restore last session ---
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

    // 2. Persist to memory (Room)
    MemoryManager.getInstance().onUserMessage(message, "1");

    // 3. Extract long-term memories (Mem0, async)
    MemoryManager.getInstance().memorize(text);

    // 4. Show typing indicator
    IUser aiUser = new DefaultUser("0", "AI", "R.drawable.deadpool");
    IMessage typingMsg = MessageConverter.createTypingIndicator(aiUser);
    mAdapter.addToStart(typingMsg, true);
    String typingMsgId = typingMsg.getMsgId();

    // 5. Refresh system prompt with latest Mem0 memories before sending
    MemoryManager.getInstance().buildSystemPromptAsync(prompt -> {
        AiInteract.getInstance().setSystemPrompt(prompt);

        // 6. Send to AI
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
    });

    return true;
}
```

#### 2d. In `onDestroy()`:

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

## 3. Make AiApiClient use configurable system prompt

### File: `aiinteract/src/main/java/com/chatai/aiinteract/network/AiApiClient.java`

The AiApiClient hardcodes its system prompt. The memory module needs to inject its own (with Mem0 memories).

**Option A** (minimal): AiApiClient already builds the system prompt from a constant. `AiInteract.setSystemPrompt()` adds a system message to conversation history. The hardcoded SYSTEM_PROMPT in AiApiClient is a separate system message added *before* the conversation history. Update `buildRequestBody()` to use a configurable prompt:

```java
private String customSystemPrompt;

public void setSystemPrompt(String prompt) {
    this.customSystemPrompt = prompt;
}

private String getPrompt() {
    return customSystemPrompt != null ? customSystemPrompt : SYSTEM_PROMPT;
}
```

Then use `getPrompt()` instead of `SYSTEM_PROMPT` in `buildRequestBody()` and `buildRequestBodyStreaming()`.

**Option B** (cleaner): MemoryManager provides the full context list via `getContextForLLM(userInput)`, which already includes the system prompt. If AiInteract had a method like `sendWithContext(List<AiMessage> messages, AiCallback cb)` that bypasses its own conversation history building, the integration would be seamless.

For now, use **Option A + calling `AiInteract.getInstance().setSystemPrompt(prompt)` before each send**.

---

## 4. Remove mock data (for production)

In `initMsgAdapter()`, remove or comment out the hardcoded sample messages (~lines 611-634 in MessageListActivity). Also remove `mAdapter.addToEndChronologically(mData)`.

---

## 5. Mem0 Server Setup

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

Configure via `MemoryConfig.Builder.mem0(endpoint, apiKey, userId)`.

---

## Summary of Files to Change

| File | Change |
|---|---|
| `Android/settings.gradle` | Add `:memory` and `:aiinteract` module includes |
| `Android/sample/exampleui/build.gradle` | Add `implementation project(':memory')` and `:aiinteract` |
| `aiinteract/.../network/AiApiClient.java` | Make system prompt configurable |
| `Android/.../MessageListActivity.java` | Wire MemoryManager + AiInteract, remove mock data |

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
        ├──► MemoryManager.buildSystemPromptAsync()    [获取最新 Mem0 记忆]
        │     └── AiInteract.setSystemPrompt()         [注入到 LLM system prompt]
        │
        └──► AiInteract.sendTextMessage()              [发送给 LLM]
                │
                ▼
        AiCallback.onResponse(aiMsg)
                │
                ├──► MemoryManager.onAiResponse()      [Room 持久化]
                │
                └──► AiMessageBridge → mAdapter        [UI display]
```
