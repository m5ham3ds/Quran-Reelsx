package com.example.generator

import android.content.Context
import com.example.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class PlatformMeta(
    val title: String,
    val description: String,
    val hashtags: String
)

data class GeneratedMetaResult(
    val tiktok: PlatformMeta?,
    val instagram: PlatformMeta?,
    val facebook: PlatformMeta?,
    val youtube: PlatformMeta?
)

class GeminiMetaGenerator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun generateSocialMeta(
        context: Context,
        surahName: String,
        startAyah: Int,
        endAyah: Int,
        reciterName: String,
        isTiktok: Boolean,
        isInstagram: Boolean,
        isFacebook: Boolean,
        isYoutube: Boolean
    ): GeneratedMetaResult? = withContext(Dispatchers.IO) {
        val settingsManager = SettingsManager(context)
        var apiKey = settingsManager.geminiApiKey.first()
        
        // If empty in user settings, check if there is an injection in BuildConfig
        if (apiKey.isBlank()) {
            apiKey = com.example.BuildConfig.GEMINI_API_KEY
        }
        
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val prompt = """
            Generate tailored engaging, highly spiritual titles, descriptions, and hashtags/tags in Arabic for sharing an Islamic video on social media.
            Video Context Details:
            - Quran Surah: $surahName
            - Ayah Range: $startAyah to $endAyah
            - Reciter Voice: $reciterName
            
            We are publishing this video reel to the following target platforms:
            TikTok: $isTiktok
            Instagram: $isInstagram
            Facebook: $isFacebook
            YouTube Shorts: $isYoutube
            
            Format your final response strictly as a single JSON object containing keys "tiktok", "instagram", "facebook", and "youtube" (only include key if platform is true).
            Each platform's value should be an object containing:
            1. "title": A catchy spiritually-moving title optimized for that platform.
            2. "description": A highly engaging description matching that platform's character limits & vibe, decorated with fitting emojis, encouraging viewers to listen and ponder.
            3. "hashtags": A space-separated list of highly professional, relevant, trending hashtags like #quran #قرآن #تلاوة_خاشعة #قران_كريم #reels #shorts etc.
            
            Respond with ONLY the raw JSON string, never surround it in markdown notation or code blocks.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val countArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", countArray)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.75)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
            
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val rootJson = JSONObject(responseStr)
                val candidates = rootJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    if (parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).getString("text").trim()
                        
                        // Parse JSON output from Gemini
                        val cleanText = if (rawText.startsWith("```json")) {
                            rawText.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (rawText.startsWith("```")) {
                            rawText.substringAfter("```").substringBeforeLast("```").trim()
                        } else {
                            rawText
                        }
                        
                        val metaJson = JSONObject(cleanText)
                        
                        val tiktokMeta = if (isTiktok && metaJson.has("tiktok")) {
                            val obj = metaJson.getJSONObject("tiktok")
                            PlatformMeta(obj.getString("title"), obj.getString("description"), obj.getString("hashtags"))
                        } else null
                        
                        val instagramMeta = if (isInstagram && metaJson.has("instagram")) {
                            val obj = metaJson.getJSONObject("instagram")
                            PlatformMeta(obj.getString("title"), obj.getString("description"), obj.getString("hashtags"))
                        } else null

                        val facebookMeta = if (isFacebook && metaJson.has("facebook")) {
                            val obj = metaJson.getJSONObject("facebook")
                            PlatformMeta(obj.getString("title"), obj.getString("description"), obj.getString("hashtags"))
                        } else null

                        val youtubeMeta = if (isYoutube && metaJson.has("youtube")) {
                            val obj = metaJson.getJSONObject("youtube")
                            PlatformMeta(obj.getString("title"), obj.getString("description"), obj.getString("hashtags"))
                        } else null
                        
                        return@withContext GeneratedMetaResult(tiktokMeta, instagramMeta, facebookMeta, youtubeMeta)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
