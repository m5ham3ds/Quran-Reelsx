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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
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
                        val isGoogle = settingsManager.googleDriveSheetsLinked.first() && settingsManager.googleAutoSaveEnabled.first()

                        val tiktokToken = settingsManager.tiktokAccessToken.first()
                        val instagramToken = settingsManager.instagramAccessToken.first()
                        val facebookToken = settingsManager.facebookAccessToken.first()
                        val youtubeToken = settingsManager.youtubeAccessToken.first()
                        val webhookUrl = settingsManager.webhookPublishUrl.first()

                        val hasAnyAutopost = isTiktok || isInstagram || isFacebook || isYoutube || webhookUrl.isNotBlank() || isGoogle

                        if (hasAnyAutopost) {
                            val isArabic = settingsManager.language.first() == "ar"
                            _uiState.value = ReelState.Loading(
                                if (isArabic) "جاري النشر التلقائي وتوزيع الفيديو الفعلي..." 
                                else "Performing real-time auto-publishing and distribution...",
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

                            // If direct Google Drive & Sheets integration is enabled
                            var googleDriveLink: String? = null
                            if (isGoogle) {
                                try {
                                    val tempFile = java.io.File(app.cacheDir, "temp_google_upload_${System.currentTimeMillis()}.mp4")
                                    app.contentResolver.openInputStream(state.uri)?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    
                                    val descriptionText = meta?.tiktok?.description 
                                        ?: meta?.instagram?.description 
                                        ?: "Quran Reel - Surah $surahName Ayat $currentStartAyah-$currentEndAyah"

                                    val googlePublisher = com.example.generator.GoogleDriveSheetsPublisher(app)
                                    val res = googlePublisher.publishReel(
                                        videoFile = tempFile,
                                        surahName = surahName,
                                        ayahRange = "$currentStartAyah-$currentEndAyah",
                                        reciterName = reciterName,
                                        description = descriptionText
                                    )
                                    if (res != null) {
                                        googleDriveLink = res.first
                                        android.util.Log.d("GooglePublisher", "Direct Google Publisher succeeded! Link: $googleDriveLink")
                                    }
                                    try { tempFile.delete() } catch (ex: Exception) {}
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.util.Log.e("GooglePublisher", "Direct Google Publisher failed: ${e.message}")
                                }
                            }

                            // If a real Webhook is configured, dispatch the video and data to it
                            if (webhookUrl.isNotBlank()) {
                                dispatchWebhook(
                                    webhookUrl = webhookUrl,
                                    videoUri = state.uri,
                                    surah = currentSurah,
                                    startAyah = currentStartAyah,
                                    endAyah = currentEndAyah,
                                    reciter = currentReciterId,
                                    tiktokToken = tiktokToken,
                                    instagramToken = instagramToken,
                                    facebookToken = facebookToken,
                                    youtubeToken = youtubeToken,
                                    metaResult = meta
                                )
                            }

                            _uiState.value = ReelState.Success(
                                uri = state.uri,
                                generatedMeta = meta,
                                publishedPlatforms = mapOf(
                                    "tiktok" to isTiktok,
                                    "instagram" to isInstagram,
                                    "facebook" to isFacebook,
                                    "youtube" to isYoutube,
                                    "google_drive" to (googleDriveLink != null)
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
    
    fun togglePauseGeneration() {
        VideoGenerationService.togglePauseResumed()
        val current = _uiState.value
        if (current is ReelState.Loading) {
            _uiState.value = ReelState.Loading(current.message, current.progress)
        }
    }

    fun cancelGeneration() {
        VideoGenerationService.cancelGeneration()
        _uiState.value = ReelState.Idle
    }

    fun isGenerationPaused(): Boolean {
        return VideoGenerationService.isPaused
    }
    
    fun reset() {
        VideoGenerationService.clearState()
        _uiState.value = ReelState.Idle
    }

    private fun dispatchWebhook(
        webhookUrl: String,
        videoUri: Uri,
        surah: Int,
        startAyah: Int,
        endAyah: Int,
        reciter: String,
        tiktokToken: String,
        instagramToken: String,
        facebookToken: String,
        youtubeToken: String,
        metaResult: com.example.generator.GeneratedMetaResult?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                var fileBytes: ByteArray? = null
                try {
                    app.contentResolver.openInputStream(videoUri)?.use { input ->
                        fileBytes = input.readBytes()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("Webhook", "Could not read video file bytes: ${e.message}")
                }

                val json = JSONObject().apply {
                    put("event", "video_generated")
                    put("surah", surah)
                    put("startAyah", startAyah)
                    put("endAyah", endAyah)
                    put("reciter", reciter)
                    put("localVideoUri", videoUri.toString())
                    
                    val metaJson = JSONObject().apply {
                        if (metaResult != null) {
                            metaResult.tiktok?.let {
                                put("tiktok", JSONObject().apply {
                                    put("title", it.title)
                                    put("description", it.description)
                                    put("hashtags", it.hashtags)
                                })
                            }
                            metaResult.instagram?.let {
                                put("instagram", JSONObject().apply {
                                    put("title", it.title)
                                    put("description", it.description)
                                    put("hashtags", it.hashtags)
                                })
                            }
                            metaResult.facebook?.let {
                                put("facebook", JSONObject().apply {
                                    put("title", it.title)
                                    put("description", it.description)
                                    put("hashtags", it.hashtags)
                                })
                            }
                            metaResult.youtube?.let {
                                put("youtube", JSONObject().apply {
                                    put("title", it.title)
                                    put("description", it.description)
                                    put("hashtags", it.hashtags)
                                })
                            }
                        }
                    }
                    put("geminiMetadata", metaJson)

                    val tokensJson = JSONObject().apply {
                        put("tiktokToken", tiktokToken)
                        put("instagramToken", instagramToken)
                        put("facebookToken", facebookToken)
                        put("youtubeToken", youtubeToken)
                    }
                    put("apiTokens", tokensJson)
                }

                val requestBody = if (fileBytes != null) {
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("event", "video_generated")
                        .addFormDataPart("surah", surah.toString())
                        .addFormDataPart("startAyah", startAyah.toString())
                        .addFormDataPart("endAyah", endAyah.toString())
                        .addFormDataPart("reciter", reciter)
                        .addFormDataPart("payload", json.toString())
                        .addFormDataPart(
                            "video",
                            "quran_reel_${System.currentTimeMillis()}.mp4",
                            fileBytes!!.toRequestBody("video/mp4".toMediaType())
                        )
                        .build()
                } else {
                    json.toString().toRequestBody("application/json".toMediaType())
                }

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    android.util.Log.d("Webhook", "Webhook execution completed with code: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("Webhook", "Webhook execution failed: ${e.message}")
            }
        }
    }
}
