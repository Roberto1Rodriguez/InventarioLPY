package com.example.inventariolpy

data class Empleado(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val curp: String,
    val rfc: String,
    val contacto: String?,
    val estado: String,
    val foto: ByteArray?,
    val nfcId: String?,  // Agregado para almacenar el ID NFC del empleado
    val qrId: String?    // Agregado para almacenar el identificador QR del empleado
)