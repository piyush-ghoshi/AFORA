package com.afora.shared.util

/**
 * Result wrapper for success/error handling.
 * Cleaner than exceptions for expected failures (network, validation, etc.).
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
    }
    
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
}

/**
 * Application error types.
 */
sealed class AppError(open val message: String) {
    data class NetworkError(override val message: String = "Network error") : AppError(message)
    data class ApiError(val code: Int, override val message: String) : AppError(message)
    data class ValidationError(val errors: List<String>) : AppError(errors.joinToString(", "))
    data class UnauthorizedError(override val message: String = "Unauthorized") : AppError(message)
    data class AuthenticationError(override val message: String) : AppError(message)
    data class NotFoundError(override val message: String = "Resource not found") : AppError(message)
    data class UnknownError(override val message: String = "Unknown error") : AppError(message)
}
