package com.example.inventariolpy
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class ListaPDFsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_pdfs)

        // Llamar a la función para listar los PDFs
        listarPDFs()
    }

    private fun listarPDFs() {
        val pdfDir = File(filesDir, "pdfs")
        val pdfFiles = pdfDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()

        val pdfNames = pdfFiles.map { it.name } // Lista de nombres de los archivos PDF

        val listView: ListView = findViewById(R.id.listViewPDFs)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pdfNames)
        listView.adapter = adapter

        // Permitir seleccionar un PDF para compartirlo
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedFile = pdfFiles[position]
            compartirPDF(selectedFile)
        }
    }

    private fun compartirPDF(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Mostrar el menú de compartir
        startActivity(Intent.createChooser(intent, "Compartir PDF con"))
    }
}