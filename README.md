# 吉他譜架 Android App

[吉他譜架](https://github.com/agan0617/GuitarTab)（邊彈邊看吉他譜的網頁）的 Android 外殼。

- 整個畫面是一個 WebView，載入 <https://agan0617.github.io/GuitarTab/>。網頁更新 App 不用重裝
- App 開著時螢幕不會自動關；全螢幕顯示，從邊緣滑一下叫出系統列
- 藍牙翻頁踏板、鍵盤的按鍵直接交給網頁處理；返回鍵在看譜中回譜架，在譜架上退到背景
- 沒用任何第三方函式庫，只有 Android 平台 API

## 安裝與連線

1. 到這個 repo 的 [Releases](../../releases) 下載最新的 `.apk`，在手機上打開安裝
2. 打開 App → 右上角「連線設定」→ 貼上只授權譜架 repo、Contents 可讀的 fine-grained token → 連線。App 的 WebView 跟瀏覽器、K書吧 App 的資料是分開的，所以要另外貼一次

## 建置

需要 Android Studio 內建的 JDK 21（`C:\Program Files\Android\Android Studio\jbr`）與 SDK 34。

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleRelease
```

改版時三個地方一起改：`app/build.gradle` 的 `versionCode`（+1）、`versionName`，以及 `BuildConfigVersion.NAME`；Release 的 tag 用 `v` + versionName。

產出 `app/build/outputs/apk/release/app-release.apk`（用這台電腦的 debug 金鑰簽章，換電腦建置的版本要先移除舊的才能裝）。

⚠️ 從 Claude 桌面版開的 shell 建置時，`%TEMP%` 會被 MSIX 沙盒導走，Gradle 會報「Unable to establish loopback connection」。把 `TEMP`／`TMP` 設到 `D:\Temp\jdk`，並加 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=D:\Temp\jdk -Djava.io.tmpdir=D:\Temp\jdk`。
