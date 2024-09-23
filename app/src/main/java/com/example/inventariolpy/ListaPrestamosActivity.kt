package com.example.inventariolpy

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaPrestamosActivity : AppCompatActivity() {
    private lateinit var btnVerPrestadas: Button
    private lateinit var btnVerDevueltas: Button

    // Declarar las listas a nivel de clase
    private val prestamoIds = ArrayList<Int>()
    private val herramientaIds = ArrayList<Int>()

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
        btnVerPrestadas = findViewById(R.id.btnVerActivos)
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

        // Listener para cuando se hace clic en un préstamo de la lista
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < prestamoIds.size) {
                val prestamoId = prestamoIds[position]

                // Obtener todas las herramientas asociadas al préstamo desde la tabla intermedia
                val herramientas = obtenerHerramientasPorPrestamo(prestamoId)

                // Iniciar la actividad de devolución pasando los IDs del préstamo y las herramientas
                val intent = Intent(this, DevolucionActivity::class.java)
                intent.putExtra("prestamoId", prestamoId)
                intent.putParcelableArrayListExtra(
                    "herramientas",
                    ArrayList(herramientas)
                ) // Enviar la lista de herramientas
                startActivity(intent)
            } else {
                // Mostrar mensaje de error si las listas están vacías o el índice es inválido
                Toast.makeText(
                    this,
                    "Error: No se encontró el préstamo seleccionado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

        // Función para obtener todas las herramientas asociadas a un préstamo
        private fun obtenerHerramientasPorPrestamo(prestamoId: Int): List<Herramienta> {
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.readableDatabase

            val query = """
        SELECT h.${DatabaseHelper.COL_ID_HERRAMIENTA}, h.${DatabaseHelper.COL_NOMBRE}, h.${DatabaseHelper.COL_ESTADO}
        FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS} ph
        JOIN ${DatabaseHelper.TABLE_HERRAMIENTAS} h ON ph.${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
        WHERE ph.${DatabaseHelper.COL_PRESTAMO_ID} = ?
    """
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            val herramientas = mutableListOf<Herramienta>()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
                herramientas.add(Herramienta(id, nombre, estado))
            }
            cursor.close()

            return herramientas
        }

    private fun cargarListaPrestamos(listView: ListView, filtroEstado: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Limpiar las listas antes de cargarlas nuevamente
        prestamoIds.clear()
        herramientaIds.clear()

        // Definir la consulta en función del filtro
        val selection =
            if (filtroEstado.isEmpty()) null else "${DatabaseHelper.COL_ESTADO_PRESTAMO} = ?"
        val selectionArgs = if (filtroEstado.isEmpty()) null else arrayOf(filtroEstado)

        // Consulta con JOIN y GROUP_CONCAT para obtener múltiples herramientas en una fila
        val query = """
        SELECT prestamos.${DatabaseHelper.COL_ID_PRESTAMO}, empleados.${DatabaseHelper.COL_NOMBRE_EMPLEADO}, 
               GROUP_CONCAT(prestamo_herramientas.${DatabaseHelper.COL_HERRAMIENTA_ID}, ', ') AS herramientas, 
               prestamos.${DatabaseHelper.COL_FECHA_PRESTAMO}, prestamos.${DatabaseHelper.COL_ESTADO_PRESTAMO}
        FROM ${DatabaseHelper.TABLE_PRESTAMOS} 
        JOIN ${DatabaseHelper.TABLE_EMPLEADOS} 
        ON prestamos.${DatabaseHelper.COL_EMPLEADO_ID} = empleados.${DatabaseHelper.COL_ID_EMPLEADO}
        JOIN ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS}
        ON prestamos.${DatabaseHelper.COL_ID_PRESTAMO} = prestamo_herramientas.${DatabaseHelper.COL_PRESTAMO_ID}
        ${if (selection != null) "WHERE $selection" else ""}
        GROUP BY prestamos.${DatabaseHelper.COL_ID_PRESTAMO}
    """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, selectionArgs)

        val prestamos = ArrayList<String>()

        while (cursor.moveToNext()) {
            val prestamoId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
            val herramientas = cursor.getString(cursor.getColumnIndexOrThrow("herramientas")) // Esto es una cadena como "2, 3"
            val fechaPrestamoMillis = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_prestamo"))

            // Convertir la fecha de milisegundos a formato legible
            val fechaPrestamo = convertirFechaLegible(fechaPrestamoMillis)

            val estadoPrestamo = cursor.getString(cursor.getColumnIndexOrThrow("estado_prestamo"))

            // Dividimos las herramientas por coma y las mostramos de manera compacta
            val herramientasList = herramientas.split(",").map { it.trim() }
            val herramientasTexto = herramientasList.joinToString(", ") // Solo mostrar los IDs separados por comas

            // Añadir la información del préstamo a la lista
            prestamos.add("Empleado: $nombreEmpleado, HerramientasIDs: $herramientasTexto, Fecha: $fechaPrestamo, Estado: $estadoPrestamo")
            prestamoIds.add(prestamoId)
        }
        cursor.close()

        // Verificar si se encontraron resultados
        if (prestamos.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, prestamos)
            listView.adapter = adapter
        } else {
            // Mostrar mensaje si no hay préstamos que mostrar
            Toast.makeText(this, "No se encontraron préstamos.", Toast.LENGTH_SHORT).show()
        }

    }
    // Función para convertir milisegundos a un formato de fecha legible
    private fun convertirFechaLegible(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = Date(millis)
        return sdf.format(date)
    }
}