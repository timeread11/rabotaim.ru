package com.codekon.rabotaim

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.InputStream
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val TARGET_URL = "https://rabotaim.ru"
    private var isErrorState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        setupWebView()
        setupSwipeRefresh()

        if (isNetworkAvailable()) {
            loadMainUrl()
        } else {
            showOfflinePage()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = settings.userAgentString + " RabotaimApp/1.0.0"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view?.loadUrl(url)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showOfflinePage()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener {
            if (isNetworkAvailable()) {
                if (isErrorState) {
                    loadMainUrl()
                } else {
                    webView.reload()
                }
            } else {
                swipeRefresh.isRefreshing = false
                showOfflinePage()
                Toast.makeText(this, "Отсутствует подключение к интернету", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMainUrl() {
        isErrorState = false
        webView.loadUrl(TARGET_URL)
    }

    private fun showOfflinePage() {
        isErrorState = true
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.offline)
            val bytes = inputStream.readBytes()
            val html = String(bytes, StandardCharsets.UTF_8)
            webView.loadDataWithBaseURL(TARGET_URL, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            webView.loadData("<h2>Нет подключения к интернету</h2><button onclick=\"location.href='https://rabotaim.ru'\">Обновить страницу</button>", "text/html", "UTF-8")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            if (isErrorState) {
                finish()
            } else {
                webView.goBack()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun reload() {
            runOnUiThread {
                if (isNetworkAvailable()) {
                    loadMainUrl()
                } else {
                    showOfflinePage()
                    Toast.makeText(this@MainActivity, "Интернет по-прежнему недоступен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
