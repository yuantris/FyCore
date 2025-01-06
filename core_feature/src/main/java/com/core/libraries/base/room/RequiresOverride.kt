package com.core.libraries.base.room

/**
 * 子类必须实现该方法以返回表名
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresOverride
