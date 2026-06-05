# 跨國匯率即時轉換 App

一個兼具『即時轉換』、『主動背景提醒』、『動態資料視覺化』與『沉浸式趣味互動』的 Android 原生應用程式。
在現今高度全球化的經濟環境中，無論是海外網購、出國旅遊，或進行 ETF 投資，即時且精準的匯率資訊皆是不可或缺的決策依據。本專案打破傳統金融工具冷冰冰的刻板印象，除了透過 Retrofit 與雙重非同步 API 提供零時差的匯率計算，更導入了**統一基準值的「動態購買力指標」**、**基於歷史趨勢的「迷因系推播」**，以及**純 Native Code 實作的「黃金彩帶雨」視覺特效**，全面提升金融資訊取得的效率即時性與趣味性。

---
## 🚀 專案核心特色

* **即時匯率精準換算與最愛名單**：支援 16 種常用貨幣，透過 Retrofit 2 + Gson 串接即時 API ，提供零時差計算。
* **個人化最愛貨幣**：使用 `SharedPreferences` 實作一鍵加入最愛（標示 ⭐）功能，自動將常用貨幣置頂於選擇清單，打造專屬且高效的換匯體驗。
* **歷史走勢視覺化與多重非同步處理**：整合 `MPAndroidChart` 繪製匯率走勢線圖。透過串接開源全貨幣 API (fawazahmed0)，實作多重非同步網路請求 (Multiple Async Requests) 拉取過去 7 天的真實歷史數據，直觀呈現貨幣波動趨勢。
* **智慧背景監控與動態推播**：導入 WorkManager 實作每 15 分鐘的背景自動排程比對。當目標匯率達標時，系統不僅會即時觸發 Android 高優先級推播，讓使用者無需開啟 App 也能掌握最佳換匯先機；更會在背景同步抓取「昨日歷史匯率」進行交叉比對，根據真實的漲跌趨勢，發送專屬的「情緒化 / 迷因系」動態文案。
* **統一基準之動態購買力指標**：系統自動將輸入面額透過交叉匯率轉換為「等值新台幣 (TWD)」作為統一基準。並根據資產總值動態切換四個級距（平民、小資、土豪），顯示相應的民生物價指標（如大麥克、iPhone、特斯拉），避免匯率面額差異導致的價值失真。
* **純原生「財富自由」粒子特效**：捨棄外部依賴圖檔，導入 `Konfetti` 開源庫實作純程式碼渲染的粒子系統。當換算總值真實達到「一百萬台幣」時，觸發滿版黃金彩帶雨特效。
* **無破壞性長按趨勢彩蛋**：長按換算結果即可觸發非同步趨勢分析，自動比對今日與昨日匯率，彈出即時的趨勢解讀與趣味嘲諷，且不會清空使用者已輸入的資料。
* **雲端大數據追蹤**：整合 Firebase Firestore，將轉換紀錄同步至雲端，使用匿名 UUID 免登入即可使用，為未來的個人化匯率趨勢分析奠定基礎。
* **財經資訊無縫整合**：內建 WebView 模組嵌入財經新聞與即時匯率看板（如台灣銀行網頁），避免跳轉外部瀏覽器造成的體驗中斷。
* **直覺化硬體互動**：結合手機感測器 (Accelerometer)，實作「搖晃手機即清空所有輸入資料與結果」，提供直覺的 Reset 體驗。

---
## 📂 專案架構與技術棧

### 1. 專案目錄結構

```text
exchangerateapp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/exchangerateapp/
│   │   │   ├── api/                        ← Retrofit 客戶端與介面 (ExchangeRateApi, HistoryApi)
│   │   │   ├── model/                      ← 資料模型 (POJO)
│   │   │   ├── worker/                     ← WorkManager 背景任務 (RateCheckWorker)
│   │   │   ├── utils/                      ← SharedPreferences, Firebase 等工具類別
│   │   │   ├── MainActivity.java           ← 主畫面 (包含即時換算與最愛功能、動態卡片與 Konfetti 特效)
│   │   │   ├── ChartActivity.java          ← 歷史匯率走勢圖表頁面
│   │   │   ├── AlertActivity.java          ← 背景推播目標設定頁面
│   │   │   ├── WebViewActivity.java        ← 內建財經資訊網頁
│   │   │   └── HistoryActivity.java        ← 雲端歷史紀錄頁面
│   │   ├── res/                            ← XML 佈局、選單與資源檔 (採用 FrameLayout + ScrollView 支援懸浮特效)
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
* **Data Visualization & UI**：MPAndroidChart (數據圖表), Konfetti (粒子特效)
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
- **應用場景**：走勢圖表繪製、長按趨勢彩蛋分析、背景推播動態文案判定。

---

## 詳細功能實作說明

### 功能一：即時匯率換算與最愛名單（MainActivity）
1. 使用 Spinner 選擇來源與目標貨幣，結合 `SharedPreferences` 記憶使用者最愛貨幣並自動置頂排列。
2. 點擊換算後呼叫 Retrofit 非同步請求進行換算，並提供「⇅ 互換」按鈕快速交換 Spinner 選項。

### 功能二：背景監控與推播（AlertActivity + RateCheckWorker）
- **自訂監控目標**：使用 `SharedPreferences` 儲存使用者設定的目標幣別與期望匯率。
- **背景自動排程**：導入 `WorkManager` 建立週期性任務，每 15 分鐘在背景透過 Retrofit 同步呼叫 (`.execute()`) 獲取最新 API 數據。
- **即時達標推播**：當最新匯率達到設定目標時，即時觸發 Android 8.0+ 支援的 Notification Channel 發送高優先級推播，讓使用者無需開啟 App 也能掌握最佳換匯時機。
- **雙重 API 交叉比對**：達標時，背景 Worker 會同步發起第二次請求抓取「昨日歷史匯率」，並將兩者比較，決定推播通知要給予「看漲警告」或「看跌買進」的專屬迷因文案。

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

### 功能七：動態購買力指標 (TWD 價值基準)
有別於傳統金融工具只看「輸入面額」，本專案建立了一套嚴謹的資產評估邏輯。換算後系統會在背景將任意貨幣轉換為「新台幣 (TWD) 總值」，並依照以下動態級距顯示對應的趣味購買力卡片：
| 級距 | 條件 (等值台幣) | 顯示指標物品 |
| :--- | :--- | :--- |
| **隱藏** | `< 50 元` | (無，隱藏卡片防呆，避免出現 0.01 杯珍奶) |
| **平民級** | `50 ~ 9,999 元` | 大麥克、五十嵐珍奶、電影票 |
| **小資級** | `10,000 ~ 999,999 元` | 環球影城門票、最新 iPhone、全新機車 |
| **土豪級** | `≥ 1,000,000 元` | Tesla Model 3、勞力士黑水鬼、頂級和牛大餐 |

### 功能八：純原生百萬粒子特效 (Konfetti)
* 透過 FrameLayout 覆蓋於 ScrollView 之上的排版設計，結合 Konfetti 原生粒子引擎。
* 當真實價值達到「一百萬台幣」時，自動於畫面上層渲染 360 度黃金彩帶雨，無須依賴龐大的 JSON 動畫檔，兼顧效能與華麗的 UX 體驗。

### 功能九：無破壞性長按彩蛋與趨勢判斷
* 使用者長按換算結果即可觸發彩蛋。系統會啟動非同步任務呼叫歷史 API 取得昨日匯率，比對今日最新數據後，以 Toast 彈出帶有情緒化的真實趨勢解讀（如：「📉 正在下跌！大特價中...」），提供直覺的決策輔助且不干擾原有的換算進度。


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

