package com.example.inventariolpy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.zxing.integration.android.IntentIntegrator

class AgregarEmpleadoActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private lateinit var etNombre: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etCurp: EditText
    private lateinit var etRfc: EditText
    private lateinit var etContacto: EditText
    private lateinit var btnCapturarFoto: Button
    private lateinit var btnCapturarFirma: Button
    private lateinit var btnAutenticacion: Button
    private lateinit var btnAgregarEmpleado: Button

    private var fotoEmpleadoBitmap: Bitmap? = null
    private var firmaEmpleadoBitmap: Bitmap? = null
    private var nfcId: String? = null
    private var qrIdentificador: String? = null
    private var nfcAdapter: NfcAdapter? = null

    companion object {
        private const val PERMISO_CAMARA = 100
        private const val CODIGO_FOTO = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_empleado)

        etNombre = findViewById(R.id.etNombre)
        etDireccion = findViewById(R.id.etDireccion)
        etCurp = findViewById(R.id.etCurp)
        etRfc = findViewById(R.id.etRfc)
        etContacto = findViewById(R.id.etContacto)
        btnCapturarFoto = findViewById(R.id.btnCapturarFoto)
        btnCapturarFirma = findViewById(R.id.btnCapturarFirma)
        btnAutenticacion = findViewById(R.id.btnAutenticacion)
        btnAgregarEmpleado = findViewById(R.id.btnAgregarEmpleado)

        solicitarPermisoCamara()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        btnCapturarFoto.setOnClickListener {
            capturarFoto()
        }

        btnCapturarFirma.setOnClickListener {
            capturarFirma()
        }

        btnAutenticacion.setOnClickListener {
            mostrarDialogoAutenticacion()
        }

        btnAgregarEmpleado.setOnClickListener {
            agregarEmpleado()
        }
    }

    private fun solicitarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISO_CAMARA)
        }
    }

    private fun capturarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CODIGO_FOTO)
    }

    private fun capturarFirma() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_captura_firma)

        // Establece dimensiones para hacer que el diálogo se vea horizontal
        val params = WindowManager.LayoutParams()
        params.copyFrom(dialog.window?.attributes) // Copia los atributos actuales de la ventana
        params.width = WindowManager.LayoutParams.MATCH_PARENT // O un tamaño fijo como 800
        params.height = WindowManager.LayoutParams.WRAP_CONTENT // Ajusta según sea necesario
        dialog.window?.attributes = params

        val btnGuardarFirma = dialog.findViewById<Button>(R.id.btnGuardarFirma)
        val signaturePad = dialog.findViewById<SignaturePad>(R.id.signaturePad)

        btnGuardarFirma.setOnClickListener {
            firmaEmpleadoBitmap = signaturePad.signatureBitmap
            btnCapturarFirma.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
            dialog.dismiss()
        }

        dialog.show()
    }
    private lateinit var nfcDialog: AlertDialog

    private fun iniciarCapturaNFC() {
        nfcDialog = AlertDialog.Builder(this)
            .setTitle("Registrar Tarjeta NFC")
            .setMessage("Acerca tu tarjeta NFC para registrar la clave del empleado")
            .setCancelable(false)
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Al presionar cancelar, desactivar el modo lector NFC y cerrar el diálogo
                nfcAdapter?.disableReaderMode(this)
                dialog.dismiss()
            }
            .show()

        // Habilita el modo de lector NFC
        nfcAdapter?.enableReaderMode(
            this,
            this, // `this` ahora se refiere a la actividad que implementa ReaderCallback
            NfcAdapter.FLAG_READER_NFC_A,
            null
        )
    }

    override fun onTagDiscovered(tag: Tag?) {
        val idTag = tag?.id?.joinToString("") { "%02x".format(it) }
        runOnUiThread {
            nfcId = idTag
            Toast.makeText(this, "Tarjeta NFC registrada: $nfcId", Toast.LENGTH_SHORT).show()

            // Desactiva el modo lector NFC y cierra el diálogo cuando se detecta la tarjeta
            nfcAdapter?.disableReaderMode(this)
            if (nfcDialog.isShowing) {
                nfcDialog.dismiss()
            }

            iniciarCapturaQR() // Inicia el escaneo del QR después del NFC
        }
    }

    private fun iniciarCapturaQR() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.setPrompt("Escanea el código QR")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_FOTO && resultCode == Activity.RESULT_OK) {
            fotoEmpleadoBitmap = data?.extras?.get("data") as Bitmap
            btnCapturarFoto.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
        }

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                qrIdentificador = result.contents
                btnAutenticacion.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
                Toast.makeText(this, "Código QR capturado", Toast.LENGTH_SHORT).show()
                Log.d("QR_SCAN", "Contenido del QR escaneado: $qrIdentificador")

            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAutenticacion() {
        val opciones = arrayOf("Capturar NFC", "Escanear QR")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Autenticación")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> iniciarCapturaNFC()
                1 -> iniciarCapturaQR()
            }
        }
        builder.show()
    }

    private fun agregarEmpleado() {
        if (etNombre.text.isEmpty() || etDireccion.text.isEmpty() || etCurp.text.isEmpty() || etRfc.text.isEmpty() || etContacto.text.isEmpty() ||
            fotoEmpleadoBitmap == null || firmaEmpleadoBitmap == null || (nfcId == null && qrIdentificador == null)
        ) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE_EMPLEADO, etNombre.text.toString())
            put(DatabaseHelper.COL_DIRECCION, etDireccion.text.toString())
            put(DatabaseHelper.COL_CURP, etCurp.text.toString())
            put(DatabaseHelper.COL_RFC, etRfc.text.toString())
            put(DatabaseHelper.COL_CONTACTO, etContacto.text.toString())
            put(DatabaseHelper.COL_NFC_ID, nfcId)
            put(DatabaseHelper.COL_QR_IDENTIFICADOR, qrIdentificador)
            put(DatabaseHelper.COL_FIRMA, convertBitmapToByteArray(firmaEmpleadoBitmap))
            put(DatabaseHelper.COL_FOTO, convertBitmapToByteArray(fotoEmpleadoBitmap))
        }

        db.insert(DatabaseHelper.TABLE_EMPLEADOS, null, values)
        Toast.makeText(this, "Empleado registrado exitosamente", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap?): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}