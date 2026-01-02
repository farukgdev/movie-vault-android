package com.farukg.movievault.core.ui.scaffold

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*

@Stable
class AppScaffoldController {
    private val topBars = mutableStateMapOf<ScreenKey, @Composable () -> Unit>()

    var activeScreen: ScreenKey? by mutableStateOf(null)
        private set

    fun updateActiveScreen(screen: ScreenKey?) {
        activeScreen = screen
    }

    fun setTopBar(screen: ScreenKey, content: @Composable () -> Unit) {
        topBars[screen] = content
    }

    fun clearTopBar(screen: ScreenKey) {
        topBars.remove(screen)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RenderTopBar() {
        val current = activeScreen?.let { topBars[it] }

        if (current != null) {
            current()
        } else {
            // reserve height so content doesn't jump
            TopAppBar(title = {})
        }
    }
}

val LocalAppScaffold =
    staticCompositionLocalOf<AppScaffoldController> { error("AppScaffoldController not provided") }

@Composable
fun RegisterTopBar(screen: ScreenKey, content: @Composable () -> Unit) {
    val scaffold = LocalAppScaffold.current
    val latest by rememberUpdatedState(content)

    SideEffect { scaffold.setTopBar(screen) { latest() } }
    DisposableEffect(screen) { onDispose { scaffold.clearTopBar(screen) } }
}
