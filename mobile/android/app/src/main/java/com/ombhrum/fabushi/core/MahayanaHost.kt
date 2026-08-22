package com.ombhrum.fabushi.core

import android.content.Context
import org.json.JSONObject
import java.io.Closeable

class MahayanaHost(context: Context, featureHostTest: Boolean = false) : Closeable {
    private val lifecycleLock = Any()
    @Volatile private var handle: Long

    init {
        System.loadLibrary("mahayana_app_host")
        val value = if (featureHostTest) {
            nativeCreateTest(context.filesDir.absolutePath)
        } else {
            nativeCreate(context.filesDir.absolutePath)
        }
        check(value != 0L) { "Failed to initialize Mahayana Rust host" }
        handle = value
    }

    fun request(method: String, params: JSONObject = JSONObject()): JSONObject = synchronized(lifecycleLock) {
        val active = handle
        check(active != 0L) { "Mahayana host is closed" }
        val request = JSONObject().put("method", method).put("params", params)
        val response = JSONObject(nativeDispatch(active, request.toString()))
        if (!response.optBoolean("ok", false)) {
            error(response.optString("error", "Mahayana host request failed"))
        }
        response.optJSONObject("result") ?: JSONObject().put("value", response.opt("result"))
    }

    fun requestValue(method: String, params: JSONObject = JSONObject()): Any? = synchronized(lifecycleLock) {
        val active = handle
        check(active != 0L) { "Mahayana host is closed" }
        val request = JSONObject().put("method", method).put("params", params)
        val response = JSONObject(nativeDispatch(active, request.toString()))
        if (!response.optBoolean("ok", false)) error(response.optString("error", "Mahayana host request failed"))
        response.opt("result")
    }

    override fun close() {
        synchronized(lifecycleLock) {
            val active = handle
            if (active == 0L) return
            // Holding the same lock used by every JNI dispatch guarantees no
            // native call can still be dereferencing this pointer when it is freed.
            handle = 0L
            nativeDestroy(active)
        }
    }

    private external fun nativeCreate(appDataDir: String): Long
    private external fun nativeCreateTest(appDataDir: String): Long
    private external fun nativeDispatch(handle: Long, requestJson: String): String
    private external fun nativeDestroy(handle: Long)
}
