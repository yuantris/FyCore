# TaskExecutor vs Concurrency 使用指南

## 1. 核心定位差异

### TaskExecutor
- **任务执行框架**：专注于批量任务的并发执行管理
- **双模式支持**：同时提供协程和线程池两种执行模式
- **生命周期管理**：内置任务进度跟踪、错误聚合和结果收集

### Concurrency
- **并发工具集**：提供各种并发编程基础组件的增强实现
- **桥接能力**：实现协程与Java Future之间的互操作
- **线程池管理**：内置优化的线程池实例

## 2. 使用场景对比

### 优先使用 TaskExecutor 当：
✅ 需要执行**一组相关任务**并统一管理结果  
✅ 需要**协程和线程模式**的混合支持  
✅ 需要**进度回调**和**部分结果收集**功能  
✅ 需要**任务级别的超时控制**  
✅ 需要**错误聚合**处理多个任务的异常

**典型场景**：
- 批量API请求并行执行
- 分布式计算任务分片处理
- 需要显示进度条的并行操作

### 优先使用 Concurrency 当：
✅ 需要**单个异步任务**的简单执行  
✅ 需要**Future与协程互转**  
✅ 需要**定时/延迟任务**调度  
✅ 需要**基础的线程池管理**  
✅ 需要**CountDownLatch**等并发原语

**典型场景**：
- 单个耗时操作的异步执行
- 将现有Java Future转换为协程
- 简单的定时任务调度
- 并发工具类的增强使用

## 3. 代码示例对比

### TaskExecutor 典型用法
```kotlin
// 批量任务执行
TaskExecutor.get().executeConcurrent(
    tasks = listOf({ fetchUserData() }, { fetchOrderData() }),
    onComplete = { results -> /* 合并结果 */ },
    onError = { e, partial -> /* 处理错误 */ }
)

// Java线程模式
TaskExecutor.get().execute(
    tasks = listOf(ProcessorTask { heavyComputation() }),
    callback = object : ConcurrentCallback<Result> {
        override fun onComplete(results) { /*...*/ }
    }
)
```
### Concurrency 典型用法
```kotlin
// 单个异步任务
val future = Concurrency.supplyAsync { fetchData() }

// 协程桥接
val result = Concurrency.awaitCompletable(future)

// 定时任务
Concurrency.schedule({ cleanup() }, 5, TimeUnit.MINUTES)
```
## 4. 设计建议
- 新项目 ：优先使用 TaskExecutor 作为任务执行框架，Concurrency 作为补充工具
- 混合架构 ：
  - Kotlin协程部分使用 TaskExecutor 的协程模式
  - Java兼容部分使用 TaskExecutor 的线程模式
- 简单场景 ：直接使用 Concurrency 的简化API
- 复杂场景 ：使用 TaskExecutor 的完整功能集

## 5. 性能考量
| 维度     | TaskExecutor     | Concurrency |
|--------|------------------|-------------|
| 任务调度开销 | 较高（功能完整）         | 较低（轻量级）     |
| 内存占用   | 较高（维护任务状态）       | 较低          |
| 适用任务规模 | 中小规模任务组（10-1000） | 单个或少量任务     |
| 线程利用率  | 优化过的固定线程池        | 共享的弹性线程池    |
