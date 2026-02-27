package com.core.fy.android.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpClient {

    /* ======================== 配置�?======================== */
    private object Config {
        const val BASE_URL = "https://majestic-cuchufli-2d0dc9.netlify.app"
        const val CONFIG_PATH = "/config.json"

        const val MAX_RETRY = 3
        val TIMEOUT = 30L to TimeUnit.SECONDS
    }

    /* ======================== OkHttp ======================== */
    private val defaultOkHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Config.TIMEOUT.first, Config.TIMEOUT.second)
            .readTimeout(Config.TIMEOUT.first, Config.TIMEOUT.second)
            .writeTimeout(Config.TIMEOUT.first, Config.TIMEOUT.second)
            .build()
    }

    /* ======================== API ======================== */
    /**
     * 获取远端配置
     * @param client 允许外部注入，单测可 mock
     * @return Result<String> 成功返回 JSON；失败携�?NetworkException
     */
    suspend fun getConfig(client: OkHttpClient = defaultOkHttp): Result<String> =
        withContext(Dispatchers.IO) {
            val url = "${Config.BASE_URL}${Config.CONFIG_PATH}"
            val request = Request.Builder().url(url).get().build()

            repeat(Config.MAX_RETRY) { attempt ->
                runCatching {
                    client.newCall(request).execute().use { rsp ->
                        if (!rsp.isSuccessful) throw NetworkException.Http(rsp.code)
                        rsp.body?.string() ?: throw NetworkException.EmptyBody
                    }
                }
                    .onSuccess { return@withContext Result.success(it) }
                    .onFailure { e ->
                        when (e) {
                            is NetworkException -> return@withContext Result.failure(e)
                            is IOException -> if (attempt == Config.MAX_RETRY - 1) {
                                return@withContext Result.failure(NetworkException.IO(e))
                            }
                        }
                    }

                // 指数退�?
                delay((1 shl attempt) * 500L)
            }

            Result.failure(NetworkException.MaxRetryExceeded)
        }
}

/* ======================== 异常模型 ======================== */
sealed class NetworkException : RuntimeException() {
    object EmptyBody : NetworkException()
    object MaxRetryExceeded : NetworkException()
    data class Http(val code: Int) : NetworkException()
    data class IO(val origin: IOException) : NetworkException()
}