# FyCore 🚀
Android 生产级核心工具库 | [![](https://img.shields.io/badge/Version-0.1.8-brightgreen)]() [![](https://img.shields.io/badge/License-Apache%202.0-blue)]()

> 模块化设计的 Android 核心能力库，提供导航、存储、UI 组件、校验、状态管理等生产级解决方案

## 📋 目录
- [快速开始](#快速开始)
- [core_feature 模块详解](#core_feature-模块详解)
  - [导航系统 (Navigation)](#导航系统-navigation)
  - [存储工厂 (Storage)](#存储工厂-storage)
  - [校验链 (Validation)](#校验链-validation)
  - [UI 组件集 (Widgets)](#ui-组件集-widgets)
  - [多状态页面 (MultiState)](#多状态页面-multistate)
  - [对话框系统 (Dialogs)](#对话框系统-dialogs)
  - [其他引擎能力](#其他引擎能力)
- [通用工具类](#通用工具类)
- [第三方库集成](#第三方库集成)
- [重要提醒](#重要提醒)

---

## 🚀 快速开始

### Application 初始化
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 核心初始化
        Android.initialize(this, debug = true, "your_app_id")
        
        // 页面追踪（Release 模式自动开启）
        TurboTracker.initialize(this) {
            enable(CoreConfig.Environment.isRelease)
            setLogger(ReleaseLogger())
        }
        
        // 存储工厂初始化
        StorageFactory.initialize {
            type = StorageType.MMKV
            mmkvMode = MMKV.MULTI_PROCESS_MODE
            validateClass = PreferKey::class.java
        }
        
        // 导航系统初始化
        NavigationManager.initialize {
            setListener(object : NavigationListener {
                override fun onNavigationStart(request: NavigationRequest) {}
                override fun onInterceptorExecute(interceptor: NavigationInterceptor, request: NavigationRequest) {}
                override fun onNavigationResult(result: NavigationResult) {}
            })
            
            // 登录拦截器示例
            addInterceptor(
                LoginInterceptor(
                    authProvider = YourAuthProvider(),
                    loginActivityClass = LoginActivity::class,
                    protectedActivities = setOf(ProfileActivity::class)
                )
            )
        }
        
        // 可选配置
        CoreConfig.configure {
            crash {
                allowMultiProcess = true
                afterJumpActivity = MainActivity::class.java
            }
            alert {
                positiveColor = getColor(R.color.primary)
                negativeColor = getColor(R.color.error)
            }
        }
    }
}
```

---

## 📦 core_feature 模块详解

core_feature 是 FyCore 的核心模块，提供完整的 Android 开发基础设施。

### 🧭 导航系统 (Navigation)

**生产级导航拦截器库，支持类型安全、拦截器链、异步导航**

#### 核心特性
- ✅ **类型安全**：泛型 + 编译期检查，避免运行时错误
- ✅ **拦截器链**：支持登录、权限、A/B测试、埋点等拦截
- ✅ **异步支持**：协程安全的异步导航与回调
- ✅ **结果强类型**：Proceed/Redirect/Abort/Error 四种明确语义
- ✅ **全局访问**：任何模块都能使用，无需依赖主 App
- ✅ **生命周期监听**：完整的导航生命周期回调

#### 快速使用
```kotlin
// 类型安全导航
navigator.to<ProfileActivity>(context)
    .with("userId", "123")
    .with("source", "main")
    .go()

// 异步导航
navigator.to<OrderActivity>(context)
    .goAsync { result ->
        when (result) {
            is NavigationResult.Proceed -> handleSuccess()
            is NavigationResult.Redirect -> handleRedirect()
            is NavigationResult.Abort -> handleAbort(result.reason)
            is NavigationResult.Error -> handleError(result.exception)
        }
    }

// 路由导航
navigator.to(context, "profile").with("userId", "123").go()
```

#### 架构组件
- **NavigationManager**：全局单例管理器
- **Navigator**：导航器主入口，Builder 模式
- **InterceptorChain**：拦截器链，按优先级执行
- **NavigationResult**：强类型结果系统
- **NavigationListener**：生命周期监听器
- **RouteRegistry**：路由注册与解析

**📖 完整文档**：[NavReadme.md](modules/core_feature/src/main/java/io/core/nav/NavReadme.md)

---

### 💾 存储工厂 (Storage)

**统一存储接口，支持多种存储引擎与类型安全**

#### 核心特性
- 🔧 **多引擎支持**：MMKV、SharedPreferences
- 🏷️ **注解驱动**：通过 @StorageKey 声明 Key 与默认值
- 🛡️ **类型安全**：编译期检查，避免类型错误
- 🔄 **统一接口**：一套 API 适配多种存储方案

#### 使用示例
```kotlin
// 1. 定义存储 Key（推荐集中管理）
object PreferKey {
    @StorageKey(description = "展示启动动画", defaultValue = "true")
    const val SPLASH_ANIM = "isDisplaySplashAnim"
    
    @StorageKey(description = "用户ID", defaultValue = "")
    const val USER_ID = "userId"
    
    @StorageKey(description = "主题模式", defaultValue = "0")
    const val THEME_MODE = "themeMode"
}

// 2. 使用
val storage = StorageFactory.getStorage()

// 类型安全读取（自动推断类型）
val showSplash = storage.getWithAnnotation<Boolean>(PreferKey.SPLASH_ANIM)
val userId = storage.getWithAnnotation<String>(PreferKey.USER_ID)
val themeMode = storage.getWithAnnotation<Int>(PreferKey.THEME_MODE)

// 写入
storage.put(PreferKey.SPLASH_ANIM, false)
storage.put(PreferKey.USER_ID, "user_123")
```

**📁 源码位置**：`modules/core_feature/src/main/java/io/core/engine/storage/`

---

### ✅ 校验链 (Validation)

**可组合的校验规则与链式验证系统**

#### 核心特性
- 🔗 **链式校验**：多个规则组合，支持短路与全量校验
- 🎯 **内置规则**：邮箱、手机号、长度、正则等常用规则
- 🔧 **自定义规则**：轻松扩展业务校验逻辑
- 📊 **错误收集**：详细的错误信息与定位

#### 使用示例
```kotlin
// 单字段校验
val emailResult = ValidationChain.of("test@example.com")
    .required("邮箱不能为空")
    .email("邮箱格式不正确")
    .validate()

// 多字段校验
val formResult = ValidationAir.create()
    .field("email", email) {
        required("邮箱必填")
        email("邮箱格式错误")
    }
    .field("password", password) {
        required("密码必填")
        minLength(6, "密码至少6位")
        custom { value -> 
            if (!value.contains(Regex("[A-Z]"))) {
                ValidationResult.Error("密码必须包含大写字母")
            } else {
                ValidationResult.Success(value)
            }
        }
    }
    .validateAll()

// 处理结果
when (formResult) {
    is ValidationResult.Success -> submitForm()
    is ValidationResult.Error -> showErrors(formResult.errors)
}
```

**📖 详细文档**：[README.md](modules/core_feature/src/main/java/io/core/engine/validation/README.md)  
**📁 源码位置**：`modules/core_feature/src/main/java/io/core/engine/validation/`

---

### 🎨 UI 组件集 (Widgets)

**生产级自定义 UI 组件库**

#### Layout 容器组件
| 组件名 | 功能描述 | 使用场景 |
|--------|----------|----------|
| `RootLayout` | 带自定义 TitleBar 的根布局 | 统一页面结构 |
| `RatioFrameLayout` | 按比例显示的 FrameLayout | 响应式布局 |
| `FixedScrollView` | 禁止滚动的 ScrollView | 图片顶部对齐裁剪 |
| `CollapsingHeaderLayout` | 可折叠头部布局 | 详情页头部效果 |
| `BlurContainer` | 毛玻璃效果容器 | 现代化 UI 效果 |

#### View 组件
| 组件名 | 功能描述 | 特色功能 |
|--------|----------|----------|
| `AppTitleBar` | 自定义标题栏 | 统一样式，支持各种配置 |
| `LoadingView` | 加载指示器 | iOS 风格，多种动画 |
| `MarqueeTextView` | 跑马灯文本 | 自动滚动，支持暂停 |
| `DrawableTextView` | 支持限定 Drawable 大小的 TextView | 图标文字对齐 |
| `BadgeView` | 角标 TextView | 消息提醒，数字角标 |
| `PressEffectImageView` | 按压效果的 ImageView | 交互反馈 |
| `FastScrollRecyclerView` | 快速滚动的 RecyclerView | 大列表快速定位 |

#### 使用示例
```kotlin
// 自定义 TitleBar
appTitleBar.apply {
    setTitle("个人中心")
    setLeftIcon(R.drawable.ic_back) { finish() }
    setRightText("编辑") { startEditMode() }
}

// 比例布局
ratioFrameLayout.setRatio(16, 9) // 16:9 比例

// 加载视图
loadingView.apply {
    setLoadingText("加载中...")
    startLoading()
}
```

**📁 源码位置**：`modules/core_feature/src/main/java/io/core/widget/`

---

### 🔄 多状态页面 (MultiState)

**页面状态切换与占位管理**

#### 核心特性
- 📱 **多状态支持**：Loading、Empty、Error、Content
- 🎨 **自定义布局**：每种状态都可自定义
- 🔧 **简单集成**：一行代码集成到现有页面
- 🎯 **状态绑定**：与数据加载状态自动同步

#### 使用示例
```kotlin
// 1. 在布局中使用
<io.core.engine.multi_state.MultiStateContainer
    android:id="@+id/multiStateContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- 内容布局 -->
    <RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        
</io.core.engine.multi_state.MultiStateContainer>

// 2. 代码中控制状态
multiStateContainer.apply {
    showLoading("加载中...")
    
    // 加载成功
    showContent()
    
    // 加载失败
    showError("网络错误", "重试") { 
        loadData() 
    }
    
    // 空数据
    showEmpty("暂无数据", "刷新") { 
        refresh() 
    }
}

// 3. 自定义状态页面
MultiStateConfig.apply {
    setLoadingLayout(R.layout.custom_loading)
    setEmptyLayout(R.layout.custom_empty)
    setErrorLayout(R.layout.custom_error)
}
```

**📁 源码位置**：`modules/core_feature/src/main/java/io/core/engine/multi_state/`

---

### 💬 对话框系统 (Dialogs)

**统一对话框 API 与样式适配**

#### 核心特性
- 🎨 **统一样式**：Material Design 风格
- 🔧 **链式调用**：简洁的 API 设计
- 📱 **多种类型**：Alert、Selector、Custom
- 🎯 **自动适配**：不同 Android 版本样式统一

#### 使用示例
```kotlin
// 1. 简单提示对话框
alert("提示", "确定要删除这条记录吗？") {
    positiveButton("确定") { 
        deleteRecord() 
    }
    negativeButton("取消")
}

// 2. 单选对话框
selector("选择主题", listOf("浅色", "深色", "跟随系统")) { _, index ->
    when (index) {
        0 -> setLightTheme()
        1 -> setDarkTheme()
        2 -> setAutoTheme()
    }
}

// 3. 自定义对话框
AlertBuilder(this)
    .setTitle("自定义对话框")
    .setView(R.layout.custom_dialog)
    .setPositiveButton("确定") { dialog, _ ->
        // 处理确定逻辑
        dialog.dismiss()
    }
    .show()
```

**📁 源码位置**：`modules/core_feature/src/main/java/io/core/engine/dialogs/`

---

### 🔧 其他引擎能力

#### BRV 扩展
**对 BRV 列表库的适配封装**
- 默认分割线与动画
- BindingAdapter 适配
- 快捷刷新布局
- **源码**：`modules/core_feature/src/main/java/io/core/engine/brv/`

#### 键盘监听 (Keyboard)
**软键盘全局监听与回调**
```kotlin
KeyboardObserver.register(this) { isVisible, height ->
    if (isVisible) {
        // 键盘弹起
        adjustLayout(height)
    } else {
        // 键盘收起
        resetLayout()
    }
}
```
**源码**：`modules/core_feature/src/main/java/io/core/engine/keyboard/`

#### 事件总线 (LiveBus)
**跨组件事件通信**
```kotlin
// 发送事件
LiveEventBus.get("user_login").post(userInfo)

// 接收事件
LiveEventBus.get("user_login", UserInfo::class.java)
    .observe(this) { userInfo ->
        updateUI(userInfo)
    }
```
**源码**：`modules/core_feature/src/main/java/io/core/engine/livebus/`

#### Shape 构建器
**动态创建形状 Drawable**
```kotlin
val drawable = DrawableBuilder
    .setRadius(12f)
    .setSolidColor(Color.BLUE)
    .setStroke(2, Color.RED)
    .build()
```
**源码**：`modules/core_feature/src/main/java/io/core/engine/shape/`

---

## 🛠️ 通用工具类详解

### 📁 io.core.common.helper - 核心助手类

#### JsonUltra - 强大的 JSON 解析与构建工具
**功能**：支持路径访问、类型安全转换、动态构建的 JSON 工具类
```kotlin
// 复杂 JSON 构建
val jsonString = JsonUltra.build {
    "library" obj {
        "name" with "Central Library"
        "books" array {
            plusAssign(mapOf("title" to "Kotlin Coroutines", "year" to 2023))
            plusAssign(mapOf("title" to "Android Development", "year" to 2024))
        }
        "features" with listOf("wifi", "cafe", "24h")
    }
    "author" with "yuan"
}

// 路径访问与类型转换
val parser = JsonUltra.parse(jsonString)
val bookTitle = parser["library.books[1].title"]?.asString() // "Android Development"
val features = parser["library.features"]?.asList { it.asString() } // ["wifi", "cafe", "24h"]

// 嵌套 JSON 字符串自动解析
val nestedJson = JsonUltra.parse("{\"key\": \"{\\\"nested\\\": 1234}\"}")
val nestedValue = nestedJson["key.nested"]?.asInt() // 1234

// JSON 格式化
val formatted = JsonUltra.format("{\"name\":\"张三\",\"age\":18}")
```

#### ReflectHelper - 链式反射工具
**功能**：简化反射操作，支持链式调用、缓存优化、类型安全
```kotlin
// 创建实例并链式调用
val result = ReflectHelper.on("com.example.MyClass")
    .newInstance("param1", 123)
    .chainInvoke("setName", "test")
    .chainInvoke("process")
    .get<String>()

// 静态方法调用
val staticResult = ReflectHelper.on("java.lang.System")
    .chainInvoke("getProperty", "java.version")
    .get<String>()

// 字段操作
ReflectHelper.with(myObject)
    .setField("privateField", "newValue")
    .getField("anotherField")
```

#### AppTrackV2 - 应用生命周期追踪管理器
**功能**：全局生命周期监听、内存优化、性能统计、安全 UI 操作
```kotlin
// 注册前后台切换监听
AppTrackV2.registerAppStatusListener("main") { isForeground ->
    if (isForeground) {
        // 应用进入前台
        resumeOperations()
    } else {
        // 应用进入后台
        pauseOperations()
    }
}

// Activity 转换监听
AppTrackV2.registerActivityTransitionListener("nav") { activity, event ->
    when (event) {
        ActivityTransitionEvent.ENTER -> logPageEnter(activity)
        ActivityTransitionEvent.EXIT -> logPageExit(activity)
    }
}

// 安全 UI 操作（自动处理生命周期）
AppTrackV2.showToastSafely(activity, "操作成功")
AppTrackV2.showDialogSafely(activity) { ctx -> 
    AlertDialog.Builder(ctx).setMessage("确认操作？").create()
}

// Activity 时间追踪
AppTrackV2.trackActivityTime(activity, "user_profile_page")

// 获取当前状态
val topActivity = AppTrackV2.getTopActivity()
val hasMainActivity = AppTrackV2.hasActivity(MainActivity::class.java)
val activeCount = AppTrackV2.aliveActivityCount()
```

#### 其他 Helper 类
- **TimeoutHandler**：超时处理机制，适用于 FFmpeg 进度回调等场景
- **TurboTracker**：页面追踪与日志输出，可单独在 Release 环境开启
- **AESTurbo**：AES 加密解密工具
- **LocationFetcher**：位置获取助手
- **StatusBarManager**：状态栏管理工具
- **TryCatchTurbo**：异常处理增强工具

---

### 📁 io.core.common.util - 实用工具类

#### MediaScanner - 媒体库扫描工具
**功能**：高性能媒体文件查询，支持缓存、过滤、多种文件类型
```kotlin
// 查询图片文件
val imageFiles = MediaScanner.queryFiles(setOf(
    MediaScanner.MediaFileType.JPG,
    MediaScanner.MediaFileType.PNG,
    MediaScanner.MediaFileType.WEBP
))

// 查询视频文件并自定义过滤
val videoFiles = MediaScanner.queryFiles(
    types = setOf(MediaScanner.MediaFileType.MP4, MediaScanner.MediaFileType.AVI),
    addFilter = { it.size > 1024 * 1024 }, // 大于1MB
    sortOrder = MediaStoreClauses.timeAddedDESC
)

// 查询文档文件
val docFiles = MediaScanner.queryFiles(
    types = setOf(
        MediaScanner.MediaFileType.PDF,
        MediaScanner.MediaFileType.DOCX,
        MediaScanner.MediaFileType.TXT
    ),
    forceRefresh = true // 强制刷新缓存
)

// 注册内容变化监听
MediaScanner.registerContentObserver()
```

#### TaskExecutor - 并发任务执行器
**功能**：协程与线程双模式并发执行，支持超时、进度回调、异常处理
```kotlin
// Kotlin 协程模式
val tasks = listOf<suspend () -> String>(
    { fetchUserData() },
    { fetchOrderData() },
    { fetchProductData() }
)

TaskExecutor.get().execute(
    tasks = tasks,
    onEachComplete = { result, index -> 
        updateProgress(index + 1, tasks.size)
    },
    onComplete = { results ->
        displayResults(results)
    },
    onError = { error, partialResults ->
        handleError(error, partialResults)
    },
    timeoutMillis = 30_000L
)

// Java 线程模式
val javaTasks = listOf<TaskExecutor.ProcessorTask<String>>(
    { processData1() },
    { processData2() }
)

TaskExecutor.get().execute(
    tasks = javaTasks,
    callback = object : TaskExecutor.ConcurrentCallback<String> {
        override fun onComplete(results: SortedMap<Int, String>) {
            // 处理完成结果
        }
        override fun onError(e: Throwable) {
            // 处理异常
        }
        override fun onProgress(completed: Int, total: Int) {
            // 更新进度
        }
    }
)
```

#### Tools 工具类集合

**ColorTools - 颜色处理工具**
```kotlin
// 颜色操作
val randomColor = ColorTools.getRandomColor()
val isLight = ColorTools.isColorLight(color)
val hexString = ColorTools.intToString(color) // "#FF5722"

// 颜色调整
val darkerColor = ColorTools.darkenColor(originalColor)
val lighterColor = ColorTools.lightenColor(originalColor)
val invertedColor = ColorTools.invertColor(originalColor)
val alphaColor = ColorTools.withAlpha(baseColor, 0.5f)

// 颜色混合
val blendedColor = ColorTools.blendColors(color1, color2, 0.3f)
val colorDifference = ColorTools.getColorDifference(color1, color2)
```

**FileTools - 文件操作工具**
```kotlin
// 文件创建与管理
val file = FileTools.createFileIfNotExist("/storage/data/test.txt")
val folder = FileTools.createFolderIfNotExist("/storage/data/images")

// 文件操作
FileTools.copy("/source/file.txt", "/dest/file.txt")
FileTools.move("/old/path.txt", "/new/path.txt")
FileTools.delete("/temp/cache", deleteRootDir = true)

// 文件读写
val content = FileTools.readText("/data/config.txt")
FileTools.writeText("/data/output.txt", "Hello World")
val bytes = FileTools.readBytes("/data/image.jpg")

// 文件信息
val size = FileTools.getSize("/data/video.mp4") // "125.6 MB"
val extension = FileTools.getExtension("document.pdf") // "pdf"
val mimeType = FileTools.getMimeType("image.jpg") // "image/jpeg"
val dateTime = FileTools.getDateTime("/data/file.txt", "yyyy-MM-dd HH:mm")

// 文件列表
val files = FileTools.listFiles("/storage/downloads", 
    allowExtensions = arrayOf("jpg", "png", "gif"))
val sortedFiles = FileTools.listFiles("/storage/music", 
    sortType = FileTools.BY_SIZE_DESC)
```

**TimeTools - 时间处理工具**
```kotlin
// 时间格式化
val now = TimeTools.getNowString(TimePatterns.TIME_FULL) // "2023-10-12 15:30:45"
val dateOnly = TimeTools.getDateString(-1, TimePatterns.DATE_YMD) // 昨天日期

// 时间转换
val timestamp = TimeTools.string2Millis("2023-10-12", TimePatterns.DATE_YMD)
val dateStr = TimeTools.millis2String(timestamp, TimePatterns.DATE_YMD_ZH)

// 时间计算
val daysDiff = TimeTools.daysBetween(startTime, endTime)
val age = TimeTools.getAge(birthdayTimestamp)
val isToday = TimeTools.isToday(someTimestamp)
val friendlyTime = TimeTools.getFriendlyTime(timestamp) // "今天 15:30"

// 时间范围
val startOfDay = TimeTools.getStartOfDay()
val startOfWeek = TimeTools.getStartOfWeek()
val startOfMonth = TimeTools.getStartOfMonth()

// 格式转换
val converted = TimeTools.convertDateFormat("2023-10-12", TimePatterns.DATE_YMD) to TimePatterns.DATE_YMD_ZH
val season = TimeTools.getSeason() // Season.AUTUMN
val isLeapYear = TimeTools.isLeapYear(2024)
```

#### 其他 Util 工具类
- **DiveGestureLine**：全面屏手势小白条适配
- **FileSharer**：跨应用文件分享工具
- **Preferences**：用户首选项管理
- **Toaster**：Toast 显示工具
- **Once**：一次性操作控制器
- **CoreUtil**：核心工具方法集合

---

### 📁 io.core.constant - 常量定义

#### AndroidVersion - Android 版本常量
```kotlin
// 版本判断
if (Build.VERSION.SDK_INT >= ANDROID_13) {
    // Android 13+ 特性
}
if (Build.VERSION.SDK_INT >= ANDROID_11) {
    // Android 11+ 特性
}

// 支持的版本常量
ANDROID_16, ANDROID_15, ANDROID_14, ANDROID_13, ANDROID_12_L, ANDROID_12,
ANDROID_11, ANDROID_10, ANDROID_9, ANDROID_8_1, ANDROID_8, ANDROID_7_1,
ANDROID_7, ANDROID_6, ANDROID_5_1, ANDROID_5, ANDROID_4_4...
```

#### FileSize - 文件大小与时间格式化
```kotlin
// 文件大小格式化
val sizeStr = FileSize.format(1536000L) // "1.46 MB"
val customSize = FileSize.format(
    size = 2048L,
    unitStyle = FileSize.SizeUnitStyle.StandardLower, // "kb"
    precision = 1,
    minUnit = FileSize.SizeUnit.KB
) // "2.0 kb"

// 时间间隔格式化
val duration = FileSize.formatDuration(125000L) // "2m 5s"
val chineseDuration = FileSize.formatDuration(
    millis = 3665000L,
    unitStyle = FileSize.TimeUnitStyle.LONG_CHINESE
) // "1小时 1分钟 5秒"

// 自定义格式
val timePattern = FileSize.formatDuration(125000L, "mm:ss") // "02:05"

// 扩展函数
val fileSize = 1024000L.toFormattedFileSize() // "1000.00 KB"
val timeDuration = 65000L.toFormattedDuration() // "1m 5s"

// 解析文件大小
val bytes = FileSize.parseToBytes("1.5MB") // 1572864L
```

#### FileType - 文件 MIME 类型映射
```kotlin
// MIME 类型解析
val mimeType = FileType.mimeTypeOf("document.pdf") // "application/pdf"
val imageMime = FileType.resolveMimeType("jpg") // "image/jpeg"

// 获取所有 MIME 类型
val allMimes = FileType.resolveAllMimeTypes("mp4") 
// ["video/mp4"]

// 反向查找扩展名
val extensions = FileType.resolveExtensions("image/jpeg") 
// [".jpg", ".jpeg"]

// 支持检查
val isSupported = FileType.isExtSupported("webp") // true

// 支持的文件类型
// 图片：jpg, png, gif, bmp, webp, tiff, svg, ico, heic, psd
// 视频：mp4, avi, mkv, mov, wmv, 3gp, m4v, mpeg, rmvb
// 音频：mp3, wav, aac, flac, m4a, ogg, wma, midi
// 文档：pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv, rtf, epub
// 压缩：zip, rar, 7z, tar, gz, iso, dmg
// 代码：java, kt, js, py, cpp, go, swift, rs, php, sql, xml, json
```

#### TimePatterns - 时间格式模式常量
```kotlin
// 国际标准格式
TimePatterns.DATE_TIME_ISO_8601     // "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
TimePatterns.DATE_TIME_RFC_822      // "dd MMM yyyy HH:mm:ss Z"
TimePatterns.DATE_ONLY_ISO          // "yyyy-MM-dd"

// 中文格式
TimePatterns.DATE_YMD_ZH            // "yyyy年MM月dd日"
TimePatterns.TIME_FULL              // "yyyy-MM-dd HH:mm:ss"

// 特殊用途格式
TimePatterns.FILE_SAFE_TIMESTAMP    // "yyyyMMdd_HHmmss"
TimePatterns.LOG_TIMESTAMP          // "[yyyy-MM-dd HH:mm:ss.SSS]"
TimePatterns.CHAT_MESSAGE_STYLE     // "MMM dd HH:mm"

// 获取格式化器
val formatter = TimePatterns.getFormatter(TimePatterns.TIME_FULL)
val dateStr = formatter.format(Date())
```

#### 其他常量类
- **DeviceOS**：设备系统版本信息
- **CoreConst**：核心常量定义
- **MediaStoreClauses**：MediaStore 查询条件语句
- **TimeFormat**：时间格式注解

---

### 🔧 扩展函数与工具

#### Extensions 扩展函数包
- **Any.kt**：通用对象扩展
- **SystemServices.kt**：系统服务快捷访问
- **UI 扩展**：视图操作、颜色处理、尺寸转换
- **Cool 扩展**：实用工具扩展函数

#### Log 日志系统
- **LogPure**：纯净日志输出
- **LogCat**：增强日志工具
- **LogExt**：日志扩展功能
- **LogHook**：日志钩子机制

#### 并发工具
- **Concurrency**：并发控制工具
- **TaskExecutorV2**：任务执行器增强版
- **ThreadUltra**：线程管理工具

#### 网络与分享
- **NetworkTools**：网络状态检测
- **FileSharer**：文件分享工具
- **ShareAir**：分享功能封装

这些工具类构成了 FyCore 的核心基础设施，为 Android 开发提供了完整的工具链支持。每个工具类都经过生产环境验证，具备高性能、易用性和可靠性特点。

---

## 📚 第三方库集成

| 库名 | 版本 | 用途 | 文档链接 |
|------|------|------|----------|
| BRV | Latest | RecyclerView 适配器 | [GitHub](https://github.com/liangjingkanji/BRV) |
| LiveEventBus | Latest | 事件总线 | [GitHub](https://github.com/JeremyLiao/LiveEventBus) |
| ShapeView | Latest | 形状视图 | [GitHub](https://github.com/getActivity/ShapeView) |
| StateLayout | Latest | 状态布局 | [GitHub](https://github.com/liangjingkanji/StateLayout) |
| MultiStatePage | Latest | 多状态页面 | [GitHub](https://github.com/Zhao-Yan-Yan/MultiStatePage) |
| Retry | Latest | 重试机制 | [GitHub](https://github.com/zj565061763/retry-ktx) |

---

## ⚠️ 重要提醒

### 初始化顺序
1. **Android.initialize()** - 核心初始化（必须最先）
2. **TurboTracker.initialize()** - 页面追踪
3. **StorageFactory.initialize()** - 存储工厂
4. **NavigationManager.initialize()** - 导航系统
5. **其他模块初始化**

### 最佳实践
- **BRV 使用**：搭配 DataBinding 时需设置 `BRV.modelId = BR.m`
- **事件总线**：推荐使用 `postEvent` 与 `observeEvent` 方法对
- **存储管理**：统一在 `PreferKey` 对象中定义存储键
- **导航拦截**：按优先级设置拦截器（权限 50 → 登录 100 → 埋点 200）

### 架构建议
- **模块化**：各功能模块独立，通过接口通信
- **依赖注入**：使用接口抽象，便于测试与替换
- **生命周期**：注意 Activity/Fragment 生命周期管理
- **内存管理**：及时释放资源，避免内存泄漏

---

## 📄 许可证
```
Copyright 2025 FyCore

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.