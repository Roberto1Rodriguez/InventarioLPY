package com.example.inventariolpy

import android.os.Parcel
import android.os.Parcelable

data class Herramienta(
    val id: Int,
    val nombre: String,
    val estado: String
) : Parcelable {
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