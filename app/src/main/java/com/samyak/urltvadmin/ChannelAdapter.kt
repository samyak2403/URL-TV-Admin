package com.samyak.urltvadmin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.imageview.ShapeableImageView
import android.widget.ImageButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChannelAdapter(
    private var channelList: MutableList<Channel>,
    private val context: Context
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("channels")

    fun updateChannels(newChannels: List<Channel>) {
        channelList.clear()
        channelList.addAll(newChannels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channels, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channelList[position]
        holder.tvChannelName.text = channel.name
        holder.tvChannelLink.text = channel.link
        
        // Handle category display
        holder.tvChannelCategory.apply {
            text = channel.category.takeIf { it.isNotEmpty() } ?: "Uncategorized"
            visibility = if (channel.category.isEmpty()) View.GONE else View.VISIBLE
        }

        // Load logo using Glide
        if (!channel.logo.isNullOrEmpty()) {
            Glide.with(context)
                .load(channel.logo)
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(holder.ivChannelLogo)
        } else {
            holder.ivChannelLogo.setImageResource(R.drawable.ic_channel_placeholder)
        }

        holder.btnEdit.setOnClickListener {
            (context as AdminActivity).editChannel(channel)
        }

        holder.btnDelete.setOnClickListener {
            deleteChannel(channel)
        }
    }

    override fun getItemCount(): Int = channelList.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvChannelLink: TextView = itemView.findViewById(R.id.tvChannelLink)
        val tvChannelCategory: TextView = itemView.findViewById(R.id.tvChannelCategory)
        val ivChannelLogo: ShapeableImageView = itemView.findViewById(R.id.ivChannelLogo)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    private fun deleteChannel(channel: Channel) {
        // Show confirmation dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_channel)
            .setMessage(R.string.delete_channel_confirmation)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                performDelete(channel)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performDelete(channel: Channel) {
        channel.id?.let { channelId ->
            databaseReference.child(channelId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, R.string.success_channel_deleted, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, R.string.error_deleting_channel, Toast.LENGTH_SHORT).show()
                }
        }
    }
}