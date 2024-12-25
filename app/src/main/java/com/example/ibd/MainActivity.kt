package com.example.ibd

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh)

        webView = findViewById(R.id.webview)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefreshLayout.isRefreshing = false
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    this@MainActivity,
                    "Error: $description",
                    Toast.LENGTH_SHORT
                ).show()
                super.onReceivedError(view, errorCode, description, failingUrl)
            }
        }

        webView.loadUrl("https://ibd.hsiiv.uz")

        swipeRefreshLayout.setOnRefreshListener {
            if (isNetworkAvailable()) {
                webView.reload()
            } else {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        webSettings.domStorageEnabled = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.mixedContentMode =
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        swipeRefreshLayout.setColorSchemeResources(
            R.color.blue_primary,
            R.color.teal_700,
            R.color.purple_200
        )

        setImmersiveMode()
    }

    private fun setImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setImmersiveMode()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onResume() {
        super.onResume()
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
