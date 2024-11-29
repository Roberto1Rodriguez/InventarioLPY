package com.example.inventariolpy

import android.os.Parcel
import android.os.Parcelable

data class Herramienta(
    val id: Int,
    var nombre: String,
    var estado: String, // Permite la modificación del estado
    var marca: String? = null,
    var modelo: String? = null,
    var serie: String? = null,
    var codigoInterno: String? = null,
    val fotoHerramienta: ByteArray? = null, // Para imágenes
    var descripcion: String? = null,
    var precio: Double = 0.0 // Por defecto es 0 si no se proporciona un valor
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()?: "",
        parcel.readString()?: "",
        parcel.readString()?: "",
        parcel.readString()?: "",
        parcel.createByteArray(),
        parcel.readString()?: "",
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(nombre)
        parcel.writeString(estado)
        parcel.writeString(marca)
        parcel.writeString(modelo)
        parcel.writeString(serie)
        parcel.writeString(codigoInterno)
        parcel.writeByteArray(fotoHerramienta)
        parcel.writeString(descripcion)
        parcel.writeDouble(precio)
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