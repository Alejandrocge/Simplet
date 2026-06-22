package com.simplet.nav

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simplet navigation app shell.
 *
 * The map, search and (later) routing live in the bundled WebView page; Kotlin
 * stays thin: it feeds GPS into the page and keeps a small diagnostic panel
 * (ABI list + live fix) on screen during early bring-up so the real head unit
 * still reports the things we need to confirm.
 */
class MainActivity : Activity(), LocationListener {

    private lateinit var status: TextView
    private lateinit var web: WebView
    private var mapReady = false
    private var lastLocation: Location? = null

    @Suppress("DEPRECATION", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        web = findViewById(R.id.web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // The page is served from file:///android_asset, but it needs to fetch
            // Nominatim/OSRM (other origins). Allow that for our own trusted assets.
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            // Identify ourselves to OSM/Nominatim per their usage policy.
            userAgentString = "$userAgentString Simplet/0.2 (+https://github.com/Alejandrocge/Simplet)"
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                mapReady = true
                lastLocation?.let { pushLocation(it) }
            }
        }
        web.loadUrl("file:///android_asset/map.html")

        render("starting…")

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
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
        } else {
            render("location permission DENIED — grant it in app settings")
        }
    }

    private fun startGps() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsOn = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netOn = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        render("GPS provider: $gpsOn | network provider: $netOn\nwaiting for a fix…")
        try {
            if (gpsOn) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            if (netOn) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
            val last = (if (gpsOn) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null)
                ?: (if (netOn) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null)
            last?.let { onLocationChanged(it) }
        } catch (e: SecurityException) {
            render("location error: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        render("FIX: ${fmt(location)}")
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

    private fun fmt(l: Location): String {
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(l.time))
        return "lat %.6f, lng %.6f, ±%dm, %s @ %s".format(
            l.latitude, l.longitude, l.accuracy.toInt(), l.provider, t
        )
    }

    private fun render(msg: String) {
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        status.text = buildString {
            append("Simplet nav\n")
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("ABIs: $abis\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append(msg)
        }
    }
}
