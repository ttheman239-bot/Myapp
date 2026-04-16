package com.example.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapp.data.PairResult
import com.example.myapp.data.ResearchData
import com.example.myapp.data.ResearchRepository
import com.example.myapp.ui.AboutScreen
import com.example.myapp.ui.DetailScreen
import com.example.myapp.ui.HomeScreen
import com.example.myapp.ui.theme.MyAppTheme

private sealed interface Screen {
    data object Home : Screen
    data class Detail(val pair: PairResult) : Screen
    data object About : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val data: ResearchData? = remember {
        runCatching { ResearchRepository.load(context) }.getOrNull()
    }

    if (data == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "โหลด research.json ไม่สำเร็จ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    var screen: Screen by remember { mutableStateOf(Screen.Home) }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = Screen.Home
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            data = data,
            onPairClick  = { screen = Screen.Detail(it) },
            onAboutClick = { screen = Screen.About },
        )
        is Screen.Detail -> DetailScreen(
            pair = s.pair,
            onBack = { screen = Screen.Home },
        )
        Screen.About -> AboutScreen(
            onBack = { screen = Screen.Home },
        )
    }
}
