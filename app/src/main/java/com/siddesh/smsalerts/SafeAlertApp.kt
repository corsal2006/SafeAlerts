package com.siddesh.smsalerts

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class SafeAlertApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("AUTH_DIAGNOSTIC", "Firebase initialization started")
        try {
            FirebaseApp.initializeApp(this)
            val settings = FirebaseFirestoreSettings.Builder().build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            val auth = FirebaseAuth.getInstance()
            Log.d("AUTH_DIAGNOSTIC", "FirebaseAuth instance created")
            Log.d("AUTH_DIAGNOSTIC", "Current user = ${auth.currentUser?.uid ?: "none"}")
        } catch (e: Exception) {
            Log.e("AUTH_DIAGNOSTIC", "Firebase initialization error: ${e.message}", e)
        }
    }
}
