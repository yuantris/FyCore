# BaseViewModelV2 使用指南

BaseViewModelV2是一个全新设计的ViewModel基类，专门针对API调用、数据库操作、文件操作进行了优化，提供了统一的状态管理和UI事件处理机制。

## 🚀 主要特性

- **统一状态管理**：使用`ViewState`密封类管理5种视图状态
- **事件驱动架构**：通过`UiEvent`处理一次性UI事件
- **专门的数据操作方法**：针对API、数据库、文件操作的优化方法
- **完善的错误处理**：自动分类和处理不同类型的错误
- **实用工具**：防抖动、重试机制、延迟执行等
- **现代化响应式**：基于StateFlow/SharedFlow，性能更优

## 📋 核心组件

### ViewState（视图状态）

```kotlin
sealed class ViewState<out T> {
    object Idle        // 初始状态
    object Loading     // 加载中
    data class Success<T>(val data: T)  // 成功，携带数据
    data class Error(val message: String, val code: Int? = null)  // 错误
    object Empty       // 空数据
}
```

### UiEvent（UI事件）

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String)
    data class ShowSnackbar(val message: String, val actionText: String?, val action: (() -> Unit)?)
    data class ShowDialog(val title: String, val message: String, val onConfirm: (() -> Unit)?)
    object HideKeyboard
    object ShowLoading / HideLoading
    data class NavigateTo(val route: String)
    object NavigateBack
}
```

## 🛠️ 基本使用

### 1. 创建ViewModel

```kotlin
class UserViewModel : BaseViewModelV2<List<User>>() {
    
    // API调用
    fun loadUsers() {
        apiCall(
            api = { ApiService.getUsers() },
            onSuccess = { users ->
                if (users.isEmpty()) {
                    setEmpty()
                } else {
                    showToast("加载成功")
                }
            },
            showToast = true  // 出错时自动显示Toast
        )
    }
    
    // 数据库操作
    fun saveUser(user: User) {
        dbOperation(
            operation = { DatabaseHelper.insert(user) },
            onSuccess = { 
                showToast("保存成功")
                loadUsers() // 刷新列表
            }
        )
    }
    
    // 文件操作
    fun exportData(filePath: String) {
        fileOperation(
            operation = { FileHelper.writeData(filePath, getCurrentData()) },
            onSuccess = { 
                showSnackbar("导出成功", "打开") {
                    navigateTo("file_manager")
                }
            }
        )
    }
}
```

### 2. 在Activity/Fragment中观察状态

```kotlin
class UserActivity : AppCompatActivity() {
    
    private val viewModel: UserViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 观察视图状态
        viewModel.observeViewState(
            lifecycleOwner = this,
            onLoading = { 
                showProgressBar() 
            },
            onSuccess = { users ->
                hideProgressBar()
                updateUserList(users)
            },
            onError = { message, code ->
                hideProgressBar()
                showErrorDialog(message)
            },
            onEmpty = {
                hideProgressBar()
                showEmptyView()
            }
        )
        
        // 观察UI事件
        viewModel.observeUiEvents(
            lifecycleOwner = this,
            onShowToast = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            },
            onShowSnackbar = { message, actionText, action ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .apply {
                        if (actionText != null && action != null) {
                            setAction(actionText) { action() }
                        }
                    }
                    .show()
            },
            onNavigateTo = { route ->
                // 处理导航
            }
        )
    }
}
```

### 3. 简化的观察方式

```kotlin
// 只关注成功和错误
viewModel.observeResult(
    lifecycleOwner = this,
    onSuccess = { users -> updateUI(users) },
    onError = { message -> showError(message) }
)
```

## 🔧 高级功能

### 1. 防抖动操作

```kotlin
fun onSearchClick() {
    if (isDoubleClick()) return  // 防止重复点击
    
    searchUsers(searchKeyword)
}
```

### 2. 延迟执行

```kotlin
fun delayedSearch(keyword: String) {
    delayedAction(300) { // 300ms后执行
        if (keyword.isNotBlank()) {
            searchUsers(keyword)
        }
    }
}
```

### 3. 重试机制

```kotlin
fun loadDataWithRetry() {
    retryAction(
        maxRetries = 3,
        delayMs = 1000,
        action = { ApiService.getData() },
        onSuccess = { data -> 
            showToast("加载成功") 
        },
        onFinalError = { error ->
            showSnackbar("加载失败", "重试") {
                loadDataWithRetry()
            }
        }
    )
}
```

### 4. 自定义执行器

```kotlin
fun customOperation() {
    execute(
        showLoading = false,  // 不显示loading
        onStart = { 
            showToast("开始处理...") 
        },
        action = {
            // 自定义异步操作
            processData()
        },
        onComplete = {
            showToast("处理完成")
        }
    )
}
```

## 📱 实际应用场景

### 场景1：用户列表页面

```kotlin
class UserListViewModel : BaseViewModelV2<List<User>>() {
    
    fun loadUsers() {
        apiCall(
            api = { userApi.getUsers() },
            onSuccess = { users ->
                if (users.isEmpty()) {
                    setEmpty()
                } else {
                    showToast("加载了${users.size}个用户")
                }
            }
        )
    }
    
    fun refreshUsers() {
        // 下拉刷新
        retryAction(
            maxRetries = 2,
            action = { userApi.getUsers() },
            onFinalError = { 
                showSnackbar("刷新失败", "重试") { refreshUsers() }
            }
        )
    }
    
    fun deleteUser(userId: String) {
        showDialog("确认删除", "确定要删除这个用户吗？") {
            dbOperation(
                operation = { userDao.delete(userId) },
                onSuccess = { 
                    showToast("删除成功")
                    loadUsers() // 重新加载
                }
            )
        }
    }
}
```

### 场景2：设置页面

```kotlin
class SettingsViewModel : BaseViewModelV2<AppSettings>() {
    
    fun loadSettings() {
        fileOperation(
            operation = { settingsFile.read() },
            onSuccess = { settings ->
                // 设置加载成功
            }
        )
    }
    
    fun saveSetting(key: String, value: Any) {
        dbOperation(
            operation = { 
                settingsDao.update(key, value)
                settingsDao.getAll()
            },
            onSuccess = { 
                showToast("设置已保存") 
            }
        )
    }
    
    fun exportSettings() {
        fileOperation(
            operation = { 
                val settings = getCurrentSettings()
                fileHelper.export(settings)
            },
            onSuccess = {
                showSnackbar("导出成功", "分享") {
                    navigateTo("share_screen")
                }
            }
        )
    }
}
```

## 🎯 最佳实践

### 1. 状态检查

```kotlin
// 检查当前状态
if (isCurrentlyLoading()) {
    return // 避免重复请求
}

// 获取当前数据
val currentData = when (val state = getCurrentState()) {
    is ViewState.Success -> state.data
    else -> emptyList()
}
```

### 2. 错误处理

```kotlin
// 自定义错误处理
apiCall(
    api = { service.getData() },
    onError = { errorMsg ->
        when {
            errorMsg.contains("网络") -> {
                showSnackbar("网络异常", "设置") {
                    navigateTo("network_settings")
                }
            }
            errorMsg.contains("权限") -> {
                showDialog("权限不足", "请联系管理员")
            }
            else -> {
                showToast(errorMsg)
            }
        }
    }
)
```

### 3. 组合操作

```kotlin
fun syncData() {
    execute(
        action = {
            // 1. 先从API获取最新数据
            val apiData = apiService.getData()
            
            // 2. 保存到数据库
            database.saveAll(apiData)
            
            // 3. 返回合并后的数据
            database.getAllWithLocal()
        },
        onComplete = {
            showToast("同步完成")
        }
    )
}
```

## 🔍 扩展方法

使用提供的扩展方法可以进一步简化代码：

```kotlin
// 使用类型别名
class MyViewModel : BaseVM<String>() {
    
    fun loadData() {
        // 使用工厂方法
        _viewState.value = ViewStateFactory.loading()
        
        // 发送事件
        emitEvent(UiEventFactory.toast("开始加载"))
    }
}

// 状态检查扩展
val isSuccess = viewModel.viewState.value.isSuccess
val data = viewModel.viewState.value.dataOrNull
val errorMsg = viewModel.viewState.value.errorMessageOrNull
```

## 🚨 注意事项

1. **避免在UI线程执行耗时操作**：所有的`action`都会在协程中执行
2. **合理使用loading状态**：数据库操作通常很快，可以设置`showLoading = false`
3. **错误处理要全面**：根据不同的数据源选择合适的错误处理方式
4. **防抖动很重要**：用户操作频繁的地方要使用`isDoubleClick()`
5. **状态管理要清晰**：及时更新状态，避免UI显示不一致

## 🔄 迁移指南

从旧的BaseViewModel迁移到BaseViewModelV2：

1. **替换继承**：`BaseViewModel` → `BaseViewModelV2<T>`
2. **更新状态观察**：使用新的`observeViewState`方法
3. **替换方法调用**：
   - `launch()` → `execute()`
   - `emit()` → `apiCall()`/`dbOperation()`/`fileOperation()`
4. **更新错误处理**：利用新的自动错误分类机制
5. **添加事件观察**：使用`observeUiEvents`处理UI事件

BaseViewModelV2提供了更现代、更强大、更易用的ViewModel基础架构，帮助你构建更稳定和用户友好的应用程序。