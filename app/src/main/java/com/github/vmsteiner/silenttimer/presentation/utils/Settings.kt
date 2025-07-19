package com.github.vmsteiner.silenttimer.presentation.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Utility file for managing application-level preferences using Jetpack DataStore.
 *
 * Provides a Context extension to access the Preferences DataStore instance,
 * and defines preference keys used throughout the application.
 */


/**
 * Extension property that provides access to the Preferences DataStore instance
 * scoped to the application context. The DataStore is named "settings".
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Key used to access the boolean preference indicating whether the halftime alert is enabled.
 */
val HALFTIME_ALERT_KEY = booleanPreferencesKey("halftime_alert")