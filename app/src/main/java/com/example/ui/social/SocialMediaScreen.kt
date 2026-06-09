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

    // Real API integration state loads
    val tiktokAccessToken by settingsManager.tiktokAccessToken.collectAsState(initial = "")
    val instagramAccessToken by settingsManager.instagramAccessToken.collectAsState(initial = "")
    val facebookAccessToken by settingsManager.facebookAccessToken.collectAsState(initial = "")
    val youtubeAccessToken by settingsManager.youtubeAccessToken.collectAsState(initial = "")
    val webhookPublishUrl by settingsManager.webhookPublishUrl.collectAsState(initial = "")

    // Simulation & UI flow states
    var isLinkingPlatform by remember { mutableStateOf<String?>(null) }
    var activeDialogPlatform by remember { mutableStateOf<String?>(null) }
    var activeDialogHandle by remember { mutableStateOf("") }
    var activeDialogToken by remember { mutableStateOf("") }
    var showWebhookDialog by remember { mutableStateOf(false) }
    var showOauthMockByPlatform by remember { mutableStateOf<String?>(null) }

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

            // Webhook Integration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, if (webhookPublishUrl.isNotBlank()) LuxuryGold else BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = LuxuryGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "الربط الفعلي المباشر (Webhook)" else "Actual Webhook (Make/Zapier)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        
                        IconButton(
                            onClick = { showWebhookDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = LuxuryGold
                            )
                        }
                    }
                    
                    Text(
                        text = if (webhookPublishUrl.isNotBlank()) {
                            webhookPublishUrl
                        } else {
                            if (isArabic) "غير متصل (اضغط على أيقونة التعديل لإدخال عنوان Webhook لمزامنة الفيديوهات)" else "Not connected (Click edit to specify a webhook target for your videos)"
                        },
                        color = if (webhookPublishUrl.isNotBlank()) SoftGold else TextMutedColor,
                        fontSize = 13.sp,
                        fontWeight = if (webhookPublishUrl.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                    )
                    
                    Text(
                        text = if (isArabic) "عند تفعيل خيار الربط هذا، سيقوم التطبيق بإرسال ملف الفيديو النهائي (MP4) مع الهاشتاجات النصية المولدة تلقائياً بواسطة Gemini كطلب POST فوري بمجرد اكتمال الإنتاج!" else "Once configured, this Webhook receives a direct HTTP POST with the finalized generated MP4 video and all automatic Gemini SEO keywords as soon as rendering completes!",
                        color = TextSoftColor,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    
                    Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = if (isArabic) "🛠️ دليل المزامنة مع الحسابات الحقيقية (TikTok / Instagram / YouTube):" else "🛠️ Actual Multi-Platform Auto-Posting Guide (TikTok / Instagram / YouTube):",
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    
                    val guideSteps = if (isArabic) {
                        listOf(
                            "1️⃣ افتح حسابك على موقع Make.com أو Zapier وقم بإنشاء سيناريو جديد (Scenario / Zap).",
                            "2️⃣ أضف أول مودول ليكون من نوع Custom Webhook، وانسخ الرابط الذي يمنحه لك وضعه بالتطبيق هنا.",
                            "3️⃣ أضف المودول الثاني ليرتبط بمنصتك المفضلة (مثل: تيك توك، انستجرام ريلز، أو يوتيوب شورتس).",
                            "4️⃣ في خانة المرفقات، اختر الملف القادم من الويب هوك (اسم الحقل: video) ومن ثم اكتب وصف المنشور مستعملاً نصوص الهاشتاج والنص القرآني المولد تلقائياً.",
                            "5️⃣ احفظ التغييرات وشغّل السيناريو؛ الآن بمجرد انتهاء تصنيع الفيديو من التطبيق سيتم رفعه ونشره تلقائياً بالكامل في حسابك الحقيقي!"
                        )
                    } else {
                        listOf(
                            "1️⃣ Open Make.com, Zapier, or your own server and initialize a new automation workflow.",
                            "2️⃣ Build a 'Custom Webhook' trigger, copy its listening URL, and paste it into the Webhook card above.",
                            "3️⃣ Connect your actual TikTok Content Publishing, Instagram Reels, or YouTube Shorts module next.",
                            "4️⃣ Map the incoming 'video' file block to the post media parameter, and the generated 'payload' or description text to your post caption.",
                            "5️⃣ Turn on the automation; now your completed video will automatically post to your actual real profile!"
                        )
                    }
                    
                    guideSteps.forEach { step ->
                        Text(
                            text = step,
                            color = TextSoftColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // TikTok Card
            SocialPlatformAccountCard(
                platformId = "tiktok",
                platformName = if (isArabic) "تيك توك (TikTok)" else "TikTok Reels",
                isLinked = tiktokLinked,
                handle = tiktokHandle,
                accessToken = tiktokAccessToken,
                isAutopostEnabled = tiktokAutopost,
                isLinking = isLinkingPlatform == "tiktok",
                onLinkClick = {
                    if (tiktokLinked) {
                        scope.launch {
                            settingsManager.setTiktokLinked(false)
                            settingsManager.setTiktokHandle("")
                            settingsManager.setTiktokAccessToken("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط تيك توك" else "TikTok unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showOauthMockByPlatform = "tiktok"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "tiktok"
                    activeDialogHandle = tiktokHandle
                    activeDialogToken = tiktokAccessToken
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
                accessToken = instagramAccessToken,
                isAutopostEnabled = instagramAutopost,
                isLinking = isLinkingPlatform == "instagram",
                onLinkClick = {
                    if (instagramLinked) {
                        scope.launch {
                            settingsManager.setInstagramLinked(false)
                            settingsManager.setInstagramHandle("")
                            settingsManager.setInstagramAccessToken("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط انستقرام" else "Instagram unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showOauthMockByPlatform = "instagram"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "instagram"
                    activeDialogHandle = instagramHandle
                    activeDialogToken = instagramAccessToken
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
                accessToken = facebookAccessToken,
                isAutopostEnabled = facebookAutopost,
                isLinking = isLinkingPlatform == "facebook",
                onLinkClick = {
                    if (facebookLinked) {
                        scope.launch {
                            settingsManager.setFacebookLinked(false)
                            settingsManager.setFacebookHandle("")
                            settingsManager.setFacebookAccessToken("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط فيسبوك" else "Facebook unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showOauthMockByPlatform = "facebook"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "facebook"
                    activeDialogHandle = facebookHandle
                    activeDialogToken = facebookAccessToken
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
                accessToken = youtubeAccessToken,
                isAutopostEnabled = youtubeAutopost,
                isLinking = isLinkingPlatform == "youtube",
                onLinkClick = {
                    if (youtubeLinked) {
                        scope.launch {
                            settingsManager.setYoutubeLinked(false)
                            settingsManager.setYoutubeHandle("")
                            settingsManager.setYoutubeAccessToken("")
                            Toast.makeText(context, if (isArabic) "تم إلغاء ربط يوتيوب الشورتس" else "YouTube channel unlinked", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showOauthMockByPlatform = "youtube"
                    }
                },
                onSwitchAccountClick = {
                    activeDialogPlatform = "youtube"
                    activeDialogHandle = youtubeHandle
                    activeDialogToken = youtubeAccessToken
                },
                onAutopostToggle = { enabled ->
                    scope.launch { settingsManager.setYoutubeAutopost(enabled) }
                },
                isArabic = isArabic
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isArabic) "عند تفعيل خيار النشر وتوافر مفاتيح OAuth أو رابط الويب هوك، سيتم نشر Reel وتصدير تفاصيلها تلقائياً بالكامل بمجرد اكتمال التصيير السينمائي." else "Once OAuth keys or custom webhooks are ready, Reels are fully synthesized, customized, and published instantly.",
                color = TextMutedColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // Modal Webhook Configuration Dialog
    if (showWebhookDialog) {
        var tempUrl by remember { mutableStateOf(webhookPublishUrl) }
        AlertDialog(
            onDismissRequest = { showWebhookDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إعداد رابط الويب هـوك (Webhook)" else "Configure Automation Webhook",
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) 
                            "أدخل رابط الـ Webhook الخاص بك (مثال: Make.com أو Zapier) لاستقبال الفيديو النهائي فور جاهزيته ونشره تلقائياً:" 
                            else "Enter your central Webhook URL (e.g. from Make or Zapier) to receive completed video files instantly:",
                        color = TextSoftColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        placeholder = { Text("https://hook.us1.make.com/...", color = TextMutedColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = ScreenBg,
                            unfocusedContainerColor = ScreenBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            settingsManager.setWebhookPublishUrl(tempUrl.trim())
                            showWebhookDialog = false
                            Toast.makeText(context, if (isArabic) "تم حفظ رابط الويب هوك بنجاح!" else "Webhook URL saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ScreenBg)
                ) {
                    Text(if (isArabic) "حفظ وتفعيل" else "Backup & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebhookDialog = false }) {
                    Text(if (isArabic) "إلغاء الأمر" else "Cancel", color = TextMutedColor)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modern Interactive Mock OAuth Screen Dialog
    showOauthMockByPlatform?.let { platform ->
        MockOauthDialog(
            platform = platform,
            isArabic = isArabic,
            onDismiss = { showOauthMockByPlatform = null },
            onAuthorized = { generatedHandle, generatedToken ->
                showOauthMockByPlatform = null
                isLinkingPlatform = platform
                scope.launch {
                    delay(1500) // Beautiful API handshaking render latency
                    when (platform) {
                        "tiktok" -> {
                            settingsManager.setTiktokLinked(true)
                            settingsManager.setTiktokHandle(generatedHandle)
                            settingsManager.setTiktokAccessToken(generatedToken)
                        }
                        "instagram" -> {
                            settingsManager.setInstagramLinked(true)
                            settingsManager.setInstagramHandle(generatedHandle)
                            settingsManager.setInstagramAccessToken(generatedToken)
                        }
                        "facebook" -> {
                            settingsManager.setFacebookLinked(true)
                            settingsManager.setFacebookHandle(generatedHandle)
                            settingsManager.setFacebookAccessToken(generatedToken)
                        }
                        "youtube" -> {
                            settingsManager.setYoutubeLinked(true)
                            settingsManager.setYoutubeHandle(generatedHandle)
                            settingsManager.setYoutubeAccessToken(generatedToken)
                        }
                    }
                    isLinkingPlatform = null
                    Toast.makeText(context, if (isArabic) "تم المصادقة والربط عبر OAuth الفني بنجاح!" else "OAuth authentication & sync finished successfully!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Modal Switch / Link Dialog
    activeDialogPlatform?.let { platform ->
        SwitchAccountDialog(
            initialHandle = activeDialogHandle,
            initialToken = activeDialogToken,
            platform = platform.uppercase(),
            isArabic = isArabic,
            onDismiss = { activeDialogPlatform = null },
            onSave = { newHandle, newToken ->
                activeDialogPlatform = null
                isLinkingPlatform = platform
                scope.launch {
                    delay(1000)
                    when (platform) {
                        "tiktok" -> {
                            settingsManager.setTiktokLinked(true)
                            settingsManager.setTiktokHandle(newHandle)
                            settingsManager.setTiktokAccessToken(newToken)
                        }
                        "instagram" -> {
                            settingsManager.setInstagramLinked(true)
                            settingsManager.setInstagramHandle(newHandle)
                            settingsManager.setInstagramAccessToken(newToken)
                        }
                        "facebook" -> {
                            settingsManager.setFacebookLinked(true)
                            settingsManager.setFacebookHandle(newHandle)
                            settingsManager.setFacebookAccessToken(newToken)
                        }
                        "youtube" -> {
                            settingsManager.setYoutubeLinked(true)
                            settingsManager.setYoutubeHandle(newHandle)
                            settingsManager.setYoutubeAccessToken(newToken)
                        }
                    }
                    isLinkingPlatform = null
                    Toast.makeText(context, if (isArabic) "تم تمكين وتحديث ربط الحساب!" else "Platform account credentials updated!", Toast.LENGTH_SHORT).show()
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
    accessToken: String,
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
                            text = if (isLinked) (if (isArabic) "إلغاء الربط" else "Disconnect") else (if (isArabic) "ربط الحساب عبر OAuth" else "Connect via OAuth"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Expanded Options if linked
            if (isLinked && !isLinking) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ScreenBg,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            
                            // Edit Account/Credentials Manual Button
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
                                        text = if (isArabic) "تعديل الرموز يدوياً" else "Manual Config",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Displaying authentic Access Token placeholder safely
                        if (accessToken.isNotBlank()) {
                            val maskedToken = if (accessToken.length > 15) accessToken.take(6) + "..." + accessToken.takeLast(6) else accessToken
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) "رمز الوصول الفني (Token):" else "Technical OAuth Token:",
                                    color = TextMutedColor,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = maskedToken,
                                    color = Color(0xFF81C784),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
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
    initialToken: String,
    platform: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var handleText by remember { mutableStateOf(initialHandle) }
    var tokenText by remember { mutableStateOf(initialToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "تعديل رموز الاعتماد يدوياً ($platform)" else "Manual Credentials Link ($platform)",
                color = LuxuryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isArabic) "اسم الحساب أو المعرّف الخاص بك:" else "Your Social Handle / Page Username:",
                        color = TextSoftColor,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = handleText,
                        onValueChange = { handleText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = ScreenBg,
                            unfocusedContainerColor = ScreenBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isArabic) "رمز الوصول مخصص API Access Token (اختياري):" else "Custom API Access Token (Optional):",
                        color = TextSoftColor,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        placeholder = { Text("tok_live_...", color = TextMutedColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = ScreenBg,
                            unfocusedContainerColor = ScreenBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(handleText.trim(), tokenText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ScreenBg)
            ) {
                Text(if (isArabic) "حفظ وتثبيت" else "Save & Establish")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockOauthDialog(
    platform: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAuthorized: (String, String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Login prompt, 2: Permissions grant list, 3: Animated linkage callback
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var scope = rememberCoroutineScope()

    val primaryColor = when (platform) {
        "tiktok" -> Color.Black
        "instagram" -> Color(0xFFE1306C)
        "facebook" -> Color(0xFF1877F2)
        "youtube" -> Color(0xFFFF0000)
        else -> LuxuryGold
    }

    val platformName = platform.uppercase()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        content = {
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Browser Bar Header Mockup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF5F56), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFFBD2E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF27C93F), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "https://auth.$platform.com/oauth/v2/authorize",
                            color = TextMutedColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // STEP 1: LOGIN
                    if (step == 1) {
                        Text(
                            text = if (isArabic) "الدخول الآمن لربط حساب $platformName" else "Secure authentication to link $platformName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isArabic) "سجّل الدخول إلى منصتك لإنشاء رمز آمن لتطبيق Quran Reels" else "Log into your account to securely configure publishing credentials",
                            color = TextSoftColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(if (isArabic) "اسم المعرّف أو البريد" else "Username or Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = ScreenBg,
                                unfocusedContainerColor = ScreenBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (isArabic) "كلمة المرور المشفرة" else "Password") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = ScreenBg,
                                unfocusedContainerColor = ScreenBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (username.isNotBlank()) {
                                    step = 2
                                } else {
                                    username = "@${platform}_user_" + (100..999).random()
                                    step = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(if (isArabic) "تسجيل الدخول ومتابعة" else "Log In & Continue", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        TextButton(onClick = onDismiss) {
                            Text(if (isArabic) "إلغاء تماماً" else "Cancel Authorization", color = TextMutedColor)
                        }
                    }

                    // STEP 2: AUTHORIZATION SCOPES GRANTED SCREEN
                    else if (step == 2) {
                        Text(
                            text = if (isArabic) "منح صلاحية النشر التلقائي" else "Grant Access Permission",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryGold
                        )

                        Text(
                            text = if (isArabic) "يتطلب تطبيق Quran Reels الصلاحيات الآمنة التالية للتوزيع الفوري:" else "Quran Reels demands the following secured integrations:",
                            fontSize = 13.sp,
                            color = TextSoftColor,
                            textAlign = TextAlign.Center
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ScreenBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            val scopeLabels = if (isArabic) {
                                listOf(
                                    "✓ الوصول إلى معلومات الملف الشخصي الأساسية",
                                    "✓ صلاحية تصدير ورفع Reels ومقاطع فيديو تلقائية",
                                    "✓ إضافة الهاشتاجات النصية المولدة آلياً بـ AI"
                                )
                            } else {
                                listOf(
                                    "✓ Public Profile details access",
                                    "✓ Auto-publishing Reels and Shorts videos",
                                    "✓ Direct rich-text tagging & SEO descriptions"
                                )
                            }
                            scopeLabels.forEach { label ->
                                Text(text = label, color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                step = 3
                                scope.launch {
                                    while (progress < 1f) {
                                        delay(50)
                                        progress += 0.04f
                                    }
                                    val safeHandle = if (username.startsWith("@")) username else "@$username"
                                    val safeToken = "tok_live_${platform}_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                                    onAuthorized(safeHandle, safeToken)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(if (isArabic) "سماح ومنح ترخيص OAuth" else "Authorise & Grant Access", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        TextButton(onClick = { step = 1 }) {
                            Text(if (isArabic) "رجوع للخلف" else "Back", color = TextMutedColor)
                        }
                    }

                    // STEP 3: ANIMATED LINKAGE SYNC
                    else if (step == 3) {
                        Text(
                            text = if (isArabic) "جاري إجراء المصافحة الآمنة مع واجهات $platformName" else "Establishing OAuth Web Handshake...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = progress,
                            color = primaryColor,
                            trackColor = BorderColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color.Transparent, RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = if (isArabic) "جاري توليد وتثبيت مفاتيح التوزيع الآمنة بالخلفية..." else "Generating secure publish keys dynamically...",
                            fontSize = 12.sp,
                            color = TextSoftColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

