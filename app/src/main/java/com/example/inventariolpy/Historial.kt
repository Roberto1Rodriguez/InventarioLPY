package com.example.inventariolpy
data class Historial(
    val id: Int,
    val nombreHerramienta: String,
    val codigoHerramienta: String,
    val fechaPrestamo: String,
    val fechaDevolucion: String?,
    val estadoPrestamo: String,
    val estadoHerramienta:String,
)