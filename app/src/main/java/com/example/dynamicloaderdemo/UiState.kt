package com.example.dynamicloaderdemo

sealed class UiState {
    data object Downloading : UiState()

    data class Result(val deviceLine: String, val dateLine: String) : UiState()

    data class AlreadyExecuted(val deviceLine: String, val dateLine: String) : UiState()

    data class Error(val message: String) : UiState()
}
