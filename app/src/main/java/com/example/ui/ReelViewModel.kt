package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.generator.VideoGenerator
import com.example.service.VideoGenerationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class ReelState {
    object Idle : ReelState()
    data class Loading(val message: String, val progress: Float) : ReelState()
    data class Success(
        val uri: Uri,
        val generatedMeta: com.example.generator.GeneratedMetaResult? = null,
        val publishedPlatforms: Map<String, Boolean> = emptyMap()
    ) : ReelState()
    data class Error(val message: String) : ReelState()
}

class ReelViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    
    private val _uiState = MutableStateFlow<ReelState>(ReelState.Idle)
    val uiState: StateFlow<ReelState> = _uiState
    
    private val _reciters = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val reciters: StateFlow<List<Pair<String, String>>> = _reciters

    // Active Generation Metadata Tracker
    private var currentSurah: Int = 1
    private var currentStartAyah: Int = 1
    private var currentEndAyah: Int = 5
    private var currentReciterId: String = "ar.alafasy"
    
    init {
        fetchReciters()
        viewModelScope.launch {
            VideoGenerationService.serviceState.collect { state ->
                when (state) {
                    is ReelState.Success -> {
                        val currentState = _uiState.value
                        if (currentState is ReelState.Success) {
                            return@collect
                        }

                        val app = getApplication<Application>()
                        val settingsManager = com.example.settings.SettingsManager(app)
                        
                        val isTiktok = settingsManager.tiktokLinked.first() && settingsManager.tiktokAutopost.first()
                        val isInstagram = settingsManager.instagramLinked.first() && settingsManager.instagramAutopost.first()
                        val isFacebook = settingsManager.facebookLinked.first() && settingsManager.facebookAutopost.first()
                        val isYoutube = settingsManager.youtubeLinked.first() && settingsManager.youtubeAutopost.first()

                        val hasAnyAutopost = isTiktok || isInstagram || isFacebook || isYoutube

                        if (hasAnyAutopost) {
                            val isArabic = settingsManager.language.first() == "ar"
                            _uiState.value = ReelState.Loading(
                                if (isArabic) "جاري توليد تفاصيل النشر بالذكاء الاصطناعي (Gemini)..." 
                                else "Generating social publishing metadata via Gemini...",
                                0.95f
                            )

                            val surahName = com.example.SURAH_NAMES.getOrNull(currentSurah - 1) ?: "سورة"
                            val reciterName = _reciters.value.find { it.first == currentReciterId }?.second ?: "العفاسي"

                            val generator = com.example.generator.GeminiMetaGenerator()
                            val meta = generator.generateSocialMeta(
                                context = app,
                                surahName = surahName,
                                startAyah = currentStartAyah,
                                endAyah = currentEndAyah,
                                reciterName = reciterName,
                                isTiktok = isTiktok,
                                isInstagram = isInstagram,
                                isFacebook = isFacebook,
                                isYoutube = isYoutube
                            )

                            _uiState.value = ReelState.Success(
                                uri = state.uri,
                                generatedMeta = meta,
                                publishedPlatforms = mapOf(
                                    "tiktok" to isTiktok,
                                    "instagram" to isInstagram,
                                    "facebook" to isFacebook,
                                    "youtube" to isYoutube
                                )
                            )
                        } else {
                            _uiState.value = ReelState.Success(
                                uri = state.uri,
                                generatedMeta = null,
                                publishedPlatforms = emptyMap()
                            )
                        }
                    }
                    is ReelState.Loading -> {
                        _uiState.value = state
                    }
                    is ReelState.Error -> {
                        _uiState.value = state
                    }
                    is ReelState.Idle -> {
                        _uiState.value = state
                    }
                }
            }
        }
    }
    
    private fun fetchReciters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://api.alquran.cloud/v1/edition?format=audio&language=ar").build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val data = json.getJSONArray("data")
                    val list = mutableListOf<Pair<String, String>>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        val id = obj.getString("identifier")
                        val name = if (obj.has("name") && !obj.isNull("name")) {
                            obj.getString("name")
                        } else {
                            obj.getString("englishName")
                        }
                        list.add(id to name)
                    }
                    if (list.isNotEmpty()) {
                        _reciters.value = list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback list
                _reciters.value = listOf(
                    "ar.alafasy" to "مشاري العفاسي",
                    "ar.sudais" to "عبد الرحمن السديس",
                    "ar.abdulbasitmurattal" to "عبد الباسط عبد الصمد"
                )
            }
        }
    }
    
    fun generate(
        context: Context,
        surah: Int,
        startAyah: Int,
        endAyah: Int,
        reciterId: String
    ) {
        currentSurah = surah
        currentStartAyah = startAyah
        currentEndAyah = endAyah
        currentReciterId = reciterId

        _uiState.value = ReelState.Loading("جاري البدء...", 0f)
        viewModelScope.launch {
            val settingsManager = com.example.settings.SettingsManager(context)
            val showTranslation = settingsManager.showTranslation.first()
            val pexelsApiKey = settingsManager.pexelsApiKey.first()
            
            val intent = Intent(context, VideoGenerationService::class.java).apply {
                putExtra("surah", surah)
                putExtra("startAyah", startAyah)
                putExtra("endAyah", endAyah)
                putExtra("reciterId", reciterId)
                putExtra("showTranslation", showTranslation)
                putExtra("pexelsApiKey", pexelsApiKey)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    fun reset() {
        VideoGenerationService.clearState()
        _uiState.value = ReelState.Idle
    }
}
