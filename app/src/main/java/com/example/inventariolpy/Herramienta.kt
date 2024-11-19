package com.example.inventariolpy

import android.os.Parcel
import android.os.Parcelable

data class Herramienta(
    val id: Int,
    val nombre: String,
    var estado: String, // Permite la modificación del estado
    val marca: String? = null,
    val modelo: String? = null,
    val serie: String? = null,
    val codigoInterno: String? = null,
    val fotoHerramienta: ByteArray? = null, // Para imágenes
    val descripcion: String? = null,
    val precio: Double = 0.0 // Por defecto es 0 si no se proporciona un valor
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(nombre)
        parcel.writeString(estado)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Herramienta> {
        override fun createFromParcel(parcel: Parcel): Herramienta {
            return Herramienta(parcel)
        }

        override fun newArray(size: Int): Array<Herramienta?> {
            return arrayOfNulls(size)
        }
    }
}