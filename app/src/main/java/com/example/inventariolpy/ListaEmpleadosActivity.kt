package com.example.inventariolpy

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
class ListaEmpleadosActivity : AppCompatActivity() {

    private lateinit var adapter: EmpleadoAdapter
    private lateinit var listViewEmpleados: ListView
    private lateinit var searchView: SearchView
    private lateinit var btnAgregarEmpleado: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_empleados)

        listViewEmpleados = findViewById(R.id.listViewEmpleados)
        searchView = findViewById(R.id.searchViewEmpleados)
        btnAgregarEmpleado = findViewById(R.id.btnAgregarEmpleado)


        // Configurar la lista de empleados inicialmente
        configurarListaEmpleados()

        // Configurar el `SearchView` para realizar la búsqueda activa
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
        btnAgregarEmpleado.setOnClickListener {
            // Lógica para abrir la actividad de agregar empleado
            val intent = Intent(this, AgregarEmpleadoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar la lista de empleados al regresar de la actividad de edición
        recargarListaEmpleados()
    }
    private fun configurarListaEmpleados() {
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.obtenerEmpleadosActivos()

        adapter = EmpleadoAdapter(this, cursor)
        listViewEmpleados.adapter = adapter
    }



    fun recargarListaEmpleados() {
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.obtenerEmpleadosActivos()

        adapter.changeCursor(cursor)
    }

}