package ar.matias.listmaster.ui

import androidx.compose.runtime.mutableStateListOf

sealed interface Screen {
    data object Projects : Screen
    data object Pending : Screen
    data object Lists : Screen
    data object Settings : Screen

    data class ProjectDetail(val projectId: String) : Screen
    data class ProjectForm(val parentId: String?, val projectId: String? = null) : Screen
    data class ListDetail(val listId: String) : Screen
    data class ListForm(val listId: String? = null) : Screen
}

/**
 * Pila de navegación mínima, sin librería: la app tiene cinco destinos y una
 * profundidad de dos o tres pantallas. Vive en el ViewModel para sobrevivir a
 * la rotación.
 */
class Navigator {

    val stack = mutableStateListOf<Screen>(Screen.Pending)

    val current: Screen get() = stack.last()

    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToRoot() {
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }

    fun switchTab(screen: Screen) {
        if (stack.size == 1 && stack.single() == screen) return
        stack.clear()
        stack.add(screen)
    }

    fun replaceCurrent(screen: Screen) {
        stack[stack.lastIndex] = screen
    }
}
