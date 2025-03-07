package com.samyak.urltvadmin.model

data class Category(
    val id: String,
    val name: String,
    val description: String = ""
) {
    constructor() : this("", "", "")
} 