package com.audiobookshelf.wear.sync

import com.audiobookshelf.wear.WearApp
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.launch

/** Receives server connection and setting updates from the phone app, even while the watch app is closed. */
class PhoneListenerService : WearableListenerService() {
  override fun onDataChanged(dataEvents: DataEventBuffer) {
    val app = application as WearApp
    dataEvents.forEach { event ->
      if (event.type != DataEvent.TYPE_CHANGED) return@forEach
      val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
      when (event.dataItem.uri.path) {
        PhoneConfig.PATH_SERVER -> {
          app.phoneConfig.update(dataMap)
          // The phone signed in, or switched server or user
          app.appScope.launch { app.syncSignInFromPhone() }
        }
        ContentSettings.PATH_SETTINGS -> app.contentSettings.update(dataMap)
      }
    }
  }
}
