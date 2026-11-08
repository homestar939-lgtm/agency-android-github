package com.agency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.agency.app.core.ui.navigation.AgencyRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * النشاط الوحيد في التطبيق.
 *
 * كل شيء في Compose، والتنقل يُدار عبر [AgencyRoot]، والاعتماديات تُحقن
 * بواسطة Hilt من [AgencyApp] — لا يوجد أي `Repository(...)` يدوي هنا.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgencyRoot() }
    }
}