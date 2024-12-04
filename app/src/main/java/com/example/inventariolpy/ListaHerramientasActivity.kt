package com.example.inventariolpy
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


    class ListaHerramientasActivity : AppCompatActivity() {
        private lateinit var fabSolicitarPrestamo: FloatingActionButton
        private lateinit var recyclerView: RecyclerView
        private lateinit var searchView: SearchView
        private lateinit var adapter: HerramientaAdapter
        private val herramientasSeleccionadas = mutableListOf<Herramienta>()
        private val herramientas = mutableListOf<Herramienta>()
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == SOLICITAR_PRESTAMO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                val intent = intent
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0) // Evitar transiciones
            }
        }
        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            // Guardar las herramientas seleccionadas
            outState.putParcelableArrayList(
                "herramientasSeleccionadas",
                ArrayList(herramientasSeleccionadas)
            )
        }

        override fun onRestoreInstanceState(savedInstanceState: Bundle) {
            super.onRestoreInstanceState(savedInstanceState)
            // Restaurar las herramientas seleccionadas
            val seleccionadas = savedInstanceState.getParcelableArrayList<Herramienta>("herramientasSeleccionadas")
            if (seleccionadas != null) {
                herramientasSeleccionadas.clear()
                herramientasSeleccionadas.addAll(seleccionadas)
            }
            sincronizarSeleccionEnAdaptador()
        }

        private fun sincronizarSeleccionEnAdaptador() {
            herramientas.forEach { herramienta ->
                herramienta.isSelected = herramientasSeleccionadas.any { it.id == herramienta.id }
            }
            adapter.notifyDataSetChanged()
        }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_lista_herramientas)

            val btnMenuOpciones: ImageButton = findViewById(R.id.btnMenuOpciones)
            btnMenuOpciones.setOnClickListener {
                val popupMenu = PopupMenu(this, btnMenuOpciones)
                popupMenu.menuInflater.inflate(R.menu.menu_opciones, popupMenu.menu)

                // Forzar que se muestren los íconos en el menú emergente (PopupMenu)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popupMenu.setForceShowIcon(true)
                } else {
                    try {
                        val fields = popupMenu.javaClass.declaredFields
                        for (field in fields) {
                            if ("mPopup" == field.name) {
                                field.isAccessible = true
                                val menuPopupHelper = field.get(popupMenu)
                                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                                val setForceIcons = classPopupHelper.getMethod(
                                    "setForceShowIcon",
                                    Boolean::class.javaPrimitiveType
                                )
                                setForceIcons.invoke(menuPopupHelper, true)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Configurar listeners para cada ítem del menú
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_generar_reporte -> {
                            generarReporteInventario()
                            true
                        }

                        R.id.menu_agregar_herramienta -> {
                            val intent = Intent(this, RegistrarHerramientaActivity::class.java)
                            startActivity(intent)
                            true
                        }

                        else -> false
                    }
                }

                popupMenu.show()
            }

            fabSolicitarPrestamo = findViewById(R.id.fabSolicitarPrestamo)
            recyclerView = findViewById(R.id.recyclerViewHerramientas)
            searchView = findViewById(R.id.searchViewHerramientas)
            fabSolicitarPrestamo.visibility = View.GONE

            // Configurar el adaptador con las herramientas y el listener
            adapter = HerramientaAdapter(this, herramientas) { seleccionadas ->
                herramientasSeleccionadas.clear()
                herramientasSeleccionadas.addAll(seleccionadas)

                // Mostrar u ocultar el FAB con animación
                if (herramientasSeleccionadas.isNotEmpty()) {
                    fabSolicitarPrestamo.show()
                } else {
                    fabSolicitarPrestamo.hide()
                }
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)

            fabSolicitarPrestamo.setOnClickListener {
                if (herramientasSeleccionadas.isNotEmpty()) {
                    iniciarSolicitudPrestamo()
                } else {
                    Toast.makeText(this, "Selecciona al menos una herramienta", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filtrarHerramientas(newText ?: "")
                    return true
                }
            })

            cargarListaHerramientas()
        }
        private var debeActualizarLista = false // Indicador para actualizar la lista
        override fun onResume() {
            super.onResume()

            if (debeActualizarLista) {
                // Actualizar la lista de herramientas
                actualizarListaHerramientas()

                // Limpiar herramientas seleccionadas que ya no están disponibles
                herramientasSeleccionadas.removeAll { herramienta ->
                    herramienta.estado != "Disponible"
                }

                adapter.notifyDataSetChanged()

                // Restablecer el indicador
                debeActualizarLista = false
            }
        }
        fun actualizarListaHerramientas() {
            val dbHelper = DatabaseHelper(this)
            val cursor = dbHelper.obtenerHerramientasActivas()

            herramientas.clear()
            while (cursor.moveToNext()) {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
                val codigoInterno = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODIGO_INTERNO))
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))
                val foto = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO_HERRAMIENTA)) // Recuperar la imagen

                val herramienta = Herramienta(id, nombre, estado, codigoInterno = codigoInterno, fotoHerramienta = foto)
                herramientas.add(herramienta)
            }
            cursor.close()

            // Limpiar selección al actualizar la lista
            herramientasSeleccionadas.clear()

            adapter.actualizarLista(herramientas)
        }

        private fun cargarListaHerramientas() {
            val dbHelper = DatabaseHelper(this)
            val cursor = dbHelper.obtenerHerramientasActivas()

            herramientas.clear()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
                val codigoInterno = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODIGO_INTERNO))
                val foto = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO_HERRAMIENTA)) // Recuperar la imagen

                val herramienta = Herramienta(
                    id = id,
                    nombre = nombre,
                    estado = estado,
                    codigoInterno = codigoInterno,
                    fotoHerramienta = foto,
                    isSelected = herramientasSeleccionadas.any { it.id == id } // Sincronizar estado
                )
                herramientas.add(herramienta)
            }
            cursor.close()

            adapter.actualizarLista(herramientas)
        }
        private fun filtrarHerramientas(query: String) {
            if (query.isEmpty()) {
                herramientas.forEach { herramienta ->
                    herramienta.isSelected = herramientasSeleccionadas.any {
                        it.id == herramienta.id && it.estado == "Disponible"
                    }
                }
                adapter.actualizarLista(herramientas) // Mostrar todas las herramientas
            } else {
                val herramientasFiltradas = herramientas.filter {
                    it.nombre.contains(query, ignoreCase = true)
                }
                herramientasFiltradas.forEach { herramienta ->
                    herramienta.isSelected = herramientasSeleccionadas.any {
                        it.id == herramienta.id && it.estado == "Disponible"
                    }
                }
                adapter.actualizarLista(herramientasFiltradas)
            }
        }
        private fun iniciarSolicitudPrestamo() {
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.readableDatabase

            val cursor = db.rawQuery(
                "SELECT COUNT(*) AS cantidad FROM ${DatabaseHelper.TABLE_EMPLEADOS} WHERE ${DatabaseHelper.COL_ESTADO_EMPLEADO} != ?",
                arrayOf("Inactivo")
            )

            var empleadosActivos = 0
            if (cursor.moveToFirst()) {
                empleadosActivos = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad"))
            }
            cursor.close()

            if (empleadosActivos == 0) {
                Toast.makeText(
                    this,
                    "No se puede realizar el préstamo porque no hay empleados activos.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val herramientasValidas = herramientasSeleccionadas.filter { it.estado == "Disponible" }
                if (herramientasValidas.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No hay herramientas disponibles para el préstamo",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(this, SolicitarPrestamoActivity::class.java)
                    intent.putParcelableArrayListExtra(
                        "herramientasSeleccionadas",
                        ArrayList(herramientasValidas)
                    )
                    startActivityForResult(intent, SOLICITAR_PRESTAMO_REQUEST_CODE) // Iniciar con resultado

                    // Limpiar herramientas seleccionadas que serán prestadas
                    herramientasSeleccionadas.removeAll(herramientasSeleccionadas)
                    debeActualizarLista = true

                }
            }
        }
        private fun generarReporteInventario() {
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.readableDatabase
            val query = """
        SELECT 
            h.${DatabaseHelper.COL_ID_HERRAMIENTA} AS id, 
            h.${DatabaseHelper.COL_CODIGO_INTERNO} AS codigoInterno,
            h.${DatabaseHelper.COL_NOMBRE} AS nombre, 
            h.${DatabaseHelper.COL_ESTADO} AS estado, 
            h.${DatabaseHelper.COL_MARCA} AS marca, 
            h.${DatabaseHelper.COL_MODELO} AS modelo, 
            h.${DatabaseHelper.COL_PRECIO} AS precio, 
            h.activo AS activo,
            CASE 
                WHEN h.${DatabaseHelper.COL_ESTADO} IN ('Prestada', 'Rota', 'Perdida') THEN 
                    (SELECT p.${DatabaseHelper.COL_NOMBRE_EMPLEADO} 
                     FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS} ph
                     JOIN ${DatabaseHelper.TABLE_PRESTAMOS} pr 
                     ON ph.${DatabaseHelper.COL_PRESTAMO_ID} = pr.${DatabaseHelper.COL_ID_PRESTAMO} 
                     JOIN ${DatabaseHelper.TABLE_EMPLEADOS} p 
                     ON pr.${DatabaseHelper.COL_EMPLEADO_ID} = p.${DatabaseHelper.COL_ID_EMPLEADO}
                     WHERE ph.${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
                     ORDER BY pr.${DatabaseHelper.COL_FECHA_PRESTAMO} DESC
                     LIMIT 1
                    ) 
                ELSE 'N/A' 
            END AS nombreEmpleado, 
            CASE 
                WHEN h.${DatabaseHelper.COL_ESTADO} IN ('Prestada', 'Rota', 'Perdida') THEN 
                    (SELECT pr.${DatabaseHelper.COL_FECHA_PRESTAMO} 
                     FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS} ph
                     JOIN ${DatabaseHelper.TABLE_PRESTAMOS} pr 
                     ON ph.${DatabaseHelper.COL_PRESTAMO_ID} = pr.${DatabaseHelper.COL_ID_PRESTAMO} 
                     WHERE ph.${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
                     ORDER BY pr.${DatabaseHelper.COL_FECHA_PRESTAMO} DESC
                     LIMIT 1
                    ) 
                ELSE '' 
            END AS fecha_prestamo 
        FROM 
            ${DatabaseHelper.TABLE_HERRAMIENTAS} h
    """
            val cursor = db.rawQuery(query, null)

            val herramientas = mutableListOf<HerramientaReporte>()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val codigoInterno = cursor.getString(cursor.getColumnIndexOrThrow("codigoInterno")) ?: "N/A"
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "N/A"
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: "N/A"
                val marca = cursor.getString(cursor.getColumnIndexOrThrow("marca")) ?: "N/A"
                val modelo = cursor.getString(cursor.getColumnIndexOrThrow("modelo")) ?: "N/A"
                val precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio"))
                val activo = cursor.getInt(cursor.getColumnIndexOrThrow("activo")) // Nuevo campo
                val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow("nombreEmpleado")) ?: "N/A"
                val fechaPrestamo = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_prestamo"))

                herramientas.add(
                    HerramientaReporte(
                        id = id,
                        codigoInterno = codigoInterno,
                        nombre = nombre,
                        estado = estado,
                        marca = marca,
                        modelo = modelo,
                        precio = precio,
                        nombreEmpleado = nombreEmpleado,
                        fechaPrestamo = if (fechaPrestamo > 0) SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(Date(fechaPrestamo)) else "N/A",
                        activo = activo
                    )
                )
            }
            cursor.close()

            generarReporteExcelYCompartir(herramientas)
        }

        private fun generarReporteExcelYCompartir(herramientas: List<HerramientaReporte>) {
            val sdf = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault())
            val fechaActual = sdf.format(Date())
            val fileName = "Reporte_Inventario_$fechaActual.xls"

            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet("Reporte Inventario")
            val headerRow = sheet.createRow(0)

            // Encabezados del reporte
            headerRow.createCell(0).setCellValue("ID Herramienta")
            headerRow.createCell(1).setCellValue("Codigo Interno")
            headerRow.createCell(2).setCellValue("Nombre")
            headerRow.createCell(3).setCellValue("Estado")
            headerRow.createCell(4).setCellValue("Marca")
            headerRow.createCell(5).setCellValue("Modelo")
            headerRow.createCell(6).setCellValue("Precio")
            headerRow.createCell(7).setCellValue("Empleado (si prestada)")
            headerRow.createCell(8).setCellValue("Fecha Préstamo (si aplicable)")
            headerRow.createCell(9).setCellValue("Activo") // Nuevo campo

            herramientas.forEachIndexed { index, herramienta ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(herramienta.id.toDouble())
                row.createCell(1).setCellValue(herramienta.codigoInterno)
                row.createCell(2).setCellValue(herramienta.nombre)
                row.createCell(3).setCellValue(herramienta.estado)
                row.createCell(4).setCellValue(herramienta.marca)
                row.createCell(5).setCellValue(herramienta.modelo)
                row.createCell(6).setCellValue(herramienta.precio)
                row.createCell(7).setCellValue(herramienta.nombreEmpleado)
                row.createCell(8).setCellValue(herramienta.fechaPrestamo)
                row.createCell(9).setCellValue(if (herramienta.activo == 1) "Activo" else "Inactivo")
            }

            try {
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                Toast.makeText(this, "Reporte generado: ${file.absolutePath}", Toast.LENGTH_LONG).show()

                // Compartir el archivo
                compartirArchivo(file)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
            }
        }

        private fun compartirArchivo(file: File) {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "com.example.inventariolpy.provider", // Autoridad configurada en el manifest
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel" // MIME type para archivos Excel
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Inventario")
                putExtra(Intent.EXTRA_TEXT, "Se adjunta el reporte de inventario generado.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Compartir reporte a través de"))
        }

        companion object {
            const val SOLICITAR_PRESTAMO_REQUEST_CODE = 1
        }
    }


data class HerramientaReporte(
    val id: Int,
    val codigoInterno:String,
    val nombre: String,
    val estado: String,
    val marca: String,
    val modelo: String,
    val precio: Double,
    val nombreEmpleado: String,
    val fechaPrestamo: String,
    val activo:Int
)