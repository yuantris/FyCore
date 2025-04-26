# UniversalPool 通用对象池使用指南

## 1. 基础构建流程
```kotlin
val bitmapPool = UniversalPool.Builder<Bitmap>().apply {
    maxSize = 5
    creator = { Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888) }
    resetter = { it.recycle() }
    validator = { !it.isRecycled }
    destroyer = { it.recycle() }
}.build()
```

## 2. Bitmap池化示例（避免内存抖动）
```kotlin
// 在RecyclerView的onBindViewHolder中使用
fun bind(position: Int) {
    val bitmap = bitmapPool.borrow()
    try {
        // 使用bitmap进行绘图操作
        imageView.setImageBitmap(bitmap)
    } finally {
        if (!bitmapPool.release(bitmap)) {
            // 处理无效bitmap
        }
    }
}
```

## 3. 通用对象复用示例
```kotlin
// 自定义数据类池化
val dataPool = UniversalPool.Builder<DataBean>().apply {
    maxSize = 10
    creator = { DataBean() }
    resetter = { it.reset() } // 自定义重置方法
    validator = { it.isValid }
}.build()

// 对象借还流程
val data = dataPool.borrow()
try {
    // 处理业务逻辑
} finally {
    dataPool.release(data)
}
```

## 4. 统计监控方法
```kotlin
// 实时监控池状态
val stats = """
    池状态报告：
    总创建数：${bitmapPool.totalCreated}
    活跃实例：${bitmapPool.currentActive}
    可用容量：${bitmapPool.availableCount}
    销毁总数：${bitmapPool.totalDestroyed}
""".trimIndent()
```

## 5. 异常处理建议
```kotlin
try {
    val obj = pool.borrow()
} catch (e: PoolExhaustedException) {
    // 处理池耗尽情况：
    // 1. 适当扩大池容量
    // 2. 检查对象释放逻辑
    // 3. 添加等待重试机制
}
```

## 最佳实践建议
1. 合理设置maxSize（建议5-20之间）
2. 重要对象必须实现验证器（validator）
3. 结合Android生命周期自动清理
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onDestroy() {
        bitmapPool.clear()
        super.onDestroy()
    }
}
```