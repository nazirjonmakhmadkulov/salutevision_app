package com.example.salutevision_app

data class MrzRecord(
    val doc_type_code: String,
    val name: String,
    val last_name: String,
    val gender: String,
    val nationality: String,
    val country: String,
    val birth_date: String,
    val personal_number: String,
    val document_number: String,
    val expiry_date: String
)