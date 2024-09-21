package com.example.inventariolpy

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ListaPrestamosActivity : AppCompatActivity() {
    private lateinit var btnVerPrestadas: Button
    private lateinit var btnVerDevueltas: Button

    // Método que se llama cuando la actividad vuelve a estar activa
    override fun onResume() {
        super.onResume()

        // Recargar los datos al volver a la actividad
        val listView: ListView = findViewById(R.id.listViewPrestamos)
        cargarListaPrestamos(listView, "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_prestamos)

        val listView: ListView = findViewById(R.id.listViewPrestamos)
        btnVerPrestadas = findViewById(R.id.btnVerPrestadas)
        btnVerDevueltas = findViewById(R.id.btnVerDevueltas)

        // Cargar todos los préstamos por defecto
        cargarListaPrestamos(listView, "")

        // Al hacer clic en "Ver Prestadas", cargar solo las prestadas
        btnVerPrestadas.setOnClickListener {
            cargarListaPrestamos(listView, "Activo")
        }

        // Al hacer clic en "Ver Devueltas", cargar solo las devueltas
        btnVerDevueltas.setOnClickListener {
            cargarListaPrestamos(listView, "Devuelto")
        }
    }

    // Función para cargar y actualizar la lista de préstamos filtrada
    private fun cargarListaPrestamos(listView: ListView, filtroEstado: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Definir la consulta en función del filtro
        val selection =
            if (filtroEstado.isEmpty()) null else "${DatabaseHelper.COL_ESTADO_PRESTAMO} = ?"
        val selectionArgs = if (filtroEstado.isEmpty()) null else arrayOf(filtroEstado)

        // Consulta con un JOIN para obtener el nombre del empleado desde la tabla empleados
        val query = """
        SELECT prestamos.${DatabaseHelper.COL_ID}, empleados.${DatabaseHelper.COL_NOMBRE_EMPLEADO}, 
               prestamos.${DatabaseHelper.COL_HERRAMIENTA_ID}, prestamos.${DatabaseHelper.COL_FECHA_PRESTAMO}, prestamos.${DatabaseHelper.COL_ESTADO_PRESTAMO}
        FROM ${DatabaseHelper.TABLE_PRESTAMOS} 
        JOIN ${DatabaseHelper.TABLE_EMPLEADOS} 
        ON prestamos.${DatabaseHelper.COL_EMPLEADO_ID} = empleados.${DatabaseHelper.COL_ID}
        ${if (selection != null) "WHERE $selection" else ""}
    """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, selectionArgs)

        val prestamos = ArrayList<String>()
        val prestamoIds = ArrayList<Int>()
        val herramientaIds = ArrayList<Int>()

        while (cursor.moveToNext()) {
            val prestamoId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
            val nombreEmpleado =
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
            val herramientaId =
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HERRAMIENTA_ID))
            val fechaPrestamo =
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FECHA_PRESTAMO))
            val estadoPrestamo =
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO_PRESTAMO))

            prestamos.add("Empleado: $nombreEmpleado, Herramienta ID: $herramientaId, Fecha: $fechaPrestamo, Estado: $estadoPrestamo")
            prestamoIds.add(prestamoId)
            herramientaIds.add(herramientaId)
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, prestamos)
        listView.adapter = adapter
    }
}