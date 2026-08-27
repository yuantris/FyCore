package io.core.engine.storage

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class StorageKey(
    val defaultValue: String = "",
    val description: String = ""
)