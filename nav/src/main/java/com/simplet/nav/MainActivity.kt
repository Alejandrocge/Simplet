package com.simplet.nav

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Simplet navigation shell. The map, search, routing and guidance all live in
 * the bundled WebView page; Kotlin only feeds it GPS fixes and, once at launch,
 * a device/ABI line (shown briefly as a toast so the real head unit can still
 * be identified during bring-up).
 */
class MainActivity : Activity(), LocationListener {

    private lateinit var web: WebView
    private var mapReady = false
    private var lastLocation: Location? = null
    private var lastGpsFixAt = 0L

    @Suppress("DEPRECATION", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        web = findViewById(R.id.web)

        // Render the WebView in software. On some Android-10 / emulator GPUs the
        // hardware layer does not present frames until an input event, leaving the
        // map and controls black/blank until tapped. Software rendering is reliable
        // (we can revisit for performance once the real unit is verified).
        web.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            userAgentString = "$userAgentString Simplet/0.4 (+https://github.com/Alejandrocge/Simplet)"
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                mapReady = true
                pushDeviceInfo()
                lastLocation?.let { pushLocation(it) }
            }
        }
        web.loadUrl("file:///android_asset/map.html")

        // Runtime permissions only exist on API 23+; below that they are granted
        // at install time, so just start.
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            startGps()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps()
        }
    }

    private fun startGps() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsOn = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netOn = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        try {
            if (gpsOn) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            if (netOn) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
            val last = (if (gpsOn) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null)
                ?: (if (netOn) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null)
            last?.let { onLocationChanged(it) }
        } catch (e: SecurityException) {
            // permission revoked mid-session; nothing to do
        }
    }

    override fun onLocationChanged(location: Location) {
        val now = System.currentTimeMillis()
        if (location.provider == LocationManager.GPS_PROVIDER) lastGpsFixAt = now
        // Ignore coarse network fixes while a fresh GPS fix is available, so the
        // position marker doesn't jump between accurate and approximate fixes.
        if (location.provider == LocationManager.NETWORK_PROVIDER && now - lastGpsFixAt < 15000) return
        lastLocation = location
        if (mapReady) pushLocation(location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Required on older API levels")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    private fun pushLocation(loc: Location) {
        web.evaluateJavascript(
            "setLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy});",
            null
        )
    }

    private fun pushDeviceInfo() {
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val info = "Simplet • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" +
            " • ${Build.MANUFACTURER} ${Build.MODEL} • ABIs: $abis"
        web.evaluateJavascript("setDeviceInfo(${jsString(info)});", null)
    }

    private fun jsString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}
