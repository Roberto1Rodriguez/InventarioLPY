package com.example.inventariolpy

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SolicitarPrestamoActivity : AppCompatActivity() {
    private var herramientaId: Int = 0
    private lateinit var signaturePad: SignaturePad
    private lateinit var btnLimpiarFirma: Button
    private lateinit var btnConfirmarPrestamo: Button

    // Código de solicitud de permisos
    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_prestamo)

        // Verificar y solicitar permisos de almacenamiento si es necesario
        checkStoragePermissions()

        herramientaId = intent.getIntExtra("herramientaId", 0)

        val etNombreEmpleado: EditText = findViewById(R.id.etNombreEmpleado)
        val etNumeroContacto: EditText = findViewById(R.id.etNumeroContacto)
        signaturePad = findViewById(R.id.signaturePad)
        btnLimpiarFirma = findViewById(R.id.btnLimpiarFirma)
        btnConfirmarPrestamo = findViewById(R.id.btnConfirmarPrestamo)

        btnLimpiarFirma.setOnClickListener {
            signaturePad.clear()
        }

        btnConfirmarPrestamo.setOnClickListener {
            val nombreEmpleado = etNombreEmpleado.text.toString()
            val numeroContacto = etNumeroContacto.text.toString()

            if (nombreEmpleado.isNotEmpty() && numeroContacto.isNotEmpty() && !signaturePad.isEmpty) {
                val firmaBitmap = signaturePad.signatureBitmap
                val firmaBase64 = convertBitmapToBase64(firmaBitmap)
                generarPDFFormal(nombreEmpleado, numeroContacto, herramientaId, firmaBitmap)
                realizarPrestamo(nombreEmpleado, numeroContacto, firmaBase64)
            } else {
                Toast.makeText(
                    this,
                    "Completa todos los campos y firma antes de continuar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun realizarPrestamo(nombreEmpleado: String, numeroContacto: String, firma: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val valuesPrestamo = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE_EMPLEADO, nombreEmpleado)
            put(DatabaseHelper.COL_CONTACTO, numeroContacto)
            put(DatabaseHelper.COL_HERRAMIENTA_ID, herramientaId)
            put(DatabaseHelper.COL_FECHA_PRESTAMO, System.currentTimeMillis().toString())
            put(DatabaseHelper.COL_FIRMA, firma)
        }

        db.insert(DatabaseHelper.TABLE_PRESTAMOS, null, valuesPrestamo)

        val valuesHerramienta = ContentValues().apply {
            put(DatabaseHelper.COL_ESTADO, "Prestada")
        }

        db.update(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            valuesHerramienta,
            "${DatabaseHelper.COL_ID}=?",
            arrayOf(herramientaId.toString())
        )

        Toast.makeText(this, "Préstamo realizado con éxito", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generarPDFFormal(
        nombreEmpleado: String,
        numeroContacto: String,
        herramientaId: Int,
        firmaBitmap: Bitmap
    ) {
        // Obtener el nombre de la herramienta desde la base de datos
        val nombreHerramienta = obtenerNombreHerramientaDesdeDB(herramientaId)

        // Obtener la fecha actual en formato "día de mes de año"
        val sdf =
            SimpleDateFormat("dd_MM_yyyy_HHmmss", Locale.getDefault()) // Para el nombre del archivo
        val fechaActual = sdf.format(Date())

        if (nombreHerramienta != null) {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4
            val page = pdfDocument.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            val paint = Paint()
            paint.textSize = 12f
            paint.isFakeBoldText = true // Para hacer el texto en negritas en algunas partes

            // Título
            canvas.drawText("PAGARÉ DE RESPONSABILIDAD", 200f, 80f, paint)

            paint.isFakeBoldText = false // Texto normal

            // Ajustar el texto en párrafos con el nombre de la herramienta
            val text1 = """
            A través de este pagaré, yo, $nombreEmpleado, con número de contacto $numeroContacto,
            me comprometo a cuidar la herramienta identificada como "$nombreHerramienta"
            y devolverla en las mismas condiciones en que fue prestada.
            
            En caso de pérdida, daño o no devolución de la herramienta, reconozco mi responsabilidad y me comprometo a cubrir
            los costos asociados para reparar o reemplazar la herramienta según sea necesario.
        """.trimIndent()

            // Función para dividir el texto en líneas basadas en el ancho disponible
            val maxWidth = 500 // Ancho máximo permitido
            val lines = text1.split("\n")
            var yPos = 120f // Posición vertical inicial

            for (line in lines) {
                // Dividimos el texto para que quepa en el ancho permitido
                val words = line.split(" ")
                var currentLine = ""
                for (word in words) {
                    val potentialLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val lineWidth = paint.measureText(potentialLine)
                    if (lineWidth > maxWidth) {
                        // Dibujar la línea actual si excede el ancho máximo
                        canvas.drawText(currentLine, 40f, yPos, paint)
                        yPos += 20f
                        currentLine = word // Empezar una nueva línea
                    } else {
                        currentLine = potentialLine
                    }
                }
                // Dibujar la última línea
                canvas.drawText(currentLine, 40f, yPos, paint)
                yPos += 20f
            }

            // Firma
            val scaledBitmap = Bitmap.createScaledBitmap(firmaBitmap, 200, 100, true)
            canvas.drawBitmap(scaledBitmap, 80f, yPos + 100f, paint)

            // Detalles adicionales con la fecha actual
            val text2 = """
            Firmado el: $fechaActual
            
            Nombre del responsable: $nombreEmpleado
        """.trimIndent()
            canvas.drawText(text2, 40f, yPos + 240f, paint)

            pdfDocument.finishPage(page)

            // Crear un directorio privado dentro de la aplicación para guardar los PDFs
            val pdfDir = File(filesDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs() // Crear el directorio si no existe
            }

            // Guardar el PDF con un nombre único (incluyendo la fecha)
            val file = File(pdfDir, "Pagare_${nombreEmpleado}_$fechaActual.pdf")

            try {
                pdfDocument.writeTo(FileOutputStream(file))
                Toast.makeText(this, "PDF generado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
            }

            pdfDocument.close()
        } else {
            Toast.makeText(
                this,
                "Error: No se pudo obtener el nombre de la herramienta.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Función para obtener el nombre de la herramienta desde la base de datos
    private fun obtenerNombreHerramientaDesdeDB(herramientaId: Int): String? {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        var nombreHerramienta: String? = null

        val projection = arrayOf(DatabaseHelper.COL_NOMBRE)
        val selection = "${DatabaseHelper.COL_ID} = ?"
        val selectionArgs = arrayOf(herramientaId.toString())

        val cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS, // Tabla
            projection, // Columnas a devolver
            selection, // WHERE clause
            selectionArgs, // WHERE arguments
            null, // groupBy
            null, // having
            null  // orderBy
        )

        if (cursor.moveToFirst()) {
            nombreHerramienta =
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
        }
        cursor.close()

        return nombreHerramienta
    }

    // Función para convertir un bitmap a base64
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Método para verificar y solicitar permisos de almacenamiento en tiempo de ejecución
    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Solicitar permisos de almacenamiento
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    // Manejar la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos de almacenamiento concedidos", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Permisos de almacenamiento denegados", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}