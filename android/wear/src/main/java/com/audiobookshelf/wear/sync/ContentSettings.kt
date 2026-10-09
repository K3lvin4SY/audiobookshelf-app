package com.audiobookshelf.wear.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Client-only preferences; nothing here is sent to the server. The explicit content setting is
 * linked with the phone app over the Data Layer: both devices write `/abs/settings` and whichever
 * change is newest wins (path and keys must match `WatchSync` in the phone app).
 */
class ContentSettings(private val context: Context) {
  private val prefs = context.getSharedPreferences("content_settings", Context.MODE_PRIVATE)
  private val _hideExplicit = MutableStateFlow(prefs.getBoolean(KEY_HIDE_EXPLICIT, false))

  /** True when the user turned "Show explicit content" off. */
  val hideExplicit: StateFlow<Boolean> = _hideExplicit.asStateFlow()

  private val _classicHome = MutableStateFlow(prefs.getBoolean(KEY_CLASSIC_HOME, false))

  /** Use the original scrolling list home instead of the hub. Watch only; not shared with the phone. */
  val classicHome: StateFlow<Boolean> = _classicHome.asStateFlow()

  fun setClassicHome(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_CLASSIC_HOME, enabled).apply()
    _classicHome.value = enabled
  }

  private val updatedAt get() = prefs.getLong(KEY_UPDATED_AT, 0L)

  /** Changed on the watch: save it and tell the phone. */
  fun setHideExplicit(hide: Boolean) {
    val now = System.currentTimeMillis()
    save(hide, now)
    val request = PutDataMapRequest.create(PATH_SETTINGS).apply {
      dataMap.putBoolean(KEY_HIDE_EXPLICIT, hide)
      dataMap.putLong(KEY_UPDATED_AT, now)
    }.asPutDataRequest().setUrgent()
    Wearable.getDataClient(context).putDataItem(request)
      .addOnFailureListener { Log.w(TAG, "Could not share settings with phone: ${it.message}") }
  }

  /** A settings item from either device; applied only if newer than what we have. */
  fun update(dataMap: DataMap) {
    val changedAt = dataMap.getLong(KEY_UPDATED_AT, 0L)
    if (changedAt > updatedAt) save(dataMap.getBoolean(KEY_HIDE_EXPLICIT, false), changedAt)
  }

  suspend fun refresh() {
    try {
      val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH_SETTINGS).build()
      val items = Wearable.getDataClient(context).getDataItems(uri, DataClient.FILTER_PREFIX).await()
      try {
        items.forEach { update(DataMapItem.fromDataItem(it).dataMap) }
      } finally {
        items.release()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not read settings from phone", e)
    }
  }

  private fun save(hide: Boolean, changedAt: Long) {
    prefs.edit().putBoolean(KEY_HIDE_EXPLICIT, hide).putLong(KEY_UPDATED_AT, changedAt).apply()
    _hideExplicit.value = hide
  }

  companion object {
    private const val TAG = "ContentSettings"
    const val PATH_SETTINGS = "/abs/settings"
    const val KEY_HIDE_EXPLICIT = "hideExplicitContent"
    const val KEY_UPDATED_AT = "updatedAt"
    private const val KEY_CLASSIC_HOME = "classicHome"
  }
}
