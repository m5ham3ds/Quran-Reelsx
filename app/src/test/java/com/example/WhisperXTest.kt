package com.example

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@RunWith(RobolectricTestRunner::class)
class WhisperXTest {
    @Test
    fun fetchApiSpec() {
        val client = OkHttpClient()
        val uploadRequest = Request.Builder()
            .url("https://qalam249-whisperx.hf.space/gradio_api/upload")
            // No file upload testing in junit for now, let's just test the /align_audio with fake path?
            // Actually, we can just test if the endpoint returns missing params if we hit /align_audio with no audio
            .build()
        
        val fileObject = org.json.JSONObject().apply {
            put("path", "fake/path")
            put("meta", org.json.JSONObject().apply {
                put("_type", "gradio.FileData")
            })
        }
        val alignPayload = org.json.JSONObject().apply {
            put("data", org.json.JSONArray().apply {
                put(fileObject)
                put("Hello!!")
            })
        }

        val jsonMediaType = "application/json".toMediaTypeOrNull()
        val alignRequest = Request.Builder()
            .url("https://qalam249-whisperx.hf.space/gradio_api/call/align_audio")
            .post(alignPayload.toString().toRequestBody(jsonMediaType))
            .build()
        val alignResponse = client.newCall(alignRequest).execute()
        java.io.File("/tmp/whisperx.txt").writeText(alignResponse.body?.string() ?: "Empty")
    }
}
