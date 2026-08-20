package com.ombhrum.fabushi.core

import android.content.Context
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

class MahayanaHost(context: Context, featureHostTest: Boolean = false) : Closeable {
    private val handle: AtomicLong

    init {
        System.loadLibrary("mahayana_app_host")
        val value = if (featureHostTest) {
            nativeCreateTest(context.filesDir.absolutePath)
        } else {
            nativeCreate(context.filesDir.absolutePath)
        }
        check(value != 0L) { "Failed to initialize Mahayana Rust host" }
        handle = AtomicLong(value)
    }

    fun request(method: String, params: JSONObject = JSONObject()): JSONObject {
        val active = handle.get()
        check(active != 0L) { "Mahayana host is closed" }
        val request = JSONObject()
            .put("method", method)
            .put("params", params)
        val response = JSONObject(nativeDispatch(active, request.toString()))
        if (!response.optBoolean("ok", false)) {
            error(response.optString("error", "Mahayana host request failed"))
        }
        return response.optJSONObject("result") ?: JSONObject().put("value", response.opt("result"))
    }

    fun requestValue(method: String, params: JSONObject = JSONObject()): Any? {
        val active = handle.get()
        check(active != 0L) { "Mahayana host is closed" }
        val request = JSONObject().put("method", method).put("params", params)
        val response = JSONObject(nativeDispatch(active, request.toString()))
        if (!response.optBoolean("ok", false)) error(response.optString("error", "Mahayana host request failed"))
        return response.opt("result")
    }

    override fun close() {
        val active = handle.getAndSet(0L)
        if (active != 0L) nativeDestroy(active)
    }

    private external fun nativeCreate(appDataDir: String): Long
    private external fun nativeCreateTest(appDataDir: String): Long
    private external fun nativeDispatch(handle: Long, requestJson: String): String
    private external fun nativeDestroy(handle: Long)
}
