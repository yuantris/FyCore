package com.core.calendar

/**
 * 日历操作回调接口
 * 支持Java友好的函数式接口
 */
fun interface CalendarCallback<T> {
    fun onResult(result: Result<T>)
    
    // 提供默认实现用于Java
    companion object {
        @JvmStatic
        fun <T> create(
            onSuccess: (T) -> Unit,
            onError: (Throwable) -> Unit = {}
        ): CalendarCallback<T> = CalendarCallback { result ->
            result.fold(onSuccess, onError)
        }
        
        @JvmStatic
        fun <T> onSuccess(onSuccess: (T) -> Unit): CalendarCallback<T> = 
            CalendarCallback { result ->
                result.onSuccess(onSuccess)
            }
        
        @JvmStatic
        fun <T> onFailure(onError: (Throwable) -> Unit): CalendarCallback<T> = 
            CalendarCallback { result ->
                result.onFailure(onError)
            }
    }
}