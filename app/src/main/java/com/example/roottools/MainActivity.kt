package com.example.roottools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class MainTab(val title: String, val icon: String) {
    TOOLS("الأدوات", "⚙"),
    FILES("التخزين والملفات", "▤"),
    CUSTOMIZATION("الواجهة والتخصيص", "✦")
}

private data class ToolItem(val number: Int, val title: String, val description: String)

private val toolTabs = mapOf(
    MainTab.TOOLS to listOf(
        ToolItem(1, "معلومات الجهاز وفاحص الروت", "معلومات الجهاز ومدير الروت وحالة الأقسام"),
        ToolItem(2, "خيارات إعادة التشغيل السريعة", "خيارات الإقلاع وإعادة تشغيل الجهاز"),
        ToolItem(3, "مدير الفلاش والنسخ الاحتياطي", "إدارة النسخ الاحتياطية للأقسام"),
        ToolItem(4, "معايرة البطارية", "عرض معلومات البطارية وإحصاءاتها"),
        ToolItem(5, "محرر خصائص النظام", "عرض وإدارة خصائص النظام"),
        ToolItem(6, "إعدادات المنطقة والشبكة", "إعدادات المنطقة والشبكة بحسب الجهاز")
    ),
    MainTab.FILES to listOf(
        ToolItem(7, "متصفح ملفات الروت", "إدارة الملفات وعرض الصلاحيات"),
        ToolItem(8, "تركيب أقسام النظام", "عرض حالة أقسام النظام وإدارتها"),
        ToolItem(9, "مدير التطبيقات", "عرض تطبيقات النظام والمستخدم")
    ),
    MainTab.CUSTOMIZATION to listOf(
        ToolItem(10, "كثافة وأبعاد الشاشة", "إدارة كثافة العرض وأبعاده"),
        ToolItem(11, "مثبت الخطوط", "إدارة خطوط النظام"),
        ToolItem(12, "مغير الإيموجي", "إدارة حزمة الإيموجي"),
        ToolItem(13, "لغة النظام", "إعداد لغة النظام"),
        ToolItem(14, "النوافذ العائمة", "إعداد دعم النوافذ الحرة"),
        ToolItem(15, "إيماءات الأزرار والبصمة", "إعداد الاخ��صارات والإيماءات")
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RootToolsApp()
                }
            }
        }
    }
}

@Composable
private fun RootToolsApp() {
    var selectedTab by remember { mutableStateOf(MainTab.TOOLS) }
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var report by remember { mutableStateOf<DeviceReport?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Text(
                text = "أساسيات الروت",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)
            )
            Text(
                text = "أدوات النظام والتخصيص",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("فحص معلومات الجهاز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        report?.let { "الجهاز: ${it.manufacturer} ${it.model} • أندرويد ${it.androidVersion} • الروت: ${if (it.hasRoot) "متاح" else "غير متاح"} • المدير: ${it.rootManager.arabicName()}" }
                            ?: "يمكنك إجراء فحص معلوماتي لا يغيّر ملفات النظام.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        enabled = !isChecking,
                        onClick = {
                            scope.launch {
                                isChecking = true
                                report = runCatching { RootInspector.inspect() }.getOrNull()
                                isChecking = false
                            }
                        }
                    ) {
                        if (isChecking) CircularProgressIndicator()
                        else Text(if (report == null) "فحص الجهاز" else "إعادة الفحص")
                    }
                    report?.partitions?.forEach { (partition, access) ->
                        Text("${partition}: ${access.arabicName()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(toolTabs[selectedTab].orEmpty()) { tool ->
                    ToolCard(tool = tool, onClick = { selectedTool = tool })
                }
            }
        }
    }

    selectedTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { selectedTool = null },
            title = { Text(tool.title) },
            text = { Text("هذه بطاقة تعريفية في المرحلة الأولى. تنفيذ وظائف الأداة سيضاف لاحقاً، ولن تُجرى تغييرات على جهازك الآن.") },
            confirmButton = {
                TextButton(onClick = { selectedTool = null }) { Text("حسناً") }
            }
        )
    }
}

@Composable
private fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                Text(
                    text = tool.number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun RootManager.arabicName(): String = when (this) {
    RootManager.MAGISK -> "ماجيسك"
    RootManager.KERNELSU -> "كيرنل سو"
    RootManager.APATCH -> "أباتش"
    RootManager.UNKNOWN -> "غير معروف"
}

private fun MountAccess.arabicName(): String = when (this) {
    MountAccess.READ_ONLY -> "قراءة فقط"
    MountAccess.READ_WRITE -> "قراءة وكتابة"
    MountAccess.UNKNOWN -> "غير معروف"
}
