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
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var spinnerEmpleados: Spinner

    private var empleadoId: Int = 0 // ID del empleado seleccionado
    private var nombreEmpleado: String = "" // Nombre del empleado seleccionado
    private var numeroContacto: String = "" // Número de contacto del empleado seleccionado
    private var claveAccesoEmpleado: String = "" // Clave de acceso del empleado

    // Código de solicitud de permisos
    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_prestamo)

        // Verificar y solicitar permisos de almacenamiento si es necesario
        checkStoragePermissions()

        herramientaId = intent.getIntExtra("herramientaId", 0)

        // Referencias a los componentes de la interfaz
        signaturePad = findViewById(R.id.signaturePad)
        btnLimpiarFirma = findViewById(R.id.btnLimpiarFirma)
        btnConfirmarPrestamo = findViewById(R.id.btnConfirmarPrestamo)
        spinnerEmpleados = findViewById(R.id.spinnerEmpleados)

        // Cargar los empleados en el Spinner
        cargarEmpleados()

        // Botón para limpiar la firma
        btnLimpiarFirma.setOnClickListener {
            signaturePad.clear()
        }

        // Botón para confirmar el préstamo
        btnConfirmarPrestamo.setOnClickListener {
            if (empleadoId == 0 || herramientaId == 0) {
                Toast.makeText(this, "Selecciona un empleado y una herramienta antes de continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!signaturePad.isEmpty) {
                // Mostrar el diálogo para ingresar la clave de acceso
                mostrarDialogoClaveAcceso()
            } else {
                Toast.makeText(this, "Completa todos los campos y firma antes de continuar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mostrar diálogo para ingresar la clave de acceso del empleado
    private fun mostrarDialogoClaveAcceso() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clave_acceso, null)
        val etClaveAcceso = dialogView.findViewById<EditText>(R.id.etClaveAcceso)

        AlertDialog.Builder(this)
            .setTitle("Verificación de Clave de Acceso")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { dialog, which ->
                val claveIngresada = etClaveAcceso.text.toString()
                if (claveIngresada.isNotEmpty()) {
                    verificarClaveAcceso(claveIngresada)
                } else {
                    Toast.makeText(this, "Por favor ingresa la clave de acceso", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Verificar si la clave de acceso ingresada es correcta
    private fun verificarClaveAcceso(claveIngresada: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Obtener la clave de acceso almacenada para el empleado seleccionado
        val projection = arrayOf(DatabaseHelper.COL_CLAVE_ACCESO)
        val selection = "${DatabaseHelper.COL_ID_EMPLEADO} = ?"
        val selectionArgs = arrayOf(empleadoId.toString())

        val cursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            val claveAlmacenada = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLAVE_ACCESO))

            // Verificar si la clave ingresada coincide con la almacenada
            if (claveIngresada == claveAlmacenada) {
                // La clave es correcta, proceder con el préstamo
                val firmaBitmap = signaturePad.signatureBitmap
                val firmaBase64 = convertBitmapToBase64(firmaBitmap)
                generarPDFFormal(nombreEmpleado, numeroContacto, herramientaId, firmaBitmap)
                realizarPrestamo(empleadoId, firmaBase64)
            } else {
                // La clave es incorrecta
                Toast.makeText(this, "Clave de acceso incorrecta", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error al verificar la clave de acceso", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
    }

    // Función para registrar el préstamo
    private fun realizarPrestamo(empleadoId: Int, firma: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val valuesPrestamo = ContentValues().apply {
            put(DatabaseHelper.COL_EMPLEADO_ID, empleadoId)
            put(DatabaseHelper.COL_HERRAMIENTA_ID, herramientaId)
            put(DatabaseHelper.COL_FECHA_PRESTAMO, System.currentTimeMillis().toString())
            put(DatabaseHelper.COL_FIRMA, firma)
        }

        db.insert(DatabaseHelper.TABLE_PRESTAMOS, null, valuesPrestamo)

        val valuesHerramienta = ContentValues().apply {
            put(DatabaseHelper.COL_ESTADO, "Activo")
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

    // Función para cargar empleados desde la base de datos
    private fun cargarEmpleados() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(DatabaseHelper.COL_ID_EMPLEADO, DatabaseHelper.COL_NOMBRE_EMPLEADO, DatabaseHelper.COL_CONTACTO)
        val cursor = db.query(DatabaseHelper.TABLE_EMPLEADOS, projection, null, null, null, null, null)

        val empleados = ArrayList<String>()
        val empleadoIds = ArrayList<Int>()
        val contactos = ArrayList<String>()

        while (cursor.moveToNext()) {
            val empleadoId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO))
            val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
            val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO))

            empleados.add(nombreEmpleado)
            empleadoIds.add(empleadoId)
            contactos.add(contacto)
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, empleados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmpleados.adapter = adapter

        spinnerEmpleados.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                empleadoId = empleadoIds[position]
                nombreEmpleado = empleados[position]
                numeroContacto = contactos[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Función para generar el PDF
    private fun generarPDFFormal(nombreEmpleado: String, numeroContacto: String, herramientaId: Int, firmaBitmap: Bitmap) {
        val nombreHerramienta = obtenerNombreHerramientaDesdeDB(herramientaId)
        val sdf = SimpleDateFormat("dd_MM_yyyy_HHmmss", Locale.getDefault())
        val fechaActual = sdf.format(Date())

        if (nombreHerramienta != null) {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            val paint = Paint()
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("PAGARÉ DE RESPONSABILIDAD", 200f, 80f, paint)

            paint.isFakeBoldText = false
            val text1 = """
                A través de este pagaré, yo, $nombreEmpleado, con número de contacto $numeroContacto,
                me comprometo a cuidar la herramienta identificada como "$nombreHerramienta"
                y devolverla en las mismas condiciones en que fue prestada.
                
                En caso de pérdida, daño o no devolución de la herramienta, reconozco mi responsabilidad y me comprometo a cubrir
                los costos asociados para reparar o reemplazar la herramienta según sea necesario.
            """.trimIndent()

            val lines = text1.split("\n")
            var yPos = 120f
            for (line in lines) {
                canvas.drawText(line.trim(), 40f, yPos, paint)
                yPos += 20f
            }

            val scaledBitmap = Bitmap.createScaledBitmap(firmaBitmap, 200, 100, true)
            canvas.drawBitmap(scaledBitmap, 80f, yPos + 100f, paint)
            val text2 = """
                Firmado el: $fechaActual
                Nombre del responsable: $nombreEmpleado
            """.trimIndent()
            canvas.drawText(text2, 40f, yPos + 240f, paint)

            pdfDocument.finishPage(page)
            val pdfDir = File(filesDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

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
            Toast.makeText(this, "Error: No se pudo obtener el nombre de la herramienta.", Toast.LENGTH_SHORT).show()
        }
    }


    // Método para verificar y solicitar permisos de almacenamiento en tiempo de ejecución
    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permisos de almacenamiento concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permisos de almacenamiento denegados", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para convertir un bitmap a base64
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Función para obtener el nombre de la herramienta desde la base de datos
    private fun obtenerNombreHerramientaDesdeDB(herramientaId: Int): String? {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        var nombreHerramienta: String? = null

        val projection = arrayOf(DatabaseHelper.COL_NOMBRE)
        val selection = "${DatabaseHelper.COL_ID} = ?"
        val selectionArgs = arrayOf(herramientaId.toString())

        val cursor = db.query(DatabaseHelper.TABLE_HERRAMIENTAS, projection, selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            nombreHerramienta = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
        }
        cursor.close()

        return nombreHerramienta
    }
}