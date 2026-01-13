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
            _state.value = if (last != null) {
                UiState.AlreadyExecuted(last.first, last.second)
            } else {
                UiState.AlreadyExecuted("Already executed", "")
            }
            return
        }

        _state.value = UiState.Downloading

        viewModelScope.launch {
            try {
                val jarBytes = withContext(Dispatchers.IO) {
                    repository.downloadModuleJarBytes(moduleUrl)
                }

                val rawResult = withContext(Dispatchers.IO) {
                    repository.runModuleFromJarBytes(jarBytes)
                }

                val (deviceLine, dateLine) = splitToTwoLines(rawResult)

                _state.value = UiState.Result(deviceLine, dateLine)

                repository.markExecuted(deviceLine, dateLine)

            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun splitToTwoLines(raw: String): Pair<String, String> {
        val idx = raw.lastIndexOf(" - ")
        return if (idx > 0 && idx + 3 < raw.length) {
            val device = raw.substring(0, idx).trim()
            val date = raw.substring(idx + 3).trim()
            device to date
        } else {
            raw to ""
        }
    }
}
