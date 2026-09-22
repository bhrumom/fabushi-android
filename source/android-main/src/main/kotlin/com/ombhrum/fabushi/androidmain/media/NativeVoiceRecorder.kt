package com.ombhrum.fabushi

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

internal class NativeVoiceRecorder(private val context: Context) {
    data class Recording(val file: File, val bytes: ByteArray)

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun start(): Result<Unit> = runCatching {
        check(!isRecording) { "录音已经开始" }
        val directory = File(context.cacheDir, "fabushi-voice").apply { mkdirs() }
        val file = File(directory, "voice-${System.nanoTime()}.m4a")
        @Suppress("DEPRECATION")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioEncodingBitRate(64_000)
        mediaRecorder.setAudioSamplingRate(44_100)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorder = mediaRecorder
        outputFile = file
        isRecording = true
    }

    fun stop(): Result<Recording> = runCatching {
        check(isRecording) { "录音尚未开始" }
        val active = checkNotNull(recorder)
        val file = checkNotNull(outputFile)
        try { active.stop() } finally { active.release() }
        recorder = null
        outputFile = null
        isRecording = false
        val bytes = file.readBytes()
        check(bytes.isNotEmpty()) { "录音文件为空" }
        Recording(file, bytes)
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        isRecording = false
        outputFile?.delete()
        outputFile = null
    }
}
