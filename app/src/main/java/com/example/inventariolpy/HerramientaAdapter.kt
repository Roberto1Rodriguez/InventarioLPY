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

            // Abrir popup de detalles al presionar el elemento
            itemView.setOnClickListener {
                mostrarPopupDetalles(herramienta)
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

    private fun mostrarPopupDetalles(herramienta: Herramienta) {
        // Realizar la consulta para obtener los datos más recientes
        val dbHelper = DatabaseHelper(context)
        val herramientaActualizada = dbHelper.obtenerHerramientaPorId(herramienta.id)

        if (herramientaActualizada == null) {
            Toast.makeText(context, "No se encontraron datos para esta herramienta", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.activity_editar_herramienta, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etMarca = dialogView.findViewById<EditText>(R.id.etMarca)
        val etModelo = dialogView.findViewById<EditText>(R.id.etModelo)
        val etSerie = dialogView.findViewById<EditText>(R.id.etSerie)
        val etCodigoInterno = dialogView.findViewById<EditText>(R.id.etCodigoInterno)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecio)
        val spinnerEstado = dialogView.findViewById<Spinner>(R.id.spinnerEstadoHerramienta)
        val imgFoto = dialogView.findViewById<ImageView>(R.id.imgFotoHerramienta)
        val btnCambiarFoto = dialogView.findViewById<Button>(R.id.btnCambiarFoto)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardar)
        val btnEditarCancelar = dialogView.findViewById<ImageButton>(R.id.btnEditarCancelar)

        // Configurar campos con los datos más recientes
        etNombre.setText(herramientaActualizada.nombre)
        etMarca.setText(herramientaActualizada.marca ?: "")
        etModelo.setText(herramientaActualizada.modelo ?: "")
        etSerie.setText(herramientaActualizada.serie ?: "")
        etCodigoInterno.setText(herramientaActualizada.codigoInterno ?: "")
        etDescripcion.setText(herramientaActualizada.descripcion ?: "")
        etPrecio.setText(herramientaActualizada.precio.toString())
        spinnerEstado.setSelection(context.resources.getStringArray(R.array.estados_herramienta).indexOf(herramientaActualizada.estado))

        if (herramientaActualizada.fotoHerramienta != null) {
            val fotoBitmap = BitmapFactory.decodeByteArray(herramientaActualizada.fotoHerramienta, 0, herramientaActualizada.fotoHerramienta.size)
            imgFoto.setImageBitmap(fotoBitmap)
        } else {
            imgFoto.setImageResource(R.drawable.ic_placeholder)
        }

        // Inicialmente, los campos están deshabilitados
        setFieldsEnabled(false, etNombre, etMarca, etModelo, etSerie, etCodigoInterno, etDescripcion, etPrecio, spinnerEstado, btnCambiarFoto)

        // Configurar funcionalidad del botón lápiz (editar/cancelar)
        var enEdicion = false
        btnEditarCancelar.setOnClickListener {
            enEdicion = !enEdicion
            setFieldsEnabled(enEdicion, etNombre, etMarca, etModelo, etSerie, etCodigoInterno, etDescripcion, etPrecio, spinnerEstado, btnCambiarFoto)
            btnEditarCancelar.setImageResource(if (enEdicion) R.drawable.close else R.drawable.edit)
            btnGuardar.visibility = if (enEdicion) View.VISIBLE else View.GONE
        }

        // Guardar los cambios
        btnGuardar.setOnClickListener {
            herramientaActualizada.nombre = etNombre.text.toString()
            herramientaActualizada.marca = etMarca.text.toString()
            herramientaActualizada.modelo = etModelo.text.toString()
            herramientaActualizada.serie = etSerie.text.toString()
            herramientaActualizada.codigoInterno = etCodigoInterno.text.toString()
            herramientaActualizada.descripcion = etDescripcion.text.toString()
            herramientaActualizada.precio = etPrecio.text.toString().toDouble()
            herramientaActualizada.estado = spinnerEstado.selectedItem.toString()

            dbHelper.actualizarHerramienta(herramientaActualizada)
            (context as ListaHerramientasActivity).actualizarListaHerramientas()
            dialog.dismiss()
        }

        dialog.show()
    }

    // Método para habilitar/deshabilitar los campos
    private fun setFieldsEnabled(
        enabled: Boolean,
        vararg fields: View
    ) {
        fields.forEach { field ->
            field.isEnabled = enabled
        }
    }
    fun actualizarLista(nuevaLista: List<Herramienta>) {
        herramientas = nuevaLista
        notifyDataSetChanged()
    }
}