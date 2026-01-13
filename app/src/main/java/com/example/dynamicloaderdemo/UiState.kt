package com.example.dynamicloaderdemo

sealed class UiState {
    data object Downloading : UiState()
    data class Result(val text: String) : UiState()
    data class AlreadyExecuted(val text: String) : UiState()
    data class Error(val message: String) : UiState()
}
