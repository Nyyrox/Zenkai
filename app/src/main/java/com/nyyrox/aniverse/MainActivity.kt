package com.nyyrox.aniverse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.player.PlayerGesturePlugin
import com.nyyrox.aniverse.util.DisplayRefreshRateManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.jan.supabase.auth.handleDeeplinks
import com.nyyrox.aniverse.data.supabase.SupabaseManager

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nyyrox.aniverse.notifications.ZenkaiNotificationHelper
import android.app.PictureInPictureParams
import android.util.Rational
import android.content.res.Configuration
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.nyyrox.aniverse.data.discord.DiscordAccountManager

class MainActivity : ComponentActivity() {
    companion object {
        val isInPipMode = mutableStateOf(false)
        var isPlayerActive: Boolean = false

        fun enterPip(activity: ComponentActivity, rational: Rational = Rational(16, 9)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val builder = PictureInPictureParams.Builder()
                        .setAspectRatio(clampRational(rational))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        builder.setAutoEnterEnabled(true)
                        builder.setSeamlessResizeEnabled(true)
                    }
                    activity.enterPictureInPictureMode(builder.build())
                } catch (_: Exception) {}
            }
        }

        fun updatePipParams(activity: ComponentActivity, isPlaying: Boolean, rational: Rational = Rational(16, 9)) {
            isPlayerActive = isPlaying
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(isPlaying)
                        .setAspectRatio(clampRational(rational))
                        .setSeamlessResizeEnabled(true)
                        .build()
                    activity.setPictureInPictureParams(params)
                } catch (_: Exception) {}
            }
        }

        fun clampRational(r: Rational): Rational {
            val f = r.toFloat()
            return when {
                f < 0.41841f -> Rational(100, 239)
                f > 2.39f -> Rational(239, 100)
                else -> r
            }
        }
    }

    private val deepLinkUriState = mutableStateOf<String?>(null)

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED == intent?.action) {
                applyConfiguredRefreshRate()
            }
        }
    }

    private fun applyConfiguredRefreshRate() {
        try {
            val settings = StorageManager(this).getThemeSettings()
            DisplayRefreshRateManager.applyRefreshRate(
                this,
                settings.preferredRefreshRate,
                settings.isPowerSavingEnabled
            )
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    // Install splash screen before super.onCreate per Android docs
    val splashScreen = installSplashScreen()
    var keepSplash = true
    splashScreen.setKeepOnScreenCondition { keepSplash }
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.decorView.postDelayed({ keepSplash = false }, 700)
    enableEdgeToEdge()
    applyConfiguredRefreshRate()

    // Initialize notification channel and request notification permission on Android 13+
    ZenkaiNotificationHelper.createNotificationChannel(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

        try {
            registerReceiver(
                powerSaveReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            )
        } catch (_: Throwable) {}

        // Start client-side HTTP stream proxy server
        com.nyyrox.aniverse.player.StreamProxyServer.start()

        // Initialize AniBridge v3 Mapping Engine
        com.nyyrox.aniverse.data.mapping.AniBridgeResolver.init(this)

        // Initialize Discord Integration & Credentials
        com.nyyrox.aniverse.data.discord.DiscordAccountManager.init(this)
        val discordStorage = StorageManager(this)
        com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_ID = discordStorage.getDiscordClientId()
        com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_SECRET = discordStorage.getDiscordClientSecret()

        handleDiscordDeepLink(intent?.data)

        try {
            val imageOkHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val requestBuilder = request.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    if (request.header("Referer") == null) {
                        requestBuilder.header("Referer", "https://weebcentral.com/")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            SingletonImageLoader.setSafe { context ->
                ImageLoader.Builder(context)
                    .components {
                        add(OkHttpNetworkFetcherFactory(callFactory = { imageOkHttpClient }))
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(coil3.gif.AnimatedImageDecoder.Factory())
                        } else {
                            add(coil3.gif.GifDecoder.Factory())
                        }
                    }
                    .build()
            }
        } catch (_: Exception) {
            // Safe fallback if already initialized
        }

        val initialActionUrl = intent?.getStringExtra("action_url")
        val initialRouteExtra = intent?.getStringExtra("route")
        val initialPartnerId = intent?.getStringExtra("partner_user_id") ?: intent?.getStringExtra("target_id")
        deepLinkUriState.value = if (!initialActionUrl.isNullOrBlank()) {
            initialActionUrl
        } else if (!initialRouteExtra.isNullOrBlank()) {
            if ((initialRouteExtra == "direct_chat" || initialRouteExtra == "dm") && !initialPartnerId.isNullOrBlank()) {
                "zenkai://direct_chat/$initialPartnerId"
            } else {
                "zenkai://$initialRouteExtra"
            }
        } else intent?.dataString
        try {
            intent?.let { SupabaseManager.client.handleDeeplinks(it) }
        } catch (_: Throwable) {}

        setContent {
            Box(Modifier.fillMaxSize()) {
                App(deepLinkUri = deepLinkUriState.value)
                com.nyyrox.aniverse.player.MiniPlayerHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val actionUrlExtra = intent.getStringExtra("action_url")
        val routeExtra = intent.getStringExtra("route")
        val partnerId = intent.getStringExtra("partner_user_id") ?: intent.getStringExtra("target_id")
        deepLinkUriState.value = if (!actionUrlExtra.isNullOrBlank()) {
            actionUrlExtra
        } else if (!routeExtra.isNullOrBlank()) {
            if ((routeExtra == "direct_chat" || routeExtra == "dm") && !partnerId.isNullOrBlank()) {
                "zenkai://direct_chat/$partnerId"
            } else {
                "zenkai://$routeExtra"
            }
        } else intent.dataString
        try {
            SupabaseManager.client.handleDeeplinks(intent)
        } catch (_: Throwable) {}

        handleDiscordDeepLink(intent.data)
    }

    private fun handleDiscordDeepLink(data: android.net.Uri?) {
        if (data == null) return
        val scheme = data.scheme.orEmpty()
        val host = data.host.orEmpty()
        if ((scheme == "zenkai" || scheme == "aniverse") && (host == "discord" || host == "login-callback")) {
            val code = data.getQueryParameter("code")
            val fragment = data.fragment.orEmpty()
            val fragmentParams = fragment.split("&").associate {
                val parts = it.split("=")
                parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
            }
            val token = data.getQueryParameter("provider_token")
                ?: fragmentParams["provider_token"]
                ?: data.getQueryParameter("access_token")
                ?: fragmentParams["access_token"]

            lifecycleScope.launch {
                if (!token.isNullOrBlank()) {
                    DiscordAccountManager.fetchProfileWithToken(token)
                } else if (!code.isNullOrBlank()) {
                    DiscordAccountManager.exchangeCodeForToken(code)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val mini = com.nyyrox.aniverse.player.MiniPlayerManager.parked?.player
        if ((isPlayerActive || mini?.isPlaying == true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val vs = mini?.videoSize
                if (!isPlayerActive && vs != null && vs.width > 0 && vs.height > 0) enterPip(this, Rational(vs.width, vs.height))
                else enterPip(this)
            } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            // When exiting PiP mode: if activity is finishing or not resumed (i.e. user closed/dismissed PiP window), pause playback
            if (isFinishing || lifecycle.currentState < androidx.lifecycle.Lifecycle.State.RESUMED) {
                com.nyyrox.aniverse.service.ActivePlayerHolder.player?.pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInPictureInPictureMode
        } else false
        if (!inPip) {
            com.nyyrox.aniverse.service.ActivePlayerHolder.player?.pause()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (PlayerGesturePlugin.VolumeNotifier.isPlayerActive) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                val newVol = PlayerGesturePlugin.adjustVolumeByStep(this, 1)
                PlayerGesturePlugin.VolumeNotifier.onVolumeChanged?.invoke(newVol)
                return true
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                val newVol = PlayerGesturePlugin.adjustVolumeByStep(this, -1)
                PlayerGesturePlugin.VolumeNotifier.onVolumeChanged?.invoke(newVol)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        applyConfiguredRefreshRate()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(powerSaveReceiver)
        } catch (_: Throwable) {}
        com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
        if (isFinishing) com.nyyrox.aniverse.player.MiniPlayerManager.release(this)
    }
}
