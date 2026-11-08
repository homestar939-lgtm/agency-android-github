package com.agency.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.agency.app.core.ui.navigation.AgencyRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * النشاط الوحيد في التطبيق.
 *
 * يرث [FragmentActivity] لا `ComponentActivity` لأن `BiometricPrompt`
 * يتطلب سياق `FragmentActivity`؛ بدون ذلك لا يمكن طلب البصمة للسندات
 * الكبيرة، وهي قاعدة مالية لا نقبل تعطيلها.
 *
 * كل شيء في Compose، والتنقل يُدار عبر [AgencyRoot]، والاعتماديات تُحقن
 * بواسطة Hilt من [AgencyApp] — لا يوجد أي `Repository(...)` يدوي هنا.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgencyRoot() }
    }
}