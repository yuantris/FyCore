# FyCore 扩展方法完整索引

本文档提供了FyCore项目中所有扩展方法的完整索引，帮助开发者快速找到所需的扩展功能。

## 📁 文件结构概览

```
extensions/
├── Any.kt                    # 通用扩展方法
├── SystemServices.kt         # 系统服务扩展
├── cool/                     # 核心功能扩展包
│   ├── Bitmap.kt            # 位图处理扩展
│   ├── Boolean.kt           # 布尔值扩展
│   ├── Collection.kt        # 集合操作扩展
│   ├── Convert.kt           # 单位转换扩展
│   ├── Coroutine.kt         # 协程扩展
│   ├── Environment.kt       # 环境信息扩展
│   ├── File.kt              # 文件操作扩展
│   ├── Flow.kt              # Flow扩展
│   ├── Gson.kt              # JSON处理扩展
│   ├── Handler.kt           # Handler扩展
│   ├── InputStream.kt       # 输入流扩展
│   ├── Intent.kt            # Intent扩展
│   ├── LiveEventBus.kt      # 事件总线扩展
│   ├── Number.kt            # 数值处理扩展
│   ├── Paint.kt             # 绘制扩展
│   ├── Permission.kt        # 权限处理扩展
│   ├── SharedPreferences.kt # 偏好设置扩展
│   ├── String.kt            # 字符串处理扩展
│   ├── Thread.kt            # 线程扩展
│   ├── Throwable.kt         # 异常处理扩展
│   ├── Toast.kt             # Toast扩展
│   ├── Uri.kt               # URI处理扩展
│   └── ViewModel.kt         # ViewModel扩展
└── ui/                       # UI相关扩展包
    ├── Activity.kt          # Activity扩展
    ├── Context.kt           # Context扩展
    ├── Dialog.kt            # 对话框扩展
    ├── EditText.kt          # 编辑框扩展
    ├── Fragment.kt          # Fragment扩展
    ├── Menu.kt              # 菜单扩展
    ├── RecyclerView.kt      # 列表扩展
    ├── SeekBar.kt           # 滑动条扩展
    ├── View.kt              # 视图扩展
    ├── ViewBinding.kt       # 视图绑定扩展
    ├── ViewPager.kt         # 页面切换扩展
    └── Window.kt            # 窗口扩展
```

---

## 🔍 快速查找指南

### 按功能分类

#### 🎯 Activity & Fragment 生命周期
- **Activity扩展** → `ui/Activity.kt`
- **Fragment扩展** → `ui/Fragment.kt`
- **生命周期管理** → `Any.kt`

#### 🎨 UI & 视图操作
- **View通用操作** → `ui/View.kt`
- **Context操作** → `ui/Context.kt`
- **对话框** → `ui/Dialog.kt`
- **编辑框** → `ui/EditText.kt`
- **列表视图** → `ui/RecyclerView.kt`

#### 📱 系统功能
- **权限管理** → `cool/Permission.kt`
- **系统服务** → `SystemServices.kt`
- **环境信息** → `cool/Environment.kt`
- **Toast提示** → `cool/Toast.kt`

#### 💾 数据处理
- **文件操作** → `cool/File.kt`
- **JSON处理** → `cool/Gson.kt`
- **偏好设置** → `cool/SharedPreferences.kt`
- **集合操作** → `cool/Collection.kt`

#### 🔧 工具类扩展
- **字符串处理** → `cool/String.kt`
- **数值处理** → `cool/Number.kt`
- **单位转换** → `cool/Convert.kt`
- **异步处理** → `cool/Coroutine.kt`

---

## 📋 详细方法索引

### 📄 Any.kt - 通用扩展方法
```kotlin
// 生命周期与验证
T?.verify(ifNull, ifNotNull)              // 空值验证
T?.orElseRun(block)                       // 空值替代执行
Any.simpleName()                          // 获取类名
Any?.exitApp()                            // 退出应用

// 时间工具
val currentTimeMillis: Long               // 当前时间戳
val currentTime: String                   // 当前时间字符串
val fileNameByTime: String                // 基于时间的文件名
val authority: String                     // FileProvider权限

// 返回键处理
OnBackPressedDispatcher.addCallback()     // 添加返回键回调

// 日志扩展
Any?.logE/logV/logD/logI/logW/logJson()  // 各级别日志输出
```

### 📄 cool/String.kt - 字符串处理扩展
```kotlin
// 字符串验证与转换
String?.safeTrim()                        // 安全去空格
String?.isContentScheme()                 // 检查content://协议
String.toEditable()                       // 转换为Editable
String.parseToUri()                       // 解析为URI
String?.isUri()                           // 检查是否为URI
String?.isAbsUrl()                        // 检查是否为绝对URL
String?.isDataUrl()                       // 检查是否为Data URL
String?.isJson/isJsonObject/isJsonArray() // JSON格式检查
String?.isXml()                           // XML格式检查
String?.isTrue()                          // 布尔值判断

// 字符串操作
String.splitNotBlank()                    // 分割非空字符串
String.cnCompare()                        // 中文字符串比较
String?.memorySize()                      // 字符串内存大小
String.isChinese()                        // 检查是否包含中文
CharSequence.toStringArray()              // 拆分为字符数组
String.spanForeColor()                    // 设置前景色
String.toast()                            // 显示Toast
String.removeWhitespace()                 // 移除空白字符
```

### 📄 cool/Number.kt - 数值处理扩展
```kotlin
// 数值安全处理
T?.orThis(default)                        // 安全获取数值

// 时间格式化
Long.timeFormat(pattern)                  // 时间格式化
Long.toTimeAgo()                          // 相对时间显示
Long.currentTimeFormatted                 // 当前时间格式化

// 数值格式化
Number.formatToFixedDecimal()             // 固定小数位格式化

// 颜色处理
Int.isDarkColor()                         // 判断颜色深浅
```

### 📄 cool/Collection.kt - 集合操作扩展
```kotlin
// 查找操作
List<T>.findFirstByProperty()             // 按属性查找第一个
List<T>.findAllByProperty()               // 按属性查找所有

// 性能优化
List<Float>.fastSum()                     // 快速求和
List<T>.fastBinarySearch()                // 快速二分查找
List<T>.fastBinarySearchBy()              // 按属性二分查找

// 列表操作
MutableList<T>.removeLastElement()        // 移除最后一个元素
List<T>.sortedByInt()                     // 按整数排序
MutableList<T>.sortByInt()                // 原地整数排序

// Map操作
HashMap<String, *>.has()                  // 检查键存在
HashMap<String, T>.get()                  // 获取值
String.jsonToMap()                        // JSON转Map

// 构建器
createSet()                               // 创建Set
createMap()                               // 创建Map
mapBuilder()                              // Map构建器
```

### 📄 cool/Boolean.kt - 布尔值扩展
```kotlin
// 条件执行
Boolean.ifNext {                          // 条件分支执行
    ifTrue = { ... }
    ifFalse = { ... }
}
```

### 📄 cool/File.kt - 文件操作扩展
```kotlin
// 文件系统操作
File.refreshMediaLibrary()                // 刷新媒体库
File.getUri()                             // 获取URI
File.getFile()                            // 获取子文件
String.getFileName()                      // 获取文件名
File.getNameNoExtension()                 // 获取无扩展名文件名
String.isFilePath()                       // 检查是否为文件路径
File.exists()                             // 检查文件存在

// 文件创建与管理
File.createFileIfNotExist()               // 创建文件（如不存在）
File.createFileReplace()                  // 替换创建文件
File.createFolderIfNotExist()             // 创建文件夹（如不存在）
File.createFolderReplace()                // 替换创建文件夹
File.checkWrite()                         // 检查写权限
File.outputStream()                       // 获取输出流
File.listFileDocs()                       // 列出文件文档
```

### 📄 cool/Convert.kt - 单位转换扩展
```kotlin
// 像素单位转换
Int.dpToPx/spToPx/pxToDp/pxToSp()        // 整数单位转换
Float.dpToPx/spToPx/pxToDp/pxToSp()      // 浮点单位转换

// 便捷属性
Float.dp / Int.dp                         // dp转px
Float.px / Int.px                         // px转dp
Int.hexString                             // 十六进制字符串
```

### 📄 cool/Intent.kt - Intent扩展
```kotlin
// JSON数据传递
Intent.putJson(key, any)                  // 存储JSON数据
Intent.getJsonObject<T>(key)              // 获取JSON对象
Intent.getJsonArray<T>(key)               // 获取JSON数组
```

### 📄 cool/Toast.kt - Toast扩展
```kotlin
// Toast显示
Context.toastOnUI()                       // UI线程显示Toast
Context.longToastOnUI()                   // UI线程长显示Toast
Fragment.toastOnUI()                      // Fragment显示Toast
Fragment.longToast()                      // Fragment长显示Toast
```

### 📄 ui/Activity.kt - Activity扩展
```kotlin
// 生命周期管理
AppCompatActivity.handleDoubleBackPressExit() // 双击退出处理
Activity.isAlive()                        // 检查Activity存活
Activity.moveTaskToFront()                // 移到前台

// Activity跳转
Activity.startNoTransition<T>()           // 无动画跳转（泛型）
Activity.startNoTransition(clazz)         // 无动画跳转（Class）

// DialogFragment管理
AppCompatActivity.showDialogFragment<T>() // 显示对话框（泛型）
AppCompatActivity.showDialogFragment()    // 显示对话框（实例）

// 屏幕显示控制
Activity.fullScreen()                     // 全屏模式
Activity.setLightStatusBar()              // 状态栏亮色模式
Activity.keepScreenOn()                   // 屏幕常亮

// NavigationBar属性
Activity.navigationBar                    // 导航栏视图
Activity.isNavigationBarExist            // 导航栏是否存在
Activity.navigationBarHeight              // 导航栏高度
Activity.navigationBarGravity             // 导航栏位置

// 状态栏自适应
Activity.adaptStatusBarToView()           // 根据视图自适应
Activity.adaptStatusBarToImage()          // 根据图片自适应
```

### 📄 ui/Context.kt - Context扩展
```kotlin
// Context工具
Context.ctx                               // 获取Activity
Context.isActivity                        // 检查是否为Activity
Context.getLauncherActivityIntent()       // 获取启动Intent

// 组件启动
Context.startActivity<T>()                // 启动Activity
Context.startService<T>()                 // 启动Service
Context.bindService<T>()                  // 绑定Service
Context.stopService<T>()                  // 停止Service

// PendingIntent创建
Context.servicePendingIntent<T>()         // Service PendingIntent
Context.activityPendingIntent<T>()        // Activity PendingIntent
Context.broadcastPendingIntent<T>()       // Broadcast PendingIntent

// 广播接收器
LifecycleOwner.registerBroadcastReceiver() // 注册广播接收器

// 偏好设置
Context.getPref*/putPref*()               // 偏好设置读写

// 资源获取
Context.getCompatColor/Drawable()         // 兼容性资源获取
Context.layout2View()                     // 布局转视图

// Toast显示
Context.toast/toastLong()                 // Toast显示

// 系统信息
Context.statusBarHeight                   // 状态栏高度
Context.navigationBarHeight               // 导航栏高度
Context.screenWidth/HeightPx              // 屏幕尺寸（像素）
Context.screenWidth/HeightDp              // 屏幕尺寸（dp）
Context.sysBattery                        // 电池电量
Context.sysScreenOffTime                  // 息屏时间
Context.isPad                             // 是否平板
Context.channel                           // 渠道信息
Context.isDebuggable                      // 是否可调试
Context.isSystemApp                       // 是否系统应用
Context.appName/PackageName/Version*      // 应用信息

// 系统操作
Context.restart()                         // 重启应用
```

### 📄 ui/Fragment.kt - Fragment扩展
```kotlin
// Fragment生命周期
Fragment.isAlive()                        // 检查Fragment存活
Fragment.isCreated                        // 是否已创建

// DialogFragment管理
Fragment.showDialogFragment<T>()          // 显示对话框（泛型）
Fragment.showDialogFragment()             // 显示对话框（实例）

// 偏好设置
Fragment.getPref*/putPref*()              // 偏好设置读写

// 资源获取
Fragment.getCompatColor/Drawable()        // 兼容性资源获取

// Activity启动
Fragment.startActivity<T>()               // 启动Activity

// 布局操作
Fragment.addViewToZYLayout()              // 添加视图到布局

// 便捷属性
Fragment.ctx                              // 获取Context
Fragment.aty                              // 获取Activity
```

### 📄 ui/View.kt - View扩展
```kotlin
// 基础属性
View.activity                             // 获取Activity

// 点击事件
View.onClick()                            // 设置点击事件
View.onDebouncedClick()                   // 防抖点击
View.onClickWithKbHide()                  // 点击隐藏键盘
View.onDebouncedClickWithKbHide()         // 防抖点击隐藏键盘
View.onLongClick()                        // 长按事件

// 可见性控制
View.gone/visible/show/hide/invisible()   // 可见性设置
View.setVisible()                         // 设置可见性

// 内边距设置
View.setPaddingBottom()                   // 设置底部内边距
View.topPadding/bottomPadding             // 上下内边距属性
View.startPadding/endPadding              // 开始结束内边距
View.leftPadding/rightPadding             // 左右内边距

// 颜色与截图
View.getPixelColor()                      // 获取像素颜色
View.backgroundAsColor()                  // 获取背景颜色
View.screenshot()                         // 截图

// 系统UI适配
View.applyStatusBarPadding()              // 应用状态栏内边距
View.applyNavigationBarPadding()          // 应用导航栏内边距
View.applyNavigationBarMargin()           // 应用导航栏外边距

// 可见性监听
View.onVisibilityChange()                 // 可见性变化监听
View.isInScreen                           // 是否在屏幕内

// 动画效果
View.fadeIn/fadeOut()                     // 淡入淡出动画
View.scaleUp/scaleDown()                  // 缩放动画

// 布局操作
View.getWidthCompat/getHeightCompat()     // 兼容性尺寸获取
View.setMargin()                          // 设置外边距
View.centerHorizontally/Vertically()      // 居中对齐
View.setWidthAndHeight()                  // 设置宽高

// 其他功能
View.hideSoftInput()                      // 隐藏软键盘
View.disableAutoFill()                    // 禁用自动填充
View.setBackgroundKeepPadding()           // 设置背景保持内边距
View.setPressEffect()                     // 设置按压效果
View.setShadow()                          // 设置阴影
View.rotate()                             // 旋转动画
View.takeScreenshot()                     // 获取截图
View.isClicked()                          // 检查是否被点击
View.postDelayed()                        // 延迟执行
```

### 📄 其他扩展文件

#### SystemServices.kt - 系统服务扩展
提供系统服务的便捷访问方法

#### cool/Bitmap.kt - 位图处理扩展
位图操作和处理相关扩展

#### cool/Coroutine.kt - 协程扩展
协程和异步操作相关扩展

#### cool/Environment.kt - 环境信息扩展
系统环境和设备信息相关扩展

#### cool/Flow.kt - Flow扩展
Kotlin Flow相关扩展

#### cool/Gson.kt - JSON处理扩展
JSON序列化和反序列化扩展

#### cool/Handler.kt - Handler扩展
Handler和消息处理扩展

#### cool/InputStream.kt - 输入流扩展
输入流操作扩展

#### cool/LiveEventBus.kt - 事件总线扩展
事件总线相关扩展

#### cool/Paint.kt - 绘制扩展
Paint和绘制相关扩展

#### cool/Permission.kt - 权限处理扩展
权限申请和检查扩展

#### cool/SharedPreferences.kt - 偏好设置扩展
SharedPreferences操作扩展

#### cool/Thread.kt - 线程扩展
线程操作相关扩展

#### cool/Throwable.kt - 异常处理扩展
异常处理和错误管理扩展

#### cool/Uri.kt - URI处理扩展
URI操作和处理扩展

#### cool/ViewModel.kt - ViewModel扩展
ViewModel相关扩展

#### ui/Dialog.kt - 对话框扩展
对话框操作和管理扩展

#### ui/EditText.kt - 编辑框扩展
EditText相关扩展

#### ui/Menu.kt - 菜单扩展
菜单操作扩展

#### ui/RecyclerView.kt - 列表扩展
RecyclerView相关扩展

#### ui/SeekBar.kt - 滑动条扩展
SeekBar操作扩展

#### ui/ViewBinding.kt - 视图绑定扩展
ViewBinding相关扩展

#### ui/ViewPager.kt - 页面切换扩展
ViewPager操作扩展

#### ui/Window.kt - 窗口扩展
Window相关扩展

---

## 🎯 使用建议

### 1. 按需导入
```kotlin
// 导入特定扩展
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.cool.toast

// 或导入整个包
import io.core.common.util.extensions.ui.*
import io.core.common.util.extensions.cool.*
```

### 2. 常用组合
```kotlin
// Activity中常用组合
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏和状态栏
        fullScreen()
        adaptStatusBarToView()
        
        // 处理双击退出
        handleDoubleBackPressExit(
            onShowPrompt = { toast(it) },
            onExit = { finish() }
        )
        
        // 设置点击事件
        button.onDebouncedClickWithKbHide {
            startNoTransition<NextActivity>()
        }
    }
}
```

### 3. Fragment中常用组合
```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 使用便捷属性
        val context = ctx
        val activity = aty
        
        // 显示对话框
        showDialogFragment<MyDialogFragment> {
            putString("title", "标题")
        }
        
        // 偏好设置
        putPrefString("key", "value")
        val value = getPrefString("key")
    }
}
```

---

## 📝 更新日志

- **v1.0.0** - 初始版本，包含基础扩展功能
- **v1.1.0** - 添加UI相关扩展
- **v1.2.0** - 完善文档和注释
- **v1.3.0** - 添加完整索引文档

---

## 🤝 贡献指南

1. 新增扩展方法时，请确保添加详细的KDoc注释
2. 按功能分类放置到合适的文件中
3. 更新本索引文档
4. 添加使用示例

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issues
- 代码Review
- 技术讨论群

---

*最后更新时间: 2025-01-15*