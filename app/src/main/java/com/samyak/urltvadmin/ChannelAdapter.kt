package com.samyak.urltvadmin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samyak.urltvadmin.databinding.ItemChannelsBinding
import com.samyak.urltvadmin.repository.ChannelRepository

class ChannelAdapter(
    private var channelList: MutableList<Channel>,
    private val context: Context
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val channelRepository = ChannelRepository()

    fun updateChannels(newChannels: List<Channel>) {
        channelList.clear()
        channelList.addAll(newChannels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channelList[position]
        holder.bind(channel)
    }

    override fun getItemCount(): Int = channelList.size

    inner class ChannelViewHolder(private val binding: ItemChannelsBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(channel: Channel) {
            binding.tvChannelName.text = channel.name
            binding.tvChannelLink.text = channel.link
            
            // Handle category display
            binding.tvChannelCategory.apply {
                text = channel.category.takeIf { it.isNotEmpty() } ?: "Uncategorized"
                visibility = if (channel.category.isEmpty()) View.GONE else View.VISIBLE
            }

            // Load logo using Glide
            if (!channel.logo.isNullOrEmpty()) {
                Glide.with(context)
                    .load(channel.logo)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(binding.ivChannelLogo)
            } else {
                binding.ivChannelLogo.setImageResource(R.drawable.ic_channel_placeholder)
            }

            binding.btnEdit.setOnClickListener {
                (context as AdminActivity).editChannel(channel)
            }

            binding.btnDelete.setOnClickListener {
                deleteChannel(channel)
            }
        }
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
            channelRepository.deleteChannel(channelId)
                .addOnSuccessListener {
                    Toast.makeText(context, R.string.success_channel_deleted, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, R.string.error_deleting_channel, Toast.LENGTH_SHORT).show()
                }
        }
    }
}