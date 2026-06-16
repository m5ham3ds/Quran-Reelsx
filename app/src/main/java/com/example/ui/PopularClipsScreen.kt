package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import com.example.ui.ReelState
import com.example.ui.ReelViewModel
import com.example.SURAH_NAMES
import com.example.LuxuryGold
import com.example.SoftGold
import com.example.ScreenBg
import com.example.CardBg
import com.example.BorderColor
import com.example.TextSoftColor
import com.example.TextMutedColor

data class CuratedClip(
    val id: String,
    val reciter: String,
    val title: String,
    val surah: Int,
    val ayahStart: Int,
    val ayahEnd: Int,
    val audioUrl: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularClipsScreen(
    viewModel: ReelViewModel,
    isArabic: Boolean,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    
    // Core built-in Curated database
    val baseClipsList = remember {
        mutableStateListOf(
            CuratedClip(
                id = "clip_mossad_rahman",
                reciter = "عبد الرحمن مسعد",
                title = "الرحمن • علّم القرآن • خلق الإنسان",
                surah = 55,
                ayahStart = 1,
                ayahEnd = 13,
                audioUrl = "https://download.tvquran.com/download/recitations/372/303/055.mp3",
                category = "طمأنينة"
            ),
            CuratedClip(
                id = "clip_mossad_sajdah",
                reciter = "عبد الرحمن مسعد",
                title = "تنزيل الكتاب لا ريب فيه من رب العالمين",
                surah = 32,
                ayahStart = 1,
                ayahEnd = 9,
                audioUrl = "https://download.tvquran.com/download/recitations/372/303/032.mp3",
                category = "طمأنينة"
            ),
            CuratedClip(
                id = "clip_mossad_mulk",
                reciter = "عبد الرحمن مسعد",
                title = "تبارك الذي بيده الملك وهو على كل شيء قدير",
                surah = 67,
                ayahStart = 1,
                ayahEnd = 5,
                audioUrl = "https://download.tvquran.com/download/recitations/372/303/067.mp3",
                category = "سكينة"
            ),
            CuratedClip(
                id = "clip_mossad_anbiya",
                reciter = "عبد الرحمن مسعد",
                title = "دعاء ذي النون - لا إله إلا أنت سبحانك إني كنت من الظالمين",
                surah = 21,
                ayahStart = 87,
                ayahEnd = 88,
                audioUrl = "https://download.tvquran.com/download/recitations/372/303/021.mp3",
                category = "دعاء"
            ),
            CuratedClip(
                id = "clip_mossad_infitar",
                reciter = "عبد الرحمن مسعد",
                title = "يا أيها الإنسان ما غرك بربك الكريم",
                surah = 82,
                ayahStart = 6,
                ayahEnd = 12,
                audioUrl = "https://download.tvquran.com/download/recitations/372/303/082.mp3",
                category = "خشوع"
            ),
            CuratedClip(
                id = "clip_sobhi_isra",
                reciter = "إسلام صبحي",
                title = "إن هذا القرآن يهدي للتي هي أقوم ويبشر المؤمنين",
                surah = 17,
                ayahStart = 9,
                ayahEnd = 11,
                audioUrl = "https://server11.mp3quran.net/sobhi/017.mp3",
                category = "طمأنينة"
            ),
            CuratedClip(
                id = "clip_sobhi_kahf",
                reciter = "إسلام صبحي",
                title = "المال والبنون زينة الحياة الدنيا والباقيات الصالحات",
                surah = 18,
                ayahStart = 46,
                ayahEnd = 49,
                audioUrl = "https://server11.mp3quran.net/sobhi/018.mp3",
                category = "سكينة"
            ),
            CuratedClip(
                id = "clip_alafasy_hashr",
                reciter = "مشاري العفاسي",
                title = "لو أنزلنا هذا القرآن على جبل لرأيته خاشعاً متصدعاً",
                surah = 59,
                ayahStart = 21,
                ayahEnd = 24,
                audioUrl = "https://server8.mp3quran.net/afs/059.mp3",
                category = "خشوع"
            )
        )
    }

    val categories = listOf(
        if (isArabic) "الكل" else "All",
        if (isArabic) "طمأنينة" else "Tranquility",
        if (isArabic) "خشوع" else "Devotion",
        if (isArabic) "سكينة" else "Serenity",
        if (isArabic) "دعاء" else "Dua"
    )
    
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedClip by remember { mutableStateOf<CuratedClip?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Promotional Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(LuxuryGold.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .border(1.dp, LuxuryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "توليد المقاطع الرائجة بنقرة واحدة" else "Trending One-Click Production",
                        color = LuxuryGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) 
                            "مقاطع تم تلاوتها بنشيد روحي خاشع وهادئ، جاهزة تلقائياً للمونتاج والتوافق البصري بالذكاء الاصطناعي."
                        else 
                            "Curated, peaceful verses read by elite reciters, optimized instantly for WhisperX word-alignment.",
                        color = TextMutedColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LuxuryGold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = LuxuryGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories Chips List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isSelected) LuxuryGold else CardBg)
                        .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(50.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("category_chip_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) ScreenBg else TextSoftColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Custom Clip Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_custom_clip_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardBg,
                contentColor = LuxuryGold
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "إضافة مقطع رائج مخصص" else "Add Custom Curated Clip",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filtering list of clips
        val filteredClips = if (selectedCategory == "الكل" || selectedCategory == "All") {
            baseClipsList
        } else {
            baseClipsList.filter { it.category == selectedCategory }
        }

        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد مقاطع مضافة في هذا التصنيف حالياً" else "No clips found in this category",
                    color = TextMutedColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filteredClips.forEach { clip ->
                val isCurrentSelected = selectedClip?.id == clip.id
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentSelected) CardBg.copy(alpha = 0.8f) else CardBg
                    ),
                    border = BorderStroke(
                        width = if (isCurrentSelected) 1.5.dp else 1.dp,
                        color = if (isCurrentSelected) LuxuryGold else BorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            selectedClip = if (isCurrentSelected) null else clip
                        }
                        .testTag("clip_card_${clip.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LuxuryGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🎧", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = clip.reciter,
                                        color = TextSoftColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val surahName = SURAH_NAMES.getOrNull(clip.surah - 1) ?: "سورة ${clip.surah}"
                                    val rangeText = if (clip.ayahStart == clip.ayahEnd) "${clip.ayahStart}" else "${clip.ayahStart}-${clip.ayahEnd}"
                                    Text(
                                        text = "$surahName • الآية $rangeText",
                                        color = TextMutedColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LuxuryGold.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = clip.category,
                                    color = LuxuryGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = clip.title,
                            color = SoftGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )

                        AnimatedVisibility(
                            visible = isCurrentSelected,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = LuxuryGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "رابط البث: ${clip.audioUrl.take(45)}..." else "Source: ${clip.audioUrl.take(45)}...",
                                        color = TextMutedColor,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Generate Trigger Button
                                Button(
                                    onClick = {
                                        viewModel.generate(
                                            context = context,
                                            surah = clip.surah,
                                            startAyah = clip.ayahStart,
                                            endAyah = clip.ayahEnd,
                                            reciterId = "popular|" + clip.audioUrl
                                        )
                                        Toast.makeText(context, if (isArabic) "بدء المونتاج لـ ${clip.reciter}..." else "Starting production...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("generate_popular_clip_btn"),
                                    enabled = state !is ReelState.Loading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LuxuryGold,
                                        contentColor = ScreenBg
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isArabic) "إنشاء ريل سينمائي مبارك" else "Create Cinematic Quran Reel",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Dialog to Add Custom Clips
    if (showAddDialog) {
        var addReciter by remember { mutableStateOf("") }
        var addTitle by remember { mutableStateOf("") }
        var addSurahStr by remember { mutableStateOf("1") }
        var addStartStr by remember { mutableStateOf("1") }
        var addEndStr by remember { mutableStateOf("1") }
        var addUrl by remember { mutableStateOf("") }
        var addCategory by remember { mutableStateOf("سكينة") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardBg,
            title = {
                Text(
                    text = if (isArabic) "إضافة مقطع تلاوة جديدة" else "Add New Recitation Clip",
                    color = LuxuryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = addReciter,
                        onValueChange = { addReciter = it },
                        label = { Text(if (isArabic) "القارئ" else "Reciter") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = LuxuryGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addTitle,
                        onValueChange = { addTitle = it },
                        label = { Text(if (isArabic) "عنوان المقطع / الآية المقروءة" else "Clip Title / Verse Preview") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = LuxuryGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = addSurahStr,
                            onValueChange = { addSurahStr = it },
                            label = { Text(if (isArabic) "السورة (رقم)" else "Surah No") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = LuxuryGold
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = addStartStr,
                            onValueChange = { addStartStr = it },
                            label = { Text(if (isArabic) "من آية" else "From") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = LuxuryGold
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = addEndStr,
                            onValueChange = { addEndStr = it },
                            label = { Text(if (isArabic) "إلى آية" else "To") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = LuxuryGold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = addUrl,
                        onValueChange = { addUrl = it },
                        label = { Text(if (isArabic) "رابط تيار الصوت المستمر (MP3)" else "Continuous MP3 Stream URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = LuxuryGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Simple select category
                    Text(
                        text = if (isArabic) "التصنيف الروحي" else "Spiritual Theme",
                        color = TextSoftColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableThemes = listOf("طمأنينة", "خشوع", "سكينة", "دعاء")
                        availableThemes.forEach { th ->
                            val isSelected = addCategory == th
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) LuxuryGold else ScreenBg)
                                    .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { addCategory = th }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = th,
                                    color = if (isSelected) ScreenBg else TextSoftColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sNum = addSurahStr.toIntOrNull() ?: 1
                        val startNum = addStartStr.toIntOrNull() ?: 1
                        val endNum = addEndStr.toIntOrNull() ?: 1
                        
                        if (addReciter.isBlank() || addTitle.isBlank() || addUrl.isBlank()) {
                            Toast.makeText(context, if (isArabic) "يرجى ملء جميع الحقول العامة والرابط!" else "Fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            baseClipsList.add(
                                CuratedClip(
                                    id = "clip_custom_${System.currentTimeMillis()}",
                                    reciter = addReciter,
                                    title = addTitle,
                                    surah = sNum,
                                    ayahStart = startNum,
                                    ayahEnd = endNum,
                                    audioUrl = addUrl,
                                    category = addCategory
                                )
                            )
                            showAddDialog = false
                            Toast.makeText(context, if (isArabic) "تمت إضافة المقطع الرائج لقائمتك بنجاح!" else "Clip added successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ScreenBg)
                ) {
                    Text(if (isArabic) "حفظ المقرأ" else "Save Clip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isArabic) "إلغاء الأمر" else "Cancel", color = TextMutedColor)
                }
            }
        )
    }
}
