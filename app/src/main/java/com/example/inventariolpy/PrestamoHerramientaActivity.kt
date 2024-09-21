package com.example.inventariolpy

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.ByteArrayOutputStream
class PrestamoHerramientaActivity:AppCompatActivity() {
    private lateinit var signaturePad: SignaturePad
    private lateinit var btnGuardarFirma: Button
    private lateinit var btnLimpiarFirma: Button
    private lateinit var etNombreEmpleado: EditText
    private lateinit var etNumeroContacto: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prestamo_herramienta)

        // Referencias a los componentes de la interfaz
        signaturePad = findViewById(R.id.signaturePad)
        btnGuardarFirma = findViewById(R.id.btnGuardarFirma)
        btnLimpiarFirma = findViewById(R.id.btnLimpiarFirma)
        etNombreEmpleado = findViewById(R.id.etNombreEmpleado)
        etNumeroContacto = findViewById(R.id.etNumeroContacto)

        // Botón para limpiar la firma
        btnLimpiarFirma.setOnClickListener {
            signaturePad.clear()
        }

        // Botón para guardar el préstamo junto con la firma
        btnGuardarFirma.setOnClickListener {
            val nombreEmpleado = etNombreEmpleado.text.toString()
            val numeroContacto = etNumeroContacto.text.toString()

            if (signaturePad.isEmpty) {
                Toast.makeText(this, "Por favor, firma antes de continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir la firma a base64
            val signatureBitmap = signaturePad.signatureBitmap
            val signatureBase64 = convertBitmapToBase64(signatureBitmap)

            // Guardar los datos en la base de datos
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_NOMBRE_EMPLEADO, nombreEmpleado)
                put(DatabaseHelper.COL_CONTACTO, numeroContacto)
                put(DatabaseHelper.COL_FIRMA, signatureBase64)
                put(DatabaseHelper.COL_FECHA_PRESTAMO, System.currentTimeMillis().toString()) // Fecha actual en milisegundos
                // Aquí debes añadir el ID de la herramienta seleccionada para prestarla
            }

            val newRowId = db.insert(DatabaseHelper.TABLE_PRESTAMOS, null, values)

            if (newRowId != -1L) {
                Toast.makeText(this, "Préstamo registrado correctamente", Toast.LENGTH_SHORT).show()
                signaturePad.clear() // Limpiar la firma después de guardar
                etNombreEmpleado.text.clear()
                etNumeroContacto.text.clear()
            } else {
                Toast.makeText(this, "Error al registrar el préstamo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para convertir un bitmap a base64
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Función para convertir base64 a bitmap (si deseas cargar la firma en el futuro)
    private fun convertBase64ToBitmap(base64String: String): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }
}