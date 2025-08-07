package com.example.inventariolpy

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class HerramientaDialogAdapter(
    private val context: Context,
    private val herramientas: List<Herramienta>
) : BaseAdapter() {

    override fun getCount(): Int = herramientas.size

    override fun getItem(position: Int): Any = herramientas[position]

    override fun getItemId(position: Int): Long = herramientas[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_herramienta_dialog, parent, false)

        val imgHerramienta = view.findViewById<ImageView>(R.id.imgHerramienta)
        val tvNombre = view.findViewById<TextView>(R.id.tvNombreHerramienta)
        val tvCodigo = view.findViewById<TextView>(R.id.tvCodigoHerramienta)
        val tvFechaPrestamo = view.findViewById<TextView>(R.id.tvFechaPrestamo)

        val herramienta = herramientas[position]

        tvNombre.text = herramienta.nombre
        tvCodigo.text = "Código: ${herramienta.codigoInterno}"

        // Cargar imagen si está disponible
        if (herramienta.fotoHerramienta != null) {
            val bitmap = BitmapFactory.decodeByteArray(herramienta.fotoHerramienta, 0, herramienta.fotoHerramienta!!.size)
            imgHerramienta.setImageBitmap(bitmap)
        } else {
            imgHerramienta.setImageResource(R.drawable.ic_placeholder) // Imagen por defecto si no hay foto
        }

        return view
    }
}