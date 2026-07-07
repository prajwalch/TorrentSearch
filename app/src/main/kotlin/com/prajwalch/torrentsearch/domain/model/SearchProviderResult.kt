package com.prajwalch.torrentsearch.domain.model

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface SearchProviderResult<out T> {
    data class Error(val error: SearchProviderError) : SearchProviderResult<Nothing>

    data class Success<T>(val value: T) : SearchProviderResult<T>

    fun getOrNull(): T? = when (this) {
        is Error -> null
        is Success<T> -> this.value
    }
}

@OptIn(ExperimentalContracts::class)
fun <T, R> SearchProviderResult<T>.fold(
    onSuccess: (value: T) -> R,
    onError: (error: SearchProviderError) -> R,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is SearchProviderResult.Error -> onError(this.error)
        is SearchProviderResult.Success<T> -> onSuccess(this.value)
    }
}