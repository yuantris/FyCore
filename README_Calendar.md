# Android日历管理器 (AndroidCalendarManager)

一个功能强大、可扩展的Android系统日历处理库，充分利用Kotlin特性的同时保持与Java的完美兼容。

## 特性

- ✅ **完整的CRUD操作**：创建、读取、更新、删除日程事件
- ✅ **智能提醒系统**：支持多种提醒方式和时间设置
- ✅ **灵活的重复规则**：日、周、月、年重复，支持自定义规则
- ✅ **插件化架构**：支持事件处理器、同步策略、验证器等扩展
- ✅ **Kotlin协程支持**：异步操作，性能优异
- ✅ **Java完全兼容**：提供回调接口，支持Java项目无缝集成
- ✅ **权限管理**：内置权限检查和请求工具
- ✅ **类型安全**：充分利用Kotlin类型系统，减少运行时错误

## 快速开始

### 1. 添加权限

在 `AndroidManifest.xml` 中添加必要权限：

```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

### 2. 检查权限

```kotlin
// Kotlin
if (!CalendarPermissionHelper.hasAllPermissions(this)) {
    CalendarPermissionHelper.requestPermissions(this)
}
```

```java
// Java
if (!CalendarPermissionHelper.hasAllPermissions(this)) {
    CalendarPermissionHelper.requestPermissions(this);
}
```

### 3. 基础使用

#### Kotlin使用方式

```kotlin
// 获取管理器实例
val manager = AndroidCalendarManager.getInstance(this)

// 创建简单事件
val event = CalendarEvent.builder()
    .calendarId(1L)
    .title("重要会议")
    .description("项目讨论会议")
    .location("会议室A")
    .startTime(System.currentTimeMillis() + 3600000) // 1小时后
    .endTime(System.currentTimeMillis() + 7200000)   // 2小时后
    .addReminder(15) // 提前15分钟提醒
    .build()

// 协程方式创建事件
lifecycleScope.launch {
    manager.createEvent(event).fold(
        onSuccess = { eventId -> 
            println("事件创建成功，ID: $eventId") 
        },
        onFailure = { error -> 
            println("创建失败: ${error.message}") 
        }
    )
}

// 查询今天的事件
lifecycleScope.launch {
    val todayEvents = manager.queryEvents(EventQuery.today())
    todayEvents.onSuccess { events ->
        println("今天有 ${events.size} 个事件")
    }
}
```

#### Java使用方式

```java
// 获取管理器实例
AndroidCalendarManager manager = AndroidCalendarManager.getInstance(this);

// 创建事件
CalendarEvent event = CalendarEvent.builder()
    .calendarId(1L)
    .title("重要会议")
    .description("项目讨论会议")
    .location("会议室A")
    .startTime(System.currentTimeMillis() + 3600000L)
    .endTime(System.currentTimeMillis() + 7200000L)
    .addReminder(15)
    .build();

// 回调方式创建事件
manager.createEventAsync(event, CalendarCallback.create(
    eventId -> System.out.println("事件创建成功，ID: " + eventId),
    error -> System.out.println("创建失败: " + error.getMessage())
));

// 查询事件
EventQuery query = EventQuery.builder()
    .timeRange(startTime, endTime)
    .searchText("会议")
    .limit(10);

manager.queryEventsAsync(query, CalendarCallback.create(
    events -> System.out.println("查询到 " + events.size() + " 个事件")
));
```

## 高级功能

### 1. 重复事件

```kotlin
// 每日重复
val dailyEvent = CalendarEvent.builder()
    .title("晨练")
    .dailyReminder()
    .addReminder(ReminderConfig.before30Minutes())
    .build()

// 每周工作日
val weeklyMeeting = CalendarEvent.builder()
    .title("周例会")
    .weeklyReminder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    .addReminder(ReminderConfig.before15Minutes())
    .build()

// 每月固定日期
val monthlyReport = CalendarEvent.builder()
    .title("月度报告")
    .monthlyReminder(1) // 每月1号
    .addReminder(ReminderConfig.before1Day())
    .build()
```

### 2. 多种提醒设置

```kotlin
val event = CalendarEvent.builder()
    .title("重要面试")
    .addReminder(ReminderConfig.before1Day())    // 提前1天
    .addReminder(ReminderConfig.before1Hour())   // 提前1小时
    .addReminder(ReminderConfig.before15Minutes()) // 提前15分钟
    .build()
```

### 3. 查询功能

```kotlin
// 查询特定日历的事件
val query = EventQuery.builder()
    .calendarIds(1L, 2L, 3L)
    .timeRange(startTime, endTime)
    .searchText("会议")
    .sortBy(SortOrder.START_TIME_ASC)
    .limit(20)

// 使用扩展函数快速查询
val todayEvents = manager.queryEvents(EventQuery.today())
val weekEvents = manager.queryEvents(EventQuery.thisWeek())
val monthEvents = manager.queryEvents(EventQuery.thisMonth())
```

### 4. 扩展功能

#### 添加事件处理器

```kotlin
class ConflictDetectionProcessor : EventProcessor {
    override fun canHandle(event: CalendarEvent): Boolean = true
    
    override suspend fun preProcess(event: CalendarEvent): CalendarEvent {
        // 检测时间冲突逻辑
        return event
    }
    
    override suspend fun postProcess(eventId: Long, event: CalendarEvent) {
        // 后处理逻辑
    }
    
    override fun getPriority(): Int = 100
}

// 注册处理器
val extendedManager = AndroidCalendarManager.builder(context)
    .addEventProcessor(ConflictDetectionProcessor())
    .build()
```

#### 添加事件监听器

```kotlin
class AnalyticsEventListener : CalendarEventListener {
    override suspend fun onEventCreated(eventId: Long, event: CalendarEvent) {
        // 统计事件创建
        Analytics.track("event_created", mapOf("title" to event.title))
    }
    
    override suspend fun onEventUpdated(eventId: Long, event: CalendarEvent) {
        // 统计事件更新
    }
    
    override suspend fun onEventDeleted(eventId: Long) {
        // 统计事件删除
    }
    
    override suspend fun onEventQueried(events: List<CalendarEvent>) {
        // 统计查询操作
    }
}

// 注册监听器
manager.registerEventListener(AnalyticsEventListener())
```

#### 添加验证器

```kotlin
class BusinessHoursValidator : EventValidator {
    override suspend fun validate(event: CalendarEvent): Result<Unit> {
        val startHour = event.startDateTime.hour
        return if (startHour in 9..18) {
            Result.success(Unit)
        } else {
            Result.failure(ValidationException("start_time", "事件必须在工作时间内"))
        }
    }
    
    override fun getValidatorName(): String = "BusinessHoursValidator"
}

// 注册验证器
manager.registerValidator(BusinessHoursValidator())
```

## 扩展工具

### 1. 扩展函数

```kotlin
// 时间相关扩展
val dateTime = System.currentTimeMillis().toLocalDateTime()
val timestamp = LocalDateTime.now().toTimestamp()

// 事件扩展
val duration = event.getDurationInMinutes()
val hasReminders = event.hasReminders()
val isRecurring = event.isRecurring()

// 集合扩展
val todayEvents = allEvents.filterToday()
val conflicts = allEvents.findConflicts(targetEvent)
val sortedEvents = allEvents.sortByStartTime()
val groupedEvents = allEvents.groupByDate()
```

### 2. 权限管理

```kotlin
// 检查权限
val hasRead = CalendarPermissionHelper.hasReadPermission(context)
val hasWrite = CalendarPermissionHelper.hasWritePermission(context)
val hasAll = CalendarPermissionHelper.hasAllPermissions(context)

// 获取缺失权限
val missing = CalendarPermissionHelper.getMissingPermissions(context)

// 处理权限请求结果
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    if (CalendarPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
        // 权限已授予
        initCalendarManager()
    } else {
        // 权限被拒绝
        showPermissionDeniedDialog()
    }
}
```

## 架构设计

### 核心组件

1. **AndroidCalendarManager**: 主管理器，提供统一API
2. **CalendarOperator**: 日历操作接口，可自定义实现
3. **ReminderProcessor**: 提醒处理接口
4. **EventProcessor**: 事件处理器，支持预处理和后处理
5. **EventValidator**: 事件验证器
6. **CalendarEventListener**: 事件生命周期监听器
7. **SyncStrategy**: 同步策略接口

### 扩展点

- **事件处理链**: 支持添加预处理、后处理逻辑
- **同步策略**: 支持多种第三方日历同步
- **验证器**: 支持自定义业务规则验证
- **监听器**: 支持事件生命周期监听
- **数据转换器**: 支持多种数据格式转换

## 最佳实践

### 1. 错误处理

```kotlin
lifecycleScope.launch {
    manager.createEvent(event).fold(
        onSuccess = { eventId ->
            // 成功处理
        },
        onFailure = { error ->
            when (error) {
                is CalendarException.PermissionDeniedException -> {
                    // 处理权限问题
                }
                is CalendarException.ValidationException -> {
                    // 处理验证错误
                }
                is CalendarException.ConflictException -> {
                    // 处理时间冲突
                }
                else -> {
                    // 处理其他错误
                }
            }
        }
    )
}
```

### 2. 性能优化

```kotlin
// 批量操作
val events = listOf(event1, event2, event3)
events.forEach { event ->
    launch {
        manager.createEvent(event)
    }
}

// 使用限制查询
val query = EventQuery.builder()
    .timeRange(startTime, endTime)
    .limit(50) // 限制结果数量
    .build()
```

### 3. 内存管理

```kotlin
// 及时取消协程
class CalendarActivity : AppCompatActivity() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // 取消所有协程
    }
}
```

## 注意事项

1. **权限**: 使用前必须获取日历读写权限
2. **线程安全**: 所有操作都是线程安全的
3. **异常处理**: 建议使用Result类型进行错误处理
4. **内存泄漏**: 注意及时取消协程和移除监听器
5. **性能**: 大量数据查询时建议使用分页和限制

## 依赖要求

- Android API 21+
- Kotlin 1.8+
- Kotlinx Coroutines
- AndroidX Core

## 许可证

MIT License