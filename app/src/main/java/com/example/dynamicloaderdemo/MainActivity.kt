package com.example.dynamicloaderdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.dynamicloaderdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val moduleUrl = "https://test/module.jar"

    private val viewModel: MainViewModel by lazy {
        val repo = ModuleRepository(applicationContext)
        val factory = MainViewModelFactory(repo, moduleUrl)
        ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.statusText.text = when (state) {
                    UiState.Downloading -> "Downloading…"
                    is UiState.Result -> state.text
                    is UiState.AlreadyExecuted -> state.text
                    is UiState.Error -> "Error: ${state.message}"
                }
            }
        }

        viewModel.start()
    }
}
