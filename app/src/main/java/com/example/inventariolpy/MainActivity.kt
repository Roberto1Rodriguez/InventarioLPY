package com.example.inventariolpy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        val btnListaHerramientas: LinearLayout = findViewById(R.id.btnListaHerramientas)
        val btnListaPrestamos: LinearLayout = findViewById(R.id.btnListaPrestamos)
        val btnVerEmpleados: LinearLayout = findViewById(R.id.btnVerEmpleados)

        btnListaHerramientas.setOnClickListener {
            val intent = Intent(this, ListaHerramientasActivity::class.java)
            startActivity(intent)
        }

        btnListaPrestamos.setOnClickListener {
            val intent = Intent(this, ListaPrestamosActivity::class.java)
            startActivity(intent)
        }

     
        btnVerEmpleados.setOnClickListener {
            val intent = Intent(this, ListaEmpleadosActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onResume() {
        super.onResume()

        // Verificar si debe abrir la lista de herramientas
        val abrirLista = intent.getBooleanExtra("abrirListaHerramientas", false)
        if (abrirLista) {
            // Restablecer el indicador para evitar reabrir la lista al regresar
            intent.removeExtra("abrirListaHerramientas")

            // Iniciar ListaHerramientasActivity
            val intent = Intent(this, ListaHerramientasActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportarBaseDatosYPagares()
                true
            }
            R.id.action_import -> {
                selectFileForImport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportarBaseDatosYPagares() {
        val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DatabaseBackup")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fechaActual = sdf.format(Date())
        val backupFile = File(exportDir, "RespaldoBD_$fechaActual.db")

        val dbPath = getDatabasePath(DatabaseHelper.DATABASE_NAME).absolutePath
        try {
            // Copiar la base de datos
            val src = FileInputStream(dbPath).channel
            val dst = FileOutputStream(backupFile).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()

            // Crear un archivo zip para incluir la base de datos y los PDFs de pagarés
            val zipFile = File(exportDir, "Respaldo_$fechaActual.zip")
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
            zipOutputStream.use { zos ->
                // Agregar la base de datos al zip
                addFileToZip(backupFile, zos)

                // Agregar los PDF de pagarés si existen
                val pagaresDir = File(filesDir, "pdfs")
                if (pagaresDir.exists() && pagaresDir.isDirectory) {
                    pagaresDir.listFiles()?.forEach { pdfFile ->
                        addFileToZip(pdfFile, zos)
                    }
                }
            }

            // Mostrar notificación de respaldo creado y opción para compartir
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", zipFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Toast.makeText(this, "Respaldo creado: ${zipFile.absolutePath}", Toast.LENGTH_LONG).show()
            startActivity(Intent.createChooser(shareIntent, "Compartir respaldo usando"))

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear el respaldo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addFileToZip(file: File, zos: ZipOutputStream) {
        val fis = FileInputStream(file)
        val entry = ZipEntry(file.name)
        zos.putNextEntry(entry)
        fis.copyTo(zos)
        fis.close()
        zos.closeEntry()
    }

    private fun selectFileForImport() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerLauncher.launch(intent)
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                importDatabaseAndPDFs(uri)
            }
        }
    }

    private fun importDatabaseAndPDFs(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempDir = File(filesDir, "temp_import")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                val zipFile = File(tempDir, "import.zip")
                val outputStream = FileOutputStream(zipFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                // Extraer el contenido del zip
                val zipInputStream = ZipInputStream(FileInputStream(zipFile))
                var entry: ZipEntry?
                var dbImported = false
                Log.d("ImportDebug", "Contenido del archivo ZIP:")
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    Log.d("ImportDebug", "Archivo encontrado en ZIP: ${entry!!.name}")
                    val outputFile = File(filesDir, entry!!.name)
                    val fileOutput = FileOutputStream(outputFile)
                    zipInputStream.copyTo(fileOutput)
                    fileOutput.close()
                    zipInputStream.closeEntry()

                    // Si el archivo extraído es una base de datos, importarla
                    if (entry!!.name.endsWith(".db")) {
                        val dbPath = getDatabasePath(DatabaseHelper.DATABASE_NAME).absolutePath
                        val src = FileInputStream(outputFile).channel
                        val dst = FileOutputStream(dbPath).channel
                        dst.transferFrom(src, 0, src.size())
                        src.close()
                        dst.close()
                        dbImported = true
                        Toast.makeText(this, "Base de datos importada: ${entry!!.name}", Toast.LENGTH_SHORT).show()
                    } else if (entry!!.name.endsWith(".pdf")) {
                        // Mover los PDFs a la ubicación adecuada
                        val pdfDir = File(filesDir, "pdfs")
                        if (!pdfDir.exists()) {
                            pdfDir.mkdirs()
                        }
                        outputFile.copyTo(File(pdfDir, outputFile.name), overwrite = true)
                    }
                }
                zipInputStream.close()

                if (!dbImported) {
                    Toast.makeText(this, "No se encontró un archivo de base de datos para importar", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Importación completada", Toast.LENGTH_SHORT).show()
                    // Reinicia la aplicación para que los cambios surtan efecto
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "No se pudo abrir el archivo seleccionado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al importar el archivo", Toast.LENGTH_SHORT).show()
        }
    }
}