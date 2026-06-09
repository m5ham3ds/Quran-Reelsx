package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
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

data class VerseData(val text: String, val translation: String?, val audioPath: String, val durationUs: Long)

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
        onProgress: (String, Float) -> Unit,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        threadError = null
        var videoCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var bgBitmap: Bitmap? = null
        
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
            
            val translationFontSize = settingsManager.translationFontSize.first()
            val translationColorStr = settingsManager.translationColor.first()
            val pixabayApiKey = settingsManager.pixabayApiKey.first()
            
            // 2. Fetch Background Image if Pexels or Pixabay key is provided
            var imageLoaded = false
            val bgFile = File(context.cacheDir, "bg_image.jpg")
            
            if (pexelsApiKey.isNotBlank()) {
                onProgress(if (isArabic) "جاري تحميل الخلفية السينمائية (Pexels)..." else "Downloading background (Pexels)...", 0.05f)
                try {
                    val request = Request.Builder()
                        .url("https://api.pexels.com/v1/search?query=scenic+night+stars+minimalist&orientation=portrait&per_page=15")
                        .addHeader("Authorization", pexelsApiKey)
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val photos = json.getJSONArray("photos")
                        if (photos.length() > 0) {
                            val randomPhoto = photos.getJSONObject((0 until photos.length()).random())
                            val imgUrl = randomPhoto.getJSONObject("src").getString("large2x")
                            downloadAudio(imgUrl, bgFile)
                            bgBitmap = android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath)
                            imageLoaded = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (!imageLoaded && pixabayApiKey.isNotBlank()) {
                onProgress(if (isArabic) "جاري تحميل الخلفية السينمائية (Pixabay)..." else "Downloading background (Pixabay)...", 0.05f)
                try {
                    val request = Request.Builder()
                        .url("https://pixabay.com/api/?key=$pixabayApiKey&q=stars+night+scenic+minimalist&image_type=photo&orientation=vertical&per_page=15")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val hits = json.getJSONArray("hits")
                        if (hits.length() > 0) {
                            val randomPhoto = hits.getJSONObject((0 until hits.length()).random())
                            val imgUrl = randomPhoto.getString("largeImageURL")
                            downloadAudio(imgUrl, bgFile)
                            bgBitmap = android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath)
                            imageLoaded = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 3. Download translation & audio files, then transcode to AAC/M4A for 100% video muxing compatibility
            for (i in 0 until totalAyahs) {
                val ayah = startAyah + i
                onProgress(if (isArabic) "جاري تحميل الآية $ayah وحفظ مراجع الصوت..." else "Downloading reference audio for Ayah $ayah...", 0.1f + (i * 0.4f / totalAyahs))
                
                val verseInfo = fetchVerseInfo(surah, ayah, "quran-uthmani")
                val text = verseInfo.first
                val globalAyahNumber = verseInfo.second
                val translation = if (showTranslation) fetchVerseInfo(surah, ayah, "en.asad").first else null

                val audioFileName = "${reciterId}_${surah}_${ayah}.mp3"
                val url = "https://cdn.islamic.network/quran/audio/64/$reciterId/$globalAyahNumber.mp3"
                val destFile = File(context.cacheDir, audioFileName)
                
                downloadAudio(url, destFile)
                
                onProgress(if (isArabic) "جاري ترميز ملف الصوت بدقة سينمائية..." else "Encoding audio block dynamically...", 0.15f + (i * 0.4f / totalAyahs))
                val aacFileName = "${reciterId}_${surah}_${ayah}_transcoded.m4a"
                val aacFile = File(context.cacheDir, aacFileName)
                transcodeMp3ToAac(destFile.absolutePath, aacFile.absolutePath)
                
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
                verses.add(VerseData(text, translation, aacFile.absolutePath, durationUs))
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
                onProgress(if (isArabic) "جاري تصوير مشهد الآية ${startAyah + idx}..." else "Rendering scenes for Ayah ${startAyah + idx}...", 0.5f + (idx * 0.4f / verses.size))
                
                val bitmap = createVerseBitmap(
                    text = verse.text,
                    translation = verse.translation,
                    bgBitmap = bgBitmap,
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
                    translationFontSize = translationFontSize,
                    translationColorStr = translationColorStr
                )
                
                val framesNeeded = Math.max(1, (verse.durationUs / frameDurationUs).toInt() + 1)
                
                for (i in 0 until framesNeeded) {
                    if (threadError != null) {
                        throw Exception("خطأ في قنوات المعالجة الخلفية: ${threadError?.localizedMessage}")
                    }
                    
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
                }
                bitmap.recycle()
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
                    
                    uri = Uri.fromFile(targetFile)
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

    private fun transcodeMp3ToAac(inputPath: String, outputPath: String) {
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
                    
                    var encInIdx = -1
                    while (encInIdx < 0 && !isDecoderEOS) {
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
        translationFontSize: Int,
        translationColorStr: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. Draw Background
        if (bgBitmap != null) {
            val src = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dst = android.graphics.Rect(0, 0, 720, 1280)
            canvas.drawBitmap(bgBitmap, src, dst, null)
            canvas.drawColor(Color.argb(140, 0, 0, 0))
        } else {
            canvas.drawColor(Color.parseColor("#0F0F14"))
        }
        
        // 2. Typeface config
        val tf = when (fontFamily) {
            "Amiri" -> Typeface.create("serif", Typeface.NORMAL)
            "Cairo" -> Typeface.create("sans-serif", Typeface.NORMAL)
            "Monospace" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT_BOLD
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
            textAlign = Paint.Align.CENTER
            typeface = tf
            this.textSize = textFontSize.toFloat() * 1.8f
            setShadowLayer(8f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        
        val sl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 620)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.4f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, 620, Layout.Alignment.ALIGN_CENTER, 1.4f, 0f, false)
        }
        
        // 3. Translation Paint
        val transColor = try {
            Color.parseColor(translationColorStr)
        } catch (e: Exception) {
            Color.parseColor("#E0E0E0")
        }
        
        val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = transColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            this.textSize = translationFontSize.toFloat() * 1.8f
            setShadowLayer(8f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        
        val transSl: StaticLayout? = if (translation != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(translation, 0, translation.length, transPaint, 620)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(translation, transPaint, 620, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
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
        canvas.translate(360f, startY)
        sl.draw(canvas)
        canvas.restore()
        
        // 6. Draw translation
        if (transSl != null) {
            canvas.save()
            canvas.translate(360f, startY + sl.height + 60f)
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
