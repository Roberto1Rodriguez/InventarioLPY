package com.example.inventariolpy

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class EditarHerramientaActivity: AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etSerie: EditText
    private lateinit var etCodigoInterno: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio: EditText
    private lateinit var imgFotoHerramienta: ImageView
    private lateinit var btnCambiarFoto: Button
    private lateinit var spinnerEstado: Spinner

    private var herramientaId: Int = 0
    private var fotoHerramientaBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }

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
        imgFotoHerramienta = findViewById(R.id.imgFotoHerramienta)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)
        spinnerEstado = findViewById(R.id.spinnerEstadoHerramienta)


        // Obtener el ID de la herramienta a editar
        herramientaId = intent.getIntExtra("herramientaId", 0)

        // Cargar los datos de la herramienta desde la base de datos
        cargarDatosHerramienta()

        // Configurar botón para cambiar la foto
        btnCambiarFoto.setOnClickListener {
            mostrarOpcionesCaptura()
        }

        // Configurar botón de guardar
        val btnGuardar: Button = findViewById(R.id.btnGuardar)
        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }
    private fun cargarDatosHerramienta() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            null,
            "${DatabaseHelper.COL_ID_HERRAMIENTA} = ?",
            arrayOf(herramientaId.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE)))
            etMarca.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MARCA)))
            etModelo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO)))
            etSerie.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SERIE)))
            etCodigoInterno.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODIGO_INTERNO)))
            etDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPCION)))
            etPrecio.setText(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRECIO)).toString())

            // Configurar la selección del Spinner para el estado actual
            val estadoActual = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))

            // Configurar estados permitidos según el estado actual
            val estados = if (estadoActual == "Prestada") {
                // Si el estado actual es "Prestada", solo mostrar el estado actual y no permitir cambiarlo
                arrayOf("Prestada")
            } else {
                // Excluir "Prestada" si el estado no es "Prestada"
                resources.getStringArray(R.array.estados_herramienta).filter { it != "Prestada" }.toTypedArray()
            }

            val estadosAdaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
            estadosAdaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEstado.adapter = estadosAdaptador

            // Seleccionar el estado actual en el Spinner si está permitido
            val index = estados.indexOf(estadoActual)
            if (index >= 0) {
                spinnerEstado.setSelection(index)
            }

            // Deshabilitar el Spinner si el estado es "Prestada"
            if (estadoActual == "Prestada") {
                spinnerEstado.isEnabled = false
            }

            // Cargar la foto
            val fotoBlob = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO_HERRAMIENTA))
            if (fotoBlob != null) {
                val bitmap = BitmapFactory.decodeByteArray(fotoBlob, 0, fotoBlob.size)
                imgFotoHerramienta.setImageBitmap(bitmap)
                fotoHerramientaBitmap = bitmap
            }
        }
        cursor.close()
    }
    private fun mostrarOpcionesCaptura() {
        val options = arrayOf("Tomar Foto", "Elegir de la Galería")
        AlertDialog.Builder(this)
            .setTitle("Cambiar Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> capturarFoto()
                    1 -> seleccionarFotoDeGaleria()
                }
            }
            .show()
    }

    private fun capturarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun seleccionarFotoDeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    imgFotoHerramienta.setImageBitmap(bitmap)
                    fotoHerramientaBitmap = bitmap
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    imgFotoHerramienta.setImageBitmap(bitmap)
                    fotoHerramientaBitmap = bitmap
                }
            }
        }
    }

    private fun guardarCambios() {
        // Validar que los campos obligatorios no estén vacíos
        if (etNombre.text.isNullOrBlank()) {
            etNombre.error = "El nombre de la herramienta es obligatorio"
            etNombre.requestFocus()
            return
        }

        if (etMarca.text.isNullOrBlank()) {
            etMarca.error = "La marca es obligatoria"
            etMarca.requestFocus()
            return
        }

        if (etModelo.text.isNullOrBlank()) {
            etModelo.error = "El modelo es obligatorio"
            etModelo.requestFocus()
            return
        }

        if (etCodigoInterno.text.isNullOrBlank()) {
            etCodigoInterno.error = "El código interno es obligatorio"
            etCodigoInterno.requestFocus()
            return
        }

        val precio = etPrecio.text.toString().toDoubleOrNull()
        if (precio == null || precio < 0) {
            etPrecio.error = "El precio debe ser un valor numérico positivo"
            etPrecio.requestFocus()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val nuevoEstado = spinnerEstado.selectedItem.toString()

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, etNombre.text.toString().trim())
            put(DatabaseHelper.COL_MARCA, etMarca.text.toString().trim())
            put(DatabaseHelper.COL_MODELO, etModelo.text.toString().trim())
            put(DatabaseHelper.COL_SERIE, etSerie.text.toString().trim())
            put(DatabaseHelper.COL_CODIGO_INTERNO, etCodigoInterno.text.toString().trim())
            put(DatabaseHelper.COL_DESCRIPCION, etDescripcion.text.toString().trim())
            put(DatabaseHelper.COL_PRECIO, precio)
            put(DatabaseHelper.COL_ESTADO, nuevoEstado)
            if (fotoHerramientaBitmap != null) {
                put(DatabaseHelper.COL_FOTO_HERRAMIENTA, convertirBitmapABlob(fotoHerramientaBitmap))
            }
        }

        val rowsUpdated = db.update(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            values,
            "${DatabaseHelper.COL_ID_HERRAMIENTA} = ?",
            arrayOf(herramientaId.toString())
        )

        if (rowsUpdated > 0) {
            Toast.makeText(this, "Herramienta actualizada", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error al actualizar la herramienta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertirBitmapABlob(bitmap: Bitmap?): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}