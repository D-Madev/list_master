package ar.matias.listmaster.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.matias.listmaster.ui.theme.ListMasterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arrancaEnProyectosYPermiteCambiarDeSeccion() {
        composeRule.setContent {
            ListMasterTheme(darkTheme = false) {
                ListMasterApp()
            }
        }

        composeRule.onNodeWithText("Mis Proyectos").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no tenés proyectos.").assertIsDisplayed()

        composeRule.onNodeWithText("Listas").performClick()
        composeRule.onNodeWithText("Mis Listas").assertIsDisplayed()

        composeRule.onNodeWithText("Ajustes").performClick()
        composeRule.onNodeWithText("Tus preferencias, en un solo lugar.").assertIsDisplayed()
    }
}
