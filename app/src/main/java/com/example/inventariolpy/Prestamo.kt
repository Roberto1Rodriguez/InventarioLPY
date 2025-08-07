package com.example.inventariolpy
data class Prestamo(
    val id: Int,
    val nombreEmpleado: String,
    val herramientas: List<String>,
    val fecha: String,
    val estado: String
)