package com.realityexpander.coroutineplayground

import MainViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewModelScope
import com.realityexpander.coroutineplayground.ui.theme.CoroutinePlaygroundTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var viewModel = MainViewModel()

        setContent {
            CoroutinePlaygroundTheme {

                LaunchedEffect(key1 = true) {
                    delay(150)


                    println("------------------------- CANCEL & INIT NEW VIEWMODEL")
                    viewModel.viewModelScope.cancel()
                    viewModel = MainViewModel()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state by viewModel.state.collectAsState()

                    ViewState(
                        when(state) {
                            is ViewState.Success -> (state as ViewState.Success<String>).data
                            is ViewState.Error -> "ERROR: " + (state as ViewState.Error<String>).error.localizedMessage
                            is ViewState.Loading -> (state as ViewState.Loading<String>).msg
                            else -> "Unknown"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ViewState(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "ViewState: $name")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoroutinePlaygroundTheme {
        ViewState("Android")
    }
}