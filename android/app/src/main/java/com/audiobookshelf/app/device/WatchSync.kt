package com.audiobookshelf.app.device

import android.content.Context
import android.util.Log
import com.audiobookshelf.app.data.ServerConnectionConfig
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/**
 * Tells the Audiobookshelf Wear OS app when the phone's server or user changes (the watch then
 * asks [WatchBridge] for a sign in), and keeps the explicit content setting the same on both
 * devices. Paths and keys must match `PhoneConfig` and `ContentSettings` in the wear module.
 */
object WatchSync {
  private const val tag = "WatchSync"
  private const val PATH_SERVER = "/abs/server"
  const val PATH_SETTINGS = "/abs/settings"
  const val KEY_HIDE_EXPLICIT = "hideExplicitContent"
  const val KEY_UPDATED_AT = "updatedAt"

  /** Shares the "Show Explicit Content" setting so the watch matches. Path and keys match `ContentSettings` on the watch. */
  fun publishSettings(context: Context, hideExplicitContent: Boolean, updatedAt: Long) {
    val request = PutDataMapRequest.create(PATH_SETTINGS).apply {
      dataMap.putBoolean(KEY_HIDE_EXPLICIT, hideExplicitContent)
      dataMap.putLong(KEY_UPDATED_AT, updatedAt)
    }.asPutDataRequest().setUrgent()

    Wearable.getDataClient(context.applicationContext).putDataItem(request)
      .addOnFailureListener { e -> Log.d(tag, "Could not share settings with watch: ${e.message}") }
  }

  fun publishServerConfig(context: Context, config: ServerConnectionConfig?) {
    if (config == null) return
    val request = PutDataMapRequest.create(PATH_SERVER).apply {
      dataMap.putString("address", config.address)
      dataMap.putString("username", config.username)
      dataMap.putString("customHeaders", JSONObject(config.customHeaders ?: emptyMap<String, String>()).toString())
    }.asPutDataRequest().setUrgent()

    Wearable.getDataClient(context.applicationContext).putDataItem(request)
      .addOnFailureListener { e ->
        // Expected on phones without a paired watch or the Wear OS companion
        Log.d(tag, "Could not share server with watch: ${e.message}")
      }
  }
}
