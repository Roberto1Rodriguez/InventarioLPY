package com.example.inventariolpy
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Log
import android.widget.ImageView

class RegistrarHerramientaActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etSerie: EditText
    private lateinit var etCodigoInterno: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio: EditText // Nuevo campo para el precio
    private lateinit var btnCapturarFoto: Button
    private lateinit var btnRegistrar: Button
    private lateinit var imgVistaPreviaHerramienta: ImageView // Vista previa de la foto

    private var fotoHerramientaBitmap: Bitmap? = null

    companion object {
        private const val PERMISO_CAMARA = 100
        private const val CODIGO_FOTO = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_herramienta)

        // Inicializar los campos
        etNombre = findViewById(R.id.etNombreHerramienta)
        etMarca = findViewById(R.id.etMarcaHerramienta)
        etModelo = findViewById(R.id.etModeloHerramienta)
        etSerie = findViewById(R.id.etSerieHerramienta)
        etCodigoInterno = findViewById(R.id.etCodigoInternoHerramienta)
        etDescripcion = findViewById(R.id.etDescripcionHerramienta)
        etPrecio = findViewById(R.id.etPrecioHerramienta)
        btnCapturarFoto = findViewById(R.id.btnCapturarFotoHerramienta)
        btnRegistrar = findViewById(R.id.btnRegistrarHerramienta)
        imgVistaPreviaHerramienta = findViewById(R.id.imgVistaPreviaHerramienta) // Inicialización

        solicitarPermisoCamara()

        btnCapturarFoto.setOnClickListener {
            capturarFotoHerramienta()
        }

        btnRegistrar.setOnClickListener {
            registrarHerramienta()
        }
    }

    private fun solicitarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISO_CAMARA)
        }
    }

    private fun capturarFotoHerramienta() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CODIGO_FOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_FOTO && resultCode == Activity.RESULT_OK) {
            fotoHerramientaBitmap = data?.extras?.get("data") as Bitmap
            imgVistaPreviaHerramienta.setImageBitmap(fotoHerramientaBitmap) // Actualizar la vista previa
            btnCapturarFoto.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
        }
    }

    private fun registrarHerramienta() {
        val nombre = etNombre.text.toString()
        val marca = etMarca.text.toString()
        val modelo = etModelo.text.toString()
        val serie = etSerie.text.toString()
        val codigoInterno = etCodigoInterno.text.toString()
        val descripcion = etDescripcion.text.toString()
        val precioText = etPrecio.text.toString()

        val precio = precioText.toDoubleOrNull()
        if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty() || serie.isEmpty() ||
            codigoInterno.isEmpty() || descripcion.isEmpty() || fotoHerramientaBitmap == null || precio == null) {
            Toast.makeText(
                this,
                "Completa todos los campos, captura la foto y asegúrate de ingresar un precio válido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, nombre)
            put(DatabaseHelper.COL_MARCA, marca)
            put(DatabaseHelper.COL_MODELO, modelo)
            put(DatabaseHelper.COL_SERIE, serie)
            put(DatabaseHelper.COL_CODIGO_INTERNO, codigoInterno)
            put(DatabaseHelper.COL_DESCRIPCION, descripcion)
            put(DatabaseHelper.COL_PRECIO, precio)
            put(DatabaseHelper.COL_ESTADO, "Disponible")
            put(DatabaseHelper.COL_FOTO_HERRAMIENTA, convertBitmapToByteArray(fotoHerramientaBitmap))
        }

        val newRowId = db.insert(DatabaseHelper.TABLE_HERRAMIENTAS, null, values)

        if (newRowId != -1L) {
            Toast.makeText(this, "Herramienta registrada exitosamente", Toast.LENGTH_SHORT).show()
            logDatosBaseDatos() // Llamada al método para mostrar los datos en el log
            limpiarCampos()
            finish()
        } else {
            Toast.makeText(this, "Error al registrar la herramienta", Toast.LENGTH_SHORT).show()
        }
    }
    private fun limpiarCampos() {
        etNombre.text.clear()
        etMarca.text.clear()
        etModelo.text.clear()
        etSerie.text.clear()
        etCodigoInterno.text.clear()
        etDescripcion.text.clear()
        etPrecio.text.clear()
        fotoHerramientaBitmap = null
        imgVistaPreviaHerramienta.setImageResource(R.drawable.ic_placeholder) // Resetear la imagen
    }
    private fun logDatosBaseDatos() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            null, // Obtener todas las columnas
            null,
            null,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
                val marca = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MARCA))
                val modelo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO))
                val serie = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SERIE))
                val codigoInterno =
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODIGO_INTERNO))
                val descripcion =
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPCION))
                val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRECIO))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))

                Log.d(
                    "HerramientasDB",
                    "ID: $id, Nombre: $nombre, Marca: $marca, Modelo: $modelo, Serie: $serie, " +
                            "Código Interno: $codigoInterno, Descripción: $descripcion, Precio: $precio, Estado: $estado"
                )
            } while (cursor.moveToNext())
        } else {
            Log.d("HerramientasDB", "No hay herramientas almacenadas en la base de datos.")
        }
        cursor.close()
    }
    private fun convertBitmapToByteArray(bitmap: Bitmap?): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}