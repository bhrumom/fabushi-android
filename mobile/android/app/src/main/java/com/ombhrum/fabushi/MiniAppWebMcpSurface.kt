package com.ombhrum.fabushi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val WEB_MCP_ORIGIN = "fabushi.ombhrum.com"
private const val LOCAL_WEB_MCP_ORIGIN = "miniapp.local.fabushi.invalid"

private class MiniAppNativeWebMcpBridge(
    private val plugin: MarketplacePlugin,
    private val context: android.content.Context,
    private val localDocumentActive: () -> Boolean,
    private val callRuntimeToolJson: (pluginId: String, name: String, argumentsJson: String) -> String,
) {
    private val tools = plugin.tools.associateBy { it.name }

    @JavascriptInterface
    fun callTool(name: String, argumentsJson: String): String {
        if (!localDocumentActive()) {
            return JSONObject()
                .put("ok", false)
                .put("error", "Native WebMCP bridge is available only to the local MiniApp document")
                .toString()
        }
        val tool = tools[name]
            ?: return JSONObject().put("ok", false).put("error", "Tool is not in this MiniApp contract").toString()
        if (tool.approval != "none" && !requestNativeApproval(tool)) {
            return JSONObject().put("ok", false).put("error", "用户取消了 WebMCP Tool 调用").toString()
        }
        return runCatching {
            val resultJson = callRuntimeToolJson(plugin.pluginId, name, argumentsJson)
            "{\"ok\":true,\"result\":$resultJson}"
        }.getOrElse { error ->
            JSONObject()
                .put("ok", false)
                .put("error", error.message ?: "WebMCP runtime call failed")
                .toString()
        }
    }

    private fun requestNativeApproval(tool: MiniAppToolContract): Boolean {
        val decided = AtomicBoolean(false)
        val allowed = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            val warning = if (tool.approval == "destructive") {
                "该操作可能产生破坏性修改。"
            } else {
                "该操作会修改小程序或后台状态。"
            }
            AlertDialog.Builder(context)
                .setTitle("允许 WebMCP 调用 ${tool.name}？")
                .setMessage("${tool.description}\n\n$warning")
                .setPositiveButton("允许") { _, _ ->
                    if (decided.compareAndSet(false, true)) {
                        allowed.set(true)
                        latch.countDown()
                    }
                }
                .setNegativeButton("取消") { _, _ ->
                    if (decided.compareAndSet(false, true)) latch.countDown()
                }
                .setOnCancelListener {
                    if (decided.compareAndSet(false, true)) latch.countDown()
                }
                .show()
        }
        latch.await(2, TimeUnit.MINUTES)
        return allowed.get()
    }
}

private fun toolContractJson(plugin: MarketplacePlugin): String {
    val rows = JSONArray()
    for (tool in plugin.tools) {
        rows.put(
            JSONObject()
                .put("name", tool.name)
                .put("description", tool.description)
                .put("readOnlyHint", tool.approval == "none"),
        )
    }
    return rows.toString()
}

private fun injectLocalWebMcp(html: String, plugin: MarketplacePlugin): String {
    val tools = toolContractJson(plugin)
    val bootstrap = """
        <script>
        (function(){
          const definitions=$tools;
          const localTools=new Map();
          const controllers=[];
          function publicTool(tool){const copy={...tool};delete copy.execute;return copy;}
          function nativeCall(name,input){
            const raw=window.FabushiWebMcpNative.callTool(name,JSON.stringify(input||{}));
            const payload=JSON.parse(raw);
            if(!payload.ok)throw new Error(payload.error||'WebMCP runtime call failed');
            return payload.result;
          }
          function register(item){
            const tool={name:item.name,description:item.description||item.name,inputSchema:{type:'object',properties:{}},annotations:{readOnlyHint:item.readOnlyHint===true},execute:(input)=>nativeCall(item.name,input)};
            localTools.set(tool.name,tool);
            if(document.modelContext&&typeof document.modelContext.registerTool==='function'){
              const controller=new AbortController();controllers.push(controller);
              Promise.resolve(document.modelContext.registerTool(tool,{signal:controller.signal})).catch(()=>{});
            }
          }
          for(const item of definitions)register(item);
          Object.defineProperty(window,'__fabushiWebMcp',{configurable:true,value:{version:1,list:()=>Array.from(localTools.values()).map(publicTool),call:async(name,input={})=>{const tool=localTools.get(name);if(!tool)throw new Error('Unknown WebMCP tool: '+name);return tool.execute(input);}}});
          window.addEventListener('pagehide',()=>{for(const controller of controllers)controller.abort();},{once:true});
          window.dispatchEvent(new CustomEvent('fabushi:webmcp-ready',{detail:{pluginId:${JSONObject.quote(plugin.pluginId)},tools:definitions.map(t=>t.name)}}));
        })();
        </script>
    """.trimIndent()
    return if (html.contains("</head>", ignoreCase = true)) {
        html.replace(Regex("</head>", RegexOption.IGNORE_CASE), "$bootstrap</head>")
    } else {
        "$bootstrap$html"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniAppWebMcpSurface(
    plugin: MarketplacePlugin,
    loadLocalHtml: suspend (pluginId: String) -> String?,
    callRuntimeToolJson: (pluginId: String, name: String, argumentsJson: String) -> String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember(plugin.pluginId) { mutableStateOf("正在解析本地 WebMCP…") }
    var localHtml by remember(plugin.pluginId) { mutableStateOf<String?>(null) }
    var sourceResolved by remember(plugin.pluginId) { mutableStateOf(false) }
    val localDocumentActive = remember(plugin.pluginId) { AtomicBoolean(false) }
    val encodedId = URLEncoder.encode(plugin.pluginId, StandardCharsets.UTF_8.toString())
    val hostedUrl = "https://fabushi.ombhrum.com/miniapps/$encodedId/"

    LaunchedEffect(plugin.pluginId) {
        localHtml = loadLocalHtml(plugin.pluginId)
        sourceResolved = true
        status = if (localHtml != null) "正在加载本地 WebMCP…" else "正在加载 Hosted WebMCP…"
    }

    BackHandler(onBack = onClose)

    Column(modifier = Modifier.fillMaxSize().testTag("miniapp-webmcp-surface")) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Button(onClick = onClose, modifier = Modifier.testTag("miniapp-webmcp-close")) {
                Text("返回")
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(plugin.displayName, style = MaterialTheme.typography.titleMedium)
                Text(status, style = MaterialTheme.typography.labelSmall)
            }
        }

        val webView = remember(plugin.pluginId) {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                addJavascriptInterface(
                    MiniAppNativeWebMcpBridge(
                        plugin = plugin,
                        context = context,
                        localDocumentActive = localDocumentActive::get,
                        callRuntimeToolJson = callRuntimeToolJson,
                    ),
                    "FabushiWebMcpNative",
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        return uri.scheme != "https" || uri.host !in setOf(WEB_MCP_ORIGIN, LOCAL_WEB_MCP_ORIGIN)
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        localDocumentActive.set(url?.startsWith("https://$LOCAL_WEB_MCP_ORIGIN/") == true)
                        status = "正在加载 WebMCP…"
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        val probe = """
                            (() => {
                              const tools = window.__fabushiWebMcp?.list?.() || [];
                              return JSON.stringify({ ready: tools.length > 0, tools: tools.map(t => t.name) });
                            })()
                        """.trimIndent()
                        view.evaluateJavascript(probe) { value ->
                            status = if (value.contains("\\\"ready\\\":true")) {
                                if (url?.contains(LOCAL_WEB_MCP_ORIGIN) == true) "本地 WebMCP 已连接" else "WebMCP 已连接"
                            } else {
                                "WebMCP 页面已打开"
                            }
                        }
                    }
                }
            }
        }

        AndroidView(
            factory = { webView },
            update = { view ->
                if (!sourceResolved) return@AndroidView
                val local = localHtml
                val desiredTag = if (local != null) "local:${plugin.pluginId}" else "hosted:${plugin.pluginId}"
                if (view.tag == desiredTag) return@AndroidView
                view.tag = desiredTag
                if (local != null) {
                    localDocumentActive.set(true)
                    val baseUrl = "https://$LOCAL_WEB_MCP_ORIGIN/miniapps/$encodedId/"
                    view.loadDataWithBaseURL(
                        baseUrl,
                        injectLocalWebMcp(local, plugin),
                        "text/html",
                        "utf-8",
                        null,
                    )
                } else {
                    localDocumentActive.set(false)
                    view.loadUrl(hostedUrl)
                }
            },
            modifier = Modifier.fillMaxSize().testTag("miniapp-webmcp-webview"),
        )

        DisposableEffect(webView) {
            onDispose {
                localDocumentActive.set(false)
                webView.stopLoading()
                webView.removeJavascriptInterface("FabushiWebMcpNative")
                webView.loadUrl("about:blank")
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }
}
