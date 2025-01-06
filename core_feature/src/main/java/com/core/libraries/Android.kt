package com.core.libraries

import android.app.Application
import kotlin.properties.Delegates

class Android private constructor() {

    companion object {
        // 全局APPLICATION上下文
        private lateinit var _context: Application
        // 是否是DEBUG模式
        private var _debug by Delegates.notNull<Boolean>()

        val context: Application
            get() {
                if (!::_context.isInitialized) {
                    throw IllegalStateException("Android context has not been initialized")
                }
                return _context
            }

        val debug: Boolean
            get() {
                return _debug
            }

        /**
         * 初始化FyCore全局APPLICATION上下文
         */
        fun initialize(application: Application, debug: Boolean = true) {
            if (::_context.isInitialized) {
                throw IllegalStateException("Android context is already initialized")
            }
            _debug = debug
            _context = application
        }
    }
}