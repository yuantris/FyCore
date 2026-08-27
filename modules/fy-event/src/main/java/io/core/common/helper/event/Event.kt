package io.core.common.helper.event

sealed class Event {
    data class Created<T>(val data: T) : Event()
}
