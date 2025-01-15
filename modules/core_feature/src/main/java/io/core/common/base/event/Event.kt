package io.core.common.base.event

sealed class Event {
    data class Created<T>(val data: T) : Event()
}
