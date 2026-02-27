package io.core.utils.tools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.Reader
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

object GsonTools {

    private const val KEY_DEFAULT = "defaultGson"
    private const val KEY_DELEGATE = "delegateGson"
    private const val KEY_LOG_UTILS = "logUtilsGson"

    private val GSONS = ConcurrentHashMap<String, Gson>()

    /**
     * 设置[Gson]的代理实�?
     *
     * @param delegate [Gson]的代理实�?
     */
    fun setGsonDelegate(delegate: Gson?) {
        delegate ?: return
        GSONS[KEY_DELEGATE] = delegate
    }

    /**
     * 根据key设置[Gson]实例
     *
     * @param key 键�?
     * @param gson [Gson]实例
     */
    fun setGson(key: String?, gson: Gson?) {
        key?.let { str ->
            gson?.let {
                GSONS[str] = it
            }
        }
    }

    /**
     * 根据key获取[Gson]实例
     *
     * @param key 键�?
     * @return 对应的[Gson]实例
     */
    fun getGson(key: String): Gson? = GSONS[key]

    fun getGson(): Gson {
        GSONS[KEY_DELEGATE]?.let { return it }
        return GSONS.getOrPut(KEY_DEFAULT) { createGson() }
    }

    /**
     * 将对象序列化为json字符�?
     *
     * @param object 要序列化的对�?
     * @return 序列化后的json字符�?
     */
    fun toJson(`object`: Any?): String = toJson(getGson(), `object`)

    /**
     * 将对象序列化为json字符�?
     *
     * @param src 要序列化的对�?
     * @param typeOfSrc 对象的特定泛型类�?
     * @return 序列化后的json字符�?
     */
    fun toJson(src: Any?, typeOfSrc: Type): String = toJson(getGson(), src, typeOfSrc)

    /**
     * 使用指定的Gson实例将对象序列化为json字符�?
     *
     * @param gson Gson实例
     * @param object 要序列化的对�?
     * @return 序列化后的json字符�?
     */
    fun toJson(gson: Gson, `object`: Any?): String = gson.toJson(`object`)

    /**
     * 使用指定的Gson实例将对象序列化为json字符�?
     *
     * @param gson Gson实例
     * @param src 要序列化的对�?
     * @param typeOfSrc 对象的特定泛型类�?
     * @return 序列化后的json字符�?
     */
    fun toJson(gson: Gson, src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)


    /**
     * 将json字符串转换为指定类型的对�?
     *
     * @param json 要转换的json字符�?
     * @param type 目标类型
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(json: String?, type: Class<T>): T? =
        fromJson(getGson(), json, type)

    /**
     * 将json字符串转换为指定类型(Type)的对�?
     *
     * @param json 要转换的json字符�?
     * @param type 目标类型(Type)
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(json: String?, type: Type): T? = fromJson(getGson(), json, type)

    /**
     * 将Reader内容转换为指定类型的对象
     *
     * @param reader 要转换的Reader
     * @param type 目标类型
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(reader: Reader?, type: Class<T>): T? =
        fromJson(getGson(), reader, type)

    /**
     * 将Reader内容转换为指定类�?Type)的对�?
     *
     * @param reader 要转换的Reader
     * @param type 目标类型(Type)
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(reader: Reader?, type: Type): T? =
        fromJson(getGson(), reader, type)

    /**
     * 使用指定的Gson实例将json字符串转换为指定类型的对�?
     *
     * @param gson Gson实例
     * @param json 要转换的json字符�?
     * @param type 目标类型
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(gson: Gson, json: String?, type: Class<T>): T? =
        json?.let { gson.fromJson(it, type) }

    /**
     * 使用指定的Gson实例将json字符串转换为指定类型(Type)的对�?
     *
     * @param gson Gson实例
     * @param json 要转换的json字符�?
     * @param type 目标类型(Type)
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(gson: Gson, json: String?, type: Type): T? =
        json?.let { gson.fromJson(it, type) }

    /**
     * 使用指定的Gson实例将Reader内容转换为指定类型的对象
     *
     * @param gson Gson实例
     * @param reader 要转换的Reader
     * @param type 目标类型
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(gson: Gson, reader: Reader?, type: Class<T>): T? =
        reader?.let { gson.fromJson(it, type) }

    /**
     * 使用指定的Gson实例将Reader内容转换为指定类�?Type)的对�?
     *
     * @param gson Gson实例
     * @param reader 要转换的Reader
     * @param type 目标类型(Type)
     * @return 转换后的对象实例
     */
    inline fun <reified T> fromJson(gson: Gson, reader: Reader?, type: Type): T? =
        reader?.let { gson.fromJson(it, type) }

    /**
     * 获取指定元素类型的List类型
     *
     * @param type 元素类型
     * @return List类型
     */
    fun getListType(type: Type): Type =
        TypeToken.getParameterized(List::class.java, type).type


    /**
     * 获取指定元素类型的Set类型
     *
     * @param type 元素类型
     * @return Set类型
     */
    fun getSetType(type: Type): Type =
        TypeToken.getParameterized(Set::class.java, type).type

    /**
     * 获取指定键值类型的Map类型
     *
     * @param keyType 键类�?
     * @param valueType 值类�?
     * @return Map类型
     */
    fun getMapType(keyType: Type, valueType: Type): Type =
        TypeToken.getParameterized(Map::class.java, keyType, valueType).type

    /**
     * 获取指定类型的数组类�?
     *
     * @param type 元素类型
     * @return 数组类型
     */
    fun getArrayType(type: Type): Type = TypeToken.getArray(type).type

    /**
     * 获取带泛型参数的类型
     *
     * @param rawType 原始类型
     * @param typeArguments 类型参数
     * @return 带泛型参数的类型
     */
    fun getType(rawType: Type, vararg typeArguments: Type): Type =
        TypeToken.getParameterized(rawType, *typeArguments).type

    /**
     * 获取用于日志工具（LogUtils）的Gson实例�?
     * 该函数使用了一个线程安全的懒加载机制，确保在需要时创建并返回Gson实例�?
     * 如果已经存在对应的Gson实例，则直接返回；否则，创建一个新的Gson实例并缓存起来�?
     *
     * @return Gson 返回一个配置了漂亮打印（Pretty Printing）和序列化空值（Serialize Nulls）的Gson实例�?
     */
    internal fun getGson4Log(): Gson =
        GSONS.getOrPut(KEY_LOG_UTILS) {
            GsonBuilder().setPrettyPrinting().serializeNulls().create()
        }

    private fun createGson(): Gson =
        GsonBuilder().serializeNulls().disableHtmlEscaping().create()
}