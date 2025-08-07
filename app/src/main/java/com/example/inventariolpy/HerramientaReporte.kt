package com.example.inventariolpy

data class HerramientaReporte(
    val id: Int,
    val codigoInterno: String,
    val nombre: String,
    val estado: String,
    val marca: String,
    val modelo: String,
    val precio: Double,
    val nombreEmpleado: String,
    val fechaPrestamo: String,
    val activo: Int,
    val firmaEmpleado: ByteArray? = null // 👈 nueva propiedad opcional

)