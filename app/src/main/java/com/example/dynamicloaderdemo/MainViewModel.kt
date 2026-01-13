package com.example.dynamicloaderdemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repository: ModuleRepository,
    private val moduleUrl: String
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Downloading)
    val state: StateFlow<UiState> = _state

    fun start() {
        if (repository.isAlreadyExecuted()) {
            val last = repository.getLastResultOrNull()
            _state.value = UiState.AlreadyExecuted(last ?: "Module already executed")
            return
        }

        _state.value = UiState.Downloading

        viewModelScope.launch {
            try {
                val jarFile = withContext(Dispatchers.IO) {
                    repository.downloadModuleJar(moduleUrl)
                }

                val result = withContext(Dispatchers.IO) {
                    repository.runModuleFromJar(jarFile)
                }

                _state.value = UiState.Result(result)

                withContext(Dispatchers.IO) {
                    repository.deleteJar(jarFile)
                }

                repository.markExecuted(result)

            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
