package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.RoomScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PaintViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: PaintViewModel = viewModel()
        val activeRoom by viewModel.activeRoom.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Modifier.padding(innerPadding) // consume safe drawing padding
          if (activeRoom != null) {
            RoomScreen(viewModel = viewModel)
          } else {
            MainScreen(viewModel = viewModel)
          }
        }
      }
    }
  }
}
