package com.samyak.urltvadmin

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class URLTVAdminApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        
        // Verbose Logging set to help debug issues, remove before releasing your app.
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        
        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        
        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
    
    companion object {
        // Replace with your actual OneSignal App ID
        const val ONESIGNAL_APP_ID = "ba1cab7b-e6a6-4e54-907b-c6f6ab2eb906"
    }
} 