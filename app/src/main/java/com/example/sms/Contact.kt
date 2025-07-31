package com.example.sms

data class Contact(
    val name: String,
    val phoneNumber: String,
    var isSelected: Boolean = false
)
