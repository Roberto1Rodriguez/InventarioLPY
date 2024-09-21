package com.example.inventariolpy

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DevolucionActivity : AppCompatActivity() {

    private var prestamoId: Int = 0
    private var herramientaId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devolucion)

        prestamoId = intent.getIntExtra("prestamoId", 0)
        herramientaId = intent.getIntExtra("herramientaId", 0)

        val etReporteDevolucion: EditText = findViewById(R.id.etReporteDevolucion)
        val btnConfirmarDevolucion: Button = findViewById(R.id.btnConfirmarDevolucion)

        btnConfirmarDevolucion.setOnClickListener {
            val reporteDevolucion = etReporteDevolucion.text.toString()
            registrarDevolucion(reporteDevolucion)
        }
    }

    private fun registrarDevolucion(reporteDevolucion: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // Actualizar el préstamo como "Devuelto"
        val valuesPrestamo = ContentValues().apply {
            put(DatabaseHelper.COL_ESTADO_PRESTAMO, "Devuelto")
            put(DatabaseHelper.COL_FECHA_DEVOLUCION, System.currentTimeMillis().toString())
            put(DatabaseHelper.COL_REPORTE_DEVOLUCION, reporteDevolucion)
        }
        db.update(DatabaseHelper.TABLE_PRESTAMOS, valuesPrestamo, "${DatabaseHelper.COL_ID}=?", arrayOf(prestamoId.toString()))

        // Actualizar la herramienta a "Disponible"
        val valuesHerramienta = ContentValues().apply {
            put(DatabaseHelper.COL_ESTADO, "Disponible")
        }
        db.update(DatabaseHelper.TABLE_HERRAMIENTAS, valuesHerramienta, "${DatabaseHelper.COL_ID}=?", arrayOf(herramientaId.toString()))

        Toast.makeText(this, "Devolución registrada con éxito", Toast.LENGTH_SHORT).show()
        finish()
    }
}