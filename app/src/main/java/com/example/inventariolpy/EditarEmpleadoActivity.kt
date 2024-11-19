package com.example.inventariolpy

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.MediaStore
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.zxing.integration.android.IntentIntegrator
import java.io.ByteArrayOutputStream
class EditarEmpleadoActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etCurp: EditText
    private lateinit var etRfc: EditText
    private lateinit var etContacto: EditText
    private lateinit var btnCapturarFoto: Button
    private lateinit var btnCapturarFirma: Button
    private lateinit var btnAutenticacion: Button
    private lateinit var imgFotoEmpleado: ImageView
    private var empleadoId: Int = 0
    private var fotoEmpleadoBitmap: Bitmap? = null
    private var firmaEmpleadoBitmap: Bitmap? = null
    private var nfcAdapter: NfcAdapter? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_empleado)

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etDireccion = findViewById(R.id.etDireccion)
        etCurp = findViewById(R.id.etCurp)
        etRfc = findViewById(R.id.etRfc)
        etContacto = findViewById(R.id.etContacto)
        btnCapturarFoto = findViewById(R.id.btnCapturarFoto)
        btnCapturarFirma = findViewById(R.id.btnCapturarFirma)
        btnAutenticacion = findViewById(R.id.btnAutenticacion)
        imgFotoEmpleado = findViewById(R.id.imgFotoEmpleado)

        // Obtener el ID del empleado a editar
        empleadoId = intent.getIntExtra("empleadoId", 0)

        // Cargar los datos del empleado desde la base de datos
        cargarDatosEmpleado()

        // Configurar botones de captura de foto y firma
        btnCapturarFoto.setOnClickListener { mostrarOpcionesCapturaFoto() }
        btnCapturarFirma.setOnClickListener { capturarFirma() }

        // Configurar botón de autenticación NFC/QR
        btnAutenticacion.setOnClickListener { mostrarPopupAutenticacion() }

        // Configurar botón de guardar cambios
        val btnGuardar: Button = findViewById(R.id.btnGuardarEmpleado)
        btnGuardar.text = "Guardar Cambios"
        btnGuardar.setOnClickListener { guardarCambios() }

        // Configurar NFC si está disponible
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    private fun cargarDatosEmpleado() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            null,
            "${DatabaseHelper.COL_ID_EMPLEADO} = ?",
            arrayOf(empleadoId.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO)))
            etDireccion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DIRECCION)))
            etCurp.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CURP)))
            etRfc.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_RFC)))
            etContacto.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO)))

            val fotoBlob = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO))
            if (fotoBlob != null) {
                fotoEmpleadoBitmap = BitmapFactory.decodeByteArray(fotoBlob, 0, fotoBlob.size)
                imgFotoEmpleado.setImageBitmap(fotoEmpleadoBitmap) // Mostrar la foto del empleado
            } else {
                imgFotoEmpleado.setImageResource(R.drawable.ic_placeholder) // Mostrar un placeholder si no hay foto
            }
        }
        cursor.close()
    }

    private fun mostrarOpcionesCapturaFoto() {
        val opciones = arrayOf("Tomar Foto", "Elegir de la Galería")
        AlertDialog.Builder(this)
            .setTitle("Capturar Foto")
            .setItems(opciones) { _, which ->
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
                    fotoEmpleadoBitmap = bitmap
                    imgFotoEmpleado.setImageBitmap(bitmap)
                    btnCapturarFoto.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    fotoEmpleadoBitmap = bitmap
                    imgFotoEmpleado.setImageBitmap(bitmap)
                    btnCapturarFoto.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
                }
            }
        }
    }

    private fun capturarFirma() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_captura_firma)

        val params = WindowManager.LayoutParams()
        params.copyFrom(dialog.window?.attributes)
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = params

        val btnGuardarFirma = dialog.findViewById<Button>(R.id.btnGuardarFirma)
        val signaturePad = dialog.findViewById<SignaturePad>(R.id.signaturePad)

        btnGuardarFirma.setOnClickListener {
            if (!signaturePad.isEmpty) {
                firmaEmpleadoBitmap = signaturePad.signatureBitmap
                btnCapturarFirma.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCompleted))
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Por favor, dibuja tu firma antes de guardar", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun mostrarPopupAutenticacion() {
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
            .setMessage("Acerca tu tarjeta NFC para confirmar")
            .setCancelable(false)
            .setNegativeButton("Cancelar") { dialog, _ ->
                nfcAdapter?.disableReaderMode(this)
                dialog.dismiss()
            }
            .show()

        nfcAdapter?.enableReaderMode(this, { tag ->
            val idTag = tag?.id?.joinToString("") { "%02x".format(it) }
            runOnUiThread {
                if (idTag == "EXPECTED_NFC_ID") {
                    Toast.makeText(this, "Autenticación NFC exitosa", Toast.LENGTH_SHORT).show()
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
        integrator.setPrompt("Escanea el código QR de la identificación")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    private fun guardarCambios() {
        if (etNombre.text.isNullOrBlank()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return
        }

        if (etContacto.text.isNullOrBlank()) {
            etContacto.error = "El contacto es obligatorio"
            etContacto.requestFocus()
            return
        }

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE_EMPLEADO, etNombre.text.toString().trim())
            put(DatabaseHelper.COL_DIRECCION, etDireccion.text.toString().trim())
            put(DatabaseHelper.COL_CURP, etCurp.text.toString().trim())
            put(DatabaseHelper.COL_RFC, etRfc.text.toString().trim())
            put(DatabaseHelper.COL_CONTACTO, etContacto.text.toString().trim())
            if (fotoEmpleadoBitmap != null) {
                put(DatabaseHelper.COL_FOTO, convertirBitmapABlob(fotoEmpleadoBitmap))
            }
        }

        val rowsUpdated = db.update(
            DatabaseHelper.TABLE_EMPLEADOS,
            values,
            "${DatabaseHelper.COL_ID_EMPLEADO} = ?",
            arrayOf(empleadoId.toString())
        )

        if (rowsUpdated > 0) {
            Toast.makeText(this, "Empleado actualizado", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error al actualizar el empleado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertirBitmapABlob(bitmap: Bitmap?): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}