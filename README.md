# 跨國匯率即時轉換 App

一個兼具『即時轉換』、『主動背景提醒』、『流暢硬體互動』與『資料視覺化』的 Android 原生應用程式。
在現今高度全球化的經濟環境中，無論是海外網購、出國旅遊，或進行 ETF 投資，即時且精準的匯率資訊皆是不可或缺的決策依據。本專案將匯率資訊從傳統的「被動查詢」轉變為「主動提醒服務」，並透過多重非同步請求呈現真實匯率走勢，全面提升金融資訊取得的即時性與效率。

---
## 專案特色

* **即時匯率精準換算**：支援 16 種常用貨幣，透過 Retrofit 2 + Gson 串接第三方 API (open.er-api.com) 提供零時差計算。限制輸入格式 (numberDecimal) 確保金流數據正確。
* **個人化最愛貨幣**：使用 `SharedPreferences` 實作一鍵加入最愛（標示 ⭐）功能，自動將常用貨幣置頂於選擇清單，打造專屬且高效的換匯體驗。
* **歷史走勢視覺化與多重非同步處理**：整合 `MPAndroidChart` 繪製匯率走勢線圖。透過串接開源全貨幣 API (fawazahmed0)，實作多重非同步網路請求 (Multiple Async Requests) 拉取過去 7 天的真實歷史數據，直觀呈現貨幣波動趨勢。
* **背景監控與主動推播**：導入 WorkManager 實作背景自動排程比對，達標時即時觸發 Android Notification，讓使用者無需開啟 App 也能掌握最佳換匯先機。
* **財經資訊無縫整合**：內建 WebView 模組嵌入財經新聞與即時匯率看板（如台灣銀行網頁），避免跳轉外部瀏覽器造成的體驗中斷。
* **直覺化硬體互動**：結合手機感測器 (Accelerometer)，實作「搖晃手機即清空所有輸入資料與結果」，提供直覺的 Reset 體驗。
* **雲端大數據追蹤**：整合 Firebase Firestore，將轉換紀錄與關注貨幣同步至雲端，為未來的個人化匯率趨勢分析奠定基礎。

---
## 專案架構

### 1. 專案目錄結構

```text
exchangerateapp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/exchangerateapp/
│   │   │   ├── api/                        ← Retrofit 客戶端與介面 (ExchangeRateApi, HistoryApi)
│   │   │   ├── model/                      ← 資料模型 (POJO)
│   │   │   ├── worker/                     ← WorkManager 背景任務
│   │   │   ├── utils/                      ← SharedPreferences, Firebase 等工具類別
│   │   │   ├── MainActivity.java           ← 主畫面 (包含即時換算與最愛功能)
│   │   │   ├── ChartActivity.java          ← 歷史匯率走勢圖表頁面
│   │   │   ├── AlertActivity.java
│   │   │   ├── WebViewActivity.java
│   │   │   └── HistoryActivity.java
│   │   ├── res/                            ← XML 佈局、選單與資源檔
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── google-services.json                ← Firebase 設定檔
├── build.gradle (Project)
├── settings.gradle
└── gradle.properties
```

### 2. 技術架構

本專案基於 Android 原生開發環境（純 Java，Minimum SDK 24），採用 Empty Views Activity 架構。

### 技術堆疊 (簡易 MVVM)
* **View**：Activity + XML Layout
* **Model**：POJO 類別 (`ExchangeResponse`, `ConversionRecord`)
* **Network**：Retrofit 2 + OkHttp + Gson (支援多 API 端點與 JsonObject 動態解析)
* **Persistence**：SharedPreferences (最愛名單) + Firebase Firestore (雲端紀錄)
* **Background**：WorkManager (PeriodicWorkRequest)
* **Data Visualization**：MPAndroidChart
* **Sensor**：SensorManager (加速度計)

### API 資訊
**【即時匯率】**
- **API 名稱**：Exchange Rate API (免費公開版本)
- **Base URL**：`https://open.er-api.com/`
- **Endpoint**：`GET /v6/latest/{base_currency}`
- **更新頻率**：約每 30 分鐘更新一次
- **支援貨幣**：包含 USD, TWD, JPY, EUR 等 16 種常用貨幣。

**【歷史匯率】**
- **API 名稱**：Fawazahmed0 Currency API (開源全貨幣版本)
- **Base URL**：`https://cdn.jsdelivr.net/`
- **Endpoint**：`GET npm/@fawazahmed0/currency-api@{date}/v1/currencies/{base_currency}.json`
- **更新頻率**：每日更新
- **支援貨幣**：支援全球超過 150 種法定貨幣及加密貨幣（完整涵蓋 TWD 等亞洲貨幣，且代碼需轉換為小寫格式）。

---

## 詳細功能實作說明

### 功能一：即時匯率換算與最愛名單（MainActivity）

1. 使用 Spinner 選擇來源與目標貨幣，結合 `SharedPreferences` 記憶使用者最愛貨幣並自動置頂排列。
2. 點擊換算後呼叫 Retrofit 非同步請求進行換算，並提供「⇅ 互換」按鈕快速交換 Spinner 選項。


### 功能二：背景監控與推播（AlertActivity + RateCheckWorker）

- **自訂監控目標**：使用 `SharedPreferences` 儲存使用者設定的目標幣別與期望匯率。
- **背景自動排程**：導入 `WorkManager` 建立週期性任務，每 15 分鐘在背景透過 Retrofit 同步呼叫 (`.execute()`) 獲取最新 API 數據。
- **即時達標推播**：當最新匯率達到設定目標時，即時觸發 Android 8.0+ 支援的 Notification Channel 發送高優先級推播，讓使用者無需開啟 App 也能掌握最佳換匯時機。


### 功能三：財經資訊瀏覽（WebViewActivity）

* `TabLayout` 結合 `WebView`，快速切換台灣銀行、Google 財經、Yahoo 外匯。
* 實作 `WebViewClient` 攔截連結防止跳轉至外部瀏覽器，並透過 `WebChromeClient` 顯示載入進度。

### 功能四：搖晃清除（感測器互動）

利用 `Sensor.TYPE_ACCELEROMETER` 偵測裝置搖晃，若加速度大於 12.0f 閾值即清空輸入與計算結果。
*註：於 `onResume()` 註冊並在 `onPause()` 解除註冊以節省電量。*

### 功能五：雲端資料儲存與歷史紀錄（Firebase + HistoryActivity）

* **自動儲存**：每次成功換算自動存入 Firestore。使用匿名 UUID 作為 `userId`，免登入即可使用。
* **資料結構**：`users/{userId}/conversions/{docId}` (包含幣別、金額、匯率與時間戳記)。
* **歷史查詢**：`HistoryActivity` 透過 ListView 撈取並顯示最近 20 筆換算紀錄。

### 功能六：匯率走勢圖表（ChartActivity）

* 導入 `MPAndroidChart` 套件，實作 `LineChart` 折線圖。
* 實作迴圈發送 7 次非同步 API 請求，並透過計數器攔截所有回傳結果，將真實歷史匯率數據動態轉換為圖表資料點 (`Entry`) 進行視覺化。

---

## 開發環境與主要套件

* **IDE**：Android Studio Ladybug
* **語言**：純 Java
* **Min / Target SDK**：24 / 35
* **主要依賴**：Retrofit 2, Gson, Firebase Firestore, WorkManager, MPAndroidChart, Material Components

---

##  安裝與執行

1. Clone 或下載本專案原始碼。
2. 將對應的 `google-services.json` 放入 `app/` 目錄中。
3. Sync Gradle 確認依賴套件下載完成。
4. 部署至模擬器或實機執行（Android 13+ 設備請留意並允許通知權限）。

*注意：模擬器需支援感測器（Virtual Sensors）設定以測試搖晃功能。若有正式上線需求，建議至官方申請專屬 API Key。*

---

**開發者**：XCY

