package com.agency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.agency.app.core.ui.navigation.AppNavGraph
import com.agency.app.core.ui.theme.AgencyAppTheme
import com.agency.app.core.ui.theme.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ملاحظة: سنقوم لاحقاً بربط هذا بالدور الحقيقي للمستخدم عبر AuthManager
        // حالياً نضبطه على WAREHOUSE للاختبار
        val currentRole = UserRole.WAREHOUSE 

        setContent {
            AgencyAppTheme(role = currentRole) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // هنا يتم ربط نقطة الدخول بشبكة التنقل الحقيقية
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
