package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.settings.SettingsManager
import com.example.ui.ReelState
import com.example.ui.ReelViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.social.SocialMediaScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

val SURAH_NAMES = listOf(
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس", "التكوير", "الإنفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص", "الفلق", "الناس"
)

val SURAH_COUNTS = mapOf(1 to 7, 2 to 286, 3 to 200, 4 to 176, 5 to 120, 6 to 165, 7 to 206, 8 to 75, 9 to 129, 10 to 109, 11 to 123, 12 to 111, 13 to 43, 14 to 52, 15 to 99, 16 to 128, 17 to 111, 18 to 110, 19 to 98, 20 to 135, 21 to 112, 22 to 78, 23 to 118, 24 to 64, 25 to 77, 26 to 227, 27 to 93, 28 to 88, 29 to 69, 30 to 60, 31 to 34, 32 to 30, 33 to 73, 34 to 54, 35 to 45, 36 to 83, 37 to 182, 38 to 88, 39 to 75, 40 to 85, 41 to 54, 42 to 53, 43 to 89, 44 to 59, 45 to 37, 46 to 35, 47 to 38, 48 to 29, 49 to 18, 50 to 45, 51 to 60, 52 to 49, 53 to 62, 54 to 55, 55 to 78, 56 to 96, 57 to 29, 58 to 22, 59 to 24, 60 to 13, 61 to 14, 62 to 11, 63 to 11, 64 to 18, 65 to 12, 66 to 12, 67 to 30, 68 to 52, 69 to 52, 70 to 44, 71 to 28, 72 to 28, 73 to 20, 74 to 56, 75 to 40, 76 to 31, 77 to 50, 78 to 40, 79 to 46, 80 to 42, 81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22, 86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20, 91 to 15, 92 to 21, 93 to 11, 94 to 8, 95 to 8, 96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11, 101 to 11, 102 to 8, 103 to 3, 104 to 9, 105 to 5, 106 to 4, 107 to 7, 108 to 3, 109 to 6, 110 to 3, 111 to 5, 112 to 4, 113 to 5, 114 to 6)

// Color Palette for Dark Cinematic Feel
val LuxuryGold = Color(0xFFD29E57)
val SoftGold = Color(0xFFE5C085)
val ScreenBg = Color(0xFF0F0F12)
val CardBg = Color(0xFF18181D)
val BorderColor = Color(0xFF282830)
val TextSoftColor = Color(0xFFE0E0E6)
val TextMutedColor = Color(0xFF9E9EA5)

class MainActivity : ComponentActivity() {
    private val viewModel: ReelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val isDark by settingsManager.themeMode.collectAsState(initial = true)
            val language by settingsManager.language.collectAsState(initial = "ar")
            val isArabic = language == "ar"

            MyApplicationTheme(darkTheme = isDark) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    MainNavigationScaffold(
                        viewModel = viewModel,
                        settingsManager = settingsManager,
                        isArabic = isArabic
                    )
                }
            }
        }
    }
}

@Composable
fun MainNavigationScaffold(
    viewModel: ReelViewModel,
    settingsManager: SettingsManager,
    isArabic: Boolean
) {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = ScreenBg,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(if (isArabic) "الرئيسية" else "Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ScreenBg,
                        selectedTextColor = LuxuryGold,
                        unselectedIconColor = TextMutedColor,
                        unselectedTextColor = TextMutedColor,
                        indicatorColor = LuxuryGold
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "font",
                    onClick = { selectedTab = "font" },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text(if (isArabic) "الخط" else "Font") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ScreenBg,
                        selectedTextColor = LuxuryGold,
                        unselectedIconColor = TextMutedColor,
                        unselectedTextColor = TextMutedColor,
                        indicatorColor = LuxuryGold
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "social",
                    onClick = { selectedTab = "social" },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    label = { Text(if (isArabic) "النشر" else "Publish") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ScreenBg,
                        selectedTextColor = LuxuryGold,
                        unselectedIconColor = TextMutedColor,
                        unselectedTextColor = TextMutedColor,
                        indicatorColor = LuxuryGold
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "settings",
                    onClick = { selectedTab = "settings" },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(if (isArabic) "الإعدادات" else "Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ScreenBg,
                        selectedTextColor = LuxuryGold,
                        unselectedIconColor = TextMutedColor,
                        unselectedTextColor = TextMutedColor,
                        indicatorColor = LuxuryGold
                    )
                )
            }
        },
        containerColor = ScreenBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(viewModel = viewModel, isArabic = isArabic)
                "font" -> FontFormattingScreen(settingsManager = settingsManager, isArabic = isArabic)
                "social" -> SocialMediaScreen(isArabic = isArabic)
                "settings" -> SettingsScreen(onNavigateBack = { selectedTab = "home" })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ReelViewModel, isArabic: Boolean) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val recitersList by viewModel.reciters.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    var selectedSurahIdx by remember { mutableIntStateOf(0) }
    var startAyahText by remember { mutableStateOf("1") }
    var endAyahText by remember { mutableStateOf("5") }
    var selectedReciterIdx by remember { mutableIntStateOf(0) }

    var surahExpanded by remember { mutableStateOf(false) }
    var reciterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isArabic) "صانع المقاطع" else "Reel Maker", fontWeight = FontWeight.Bold, color = LuxuryGold, fontSize = 22.sp) },
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mosque Ring Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .border(2.dp, LuxuryGold.copy(alpha = 0.3f), CircleShape)
                    .padding(8.dp)
                    .border(1.dp, LuxuryGold, CircleShape)
                    .background(CardBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = LuxuryGold,
                    modifier = Modifier.size(54.dp)
                )
            }
            Text(
                text = if (isArabic) "اختر السورة والآيات لبدء الإنشاء" else "Select Surah and Ayahs to start creating",
                color = TextSoftColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            // Main Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Surah selection
                    Text(if (isArabic) "اختيار السورة" else "Select Surah", color = TextMutedColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(
                        expanded = surahExpanded,
                        onExpandedChange = { surahExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${selectedSurahIdx + 1}. ${SURAH_NAMES[selectedSurahIdx]}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surahExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextSoftColor,
                                unfocusedTextColor = TextSoftColor,
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = ScreenBg,
                                unfocusedContainerColor = ScreenBg,
                                disabledContainerColor = ScreenBg,
                                errorContainerColor = ScreenBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = surahExpanded,
                            onDismissRequest = { surahExpanded = false }
                        ) {
                            SURAH_NAMES.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text("${index + 1}. $name", color = TextSoftColor) },
                                    onClick = {
                                        selectedSurahIdx = index
                                        surahExpanded = false
                                        // Reset bounds when surah changes
                                        startAyahText = "1"
                                        val max = SURAH_COUNTS[index + 1] ?: 1
                                        endAyahText = if (max >= 5) "5" else max.toString()
                                    }
                                )
                            }
                        }
                    }

                    // Ayah bounds row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isArabic) "من الآية" else "From Ayah", color = TextMutedColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                            OutlinedTextField(
                                value = startAyahText,
                                onValueChange = { startAyahText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextSoftColor,
                                    unfocusedTextColor = TextSoftColor,
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = ScreenBg,
                                    unfocusedContainerColor = ScreenBg,
                                    disabledContainerColor = ScreenBg,
                                    errorContainerColor = ScreenBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isArabic) "إلى الآية" else "To Ayah", color = TextMutedColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                            OutlinedTextField(
                                value = endAyahText,
                                onValueChange = { endAyahText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextSoftColor,
                                    unfocusedTextColor = TextSoftColor,
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = ScreenBg,
                                    unfocusedContainerColor = ScreenBg,
                                    disabledContainerColor = ScreenBg,
                                    errorContainerColor = ScreenBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Reciter Dropdown
                    Text(if (isArabic) "القارئ" else "Reciter", color = TextMutedColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(
                        expanded = reciterExpanded,
                        onExpandedChange = { reciterExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (recitersList.isNotEmpty() && selectedReciterIdx < recitersList.size) recitersList[selectedReciterIdx].second else (if (isArabic) "جاري التحميل..." else "Loading..."),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextSoftColor,
                                unfocusedTextColor = TextSoftColor,
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = ScreenBg,
                                unfocusedContainerColor = ScreenBg,
                                disabledContainerColor = ScreenBg,
                                errorContainerColor = ScreenBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = reciterExpanded,
                            onDismissRequest = { reciterExpanded = false }
                        ) {
                            recitersList.forEachIndexed { index, reciter ->
                                DropdownMenuItem(
                                    text = { Text(reciter.second, color = TextSoftColor) },
                                    onClick = {
                                        selectedReciterIdx = index
                                        reciterExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Create Button
                    if (state is ReelState.Idle || state is ReelState.Error || state is ReelState.Success) {
                        Button(
                            onClick = {
                                val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                val onGenerateAction = {
                                    val start = startAyahText.toIntOrNull() ?: 1
                                    val end = endAyahText.toIntOrNull() ?: start
                                    val maxAyahs = SURAH_COUNTS[selectedSurahIdx + 1] ?: 1
                                    
                                    val cStart = start.coerceIn(1, maxAyahs)
                                    val cEnd = end.coerceIn(cStart, maxAyahs)
                                    
                                    viewModel.generate(
                                        context = context,
                                        surah = selectedSurahIdx + 1,
                                        startAyah = cStart,
                                        endAyah = cEnd,
                                        reciterId = if (recitersList.isNotEmpty()) recitersList[selectedReciterIdx].first else "ar.alafasy"
                                    )
                                }

                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    onGenerateAction()
                                } else {
                                    onGenerateAction()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuxuryGold,
                                contentColor = ScreenBg
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("generate_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ScreenBg, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "إنشاء الريلز" else "Create Reel",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Status and Results
            AnimatedVisibility(
                visible = state !is ReelState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (state) {
                            is ReelState.Error -> {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                                Text(
                                    text = (state as ReelState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.reset() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor, contentColor = TextSoftColor)
                                ) {
                                    Text(if (isArabic) "إعادة المحاولة" else "Retry")
                                }
                            }
                            is ReelState.Loading -> {
                                val loadingState = state as ReelState.Loading
                                CircularProgressIndicator(color = LuxuryGold, strokeWidth = 3.dp)
                                Text(
                                    text = loadingState.message,
                                    color = TextSoftColor,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                LinearProgressIndicator(
                                    progress = { loadingState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = LuxuryGold,
                                    trackColor = BorderColor
                                )
                            }
                            is ReelState.Success -> {
                                val successState = state as ReelState.Success
                                val uri = successState.uri
                                val generatedMeta = successState.generatedMeta
                                Text(
                                    text = if (isArabic) "تم إنشاء المقطع وسُجل بالاستوديو بنجاح! 🎉" else "Reel created and saved successfully! 🎉",
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
                                DisposableEffect(uri) {
                                    val player = ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(uri))
                                        prepare()
                                    }
                                    exoPlayer = player
                                    onDispose { player.release() }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                )

                                // Insert Gemini Social Media Metadata Section
                                generatedMeta?.let { meta ->
                                    GeneratedMetaSection(meta = meta, isArabic = isArabic)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, if (isArabic) "مشاركة المقطع" else "Share Reel"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ScreenBg),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isArabic) "مشاركة" else "Share", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.reset() },
                                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor, contentColor = TextSoftColor),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isArabic) "إنشاء جديد" else "New Reel", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontFormattingScreen(settingsManager: SettingsManager, isArabic: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences Collectors
    val fontFamily by settingsManager.fontFamily.collectAsState(initial = "Amiri")
    val fontSize by settingsManager.fontSize.collectAsState(initial = 40)
    val textColorStr by settingsManager.textColor.collectAsState(initial = "#FFD54F")
    val textOpacity by settingsManager.textOpacity.collectAsState(initial = 1.0f)
    
    val showTextBg by settingsManager.showTextBackground.collectAsState(initial = false)
    val textBgColorStr by settingsManager.textBgColor.collectAsState(initial = "#1C1C1E")
    val textBgOpacity by settingsManager.textBgOpacity.collectAsState(initial = 0.6f)
    val textBgRadius by settingsManager.textBgRadius.collectAsState(initial = 16)
    
    val textPosition by settingsManager.textPosition.collectAsState(initial = "Center")
    
    val showTranslation by settingsManager.showTranslation.collectAsState(initial = true)
    val translationFontSize by settingsManager.translationFontSize.collectAsState(initial = 25)
    val translationColorStr by settingsManager.translationColor.collectAsState(initial = "#FFFFFF")

    // Expansions
    var fontTypeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isArabic) "تنسيق الخط والترجمة" else "Font & Subtitles Style", fontWeight = FontWeight.Bold, color = LuxuryGold, fontSize = 20.sp) },
                actions = {
                    TextButton(onClick = {
                        Toast.makeText(context, if (isArabic) "تم حفظ التنسيق تلقائياً" else "Style saved automatically", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(if (isArabic) "حفظ" else "Save", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: الخط الأساسي
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "الخط الأساسي" else "Primary Font", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Font Type Dropdown
                    Text(if (isArabic) "نوع الخط" else "Font Family", color = TextMutedColor, fontSize = 13.sp)
                    ExposedDropdownMenuBox(
                        expanded = fontTypeExpanded,
                        onExpandedChange = { fontTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (fontFamily) {
                                "Amiri" -> if (isArabic) "Amiri (أميري)" else "Amiri (Classical)"
                                "Cairo" -> if (isArabic) "Cairo (كايرو)" else "Cairo (Modern)"
                                "Monospace" -> if (isArabic) "Monospace (منسق)" else "Monospace"
                                else -> if (isArabic) "Kufi (كوفي) / الافتراضي" else "Kufic / Default"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontTypeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextSoftColor,
                                unfocusedTextColor = TextSoftColor,
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = ScreenBg,
                                unfocusedContainerColor = ScreenBg,
                                disabledContainerColor = ScreenBg,
                                errorContainerColor = ScreenBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fontTypeExpanded,
                            onDismissRequest = { fontTypeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "Amiri (أميري)" else "Amiri Thread", color = TextSoftColor) },
                                onClick = {
                                    scope.launch { settingsManager.setFontFamily("Amiri") }
                                    fontTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "Cairo (كايرو)" else "Cairo Flat", color = TextSoftColor) },
                                onClick = {
                                    scope.launch { settingsManager.setFontFamily("Cairo") }
                                    fontTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "الخط القياسي (Default)" else "Standard Bold", color = TextSoftColor) },
                                onClick = {
                                    scope.launch { settingsManager.setFontFamily("Default") }
                                    fontTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "Monospace (ثابت العرض)" else "Monopoise Elegant", color = TextSoftColor) },
                                onClick = {
                                    scope.launch { settingsManager.setFontFamily("Monospace") }
                                    fontTypeExpanded = false
                                }
                            )
                        }
                    }

                    // Font Size Buttons (A- and A+)
                    Text(if (isArabic) "حجم النص" else "Text Size", color = TextMutedColor, fontSize = 13.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScreenBg, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        TextButton(
                            onClick = { scope.launch { settingsManager.setFontSize((fontSize - 2).coerceAtLeast(20)) } }
                        ) {
                            Text("A-", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        
                        Text("${fontSize}px", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        
                        TextButton(
                            onClick = { scope.launch { settingsManager.setFontSize((fontSize + 2).coerceAtMost(100)) } }
                        ) {
                            Text("A+", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Section 2: اللون والشفافية
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "اللون والشفافية" else "Color & Opacity", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Preset Color Dots
                    Text(if (isArabic) "لون النص" else "Text Color", color = TextMutedColor, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colorPresets = listOf(
                            "#FFD54F" to Color(0xFFFFD54F), // Yellow/Gold
                            "#E6D5C3" to Color(0xFFE6D5C3), // Beige
                            "#A5D6A7" to Color(0xFFA5D6A7), // Light Green
                            "#FFFFFF" to Color(0xFFFFFFFF), // White
                        )
                        colorPresets.forEach { (hex, clr) ->
                            val isSelected = textColorStr.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) LuxuryGold else BorderColor,
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .background(clr, CircleShape)
                                    .clickable {
                                        scope.launch { settingsManager.setTextColor(hex) }
                                    }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (clr == Color.White) Color.Black else Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    // Text Opacity Slider
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "شفافية النص" else "Text Opacity", color = TextMutedColor, fontSize = 13.sp)
                        Text("${(textOpacity * 100).toInt()}%", color = LuxuryGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Slider(
                        value = textOpacity,
                        onValueChange = { scope.launch { settingsManager.setTextOpacity(it) } },
                        colors = SliderDefaults.colors(
                            thumbColor = LuxuryGold,
                            activeTrackColor = LuxuryGold,
                            inactiveTrackColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 3: خلفية النص
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "خلفية النص" else "Text Background Box", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Switch(
                            checked = showTextBg,
                            onCheckedChange = { scope.launch { settingsManager.setShowTextBackground(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ScreenBg,
                                checkedTrackColor = LuxuryGold,
                                uncheckedThumbColor = TextMutedColor,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }

                    if (showTextBg) {
                        // Background Opacity
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "شفافية الخلفية" else "Background Opacity", color = TextMutedColor, fontSize = 13.sp)
                            Text("${(textBgOpacity * 100).toInt()}%", color = LuxuryGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Slider(
                            value = textBgOpacity,
                            onValueChange = { scope.launch { settingsManager.setTextBgOpacity(it) } },
                            colors = SliderDefaults.colors(thumbColor = LuxuryGold, activeTrackColor = LuxuryGold, inactiveTrackColor = BorderColor)
                        )

                        // Corner Radius
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "استدارة الزوايا" else "Corner Radius", color = TextMutedColor, fontSize = 13.sp)
                            Text("${textBgRadius}dp", color = LuxuryGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Slider(
                            value = textBgRadius.toFloat(),
                            onValueChange = { scope.launch { settingsManager.setTextBgRadius(it.toInt()) } },
                            valueRange = 0f..40f,
                            colors = SliderDefaults.colors(thumbColor = LuxuryGold, activeTrackColor = LuxuryGold, inactiveTrackColor = BorderColor)
                        )

                        // Background Colors
                        Text(if (isArabic) "اللون" else "Color", color = TextMutedColor, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val bgColors = listOf(
                                "#2C2621" to Color(0xFF2C2621), // Cozy Gold/Bronze
                                "#1C1C1E" to Color(0xFF1C1C1E), // Rich Carbon Grey
                                "#000000" to Color(0xFF000000)  // Pitch Black
                            )
                            bgColors.forEach { (hex, clr) ->
                                val isSelected = textBgColorStr.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) LuxuryGold else BorderColor,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .background(clr, CircleShape)
                                        .clickable {
                                            scope.launch { settingsManager.setTextBgColor(hex) }
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: الموضع
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "الموضع" else "Text Alignment Position", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val positions = listOf("Top" to "أعلى", "Center" to "وسط", "Bottom" to "أسفل")
                        positions.forEach { (pos, label) ->
                            val isSelected = textPosition == pos
                            Card(
                                onClick = { scope.launch { settingsManager.setTextPosition(pos) } },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LuxuryGold else ScreenBg
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, BorderColor) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) ScreenBg else TextSoftColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: الترجمة
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "الترجمة المصاحبة" else "Translation Settings", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Switch(
                            checked = showTranslation,
                            onCheckedChange = { scope.launch { settingsManager.setShowTranslation(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ScreenBg,
                                checkedTrackColor = LuxuryGold,
                                uncheckedThumbColor = TextMutedColor,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }

                    if (showTranslation) {
                        // Translation Text Size
                        Text(if (isArabic) "حجم خط الترجمة" else "Subtitles Font Size", color = TextMutedColor, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ScreenBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            TextButton(onClick = { scope.launch { settingsManager.setTranslationFontSize((translationFontSize - 2).coerceAtLeast(15)) } }) {
                                Text("A-", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("${translationFontSize}px", color = TextSoftColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { scope.launch { settingsManager.setTranslationFontSize((translationFontSize + 2).coerceAtMost(60)) } }) {
                                Text("A+", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Translation Color presets
                        Text(if (isArabic) "لون الترجمة" else "Subtitles Text Color", color = TextMutedColor, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val transColors = listOf(
                                "#E6D5C3" to Color(0xFFE6D5C3), // Beige
                                "#FFE082" to Color(0xFFFFE082), // Light Amber
                                "#FFFFFF" to Color(0xFFFFFFFF)  // Pure White
                            )
                            transColors.forEach { (hex, clr) ->
                                val isSelected = translationColorStr.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) LuxuryGold else BorderColor,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .background(clr, CircleShape)
                                        .clickable {
                                            scope.launch { settingsManager.setTranslationColor(hex) }
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (clr == Color.White) Color.Black else Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cinematic Interactive Live Preview Section
            Text(
                text = if (isArabic) "معاينة حية للمقطع" else "Live Reel Preview",
                color = LuxuryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            LivePreviewContainer(
                fontFamily = fontFamily,
                fontSize = fontSize,
                textColorStr = textColorStr,
                textOpacity = textOpacity,
                showTextBg = showTextBg,
                textBgColorStr = textBgColorStr,
                textBgOpacity = textBgOpacity,
                textBgRadius = textBgRadius,
                textPosition = textPosition,
                showTranslation = showTranslation,
                translationFontSize = translationFontSize,
                translationColorStr = translationColorStr,
                isArabic = isArabic
            )
        }
    }
}

@Composable
fun LivePreviewContainer(
    fontFamily: String,
    fontSize: Int,
    textColorStr: String,
    textOpacity: Float,
    showTextBg: Boolean,
    textBgColorStr: String,
    textBgOpacity: Float,
    textBgRadius: Int,
    textPosition: String,
    showTranslation: Boolean,
    translationFontSize: Int,
    translationColorStr: String,
    isArabic: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070A14), Color(0xFF140D07))
                )
            )
            .border(2.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        // Star sparkle circles inside preview
        Box(modifier = Modifier.size(6.dp).offset(x = 60.dp, y = 140.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
        Box(modifier = Modifier.size(4.dp).offset(x = 240.dp, y = 80.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
        Box(modifier = Modifier.size(5.dp).offset(x = 180.dp, y = 300.dp).background(Color.White.copy(alpha = 0.2f), CircleShape))

        // Text Positioner Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 50.dp),
            contentAlignment = when (textPosition) {
                "Top" -> Alignment.TopCenter
                "Bottom" -> Alignment.BottomCenter
                else -> Alignment.Center
            }
        ) {
            val contentCol = @Composable {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = if (showTextBg) {
                        val bgColor = try { Color(android.graphics.Color.parseColor(textBgColorStr)) } catch (e: Exception) { Color.Black }
                        Modifier
                            .background(
                                color = bgColor.copy(alpha = textBgOpacity),
                                shape = RoundedCornerShape(textBgRadius.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    } else {
                        Modifier
                    }
                ) {
                    // Quran Arabic Head
                    val rawCol = try { Color(android.graphics.Color.parseColor(textColorStr)) } catch (e: Exception) { Color.White }
                    val quranTextColor = rawCol.copy(alpha = textOpacity)
                    
                    Text(
                        text = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ",
                        fontFamily = when (fontFamily) {
                            "Amiri" -> FontFamily.Serif
                            "Cairo" -> FontFamily.SansSerif
                            "Monospace" -> FontFamily.Monospace
                            else -> FontFamily.Default
                        },
                        fontSize = (fontSize * 0.7f).sp, // Scaled for preview fits
                        fontWeight = FontWeight.Bold,
                        color = quranTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // English Subtitle Translation
                    if (showTranslation) {
                        val transColor = try { Color(android.graphics.Color.parseColor(translationColorStr)) } catch (e: Exception) { Color.LightGray }
                        HorizontalDivider(
                            modifier = Modifier
                                .width(50.dp)
                                .alpha(0.3f),
                            thickness = 1.dp,
                            color = transColor
                        )
                        Text(
                            text = "Indeed, We have granted you, [O Muhammad], al-Kawthar.",
                            fontSize = (translationFontSize * 0.65f).sp,
                            fontWeight = FontWeight.Medium,
                            color = transColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            contentCol()
        }

        // Realistic Reel Social Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(26.dp))
                }
                Text("1.2K", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text("48", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            // Spinning disk silhouette
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .background(Color.Black, CircleShape)
            )
        }

        // Live Preview Eyes banner (floating footer indicator)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) "معاينة حية للمقطع" else "Live Reel Mockup View",
                color = TextSoftColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GeneratedMetaSection(
    meta: com.example.generator.GeneratedMetaResult,
    isArabic: Boolean
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "🤖 وصف وعناوين النشر بالذكاء الاصطناعي (Gemini)" else "🤖 AI Generated Publishing Metadata (Gemini)",
            color = LuxuryGold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        meta.tiktok?.let { tiktok ->
            PlatformMetaPreviewCard(
                platformName = if (isArabic) "تيك توك (TikTok Reels)" else "TikTok Reels",
                title = tiktok.title,
                description = tiktok.description,
                hashtags = tiktok.hashtags,
                isArabic = isArabic,
                onCopy = {
                    val text = "العنوان: ${tiktok.title}\n\nالوصف:\n${tiktok.description}\n\nالهاشتاجات:\n${tiktok.hashtags}"
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(context, if (isArabic) "تم نسخ تفاصيل تيك توك" else "TikTok metadata copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        meta.instagram?.let { instagram ->
            PlatformMetaPreviewCard(
                platformName = if (isArabic) "انستقرام (Instagram Reels)" else "Instagram Reels",
                title = instagram.title,
                description = instagram.description,
                hashtags = instagram.hashtags,
                isArabic = isArabic,
                onCopy = {
                    val text = "العنوان: ${instagram.title}\n\nالوصف:\n${instagram.description}\n\nالهاشتاجات:\n${instagram.hashtags}"
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(context, if (isArabic) "تم نسخ تفاصيل انستقرام" else "Instagram metadata copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        meta.facebook?.let { facebook ->
            PlatformMetaPreviewCard(
                platformName = if (isArabic) "فيسبوك (Facebook Reels)" else "Facebook Reels",
                title = facebook.title,
                description = facebook.description,
                hashtags = facebook.hashtags,
                isArabic = isArabic,
                onCopy = {
                    val text = "العنوان: ${facebook.title}\n\nالوصف:\n${facebook.description}\n\nالهاشتاجات:\n${facebook.hashtags}"
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(context, if (isArabic) "تم نسخ تفاصيل فيسبوك" else "Facebook metadata copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        meta.youtube?.let { youtube ->
            PlatformMetaPreviewCard(
                platformName = if (isArabic) "يوتيوب (YouTube Shorts)" else "YouTube Shorts",
                title = youtube.title,
                description = youtube.description,
                hashtags = youtube.hashtags,
                isArabic = isArabic,
                onCopy = {
                    val text = "العنوان: ${youtube.title}\n\nالوصف:\n${youtube.description}\n\nالهاشتاجات:\n${youtube.hashtags}"
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(context, if (isArabic) "تم نسخ تفاصيل يوتيوب" else "YouTube metadata copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun PlatformMetaPreviewCard(
    platformName: String,
    title: String,
    description: String,
    hashtags: String,
    isArabic: Boolean,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ScreenBg),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = platformName,
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                TextButton(
                    onClick = onCopy,
                    colors = ButtonDefaults.textButtonColors(contentColor = LuxuryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "نسخ النصوص" else "Copy Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Text(
                text = if (isArabic) "العنوان المقترح:" else "Suggested Title:",
                color = TextMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = if (isArabic) "الوصف والتعليق المقترح:" else "Suggested Caption:",
                color = TextMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TextSoftColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Text(
                text = if (isArabic) "الهاشتاجات الذكية والكلمات المفتاحية:" else "Smart Hashtags & Tags:",
                color = TextMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = hashtags,
                color = LuxuryGold.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
