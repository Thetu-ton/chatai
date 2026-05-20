# 项目构建 & 推送状态

## APK 构建成功 — 可运行

```
APK: Android/sample/exampleui/build/outputs/apk/debug/exampleui-debug.apk
大小: 8.5 MB
minSdk: 19 (Android 4.4)
targetSdk: 33
```

## 构建环境

| 组件 | 路径 |
|---|---|
| JDK 17 (Corretto) | `C:\Users\honor\tools\jdk17\jdk17.0.19_10` |
| Android SDK | `C:\Users\honor\tools\android-sdk` |

## 运行时流程

1. App 启动 → 进入聊天界面 (MessageListActivity)
2. 用户点击右上角设置 → 进入 AI 提供商选择界面 (ApiProviderActivity)
3. 选择 Grok / OpenAI / Claude / 自定义
4. 输入 API Key + 保存 → AI 连接就绪
5. 返回聊天界面 → 发送文字/语音/视频消息 → AI 回复

## 构建命令

```bash
cd d:\allproject\chatai\Android
set JAVA_HOME=C:\Users\honor\tools\jdk17\jdk17.0.19_10
set ANDROID_HOME=C:\Users\honor\tools\android-sdk
gradlew :exampleui:assembleDebug
```

## 本次变更

### aiinteract 模块
- 新增 `ApiPreset.java` — Grok / OpenAI / Claude / Custom 预设
- 更新 `AiConfig.java` — 支持 `fromPreset()` 静态工厂
- 更新 `AiInteract.java` — 新增 `switchToPreset()`、`getAvailablePresets()`
- 更新 `AiApiClient.java` — 补充 `AiMessageType` import
- 更新 `CLAUDE.md` — UI 团队需要实现 API 提供商选择界面
- 更新 `read.md` — ApiPreset 文档

### memory 模块
- 修复 `MessageFts.java` — 移除 FTS4 contentEntity
- 修复 `StoredMessageDao.java` — 用 LIKE 替代 FTS MATCH 查询
- 修复 `MemoryDatabase.java` — 移除 MessageFts 实体

### Android UI 模块
- 修复 `MessageListActivity.java` — 替换 EasyPermissions 为 AndroidX API
- 修复 `ApiProviderActivity.java` — 补充 ImageView import，修复 radio drawable 引用
- 修复 `BrowserImageActivity.java` — 替换缺失 drawable
- 修复 `IMUISampleApplication.java` — 添加 MultiDex 支持
- 修复 `AndroidManifest.xml` — 添加 android:exported
- 修复 `exampleui/build.gradle` — multidex、minSdk 19、移除 easypermissions

### 新增占位资源
- `drawable/deadpool.xml` — 用户头像占位
- `drawable/ironman.xml` — AI 头像占位
- `drawable/placeholder_image.xml` — 图片占位
- `drawable/placeholder_avatar.xml` — 头像占位
- `drawable/ic_check_selected.xml` — 选中图标
- `drawable/ic_check_unselected.xml` — 未选中图标

## 可推送至 GitHub

所有编译错误已修复，运行时资源已补全，APK 可安装运行。
