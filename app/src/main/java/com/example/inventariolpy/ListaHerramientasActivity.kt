package com.example.inventariolpy
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
                            val classPopupHelper = Class.forName(
                                menuPopupHelper.javaClass.name
                            )
                            val setForceIcons = classPopupHelper.getMethod(
                                "setForceShowIcon", Boolean::class.javaPrimitiveType
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
                        generarReporteInventario() // Método de ejemplo para generar reporte
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
        // Modificación del listener que maneja la visibilidad del FloatingActionButton
        adapter = HerramientaAdapter(this, herramientas) { seleccionadas ->
            herramientasSeleccionadas.clear()
            herramientasSeleccionadas.addAll(seleccionadas)

            // Mostrar u ocultar el FloatingActionButton con animación
            if (herramientasSeleccionadas.isNotEmpty()) {
                fabSolicitarPrestamo.show() // Muestra el FAB con animación
            } else {
                fabSolicitarPrestamo.hide() // Oculta el FAB con animación
            }
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        fabSolicitarPrestamo.setOnClickListener {
            if (herramientasSeleccionadas.isNotEmpty()) {
                iniciarSolicitudPrestamo()
            } else {
                Toast.makeText(this, "Selecciona al menos una herramienta", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        cargarListaHerramientas()
    }

    private fun cargarListaHerramientas() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            DatabaseHelper.COL_NOMBRE,
            DatabaseHelper.COL_ESTADO,
            DatabaseHelper.COL_CODIGO_INTERNO,
            DatabaseHelper.COL_ID_HERRAMIENTA
        )
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_HERRAMIENTAS,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        herramientas.clear()
        while (cursor.moveToNext()) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE))
            val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTADO))
            val codigoInterno=cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODIGO_INTERNO))
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_HERRAMIENTA))

            val herramienta = Herramienta(id, nombre, estado,codigoInterno=codigoInterno)
            herramientas.add(herramienta)
        }
        cursor.close()

        adapter.notifyDataSetChanged()
    }

    private fun filtrarHerramientas(query: String) {
        val herramientasFiltradas = herramientas.filter {
            it.nombre.contains(query, ignoreCase = true)

        }
        adapter.actualizarLista(herramientasFiltradas)
    }

    private fun iniciarSolicitudPrestamo() {
        val intent = Intent(this, SolicitarPrestamoActivity::class.java)
        intent.putParcelableArrayListExtra(
            "herramientasSeleccionadas",
            ArrayList(herramientasSeleccionadas)
        )
        startActivity(intent)
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
        CASE 
            WHEN h.${DatabaseHelper.COL_ESTADO} = 'Prestada' THEN 
                (SELECT p.${DatabaseHelper.COL_NOMBRE_EMPLEADO} 
                 FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS} ph
                 JOIN ${DatabaseHelper.TABLE_PRESTAMOS} pr 
                 ON ph.${DatabaseHelper.COL_PRESTAMO_ID} = pr.${DatabaseHelper.COL_ID_PRESTAMO} 
                 AND pr.${DatabaseHelper.COL_ESTADO_PRESTAMO} = 'Activo'
                 JOIN ${DatabaseHelper.TABLE_EMPLEADOS} p 
                 ON pr.${DatabaseHelper.COL_EMPLEADO_ID} = p.${DatabaseHelper.COL_ID_EMPLEADO}
                 WHERE ph.${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
                 LIMIT 1
                ) 
            ELSE 'N/A' 
        END AS nombreEmpleado, 
        CASE 
            WHEN h.${DatabaseHelper.COL_ESTADO} = 'Prestada' THEN 
                (SELECT pr.${DatabaseHelper.COL_FECHA_PRESTAMO} 
                 FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS} ph
                 JOIN ${DatabaseHelper.TABLE_PRESTAMOS} pr 
                 ON ph.${DatabaseHelper.COL_PRESTAMO_ID} = pr.${DatabaseHelper.COL_ID_PRESTAMO} 
                 AND pr.${DatabaseHelper.COL_ESTADO_PRESTAMO} = 'Activo'
                 WHERE ph.${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
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
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id")) // Usa el alias "id"
            val codigoInterno = cursor.getString(cursor.getColumnIndexOrThrow("codigoInterno")) ?: "N/A"
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "N/A"
            val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: "N/A"
            val marca = cursor.getString(cursor.getColumnIndexOrThrow("marca")) ?: "N/A"
            val modelo = cursor.getString(cursor.getColumnIndexOrThrow("modelo")) ?: "N/A"
            val precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio"))
            val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow("nombreEmpleado")) ?: "N/A" // Usa el alias "nombreEmpleado"
            val fechaPrestamo = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_prestamo"))
            herramientas.add(
                HerramientaReporte(
                    id,
                    codigoInterno,
                    nombre,
                    estado,
                    marca,
                    modelo,
                    precio,
                    nombreEmpleado ?: "N/A",
                    if (fechaPrestamo > 0) SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(
                        Date(fechaPrestamo)
                    ) else "N/A"
                )
            )
            Log.d("CursorDebug", "Nombre Herramienta: $nombre, Nombre Empleado: $nombreEmpleado")

        }
        cursor.close()

        generarReporteExcel(herramientas)
    }

    private fun generarReporteExcel(herramientas: List<HerramientaReporte>) {
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault())
        val fechaActual = sdf.format(Date())
        val fileName = "Reporte_Inventario_$fechaActual.xls"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    val workbook = HSSFWorkbook()
                    val sheet = workbook.createSheet("Reporte Inventario")
                    val headerRow = sheet.createRow(0)
                    headerRow.createCell(0).setCellValue("ID Herramienta")
                    headerRow.createCell(1).setCellValue("Codigo Interno")
                    headerRow.createCell(2).setCellValue("Nombre")
                    headerRow.createCell(3).setCellValue("Estado")
                    headerRow.createCell(4).setCellValue("Marca")
                    headerRow.createCell(5).setCellValue("Modelo")
                    headerRow.createCell(6).setCellValue("Precio")
                    headerRow.createCell(7).setCellValue("Empleado (si prestada)")
                    headerRow.createCell(8).setCellValue("Fecha Préstamo (si aplicable)")

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
                    }

                    contentResolver.openOutputStream(uri).use { outputStream ->
                        workbook.write(outputStream)
                    }
                    workbook.close()

                    Toast.makeText(this, "Reporte descargado: $fileName", Toast.LENGTH_LONG).show()
                    mostrarNotificacionDescarga(fileName)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            try {
                val workbook = HSSFWorkbook()
                val sheet = workbook.createSheet("Reporte Inventario")
                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("ID Herramienta")
                headerRow.createCell(1).setCellValue("Codigo Interno")
                headerRow.createCell(2).setCellValue("Nombre")
                headerRow.createCell(3).setCellValue("Estado")
                headerRow.createCell(4).setCellValue("Marca")
                headerRow.createCell(5).setCellValue("Modelo")
                headerRow.createCell(6).setCellValue("Precio")
                headerRow.createCell(7).setCellValue("Empleado (si prestada)")
                headerRow.createCell(8).setCellValue("Fecha Préstamo (si aplicable)")

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
                }

                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                Toast.makeText(this, "Reporte descargado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                mostrarNotificacionDescarga(fileName)

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarNotificacionDescarga(fileName: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "descarga_reporte_excel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Descargas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación de Descarga de Reporte"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga Completa")
            .setContentText("El reporte $fileName ha sido descargado")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
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
    val fechaPrestamo: String
)