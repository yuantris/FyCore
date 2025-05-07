# ProgressNotifier 使用文档

## 一、功能概述
ProgressNotifier 是一个用于管理任务进度通知和超时处理的工具类，主要提供以下核心功能：
- **任务生命周期管理**：支持任务启动、进度更新、手动完成等操作
- **进度通知机制**：通过监听器回调实时通知进度变化（自动切换到主线程）
- **超时与闲置清理**：支持任务超时检测和长期未更新任务的自动清理（默认10分钟）

## 二、核心类型与类结构
### 1. 类型别名
- `ProgressCallback`：进度回调函数类型 `(taskId: String, percent: Int) -> Unit`（默认在主线程执行）
- `TimeoutCallback`：超时回调函数类型 `(taskId: String) -> Unit`

### 2. 核心方法
| 方法名                  | 功能描述                 | 参数说明                                                                        | 返回值           |
|----------------------|----------------------|-----------------------------------------------------------------------------|---------------|
| `register`           | 注册进度监听器（自动切换到主线程回调）  | `listener`: 进度回调函数                                                          | 监听器ID（String） |
| `startTask`          | 启动新任务（带超时配置）         | `totalSteps`: 总步数（>0）<br>`timeoutMillis`: 超时时间（可选）<br>`onTimeout`: 超时回调（可选） | 任务ID（String）  |
| `incrementProgress`  | 增量更新进度（安全累加，不超过总步数）  | `taskId`: 任务ID<br>`step`: 步长（默认1）                                           | 无             |
| `setCurrentProgress` | 直接设置当前进度（安全限制在0~总步数） | `taskId`: 任务ID<br>`current`: 当前步数                                           | 无             |
| `complete`           | 标记任务完成（自动补满进度并移除任务）  | `taskId`: 任务ID                                                              | 无             |

## 三、使用步骤
### 1. 注册进度监听器
在需要监听进度的地方（如Activity/Fragment）注册监听器，建议在生命周期开始时注册，结束时移除（通过返回的监听器ID）。
```kotlin
// 在Activity的onCreate中注册
val listenerId = ProgressNotifier.register { taskId, percent ->
    // 更新UI（已自动切换到主线程）
    progressBar.progress = percent
    tvProgress.text = "进度：$percent%"
}
```

### 2. 启动任务
在需要跟踪进度的操作开始时调用`startTask`，例如文件下载、数据处理等。
```kotlin
// 启动一个最多30秒的文件下载任务
val taskId = ProgressNotifier.startTask(
    totalSteps = 100, // 总共有100步
    timeoutMillis = 30_000, // 30秒超时
    onTimeout = { taskId ->
        Toast.makeText(context, "任务超时：$taskId", Toast.LENGTH_SHORT).show()
    }
)
```

### 3. 更新进度
在任务执行过程中，通过`incrementProgress`或`setCurrentProgress`更新进度。
```kotlin
// 方式1：增量更新（每完成1步调用一次）
repeat(100) { step ->
    Thread.sleep(100) // 模拟耗时操作
    ProgressNotifier.incrementProgress(taskId)
}

// 方式2：直接设置（已知当前完成50步时）
ProgressNotifier.setCurrentProgress(taskId, 50)
```

### 4. 任务完成/超时处理
- **正常完成**：任务结束时调用`complete`（会自动触发进度100%回调并清理任务）
  ```kotlin
  ProgressNotifier.complete(taskId)
  ```
- **超时处理**：若任务超时，会触发注册的`onTimeout`回调和`defaultTimeoutHandler`（全局默认超时处理）

## 四、注意事项
1. **监听器管理**：监听器使用弱引用存储，需保持外部持有监听器对象的强引用，避免被GC回收
2. **线程安全**：所有公开方法均为线程安全，可在任意线程调用
3. **超时配置**：未设置`timeoutMillis`的任务不会触发超时，但仍会在10分钟未更新时被清理（`MAX_IDLE_TIME_MS`）
4. **内存优化**：长期未使用的监听器会自动清理（通过`WeakReference`），无需手动移除

## 五、扩展使用
### 设置全局默认超时处理
```kotlin
// 在Application初始化时设置
ProgressNotifier.defaultTimeoutHandler = { taskId ->
    Log.e("ProgressNotifier", "全局超时任务：$taskId")
}
```