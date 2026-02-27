package io.core.utils.tools.file

/**
 * 文件操作结果封装�?
 * 使用密封类提供类型安全的错误处理
 */
sealed class FileOperationResult<out T> {
    data class Success<T>(val data: T) : FileOperationResult<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : FileOperationResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): FileOperationResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): FileOperationResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Throwable, String) -> Unit): FileOperationResult<T> {
        if (this is Error) action(exception, message)
        return this
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
}

/**
 * 扩展函数：安全执行文件操�?
 */
inline fun <T> safeFileOperation(operation: () -> T): FileOperationResult<T> {
    return try {
        FileOperationResult.Success(operation())
    } catch (e: Exception) {
        FileOperationResult.Error(e)
    }
}