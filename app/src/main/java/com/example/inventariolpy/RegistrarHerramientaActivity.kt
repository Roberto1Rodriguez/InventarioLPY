package com.example.inventariolpy
import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
class RegistrarHerramientaActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_herramienta)
        registrarHerramiente()
    }
    private fun registrarHerramiente(){
        val btnRegistrar: Button = findViewById(R.id.btnRegistrarHerramienta)
        val etNombre: EditText = findViewById(R.id.etNombreHerramienta)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionHerramienta)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val descripcion = etDescripcion.text.toString()

            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_NOMBRE, nombre)
                put(DatabaseHelper.COL_DESCRIPCION, descripcion)
            }

            val newRowId = db.insert(DatabaseHelper.TABLE_HERRAMIENTAS, null, values)

            if (newRowId != -1L) {
                Toast.makeText(this, "Herramienta registrada exitosamente", Toast.LENGTH_SHORT).show()
                etNombre.text.clear()
                etDescripcion.text.clear()
                finish()
            } else {
                Toast.makeText(this, "Error al registrar la herramienta", Toast.LENGTH_SHORT).show()
            }
        }

    }
}