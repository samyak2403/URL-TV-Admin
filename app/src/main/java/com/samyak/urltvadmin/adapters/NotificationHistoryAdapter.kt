package com.samyak.urltvadmin.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.samyak.urltvadmin.R
import com.samyak.urltvadmin.models.NotificationHistoryItem

class NotificationHistoryAdapter(
    context: Context,
    private val items: List<NotificationHistoryItem>
) : ArrayAdapter<NotificationHistoryItem>(context, 0, items) {

    private data class ViewHolder(
        val titleText: TextView,
        val messageText: TextView,
        val targetText: TextView,
        val dateText: TextView,
        val notificationImage: ImageView,
        val imageContainer: View
    )

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification_history, parent, false)
            
            holder = ViewHolder(
                titleText = view.findViewById(R.id.titleText),
                messageText = view.findViewById(R.id.messageText),
                targetText = view.findViewById(R.id.targetText),
                dateText = view.findViewById(R.id.dateText),
                notificationImage = view.findViewById(R.id.notificationImage),
                imageContainer = view.findViewById(R.id.imageContainer)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = items[position]

        with(holder) {
            titleText.text = item.title
            messageText.text = item.message
            targetText.text = "To: ${item.targetCategory}"
            dateText.text = item.date

            if (item.imageUrl.isNotEmpty()) {
                imageContainer.visibility = View.VISIBLE
                Glide.with(context)
                    .load(item.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(8))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageContainer.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageContainer.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(notificationImage)
            } else {
                imageContainer.visibility = View.GONE
            }
        }

        return view
    }
} 