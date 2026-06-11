package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class VerseData(
    val text: String,
    val translation: String?,
    val audioPath: String,
    val durationUs: Long,
    val energyTimeline: List<Pair<Long, Float>>
)

class VideoGenerator {

    private val client = OkHttpClient()
    
    @Volatile 
    private var threadError: Throwable? = null

    suspend fun generateReel(
        context: Context,
        surah: Int,
        startAyah: Int,
        endAyah: Int,
        reciterId: String,
        showTranslation: Boolean,
        pexelsApiKey: String,
        isRetry: Boolean = false,
        onProgress: (String, Float) -> Unit,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        threadError = null
        var videoCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var bgBitmap: Bitmap? = null
        var retriever: MediaMetadataRetriever? = null
        
        try {
            val verses = mutableListOf<VerseData>()
            val totalAyahs = endAyah - startAyah + 1
            
            // 1. Fetch text & style configurations
            val settingsManager = SettingsManager(context)
            val language = settingsManager.language.first()
            val isArabic = language == "ar"
            
            val fontFamily = settingsManager.fontFamily.first()
            val textFontSize = settingsManager.fontSize.first()
            val textColorStr = settingsManager.textColor.first()
            val textOpacity = settingsManager.textOpacity.first()
            
            val showTextBg = settingsManager.showTextBackground.first()
            val textBgColorStr = settingsManager.textBgColor.first()
            val textBgOpacity = settingsManager.textBgOpacity.first()
            val textBgRadius = settingsManager.textBgRadius.first()
            
            val textPosition = settingsManager.textPosition.first()
            val textAlign = settingsManager.textAlign.first()
            
            val translationFontSize = settingsManager.translationFontSize.first()
            val translationColorStr = settingsManager.translationColor.first()
            val pixabayApiKey = settingsManager.pixabayApiKey.first()
            
            // 2. Download translation & audio files, then transcode to AAC/M4A for 100% video muxing compatibility
            for (i in 0 until totalAyahs) {
                val ayah = startAyah + i
                onProgress(if (isArabic) "جاري تحميل الآية $ayah وحفظ مراجع الصوت..." else "Downloading reference audio for Ayah $ayah...", 0.05f + (i * 0.2f / totalAyahs))
                
                val verseInfo = fetchVerseInfo(surah, ayah, "quran-uthmani")
                val text = verseInfo.first
                val globalAyahNumber = verseInfo.second
                val translation = if (showTranslation) fetchVerseInfo(surah, ayah, "en.asad").first else null

                val audioFileName = "${reciterId}_${surah}_${ayah}.mp3"
                val url = "https://cdn.islamic.network/quran/audio/64/$reciterId/$globalAyahNumber.mp3"
                val destFile = File(context.cacheDir, audioFileName)
                
                downloadAudio(url, destFile)
                
                onProgress(if (isArabic) "جاري ترميز ملف الصوت بدقة سينمائية..." else "Encoding audio block dynamically...", 0.08f + (i * 0.2f / totalAyahs))
                val aacFileName = "${reciterId}_${surah}_${ayah}_transcoded.m4a"
                val aacFile = File(context.cacheDir, aacFileName)
                val timeline = transcodeMp3ToAac(destFile.absolutePath, aacFile.absolutePath)
                
                val ext = MediaExtractor().apply { setDataSource(aacFile.absolutePath) }
                ext.selectTrack(0)
                var durationUs = ext.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION, -1L)
                if (durationUs <= 0) {
                    var maxTs = 0L
                    val bb = ByteBuffer.allocate(256)
                    while (ext.readSampleData(bb, 0) >= 0) {
                        maxTs = ext.sampleTime
                        ext.advance()
                    }
                    durationUs = maxTs
                }
                ext.release()
                verses.add(VerseData(text, translation, aacFile.absolutePath, durationUs, timeline))
            }
            
            // 3. Fetch Cinematic Background Portrait Video clip if Pexels or Pixabay API key is provided
            var videoLoaded = false
            val downloadedVideoFiles = mutableListOf<File>()
            
            if (!isRetry) {
                try {
                    val files = context.cacheDir.listFiles()
                    files?.forEach { f ->
                        if (f.name.startsWith("bg_video_") && f.name.endsWith(".mp4")) {
                            f.delete()
                        }
                    }
                } catch (ex: Exception) {}
            }
            
            if (pexelsApiKey.isNotBlank()) {
                onProgress(if (isArabic) "جاري البحث عن مشاهد سينمائية سريعة (Pexels)..." else "Searching for dynamic fast-paced cinematic scenes (Pexels)...", 0.3f)
                try {
                    val pexelsQueries = listOf(
                        "cinematic+drone+fast+flight+nature",
                        "speed+drone+flyby+waterfall",
                        "dynamic+fpv+drone+mountains",
                        "epic+aerial+coastline+waves+motion",
                        "cinematic+sunset+landscape+timelapse",
                        "clouds+hyperlapse+epic+mountain"
                    )
                    val chosenQuery = pexelsQueries.random()
                    val requestUrl = "https://api.pexels.com/videos/search?query=$chosenQuery&orientation=portrait&per_page=30"
                    val request = Request.Builder()
                        .url(requestUrl)
                        .addHeader("Authorization", pexelsApiKey)
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val videos = json.getJSONArray("videos")
                        if (videos.length() > 0) {
                            val availableVideosList = mutableListOf<JSONObject>()
                            for (vIdx in 0 until videos.length()) {
                                availableVideosList.add(videos.getJSONObject(vIdx))
                            }
                            
                            for (vidIdx in 0 until totalAyahs) {
                                val verse = verses[vidIdx]
                                val neededDurSec = (verse.durationUs / 1000000L).toInt() + 2
                                
                                // Try to find a video with duration >= neededDurSec, otherwise find the longest video
                                var selectedVideoJson = availableVideosList.filter {
                                    it.optInt("duration", 0) >= neededDurSec
                                }.minByOrNull { it.optInt("duration", 0) }
                                
                                if (selectedVideoJson == null) {
                                    selectedVideoJson = availableVideosList.maxByOrNull { it.optInt("duration", 0) }
                                }
                                
                                if (selectedVideoJson != null) {
                                    // Remove selected video from lists to maintain variety
                                    if (availableVideosList.size > 1) {
                                        availableVideosList.remove(selectedVideoJson)
                                    }
                                    
                                    val videoFiles = selectedVideoJson.getJSONArray("video_files")
                                    var selectedVideoUrl: String? = null
                                    for (v in 0 until videoFiles.length()) {
                                        val fileObj = videoFiles.getJSONObject(v)
                                        val link = fileObj.getString("link")
                                        val width = fileObj.optInt("width", 0)
                                        val height = fileObj.optInt("height", 0)
                                        if (width < height && link.contains("mp4", ignoreCase = true)) {
                                            selectedVideoUrl = link
                                            break
                                        }
                                    }
                                    if (selectedVideoUrl == null && videoFiles.length() > 0) {
                                        for (v in 0 until videoFiles.length()) {
                                            val fileObj = videoFiles.getJSONObject(v)
                                            val link = fileObj.getString("link")
                                            if (link.contains("mp4", ignoreCase = true)) {
                                                selectedVideoUrl = link
                                                break
                                            }
                                        }
                                    }
                                    if (selectedVideoUrl == null && videoFiles.length() > 0) {
                                        selectedVideoUrl = videoFiles.getJSONObject(0).getString("link")
                                    }
                                    
                                    if (selectedVideoUrl != null) {
                                        onProgress(
                                            if (isArabic) "جاري تحميل مشهد متناسق للمقطع ${vidIdx + 1} من $totalAyahs..." else "Downloading duration-matched scene ${vidIdx + 1} of $totalAyahs...",
                                            0.35f + (vidIdx * 0.15f / totalAyahs)
                                        )
                                        val targetFile = File(context.cacheDir, "bg_video_$vidIdx.mp4")
                                        downloadAudio(selectedVideoUrl, targetFile)
                                        downloadedVideoFiles.add(targetFile)
                                    }
                                }
                            }
                            if (downloadedVideoFiles.isNotEmpty()) {
                                videoLoaded = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (!videoLoaded && pixabayApiKey.isNotBlank()) {
                onProgress(if (isArabic) "جاري البحث عن مناظر طبيعية هادئة سريعة (Pixabay)..." else "Searching for active nature landscapes (Pixabay)...", 0.3f)
                try {
                    val pixabayQueries = listOf(
                        "fpv+drone+nature+fast",
                        "timelapse+clouds+mountains",
                        "cinematic+waterfall+rapid+aerial",
                        "epic+mountain+drone+flyover",
                        "ocean+waves+stormy+aerial"
                    )
                    val chosenPixabayQuery = pixabayQueries.random()
                    val request = Request.Builder()
                        .url("https://pixabay.com/api/videos/?key=$pixabayApiKey&q=$chosenPixabayQuery&orientation=vertical&per_page=30")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val hits = json.getJSONArray("hits")
                        if (hits.length() > 0) {
                            val availableHitsList = mutableListOf<JSONObject>()
                            for (hIdx in 0 until hits.length()) {
                                availableHitsList.add(hits.getJSONObject(hIdx))
                            }
                            
                            for (vidIdx in 0 until totalAyahs) {
                                val verse = verses[vidIdx]
                                val neededDurSec = (verse.durationUs / 1000000L).toInt() + 2
                                
                                var selectedHit = availableHitsList.filter {
                                    it.optInt("duration", 0) >= neededDurSec
                                }.minByOrNull { it.optInt("duration", 0) }
                                
                                if (selectedHit == null) {
                                    selectedHit = availableHitsList.maxByOrNull { it.optInt("duration", 0) }
                                }
                                
                                if (selectedHit != null) {
                                    if (availableHitsList.size > 1) {
                                        availableHitsList.remove(selectedHit)
                                    }
                                    
                                    val videosObj = selectedHit.getJSONObject("videos")
                                    val sizeKeys = listOf("medium", "small", "large", "tiny")
                                    var selectedVideoUrl: String? = null
                                    for (key in sizeKeys) {
                                        if (videosObj.has(key)) {
                                            val vObj = videosObj.getJSONObject(key)
                                            val url = vObj.getString("url")
                                            if (url.isNotBlank()) {
                                                selectedVideoUrl = url
                                                break
                                            }
                                        }
                                    }
                                    if (selectedVideoUrl != null) {
                                        onProgress(
                                            if (isArabic) "جاري تحميل مشهد متناسق للمقطع ${vidIdx + 1} من $totalAyahs..." else "Downloading duration-matched scene ${vidIdx + 1} of $totalAyahs...",
                                            0.35f + (vidIdx * 0.15f / totalAyahs)
                                        )
                                        val targetFile = File(context.cacheDir, "bg_video_$vidIdx.mp4")
                                        downloadAudio(selectedVideoUrl, targetFile)
                                        downloadedVideoFiles.add(targetFile)
                                    }
                                }
                            }
                            if (downloadedVideoFiles.isNotEmpty()) {
                                videoLoaded = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Fallback to high-quality direct public video CDN loop URLs so we NEVER show a blank background or static image
            if (!videoLoaded) {
                onProgress(if (isArabic) "جاري تحميل مشاهد طبيعية متحركة عالية الجودة..." else "Downloading premium cinematic video loops...", 0.3f)
                val directUrls = listOf(
                    "https://assets.mixkit.co/videos/preview/mixkit-vertical-shot-of-a-beautiful-waterfall-in-a-forest-43756-large.mp4",
                    "https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-vertical-shot-44445-large.mp4",
                    "https://assets.mixkit.co/videos/preview/mixkit-waves-crashing-on-a-sandy-beach-from-above-41793-large.mp4",
                    "https://assets.mixkit.co/videos/preview/mixkit-vertical-shot-of-the-sea-under-a-clear-sky-40767-large.mp4",
                    "https://assets.mixkit.co/videos/preview/mixkit-light-rain-falling-on-green-leaves-vertical-shot-42022-large.mp4"
                )
                val countToLoad = Math.min(totalAyahs, directUrls.size)
                for (vidIdx in 0 until countToLoad) {
                    try {
                        onProgress(
                            if (isArabic) "جاري تحميل مشهد سينمائي عالي الجودة ${vidIdx + 1} من $countToLoad..." else "Loading cinematic nature loop ${vidIdx + 1} of $countToLoad...",
                            0.35f + (vidIdx * 0.15f / countToLoad)
                        )
                        val targetFile = File(context.cacheDir, "bg_video_$vidIdx.mp4")
                        downloadAudio(directUrls[vidIdx], targetFile)
                        if (targetFile.exists() && targetFile.length() > 0) {
                            downloadedVideoFiles.add(targetFile)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (downloadedVideoFiles.isNotEmpty()) {
                    videoLoaded = true
                }
            }
            
            onProgress(if (isArabic) "جاري تهيئة معالجات المقطع..." else "Initializing video filters...", 0.5f)
            
            if (verses.isEmpty()) throw Exception("لا توجد آيات صالحة لعمل المقطع")
            
            val outputPath = File(context.cacheDir, "quran_reel_${System.currentTimeMillis()}.mp4").absolutePath
            val finalMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = finalMuxer
            
            var videoTrackIdx = -1
            var audioTrackIdx = -1
            val muxerStarted = java.util.concurrent.atomic.AtomicBoolean(false)
            
            val audioFormat = MediaExtractor().apply { setDataSource(verses[0].audioPath) }.apply { selectTrack(0) }.getTrackFormat(0)
            
            val videoFormat = MediaFormat.createVideoFormat("video/avc", 720, 1280).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            
            val encoder = MediaCodec.createEncoderByType("video/avc")
            videoCodec = encoder
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            
            val drainLatch = CountDownLatch(1)
            
            val drainThread = thread {
                try {
                    val bufferInfo = MediaCodec.BufferInfo()
                    while (threadError == null) {
                        val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            val vf = encoder.outputFormat
                            
                            // Build a clean audio format container containing only keys supported by MediaMuxer
                            val cleanAudioFormat = MediaFormat.createAudioFormat(
                                audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm",
                                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            )
                            if (audioFormat.containsKey("csd-0")) {
                                cleanAudioFormat.setByteBuffer("csd-0", audioFormat.getByteBuffer("csd-0")!!)
                            }
                            if (audioFormat.containsKey("csd-1")) {
                                cleanAudioFormat.setByteBuffer("csd-1", audioFormat.getByteBuffer("csd-1")!!)
                            }

                            videoTrackIdx = finalMuxer.addTrack(vf)
                            audioTrackIdx = finalMuxer.addTrack(cleanAudioFormat)
                            finalMuxer.start()
                            muxerStarted.set(true)
                        } else if (outIdx >= 0) {
                            val buf = encoder.getOutputBuffer(outIdx)!!
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted.get()) {
                                buf.position(bufferInfo.offset)
                                buf.limit(bufferInfo.offset + bufferInfo.size)
                                synchronized(finalMuxer) {
                                    finalMuxer.writeSampleData(videoTrackIdx, buf, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    threadError = e
                    e.printStackTrace()
                } finally {
                    drainLatch.countDown()
                }
            }
            
            val audioThread = thread {
                try {
                    var audioPtsUs = 0L
                    for (verse in verses) {
                        if (threadError != null) break
                        val ext = MediaExtractor().apply { setDataSource(verse.audioPath) }
                        ext.selectTrack(0)
                        val buf = ByteBuffer.allocate(1024 * 1024)
                        val info = MediaCodec.BufferInfo()
                        while (threadError == null) {
                            val size = ext.readSampleData(buf, 0)
                            if (size < 0) break
                            val pts = ext.sampleTime
                            info.offset = 0
                            info.size = size
                            info.flags = if ((ext.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else 0
                            info.presentationTimeUs = audioPtsUs + pts
                            
                            while (!muxerStarted.get() && drainLatch.count > 0 && threadError == null) {
                                Thread.sleep(10)
                            }
                            if (threadError != null) break
                            if (muxerStarted.get()) {
                                synchronized(finalMuxer) {
                                    finalMuxer.writeSampleData(audioTrackIdx, buf, info)
                                }
                            }
                            ext.advance()
                        }
                        audioPtsUs += verse.durationUs
                        ext.release()
                    }
                } catch (e: Exception) {
                    threadError = e
                    e.printStackTrace()
                }
            }
            
            var videoPtsUs = 0L
            val fps = 15
            val frameDurationUs = 1000000L / fps
            
            for ((idx, verse) in verses.withIndex()) {
                onProgress(if (isArabic) "جاري تصوير مشهدي الآية ${startAyah + idx}..." else "Rendering scenes for Ayah ${startAyah + idx}...", 0.5f + (idx * 0.4f / verses.size))
                
                var frameDecoder: SequentialFrameDecoder? = null
                if (videoLoaded && downloadedVideoFiles.isNotEmpty()) {
                    try {
                        val videoFile = downloadedVideoFiles[idx % downloadedVideoFiles.size]
                        if (videoFile.exists()) {
                            frameDecoder = SequentialFrameDecoder(videoFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                val framesNeeded = Math.max(1, (verse.durationUs / frameDurationUs).toInt() + 1)
                
                for (i in 0 until framesNeeded) {
                    checkCancellationAndPause()
                    if (threadError != null) {
                        throw Exception("خطأ في قنوات المعالجة الخلفية: ${threadError?.localizedMessage}")
                    }
                    
                    var bgFrameBitmap: Bitmap? = null
                    if (frameDecoder != null) {
                        try {
                            bgFrameBitmap = frameDecoder.getNextFrame()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val frameIndex = videoPtsUs / frameDurationUs
                    val chunkedText = getActiveTextChunk(verse.text, i, framesNeeded, verse.durationUs, verse.energyTimeline)
                    val chunkedTranslation = getActiveTranslationChunk(verse.translation, verse.text, i, framesNeeded, verse.durationUs, verse.energyTimeline)
                    
                    val bitmap = createVerseBitmap(
                        text = chunkedText,
                        translation = chunkedTranslation,
                        bgBitmap = bgFrameBitmap,
                        context = context,
                        fontFamily = fontFamily,
                        textFontSize = textFontSize,
                        textColorStr = textColorStr,
                        textOpacity = textOpacity,
                        showTextBg = showTextBg,
                        textBgColorStr = textBgColorStr,
                        textBgOpacity = textBgOpacity,
                        textBgRadius = textBgRadius,
                        textPosition = textPosition,
                        textAlign = textAlign,
                        translationFontSize = translationFontSize,
                        translationColorStr = translationColorStr,
                        frameIndex = frameIndex
                    )
                    
                    var inIdx = -1
                    while (inIdx < 0) {
                        if (threadError != null) {
                            throw Exception("خطأ في قنوات المعالجة الخلفية: ${threadError?.localizedMessage}")
                        }
                        inIdx = encoder.dequeueInputBuffer(50000)
                    }
                    
                    val img = encoder.getInputImage(inIdx)!!
                    fillImageFromBitmap(img, bitmap)
                    encoder.queueInputBuffer(inIdx, 0, img.planes[0].buffer.capacity() * 3/2, videoPtsUs, 0)
                    videoPtsUs += frameDurationUs
                    
                    bitmap.recycle()
                    bgFrameBitmap?.recycle()
                }
                
                try { frameDecoder?.release() } catch (ex: Exception) {}
            }
            
            var eosIdx = -1
            while (eosIdx < 0) {
                if (threadError != null) {
                    throw Exception("خطأ في قنوات المعالجة الخلفية: ${threadError?.localizedMessage}")
                }
                eosIdx = encoder.dequeueInputBuffer(50000)
            }
            encoder.queueInputBuffer(eosIdx, 0, 0, videoPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            
            val drainCompleted = drainLatch.await(5, TimeUnit.MINUTES)
            if (!drainCompleted) {
                throw Exception("توقيت معالجة الفيديو انتهى دون استجابة الترميز")
            }
            audioThread.join(10000)
            
            if (threadError != null) {
                throw Exception("فشلت معالجة مقطع الفيديو: ${threadError?.localizedMessage}")
            }
            
            finalMuxer.stop()
            finalMuxer.release()
            muxer = null
            
            encoder.stop()
            encoder.release()
            videoCodec = null
            
            bgBitmap?.recycle()
            bgBitmap = null
            
            onProgress(if (isArabic) "جاري تصدير المقطع وحفظه بالاستوديو..." else "Exporting video and registering in Gallery...", 0.95f)
            
            var uri: Uri? = null
            
            // 1. Direct custom directory save attempt as requested: /storage/emulated/0/Quran Reels
            try {
                val customDir = File("/storage/emulated/0/Quran Reels")
                if (!customDir.exists()) {
                    customDir.mkdirs()
                }
                if (customDir.exists()) {
                    val targetFile = File(customDir, "Quran_Reel_${System.currentTimeMillis()}.mp4")
                    File(outputPath).copyTo(targetFile, overwrite = true)
                    
                    // Crucial: Scan file so it is indexed in the system media database & instantly visible in standard players/gallery!
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                    
                    // Create an internal playable file that ExoPlayer can always read without permission
                    val playableFile = File(context.cacheDir, "playable_reel.mp4")
                    File(outputPath).copyTo(playableFile, overwrite = true)
                    uri = Uri.fromFile(playableFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 2. Fallback to standard MediaStore registration if Scoped Storage blocks raw file creation (this is 100% reliable on Android 10+ and places it in Movies directory)
            if (uri == null) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "Quran_Reel_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Quran Reels")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    val mUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    if (mUri != null) {
                        context.contentResolver.openOutputStream(mUri)?.use { out ->
                            File(outputPath).inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Video.Media.IS_PENDING, 0)
                            context.contentResolver.update(mUri, values, null, null)
                        }
                        uri = mUri
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 3. Absolute Fallback to Internal Cache to prevent failures (guarantees we always have a valid URI)
            if (uri == null) {
                uri = Uri.fromFile(File(outputPath))
            } else {
                // Delete temporary internal file after successful copy
                try { File(outputPath).delete() } catch (ex: Exception) {}
            }
            
            val finalUri = uri
            if (finalUri != null) {
                withContext(Dispatchers.Main) { onComplete(finalUri) }
            } else {
                throw Exception("لم نتمكن من حفظ المقطع في المعرض.")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (ex: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ex: Exception) {}
            bgBitmap?.recycle()
            try { retriever?.release() } catch (ex: Exception) {}
            
            val errorMsg = e.message ?: "حدث خطأ غير معروف في صانع المقطع"
            withContext(Dispatchers.Main) { onError(errorMsg) }
        }
    }

    private fun fetchVerseInfo(surah: Int, ayah: Int, edition: String): Pair<String, Int> {
        val url = "https://api.alquran.cloud/v1/ayah/$surah:$ayah/$edition"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل تحميل نصوص الآيات من الخادم")
        val body = response.body?.string() ?: ""
        val json = JSONObject(body)
        val data = json.getJSONObject("data")
        return Pair(data.getString("text"), data.getInt("number"))
    }

    private fun downloadAudio(url: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0) return
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل تحميل الملفات الصوتية المحددة")
        response.body?.byteStream()?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun checkCancellationAndPause() {
        if (com.example.service.VideoGenerationService.isCancelled) {
            throw kotlinx.coroutines.CancellationException("تم إلغاء عملية إنتاج الفيديو")
        }
        if (com.example.service.VideoGenerationService.isPaused) {
            synchronized(com.example.service.VideoGenerationService.pauseLock) {
                while (com.example.service.VideoGenerationService.isPaused && !com.example.service.VideoGenerationService.isCancelled) {
                    try {
                        com.example.service.VideoGenerationService.pauseLock.wait(100)
                    } catch (e: Exception) {}
                }
            }
            if (com.example.service.VideoGenerationService.isCancelled) {
                throw kotlinx.coroutines.CancellationException("تم إلغاء عملية إنتاج الفيديو")
            }
        }
    }

    private fun transcodeMp3ToAac(inputPath: String, outputPath: String): List<Pair<Long, Float>> {
        val rawEnergySamples = mutableListOf<Pair<Long, Float>>()
        val extractor = MediaExtractor().apply { setDataSource(inputPath) }
        if (extractor.trackCount == 0) {
            extractor.release()
            throw Exception("ملف الصوت فارغ أو غير صالح للاستخدام")
        }
        extractor.selectTrack(0)
        val inputFormat = extractor.getTrackFormat(0)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"
        
        // 1. Setup Decoder
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()
        
        // 2. Setup Encoder (AAC)
        val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        
        // 3. Setup Muxer
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var outTrackIdx = -1
        
        val decoderBufferInfo = MediaCodec.BufferInfo()
        val encoderBufferInfo = MediaCodec.BufferInfo()
        
        var isExtractorEOS = false
        var isDecoderEOS = false
        var isEncoderEOS = false
        
        val timeoutUs = 5000L
        var muxerStarted = false
        
        while (!isEncoderEOS) {
            checkCancellationAndPause()

            // A. Read from extractor and feed decoder
            if (!isExtractorEOS) {
                val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                if (inIdx >= 0) {
                    val buf = decoder.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorEOS = true
                    } else {
                        decoder.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            
            // B. Decode output into encoder input
            if (!isDecoderEOS) {
                val outIdx = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
                if (outIdx >= 0) {
                    val buf = decoder.getOutputBuffer(outIdx)!!
                    val size = decoderBufferInfo.size
                    
                    if (size > 0) {
                        // Amplitude Energy Analysis for precise timing
                        try {
                            val bufferDuplicate = buf.duplicate()
                            bufferDuplicate.position(decoderBufferInfo.offset)
                            bufferDuplicate.limit(decoderBufferInfo.offset + size)
                            
                            var sumOfAbs = 0L
                            var count = 0
                            while (bufferDuplicate.remaining() >= 2) {
                                val sample = bufferDuplicate.short
                                sumOfAbs += Math.abs(sample.toInt())
                                count++
                            }
                            val avgEnergy = if (count > 0) sumOfAbs.toFloat() / count else 0f
                            rawEnergySamples.add(Pair(decoderBufferInfo.presentationTimeUs, avgEnergy))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    var encInIdx = -1
                    while (encInIdx < 0 && !isDecoderEOS) {
                        checkCancellationAndPause()
                        encInIdx = encoder.dequeueInputBuffer(timeoutUs)
                        if (encInIdx < 0) {
                            // While waiting for an encoder input buffer, we should also drain the encoder output buffer to prevent a deadlock
                            val encOutIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
                            if (encOutIdx >= 0) {
                                val encBuf = encoder.getOutputBuffer(encOutIdx)!!
                                if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    encoderBufferInfo.size = 0
                                }
                                if (encoderBufferInfo.size > 0 && outTrackIdx >= 0) {
                                    encBuf.position(encoderBufferInfo.offset)
                                    encBuf.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                                    muxer.writeSampleData(outTrackIdx, encBuf, encoderBufferInfo)
                                }
                                if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    isEncoderEOS = true
                                }
                                encoder.releaseOutputBuffer(encOutIdx, false)
                            } else if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                outTrackIdx = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                    
                    if (encInIdx >= 0) {
                        val encBuf = encoder.getInputBuffer(encInIdx)!!
                        encBuf.clear()
                        if (size > 0) {
                            buf.position(decoderBufferInfo.offset)
                            buf.limit(decoderBufferInfo.offset + size)
                            encBuf.put(buf)
                        }
                        
                        val flags = if ((decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isDecoderEOS = true
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        } else 0
                        encoder.queueInputBuffer(encInIdx, 0, size, decoderBufferInfo.presentationTimeUs, flags)
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                }
            }
            
            // C. Encode output and write to muxer
            val encOutIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
            if (encOutIdx >= 0) {
                val buf = encoder.getOutputBuffer(encOutIdx)!!
                if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    encoderBufferInfo.size = 0
                }
                
                if (encoderBufferInfo.size > 0 && outTrackIdx >= 0) {
                    buf.position(encoderBufferInfo.offset)
                    buf.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                    muxer.writeSampleData(outTrackIdx, buf, encoderBufferInfo)
                }
                
                if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEncoderEOS = true
                }
                encoder.releaseOutputBuffer(encOutIdx, false)
            } else if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outTrackIdx = muxer.addTrack(encoder.outputFormat)
                muxer.start()
                muxerStarted = true
            }
        }
        
        // Cleanup all
        try { decoder.stop(); decoder.release() } catch (e: Exception) {}
        try { encoder.stop(); encoder.release() } catch (e: Exception) {}
        try { if (muxerStarted) muxer.stop(); muxer.release() } catch (e: Exception) {}
        try { extractor.release() } catch (e: Exception) {}

        // Smooth energy with noise-gate threshold - lowered to 0.02f for maximum syllables tracking
        val peak = rawEnergySamples.maxOfOrNull { it.second } ?: 1f
        val gateThreshold = peak * 0.02f
        return rawEnergySamples.map { (time, energy) ->
            val gatedEnergy = if (energy > gateThreshold) energy - gateThreshold else 0f
            Pair(time, gatedEnergy)
        }
    }

    private fun getCumulativeEnergyRatio(
        currentFrame: Int,
        totalFrames: Int,
        durationUs: Long,
        timeline: List<Pair<Long, Float>>
    ): Float {
        if (timeline.isEmpty() || totalFrames <= 1) {
            return currentFrame.toFloat() / totalFrames.toFloat()
        }
        
        // Find peak energy to determine noise gate threshold to identify active speech boundaries
        val peak = timeline.maxOfOrNull { it.second } ?: 1f
        val activeThreshold = peak * 0.04f // 4% threshold for active voice detection
        
        val firstActiveIdx = timeline.indexOfFirst { it.second > activeThreshold }.coerceAtLeast(0)
        val lastActiveIdx = timeline.indexOfLast { it.second > activeThreshold }.coerceAtLeast(0).coerceAtMost(timeline.size - 1)
        
        val startSpeechUs = timeline[firstActiveIdx].first
        val endSpeechUs = timeline[lastActiveIdx].first
        val activeDurationUs = endSpeechUs - startSpeechUs
        
        val currentTimeUs = (currentFrame.toFloat() / totalFrames.toFloat()) * durationUs
        
        // Before active speech has begun
        if (currentTimeUs < startSpeechUs) {
            return 0.0f
        }
        // After active speech has concluded
        if (currentTimeUs > endSpeechUs) {
            return 1.0f
        }
        
        if (activeDurationUs <= 0L) {
            return (currentTimeUs - startSpeechUs).toFloat() / Math.max(1f, durationUs.toFloat())
        }
        
        var totalEnergy = 0f
        var cumulativeEnergy = 0f
        
        for (sample in timeline) {
            if (sample.first >= startSpeechUs && sample.first <= endSpeechUs) {
                totalEnergy += sample.second
                if (sample.first <= currentTimeUs) {
                    cumulativeEnergy += sample.second
                }
            }
        }
        
        return if (totalEnergy > 0f) {
            cumulativeEnergy / totalEnergy
        } else {
            (currentTimeUs - startSpeechUs).toFloat() / activeDurationUs.toFloat()
        }
    }

    private fun getActiveTextChunk(
        text: String, 
        currentFrame: Int, 
        totalFrames: Int,
        durationUs: Long,
        timeline: List<Pair<Long, Float>>
    ): String {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= 5) return text // Short verse, show complete text
        
        // Group words into chunks of 4-5 words
        val chunkSize = if (words.size <= 8) 4 else 5
        val chunks = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val end = Math.min(i + chunkSize, words.size)
            chunks.add(words.subList(i, end).joinToString(" "))
            i += chunkSize
        }
        
        if (chunks.isEmpty()) return text
        
        // Use custom cumulative voice power to index words perfectly in sync with the speaker
        val ratio = getCumulativeEnergyRatio(currentFrame, totalFrames, durationUs, timeline)
        val chunkIdx = (ratio * chunks.size).toInt().coerceIn(0, chunks.size - 1)
        return chunks[chunkIdx]
    }

    private fun getActiveTranslationChunk(
        translation: String?, 
        text: String, 
        currentFrame: Int, 
        totalFrames: Int,
        durationUs: Long,
        timeline: List<Pair<Long, Float>>
    ): String? {
        if (translation == null) return null
        val wordsOrig = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (wordsOrig.size <= 5) return translation
        
        val transWords = translation.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (transWords.size <= 6) return translation
        
        // Calculate chunks of original text
        val chunkSize = if (wordsOrig.size <= 8) 4 else 5
        var quranChunksCount = 0
        var i = 0
        while (i < wordsOrig.size) {
            quranChunksCount++
            i += chunkSize
        }
        if (quranChunksCount <= 1) return translation
        
        // Proportional slicing of translation words into the exact same amount of pages
        val wordsPerTransChunk = Math.ceil(transWords.size.toDouble() / quranChunksCount.toDouble()).toInt().coerceAtLeast(1)
        val transChunks = mutableListOf<String>()
        var tIdx = 0
        while (tIdx < transWords.size) {
            val end = Math.min(tIdx + wordsPerTransChunk, transWords.size)
            transChunks.add(transWords.subList(tIdx, end).joinToString(" "))
            tIdx += wordsPerTransChunk
        }
        
        val ratio = getCumulativeEnergyRatio(currentFrame, totalFrames, durationUs, timeline)
        val chunkIdx = (ratio * quranChunksCount).toInt().coerceIn(0, quranChunksCount - 1)
        
        if (chunkIdx < transChunks.size) {
            return transChunks[chunkIdx]
        }
        return transChunks.lastOrNull() ?: translation
    }

    private fun createVerseBitmap(
        text: String,
        translation: String?,
        bgBitmap: Bitmap?,
        context: Context,
        fontFamily: String,
        textFontSize: Int,
        textColorStr: String,
        textOpacity: Float,
        showTextBg: Boolean,
        textBgColorStr: String,
        textBgOpacity: Float,
        textBgRadius: Int,
        textPosition: String,
        textAlign: String,
        translationFontSize: Int,
        translationColorStr: String,
        frameIndex: Long
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. Draw Background
        if (bgBitmap != null) {
            val src = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dst = android.graphics.Rect(0, 0, 720, 1280)
            canvas.drawBitmap(bgBitmap, src, dst, null)
            
            // Apply dual layers of premium dark mysterious gradient filters!
            // Layer A: Vertical dark-gold/violet linear shadow gradient
            val verticalGrad = android.graphics.LinearGradient(
                0f, 0f, 0f, 1280f,
                intArrayOf(Color.argb(210, 10, 14, 23), Color.argb(120, 20, 16, 26), Color.argb(240, 6, 8, 14)),
                null,
                Shader.TileMode.CLAMP
            )
            val verticalPaint = Paint().apply { shader = verticalGrad }
            canvas.drawRect(0f, 0f, 720f, 1280f, verticalPaint)

            // Layer B: Dramatic dark spotlight radial vignette centering the Quranic Verses
            val vignetteColors = intArrayOf(
                Color.argb(0, 0, 0, 0),        // Clear center
                Color.argb(120, 4, 3, 5),      // Soft shadow
                Color.argb(230, 2, 2, 3)       // Deep cosmic vignette edge
            )
            val vignetteOffsets = floatArrayOf(0.35f, 0.75f, 1.0f)
            val vignetteGrad = android.graphics.RadialGradient(
                360f, 640f, 760f,
                vignetteColors,
                vignetteOffsets,
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vignetteGrad }
            canvas.drawRect(0f, 0f, 720f, 1280f, vignettePaint)
        } else {
            // Draw a gorgeous dynamic animated gradient background!
            val grad = android.graphics.LinearGradient(
                0f, 0f, 720f, 1280f,
                intArrayOf(Color.parseColor("#07090E"), Color.parseColor("#150F18"), Color.parseColor("#0F0D16")),
                null,
                Shader.TileMode.CLAMP
            )
            val gradPaint = Paint().apply {
                shader = grad
            }
            canvas.drawRect(0f, 0f, 720f, 1280f, gradPaint)
            
            // Draw slow-drifting stellar particles for an elite aesthetic!
            val random = java.util.Random(42)
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }
            for (s in 0 until 40) {
                val baseX = random.nextFloat() * 720f
                val baseY = random.nextFloat() * 1280f
                val speed = 0.5f + random.nextFloat() * 1.5f
                
                // Drift based on frameIndex (loops visually since we restrict to 1280)
                val driftY = (baseY + frameIndex * speed) % 1280f
                val size = 1f + random.nextFloat() * 3f
                val baseAlpha = 50 + random.nextInt(155)
                val twinkle = (Math.sin((frameIndex * 0.1f + s).toDouble()) * 50).toInt()
                val finalAlpha = (baseAlpha + twinkle).coerceIn(0, 255)
                
                starPaint.alpha = finalAlpha
                canvas.drawCircle(baseX, driftY, size, starPaint)
            }
        }
        
        // 2. Typeface config
        val tf = when (fontFamily) {
            "Amiri" -> Typeface.create("serif", Typeface.BOLD)
            "Cairo" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "Monospace" -> Typeface.create("monospace", Typeface.BOLD)
            else -> Typeface.create("sans-serif-black", Typeface.NORMAL)
        }
        
        val tColor = try {
            Color.parseColor(textColorStr)
        } catch (e: Exception) {
            Color.WHITE
        }
        val alpha = (textOpacity * 255).toInt().coerceIn(0, 255)
        val finalTextColor = Color.argb(alpha, Color.red(tColor), Color.green(tColor), Color.blue(tColor))
        
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = finalTextColor
            this.textAlign = Paint.Align.LEFT
            typeface = tf
            this.textSize = textFontSize.toFloat() * 1.8f
            setShadowLayer(8f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        
        val layoutAlign = when (textAlign) {
            "Left" -> Layout.Alignment.ALIGN_NORMAL
            "Right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        
        val sl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 620)
                .setAlignment(layoutAlign)
                .setLineSpacing(0f, 1.4f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, 620, layoutAlign, 1.4f, 0f, false)
        }
        
        // 3. Translation Paint
        val transColor = try {
            Color.parseColor(translationColorStr)
        } catch (e: Exception) {
            Color.parseColor("#E0E0E0")
        }
        
        val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = transColor
            this.textAlign = Paint.Align.LEFT
            typeface = Typeface.create("serif", Typeface.ITALIC)
            this.textSize = translationFontSize.toFloat() * 1.8f
            setShadowLayer(8f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        
        val transSl: StaticLayout? = if (translation != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(translation, 0, translation.length, transPaint, 620)
                    .setAlignment(layoutAlign)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(translation, transPaint, 620, layoutAlign, 1f, 0f, false)
            }
        } else {
            null
        }
 
        val totalHeight = sl.height + (transSl?.height?.plus(60f) ?: 0f)
        
        val startY = when (textPosition) {
            "Top" -> 150f
            "Bottom" -> 1280f - totalHeight - 200f
            else -> (1280f - totalHeight) / 2f
        }
        
        // 4. Draw Background Box
        if (showTextBg) {
            val bgColor = try { Color.parseColor(textBgColorStr) } catch (e: Exception) { Color.BLACK }
            val bgAlpha = (textBgOpacity * 255).toInt().coerceIn(0, 255)
            val finalBgColor = Color.argb(bgAlpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = finalBgColor
                style = Paint.Style.FILL
            }
            
            val boxWidth = 660f
            val boxHeight = totalHeight + 84f
            val boxLeft = 360f - boxWidth / 2f
            val boxTop = startY - 42f
            val boxRight = boxLeft + boxWidth
            val boxBottom = boxTop + boxHeight
            
            val rect = android.graphics.RectF(boxLeft, boxTop, boxRight, boxBottom)
            val radius = textBgRadius.toFloat() * 1.5f
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
        }
        
        // 5. Draw Primary Text
        canvas.save()
        canvas.translate(50f, startY)
        sl.draw(canvas)
        canvas.restore()
        
        // 6. Draw translation with divider line
        if (transSl != null) {
            canvas.save()
            
            // Draw a beautiful elegant thin separator divider as in the App live preview!
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = transColor
                setAlpha(90)
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            val dividerY = startY + sl.height + 25f
            
            when (textAlign) {
                "Left" -> canvas.drawLine(50f, dividerY, 150f, dividerY, dividerPaint)
                "Right" -> canvas.drawLine(570f, dividerY, 670f, dividerY, dividerPaint)
                else -> canvas.drawLine(310f, dividerY, 410f, dividerY, dividerPaint)
            }
            
            canvas.translate(50f, startY + sl.height + 60f)
            transSl.draw(canvas)
            canvas.restore()
        }
        
        return bitmap
    }

    private fun fillImageFromBitmap(image: Image, bitmap: Bitmap) {
        val imgWidth = image.width
        val imgHeight = image.height
        
        val scaledBitmap = if (bitmap.width != imgWidth || bitmap.height != imgHeight) {
            Bitmap.createScaledBitmap(bitmap, imgWidth, imgHeight, true)
        } else {
            bitmap
        }
        
        val argb = IntArray(imgWidth * imgHeight)
        scaledBitmap.getPixels(argb, 0, imgWidth, 0, 0, imgWidth, imgHeight)
        
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        
        yBuffer.clear()
        uBuffer.clear()
        vBuffer.clear()
        
        val yBytes = ByteArray(imgWidth)
        var index = 0
        
        for (r in 0 until imgHeight) {
            for (c in 0 until imgWidth) {
                if (index >= argb.size) break
                val color = argb[index++]
                val rCol = (color and 0xff0000) shr 16
                val gCol = (color and 0xff00) shr 8
                val bCol = (color and 0xff) shr 0
                
                var Y = ((66 * rCol + 129 * gCol + 25 * bCol + 128) shr 8) + 16
                Y = Y.coerceIn(0, 255)
                yBytes[c] = Y.toByte()

                if (r % 2 == 0 && c % 2 == 0) {
                    var U = ((-38 * rCol - 74 * gCol + 112 * bCol + 128) shr 8) + 128
                    var V = ((112 * rCol - 94 * gCol - 18 * bCol + 128) shr 8) + 128
                    U = U.coerceIn(0, 255)
                    V = V.coerceIn(0, 255)
                    
                    val cHalf = c / 2
                    val uPos = (r / 2) * uRowStride + cHalf * uPixelStride
                    val vPos = (r / 2) * vRowStride + cHalf * vPixelStride
                    
                    if (uPos < uBuffer.capacity()) {
                        uBuffer.position(uPos)
                        uBuffer.put(U.toByte())
                    }
                    if (vPos < vBuffer.capacity()) {
                        vBuffer.position(vPos)
                        vBuffer.put(V.toByte())
                    }
                }
            }
            if (r * yRowStride + imgWidth <= yBuffer.capacity()) {
                yBuffer.position(r * yRowStride)
                yBuffer.put(yBytes)
            }
        }
    }
}

class SequentialFrameDecoder(private val videoPath: String) {
    private var extractor: MediaExtractor? = null
    private var decoder: MediaCodec? = null
    private var width = 720
    private var height = 1280
    private var trackIndex = -1
    private val bufferInfo = MediaCodec.BufferInfo()
    private var isEOS = false

    init {
        try {
            val ext = MediaExtractor()
            ext.setDataSource(videoPath)
            extractor = ext
            for (i in 0 until ext.trackCount) {
                val format = ext.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    ext.selectTrack(i)
                    trackIndex = i
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    
                    val dec = MediaCodec.createDecoderByType(mime)
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    dec.configure(format, null, null, 0)
                    dec.start()
                    decoder = dec
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            release()
        }
    }

    fun getNextFrame(): Bitmap? {
        val dec = decoder ?: return null
        val ext = extractor ?: return null
        if (trackIndex == -1) return null
        
        val timeoutUs = 5000L
        var attempts = 0
        while (attempts < 80) {
            attempts++
            try {
                if (!isEOS) {
                    val inIdx = dec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val buf = dec.getInputBuffer(inIdx)!!
                        val sampleSize = ext.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            dec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            dec.queueInputBuffer(inIdx, 0, sampleSize, ext.sampleTime, 0)
                            ext.advance()
                        }
                    }
                }

                val outIdx = dec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIdx >= 0) {
                    var bitmap: Bitmap? = null
                    try {
                        val image = dec.getOutputImage(outIdx)
                        if (image != null) {
                            bitmap = convertYUVImageToBitmap(image)
                            image.close()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    dec.releaseOutputBuffer(outIdx, false)
                    
                    if (bitmap != null) {
                        return bitmap
                    }
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val format = dec.outputFormat
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                } else if (isEOS && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    ext.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    isEOS = false
                    dec.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
        return null
    }

    private fun convertYUVImageToBitmap(image: Image): Bitmap {
        val w = image.width
        val h = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        
        var index = 0
        for (y in 0 until h) {
            val yRowStart = y * yRowStride
            for (x in 0 until w) {
                val yValue = (yBuffer.get(yRowStart + x).toInt() and 0xff)
                
                val uvIndex = (y / 2) * uRowStride + (x / 2) * uPixelStride
                val vIndex = (y / 2) * vRowStride + (x / 2) * vPixelStride
                
                val uValue = if (uvIndex < uBuffer.capacity()) (uBuffer.get(uvIndex).toInt() and 0xff) - 128 else 0
                val vValue = if (vIndex < vBuffer.capacity()) (vBuffer.get(vIndex).toInt() and 0xff) - 128 else 0
                
                var rCol = (yValue + 1.370705f * vValue).toInt()
                var gCol = (yValue - 0.337633f * uValue - 0.698001f * vValue).toInt()
                var bCol = (yValue + 1.732446f * uValue).toInt()
                
                rCol = rCol.coerceIn(0, 255)
                gCol = gCol.coerceIn(0, 255)
                bCol = bCol.coerceIn(0, 255)
                
                pixels[index++] = (0xff shl 24) or (rCol shl 16) or (gCol shl 8) or bCol
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun release() {
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {}
        decoder = null
        
        try {
            extractor?.release()
        } catch (e: Exception) {}
        extractor = null
    }
}

