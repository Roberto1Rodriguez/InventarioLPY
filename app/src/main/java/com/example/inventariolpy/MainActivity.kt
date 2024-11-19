package com.example.inventariolpy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnListaHerramientas: Button = findViewById(R.id.btnListaHerramientas)
        val btnListaPrestamos: Button = findViewById(R.id.btnListaPrestamos)

        val btnVerPagares:Button=findViewById(R.id.btnVerPagares)


        btnListaHerramientas.setOnClickListener {
            val intent = Intent(this, ListaHerramientasActivity::class.java)
            startActivity(intent)
        }
        // Abrir la lista de préstamos
        btnListaPrestamos.setOnClickListener {
            val intent = Intent(this, ListaPrestamosActivity::class.java)
            startActivity(intent)
        }
        // Abrir la lista de préstamos
        btnVerPagares.setOnClickListener {
            val intent = Intent(this, ListaPDFsActivity::class.java)
            startActivity(intent)
        }



        val btnVerEmpleados: Button = findViewById(R.id.btnVerEmpleados)



        btnVerEmpleados.setOnClickListener {
            val intent = Intent(this, ListaEmpleadosActivity::class.java)
            startActivity(intent)
        }
    }
}