package com.agency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Minimal Android launch target for the preserved reference package.
 * The original MainActivity and all feature snippets are kept verbatim in
 * reference-code/src/main/java/com/agency/app and REFERENCE_README.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReferencePackageScreen()
                }
            }
        }
    }
}

@Composable
private fun ReferencePackageScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Agency Android",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "تم تجهيز حزمة Android للبناء عبر GitHub Actions.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الملف المرجعي الأصلي محفوظ كاملاً في REFERENCE_README.md، كما تم فصل جميع المقاطع البرمجية في reference-code دون تعديل محتواها."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "هذه شاشة تشغيل تمهيدية فقط؛ لم يتم اختراع تنفيذ للواجهات أو الخدمات الناقصة في المرجع. راجع REFERENCE_GAPS.md قبل دمج الشيفرات في التطبيق الفعلي."
                )
            }
        }
    }
}
