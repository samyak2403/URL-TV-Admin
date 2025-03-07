package com.samyak.urltvadmin

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.samyak.urltvadmin.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_URLTVAdmin)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"

        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.versionText.text = "Version 1.0"
        }
        
        // Set up other UI elements as needed
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 