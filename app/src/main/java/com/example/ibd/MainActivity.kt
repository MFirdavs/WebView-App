package com.example.ibd

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

    // Error Layout Components
    private lateinit var errorLayout: View
    private lateinit var retryButton: Button
    private lateinit var errorMessage: TextView
    private lateinit var errorImage: ImageView

    // Flag to track if an error occurred
    private var hasError: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {

            isConnected = true
            runOnUiThread {
                if (errorLayout.visibility == View.VISIBLE) {
                    // Hide error layout and attempt to reload
                    hideErrorLayout()
                    swipeRefreshLayout.isRefreshing = true
                    webView.reload()
                }
            }
        }

        override fun onLost(network: Network) {
            isConnected = false
            runOnUiThread {
                showErrorLayout(getString(R.string.no_internet_connection))
            }
        }
    }

    private var timeoutHandler: Handler? = null
    private val TIMEOUT = 10000L // 10 seconds in milliseconds
    private val TAG = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)

        // Initialize WebView
        webView = findViewById(R.id.webview)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.mixedContentMode =
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Initialize Error Layout
        errorLayout = findViewById(R.id.error_layout)
        retryButton = errorLayout.findViewById(R.id.retry_button)
        errorMessage = errorLayout.findViewById(R.id.error_message)
        errorImage = errorLayout.findViewById(R.id.error_image)

        // Set Retry Button Click Listener
        retryButton.setOnClickListener {
            if (isNetworkAvailable()) {
                hideErrorLayout()
                swipeRefreshLayout.isRefreshing = true
                webView.reload()
            } else {
                Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            }
        }

        // Setup WebViewClient
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                swipeRefreshLayout.isRefreshing = true
                hasError = false // Reset error flag at the start of page loading
                startTimeout()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefreshLayout.isRefreshing = false
                cancelTimeout()
                if (!hasError) {
                    hideErrorLayout()
                }
                super.onPageFinished(view, url)
            }


            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    hasError = true
                    swipeRefreshLayout.isRefreshing = false
                    showErrorLayout(getString(R.string.error_message))
                }
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    hasError = true
                    swipeRefreshLayout.isRefreshing = false
                    showErrorLayout("HTTP error: ${errorResponse?.statusCode}")
                }
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                hasError = true
                swipeRefreshLayout.isRefreshing = false
                showErrorLayout("SSL Error")
                handler?.cancel()
                super.onReceivedSslError(view, handler, error)
            }
        }

        // Load URL
//        webView.loadUrl("https://ibd.hsiiv.uz")
        webView.loadUrl("http://192.168.10.67:8080")

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if (errorLayout.visibility == View.VISIBLE) {
                // If error layout is visible, attempt to reload the WebView
                if (isNetworkAvailable()) {
                    hideErrorLayout()
                    swipeRefreshLayout.isRefreshing = true
                    webView.reload()
                } else {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
                }
            } else {
                // If WebView is visible, reload the WebView
                if (isNetworkAvailable()) {
                    webView.reload()
                } else {
                    swipeRefreshLayout.isRefreshing = false
                    showErrorLayout(getString(R.string.no_internet_connection))
                }
            }
        }

        swipeRefreshLayout.setColorSchemeResources(
            R.color.blue_primary,
            R.color.teal_700,
            R.color.purple_200
        )

        setImmersiveMode()
        setupOnBackPressed()
    }

    private fun startTimeout() {
        cancelTimeout() // Ensure no previous timeout is running
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
                showErrorLayout(getString(R.string.still_no_connection))
                webView.stopLoading()
            }
        }, TIMEOUT)
    }

    private fun cancelTimeout() {
        timeoutHandler?.removeCallbacksAndMessages(null)
    }

    private fun showErrorLayout(message: String) {
        errorMessage.text = message
        errorLayout.visibility = View.VISIBLE
    }

    private fun hideErrorLayout() {
        errorLayout.visibility = View.GONE
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
        cancelTimeout()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
