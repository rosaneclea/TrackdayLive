// File: MainActivity.kt
package com.trackdaysp.interlagos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit class googleDriveManager: GoogleDriveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        googleDriveManager = GoogleDriveManager(this)
        
        // RF01: 1-click Google Login
        googleDriveManager.signIn()
        
        // RF02: Auto-start recording logic goes to Service
        startRecordingService()
    }
    
    private fun startRecordingService() {
        val serviceIntent = Intent(this, RecordingService::class.java)
        startForegroundService(serviceIntent)
    }
}
