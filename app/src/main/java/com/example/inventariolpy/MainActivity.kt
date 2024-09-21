package com.example.inventariolpy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRegistrarHerramienta: Button = findViewById(R.id.btnRegistrarHerramienta)
        val btnListaHerramientas: Button = findViewById(R.id.btnListaHerramientas)
        val btnListaPrestamos: Button = findViewById(R.id.btnListaPrestamos)
        val btnVerPDFs:Button=findViewById(R.id.btnVerPDFs)

        btnRegistrarHerramienta.setOnClickListener {
            val intent = Intent(this, RegistrarHerramientaActivity::class.java)
            startActivity(intent)
        }

        btnListaHerramientas.setOnClickListener {
            val intent = Intent(this, ListaHerramientasActivity::class.java)
            startActivity(intent)
        }
        // Abrir la lista de préstamos
        btnListaPrestamos.setOnClickListener {
            val intent = Intent(this, ListaPrestamosActivity::class.java)
            startActivity(intent)
        }
        btnVerPDFs.setOnClickListener {
            val intent = Intent(this, ListaPDFsActivity::class.java)
            startActivity(intent)
        }
        val btnAgregarEmpleado: Button = findViewById(R.id.btnAgregarEmpleado)
        val btnVerEmpleados: Button = findViewById(R.id.btnVerEmpleados)

        btnAgregarEmpleado.setOnClickListener {
            val intent = Intent(this, AgregarEmpleadoActivity::class.java)
            startActivity(intent)
        }

        btnVerEmpleados.setOnClickListener {
            val intent = Intent(this, ListaEmpleadosActivity::class.java)
            startActivity(intent)
        }
    }
}