# Aurora IM-UI Android Module — AI Agent Guide

## 项目概述

本项目是基于 [jpush/aurora-imui](https://github.com/jpush/aurora-imui) 改造的 Android IM UI 组件库。已完成 AndroidX 迁移和现代化改造，可直接在 Android Studio 中打开 `Android/` 目录构建运行。

## 项目结构

```
Android/
├── build.gradle              # 根构建文件 (AGP 7.4.2, compileSdk 33)
├── settings.gradle           # 模块注册
├── gradle.properties         # AndroidX 启用配置
├── gradlew / gradlew.bat     # Gradle Wrapper (Gradle 7.6)
├── chatinput/                # [库] 聊天输入组件
├── messagelist/              # [库] 消息列表组件
├── sample/exampleui/         # [App] 示例应用
└── sample/tools/             # ProGuard 规则
```

**模块依赖关系：**
```
exampleui (App) ──┬── messagelist (Library)
                  ├── chatinput (Library)
                  ├── aiinteract (Library, 待实现)
                  └── memory (Library, 待实现)
```

---

## 核心接口速查

### 你必须实现的 4 个核心接口

#### 1. `IMessage` — 消息模型
**位置:** `messagelist/.../cn/jiguang/imui/commons/models/IMessage.java`

```java
public interface IMessage {
    String getMsgId();                          // 消息唯一ID
    IUser getFromUser();                        // 发送者
    String getTimeString();                     // 时间字符串
    int getType();                              // MessageType 枚举的 ordinal
    MessageStatus getMessageStatus();           // 发送/接收状态
    String getText();                           // 文本内容
    String getMediaFilePath();                  // 媒体文件路径
    long getDuration();                         // 语音/视频时长(秒)
    String getProgress();                       // 上传/下载进度
    HashMap<String, String> getExtras();        // 扩展字段

    enum MessageType {
        EVENT,           // 0 - 系统事件消息
        SEND_TEXT,       // 1 - 发出的文本
        RECEIVE_TEXT,    // 2 - 收到的文本
        SEND_IMAGE,      // 3 - 发出的图片
        RECEIVE_IMAGE,   // 4 - 收到的图片
        SEND_VOICE,      // 5 - 发出的语音
        RECEIVE_VOICE,   // 6 - 收到的语音
        SEND_VIDEO,      // 7 - 发出的视频
        RECEIVE_VIDEO,   // 8 - 收到的视频
        SEND_LOCATION,   // 9 - 发出的位置
        RECEIVE_LOCATION,// 10 - 收到的位置
        SEND_FILE,       // 11 - 发出的文件
        RECEIVE_FILE,    // 12 - 收到的文件
        SEND_CUSTOM,     // 13 - 发出的自定义消息
        RECEIVE_CUSTOM   // 14 - 收到的自定义消息
    }

    enum MessageStatus {
        CREATED, SEND_GOING, SEND_SUCCEED, SEND_FAILED, SEND_DRAFT,
        RECEIVE_GOING, RECEIVE_SUCCEED, RECEIVE_FAILED
    }
}
```

**参考实现** (`sample/.../models/MyMessage.java`):
```java
public class MyMessage implements IMessage {
    private long id;
    private String text;
    private int type;
    private IUser user;
    private String mediaFilePath;
    private long duration;
    private String progress;
    private String timeString;
    private MessageStatus status;

    // 构造函数直接指定 type 的 ordinal
    public MyMessage(String text, int type) {
        this.text = text;
        this.type = type;  // 例如 IMessage.MessageType.SEND_TEXT.ordinal()
        this.status = MessageStatus.CREATED;
    }

    // 所有 IMessage 接口方法的 getter/setter...
}
```

#### 2. `IUser` — 用户模型
**位置:** `messagelist/.../cn/jiguang/imui/commons/models/IUser.java`

```java
public interface IUser {
    String getId();            // 用户唯一标识
    String getDisplayName();   // 显示名称
    String getAvatarFilePath();// 头像路径
}
```

#### 3. `ImageLoader` — 图片加载适配
**位置:** `messagelist/.../cn/jiguang/imui/commons/ImageLoader.java`

```java
public interface ImageLoader {
    void loadAvatarImage(ImageView avatarImageView, String string);
    void loadImage(ImageView imageView, String string);
    void loadVideo(ImageView imageCover, String uri);
}
```

**使用 Glide 的参考实现:**
```java
public class GlideImageLoader implements ImageLoader {
    @Override
    public void loadAvatarImage(ImageView iv, String url) {
        Glide.with(iv.getContext()).load(url).circleCrop().into(iv);
    }
    @Override
    public void loadImage(ImageView iv, String url) {
        Glide.with(iv.getContext()).load(url).into(iv);
    }
    @Override
    public void loadVideo(ImageView iv, String uri) {
        Glide.with(iv.getContext()).load(uri).into(iv);
    }
}
```

#### 4. `OnMenuClickListener` — 输入栏按钮回调
**位置:** `chatinput/.../cn/jiguang/imui/chatinput/listener/OnMenuClickListener.java`

```java
public interface OnMenuClickListener {
    boolean onSendTextMessage(CharSequence input);   // 点击发送
    void onSendFiles(List<FileItem> list);           // 发送选中文件
    boolean switchToMicrophoneMode();                 // 切换到语音模式
    boolean switchToGalleryMode();                    // 切换到相册模式
    boolean switchToCameraMode();                     // 切换到拍照模式
    boolean switchToEmojiMode();                      // 切换到表情模式
}
```

---

### 可选的监听器接口（按需实现）

| 接口 | 注册位置 | 用途 |
|------|---------|------|
| `RecordVoiceListener` | `chatInputView.setRecordVoiceListener()` | 语音录制回调(5个方法) |
| `OnCameraCallbackListener` | `chatInputView.setOnCameraCallbackListener()` | 拍照/录像回调(4个方法) |
| `CameraControllerListener` | `chatInputView.setCameraControllerListener()` | 相机UI控制事件(4个方法) |
| `OnMsgClickListener<M>` | `adapter.setOnMsgClickListener()` | 消息点击 |
| `OnMsgLongClickListener<M>` | `adapter.setMsgLongClickListener()` | 消息长按 |
| `OnAvatarClickListener<M>` | `adapter.setOnAvatarClickListener()` | 头像点击 |
| `CustomMenuEventListener` | `chatInputView.setCustomMenuClickListener()` | 自定义菜单项 |
| `OnFileSelectedListener` | `selectPhotoView.setOnFileSelectedListener()` | 文件选择变化 |
| `PtrHandler` | `ptrLayout.setPtrHandler()` | 下拉刷新 |

---

## ChatInput 模块 API

### `ChatInputView` (核心组件)

```java
// === 监听器注册 ===
chatInputView.setMenuClickListener(OnMenuClickListener listener);
chatInputView.setCustomMenuClickListener(CustomMenuEventListener listener);
chatInputView.setRecordVoiceListener(RecordVoiceListener listener);
chatInputView.setOnCameraCallbackListener(OnCameraCallbackListener listener);
chatInputView.setCameraControllerListener(CameraControllerListener listener);
chatInputView.setOnClickEditTextListener(OnClickEditTextListener listener);

// === 面板控制 ===
chatInputView.showMenuLayout();           // 显示菜单面板
chatInputView.dismissMenuLayout();        // 隐藏菜单面板
chatInputView.showRecordVoiceLayout();    // 显示录音面板
chatInputView.dismissRecordVoiceLayout(); // 隐藏录音面板
chatInputView.showSelectPhotoLayout();    // 显示相册面板
chatInputView.dismissPhotoLayout();       // 隐藏相册面板
chatInputView.showCameraLayout();         // 显示拍照面板
chatInputView.dismissCameraLayout();      // 隐藏拍照面板
chatInputView.showEmojiLayout();          // 显示表情面板
chatInputView.dismissEmojiLayout();       // 隐藏表情面板

// === 相机 ===
chatInputView.initCamera();               // 初始化相机
chatInputView.setCameraQuality(0.5f);     // 设置相机画质(0~1)

// === 语音 ===
chatInputView.pauseVoice();               // 暂停播放
chatInputView.setAudioPlayByEarPhone(0);  // 0=扬声器, 非0=听筒

// === 子View获取 ===
chatInputView.getInputView();             // 输入框 EditText
chatInputView.getRecordVoiceButton();     // 录音按钮
chatInputView.getCameraContainer();       // 相机容器
chatInputView.getSelectPhotoView();       // 相册选择View
chatInputView.getMenuManager();           // 菜单管理器
chatInputView.getStyle();                 // 样式对象
chatInputView.isKeyboardVisible();        // 键盘是否可见
```

### `MenuManager` — 自定义菜单

```java
MenuManager menuManager = chatInputView.getMenuManager();

// 构建自定义菜单布局
Menu menu = Menu.newBuilder()
    .customize(true)
    .setLeft("voice", "gallery")          // 左侧按钮
    .setRight("camera", "emoji")          // 右侧按钮
    .setBottom("send")                     // 底部按钮
    .build();
menuManager.setMenu(menu);

// 添加自定义菜单项
menuManager.addCustomMenu("red_packet", R.layout.menu_item, R.layout.menu_feature);
```

### `RecordVoiceListener` 接口

```java
public interface RecordVoiceListener {
    void onStartRecord();                              // 开始录音
    void onFinishRecord(File voiceFile, int duration); // 录音完成
    void onCancelRecord();                             // 取消录音
    void onPreviewCancel();                            // 预览时取消
    void onPreviewSend();                              // 预览时发送
}
```

### `OnCameraCallbackListener` 接口

```java
public interface OnCameraCallbackListener {
    void onTakePictureCompleted(String photoPath);   // 拍照完成
    void onStartVideoRecord();                        // 开始录像
    void onFinishVideoRecord(String videoPath);       // 录像完成
    void onCancelVideoRecord();                       // 取消录像
}
```

---

## MessageList 模块 API

### `MessageList` (核心组件)

```java
// === 初始化 ===
MessageList messageList = findViewById(R.id.msg_list);
MsgListAdapter<MyMessage> adapter = new MsgListAdapter<>("myUserId", imageLoader);
messageList.setAdapter(adapter);

// === 样式配置 (40+ 方法) ===
messageList.setSendBubbleDrawable(R.drawable.bubble_send);
messageList.setSendBubbleColor(Color.BLUE);
messageList.setReceiveBubbleColor(Color.WHITE);
messageList.setDateTextSize(12);
messageList.setAvatarWidth(80);
messageList.setAvatarRadius(40);
messageList.setBubbleMaxWidth(0.75f);
messageList.setShowSenderDisplayName(true);
messageList.setShowReceiverDisplayName(true);
// ... 还有更多 setter, 见 MessageList.java 的 public 方法
```

### `MsgListAdapter<M extends IMessage>`

```java
// === 构造函数 ===
MsgListAdapter(String senderId, ImageLoader imageLoader);
MsgListAdapter(String senderId, HoldersConfig holders, ImageLoader imageLoader);

// === 数据操作 ===
adapter.addToStart(message, true);                    // 头部添加, 是否滚动到底部
adapter.addToEnd(messages);                           // 尾部批量添加
adapter.addToEndChronologically(messages);            // 尾部追加(去重排序)
adapter.updateMessage(message);                       // 更新单条(按msgId)
adapter.updateOrAddMessage(oldId, newMsg, scroll);    // 更新或添加
adapter.delete(message);                              // 删除单条
adapter.deleteById("msgId");                          // 按ID删除
adapter.deleteByIds(new String[]{"id1", "id2"});      // 批量按ID删除
adapter.clear();                                      // 清空
adapter.getMessageList();                             // 获取全部消息列表

// === 自定义消息类型 ===
adapter.addCustomMsgType(
    viewType,                                         // >= 13 的唯一值
    CustomMsgConfig.newBuilder()
        .setViewType(viewType)
        .setResourceId(R.layout.item_custom)
        .setIsSender(true)
        .setClass(MyCustomViewHolder.class)
        .build()
);

// === 功能控制 ===
adapter.enableSelectionMode(listener);                // 启用多选模式
adapter.disableSelectionMode();                       // 禁用多选
adapter.getSelectedMessages();                        // 获取已选消息
adapter.deleteSelectedMessages();                     // 删除已选消息
adapter.pauseVoice();                                 // 暂停语音播放
adapter.setAudioPlayByEarPhone(0);                    // 0=扬声器, 1=耳机, 2=听筒
```

### `HoldersConfig` — 自定义 ViewHolder

```java
MsgListAdapter.HoldersConfig holders = new MsgListAdapter.HoldersConfig();
holders.setSenderTxtMsg(MyTxtViewHolder.class, R.layout.my_send_txt);
holders.setReceiverTxtMsg(MyTxtViewHolder.class, R.layout.my_receive_txt);
holders.setSenderVoiceMsg(MyVoiceViewHolder.class, R.layout.my_send_voice);
holders.setEventMessage(MyEventViewHolder.class, R.layout.my_event);
// ... 还有 send/receive photo, video, location 的 setter
```

### 自定义 ViewHolder 基类

```java
public class MyTxtViewHolder extends BaseMessageViewHolder<MyMessage>
        implements MsgListAdapter.DefaultMessageViewHolder {

    public MyTxtViewHolder(View itemView, boolean isSender) {
        super(itemView);
        // 绑定控件
    }

    @Override
    public void onBind(MyMessage message) {
        // 绑定数据到控件
        // 可用受保护字段: mContext, mImageLoader, mPosition,
        //   mIsSelected, mMsgClickListener, mMsgLongClickListener,
        //   mAvatarClickListener, mMsgStatusViewClickListener
    }

    @Override
    public void applyStyle(MessageListStyle style) {
        // 应用 MessageList 统一样式
    }
}
```

---

## 快速集成步骤

### 1. 实现数据模型

```java
// 1. 实现 IUser
public class User implements IUser { ... }

// 2. 实现 IMessage
public class ChatMessage implements IMessage { ... }

// 3. 实现 ImageLoader (推荐用 Glide)
public class GlideImageLoader implements ImageLoader { ... }
```

### 2. 初始化 ChatView (推荐方式)

```java
// 直接用封装的 ChatView, 或手动组合 MessageList + ChatInputView
ChatView chatView = findViewById(R.id.chat_view);
chatView.initModule();

// 设置消息适配器
MsgListAdapter<ChatMessage> adapter = new MsgListAdapter<>(userId, new GlideImageLoader());
chatView.setAdapter(adapter);

// 设置语音文件路径(必须)
chatView.setRecordVoiceFile(Environment.getExternalStorageDirectory().getPath(), "voice");

// 设置相机文件路径(必须)
chatView.setCameraCaptureFile(Environment.getExternalStorageDirectory().getPath(), "photo");
```

### 3. 注册核心监听器

```java
// 必须: 输入栏事件
chatView.setOnMenuClickListener(new OnMenuClickListener() {
    @Override
    public boolean onSendTextMessage(CharSequence input) {
        // 构造消息 → 调用 SDK 发送 → 更新 adapter
        ChatMessage msg = new ChatMessage(input.toString(), MessageType.SEND_TEXT.ordinal());
        msg.setUserInfo(currentUser);
        adapter.addToStart(msg, true);
        return true;
    }
    @Override
    public void onSendFiles(List<FileItem> list) { ... }
    @Override
    public boolean switchToMicrophoneMode() { return true; }
    @Override
    public boolean switchToGalleryMode() { return true; }
    @Override
    public boolean switchToCameraMode() { return true; }
    @Override
    public boolean switchToEmojiMode() { return true; }
});

// 语音录制
chatView.setOnRecordVoiceListener(new RecordVoiceListener() {
    @Override public void onStartRecord() { }
    @Override public void onFinishRecord(File voiceFile, int duration) {
        // 发送语音消息
    }
    @Override public void onCancelRecord() { }
    @Override public void onPreviewCancel() { }
    @Override public void onPreviewSend() { }
});

// 拍照/录像
chatView.setOnCameraCallbackListener(new OnCameraCallbackListener() {
    @Override public void onTakePictureCompleted(String photoPath) { }
    @Override public void onStartVideoRecord() { }
    @Override public void onFinishVideoRecord(String videoPath) { }
    @Override public void onCancelVideoRecord() { }
});

// 消息点击
adapter.setOnMsgClickListener(msg -> {
    // 图片消息 → 打开大图浏览
    // 视频消息 → 播放视频
    // ...
});
```

---

## 待实现模块

### `aiinteract` 模块

**目的:** AI 交互逻辑层（连接 UI 与 AI SDK）

**需要实现的 API 接口:**

| 接口/类 | 职责 | 优先级 |
|---------|------|--------|
| `AIInteractManager` | 单例, 管理 AI SDK 初始化、配置、生命周期 | P0 |
| `MessageSender` | 封装消息发送逻辑(文本/图片/语音/视频) | P0 |
| `MessageReceiver` | 封装消息接收逻辑, 回调到 UI 层 | P0 |
| `ConversationManager` | 会话管理: 创建/切换/删除会话 | P1 |
| `TypingIndicator` | "对方正在输入" 状态管理 | P1 |
| `AIConfig` | AI SDK 配置数据类 (API Key, Model 等) | P0 |

**关键方法签名建议:**

```java
// AIInteractManager
public void initialize(Context context, AIConfig config);
public MessageSender getMessageSender();
public MessageReceiver getMessageReceiver();
public ConversationManager getConversationManager();

// MessageSender
public void sendText(String conversationId, String text, SendCallback callback);
public void sendImage(String conversationId, String imagePath, SendCallback callback);
public void sendVoice(String conversationId, String voicePath, int duration, SendCallback callback);
public void sendVideo(String conversationId, String videoPath, SendCallback callback);

// MessageReceiver
public void startListening(String conversationId);
public void stopListening();
public void setOnMessageReceivedListener(OnMessageReceivedListener listener);

// ConversationManager
public void createConversation(String title, Callback callback);
public void switchConversation(String conversationId);
public void deleteConversation(String conversationId);
public List<IConversation> getConversations();

// AIConfig
String apiKey;
String modelId;
String baseUrl;
Map<String, String> extraHeaders;
```

### `memory` 模块

**目的:** 持久化记忆存储（对话历史、用户偏好、上下文）

**需要实现的 API 接口:**

| 接口/类 | 职责 | 优先级 |
|---------|------|--------|
| `MemoryStore` | 核心存储接口：增删改查 | P0 |
| `MemoryManager` | 管理记忆的读写、过期、容量控制 | P0 |
| `MemoryItem` | 记忆条目数据类 | P0 |
| `MemoryType` | 枚举：用户偏好/项目上下文/反馈/引用 | P0 |
| `MemoryIndex` | 索引文件管理 (类似 MEMORY.md) | P1 |
| `MemorySearcher` | 记忆检索(向量/关键词) | P1 |

**关键方法签名建议:**

```java
// MemoryStore 接口
public interface MemoryStore {
    void save(MemoryItem item);
    void update(String id, MemoryItem item);
    void delete(String id);
    MemoryItem get(String id);
    List<MemoryItem> query(MemoryQuery query);
    List<MemoryItem> getAll();
    void clear();
}

// MemoryManager
public void init(Context context, MemoryConfig config);
public void remember(String content, MemoryType type, Map<String, String> metadata);
public void forget(String id);
public List<MemoryItem> recall(String query, int limit);
public List<MemoryItem> recallByType(MemoryType type);
public void compact();                     // 清理过期/低价值记忆
public File getMemoryDir();

// MemoryItem 数据类
class MemoryItem {
    String id;
    String name;          // slug 短名称
    String description;   // 单行摘要
    MemoryType type;      // user / feedback / project / reference
    String content;       // 正文 (Markdown)
    Map<String, String> metadata;  // 关联/时间戳等
    long createdAt;
    long updatedAt;
}

// MemoryType 枚举
enum MemoryType {
    USER,       // 用户角色/偏好/知识
    FEEDBACK,   // 反馈/正确 or 错误的行为模式
    PROJECT,    // 项目上下文/目标/Bug
    REFERENCE   // 外部系统指针 (URLs, 频道等)
}
```

---

## 构建运行

### 前置条件
- **Java 11+**
- **Android Studio** (推荐) 或 Android SDK 命令行工具
- **Android SDK 33**

### 步骤
```bash
# 1. 进入 Android 目录
cd Android

# 2. 生成 Gradle Wrapper (如果缺失 gradle-wrapper.jar)
gradle wrapper --gradle-version 7.6

# 3. 构建
./gradlew :exampleui:assembleDebug

# 4. 或直接在 Android Studio 中打开 Android/ 目录, 运行 exampleui
```

### 构建输出
- APK: `sample/exampleui/build/outputs/apk/dev/debug/`
- AAR (chatinput): `chatinput/build/outputs/aar/`
- AAR (messagelist): `messagelist/build/outputs/aar/`

---

## 关键注意事项

1. **最小 SDK:** API 16 (Android 4.1), **目标 SDK:** API 33
2. **自定义消息类型**必须使用 >= 13 的 viewType, 0-12 被内置类型占用
3. `namespace` 在各模块的 `build.gradle` 中定义, AndroidManifest.xml 中不设 `package` 属性
4. ChatInputView 需要 **CAMERA, RECORD_AUDIO, READ/WRITE_EXTERNAL_STORAGE** 权限
5. 录音前必须调用 `setRecordVoiceFile(path, fileName)` 设置输出路径
6. `MsgListAdapter.getMediaPlayer()` 是全局单例, 同时只能播放一个语音
