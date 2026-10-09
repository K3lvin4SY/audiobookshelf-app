package com.audiobookshelf.app.device

import android.util.Log
import com.audiobookshelf.app.managers.DbManager
import com.audiobookshelf.app.server.ApiHandler
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.getcapacitor.JSObject
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives settings changed on the Wear OS app, even while the phone app is closed. */
class WatchListenerService : WearableListenerService() {
  private val tag = "WatchListenerService"

  override fun onDataChanged(dataEvents: DataEventBuffer) {
    DbManager.initialize(applicationContext)
    dataEvents.forEach { event ->
      if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != WatchSync.PATH_SETTINGS) return@forEach
      val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
      val hide = dataMap.getBoolean(WatchSync.KEY_HIDE_EXPLICIT, false)
      val changedAt = dataMap.getLong(WatchSync.KEY_UPDATED_AT, 0L)
      // Our own changes come back here too; applyExplicitSetting ignores anything not newer
      if (DeviceManager.applyExplicitSetting(hide, changedAt)) {
        Log.d(tag, "Explicit content setting changed on the watch (hide=$hide)")
        notifyWebUi()
      }
    }
  }

  /** Lets the open app refresh its settings and lists. */
  private fun notifyWebUi() {
    if (!ApiHandler.checkAbsDatabaseNotifyListenersInitted()) return
    val deviceDataJson = jacksonObjectMapper().writeValueAsString(DeviceManager.deviceData)
    ApiHandler.absDatabaseNotifyListeners("onDeviceSettingsChanged", JSObject(deviceDataJson))
  }
}
