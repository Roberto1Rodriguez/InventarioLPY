package com.example.inventariolpy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.integration.android.IntentIntegrator
import java.util.concurrent.Executors

class EscanearHerramientasActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var listaHerramientas: MutableList<Herramienta>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HerramientasEscaneadasAdapter
    private lateinit var btnFinalizarEscaneo: Button
    private lateinit var btnVolver: Button

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escanear_herramientas)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Inicializa las vistas
        previewView = findViewById(R.id.previewView)
        recyclerView = findViewById(R.id.recyclerViewHerramientasEscaneadas)
        btnFinalizarEscaneo = findViewById(R.id.btnFinalizarEscaneo)
        btnVolver = findViewById(R.id.btnVolver)

        // Inicializa la lista de herramientas
        listaHerramientas = mutableListOf()
        onBackPressedDispatcher.addCallback(this) {
            regresarConResultado()
            finish()
        }
        // Recupera herramientas seleccionadas desde el Intent
        val herramientasSeleccionadas =
            intent.getParcelableArrayListExtra<Herramienta>("herramientasSeleccionadas")
        if (!herramientasSeleccionadas.isNullOrEmpty()) {
            listaHerramientas.addAll(herramientasSeleccionadas)
        }

        // Inicializa el adaptador
        adapter = HerramientasEscaneadasAdapter(this, listaHerramientas) { herramienta ->
            eliminarHerramienta(herramienta)
        }

        // Configura el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Configura el botón de finalizar escaneo
        btnFinalizarEscaneo.setOnClickListener { finalizarEscaneo() }
        btnVolver.setOnClickListener {  regresarConResultado()
            finish() }

        val dbHelper = DatabaseHelper(this)
        val herramientasDesdeDb = dbHelper.obtenerHerramientasSeleccionadas()

        for (herramienta in herramientasDesdeDb) {
            if (!listaHerramientas.any { it.id == herramienta.id }) {
                listaHerramientas.add(herramienta)
            }
        }
        adapter.notifyDataSetChanged()
        // Inicia la cámara
        iniciarCamara()

    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient()
            val analysisUseCase = ImageAnalysis.Builder()
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        procesarImagen(imageProxy, barcodeScanner)
                    }
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysisUseCase
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun procesarImagen(imageProxy: ImageProxy, barcodeScanner: BarcodeScanner) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val codigoInterno = barcode.rawValue
                        Log.d("EscanearHerramientas", "Código leído: $codigoInterno")
                        if (codigoInterno != null) {
                            agregarHerramientaPorCodigo(codigoInterno)
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private val codigosRecientes = mutableSetOf<String>() // Registro de códigos recientes
    private val tiempoEspera = 2000L // Tiempo de espera en milisegundos (2 segundos)

    private fun agregarHerramientaPorCodigo(codigoInterno: String) {
        val codigoLimpio = codigoInterno.trim().lowercase()
        Log.d("EscanearHerramientas", "Procesando código leído: '$codigoLimpio'")

        val dbHelper = DatabaseHelper(this)
        val herramienta = dbHelper.obtenerHerramientaPorCodigo(codigoLimpio)

        if (herramienta != null) {
            Log.d("EscanearHerramientas", "Herramienta encontrada: ${herramienta.nombre}")
            if (herramienta.estado == "Disponible") {
                runOnUiThread {
                    if (!listaHerramientas.any { it.codigoInterno == herramienta.codigoInterno }) {
                        listaHerramientas.add(herramienta)
                        adapter.notifyItemInserted(listaHerramientas.size - 1)
                    } else {
                    }
                }
            } else {
                Log.d("EscanearHerramientas", "Herramienta no disponible: ${herramienta.nombre}")
                runOnUiThread {
                }
            }
        } else {
            Log.d("EscanearHerramientas", "No se encontró herramienta con código: '$codigoLimpio'")
            runOnUiThread {
            }
        }
    }
    private fun eliminarHerramienta(herramienta: Herramienta) {
        listaHerramientas.remove(herramienta)
        adapter.notifyDataSetChanged()
    }

    private fun finalizarEscaneo() {
        val herramientasIds = listaHerramientas.map { it.id }
        val intent = Intent()
        intent.putParcelableArrayListExtra("herramientasSeleccionadas", ArrayList(listaHerramientas))
        intent.putExtra("accion", "finalizar") // <--- 👈 Marca como finalización
        setResult(RESULT_OK, intent)
        finish()
    }
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
    }
    override fun onPause() {
        super.onPause()
        Handler(Looper.getMainLooper()).postDelayed({
            cameraProvider?.unbindAll()
        }, 1000) // Espera 1 segundo antes de liberar la cámara
    }
    private fun regresarConResultado() {
        val intent = Intent()
        intent.putParcelableArrayListExtra("herramientasSeleccionadas", ArrayList(listaHerramientas))
        intent.putExtra("accion", "regresar") // <--- 👈 Marca como regreso
        setResult(RESULT_OK, intent)
        finish()
    }



}