package com.example.inventariolpy

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevolucionActivity : AppCompatActivity() {

    private var prestamoId: Int = 0
    private lateinit var herramientas: List<Herramienta>
    private lateinit var recyclerView: RecyclerView
    private val herramientasActualizadas = mutableListOf<Herramienta>()
    private var nombreEmpleado: String = ""

    private var empleadoNfcId: String? = null
    private var empleadoQrId: String? = null
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devolucion)

        // Obtener los datos del Intent
        prestamoId = intent.getIntExtra("prestamoId", 0)
        herramientas = intent.getParcelableArrayListExtra("herramientas") ?: listOf()
        nombreEmpleado = intent.getStringExtra("nombreEmpleado") ?: ""

        // Obtener NFC y QR del empleado
        empleadoNfcId = intent.getStringExtra("empleadoNfcId")
        empleadoQrId = intent.getStringExtra("empleadoQrId")

        recyclerView = findViewById(R.id.recyclerViewDevolucion)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = HerramientaDevolucionAdapter(herramientas) { herramienta, nuevoEstado ->
            herramienta.estado = nuevoEstado
            if (!herramientasActualizadas.contains(herramienta)) {
                herramientasActualizadas.add(herramienta)
            }
        }
        recyclerView.adapter = adapter

        val btnConfirmarDevolucion: Button = findViewById(R.id.btnConfirmarDevolucion)
        btnConfirmarDevolucion.setOnClickListener {
            mostrarPopupAutenticacion()
        }

        // Configuración del adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    private fun mostrarPopupAutenticacion() {
        // Verificar si todas las herramientas tienen un `RadioButton` seleccionado
        val herramientasSinSeleccion = herramientas.any { herramienta ->
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(herramientas.indexOf(herramienta)) as? HerramientaDevolucionAdapter.HerramientaViewHolder
            viewHolder?.let {
                !it.tieneRadioButtonSeleccionado() // Verificar si el ViewHolder tiene algún RadioButton seleccionado
            } ?: true // Considerar como no seleccionado si el ViewHolder es nulo
        }

        if (herramientasSinSeleccion) {
            Toast.makeText(this, "Debes seleccionar un estado para todas las herramientas", Toast.LENGTH_SHORT).show()
            return // Detener la ejecución si hay herramientas sin un estado seleccionado
        }

        // Continuar con el proceso de autenticación
        val opciones = if (nfcAdapter != null) {
            arrayOf("NFC", "QR")
        } else {
            arrayOf("QR")
        }

        AlertDialog.Builder(this)
            .setTitle("Método de Autenticación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> if (nfcAdapter != null) iniciarAutenticacionNFC() else iniciarEscaneoQR()
                    1 -> iniciarEscaneoQR()
                }
            }
            .setCancelable(true)
            .show()
    }
    private fun iniciarAutenticacionNFC() {
        val nfcAuthDialog = AlertDialog.Builder(this)
            .setTitle("Autenticación NFC")
            .setMessage("Acerca tu tarjeta NFC para confirmar la devolución")
            .setCancelable(false)
            .setNegativeButton("Cancelar") { dialog, _ ->
                nfcAdapter?.disableReaderMode(this)
                dialog.dismiss()
            }
            .show()

        nfcAdapter?.enableReaderMode(this, { tag ->
            val idTag = tag?.id?.joinToString("") { "%02x".format(it) }
            runOnUiThread {
                // Verifica si el ID del tag coincide con el NFC ID del empleado
                if (idTag == empleadoNfcId) {
                    Toast.makeText(this, "Autenticación NFC exitosa", Toast.LENGTH_SHORT).show()
                    registrarDevolucion() // Registramos la devolución si es exitosa
                    nfcAuthDialog.dismiss()
                } else {
                    Toast.makeText(this, "ID de tarjeta no coincide", Toast.LENGTH_SHORT).show()
                }
            }
        }, NfcAdapter.FLAG_READER_NFC_A, null)

        nfcAuthDialog.setOnDismissListener {
            nfcAdapter?.disableReaderMode(this)
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
            // Verifica si el QR escaneado coincide con el QR ID del empleado
            if (qrData == empleadoQrId) {
                registrarDevolucion() // Procede a registrar la devolución
            } else {
                Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun registrarDevolucion() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val herramientasParaPagare = mutableListOf<Herramienta>()
        var prestamoActivo = false

        herramientasActualizadas.forEach { herramienta ->
            when (herramienta.estado) {
                "Devuelta" -> {
                    // Actualizar herramienta como "Disponible" en la base de datos
                    val valuesHerramienta = ContentValues().apply {
                        put(DatabaseHelper.COL_ESTADO, "Disponible")
                    }
                    db.update(
                        DatabaseHelper.TABLE_HERRAMIENTAS,
                        valuesHerramienta,
                        "${DatabaseHelper.COL_ID_HERRAMIENTA}=?",
                        arrayOf(herramienta.id.toString())
                    )
                }
                "No Devuelta" -> {
                    prestamoActivo = true // Mantener el préstamo activo si hay herramientas no devueltas
                }
                "Rota", "Perdida" -> {
                    herramientasParaPagare.add(herramienta)
                    // Actualizar el estado de la herramienta en la base de datos
                    val valuesHerramienta = ContentValues().apply {
                        put(DatabaseHelper.COL_ESTADO, herramienta.estado)
                    }
                    db.update(
                        DatabaseHelper.TABLE_HERRAMIENTAS,
                        valuesHerramienta,
                        "${DatabaseHelper.COL_ID_HERRAMIENTA}=?",
                        arrayOf(herramienta.id.toString())
                    )
                }
            }
        }

        // Actualizar el estado del préstamo
        val valuesPrestamo = ContentValues().apply {
            put(
                DatabaseHelper.COL_ESTADO_PRESTAMO,
                if (prestamoActivo) "Activo" else "Devuelto"
            )
            put(DatabaseHelper.COL_FECHA_DEVOLUCION, System.currentTimeMillis().toString())
        }
        db.update(
            DatabaseHelper.TABLE_PRESTAMOS,
            valuesPrestamo,
            "${DatabaseHelper.COL_ID_PRESTAMO}=?",
            arrayOf(prestamoId.toString())
        )



        Toast.makeText(this, "Devolución procesada", Toast.LENGTH_SHORT).show()
        finish()
    }

    }
