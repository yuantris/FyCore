package io.core.common.net

import androidx.annotation.Keep

// 1. 基础数据类和枚举
@Keep
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

sealed class ResponseResult<out T> {
    data class Success<T>(val data: T) : ResponseResult<T>()
    data class Error(
        val code: Int,
        val message: String,
        val type: ErrorType = ErrorType.INVISIBLE
    ) : ResponseResult<Nothing>()

    data class BusinessError<T>(
        val code: Int,
        val message: String,
        val data: T?,
        val customAction: (() -> Unit)? = null
    ) : ResponseResult<T>()

    data class Custom<T>(val code: Int, val message: String, val data: T?, val action: () -> Unit) :
        ResponseResult<T>()
}

enum class ErrorType {
    INTERFACE, BUSINESS, INVISIBLE, CUSTOM
}

// code1 请求成功,  code < 0  请求错误,用户可见, code > 1 请求错误 用户不可见, -7777 接口限制
enum class DefaultStrategyType {
    SUCCESS, BUSINESS_ERROR, SERVER_ERROR, INTERFACE_ERROR
}

// 2. 策略接口和实现
interface ResponseStrategy<T> {
    fun canHandle(code: Int): Boolean
    fun handle(code: Int, message: String, data: T?): ResponseResult<T>
}

class SuccessStrategy<T> : ResponseStrategy<T> {
    override fun canHandle(code: Int) = code == 1
    override fun handle(code: Int, message: String, data: T?) =
        if (data != null) ResponseResult.Success(data)
        else ResponseResult.Error(code, message)
}

class BusinessErrorStrategy<T> : ResponseStrategy<T> {
    override fun canHandle(code: Int) = code < 0 && code != -7777
    override fun handle(code: Int, message: String, data: T?) =
        ResponseResult.BusinessError(code, message, data)
}

class ServerErrorStrategy<T> : ResponseStrategy<T> {
    override fun canHandle(code: Int) = code > 1
    override fun handle(code: Int, message: String, data: T?) =
        ResponseResult.Error(code, message, ErrorType.INVISIBLE)
}


class InterfaceErrorStrategy<T> : ResponseStrategy<T> {
    override fun canHandle(code: Int) = code == -7777
    override fun handle(code: Int, message: String, data: T?) =
        ResponseResult.Error(code, message, ErrorType.INTERFACE)
}


class CustomCodeStrategy<T>(
    private val targetCode: Int,
    private val customAction: () -> Unit
) : ResponseStrategy<T> {
    override fun canHandle(code: Int) = code == targetCode
    override fun handle(code: Int, message: String, data: T?) =
        ResponseResult.Custom(code, message, data, customAction)
}

// 3. 核心处理器
class FlexibleResponseProcessor<T> {
    private val strategies = mutableListOf<ResponseStrategy<T>>()
    internal val strategyMap = mutableMapOf<DefaultStrategyType, ResponseStrategy<T>>()

    fun addStrategy(strategy: ResponseStrategy<T>): FlexibleResponseProcessor<T> {
        strategies.add(strategy)
        return this
    }

    fun addStrategyAtFirst(strategy: ResponseStrategy<T>): FlexibleResponseProcessor<T> {
        strategies.add(0, strategy)
        return this
    }

    fun removeStrategy(type: DefaultStrategyType) {
        strategyMap[type]?.let { strategy ->
            strategies.remove(strategy)
            strategyMap.remove(type)
        }
    }

    fun process(response: ApiResponse<T>): ResponseResult<T> {
        val strategy = strategies.firstOrNull { it.canHandle(response.code) }
        return strategy?.handle(response.code, response.message, response.data)
            ?: ResponseResult.Error(response.code, response.message)
    }

    fun process(
        response: ApiResponse<T>,
        block: ResponseHandlerDsl<T>.() -> Unit
    ) {
        val result = process(response)
        val dsl = ResponseHandlerDsl<T>()
        dsl.block()

        when (result) {
            is ResponseResult.Success -> dsl.successAction?.invoke(result.data)
            is ResponseResult.Error -> dsl.errorActions[result.type]?.invoke(
                result.code,
                result.message
            )

            is ResponseResult.BusinessError -> {
                dsl.businessErrorAction?.invoke(result.code, result.message, result.data)
                result.customAction?.invoke()
            }

            is ResponseResult.Custom -> {
                dsl.customActions[result.code]?.invoke(result.code, result.message, result.data)
                result.action.invoke()
            }
        }
    }
}

// 4. DSL构建器
class ResponseHandlerDsl<T> {
    var successAction: ((T) -> Unit)? = null
    var businessErrorAction: ((Int, String, T?) -> Unit)? = null
    val errorActions = mutableMapOf<ErrorType, (Int, String) -> Unit>()
    val customActions = mutableMapOf<Int, (Int, String, T?) -> Unit>()

    fun onSuccess(action: (T) -> Unit) {
        successAction = action
    }

    fun onError(type: ErrorType = ErrorType.INVISIBLE, action: (Int, String) -> Unit) {
        errorActions[type] = action
    }

    fun onBusinessError(action: (Int, String, T?) -> Unit) {
        businessErrorAction = action
    }

    fun onCustomCode(code: Int, action: (Int, String, T?) -> Unit) {
        customActions[code] = action
    }
}

// 5. 构建器（默认包含常用策略）
class ResponseProcessorBuilder<T> {
    private val processor = FlexibleResponseProcessor<T>()

    init {
        addDefaultStrategies()
    }

    private fun addDefaultStrategies() {
        val successStrategy = SuccessStrategy<T>()
        val businessStrategy = BusinessErrorStrategy<T>()
        val serverStrategy = ServerErrorStrategy<T>()
        val interfaceStrategy = InterfaceErrorStrategy<T>()


        processor.addStrategy(successStrategy)
        processor.addStrategy(interfaceStrategy)
        processor.addStrategy(businessStrategy)
        processor.addStrategy(serverStrategy)

        processor.strategyMap[DefaultStrategyType.SUCCESS] = successStrategy
        processor.strategyMap[DefaultStrategyType.INTERFACE_ERROR] = interfaceStrategy
        processor.strategyMap[DefaultStrategyType.BUSINESS_ERROR] = businessStrategy
        processor.strategyMap[DefaultStrategyType.SERVER_ERROR] = serverStrategy
    }

    fun removeDefaultStrategy(strategyType: DefaultStrategyType) = apply {
        processor.removeStrategy(strategyType)
    }

    fun addCustomStrategy(
        codes: IntRange,
        handler: (Int, String, T?) -> ResponseResult<T>
    ) = apply {
        processor.addStrategyAtFirst(object : ResponseStrategy<T> {
            override fun canHandle(code: Int) = code in codes
            override fun handle(code: Int, message: String, data: T?) = handler(code, message, data)
        })
    }

    fun addSpecificCodeStrategy(
        code: Int,
        action: () -> Unit
    ) = apply {
        processor.addStrategyAtFirst(CustomCodeStrategy(code, action))
    }

    fun build() = processor
}

// 6. 工厂类
object ResponseProcessorFactory {

    fun <T> createDefault(): FlexibleResponseProcessor<T> {
        return ResponseProcessorBuilder<T>().build()
    }

    fun <T> createSimple(): FlexibleResponseProcessor<T> {
        return ResponseProcessorBuilder<T>()
            .removeDefaultStrategy(DefaultStrategyType.SERVER_ERROR)
            .build()
    }

    fun <T> createCustom(block: ResponseProcessorBuilder<T>.() -> Unit): FlexibleResponseProcessor<T> {
        val builder = ResponseProcessorBuilder<T>()
        builder.block()
        return builder.build()
    }
}