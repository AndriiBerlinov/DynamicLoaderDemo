package com.example.dynamicloaderdemo

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.dynamicloaderdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val moduleUrl =
        "https://github.com/AndriiBerlinov/DynamicLoaderDemo/releases/download/v1.0/module.jar"

    private val viewModel: MainViewModel by lazy {
        val repo = ModuleRepository(applicationContext)
        val factory = MainViewModelFactory(repo, moduleUrl)
        ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeButton.setOnClickListener {
            finishAffinity()
        }

        binding.retryButton.setOnClickListener {
            viewModel.start()
        }

        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    UiState.Downloading -> {
                        binding.statusText.text = "Downloading…"
                        binding.progress.visibility = View.VISIBLE
                        binding.resultText.visibility = View.GONE
                        binding.retryButton.visibility = View.GONE
                        binding.closeButton.visibility = View.GONE
                    }

                    is UiState.Result -> {
                        binding.statusText.text = "Completed"
                        binding.progress.visibility = View.GONE
                        binding.resultText.visibility = View.VISIBLE
                        binding.resultText.text = "${state.deviceLine}\n${state.dateLine}"

                        binding.retryButton.visibility = View.GONE
                        binding.closeButton.visibility = View.VISIBLE
                    }

                    is UiState.AlreadyExecuted -> {
                        binding.statusText.text = "Already executed"
                        binding.progress.visibility = View.GONE
                        binding.resultText.visibility = View.VISIBLE
                        binding.resultText.text =
                            listOf(state.deviceLine, state.dateLine)
                                .filter { it.isNotBlank() }
                                .joinToString("\n")

                        binding.retryButton.visibility = View.GONE
                        binding.closeButton.visibility = View.VISIBLE
                    }

                    is UiState.Error -> {
                        binding.statusText.text = "Error"
                        binding.progress.visibility = View.GONE
                        binding.resultText.visibility = View.VISIBLE
                        binding.resultText.text = state.message

                        binding.retryButton.visibility = View.VISIBLE
                        binding.closeButton.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewModel.start()
    }
}
