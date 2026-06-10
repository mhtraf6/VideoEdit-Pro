package com.videoeditpro.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// متغير عالمي للاحتفاظ بالإعلان البيني في الذاكرة
var mInterstitialAd: InterstitialAd? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. تهيئة نظام إعلانات جوجل بمجرد تشغيل التطبيق
        MobileAds.initialize(this) {}
        
        // 2. تحميل الإعلان البيني ليكون جاهزاً فوراً في الخلفية
        loadInterstitialAd(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VideoEditorScreen(activity = this)
            }
        }
    }
}

// دالة برمجية مسؤولة عن تحميل الإعلان باستخدام وحدتك الإعلانية الحقيقية
fun loadInterstitialAd(context: Context) {
    // تم وضع معرف الوحدة الإعلانية البينية الحقيقي الخاص بك هنا بنجاح
    val adUnitId = "ca-app-pub-8995513369904529/9290708833" 
    
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            mInterstitialAd = null
        }

        override fun onAdLoaded(interstitialAd: InterstitialAd) {
            mInterstitialAd = interstitialAd
        }
    })
}

// دالة برمجية لعرض الإعلان للمستخدم ثم إعادة تحميل إعلان آخر للداخل
fun showAdAndExecuteAction(context: Context, activity: ComponentActivity, onAdClosedAction: () -> Unit) {
    if (mInterstitialAd != null) {
        mInterstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // عند إغلاق الإعلان، نفذ ميزة الأداة وحمّل إعلان جديد للمرة القادمة
                onAdClosedAction()
                loadInterstitialAd(context)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                // إذا فشل العرض، توجه للميزة مباشرة بدون تعطيل المستخدم
                onAdClosedAction()
            }
        }
        mInterstitialAd?.show(activity)
    } else {
        // إذا لم يكن الإعلان جاهزاً بعد، افتح الأداة مباشرة وحاول التحميل مجدداً
        onAdClosedAction()
        loadInterstitialAd(context)
    }
}

data class EditorTool(val id: Int, val titleAr: String, val titleEn: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(activity: ComponentActivity) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var timelinePosition by remember { mutableStateOf(0.5f) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val toolsList = remember {
        listOf(
            EditorTool(1, "الفلاتر والمؤثرات", "Filters & Effects", Icons.Default.AutoAwesome),
            EditorTool(2, "الترجمة التلقائية", "Auto Captions", Icons.Default.ClosedCaption),
            EditorTool(3, "تسجيل صوت", "Voice Recorder", Icons.Default.Mic),
            EditorTool(4, "إضافة موسيقى", "Add Audio", Icons.Default.MusicNote),
            EditorTool(5, "المسودات", "Drafts Manager", Icons.Default.FolderSpecial),
            EditorTool(6, "قص وتقصير", "Trim & Cut", Icons.Default.ContentCut),
            EditorTool(7, "دمج المقاطع والصور", "Media Fusion", Icons.Default.Merge),
            EditorTool(8, "التحكم بالسرعة", "Speed Control", Icons.Default.Speed),
            EditorTool(9, "موزايك وفسيفساء", "Mosaic Blur", Icons.Default.BlurOn),
            EditorTool(10, "حجم الصوت", "Volume Mixer", Icons.Default.VolumeUp),
            EditorTool(11, "تدوير وقلب", "Transform", Icons.Default.FlipCameraAndroid),
            EditorTool(12, "تكبير وتصغير", "Canvas Zoom", Icons.Default.ZoomIn),
            EditorTool(13, "إضافة نصوص خطوط عربية", "Text Overlay", Icons.Default.TextFields),
            EditorTool(14, "الكروما", "Chroma Key", Icons.Default.Layers),
            EditorTool(15, "ملصقات وإيموجي", "Stickers & Emoji", Icons.Default.Mood),
            EditorTool(16, "تأثيرات الانتقال", "Transitions", Icons.Default.Transform),
            EditorTool(17, "أبعاد الفيديو", "Aspect Ratio", Icons.Default.AspectRatio),
            EditorTool(18, "عكس الفيديو", "Reverse Video", Icons.Default.SettingsBackupRestore),
            EditorTool(19, "تعديل الألوان", "Color Adjust", Icons.Default.Tune),
            EditorTool(20, "فصل الصوت", "Audio Extractor", Icons.Default.Audiotrack)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8E24AA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Column {
                            Text("VideoEdit Pro", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("لا يوجد مشروع", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle Share */ }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    IconButton(onClick = { /* Handle Settings */ }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Video Player Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFullScreen) 1f else 0.45f)
                    .padding(8.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Preview", size = 48.dp, tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("استورد مقطع فيديو", color = Color.White, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { }, modifier = Modifier.background(Color(0x66FFFFFF), CircleShape)) { Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White) }
                        IconButton(onClick = { }, modifier = Modifier.background(Color(0x66FFFFFF), CircleShape)) { Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.White) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { isFullScreen = !isFullScreen }, modifier = Modifier.background(Color(0x66FFFFFF), CircleShape)) { Icon(if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Aspect", tint = Color.White) }
                        IconButton(onClick = { isMuted = !isMuted }, modifier = Modifier.background(Color(0x66FFFFFF), CircleShape)) { Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = "Mute", tint = Color.White) }
                    }
                }
            }

            // Timeline
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الجدول الزمني", fontSize = 12.sp, color = Color.LightGray)
                        Text("0:00 / 0:00", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Slider(value = timelinePosition, onValueChange = { timelinePosition = it }, modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد فيديو")
            }

            Spacer(modifier = Modifier.weight(0.05f))

            // 20 Tools Menu with AdMob Trigger integrated
            Text(text = "أدوات التعديل الاحترافية (اسحب يميناً ويساراً) ↔️", color = Color.Gray, fontSize = 11.sp)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF121212))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                toolsList.forEach { tool ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(85.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                // تشغيل الإعلان البيني الحقيقي الخاص بك قبل فتح أي أداة!
                                showAdAndExecuteAction(context, activity) {
                                    Toast.makeText(context, "تم فتح أداة: ${tool.titleAr}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(45.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(12.dp))
                        ) {
                            Icon(tool.icon, contentDescription = tool.titleEn, tint = Color(0xFFBA68C8))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = tool.titleAr, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            // Green Export Button
            Button(
                onClick = {
                    exportProgress = 0
                    showExportDialog = true
                    scope.launch {
                        while (exportProgress < 100) {
                            delay(40)
                            exportProgress += 1
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("💾 تصدير وحفظ الفيديو النهائي (MP4)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Progress Dialog Overlay
    if (showExportDialog) {
        Dialog(onDismissRequest = { if (exportProgress >= 100) showExportDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (exportProgress < 100) "جاري معالجة وتصدير الفيديو..." else "تم حفظ الفيديو بنجاح! 🎉", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = exportProgress / 100f, modifier = Modifier.size(100.dp), color = Color(0xFF00E676), trackColor = Color.Gray)
                        Text(text = "$exportProgress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (exportProgress >= 100) {
                        Button(onClick = { showExportDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))) { Text("إغلاق") }
                    }
                }
            }
        }
    }
}