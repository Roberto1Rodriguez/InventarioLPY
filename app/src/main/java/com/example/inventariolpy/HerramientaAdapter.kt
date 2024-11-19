package com.example.inventariolpy

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView

class HerramientaAdapter(
    private val context: Context,
    private var herramientas: List<Herramienta>,
    private val listener: (List<Herramienta>) -> Unit
) : RecyclerView.Adapter<HerramientaAdapter.HerramientaViewHolder>() {

    // Lista para almacenar las herramientas seleccionadas
    private val herramientasSeleccionadas = mutableListOf<Herramienta>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HerramientaViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_herramienta, parent, false)
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
        private val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarHerramienta) // Nuevo botón de edición

        fun bind(herramienta: Herramienta) {
            val codigo = herramienta.codigoInterno?.takeIf { it.isNotEmpty() } ?: "Sin código"
            val nombreCompleto = when (herramienta.estado) {
                "Prestada" -> "${herramienta.nombre} (Código: $codigo) - Prestada"
                "Rota" -> "${herramienta.nombre} (Código: $codigo) - Rota"
                "Perdida" -> "${herramienta.nombre} (Código: $codigo) - Perdida"
                else -> "${herramienta.nombre} (Código: $codigo)"
            }
            textViewNombre.text = nombreCompleto
            checkBox.isEnabled = herramienta.estado != "Prestada" && herramienta.estado != "Rota" && herramienta.estado != "Perdida"
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

            btnEditar.setOnClickListener {
                editarHerramienta(herramienta)
            }
        }
    }
    private fun editarHerramienta(herramienta: Herramienta) {
        if (herramienta.estado == "Prestada") {
            Toast.makeText(context, "No se puede editar una herramienta que está prestada", Toast.LENGTH_SHORT).show()
        } else {
            mostrarDialogoClaveAcceso(herramienta)
        }
    }

    private fun mostrarDialogoClaveAcceso(herramienta: Herramienta) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Ingrese la clave de acceso"
        }

        AlertDialog.Builder(context)
            .setTitle("Verificación de Clave")
            .setMessage("Ingrese la clave de acceso para editar la herramienta")
            .setView(input)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val claveIngresada = input.text.toString()
                if (claveIngresada == "Balance9731") {
                    val intent = Intent(context, EditarHerramientaActivity::class.java)
                    intent.putExtra("herramientaId", herramienta.id)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Clave incorrecta", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    // Método para actualizar la lista de herramientas mostradas
    fun actualizarLista(nuevaLista: List<Herramienta>) {
        herramientas = nuevaLista
        notifyDataSetChanged() // Notifica al adaptador que los datos han cambiado
    }
}
