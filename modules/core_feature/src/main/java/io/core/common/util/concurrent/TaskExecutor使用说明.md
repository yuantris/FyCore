# TaskExecutor 使用说明文档

## 概述

TaskExecutor 是一个增强型并发任务执行器，提供协程和线程两种模式的并发任务执行能力。经过优化后，具备更好的性能、内存安全性和易用性。

### 主要特性

- 🚀 **高性能**：智能线程池配置，批量回调处理
- 🛡️ **内存安全**：改进资源管理，避免内存泄漏
- 🎯 **类型安全**：完整的泛型支持和类型推断
- 🔧 **灵活配置**：Builder 模式，支持各种执行策略
- 📊 **详细统计**：执行时间、成功率等统计信息
- 🌊 **Flow 支持**：响应式编程，流式处理
- ⚡ **协程优化**：结构化并发，优雅的异常处理

## 快速开始

### Kotlin 协程版本

```kotlin
// 简单使用
val tasks = listOf(
    { delay(1000); "任务1完成" },
    { delay(800); "任务2完成" },
    { delay(1200); "任务3完成" }
)

val executor = TaskExecutor.get()
executor.execute(tasks) { results ->
    println("所有任务完成: $results")
}
```

### Java 版本

```java
List<TaskExecutor.ProcessorTask<String>> tasks = Arrays.asList(
    () -> { Thread.sleep(1000); return "任务1完成"; },
    () -> { Thread.sleep(800); return "任务2完成"; },
    () -> { Thread.sleep(1200); return "任务3完成"; }
);

TaskExecutor.get().execute(tasks, new TaskExecutor.ConcurrentCallback<String>() {
    @Override
    public void onComplete(SortedMap<Integer, String> results) {
        System.out.println("所有任务完成: " + results);
    }
    
    @Override
    public void onError(TaskExecutor.TaskExecutionException e) {
        System.out.println("执行失败: " + e.getMessage());
    }
});
```

## 详细使用指南

### 1. Kotlin 协程模式

#### 1.1 基础使用

```kotlin
suspend fun basicExample() {
    val tasks = listOf(
        { fetchUserData(1) },
        { fetchUserData(2) },
        { fetchUserData(3) }
    )
    
    val executor = TaskExecutor.get()
    val result = executor.execute(tasks) { results ->
        println("获取到 ${results.size} 个用户数据")
    }
    
    when (result) {
        is TaskExecutor.ExecutionResult.Success -> {
            println("执行成功，耗时: ${result.statistics.executionTimeMs}ms")
        }
        is TaskExecutor.ExecutionResult.PartialSuccess -> {
            println("部分成功: ${result.results.size}/${result.statistics.totalTasks}")
            println("失败原因: ${result.errors}")
        }
        is TaskExecutor.ExecutionResult.Failure -> {
            println("执行失败: ${result.error}")
        }
    }
}
```

#### 1.2 Builder 模式配置

```kotlin
suspend fun builderExample() {
    val tasks = listOf(
        { processData("data1") },
        { processData("data2") },
        { processData("data3") }
    )
    
    val result = TaskExecutor.builder<String>()
        .timeout(30000)                    // 30秒超时
        .maxConcurrency(5)                 // 最大并发数
        .context(Dispatchers.IO)           // 协程上下文
        .onProgress { completed, total ->   // 进度回调
            println("进度: $completed/$total")
        }
        .onEachComplete { result, index ->  // 单个任务完成回调
            println("任务$index 完成: $result")
        }
        .onError { error ->                // 错误回调
            println("执行出错: ${error.message}")
        }
        .execute(tasks)
    
    // 处理结果...
}
```

#### 1.3 Flow 流式处理

```kotlin
fun flowExample() {
    val tasks = listOf(
        { downloadFile("file1.txt") },
        { downloadFile("file2.txt") },
        { downloadFile("file3.txt") }
    )
    
    TaskExecutor.get().executeAsFlow(tasks)
        .collect { result ->
            when (result) {
                is TaskExecutor.TaskResult.Success -> {
                    println("文件 ${result.index} 下载成功: ${result.value}")
                }
                is TaskExecutor.TaskResult.Failure -> {
                    println("文件 ${result.index} 下载失败: ${result.exception.message}")
                }
            }
        }
}
```

#### 1.4 扩展函数使用

```kotlin
suspend fun extensionExample() {
    val tasks = listOf(
        { calculatePi(1000) },
        { calculatePi(2000) },
        { calculatePi(3000) }
    )
    
    // 使用扩展函数
    val results = tasks.executeParallel(
        maxConcurrency = 3,
        timeoutMillis = 10000
    )
    
    println("计算结果: $results")
}
```

### 2. Java 模式

#### 2.1 完整回调示例

```java
public class TaskExecutorJavaExample {
    public static void main(String[] args) {
        // 创建任务
        List<TaskExecutor.ProcessorTask<String>> tasks = Arrays.asList(
            () -> processOrder("order1"),
            () -> processOrder("order2"),
            () -> processOrder("order3"),
            () -> { throw new RuntimeException("订单4处理失败"); }, // 模拟失败
            () -> processOrder("order5")
        );
        
        // 执行任务
        TaskExecutor.get().execute(tasks, new OrderCallback(), 30000);
    }
    
    static class OrderCallback implements TaskExecutor.ConcurrentCallback<String> {
        @Override
        public void onComplete(SortedMap<Integer, String> results) {
            System.out.println("✅ 所有订单处理完成:");
            results.forEach((index, result) -> 
                System.out.println("  订单" + index + ": " + result));
        }

        @Override
        public void onPartialComplete(SortedMap<Integer, String> partialResults) {
            System.out.println("⚠️ 部分订单处理完成:");
            partialResults.forEach((index, result) -> 
                System.out.println("  ✅ 订单" + index + ": " + result));
        }

        @Override
        public void onError(TaskExecutor.TaskExecutionException e) {
            System.out.println("❌ 订单处理失败: " + e.getMessage());
            
            if (e instanceof TaskExecutor.TaskExecutionException.Aggregate) {
                handleMultipleFailures((TaskExecutor.TaskExecutionException.Aggregate) e);
            } else if (e instanceof TaskExecutor.TaskExecutionException.TaskFailure) {
                handleSingleFailure((TaskExecutor.TaskExecutionException.TaskFailure) e);
            }
        }

        @Override
        public void onProgress(int completed, int total) {
            System.out.println("📊 处理进度: " + completed + "/" + total);
        }

        @Override
        public void onEachResult(String result, int index) {
            System.out.println("✅ 订单" + index + " 处理完成: " + result);
        }
        
        private void handleMultipleFailures(TaskExecutor.TaskExecutionException.Aggregate error) {
            System.out.println("多个订单处理失败:");
            for (TaskExecutor.TaskExecutionException cause : error.getCauses()) {
                if (cause instanceof TaskExecutor.TaskExecutionException.TaskFailure) {
                    TaskExecutor.TaskExecutionException.TaskFailure failure = 
                        (TaskExecutor.TaskExecutionException.TaskFailure) cause;
                    System.out.println("  ❌ 订单" + failure.getTaskIndex() + 
                        " 失败: " + failure.getOriginalException().getMessage());
                }
            }
            
            // 获取成功处理的订单
            Object successfulOrders = error.getPartialResults();
            if (successfulOrders != null) {
                System.out.println("成功处理的订单: " + successfulOrders);
            }
        }
        
        private void handleSingleFailure(TaskExecutor.TaskExecutionException.TaskFailure error) {
            System.out.println("订单" + error.getTaskIndex() + " 处理失败: " + 
                error.getOriginalException().getMessage());
        }
    }
    
    private static String processOrder(String orderId) throws InterruptedException {
        // 模拟订单处理
        Thread.sleep((long) (Math.random() * 2000 + 500));
        return orderId + " 处理完成";
    }
}
```

#### 2.2 简化回调示例

```java
public class SimpleJavaExample {
    public static void main(String[] args) {
        List<TaskExecutor.ProcessorTask<Integer>> tasks = Arrays.asList(
            () -> fibonacci(10),
            () -> fibonacci(15),
            () -> fibonacci(20),
            () -> fibonacci(25),
            () -> fibonacci(30)
        );
        
        TaskExecutor.get().execute(tasks, new TaskExecutor.ConcurrentCallback<Integer>() {
            @Override
            public void onComplete(SortedMap<Integer, Integer> results) {
                System.out.println("斐波那契计算完成: " + results);
            }
            
            @Override
            public void onError(TaskExecutor.TaskExecutionException e) {
                System.err.println("计算失败: " + e.getMessage());
            }
        });
    }
    
    private static Integer fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
```

### 3. 异常处理

#### 3.1 异常类型

```kotlin
// 任务失败异常
TaskExecutionException.TaskFailure(
    taskIndex: Int,           // 失败任务的索引
    originalException: Throwable  // 原始异常
)

// 超时异常
TaskExecutionException.Timeout(
    reason: String,           // 超时原因
    originalException: Throwable?,
    taskIndex: Int?
)

// 中断异常
TaskExecutionException.Interrupted(
    reason: String,           // 中断原因
    originalException: Throwable?,
    taskIndex: Int?
)

// 聚合异常（多个任务失败）
TaskExecutionException.Aggregate(
    causes: List<TaskExecutionException>,  // 所有失败的异常
    partialResults: Any?                   // 部分成功的结果
)
```

#### 3.2 异常处理示例

```kotlin
suspend fun errorHandlingExample() {
    val tasks = listOf(
        { "成功任务1" },
        { throw RuntimeException("任务2失败") },
        { "成功任务3" },
        { throw IllegalStateException("任务4失败") }
    )
    
    val result = executor.execute(tasks,
        onComplete = { results ->
            println("所有任务成功: $results")
        },
        onError = { error ->
            when (error) {
                is TaskExecutor.TaskExecutionException.Aggregate -> {
                    println("多个任务失败:")
                    error.causes.forEach { cause ->
                        if (cause is TaskExecutor.TaskExecutionException.TaskFailure) {
                            println("  任务${cause.taskIndex}失败: ${cause.originalException.message}")
                        }
                    }
                    println("成功的任务结果: ${error.partialResults}")
                }
                is TaskExecutor.TaskExecutionException.TaskFailure -> {
                    println("任务${error.taskIndex}失败: ${error.originalException.message}")
                }
                else -> {
                    println("执行失败: ${error.message}")
                }
            }
        }
    )
}
```

### 4. 高级配置

#### 4.1 自定义线程池

```kotlin
// 创建自定义实例
val customExecutor = TaskExecutor.newInstance()

// 使用自定义线程池（Java版本）
val customThreadPool = Executors.newFixedThreadPool(10)
executor.execute(tasks, callback, 0, TimeUnit.MILLISECONDS, customThreadPool)
```

#### 4.2 资源管理

```kotlin
// 关闭执行器
executor.shutdown()

// 检查是否已关闭
if (executor.isShutdown()) {
    println("执行器已关闭")
}

// 清理单例实例（用于测试）
TaskExecutor.clearInstance()
```

#### 4.3 统计信息

```kotlin
suspend fun statisticsExample() {
    val result = executor.execute(config, tasks)
    
    when (result) {
        is TaskExecutor.ExecutionResult.Success -> {
            val stats = result.statistics
            println("总任务数: ${stats.totalTasks}")
            println("完成任务数: ${stats.completedTasks}")
            println("失败任务数: ${stats.failedTasks}")
            println("总执行时间: ${stats.executionTimeMs}ms")
            println("平均任务时间: ${stats.averageTaskTimeMs}ms")
        }
    }
}
```

## 最佳实践

### 1. 任务设计

```kotlin
// ✅ 好的做法：任务独立，无副作用
val goodTasks = listOf(
    { calculateHash("data1") },
    { calculateHash("data2") },
    { calculateHash("data3") }
)

// ❌ 避免：任务间有依赖关系
val badTasks = listOf(
    { initializeDatabase() },      // 其他任务依赖这个
    { queryUser(1) },             // 依赖数据库初始化
    { queryUser(2) }              // 依赖数据库初始化
)
```

### 2. 异常处理

```kotlin
// ✅ 好的做法：细粒度异常处理
suspend fun goodErrorHandling() {
    executor.execute(tasks,
        onComplete = { results -> /* 处理成功结果 */ },
        onError = { error ->
            when (error) {
                is TaskExecutor.TaskExecutionException.Timeout -> {
                    // 处理超时
                    retryWithLongerTimeout()
                }
                is TaskExecutor.TaskExecutionException.Aggregate -> {
                    // 处理部分失败
                    processPartialResults(error.partialResults)
                    retryFailedTasks(error.causes)
                }
                else -> {
                    // 处理其他错误
                    logError(error)
                }
            }
        }
    )
}
```

### 3. 性能优化

```kotlin
// ✅ 合理设置并发数
val config = TaskExecutor.ExecutionConfig<String>(
    maxConcurrency = min(tasks.size, Runtime.getRuntime().availableProcessors() * 2),
    timeoutMillis = 30000,
    batchCallbacks = true  // 启用批量回调
)

// ✅ 使用合适的协程上下文
val ioConfig = TaskExecutor.ExecutionConfig<String>(
    context = Dispatchers.IO,  // IO密集型任务
    maxConcurrency = 50
)

val cpuConfig = TaskExecutor.ExecutionConfig<String>(
    context = Dispatchers.Default,  // CPU密集型任务
    maxConcurrency = Runtime.getRuntime().availableProcessors()
)
```

### 4. 内存管理

```kotlin
// ✅ 及时清理大对象
val tasks = largeDataList.map { data ->
    {
        val result = processLargeData(data)
        data.clear()  // 及时清理
        result
    }
}

// ✅ 在应用关闭时清理资源
Runtime.getRuntime().addShutdownHook(Thread {
    TaskExecutor.get().shutdown()
})
```

## 常见问题

### Q1: 如何处理任务超时？

```kotlin
// 设置整体超时
val config = TaskExecutor.ExecutionConfig<String>(
    timeoutMillis = 30000  // 30秒整体超时
)

// 处理超时异常
executor.execute(config, tasks,
    onError = { error ->
        if (error is TaskExecutor.TaskExecutionException.Timeout) {
            println("任务执行超时: ${error.reason}")
            // 可以选择重试或降级处理
        }
    }
)
```

### Q2: 如何限制并发数？

```kotlin
// 方法1: 通过配置限制
val config = TaskExecutor.ExecutionConfig<String>(
    maxConcurrency = 5  // 最多同时执行5个任务
)

// 方法2: 通过扩展函数限制
val results = tasks.executeParallel(maxConcurrency = 5)
```

### Q3: 如何获取执行进度？

```kotlin
// Kotlin版本
executor.execute(
    TaskExecutor.ExecutionConfig<String>(
        onProgress = { completed, total ->
            val percentage = (completed * 100) / total
            println("执行进度: $percentage%")
        }
    ),
    tasks
)

// Java版本
executor.execute(tasks, new TaskExecutor.ConcurrentCallback<String>() {
    @Override
    public void onProgress(int completed, int total) {
        int percentage = (completed * 100) / total;
        System.out.println("执行进度: " + percentage + "%");
    }
    // ... 其他方法
});
```

### Q4: 如何处理部分任务失败？

```kotlin
executor.execute(tasks,
    onComplete = { results ->
        // 所有任务成功
        processAllResults(results)
    },
    onError = { error ->
        if (error is TaskExecutor.TaskExecutionException.Aggregate) {
            // 部分任务失败
            val successResults = error.partialResults as? List<String>
            val failedTasks = error.causes
            
            // 处理成功的结果
            successResults?.let { processPartialResults(it) }
            
            // 重试失败的任务
            retryFailedTasks(failedTasks)
        }
    }
)
```

## 版本历史

### v2.0.0 (优化版本)
- ✨ 新增 Builder 模式支持
- ✨ 新增 Flow 流式处理
- ✨ 新增详细的异常体系
- ✨ 新增执行统计信息
- 🚀 性能优化：批量回调处理
- 🛡️ 内存安全：改进资源管理
- 🔧 API 改进：更好的类型安全

### v1.0.0 (原始版本)
- 基础的并发任务执行功能
- 协程和Java线程池支持
- 简单的异常处理

## 许可证

本项目采用 MIT 许可证。