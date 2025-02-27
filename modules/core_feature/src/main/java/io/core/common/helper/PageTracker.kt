package io.core.common.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.core.BuildConfig
import io.core.common.util.extensions.activityManager
import io.core.common.util.tools.isAndroid9Plus
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

object PageTracker {

    private const val DEFAULT_TAG = "PageTracker"
    private const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    private val config = AtomicReference(TrackConfig.createDefault())
    private val processNameCache = AtomicReference<String>()

    @JvmStatic
    @JvmOverloads
    fun initialize(application: Application, initializer: TrackConfig.Builder.() -> Unit = {}) {
        val builder = TrackConfig.Builder().apply(initializer)
        config.set(builder.build().applyDefaults())
        if (config.get().enabled) {
            registerLifecycle(application)
        }
    }

    private fun registerLifecycle(application: Application) {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!config.get().brief) {
                    handleActivityEvent(activity, "Created")
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                handleActivityEvent(activity, "Resumed")
            }

            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                if (!config.get().brief) {
                    handleActivityEvent(activity, "Destroyed")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    private fun handleActivityEvent(activity: Activity, event: String) {
        val currentConfig = config.get()
        if (!currentConfig.enabled) return

        val logInfo = buildLogInfo(activity, event).apply {
            putAll(collectExtraInfo(activity))
        }

        currentConfig.logger.log(formatLogMessage(logInfo, currentConfig.tag))
    }


    @SuppressLint("NewApi")
    private fun buildLogInfo(
        activity: Activity,
        event: String
    ): LinkedHashMap<String, Any> {
        return linkedMapOf(
            "timestamp" to getCurrentTime(),
            "process" to getProcessInfo(),
            "page" to getActivityName(activity),
            "event" to event,
        )
    }

    private fun getActivityName(activity: Activity): String {
        return activity.javaClass.simpleName.ifEmpty { "Unknown" }
    }


    private fun collectExtraInfo(activity: Activity): Map<String, Any> {
        return config.get().extraProviders
            .flatMap { it.provide(activity).toList() }
            .toMap()
    }


    private fun formatLogMessage(logInfo: Map<String, Any>, tag: String): String {
        return buildString {
            append("[$tag]")
            logInfo.forEach { (key, value) ->
                append(" $key=${value.toString().replace(" ", "_")}")
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat(config.get().dateFormat, Locale.getDefault()).format(Date())
    }

    @VisibleForTesting
    internal fun getProcessInfo(): String {
        return processNameCache.get() ?: run {
            val name = fetchProcessName().replace(":", "_")
            processNameCache.set(name)
            name
        }
    }

    private fun fetchProcessName(): String {
        return try {
            val pid = android.os.Process.myPid()
            if (isAndroid9Plus) {
                "[$pid-${Application.getProcessName()}]"
            } else {
                "[$pid-${
                    activityManager.runningAppProcesses
                        ?.find { it.pid == pid }
                        ?.processName ?: "unknown"
                }]"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }


    class TrackConfig private constructor(
        val enabled: Boolean,
        val brief: Boolean,
        val tag: String,
        val dateFormat: String,
        val logger: Logger,
        val extraProviders: List<ExtraInfoProvider>
    ) {

        companion object {
            fun createDefault() = Builder().build() // 添加工厂方法
        }

        internal fun applyDefaults(): TrackConfig {
            return if (dateFormat.isEmpty()) {
                copy(dateFormat = DEFAULT_DATE_FORMAT)
            } else this
        }

        fun copy(
            enabled: Boolean = this.enabled,
            brief: Boolean = this.brief,
            tag: String = this.tag,
            dateFormat: String = this.dateFormat,
            logger: Logger = this.logger,
            extraProviders: List<ExtraInfoProvider> = this.extraProviders
        ) = TrackConfig(enabled, brief, tag, dateFormat, logger, extraProviders)

        class Builder {
            private var enabled = !BuildConfig.DEBUG
            private var brief = false
            private var tag = DEFAULT_TAG
            private var dateFormat = DEFAULT_DATE_FORMAT
            private var logger: Logger = DefaultLogger()
            private val providers = mutableListOf<ExtraInfoProvider>()

            fun enable(enabled: Boolean) = apply { this.enabled = enabled }
            fun brief(brief: Boolean) = apply { this.brief = brief }
            fun setTag(tag: String) = apply { this.tag = tag }
            fun setDateFormat(format: String) = apply { this.dateFormat = format }
            fun setLogger(logger: Logger) = apply { this.logger = logger }
            fun addProvider(provider: ExtraInfoProvider) = apply { providers.add(provider) }

            fun build() = TrackConfig(
                enabled = enabled,
                brief = brief,
                tag = tag,
                dateFormat = dateFormat,
                logger = logger,
                extraProviders = providers
            )
        }
    }

    fun interface Logger {
        fun log(message: String)
    }

    fun interface ExtraInfoProvider {
        fun provide(activity: Activity): Map<String, Any>
    }

    private class DefaultLogger : Logger {
        override fun log(message: String) {
            Log.println(Log.VERBOSE, DEFAULT_TAG, message)
        }
    }

    @JvmStatic
    fun updateConfig(updater: TrackConfig.Builder.() -> Unit) {
        val newConfig = TrackConfig.Builder().apply {
            enable(config.get().enabled)
            brief(config.get().brief)
            setTag(config.get().tag)
            setDateFormat(config.get().dateFormat)
            setLogger(config.get().logger)
            config.get().extraProviders.forEach { addProvider(it) }
            updater()
        }.build()
        config.set(newConfig)
    }
}

// ProGuard 规则
/*
-keep class your.package.name.PageTracker { *; }
-keep interface your.package.name.PageTracker$Logger { *; }
-keep interface your.package.name.PageTracker$ExtraInfoProvider { *; }
-keepnames class * extends android.app.Activity
-keepnames class * extends androidx.fragment.app.Fragment
*/