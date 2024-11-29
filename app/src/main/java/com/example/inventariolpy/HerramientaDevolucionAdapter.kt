package com.example.inventariolpy

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HerramientaDevolucionAdapter(
    private val herramientas: List<Herramienta>,
    private val onEstadoSeleccionado: (Herramienta, String) -> Unit
) : RecyclerView.Adapter<HerramientaDevolucionAdapter.HerramientaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HerramientaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_herramienta_devolucion, parent, false)
        return HerramientaViewHolder(view)
    }

    override fun onBindViewHolder(holder: HerramientaViewHolder, position: Int) {
        holder.bind(herramientas[position])
    }

    override fun getItemCount(): Int = herramientas.size

    inner class HerramientaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreHerramienta: TextView = itemView.findViewById(R.id.tvNombreHerramienta)
        private val radioButtonDevuelta: RadioButton = itemView.findViewById(R.id.rbDevuelta)
        private val radioButtonNoDevuelta: RadioButton = itemView.findViewById(R.id.rbNoDevuelta)
        private val radioButtonRota: RadioButton = itemView.findViewById(R.id.rbRota)
        private val radioButtonPerdida: RadioButton = itemView.findViewById(R.id.rbPerdida)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.rgEstadoHerramienta)

        fun tieneRadioButtonSeleccionado(): Boolean {
            return radioGroup.checkedRadioButtonId != -1 // Retorna `true` si hay un `RadioButton` seleccionado
        }
        @SuppressLint("SetTextI18n")
        fun bind(herramienta: Herramienta) {
            tvNombreHerramienta.text = "${herramienta.nombre} (Código: ${herramienta.codigoInterno ?: "Sin código"}) - ${herramienta.marca ?: "Sin marca"}"

            // Seleccionar el estado actual
            when (herramienta.estado) {
                "Disponible" -> {
                    radioButtonDevuelta.isChecked = true
                    deshabilitarRadioGroup()
                }
                "Rota" -> {
                    radioButtonRota.isChecked = true
                    deshabilitarRadioGroup()
                }
                "Perdida" -> {
                    radioButtonPerdida.isChecked = true
                    deshabilitarRadioGroup()
                }
                else -> {
                    radioButtonNoDevuelta.isChecked = false
                    habilitarRadioGroup()
                }
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val nuevoEstado = when (checkedId) {
                    R.id.rbDevuelta -> "Devuelta"
                    R.id.rbNoDevuelta -> "No Devuelta"
                    R.id.rbRota -> "Rota"
                    R.id.rbPerdida -> "Perdida"
                    else -> herramienta.estado
                }
                onEstadoSeleccionado(herramienta, nuevoEstado)
            }
        }

        private fun deshabilitarRadioGroup() {
            radioGroup.isEnabled = false
            for (i in 0 until radioGroup.childCount) {
                val view = radioGroup.getChildAt(i)
                view.isEnabled = false
            }
        }

        private fun habilitarRadioGroup() {
            radioGroup.isEnabled = true
            for (i in 0 until radioGroup.childCount) {
                val view = radioGroup.getChildAt(i)
                view.isEnabled = true
            }
        }
    }
}