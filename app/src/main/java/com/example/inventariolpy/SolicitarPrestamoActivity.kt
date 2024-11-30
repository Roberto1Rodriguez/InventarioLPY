package com.example.inventariolpy

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
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
import com.google.zxing.integration.android.IntentIntegrator
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
class SolicitarPrestamoActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private lateinit var btnConfirmarPrestamo: Button
    private lateinit var spinnerEmpleados: Spinner
    private var nfcAdapter: NfcAdapter? = null // Cambia a nullable

    private var empleadoId: Int = 0 // ID del empleado seleccionado
    private var nombreEmpleado: String = "" // Nombre del empleado seleccionado
    private var numeroContacto: String = "" // Número de contacto del empleado seleccionado
    private var empleadoNfcId: String? = null // ID de tarjeta NFC del empleado
    private var qrIdentificador: String? = null // Identificador QR del empleado
    private var prestamoConfirmado = false // Verificación de préstamo confirmado

    private lateinit var herramientasSeleccionadas: ArrayList<Herramienta> // Herramientas seleccionadas para el préstamo
    private lateinit var firmaEmpleadoBitmap: Bitmap // Firma almacenada del empleado

    private val STORAGE_PERMISSION_CODE = 1001
    private val CAMERA_PERMISSION_CODE = 100

    // Referencia al diálogo de autenticación NFC
    private var nfcAuthDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_prestamo)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Verificar si NFC está disponible
        if (nfcAdapter == null) {
            // NFC no está disponible, desactiva funcionalidades relacionadas
            Toast.makeText(this, "NFC no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        }

        // Verificar y solicitar permisos de almacenamiento y cámara si es necesario
        checkStoragePermissions()
        checkCameraPermission()

        // Obtener la lista de herramientas seleccionadas
        herramientasSeleccionadas = intent.getParcelableArrayListExtra("herramientasSeleccionadas") ?: arrayListOf()

        // Referencias a los componentes de la interfaz
        btnConfirmarPrestamo = findViewById(R.id.btnConfirmarPrestamo)
        spinnerEmpleados = findViewById(R.id.spinnerEmpleados)

        // Cargar los empleados en el Spinner
        cargarEmpleados()

        // Botón para confirmar el préstamo
        btnConfirmarPrestamo.setOnClickListener {
            if (!prestamoConfirmado) {
                mostrarPopupAutenticacion()
            } else {
                realizarPrestamo() // Si el préstamo ya fue confirmado
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Cerrar el diálogo de autenticación NFC si está abierto
        nfcAuthDialog?.dismiss()
        nfcAuthDialog = null
        // Desactivar el modo lector NFC
        nfcAdapter?.disableReaderMode(this)
    }

    private fun mostrarPopupAutenticacion() {
        val opciones = if (!empleadoNfcId.isNullOrEmpty()) {
            // Si el empleado tiene NFC registrado, mostrar ambas opciones
            arrayOf("NFC", "QR")
        } else {
            // Si el empleado no tiene NFC registrado, mostrar solo QR
            arrayOf("QR")
        }

        AlertDialog.Builder(this)
            .setTitle("Método de Autenticación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> if (!empleadoNfcId.isNullOrEmpty()) iniciarAutenticacionNFC() else iniciarEscaneoQR()
                    1 -> iniciarEscaneoQR()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun iniciarAutenticacionNFC() {
        nfcAuthDialog = AlertDialog.Builder(this)
            .setTitle("Autenticación NFC")
            .setMessage("Acerca tu tarjeta NFC para confirmar el préstamo")
            .setCancelable(false)
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Al presionar cancelar, desactivar el modo lector NFC y cerrar el diálogo
                nfcAdapter?.disableReaderMode(this)
                dialog.dismiss()
            }
            .show()

        nfcAdapter?.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null)

        nfcAuthDialog?.setOnDismissListener {
            nfcAdapter?.disableReaderMode(this)
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        val idTag = tag?.id?.joinToString("") { "%02x".format(it) }
        runOnUiThread {
            if (idTag == empleadoNfcId) {
                Toast.makeText(this, "Autenticación NFC exitosa", Toast.LENGTH_SHORT).show()
                prestamoConfirmado = true
                nfcAuthDialog?.dismiss() // Cerrar el diálogo de autenticación NFC
                realizarPrestamo() // Realiza el préstamo si la autenticación es exitosa
            } else {
                Toast.makeText(this, "ID de tarjeta no coincide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarEscaneoQR() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea el código QR de la identificación INE")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val qrData = result.contents
            Log.d("QR_SCAN", "Contenido del QR escaneado: $qrData")

            if (validarDatosQR(qrData)) {
                Toast.makeText(this, "Autenticación QR exitosa", Toast.LENGTH_SHORT).show()
                prestamoConfirmado = true
                realizarPrestamo()
            } else {
                Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun validarDatosQR(qrData: String): Boolean {
        Log.d("QR_SCAN", "Comparando QR escaneado: $qrData con QR almacenado: $qrIdentificador")
        return qrData == qrIdentificador // Compara el QR escaneado con el almacenado
    }

    private fun cargarEmpleados() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            DatabaseHelper.COL_ID_EMPLEADO,
            DatabaseHelper.COL_NOMBRE_EMPLEADO,
            DatabaseHelper.COL_CONTACTO,
            DatabaseHelper.COL_NFC_ID,
            DatabaseHelper.COL_QR_IDENTIFICADOR,
        )

        // Filtra solo los empleados activos
        val selection = "estado != ?" // Asume que tienes un campo `estado` en la tabla empleados
        val selectionArgs = arrayOf("Inactivo")

        val cursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val empleados = ArrayList<String>()
        val empleadoIds = ArrayList<Int>()
        val contactos = ArrayList<String>()
        val nfcIds = ArrayList<String?>()
        val qrIds = ArrayList<String?>()
        val firmas = ArrayList<Bitmap>()

        while (cursor.moveToNext()) {
            val empleadoId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO))
            val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
            val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO))
            val nfcId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NFC_ID))
            val qrId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QR_IDENTIFICADOR))

            empleados.add(nombreEmpleado)
            empleadoIds.add(empleadoId)
            contactos.add(contacto)
            nfcIds.add(nfcId)
            qrIds.add(qrId)
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
                empleadoNfcId = nfcIds[position]
                qrIdentificador = qrIds[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun realizarPrestamo() {
        if (!prestamoConfirmado) {
            Toast.makeText(this, "Debe autenticar primero", Toast.LENGTH_SHORT).show()
            Log.d("SolicitarPrestamo", "Intento de realizar préstamo sin autenticación")
            return
        }

        Log.d("SolicitarPrestamo", "Realizando préstamo")

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // Inserción del préstamo
        val valuesPrestamo = ContentValues().apply {
            put(DatabaseHelper.COL_EMPLEADO_ID, empleadoId)
            put(DatabaseHelper.COL_FECHA_PRESTAMO, System.currentTimeMillis().toString())
            put(DatabaseHelper.COL_ESTADO_PRESTAMO, "Activo")
        }
        val prestamoId = db.insert(DatabaseHelper.TABLE_PRESTAMOS, null, valuesPrestamo)

        if (prestamoId == -1L) {
            Toast.makeText(this, "Error al registrar el préstamo", Toast.LENGTH_SHORT).show()
            Log.d("SolicitarPrestamo", "Error al registrar el préstamo en la base de datos")
            return
        } else {
            Log.d("SolicitarPrestamo", "Préstamo registrado con ID: $prestamoId")
        }

        // Filtrar herramientas seleccionadas que estén disponibles
        val herramientasDisponibles = herramientasSeleccionadas.filter { herramienta ->
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_ESTADO} FROM ${DatabaseHelper.TABLE_HERRAMIENTAS} WHERE ${DatabaseHelper.COL_ID_HERRAMIENTA} = ?",
                arrayOf(herramienta.id.toString())
            )
            var disponible = false
            if (cursor.moveToFirst()) {
                disponible = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO)) == "Disponible"
            }
            cursor.close()
            disponible
        }

        // Inserción de cada herramienta disponible en la tabla intermedia y actualización de estado
        herramientasDisponibles.forEach { herramienta ->
            val valuesIntermedia = ContentValues().apply {
                put(DatabaseHelper.COL_PRESTAMO_ID, prestamoId)
                put(DatabaseHelper.COL_HERRAMIENTA_ID, herramienta.id)
            }
            val result = db.insert(DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS, null, valuesIntermedia)

            if (result == -1L) {
                Toast.makeText(this, "Error al registrar herramienta en el préstamo", Toast.LENGTH_SHORT).show()
                Log.d("SolicitarPrestamo", "Error al registrar la herramienta ${herramienta.nombre} en la tabla intermedia")
            } else {
                Log.d("SolicitarPrestamo", "Herramienta ${herramienta.nombre} registrada en el préstamo con ID: $prestamoId")
            }

            // Actualizar el estado de la herramienta a "Prestada"
            val valuesHerramienta = ContentValues().apply {
                put(DatabaseHelper.COL_ESTADO, "Prestada")
            }
            val updatedRows = db.update(DatabaseHelper.TABLE_HERRAMIENTAS, valuesHerramienta, "${DatabaseHelper.COL_ID_HERRAMIENTA}=?", arrayOf(herramienta.id.toString()))

            if (updatedRows == 0) {
                Log.d("SolicitarPrestamo", "Error al actualizar el estado de la herramienta ${herramienta.nombre}")
            } else {
                Log.d("SolicitarPrestamo", "Estado de la herramienta ${herramienta.nombre} actualizado a Prestada")
            }
        }

        if (herramientasDisponibles.isEmpty()) {
            Toast.makeText(this, "No hay herramientas disponibles para el préstamo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Préstamo realizado con éxito", Toast.LENGTH_SHORT).show()
        }

        Log.d("SolicitarPrestamo", "Préstamo realizado con éxito")
        finish()
    }



    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }
}