package com.example.inventariolpy

import android.database.Cursor
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ListaEmpleadosActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_empleados)

        val listViewEmpleados: ListView = findViewById(R.id.listViewEmpleados)
        cargarListaEmpleados(listViewEmpleados)
    }

    private fun cargarListaEmpleados(listView: ListView) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(DatabaseHelper.COL_NOMBRE_EMPLEADO, DatabaseHelper.COL_CONTACTO)
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val empleados = ArrayList<String>()

        while (cursor.moveToNext()) {
            val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
            val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO))
            empleados.add("$nombreEmpleado - Contacto: $contacto")
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, empleados)
        listView.adapter = adapter
    }
}