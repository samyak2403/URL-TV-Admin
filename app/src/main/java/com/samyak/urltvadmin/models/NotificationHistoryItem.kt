package com.samyak.urltvadmin.models

data class NotificationHistoryItem(
    val title: String,
    val message: String,
    val targetCategory: String,
    val date: String,
    val imageUrl: String = ""
) 