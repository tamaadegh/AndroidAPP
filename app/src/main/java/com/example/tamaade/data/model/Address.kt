package com.example.tamaade.data.model

data class Address(
    val id: String,
    val type: String, // "Home" or "Trotro Station"
    val fullName: String,
    val phone: String,
    val address: String,
    val city: String,
    var isSelected: Boolean = false
)