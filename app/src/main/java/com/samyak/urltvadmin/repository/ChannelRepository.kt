package com.samyak.urltvadmin.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.samyak.urltvadmin.Channel

class ChannelRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("channels")

    fun addChannel(channel: Channel): Task<Void> {
        val id = database.push().key ?: return Tasks.forException(Exception("Failed to generate ID"))
        return database.child(id).setValue(channel.copy(id = id))
    }

    fun updateChannel(channel: Channel): Task<Void> {
        return channel.id?.let { id ->
            database.child(id).setValue(channel)
        } ?: Tasks.forException(Exception("Channel ID is null"))
    }

    fun deleteChannel(channelId: String): Task<Void> {
        return database.child(channelId).removeValue()
    }

    fun getChannelReference(): DatabaseReference = database
} 