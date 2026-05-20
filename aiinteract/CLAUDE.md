# AI Interact Module - UI Integration Guide

## Overview
The `aiinteract` module handles all AI API communication. The UI team is responsible for the Android UI (chat input, message display) and should use the public API surface documented here.

## Module Setup

Add to `Android/settings.gradle`:
```groovy
include ':aiinteract'
project(':aiinteract').projectDir = new File(rootProject.projectDir, '../aiinteract')
```

Add dependency in the UI module's `build.gradle`:
```groovy
implementation project(':aiinteract')
```

## Quick Start

### 1. Initialize with a preset (recommended)

```java
// Pick a preset and provide API key
AiConfig config = AiConfig.fromPreset(ApiPreset.GROK, "xai-your-api-key")
    .aiUserName("Grok")
    .aiUserAvatar("R.drawable.grok_avatar")
    .mediaDownloadDir(getExternalFilesDir("ai_media").getAbsolutePath())
    .build();

AiInteract.init(config);
```

### 2. Or initialize with custom endpoint

```java
AiConfig config = new AiConfig.Builder(
    "https://api.openai.com/v1/chat/completions",
    "sk-your-api-key"
)
    .model("gpt-4o")
    .build();

AiInteract.init(config);
```

### 3. Switch provider at runtime

```java
// User selects Grok in settings -> switch immediately
AiInteract.getInstance().switchToPreset(ApiPreset.GROK, "xai-new-api-key");
```

### 4. Send a text message

```java
AiInteract.getInstance().sendTextMessage("Hello AI!", new AiCallback() {
    @Override
    public void onResponse(AiMessage aiMessage) {
        IUser aiUser = new DefaultUser("ai_user", "AI", "R.drawable.ai_avatar");
        AiMessageBridge bridge = AiMessageBridge.forReceived(aiMessage, aiUser);
        mAdapter.addToStart(bridge, true);
    }

    @Override
    public void onStreamChunk(String chunk) { /* streaming text */ }

    @Override
    public void onStreamComplete() { /* streaming done */ }

    @Override
    public void onError(int code, String message) {
        Toast.makeText(context, "AI error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileDownloadProgress(int progress) { /* 0-100 */ }
});
```

## Key Classes

| Class | Purpose |
|---|---|
| `AiInteract` | Main singleton — init, send, conversation, preset switching |
| `AiConfig` | API endpoint, keys, model, feature toggles |
| `ApiPreset` | Predefined provider presets: GROK, OPENAI, CLAUDE, CUSTOM |
| `AiMessage` | Internal message model (not directly displayable) |
| `AiMessageBridge` | Converts `AiMessage` → `IMessage` for the existing MsgListAdapter |
| `AiCallback` | Callback interface for receiving AI responses |
| `AiMessageType` | Enum: TEXT, VOICE, VIDEO |

## Available Presets

| Preset | Enum | Endpoint | Default Model |
|---|---|---|---|
| Grok (xAI) | `ApiPreset.GROK` | `https://api.x.ai/v1/chat/completions` | `grok-3` |
| OpenAI | `ApiPreset.OPENAI` | `https://api.openai.com/v1/chat/completions` | `gpt-4o` |
| Claude | `ApiPreset.CLAUDE` | `https://api.anthropic.com/v1/messages` | `claude-opus-4-7` |
| Custom | `ApiPreset.CUSTOM` | (user entered) | (user entered) |

Get all available presets for UI display:
```java
List<ApiPreset> presets = AiInteract.getAvailablePresets();
// Returns: [GROK, OPENAI, CLAUDE]
// Each preset has: getId(), getDisplayName(), getDescription(), getModels(), getApiKeyUrl()
```

## UI Changes Needed

### 1. API Provider Selection Screen (NEW - highest priority)

A settings screen for users to choose their AI provider:

**Layout requirements:**
- List/Grid of available providers (Grok, OpenAI, Claude, Custom)
- Each item shows: provider icon/logo, name, description
- Selected provider is highlighted
- "Get API Key" link opens the provider's API key page in browser

**Implementation:**
```java
// Get the list for UI
List<ApiPreset> presets = AiInteract.getAvailablePresets();

// Each preset provides:
preset.getDisplayName();    // "Grok (xAI)"
preset.getDescription();    // "xAI 开发的 Grok 模型..."
preset.getApiKeyUrl();      // "https://console.x.ai" (for "Get API Key" button)
preset.getModels();         // ["grok-3", "grok-3-mini"]

// When user selects a preset:
AiInteract.getInstance().switchToPreset(selectedPreset, apiKey);
```

**Wireframe:**
```
┌─────────────────────────────────┐
│  ← AI Provider Settings         │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │ 🤖 Grok (xAI)         ✓  │  │  ← selected
│  │ grok-3 · grok-3-mini     │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ 🧠 OpenAI (ChatGPT)       │  │
│  │ gpt-4o · gpt-4 · ...     │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ 📝 Claude (Anthropic)     │  │
│  │ claude-opus-4-7 · ...    │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ ⚙ Custom API             │  │
│  └───────────────────────────┘  │
│                                 │
│  API Key: [________________]    │
│  [Get API Key]                  │
│                                 │
│  Model: [▼ grok-3         ]    │
│                                 │
│  Voice: [✓]  Video: [ ]        │
│                                 │
│       [Save & Connect]          │
└─────────────────────────────────┘
```

### 2. Custom API Configuration (for ApiPreset.CUSTOM)

When user selects Custom, show:
- API Endpoint URL input
- API Key input
- Model name input (free text)
- Voice/Video toggle switches

### 3. Send Button Flow Update

In `MessageListActivity.onSendTextMessage()`:
- Call `AiInteract.getInstance().sendTextMessage()` instead of local message creation
- Use `AiMessageBridge.forSending()` for the user's sent message
- Use `AiMessageBridge.forReceived()` for the AI's response

### 4. AI "Typing" Indicator

When waiting for AI response:
- Add a temporary "typing" message bubble to the adapter
- Remove/replace it when `onResponse()` fires
- For streaming: update the message content as `onStreamChunk()` arrives

### 5. Voice/Video Playback
- The existing `VoiceViewHolder` and `VideoViewHolder` already handle playback
- AI voice/video responses will have `getMediaFilePath()` set after download
- Progress comes through `onFileDownloadProgress(0-100)`

### 6. Error Handling UI
- Show errors from `onError()` as a toast or snackbar
- Common error codes: -1 (network), -2 (parsing), -3 (invalid input)

## API Format (for custom endpoints)

The module sends requests in OpenAI-compatible format:
```json
{
  "model": "grok-3",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "Hello"},
    {"role": "assistant", "content": "Hi there!"}
  ],
  "max_tokens": 2048,
  "temperature": 0.7,
  "stream": false
}
```

## File Structure

```
aiinteract/
  build.gradle
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/com/chatai/aiinteract/
      AiInteract.java          — Main facade (singleton)
      AiConfig.java            — Configuration builder
      ApiPreset.java           — Provider presets (GROK, OPENAI, CLAUDE, CUSTOM)
      callback/
        AiCallback.java        — UI callback interface
      models/
        AiMessage.java         — Internal message model
        AiMessageType.java     — TEXT/VOICE/VIDEO enum
        AiResponse.java        — API response parser
      network/
        AiApiClient.java       — HTTP client
      util/
        AiFileDownloader.java  — Media file downloader
      bridge/
        AiMessageBridge.java   — AiMessage → IMessage adapter
```
