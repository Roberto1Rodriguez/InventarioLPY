package com.example.inventariolpy

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
class HerramientasEscaneadasAdapter(
    private val context: Context,
    private val herramientas: MutableList<Herramienta>,
    private val onEliminarHerramienta: (Herramienta) -> Unit
) : RecyclerView.Adapter<HerramientasEscaneadasAdapter.HerramientaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HerramientaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_herramienta_escaneada, parent, false)
        return HerramientaViewHolder(view)
    }

    override fun onBindViewHolder(holder: HerramientaViewHolder, position: Int) {
        val herramienta = herramientas[position]
        holder.bind(herramienta)
    }

    override fun getItemCount(): Int = herramientas.size

    inner class HerramientaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewNombre: TextView = itemView.findViewById(R.id.textViewNombreHerramienta)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarHerramienta)
        private val imgHerramienta: ImageView = itemView.findViewById(R.id.imgHerramienta)

        fun bind(herramienta: Herramienta) {
            Log.d("HerramientasAdapter", "Binding herramienta: ${herramienta.nombre}, Código: ${herramienta.codigoInterno}")
            textViewNombre.text = "${herramienta.nombre} (Código: ${herramienta.codigoInterno ?: "N/A"})"

            val fotoByteArray = herramienta.fotoHerramienta
            if (fotoByteArray != null) {
                val fotoBitmap = BitmapFactory.decodeByteArray(fotoByteArray, 0, fotoByteArray.size)
                imgHerramienta.setImageBitmap(fotoBitmap)
            } else {
                imgHerramienta.setImageResource(R.drawable.ic_placeholder)
            }

            btnEliminar.setOnClickListener {
                onEliminarHerramienta(herramienta)
            }
        }
    }


}