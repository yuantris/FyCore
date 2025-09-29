# Android 生产级导航拦截器模块说明（Nav）

本说明文档专用于 io.core.nav 模块，涵盖架构、用法、拦截器、回调、结果语义与最佳实践。建议在业务侧仅维护此文档，避免与仓库根 README 重复。

## 目录
- 快速开始
- 核心概念与架构
- 基本用法
- 结果类型 NavigationResult
- 监听器 NavigationListener（含 onInterceptorExecute）
- 拦截器 NavigationInterceptor
- 路由注册 RouteRegistry
- 执行器 NavigationExecutor
- 登录回跳 LoginDispatcher
- 异步与协程作用域
- 测试与排错
- 常见问题（FAQ）

---

## 快速开始

在 Application.onCreate 中初始化（推荐通过 NavigationManager）：
```kotlin
NavigationManager.initialize {
    // 可选：监听器
    setListener(object : NavigationListener {
        override fun onNavigationStart(request: NavigationRequest) = Unit
        override fun onInterceptorExecute(interceptor: NavigationInterceptor, request: NavigationRequest) = Unit
        override fun onNavigationResult(result: NavigationResult) = Unit
    })

    // 示例：登录拦截器
    addInterceptor(
        LoginInterceptor(
            authProvider = object : AuthProvider {
                override suspend fun isLoggedIn() = false
                override fun isLoggedInSync() = false
            },
            loginActivityClass = YourLoginActivity::class,
            protectedActivities = setOf(YourProtectedActivity::class),
        )
    )

    // 可选：注册路由
    // registerRoute<HomeActivity>("home")
}
```

在任意位置使用：
```kotlin
// 类型安全导航
navigator.to<SettingsActivity>(ctx)
    .with("section", "privacy")
    .go()

// 路由导航
navigator.to(ctx, "profile")
    .with("userId", "123")
    .go()

// 异步导航（主线程回调）
navigator.navigateAsync(ctx, Intent(ctx, OrderActivity::class.java)) { result ->
    // 按 NavigationResult 分支处理
}
```

---

## 动态配置（运行时修改）

初始化完成后，可以动态添加拦截器、修改执行器等：

```kotlin
// 动态添加拦截器
NavigationManager.addInterceptor(NewFeatureInterceptor(), priority = 10)

// 动态移除拦截器
NavigationManager.removeInterceptor(oldInterceptor)
NavigationManager.removeInterceptor(LoginInterceptor::class.java)

// 动态设置执行器
NavigationManager.setExecutor(CustomExecutor())

// 动态设置监听器
NavigationManager.setListener(newListener)

// 动态添加路由
NavigationManager.addRoute("/new-feature", NewFeatureActivity::class.java)

// 动态移除路由
NavigationManager.removeRoute("/old-feature")

// 批量配置
NavigationManager.configure {
    addInterceptor(AnalyticsInterceptor(), priority = 5)
    addRoute("/analytics", AnalyticsActivity::class.java)
    setListener(ProductionListener())
}
```

**使用场景**：
- **模块化初始化**：各业务模块在自己的初始化时机添加拦截器
- **插件式扩展**：动态加载功能模块时注册路由和拦截器
- **A/B 测试**：根据实验配置动态调整导航行为
- **运行时配置**：根据用户权限、网络状态等动态调整拦截器
- **热修复场景**：紧急修复时动态替换执行器或拦截器

**注意事项**：
- 所有动态配置方法都是线程安全的（使用 synchronized）
- 支持链式调用，便于批量操作
- 可以通过 `getInterceptors()`、`getExecutor()`、`getListener()` 查看当前配置

---

## 核心概念与架构

- Navigator：对外导航入口（Builder 模式构造），支持同步/异步。
- InterceptorChain：拦截器链，按 priority 升序执行，支持同步/挂起拦截。
- NavigationResult：导航结果的强类型表达（Proceed/Redirect/Abort/Error）。
- NavigationListener：导航生命周期回调，含 onInterceptorExecute。
- NavigationExecutor：结果执行器（默认直接 startActivity，可自定义）。
- RouteRegistry：字符串路由注册与解析。

层级关系（简图）：
```
Navigator → NavigationBuilder → InterceptorChain → NavigationExecutor
                      └─ RouteRegistry
                ↑ listener (NavigationListener)
```

---

## 基本用法

1) 类型安全跳转
```kotlin
navigator
  .to<ProfileActivity>(context)
  .with("userId", "123")
  .with("source", "main")
  .go()
```

2) 路由跳转
```kotlin
navigator
  .to(context, "profile")
  .with("userId", "123")
  .go()
```

3) 异步跳转
```kotlin
navigator
  .to<OrderActivity>(context)
  .goAsync { result ->
      when (result) {
          is NavigationResult.Proceed -> { /* 成功 */ }
          is NavigationResult.Redirect -> { /* 被重定向（如登录）*/ }
          is NavigationResult.Abort -> { /* 业务性终止 */ }
          is NavigationResult.Error -> { /* 异常 */ }
      }
  }
```

4) Intent flags/action
```kotlin
navigator
  .to<HomeActivity>(context)
  .flags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
  .go()
```

---

## 结果类型 NavigationResult

NavigationResult 定义在 NavigationCore.kt，下列四种语义清晰、互斥：

- Proceed(intent, originalIntent)
  - 放行：按照最终 intent 启动目标页（可能经过拦截器改写）。
- Redirect(intent, originalIntent, reason?)
  - 重定向：跳往另一个页面（例如登录页/权限说明页）。
- Abort(reason, originalIntent)
  - 业务性终止：不启动任何页面。例如路由缺失、用户取消、频控/灰度关闭、门禁不通过但不应扰动用户。
  - 该类型已在模块中实际使用（如 RouteNavigationBuilder 路由不存在时返回 Abort），建议保留，便于与 Redirect/Error 区分。
- Error(exception, originalIntent)
  - 异常终止：拦截器/执行器/系统调用出错，应上报与告警。

建议在监听器/执行器中按类型区分处理，提升可观测性与用户体验。

---

## 监听器 NavigationListener

回调时机：
- onNavigationStart(request)：开始执行导航前。
- onInterceptorExecute(interceptor, request)：每个拦截器要执行前。
- onNavigationResult(result)：导航完成后（包含 Proceed/Redirect/Abort/Error）。

重要说明（已接入）：
- onInterceptorExecute 之前未被触发的问题已修复。现在 InterceptorChain 在执行每个拦截器前回调 listener?.onInterceptorExecute(interceptor, request)，并从 Navigator 传入 listener。

示例：
```kotlin
setListener(object : NavigationListener {
    override fun onNavigationStart(request: NavigationRequest) {
        // 打点/日志：目标页、来源、参数
    }
    override fun onInterceptorExecute(interceptor: NavigationInterceptor, request: NavigationRequest) {
        // 记录拦截器执行顺序与耗时
    }
    override fun onNavigationResult(result: NavigationResult) {
        // 成功率/重定向原因/业务终止原因/异常上报
    }
})
```

---

## 拦截器 NavigationInterceptor

接口概览：
```kotlin
interface NavigationInterceptor {
    val priority: Int get() = 0
    val name: String get() = this::class.java.simpleName

    fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult

    suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        return intercept(request, chain)
    }
}
```

用法要点：
- priority 越小越先执行（如权限检查 50，登录检查 100，埋点 200）。
- 同步与挂起拦截任选其一，建议在耗时逻辑（网络/IO）中使用 interceptSuspend。
- 返回值选择：
  - 放行：return chain.proceed(request) 或 chain.proceedSuspend(request)
  - 重定向：return NavigationResult.Redirect(newIntent, request.originalIntent, "需要登录")
  - 终止：return NavigationResult.Abort("频控触发", request.originalIntent)
  - 异常：try/catch 内返回 NavigationResult.Error

示例自定义拦截器：
```kotlin
addInterceptor(object : NavigationInterceptor {
    override val priority = 150
    override val name = "ABTestInterceptor"

    override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 轻量逻辑在同步执行
        return chain.proceed(request)
    }
})
```

---

## 路由注册 RouteRegistry

注册与解析：
```kotlin
// 注册（构建期）
registerRoute<ProfileActivity>("profile")
registerRoute<SettingsActivity>("settings")

// 使用
navigator.to(context, "profile").go()
```

如果路由未注册：
- RouteNavigationBuilder.go() 会返回 NavigationResult.Abort("Route not found: $route", Intent())，不会启动页面。

---

## 执行器 NavigationExecutor

默认执行器：
- DefaultNavigationExecutor：Proceed/Redirect 直接启动，Abort/Error 留给监听器处理（可按需增强）。

生产示例（可加兜底提示/上报）：
```kotlin
class ProductionNavigationExecutor : NavigationExecutor {
    override fun execute(context: Context, result: NavigationResult) {
        when (result) {
            is NavigationResult.Proceed,
            is NavigationResult.Redirect -> context.startActivity(result.intent)
            is NavigationResult.Abort -> {
                // 轻提示或沉默处理，按业务策略决定
            }
            is NavigationResult.Error -> {
                // 日志上报 + 友好提示
            }
        }
    }
}
```

---

## 登录回跳 LoginDispatcher

登录成功后回跳最初的目标页面：
```kotlin
LoginDispatcher.dispatchAfterLogin(
    activity = this,
    navigator = NavigationManager.getNavigator()
) {
    // 兜底：无回跳信息时跳首页
    navigator.to<HomeActivity>(this).go()
}
```

---

## 异步与协程作用域

- Navigator 在 NavigationManager.initialize 时注入专用 CoroutineScope(SupervisorJob)，避免使用 GlobalScope 造成泄漏。
- navigateAsync 与 executeNavigationSuspend 保证回调切换到主线程执行执行器。

---

## 测试与排错

单元测试建议：
```kotlin
// 重置
NavigationManager.reset()

// 初始化测试配置
NavigationManager.initialize {
    setListener(object : NavigationListener {})
    // 注入测试用拦截器/执行器/路由
}

// 执行
val result = NavigationManager.getNavigator()
    .to<TestActivity>(mockContext)
    .go()

assert(result is NavigationResult.Proceed)
```

排错建议：
- 监听 onInterceptorExecute 获取执行顺序与关键参数。
- 监听 onNavigationResult 统计 Proceed/Redirect/Abort/Error 分布。
- 路由缺失导致 Abort 时，补齐 registerRoute 或在业务侧容错处理。
- 异常路径统一上报（Error）。

---

## 常见问题（FAQ）

Q1：为什么要保留 Abort？
- Abort 表示“业务可预期的终止且不启动页面”，与 Redirect（跳到其他页）和 Error（异常）语义不同，便于埋点与用户体验差异化。模块内已实际使用（路由缺失）。

Q2：onInterceptorExecute 会在什么时候触发？
- 已在 InterceptorChain 执行每个拦截器前回调。可用于记录执行顺序、耗时、异常定位。

Q3：拦截器顺序如何确定？
- 按 priority 升序执行，数值越小越先执行。典型：权限 50 → 登录 100 → A/B 150 → 埋点 200。

Q4：异步/线程切换如何保证？
- 执行器调用被切回主线程（在异步路径），线程安全且不会阻塞 UI。

---

如需扩展或变更约定（例如统一提示策略、结果上报规范），建议在此文档更新并与业务侧同步。