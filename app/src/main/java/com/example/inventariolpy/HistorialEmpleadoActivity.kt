package com.example.inventariolpy

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class HistorialEmpleadoActivity : AppCompatActivity() {

        private lateinit var listViewEmpleados: ListView
        private lateinit var searchView: SearchView
        private lateinit var adapter: HistorialAdapter
        private lateinit var empleados: List<Empleado>

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_historial_empleado)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

            listViewEmpleados = findViewById(R.id.listViewEmpleados)
            searchView = findViewById(R.id.searchViewEmpleados)

            cargarEmpleados()

            // 🔹 Buscar empleados en tiempo real
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter(newText ?: "")
                    return true
                }
            })
            searchView.setIconified(false) // Mantiene el SearchView expandido

            // 🔹 Mostrar herramientas prestadas al hacer clic en un empleado
            listViewEmpleados.setOnItemClickListener { _, _, position, _ ->
                val empleadoSeleccionado = empleados[position]
                mostrarPopupHerramientasPrestadas(empleadoSeleccionado)
            }
        }

        private fun cargarEmpleados() {
            val dbHelper = DatabaseHelper(this)
            empleados = dbHelper.obtenerTodosLosEmpleadosEstadisticas()

            if (empleados.isEmpty()) {
                Toast.makeText(this, "No hay empleados registrados.", Toast.LENGTH_SHORT).show()
                return
            }

            adapter = HistorialAdapter(this, empleados)
            listViewEmpleados.adapter = adapter
        }

        private fun filtrarEmpleados(query: String?) {
            val empleadosFiltrados = if (!query.isNullOrEmpty()) {
                empleados.filter { it.nombre.contains(query, ignoreCase = true) }
            } else {
                empleados
            }
            adapter.actualizarLista(empleadosFiltrados)
        }

    fun mostrarPopupHerramientasPrestadas(empleado: Empleado) {
        val dbHelper = DatabaseHelper(this)
        val herramientas = dbHelper.obtenerHerramientasPrestadasPorEmpleado(empleado.id)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Herramientas prestadas de ${empleado.nombre}")

        if (herramientas.isEmpty()) {
            builder.setMessage("Este empleado no tiene herramientas prestadas.")
            builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
            return
        }

        // Inflar el layout del diálogo con una lista personalizada
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_lista_herramientas, null)
        val listView = dialogView.findViewById<ListView>(R.id.listViewHerramientas)

        // Crear adaptador personalizado
        val adapter = HerramientaDialogAdapter(this, herramientas)
        listView.adapter = adapter

        builder.setView(dialogView)
        builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
    }