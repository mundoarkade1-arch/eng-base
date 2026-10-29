package com.engbase.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EngBaseTheme {
                EngBaseApp()
            }
        }
    }
}

@Composable
fun EngBaseApp(vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(
        "Hoje" to Icons.Filled.Home,
        "Matérias" to Icons.AutoMirrored.Filled.MenuBook,
        "Tutor" to Icons.Filled.SmartToy,
        "Caderno" to Icons.Filled.Calculate,
        "Foco" to Icons.Filled.Timer
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> TodayScreen(vm)
                1 -> SubjectsScreen(vm)
                2 -> TutorScreen(vm)
                3 -> NotebookScreen()
                else -> FocusScreen(vm)
            }
        }
    }
}
