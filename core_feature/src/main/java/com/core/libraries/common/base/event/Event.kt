package com.core.libraries.common.base.event

sealed class Event {
    data class Created<T>(val data: T) : Event()
}
