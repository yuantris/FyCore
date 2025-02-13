## FyCore
> 项目初始化

```kotlin

Android.initialize(this)
// 设置重启对象Activity(Crash设置)
Android.homeActivity = MainActivity::class.java
```

**模块概览**
- 应用配置
  - 首页toolbar连点3次，第4次长按打开应用配置设置
    - 启动动画设置
    - 崩溃信息

**模块功能**
- ColorUtils
  - getRandomColor
  - isColorLight
  - intToString
  - stripAlpha
  - shiftColor
  - darkenColor
  - lightenColor
  - invertColor
  - adjustAlpha
  - withAlpha
- DocumentUtils
- FileUtils
- TimeUtils

- 常用拓展函数速览
  - 

- 三方库
  - BRV (https://github.com/liangjingkanji/BRV)
  - LiveEventBus (https://github.com/michaellee123/LiveEventBus)
  - ShapeView (https://github.com/getActivity/ShapeView)
  - StateLayout (https://github.com/liangjingkanji/StateLayout)
  - 

- AppLifecycleTracker
  - Lifecycle管理器,管理项目中Activity、service的状态
  
- Coroutine使用
  ```kotlin
  /**
  val coroutine = Coroutine.async(
    scope = CoroutineScope(Dispatchers.IO), // 指定作用域，默认为 MainScope()
    context = Dispatchers.Default,          // 指定执行上下文，默认为 Dispatchers.IO
    start = CoroutineStart.LAZY,            // 指定启动选项，默认为 CoroutineStart.DEFAULT
    executeContext = Dispatchers.Main,      // 指定回调执行上下文，默认为 Dispatchers.Main
    block = { /* 协程执行的代码块 */ }
  )
  */
  val coroutine = Coroutine.async(
    scope = CoroutineScope(Dispatchers.IO),
    block = {
        delay(3000) // 模拟耗时操作
        "任务完成"
    }
  )
  coroutine.timeout(5000) // 设置超时时间为 5 秒
  coroutine.onErrorReturn("任务失败")
  
  coroutine.onStart {
      println("协程已启动")
  }
  
  coroutine.onSuccess { result ->
      println("协程成功完成，结果为: $result")
  }
  
  coroutine.onError { throwable ->
      println("协程遇到异常: ${throwable.message}")
  }
  
  coroutine.onFinally {
      println("协程已完成")
  }
  
  coroutine.start() // 显式启动协程(如果指定start为CoroutineStart.LAZY，则需要显式调用start方法启动协程)
  ```
  
- MediaScanner使用
  ```kotlin

  // 查询图片文件（JPG/PNG）
  val imageFiles = MediaScanner.queryFiles(setOf(
      MediaScanner.FileType.JPG,
      MediaScanner.FileType.PNG
  ))
  
  // 查询视频文件（MP4/AVI）并自定义过滤条件
  val videoFiles = MediaScanner.queryFiles(
      types = setOf(
          MediaScanner.FileType.MP4,
          MediaScanner.FileType.AVI
      ),
      filter = { it.size > 1024 * 1024 } // 过滤大于1MB的文件
  )
  
  // 查询文档文件并自定义排序
  val docFiles = MediaScanner.queryFiles(
      types = setOf(
          MediaScanner.FileType.DOCX,
          MediaScanner.FileType.PDF
      ),
      sortOrder = "${MediaStore.MediaColumns.SIZE} DESC" // 按文件大小降序
  )
  ```
  
- ConcurrentProcessor使用
  ```java
  List<ConcurrentProcessor.ProcessorTask<String>> tasks = new ArrayList<>();
  tasks.add(() -> "234");
  tasks.add(() -> {
      try {
          Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      return "兼容";
  });
  ConcurrentProcessor.Companion.get().executeForJava(tasks,
         new ConcurrentProcessor.ConcurrentCallback<String>() {
             @Override
             public void onComplete(@NonNull List<? extends String> results) {
                 results.forEach(ToastUtil::showShort);
             }

             @Override
             public void onError(@NonNull Exception e) {

             }

         });
  ```
  ```kotlin
  // 场景1：基本并发
  suspend fun handleMediaScan() {
      val tasks = listOf<suspend () -> List<String>>(
          { /* 扫描图片实现 */ listOf("img1", "img2") },
          { /* 扫描视频实现 */ listOf("video1") },
          { /* 扫描音频实现 */ listOf("audio1") }
      )

      ConcurrentProcessor.get().executeConcurrent(tasks,
          onComplete = { results ->
              // 合并结果示例
              val total = results.flatten()
              updatePieChart(total.size)
          },
          onError = { showError(it) }
      )
  }

  // 场景2：链式处理
  suspend fun handleFileProcessing() {
      val fileTasks = listOf<suspend () -> String>(
          { File("path1").readText() },
          { File("path2").readText() }
      )

      ConcurrentProcessor.get().executeChainedConcurrent(
          firstTasks = fileTasks,
          secondProcess = { contents ->
              // 在IO线程处理中间结果
              contents.map { it.split("\n") }.flatten()
          },
          finalProcess = { processedList ->
              // 在主线程更新UI
              updateTextView(processedList.joinToString())
          }
      )
  }
  ```
  
- DrawableBuilder(ShapeDrawable构造器)
  ```kotlin
  var drawable = DrawableBuilder.setRadius(12f)
	 .setSolidColor(context.getCompatColor(R.color.md_amber_A200))
	 .build()
  ```

- Widget

  | 类名                       | 作用                      | 说明 |
  |--------------------------|-------------------------| --- |
  | `AccentTextView`         | accentColor颜色的TextView  |  |
  | `BadgeView`              | 角标TextView              |  |
  | `ClickableTextView`      | 可点击的TextView            | 标点符号后会加换行符 |
  | `CustomImageView`        | 可任意位置展示文字的View          | 自定义UI首页 |
  | `DrawableTextView`       | 支持限定Drawable大小的TextView |  |
  | `LoadingView`            | 加载View                  | IOS风格 |
  | `MarqueeTextView`        | 跑马灯TextView             |  |
  | `PressEffectImageView`   | 按压效果的ImageView          |  |
  | `RotateLoading`          | 旋转加载View                |  |
  | `ScrollTextView`         | 嵌套滑动的TextView           |  |
  | `SmartTextView`          | 自动显示和隐藏的TextView        |  |
  | `FastScrollRecyclerView` | 支持可快速定位滚动的RecyclerView  |  |
  | `SettingBar`             | 设置条自定义控件                |  |

  | 类名 | 作用 | 说明 |
  | --- | --- | --- |
  | `RatioFrameLayout` | 按照比例显示的FrameLayout |  |
  | `RootLayout` | 带自定义TitleBar的根布局 |  |
  | `FixedScrollView` | 禁止滚动/禁止显示滚动条的ScrollView | 配合[android.widget.ImageView.ScaleType.FIT_START]可实现图片顶部对齐裁剪 |

**注意事项**

- 事件总线
    - sentEvent与receiveEvent方法绑定使用
    - postEvent与observeEvent方法绑定使用(推荐使用)
  
- 用户首选项
    - Preferences.getValue
    - Preferences.setValue

- Room
  - `BaseDao`
      - 用与封装常用如增删改查的Dao语句
  - `Repository`
      - 数据库数据处理中心的作用
      - 像一些复杂列表数据处理，事务查询等都放在这里面
  - `VMFactory`
      - 这是创建ViewModel的工厂
      - VM里面是处理UI数据的
      - 负责将`Repository`里面的数据发送给页面，这样UI层只通过VM获取数据

- BaseActivity与BaseFragment
    - 接管返回键方法onBackPressedCall的使用
      - 需要在initial方法super之前加入setTakeOverBackPressed来开启/关闭接管
      - 当开启接管时，onBackPressedCall方法生效
    
- BaseBottomSheetDialog
    - initConfig(builder: Builder)
      - 子类重写该方法获取builder可进行进行额外的配置
    - initView()
      - 子类重写该方法进行初始化View
- ToolBar(Google)与AppTitleBar(自定义)
    - ToolBar(Google)
      - setDisplayHomeAsUpEnabled 系统是否接管返回键
      - ```kotlin
          override fun initial(savedInstanceState: Bundle?) {
                ImmersionBar.setTitleBar(this, binding.titleBar)
                super.initial(savedInstanceState)
                setSupportActionBar(binding.titleBar)
                supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_arrow_back)
                }
          }
         ```
    - AppTitleBar(自定义)
      - 已经在BaseActivity设置了
