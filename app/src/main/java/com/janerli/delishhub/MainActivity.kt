package com.janerli.delishhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.janerli.delishhub.core.navigation.AppNavGraph
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.theme.DelishHubTheme
import com.janerli.delishhub.data.sync.SyncManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Поднимаем сессию (автологин Firebase учитывается)
        SessionManager.init()

        // 2) Если не гость — запускаем синки (one-time + periodic)
        //    Это ключевой фикс для "на другом устройстве не подтянулось".
        if (!SessionManager.session.value.isGuest) {
            SyncManager.start(applicationContext)
        }

        setContent {
            DelishHubTheme {
                AppNavGraph()
            }
        }
    }
}
