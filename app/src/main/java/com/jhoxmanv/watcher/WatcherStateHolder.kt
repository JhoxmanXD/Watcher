package com.jhoxmanv.watcher

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A singleton object to hold the global state of the application,
 * such as the running state of the WatcherService.
 */
object WatcherStateHolder {
    // StateFlow to hold the running status of the service.
    // The UI will collect this flow to react to state changes.
    val isServiceRunning = MutableStateFlow(false)
}
