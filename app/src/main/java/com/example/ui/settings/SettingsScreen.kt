package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val pexelsKey by settingsManager.pexelsApiKey.collectAsState(initial = "")
    val pixabayKey by settingsManager.pixabayApiKey.collectAsState(initial = "")
    val geminiKey by settingsManager.geminiApiKey.collectAsState(initial = "")
    val isDark by settingsManager.themeMode.collectAsState(initial = true)
    val showTrans by settingsManager.showTranslation.collectAsState(initial = true)
    val language by settingsManager.language.collectAsState(initial = "ar")
    val videoQuality by settingsManager.videoQuality.collectAsState(initial = "Ultra")

    val isArabic = language == "ar"

    // App Colors matching MainActivity
    val ScreenBg = Color(0xFF0F0F12)
    val CardBg = Color(0xFF18181D)
    val LuxuryGold = Color(0xFFD29E57)
    val TextMutedColor = Color(0xFF9E9EA5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "إعدادات المصمم" else "Designer Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Appearance and Language Card
                Text(
                    text = if (isArabic) "المظهر واللغة" else "Appearance & Language",
                    color = LuxuryGold, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CardBg, 
                    border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Theme switches
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LuxuryGold.copy(alpha=0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = LuxuryGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "الوضع الداكن" else "Dark Theme",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isArabic) "مريح للعين وخيار مثالي للتطوير" else "Comfortable dark palette layout",
                                    color = TextMutedColor,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = isDark,
                                onCheckedChange = { scope.launch { settingsManager.setThemeMode(it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LuxuryGold,
                                    checkedTrackColor = LuxuryGold.copy(alpha=0.4f)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0x15FFFFFF))

                        // Translation switches
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LuxuryGold.copy(alpha=0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = LuxuryGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "ترجمة المعاني" else "Interpretive Translation",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isArabic) "عرض الترجمة الأجنبية تحت الآية" else "Append translated verses to screen",
                                    color = TextMutedColor,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = showTrans,
                                onCheckedChange = { scope.launch { settingsManager.setShowTranslation(it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LuxuryGold,
                                    checkedTrackColor = LuxuryGold.copy(alpha=0.4f)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0x15FFFFFF))

                        // Language picker Custom Dropdown
                        Column {
                            Text(
                                text = if (isArabic) "لغة واجهة التطبيق" else "Terminal Language",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            var langExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                onClick = { langExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x14FFFFFF),
                                border = BorderStroke(1.dp, Color(0x2BFFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "ar") "العربية" else "English",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFFCFD8DC)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = langExpanded,
                                onDismissRequest = { langExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(CardBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("اللغة العربية", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        scope.launch { settingsManager.setLanguage("ar") }
                                        langExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("English Language", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        scope.launch { settingsManager.setLanguage("en") }
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0x15FFFFFF))

                    // Video Quality Dropdown
                    Column {
                        Text(
                            text = if (isArabic) "جودة المشاهد السينمائية" else "Video Library Quality",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        var qualityExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { qualityExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x14FFFFFF),
                                border = BorderStroke(1.dp, Color(0x2BFFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when(videoQuality) {
                                            "Normal" -> if (isArabic) "عادية" else "Normal"
                                            "High" -> if (isArabic) "عالية" else "High"
                                            else -> if (isArabic) "عالية جدآ" else "Ultra"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Info, // Generic icon
                                        contentDescription = null,
                                        tint = Color(0xFFCFD8DC)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = qualityExpanded,
                                onDismissRequest = { qualityExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(CardBg)
                            ) {
                                val options = listOf("Normal", "High", "Ultra")
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                when(option) {
                                                    "Normal" -> if (isArabic) "عادية" else "Normal"
                                                    "High" -> if (isArabic) "عالية" else "High"
                                                    else -> if (isArabic) "عالية جدآ" else "Ultra"
                                                },
                                                color = Color.White, 
                                                fontWeight = FontWeight.Bold
                                            ) 
                                        },
                                        onClick = {
                                            scope.launch { settingsManager.setVideoQuality(option) }
                                            qualityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: API Keys and Video Backdrop config
            Text(
                text = if (isArabic) "مصادر ومفاتيح الـ API" else "Integration & API Configuration",
                color = LuxuryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardBg,
                border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFFFF176),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isArabic) 
                                    "قم بإدخال المفتاح أدناه لتفعيل ميزة تنزيل خلفيات سينمائية طبيعية ومتحركة تلقائياً وبشكل مجاني تماماً!" 
                                    else "Provide API keys below to download gorgeous cinematic background textures automatically and for free!",
                                color = Color(0xFFECEFF1),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Pexels Text Field & instructions
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xBCFFFFFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pexels Key", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Text(
                                    text = if (isArabic) "احصل على مفتاح Pexels" else "Get free Pexels key",
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { uriHandler.openUri("https://www.pexels.com/api/") }
                                        .padding(4.dp)
                                )
                            }
                            OutlinedTextField(
                                value = pexelsKey,
                                onValueChange = { scope.launch { settingsManager.savePexelsKey(it) } },
                                placeholder = { Text(if (isArabic) "أدخل مفتاح Pexels هنا..." else "Paste your Pexels token...", color = Color(0x61FFFFFF)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedContainerColor = Color(0x0FFFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pixabay Text Field & instructions
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xBCFFFFFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pixabay Key", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Text(
                                    text = if (isArabic) "احصل على مفتاح Pixabay" else "Get free Pixabay key",
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { uriHandler.openUri("https://pixabay.com/api/docs/") }
                                        .padding(4.dp)
                                )
                            }
                            OutlinedTextField(
                                value = pixabayKey,
                                onValueChange = { scope.launch { settingsManager.savePixabayKey(it) } },
                                placeholder = { Text(if (isArabic) "أدخل مفتاح Pixabay هنا..." else "Paste your Pixabay token...", color = Color(0x61FFFFFF)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedContainerColor = Color(0x0FFFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Gemini Text Field & instructions
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xBCFFFFFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gemini AI Key", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Text(
                                    text = if (isArabic) "احصل على مفتاح Gemini مجاناً" else "Get free Gemini key",
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { uriHandler.openUri("https://aistudio.google.com/") }
                                        .padding(4.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "مطلوب لتوليد العناوين والوصف والهاشتاجات الذكية لكل منصة بشكل احترافي" else "Required to generate smart platform-specific titles, descriptions, and tags automatically",
                                color = TextMutedColor,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { scope.launch { settingsManager.saveGeminiKey(it) } },
                                placeholder = { Text(if (isArabic) "أدخل مفتاح Gemini هنا لروبوت الذكاء الاصطناعي..." else "Paste your Gemini API key...", color = Color(0x61FFFFFF)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedContainerColor = Color(0x0FFFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Autosaver status block
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0x73FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "يتم حفظ التعديلات تلقائياً" else "All settings autosave instantly",
                        color = Color(0x73FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
