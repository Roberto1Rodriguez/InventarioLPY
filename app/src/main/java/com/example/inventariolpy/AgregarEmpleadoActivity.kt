package com.example.inventariolpy

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AgregarEmpleadoActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_empleado)

        val etNombreEmpleado: EditText = findViewById(R.id.etNombreEmpleado)
        val etContacto: EditText = findViewById(R.id.etContacto)
        val etClaveAcceso: EditText = findViewById(R.id.etClaveAcceso)
        val btnAgregarEmpleado: Button = findViewById(R.id.btnAgregarEmpleado)

        btnAgregarEmpleado.setOnClickListener {
            val nombreEmpleado = etNombreEmpleado.text.toString()
            val contacto = etContacto.text.toString()
            val claveAcceso = etClaveAcceso.text.toString()

            if (nombreEmpleado.isNotEmpty() && contacto.isNotEmpty() && claveAcceso.isNotEmpty()) {
                agregarEmpleado(nombreEmpleado, contacto, claveAcceso)
                Toast.makeText(this, "Empleado registrado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun agregarEmpleado(nombreEmpleado: String, contacto: String, claveAcceso: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE_EMPLEADO, nombreEmpleado)
            put(DatabaseHelper.COL_CONTACTO, contacto)
            put(DatabaseHelper.COL_CLAVE_ACCESO, claveAcceso)
        }

        db.insert(DatabaseHelper.TABLE_EMPLEADOS, null, values)
    }
}