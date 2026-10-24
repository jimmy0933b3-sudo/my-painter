# 🎨 My Painter 小小畫家

給 4～8 歲孩子的**離線優先 Android 繪畫遊戲**。第一個主題館是超市樂園。

## 已實作的第一個版本

- 🛒 超市六個區域：水果、蔬菜、麵包甜點、飲品乳製品、雜貨、購物玩具。
- 36 個物品都有各自的向量輪廓與分段描線資料。
- 36 色調色盤、自由畫筆、畫筆尺寸、點擊填色、復原。
- 真正記錄手指軌跡並與指定路徑比較，兩種難度的描線判定。
- 使用主圖形遮罩限制自由畫筆，避免超出主輪廓。
- 中文 TTS 語音與提示音，可在家長設定開關。
- 本機自動保存個別作品、畫畫進度；重新開啟可繼續。
- Compose 手機/平板自適應版面；不含廣告、網路與帳戶需求。

> 初版限制：部分物品由數個獨立輪廓組成，畫筆以**第一條封閉主輪廓**作為遮罩。個別開放型線條（如購物車）尚不適合自由塗色。大部分物件可由封閉區域填色。語音效果仰賴裝置是否有可用的繁體中文 TTS。尚未加入 PNG 匯出、貼紙動畫及進階形狀分割。

## 開發方式

JDK 17、Android SDK API 35、Gradle 8.11.1。

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

開發版安裝位置：`app/build/outputs/apk/debug/app-debug.apk`

## 自動發布

推送 `main` 後，GitHub Actions 會：
1. 使用 JDK 17、Gradle 8.11.1 執行單元測試與 Android Build；
2. **正式發布強制使用四個固定簽章 Secrets**；若缺少或金鑰有誤，Workflow 失敗且不取代現有 APK。PR 只產生 1 天有效的 Debug 測試檔。
3. 發布至單一 [latest Release](https://github.com/jimmy0933b3-sudo/my-painter/releases/tag/latest)，每次覆蓋 `my-painter-latest.apk`；
4. **只有新版上傳成功後**才清除舊 Release 與暫存 APK Artifacts。PR Artifacts 最多保留 1 天。

### 設定固定正式簽章（重要）

在 Repository Settings → Secrets and variables → Actions 新增：
- `KEYSTORE_BASE64`：私有 JKS 的 Base64 內容（不可提交到 GitHub）
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

最新 `main` Workflow 會先驗證 JKS、alias、密碼，再使用同一把私鑰簽署 Release APK；透過 `apksigner verify --print-certs` 取得公開證書 SHA-256，並記錄至 GitHub Release 說明。如果之後簽章指紋意外改變，CI 會拒絕覆蓋既有正式版本。妥善備份 JKS 與密碼。

**請注意：**先前下載的 DEV 測試版使用 `com.mypainter.app.debug`，與固定簽章正式版 `com.mypainter.app` 是不同 App；兩者不能直接互相覆蓋，資料也不會自動搬移。首次安裝正式版後，後續只要使用相同 JKS、套件名稱且遞增 versionCode，就可以覆蓋更新。正式版不再使用 Debug APK 備援。

啟動圖示採繪畫主題的彩色畫冊、蘋果、畫筆與調色盤，包含 Android 圓形及自適應圖示。

## 程式碼位置

- `ShopCatalog.kt`：六區 36 品項向量描線資料
- `DrawingCore.kt`：Android Canvas 圖形顯示、描線覆蓋率檢查、區域填色
- `DrawingScreen.kt`：36 色、觸控、模式切換、畫筆
- `ArtworkStore.kt`：本機作品 JSON 儲存、寫入暫存再替換
- `AudioGuide.kt`：TTS、提示聲及家長偏好
- `MainActivity.kt`：首頁、分類、作品集、家長設定

## 後續待開發／優化

- 作品縮圖預覽及 PNG 匯出
- 個別封閉區域精細遮罩與購物車等開放輪廓上色
- 更豐富的卡通音效／預錄台語或國語語音
- 真機及平板測試、描線算法更精準、特殊圖形適配
- 貼紙獎勵與完整遊戲動畫

本儲存庫目前未包含私人簽章金鑰。 
