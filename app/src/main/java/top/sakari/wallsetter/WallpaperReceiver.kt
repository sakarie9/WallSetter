package top.sakari.wallsetter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.concurrent.thread

class WallpaperReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WallpaperReceiver"
        const val ACTION_SET_WALLPAPER = "top.sakari.wallsetter.SET_WALLPAPER"
        const val EXTRA_PATH = "path"
        const val EXTRA_TARGET = "target"

        const val TARGET_HOME = "home"
        const val TARGET_LOCK = "lock"
        const val TARGET_BOTH = "both"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SET_WALLPAPER) return

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            Log.e(TAG, "No path provided in intent extra")
            return
        }

        val target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_BOTH

        val pendingResult = goAsync()

        thread {
            try {
                setWallpaper(context, path, target)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set wallpaper", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun setWallpaper(context: Context, path: String, target: String) {
        try {
            WallpaperSetter.setFromPath(context, path, target)
            Log.i(TAG, "Wallpaper set successfully: target=$target, path=$path")
        } catch (e: Exception) {
            Log.e(TAG, "Wallpaper set failed: target=$target, path=$path", e)
        }
    }
}
