package com.ombhrum.fabushi

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val REMOTE_COMPUTER_ORIGIN = "fabushi.ombhrum.com"
private const val REMOTE_COMPUTER_URL = "https://fabushi.ombhrum.com/remote-computer"

/**
 * Restricted browser surface for human-operated remote computer sessions.
 *
 * This intentionally does not register a native bridge or inject JavaScript. The hosted
 * application retains regular browser JavaScript/WebRTC support without receiving arbitrary
 * access to the native Fabushi runtime.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RemoteComputerSurface(onClose: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("正在连接我的电脑…") }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    fun isAllowedUrl(uri: Uri): Boolean =
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(REMOTE_COMPUTER_ORIGIN, ignoreCase = true) &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setGeolocationEnabled(false)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            // This restricted surface deliberately has no native bridge.
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (isAllowedUrl(uri)) return false
                    if (request.isForMainFrame) {
                        loading = false
                        status = "已阻止外部导航"
                        errorMessage = "远程电脑页面只允许访问 https://fabushi.ombhrum.com。"
                    }
                    return true
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    loading = true
                    errorMessage = null
                    status = "正在安全连接…"
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    if (errorMessage == null) status = "已安全连接"
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    loading = false
                    status = "安全连接失败"
                    errorMessage = "无法验证远程电脑服务的安全证书。"
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (!request.isForMainFrame) return
                    loading = false
                    status = "连接失败"
                    errorMessage = error.description.toString().ifBlank { "无法加载远程电脑页面。" }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (!request.isForMainFrame || errorResponse.statusCode < 400) return
                    loading = false
                    status = "连接失败"
                    errorMessage = "远程电脑服务返回 ${errorResponse.statusCode}。"
                }
            }
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TestTags.RemoteComputerSurface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onClose, modifier = Modifier.testTag(TestTags.RemoteComputerClose)) {
                Text("返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("我的电脑", style = MaterialTheme.typography.titleMedium)
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag(TestTags.RemoteComputerStatus),
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).testTag(TestTags.RemoteComputerLoading),
                    strokeWidth = 2.dp,
                )
            }
        }

        errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag(TestTags.RemoteComputerError),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("无法打开远程电脑", style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            errorMessage = null
                            loading = true
                            status = "正在重新连接…"
                            reloadToken += 1
                        },
                        modifier = Modifier.testTag(TestTags.RemoteComputerReload),
                    ) {
                        Text("重新加载")
                    }
                }
            }
        }

        AndroidView(
            factory = { webView },
            update = { view ->
                if (view.tag == reloadToken) return@AndroidView
                view.tag = reloadToken
                view.loadUrl(REMOTE_COMPUTER_URL)
            },
            modifier = Modifier.fillMaxSize().testTag(TestTags.RemoteComputerWebView),
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
