package com.example.inventariolpy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HerramientaAdapter(
    private val herramientas: List<Herramienta>,
    private val listener: (List<Herramienta>) -> Unit
) : RecyclerView.Adapter<HerramientaAdapter.HerramientaViewHolder>() {

    // Lista para almacenar las herramientas seleccionadas
    private val herramientasSeleccionadas = mutableListOf<Herramienta>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HerramientaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_heramienta, parent, false)
        return HerramientaViewHolder(view)
    }

    override fun onBindViewHolder(holder: HerramientaViewHolder, position: Int) {
        val herramienta = herramientas[position]
        holder.bind(herramienta)
    }

    override fun getItemCount(): Int = herramientas.size

    inner class HerramientaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxHerramienta)
        private val textViewNombre: TextView = itemView.findViewById(R.id.textViewNombreHerramienta)

        fun bind(herramienta: Herramienta) {
            // Formatear el nombre de la herramienta, agregando " - Prestada" si está prestada
            val nombreCompleto = if (herramienta.estado == "Prestada") {
                "${herramienta.nombre} - Prestada"
            } else {
                herramienta.nombre
            }

            // Asignar el texto al TextView
            textViewNombre.text = nombreCompleto

            // Deshabilitar la herramienta si está prestada
            checkBox.isEnabled = herramienta.estado != "Prestada"

            // Configurar el CheckBox
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = herramientasSeleccionadas.contains(herramienta)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    herramientasSeleccionadas.add(herramienta)
                } else {
                    herramientasSeleccionadas.remove(herramienta)
                }
                listener(herramientasSeleccionadas)
            }
        }
        }
    }

