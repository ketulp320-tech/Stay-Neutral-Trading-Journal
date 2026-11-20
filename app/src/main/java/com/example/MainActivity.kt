package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StayNeutralDatabase
import com.example.data.TradeRepository
import com.example.ui.StayNeutralApp
import com.example.ui.StayNeutralViewModel
import com.example.ui.theme.StayNeutralTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val database = StayNeutralDatabase.getInstance(applicationContext)
        val repository = TradeRepository(database.tradeDao())

        setContent {
            StayNeutralTheme {
                val vm: StayNeutralViewModel = viewModel(
                    factory = StayNeutralViewModel.Factory(repository),
                )
                StayNeutralApp(viewModel = vm)
            }
        }
    }
}
