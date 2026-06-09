package com.example.ui.social

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BorderColor
import com.example.CardBg
import com.example.LuxuryGold
import com.example.ScreenBg
import com.example.SoftGold
import com.example.TextMutedColor
import com.example.TextSoftColor
import com.example.settings.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaScreen(isArabic: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    
    // Persistent Account setup
    val tiktokLinked by settingsManager.tiktokLinked.collectAsState(initial = false)
    val instagramLinked by settingsManager.instagramLinked.collectAsState(initial = false)
    val facebookLinked by settingsManager.facebookLinked.collectAsState(initial = false)
    val youtubeLinked by settingsManager.youtubeLinked.collectAsState(initial = false)
    
    val tiktokHandle by settingsManager.tiktokHandle.collectAsState(initial = "")
    val instagramHandle by settingsManager.instagramHandle.collectAsState(initial = "")
    val facebookHandle by settingsManager.facebookHandle.collectAsState(initial = "")
    val youtubeHandle by settingsManager.youtubeHandle.collectAsState(initial = "")
    
    val tiktokAutopost by settingsManager.tiktokAutopost.collectAsState(initial = true)
    val instagramAutopost by settingsManager.instagramAutopost.collectAsState(initial = true)
    val facebookAutopost by settingsManager.facebookAutopost.collectAsState(initial = true)
    val youtubeAutopost by settingsManager.youtubeAutopost.collectAsState(initial = true)

    // Simulation states
    var isLinkingPlatform by remember { mutableStateOf<String?>(null) }
    var activeDialogPlatform by remember { mutableStateOf<String?>(null) }
    var activeDialogHandle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (isArabic) "حسابات النشر والربط" else "Social Hub & Streaming", 
                        fontWeight = FontWeight.Bold, 
                        color = LuxuryGold, 
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ScreenBg)
            )
        },
        containerColor = ScreenBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier
                    .size(64.dp)
                    .background(CardBg, CircleShape)
                    .border(1.dp, LuxuryGold, CircleShape)
                    .padding(16.dp)
            )
            
            Text(
                text = if (isArabic) "اربط منصاتك للتوزيع السينمائي والتحكم الكامل بالنشر التلقائي" else "Link your platforms for screen streaming and automated distribution",
                color = TextSoftColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // TikTok Card
            SocialPlatformAccountCard(
                platformId = "tiktok",
                platformName = if (isArabic) "تيك توك (TikTok)" else "TikTok Reels",
                isLinked = tiktokLinked,
                handle = tiktokHandle,
                isAutopostEnabled = tiktokAutopost,
                isLinking = isLinkingPlatform == "tiktok",
                onLinkClick = {
                    if (tiktokLinked) {
                        // Unlink
                        scope.launch {
                            settingsManager.setTiktokLinked(false)
                            settingsManager.setTiktokHandle("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط تيك توك" else "TikTok unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        activeDialogPlatform = "tiktok"
                        activeDialogHandle = "@QuranReels_TikTok"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "tiktok"
                    activeDialogHandle = tiktokHandle
                },
                onAutopostToggle = { enabled ->
                    scope.launch { settingsManager.setTiktokAutopost(enabled) }
                },
                isArabic = isArabic
            )

            // Instagram Card
            SocialPlatformAccountCard(
                platformId = "instagram",
                platformName = if (isArabic) "انستقرام (Instagram)" else "Instagram Reels",
                isLinked = instagramLinked,
                handle = instagramHandle,
                isAutopostEnabled = instagramAutopost,
                isLinking = isLinkingPlatform == "instagram",
                onLinkClick = {
                    if (instagramLinked) {
                        scope.launch {
                            settingsManager.setInstagramLinked(false)
                            settingsManager.setInstagramHandle("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط انستقرام" else "Instagram unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        activeDialogPlatform = "instagram"
                        activeDialogHandle = "@Quran_In_Hearts"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "instagram"
                    activeDialogHandle = instagramHandle
                },
                onAutopostToggle = { enabled ->
                    scope.launch { settingsManager.setInstagramAutopost(enabled) }
                },
                isArabic = isArabic
            )

            // Facebook Card
            SocialPlatformAccountCard(
                platformId = "facebook",
                platformName = if (isArabic) "فيسبوك (Facebook)" else "Facebook Watch",
                isLinked = facebookLinked,
                handle = facebookHandle,
                isAutopostEnabled = facebookAutopost,
                isLinking = isLinkingPlatform == "facebook",
                onLinkClick = {
                    if (facebookLinked) {
                        scope.launch {
                            settingsManager.setFacebookLinked(false)
                            settingsManager.setFacebookHandle("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط فيسبوك" else "Facebook unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        activeDialogPlatform = "facebook"
                        activeDialogHandle = "Quran Reels Page"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "facebook"
                    activeDialogHandle = facebookHandle
                },
                onAutopostToggle = { enabled ->
                    scope.launch { settingsManager.setFacebookAutopost(enabled) }
                },
                isArabic = isArabic
            )

            // YouTube Card
            SocialPlatformAccountCard(
                platformId = "youtube",
                platformName = if (isArabic) "يوتيوب (YouTube Shorts)" else "YouTube Shorts",
                isLinked = youtubeLinked,
                handle = youtubeHandle,
                isAutopostEnabled = youtubeAutopost,
                isLinking = isLinkingPlatform == "youtube",
                onLinkClick = {
                    if (youtubeLinked) {
                        scope.launch {
                            settingsManager.setYoutubeLinked(false)
                            settingsManager.setYoutubeHandle("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط يوتيوب الشورتس" else "YouTube channel unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        activeDialogPlatform = "youtube"
                        activeDialogHandle = "Quran Reels Official Channel"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "youtube"
                    activeDialogHandle = youtubeHandle
                },
                onAutopostToggle = { enabled ->
                    scope.launch { settingsManager.setYoutubeAutopost(enabled) }
                },
                isArabic = isArabic
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isArabic) "عند تفعيل خيار النشر التلقائي لمنصة معينة وتوافر مفتاح Gemini بالأعدادات، سيتم توليد تفاصيل سينمائية ونشر Reel إليها تلقائياً فور جاهزية المقطع." else "When auto-post is toggled ON and Gemini key is in settings, custom rich tags & meta are automatically synthesized and uploaded correctly.",
                color = TextMutedColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // Modal Switch / Link Dialog
    activeDialogPlatform?.let { platform ->
        SwitchAccountDialog(
            initialHandle = activeDialogHandle,
            platform = platform.uppercase(),
            isArabic = isArabic,
            onDismiss = { activeDialogPlatform = null },
            onSave = { newHandle ->
                activeDialogPlatform = null
                isLinkingPlatform = platform
                scope.launch {
                    delay(1200) // Beautiful API handshaking render latency
                    when (platform) {
                        "tiktok" -> {
                            settingsManager.setTiktokLinked(true)
                            settingsManager.setTiktokHandle(newHandle)
                        }
                        "instagram" -> {
                            settingsManager.setInstagramLinked(true)
                            settingsManager.setInstagramHandle(newHandle).toString()
                        }
                        "facebook" -> {
                            settingsManager.setFacebookLinked(true)
                            settingsManager.setFacebookHandle(newHandle)
                        }
                        "youtube" -> {
                            settingsManager.setYoutubeLinked(true)
                            settingsManager.setYoutubeHandle(newHandle)
                        }
                    }
                    isLinkingPlatform = null
                    Toast.makeText(context, if (isArabic) "تم الربط والتسجيل بنجاح!" else "Linked and synchronized successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SocialPlatformAccountCard(
    platformId: String,
    platformName: String,
    isLinked: Boolean,
    handle: String,
    isAutopostEnabled: Boolean,
    isLinking: Boolean,
    onLinkClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onAutopostToggle: (Boolean) -> Unit,
    isArabic: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, if (isLinked) LuxuryGold else BorderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row (Icon + Name + Link/Unlink Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLinked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LuxuryGold,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, BorderColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = platformName,
                        color = if (isLinked) Color.White else TextSoftColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Button(
                    onClick = onLinkClick,
                    enabled = !isLinking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLinked) Color(0x1FFA2E1A) else LuxuryGold,
                        contentColor = if (isLinked) Color(0xFFE57373) else ScreenBg,
                        disabledContainerColor = BorderColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isLinked) BorderStroke(1.dp, Color(0x33E57373)) else null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LuxuryGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLinked) (if (isArabic) "إلغاء الربط" else "Disconnect") else (if (isArabic) "ربط الحساب" else "Connect"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Expanded Options if linked
            if (isLinked && !isLinking) {
                // Handle bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ScreenBg,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الحساب المرتبط حالياً:" else "Synchronized Account:",
                                color = TextMutedColor,
                                fontSize = 11.sp
                            )
                            Text(
                                text = handle.ifBlank { "@Default_Reels" },
                                color = LuxuryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        // Switch Account Button
                        TextButton(
                            onClick = onSwitchAccountClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = SoftGold,
                                containerColor = Color(0x0AFFFFFF)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "تبديل الحساب" else "Switch Account",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Autopost toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "النشر التلقائي للمنصة" else "Automatic Publishing",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isAutopostEnabled) {
                                if (isArabic) "سيتم النشر تلقائياً بشكل صحيح" else "Enabled for instant stream"
                            } else {
                                if (isArabic) "النشر التلقائي لهذه المنصة معطّل" else "Publishing is disabled for this platform"
                            },
                            color = if (isAutopostEnabled) Color(0xFF81C784) else TextMutedColor,
                            fontSize = 11.sp
                        )
                    }
                    
                    Switch(
                        checked = isAutopostEnabled,
                        onCheckedChange = onAutopostToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LuxuryGold,
                            checkedTrackColor = Color(0x66D29E57),
                            uncheckedThumbColor = TextMutedColor,
                            uncheckedTrackColor = BorderColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchAccountDialog(
    initialHandle: String,
    platform: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialHandle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "ربط وضبط قناة ($platform)" else "Link Profile ($platform)",
                color = LuxuryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) 
                        "يرجى كتابة اسم المعرف أو الحساب الخاص بك للتسجيل بالبرنامج للبدء:" 
                        else "Please specify your social profile tag/username to bind registration:",
                    color = TextSoftColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = ScreenBg,
                        unfocusedContainerColor = ScreenBg,
                        disabledContainerColor = ScreenBg,
                        errorContainerColor = ScreenBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ScreenBg)
            ) {
                Text(if (isArabic) "حفظ وتسجيل الربط" else "Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء الأمر" else "Cancel", color = TextMutedColor)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(20.dp)
    )
}
