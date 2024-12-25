package com.example.ibd

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var connectivityManager: ConnectivityManager
    private var isConnected: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isConnected = true
        }

        override fun onLost(network: Network) {
            isConnected = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register network callback
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

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
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Only handle the main frame errors
                if (request?.isForMainFrame == true) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${error?.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                super.onReceivedError(view, request, error)
            }
        }

//        webView.loadUrl("https://ibd.hsiiv.uz")
        webView.loadUrl("http://192.168.10.67:8080")

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
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE // Changed for better security

        swipeRefreshLayout.setColorSchemeResources(
            R.color.blue_primary,
            R.color.teal_700,
            R.color.purple_200
        )

        setImmersiveMode()
        setupOnBackPressed()
    }

    private fun setImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupOnBackPressed() {
        // Add a callback for handling back presses
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // If the WebView can't go back, delegate the back press to the system
                    // This will either close the app or navigate to the previous activity
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork: Network? = connectivityManager.activeNetwork
        val networkCapabilities: NetworkCapabilities? =
            connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
