package ar.matias.listmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ar.matias.listmaster.ui.ListMasterApp
import ar.matias.listmaster.ui.theme.ListMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListMasterTheme {
                ListMasterApp()
            }
        }
    }
}
