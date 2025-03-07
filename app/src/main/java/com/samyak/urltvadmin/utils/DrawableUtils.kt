package com.samyak.urltvadmin.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.PathShape
import android.graphics.drawable.shapes.RoundRectShape
import androidx.core.content.ContextCompat
import com.samyak.urltvadmin.R

/**
 * Utility class to create network status emoji drawables programmatically
 */
object DrawableUtils {

    /**
     * Loads a PNG image resource based on network status
     */
    fun loadNetworkStatusImage(context: Context, status: NetworkManager.ConnectionStatus): Drawable {
        val resourceId = when (status) {
            NetworkManager.ConnectionStatus.CONNECTED_EXCELLENT -> R.drawable.excellent
            NetworkManager.ConnectionStatus.CONNECTED_GOOD -> R.drawable.good
            NetworkManager.ConnectionStatus.CONNECTED_POOR -> R.drawable.poor
            NetworkManager.ConnectionStatus.DISCONNECTED -> R.drawable.disconnected
            NetworkManager.ConnectionStatus.CHECKING -> R.drawable.checking
        }
        
        return ContextCompat.getDrawable(context, resourceId)!!
    }

    /**
     * Creates an emoji-like face drawable for network status
     */
    fun createNetworkStatusDrawable(context: Context, status: NetworkManager.ConnectionStatus): Drawable {
        // Try to load PNG image first
        try {
            return loadNetworkStatusImage(context, status)
        } catch (e: Exception) {
            // Fallback to programmatically created drawable if PNG loading fails
            
            // Create the face background
            val faceBackground = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                
                // Set color based on status
                val backgroundColor = when (status) {
                    NetworkManager.ConnectionStatus.CONNECTED_EXCELLENT -> 
                        ContextCompat.getColor(context, android.R.color.transparent)
                    NetworkManager.ConnectionStatus.CONNECTED_GOOD -> 
                        ContextCompat.getColor(context, android.R.color.transparent)
                    NetworkManager.ConnectionStatus.CONNECTED_POOR -> 
                        ContextCompat.getColor(context, android.R.color.transparent)
                    NetworkManager.ConnectionStatus.DISCONNECTED -> 
                        ContextCompat.getColor(context, android.R.color.transparent)
                    else -> 
                        ContextCompat.getColor(context, R.color.transparent)
                }
                setColor(backgroundColor)
            }
            
            // Create eyes and mouth based on status
            val eyesAndMouth = createFacialFeatures(context, status)
            
            // Create signal strength indicator
            val signalStrength = createSignalStrengthIndicator(context, status)
            
            // Combine all layers
            return LayerDrawable(arrayOf(faceBackground, eyesAndMouth, signalStrength))
        }
    }
    
    /**
     * Creates facial features (eyes and mouth) based on network status
     */
    private fun createFacialFeatures(context: Context, status: NetworkManager.ConnectionStatus): Drawable {
        val facialFeatures = ShapeDrawable(OvalShape())
        val paint = facialFeatures.paint
        
        // Set up the paint for drawing
        paint.color = Color.TRANSPARENT
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        
        // Create a custom shape drawable that draws eyes and mouth
        return object : ShapeDrawable(OvalShape()) {
            override fun draw(canvas: Canvas) {
                // Set up paint for facial features
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
                paint.strokeWidth = bounds.width() * 0.05f
                
                val width = bounds.width().toFloat()
                val height = bounds.height().toFloat()
                val centerX = width / 2
                val centerY = height / 2
                
                // Draw eyes (same for all statuses)
                val eyeRadius = width * 0.08f
                val eyeY = centerY - height * 0.1f
                val leftEyeX = centerX - width * 0.2f
                val rightEyeX = centerX + width * 0.2f
                
                canvas.drawCircle(leftEyeX, eyeY, eyeRadius, paint)
                canvas.drawCircle(rightEyeX, eyeY, eyeRadius, paint)
                
                // Draw mouth based on status
                when (status) {
                    NetworkManager.ConnectionStatus.CONNECTED_EXCELLENT -> {
                        // Big smile
                        val mouthPath = Path()
                        val mouthRect = RectF(
                            centerX - width * 0.3f,
                            centerY,
                            centerX + width * 0.3f,
                            centerY + height * 0.3f
                        )
                        mouthPath.addArc(mouthRect, 0f, 180f)
                        canvas.drawPath(mouthPath, paint)
                    }
                    NetworkManager.ConnectionStatus.CONNECTED_GOOD -> {
                        // Smaller smile
                        val mouthPath = Path()
                        val mouthRect = RectF(
                            centerX - width * 0.25f,
                            centerY,
                            centerX + width * 0.25f,
                            centerY + height * 0.2f
                        )
                        mouthPath.addArc(mouthRect, 0f, 180f)
                        canvas.drawPath(mouthPath, paint)
                    }
                    NetworkManager.ConnectionStatus.CONNECTED_POOR -> {
                        // Straight line
                        canvas.drawLine(
                            centerX - width * 0.25f,
                            centerY + height * 0.15f,
                            centerX + width * 0.25f,
                            centerY + height * 0.15f,
                            paint
                        )
                    }
                    NetworkManager.ConnectionStatus.DISCONNECTED -> {
                        // Sad face
                        val mouthPath = Path()
                        val mouthRect = RectF(
                            centerX - width * 0.3f,
                            centerY + height * 0.1f,
                            centerX + width * 0.3f,
                            centerY + height * 0.4f
                        )
                        mouthPath.addArc(mouthRect, 180f, 180f)
                        canvas.drawPath(mouthPath, paint)
                    }
                    else -> {
                        // Neutral face (straight line)
                        canvas.drawLine(
                            centerX - width * 0.25f,
                            centerY + height * 0.15f,
                            centerX + width * 0.25f,
                            centerY + height * 0.15f,
                            paint
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Creates a signal strength indicator based on network status
     */
    private fun createSignalStrengthIndicator(context: Context, status: NetworkManager.ConnectionStatus): Drawable {
        return object : ShapeDrawable(OvalShape()) {
            override fun draw(canvas: Canvas) {
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = bounds.width() * 0.04f
                    color = Color.BLACK
                    alpha = 80 // Semi-transparent
                }
                
                val width = bounds.width().toFloat()
                val height = bounds.height().toFloat()
                val centerX = width / 2
                val centerY = height / 2
                
                // Only draw signal bars for connected states
                when (status) {
                    NetworkManager.ConnectionStatus.CONNECTED_EXCELLENT -> {
                        // Draw 3 signal bars
                        drawSignalBar(canvas, centerX, centerY, width * 0.15f, -45f, paint)
                        drawSignalBar(canvas, centerX, centerY, width * 0.25f, -45f, paint)
                        drawSignalBar(canvas, centerX, centerY, width * 0.35f, -45f, paint)
                    }
                    NetworkManager.ConnectionStatus.CONNECTED_GOOD -> {
                        // Draw 2 signal bars
                        drawSignalBar(canvas, centerX, centerY, width * 0.15f, -45f, paint)
                        drawSignalBar(canvas, centerX, centerY, width * 0.25f, -45f, paint)
                    }
                    NetworkManager.ConnectionStatus.CONNECTED_POOR -> {
                        // Draw 1 signal bar
                        drawSignalBar(canvas, centerX, centerY, width * 0.15f, -45f, paint)
                    }
                    NetworkManager.ConnectionStatus.DISCONNECTED -> {
                        // Draw X mark for disconnected
                        paint.style = Paint.Style.STROKE
                        canvas.drawLine(
                            centerX - width * 0.15f, 
                            centerY - height * 0.15f,
                            centerX + width * 0.15f,
                            centerY + height * 0.15f,
                            paint
                        )
                        canvas.drawLine(
                            centerX + width * 0.15f,
                            centerY - height * 0.15f,
                            centerX - width * 0.15f,
                            centerY + height * 0.15f,
                            paint
                        )
                    }
                    else -> {
                        // Draw question mark for unknown/checking
                        val path = Path()
                        path.moveTo(centerX - width * 0.05f, centerY - height * 0.1f)
                        path.quadTo(
                            centerX, centerY - height * 0.2f,
                            centerX + width * 0.05f, centerY - height * 0.1f
                        )
                        path.lineTo(centerX + width * 0.05f, centerY)
                        path.lineTo(centerX, centerY)
                        canvas.drawPath(path, paint)
                        
                        // Draw dot for question mark
                        canvas.drawCircle(centerX, centerY + height * 0.1f, width * 0.02f, paint)
                    }
                }
            }
            
            private fun drawSignalBar(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, angle: Float, paint: Paint) {
                val radians = Math.toRadians(angle.toDouble())
                val startX = centerX + (radius * 0.7f * Math.cos(radians)).toFloat()
                val startY = centerY + (radius * 0.7f * Math.sin(radians)).toFloat()
                val endX = centerX + (radius * Math.cos(radians)).toFloat()
                val endY = centerY + (radius * Math.sin(radians)).toFloat()
                
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
} 