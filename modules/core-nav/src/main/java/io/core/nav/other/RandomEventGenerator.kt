package io.core.nav.other

import kotlin.random.Random

class RandomEventGenerator {
    // 改用带权重的事件结构
    private val weightedEvents = mutableListOf<Pair<Int, () -> Unit>>()
    private var totalWeight = 0

    // DSL 风格构建�?
    fun addEvent(weight: Int = 1, event: () -> Unit) = apply {
        require(weight > 0) { "Weight must be positive" }
        weightedEvents.add(weight to event)
        totalWeight += weight
    }

    // 线程安全的执行方�?
    @Synchronized
    fun generate() {
        if (weightedEvents.isEmpty()) {
            handleNoEvents()
            return
        }

        val randomValue = Random.nextInt(totalWeight)
        var accumulated = 0

        for ((weight, event) in weightedEvents) {
            accumulated += weight
            if (randomValue < accumulated) {
                executeSafely(event)
                return
            }
        }
    }

    // 可扩展的回调函数
    var onNoEvents: () -> Unit = { println("⚠️ No events available") }
    var onEventError: (Throwable) -> Unit = { e -> println("�?Event error: ${e.message}") }

    private fun handleNoEvents() = onNoEvents()

    private fun executeSafely(event: () -> Unit) {
        try {
            event()
        } catch (e: Exception) {
            onEventError(e)
        }
    }
}

/* 使用示例�?
val generator = RandomEventGenerator()
    .addEvent(3) { println("High probability event") }
    .addEvent(1) { println("Low probability event") }

generator.onNoEvents = { showToast("没有可执行事�?) }
generator.generate()
*/
