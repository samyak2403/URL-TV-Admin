package com.samyak.urltvadmin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.samyak.urltvadmin.databinding.ActivityMainBinding
import com.samyak.urltvadmin.utils.CategoryManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Apply staggered animations to UI elements
        animateUIElements()
        
        // Set touch listeners for hover effects
        setupCardTouchEffects(binding.adminCard)
        setupCardTouchEffects(binding.categoryCard)
        
        // Set click listeners for cards with improved handling
        binding.adminCard.setOnClickListener {
            // Ensure we navigate to AdminActivity with proper animation
            animateAndNavigate(binding.adminCard, AdminActivity::class.java)
        }
        
        binding.categoryCard.setOnClickListener {
            // For now, navigate to AdminActivity
            // In the future, this should navigate to CategoryActivity
            animateAndNavigate(binding.categoryCard, AddCategoryActivity::class.java)
        }
        
        // Initialize CategoryManager with Firebase data
        CategoryManager.loadCategoriesFromFirebase {}
    }
    
    private fun setupCardTouchEffects(card: View) {
        val hoverAnimation = AnimationUtils.loadAnimation(this, R.anim.card_hover)
        val reverseAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
        reverseAnimation.duration = 200
        
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.startAnimation(hoverAnimation)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.startAnimation(reverseAnimation)
                    v.performClick() // Ensure click is registered
                    false
                }
                else -> false
            }
        }
    }
    
    private fun animateUIElements() {
        // Animate title with delay
        binding.titleTextView.visibility = View.INVISIBLE
        binding.subtitleTextView.visibility = View.INVISIBLE
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.titleTextView.visibility = View.VISIBLE
            val titleAnim = AnimationUtils.loadAnimation(this, R.anim.staggered_fade_in)
            binding.titleTextView.startAnimation(titleAnim)
            
            // Animate subtitle with a slight delay
            Handler(Looper.getMainLooper()).postDelayed({
                binding.subtitleTextView.visibility = View.VISIBLE
                val subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.staggered_fade_in)
                binding.subtitleTextView.startAnimation(subtitleAnim)
            }, 150)
        }, 300)
        
        // Animate cards with staggered timing
        binding.adminCard.visibility = View.INVISIBLE
        binding.categoryCard.visibility = View.INVISIBLE
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.adminCard.visibility = View.VISIBLE
            val adminCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
            binding.adminCard.startAnimation(adminCardAnim)
            
            // Animate category card with a slight delay for staggered effect
            Handler(Looper.getMainLooper()).postDelayed({
                binding.categoryCard.visibility = View.VISIBLE
                val categoryCardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_and_scale)
                binding.categoryCard.startAnimation(categoryCardAnim)
            }, 150)
        }, 600)
    }
    
    private fun <T> animateAndNavigate(view: View, activityClass: Class<T>) {
        // Disable the view to prevent multiple clicks
        view.isEnabled = false
        
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.card_scale)
        view.startAnimation(scaleAnimation)
        
        // Delay the navigation to allow the animation to complete
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, activityClass)
            // Add any extra data if needed
            // intent.putExtra("key", "value")
            
            startActivity(intent)
            
            // Apply a smooth transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            // Re-enable the view (though the activity will be finishing)
            view.isEnabled = true
        }, 200)
    }
}