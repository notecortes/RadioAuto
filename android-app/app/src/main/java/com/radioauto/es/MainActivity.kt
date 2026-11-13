package com.radioauto.es

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radioauto.es.ui.screens.HomeScreen
import com.radioauto.es.ui.theme.RadioAutoTheme
import com.radioauto.es.ui.viewmodel.RadioViewModel

/**
 * Actividad principal de RadioAuto ES que aloja la interfaz de usuario en Jetpack Compose
 */
class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    /**
     * Inicializa la actividad y la composición de la interfaz gráfica
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val useDark = when (uiState.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            RadioAutoTheme(darkTheme = useDark) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
