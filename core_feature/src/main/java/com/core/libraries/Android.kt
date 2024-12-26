package com.core.libraries

import android.app.Application

class Android private constructor() {
    companion object {
        private lateinit var _context: Application

        val context: Application
            get() = _context

        fun init(application: Application) {
            if (::_context.isInitialized) {
                throw IllegalStateException("Android context is already initialized")
            }
            _context = application
        }
    }
}