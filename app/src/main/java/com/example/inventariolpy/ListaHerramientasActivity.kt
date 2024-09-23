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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListaHerramientasActivity : AppCompatActivity() {

    private lateinit var fabSolicitarPrestamo: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private val herramientasSeleccionadas = mutableListOf<Herramienta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_herramientas)

        fabSolicitarPrestamo = findViewById(R.id.fabSolicitarPrestamo)
        recyclerView = findViewById(R.id.recyclerViewHerramientas)

        // Configurar el RecyclerView con el adaptador
        cargarListaHerramientas()

        // Configurar el botón flotante
        fabSolicitarPrestamo.setOnClickListener {
            if (herramientasSeleccionadas.isNotEmpty()) {
                iniciarSolicitudPrestamo()
            } else {
                Toast.makeText(this, "Selecciona al menos una herramienta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Recargar la lista de herramientas cuando la actividad vuelve a estar activa
    override fun onResume() {
        super.onResume()
        cargarListaHerramientas()
    }

    // Cargar la lista de herramientas desde la base de datos
    private fun cargarListaHerramientas() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(DatabaseHelper.COL_NOMBRE, DatabaseHelper.COL_ESTADO, DatabaseHelper.COL_ID_HERRAMIENTA)
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val herramientas = mutableListOf<Herramienta>()
        while (cursor.moveToNext()) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
            val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))
            val herramienta = Herramienta(id, nombre, estado)
            herramientas.add(herramienta)
        }
        cursor.close()

        // Configurar el adaptador con la lista de herramientas
        val adapter = HerramientaAdapter(herramientas) { seleccionadas ->
            herramientasSeleccionadas.clear()
            herramientasSeleccionadas.addAll(seleccionadas)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Iniciar la actividad de préstamo, pasando la lista de herramientas seleccionadas
    private fun iniciarSolicitudPrestamo() {
        val intent = Intent(this, SolicitarPrestamoActivity::class.java)
        intent.putParcelableArrayListExtra("herramientasSeleccionadas", ArrayList(herramientasSeleccionadas))
        startActivity(intent)
    }
}