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

class ListaHerramientasActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_herramientas)

        val listView: ListView = findViewById(R.id.listViewHerramientas)

        // Cargar datos de la base de datos por primera vez
        cargarListaHerramientas(listView)
    }
    // Método que se llama cuando la actividad vuelve a estar activa
    override fun onResume() {
        super.onResume()

        // Recargar los datos al volver a la actividad
        val listView: ListView = findViewById(R.id.listViewHerramientas)
        cargarListaHerramientas(listView)
    }
    private fun cargarListaHerramientas(listView: ListView) {

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(DatabaseHelper.COL_NOMBRE, DatabaseHelper.COL_ESTADO, DatabaseHelper.COL_ID)
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val herramientas = ArrayList<String>()
        val herramientaIds = ArrayList<Int>()

        while (cursor.moveToNext()) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
            val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
            herramientas.add("$nombre - $estado")
            herramientaIds.add(id) // Guardamos el ID para futuros usos
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, herramientas)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedItem = herramientas[position]
            val selectedId = herramientaIds[position]

            if (selectedItem.contains("Prestada")) {
                Toast.makeText(this, "Esta herramienta está prestada y no se puede seleccionar", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, SolicitarPrestamoActivity::class.java)
                intent.putExtra("herramientaId", selectedId)
                startActivity(intent)
            }
        }
    }
}