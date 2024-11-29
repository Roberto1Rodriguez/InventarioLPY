package com.example.inventariolpy

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class HerramientaAdapter(

private val context: Context,
private var herramientas: List<Herramienta>,
private val listener: (List<Herramienta>) -> Unit
) : RecyclerView.Adapter<HerramientaAdapter.HerramientaViewHolder>() {

    private val herramientasSeleccionadas = mutableListOf<Herramienta>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HerramientaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_herramienta, parent, false)
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
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarHerramienta)
        private val imgHerramienta: ImageView = itemView.findViewById(R.id.imgHerramienta)

        fun bind(herramienta: Herramienta) {
            // Configurar nombre y estado
            val codigo = herramienta.codigoInterno?.takeIf { it.isNotEmpty() } ?: "Sin código"
            val nombreCompleto = when (herramienta.estado) {
                "Prestada" -> "${herramienta.nombre} (Código: $codigo) - Prestada"
                "Rota" -> "${herramienta.nombre} (Código: $codigo) - Rota"
                "Perdida" -> "${herramienta.nombre} (Código: $codigo) - Perdida"
                else -> "${herramienta.nombre} (Código: $codigo)"
            }
            textViewNombre.text = nombreCompleto

            // Configurar checkbox
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

            // Mostrar imagen de la herramienta
            val fotoByteArray = herramienta.fotoHerramienta
            if (fotoByteArray != null) {
                val fotoBitmap = BitmapFactory.decodeByteArray(fotoByteArray, 0, fotoByteArray.size)
                imgHerramienta.setImageBitmap(fotoBitmap)
            } else {
                imgHerramienta.setImageResource(R.drawable.ic_placeholder) // Imagen por defecto
            }

            // Botón eliminar
            btnEliminar.setOnClickListener {
                eliminarHerramienta(herramienta)
            }

            // Abrir la actividad de edición al presionar el elemento
            itemView.setOnClickListener {
                abrirActividadEdicion(herramienta)
            }
        }
    }

    private fun eliminarHerramienta(herramienta: Herramienta) {
        if (herramienta.estado == "Prestada") {
            Toast.makeText(context, "No se puede eliminar una herramienta que está prestada", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar la herramienta ${herramienta.nombre}?")
                .setPositiveButton("Sí") { _, _ ->
                    val dbHelper = DatabaseHelper(context)
                    dbHelper.eliminarHerramientaLogico(herramienta.id)
                    Toast.makeText(context, "Herramienta eliminada", Toast.LENGTH_SHORT).show()
                    (context as ListaHerramientasActivity).actualizarListaHerramientas()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
    private fun abrirActividadEdicion(herramienta: Herramienta) {
        val intent = Intent(context, EditarHerramientaActivity::class.java)
        intent.putExtra("herramientaId", herramienta.id)
        intent.putExtra("soloVisualizar", herramienta.estado == "Prestada") // Si está prestada, solo visualización
        (context as Activity).startActivityForResult(intent, EDITAR_HERRAMIENTA_REQUEST_CODE)
    }

    fun actualizarLista(nuevaLista: List<Herramienta>) {
        herramientas = nuevaLista
        notifyDataSetChanged()
    }

    companion object {
        const val EDITAR_HERRAMIENTA_REQUEST_CODE = 1
    }
}