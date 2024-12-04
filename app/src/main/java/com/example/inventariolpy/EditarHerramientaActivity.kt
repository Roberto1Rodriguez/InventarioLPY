package com.example.inventariolpy

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class EditarHerramientaActivity: AppCompatActivity() {
    private lateinit var etNombre: EditText
    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etSerie: EditText
    private lateinit var etCodigoInterno: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio: EditText
    private lateinit var spinnerEstado: Spinner
    private lateinit var imgFotoHerramienta: ImageView
    private lateinit var btnCambiarFoto: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnEditarCancelar: ImageButton

    private var herramientaId: Int = 0
    private var fotoHerramientaBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_herramienta)

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etMarca = findViewById(R.id.etMarca)
        etModelo = findViewById(R.id.etModelo)
        etSerie = findViewById(R.id.etSerie)
        etCodigoInterno = findViewById(R.id.etCodigoInterno)
        etDescripcion = findViewById(R.id.etDescripcion)
        etPrecio = findViewById(R.id.etPrecio)
        spinnerEstado = findViewById(R.id.spinnerEstadoHerramienta)
        imgFotoHerramienta = findViewById(R.id.imgFotoHerramienta)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEditarCancelar = findViewById(R.id.btnEditarCancelar)

        // Obtener el ID de la herramienta y el modo solo visualización desde el Intent
        herramientaId = intent.getIntExtra("herramientaId", 0)
        val soloVisualizar = intent.getBooleanExtra("soloVisualizar", false)

        // Cargar datos de la herramienta
        cargarDatosHerramienta()

        // Si está en modo solo visualización (herramienta prestada)
        if (soloVisualizar) {
            setFieldsEnabled(false) // Deshabilitar campos
            btnEditarCancelar.visibility = View.GONE // Ocultar botón de editar/cancelar
            btnCambiarFoto.visibility=View.GONE
            btnGuardar.visibility = View.GONE // Ocultar botón de guardar
        } else {
            // Botón para habilitar/cancelar edición
            var enEdicion = false
            btnEditarCancelar.setOnClickListener {
                enEdicion = !enEdicion
                setFieldsEnabled(enEdicion)
                btnEditarCancelar.setImageResource(if (enEdicion) R.drawable.close else R.drawable.edit)
                btnGuardar.visibility = if (enEdicion) View.VISIBLE else View.GONE
            }

            // Botón para cambiar la foto
            btnCambiarFoto.setOnClickListener { seleccionarNuevaFoto() }

            // Botón para guardar cambios
            btnGuardar.setOnClickListener { guardarCambios() }
        }
    }

    private fun cargarDatosHerramienta() {
        val dbHelper = DatabaseHelper(this)
        val herramienta = dbHelper.obtenerHerramientaPorId(herramientaId)

        if (herramienta != null) {
            etNombre.setText(herramienta.nombre)
            etMarca.setText(herramienta.marca)
            etModelo.setText(herramienta.modelo)
            etSerie.setText(herramienta.serie)
            etCodigoInterno.setText(herramienta.codigoInterno)
            etDescripcion.setText(herramienta.descripcion)
            etPrecio.setText(herramienta.precio.toString())

            // Obtener los estados y filtrar "Prestada"
            val estados = resources.getStringArray(R.array.estados_herramienta).toMutableList()
            if (herramienta.estado != "Prestada") {
                estados.remove("Prestada") // Remueve "Prestada" solo si no es el estado actual
            }

            // Configurar opciones del spinner
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEstado.adapter = spinnerAdapter

            // Seleccionar el estado actual
            spinnerEstado.setSelection(spinnerAdapter.getPosition(herramienta.estado))

            // Cargar foto de la herramienta
            if (herramienta.fotoHerramienta != null) {
                fotoHerramientaBitmap = BitmapFactory.decodeByteArray(
                    herramienta.fotoHerramienta, 0, herramienta.fotoHerramienta.size
                )
                imgFotoHerramienta.setImageBitmap(fotoHerramientaBitmap)
            } else {
                imgFotoHerramienta.setImageResource(R.drawable.ic_placeholder)
            }

            // Establecer el estado inicial de los campos
            if (herramienta.estado == "Prestada") {
                setFieldsEnabled(false) // Deshabilitar todo si está prestada
            } else {
                spinnerEstado.isEnabled = false // Spinner deshabilitado inicialmente
                setFieldsEnabled(false) // Deshabilitar otros campos de manera inicial
            }
        }
    }

    private fun seleccionarNuevaFoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val uri = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            fotoHerramientaBitmap = bitmap
            imgFotoHerramienta.setImageBitmap(bitmap)
        }
    }

    private fun guardarCambios() {
        val dbHelper = DatabaseHelper(this)
        val herramientaActualizada = Herramienta(
            id = herramientaId,
            nombre = etNombre.text.toString(),
            marca = etMarca.text.toString(),
            modelo = etModelo.text.toString(),
            serie = etSerie.text.toString(),
            codigoInterno = etCodigoInterno.text.toString(),
            descripcion = etDescripcion.text.toString(),
            precio = etPrecio.text.toString().toDouble(),
            estado = spinnerEstado.selectedItem.toString(),
            fotoHerramienta = fotoHerramientaBitmap?.let { convertirBitmapABlob(it) } // Convertir la nueva foto a ByteArray
        )

        val rowsUpdated = dbHelper.actualizarHerramienta(herramientaActualizada)

        if (rowsUpdated) {
            Toast.makeText(this, "Herramienta actualizada correctamente", Toast.LENGTH_SHORT).show()
            finish() // Cierra la actividad
        } else {
            Toast.makeText(this, "Error al actualizar la herramienta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertirBitmapABlob(bitmap: Bitmap?): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    private fun setFieldsEnabled(enabled: Boolean) {
        val textColor = if (enabled) ContextCompat.getColor(this, R.color.white) else ContextCompat.getColor(this, R.color.gray)
        val buttoncolor = if (enabled) ContextCompat.getColor(this, R.color.Botones) else ContextCompat.getColor(this, R.color.gray)

        etNombre.isEnabled = enabled
        etNombre.setTextColor(textColor)

        etMarca.isEnabled = enabled
        etMarca.setTextColor(textColor)

        etModelo.isEnabled = enabled
        etModelo.setTextColor(textColor)

        etSerie.isEnabled = enabled
        etSerie.setTextColor(textColor)

        etCodigoInterno.isEnabled = enabled
        etCodigoInterno.setTextColor(textColor)

        etDescripcion.isEnabled = enabled
        etDescripcion.setTextColor(textColor)

        etPrecio.isEnabled = enabled
        etPrecio.setTextColor(textColor)

        // El spinner solo estará habilitado si está en modo edición y tiene opciones válidas
        spinnerEstado.isEnabled = enabled && spinnerEstado.adapter != null && spinnerEstado.adapter.count > 0

        // Botón para cambiar foto solo habilitado en modo edición
        btnCambiarFoto.isEnabled = enabled
        btnCambiarFoto.setBackgroundColor(buttoncolor)
    }
}