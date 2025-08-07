package com.example.inventariolpy

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.database.getBlobOrNull
import com.example.inventariolpy.DatabaseHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ReporteInventarioActivity : AppCompatActivity() {

    private lateinit var tableLayout: TableLayout
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var searchView: SearchView
    private lateinit var checkBoxPrestadas: CheckBox
    private lateinit var checkBoxDisponibles: CheckBox
    private lateinit var checkBoxRotasPerdidas: CheckBox
    private lateinit var checkBoxInactivas:CheckBox

    private var listaHerramientas = listOf<HerramientaReporte>() // Lista completa de herramientas
    private var listaFiltrada = listOf<HerramientaReporte>() // Lista filtrada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_inventario)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        tableLayout = findViewById(R.id.tableLayoutReporte)
        searchView = findViewById(R.id.searchView)
        checkBoxPrestadas = findViewById(R.id.checkBoxPrestadas)
        checkBoxDisponibles = findViewById(R.id.checkBoxDisponibles)
        checkBoxRotasPerdidas = findViewById(R.id.checkBoxRotasPerdidas)
        checkBoxInactivas=findViewById(R.id.checkBoxInactivas)

        dbHelper = DatabaseHelper(this)

        // Cargar herramientas desde la base de datos
        listaHerramientas = generarReporteInventario()
        listaFiltrada = listaHerramientas

        mostrarHerramientas(listaFiltrada)

        // 🔹 Búsqueda en tiempo real
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarHerramientas()
                return true
            }
        })

        // 🔹 Listeners para CheckBoxes (Filtrado dinámico)
        val checkBoxListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            filtrarHerramientas()
        }
        checkBoxPrestadas.setOnCheckedChangeListener(checkBoxListener)
        checkBoxDisponibles.setOnCheckedChangeListener(checkBoxListener)
        checkBoxRotasPerdidas.setOnCheckedChangeListener(checkBoxListener)
        checkBoxInactivas.setOnCheckedChangeListener(checkBoxListener)

    }

    private fun mostrarHerramientas(lista: List<HerramientaReporte>) {
        tableLayout.removeAllViews()
        agregarEncabezados()

        lista.forEach { herramienta ->
            agregarFila(herramienta)
        }
    }

    private fun filtrarHerramientas() {
        val textoBusqueda = searchView.query.toString().lowercase(Locale.getDefault())

        listaFiltrada = listaHerramientas.filter { herramienta ->
            val coincideBusqueda = herramienta.nombre.lowercase(Locale.getDefault()).contains(textoBusqueda)

            // 🔹 Determinar si se mostrarán activas o inactivas
            val mostrarSoloInactivas = checkBoxInactivas.isChecked
            val coincideFiltroActivo = if (mostrarSoloInactivas) herramienta.activo == 0 else herramienta.activo == 1

            // 🔹 Aplicar filtros de estado
            val coincideFiltroEstado = (checkBoxPrestadas.isChecked && herramienta.estado == "Prestada") ||
                    (checkBoxDisponibles.isChecked && herramienta.estado == "Disponible") ||
                    (checkBoxRotasPerdidas.isChecked && (herramienta.estado == "Rota" || herramienta.estado == "Perdida"))

            // 🔹 Si no hay ningún checkbox de estado marcado, mostrar todas (según activas/inactivas)
            val aplicarFiltroEstado = checkBoxPrestadas.isChecked || checkBoxDisponibles.isChecked || checkBoxRotasPerdidas.isChecked
            val cumpleEstado = if (aplicarFiltroEstado) coincideFiltroEstado else true

            // 🔹 Aplicar filtros combinados
            coincideBusqueda && coincideFiltroActivo && cumpleEstado
        }

        mostrarHerramientas(listaFiltrada)
    }

    private fun agregarEncabezados() {
        val encabezados = listOf("","ID", "Código", "Nombre", "Estado", "Marca", "Modelo", "Precio", "Empleado", "Fecha", "Activo")
        val tableRow = TableRow(this)

        encabezados.forEach { texto ->
            val textView = TextView(this).apply {
                text = texto
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.color.darker_gray)
                setTextColor(Color.WHITE)
            }
            tableRow.addView(textView)
        }

        tableLayout.addView(tableRow)
    }

    private fun agregarFila(herramienta: HerramientaReporte) {
        val tableRow = TableRow(this)

        val colorFondo = when (herramienta.estado) {
            "Disponible" -> ContextCompat.getColor(this, R.color.verde)
            "Prestada" -> ContextCompat.getColor(this, R.color.rojo)
            "Rota", "Perdida" -> ContextCompat.getColor(this, R.color.amarillo)
            else -> Color.WHITE
        }

        // 🔹 Agregar emoji si está Rota o Perdida
        val iconoEmoji = TextView(this).apply {
            text = if (herramienta.estado == "Rota" || herramienta.estado == "Perdida") "🧾" else ""
            textSize = 20f
            setPadding(16, 8, 16, 8)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT) // Fondo separado
            setTextColor(Color.WHITE)

            if (text.isNotEmpty()) {
                setOnClickListener {
                    generarPDFDesdeCero(herramienta)
                }
            }
        }
        tableRow.addView(iconoEmoji)

        val valores = listOf(
            herramienta.id.toString(),
            herramienta.codigoInterno,
            herramienta.nombre,
            herramienta.estado,
            herramienta.marca,
            herramienta.modelo,
            herramienta.precio.toString(),
            herramienta.nombreEmpleado,
            herramienta.fechaPrestamo,
            if (herramienta.activo == 1) "Activo" else "Inactivo"
        )

        valores.forEach { texto ->
            val textView = TextView(this).apply {
                text = texto
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER
                setBackgroundColor(colorFondo)
                setTextColor(Color.WHITE)
            }
            tableRow.addView(textView)
        }

        tableLayout.addView(tableRow)
    }
    private fun generarPDFDesdeCero(herramienta: HerramientaReporte) {
        try {
            val outputDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val outputFile = File(outputDir, "Pagare_${herramienta.nombre}.pdf")

            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc, PageSize.LETTER)
            document.setMargins(50f, 40f, 50f, 40f)

            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            val fechaActual = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("dd 'de' MM 'de' yyyy")
            val fechaTexto = fechaActual.format(formatter)

// --- Imagen (logotipo en la esquina superior izquierda) ---
            val imageStream = assets.open("titulo.png")
            val imageData = ImageDataFactory.create(imageStream.readBytes())
            val image = Image(imageData)
                .scaleToFit(150f, 150f) // Tamaño deseado del logo
                .setFixedPosition(10f, 720f) // Posición absoluta en la página (X, Y)
            document.add(image)
            // --- Título principal ---
            val titulo = Paragraph("RESPONSIVA POR PÉRDIDA O\nDAÑO DE HERRAMIENTA")
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(bold)
                .setFontSize(16f)
                .setMarginBottom(20f)
            document.add(titulo)

            // --- Lugar y fecha actual ---
            val lugar =  "Sabinas, Coahuila"
            val lugarYFecha = Paragraph("Lugar y fecha: $lugar, $fechaTexto")
                .setFont(font)
                .setFontSize(12f)
                .setMarginBottom(20f)
            document.add(lugarYFecha)

            // --- Cuerpo del documento ---
            val cuerpo1 = """
            A quien corresponda:

            Por medio de la presente, yo, ${herramienta.nombreEmpleado}, me hago
            responsable por la pérdida / daño de la herramienta que a continuación se detalla:
        """.trimIndent()

            document.add(Paragraph(cuerpo1).setFont(font).setFontSize(12f).setMarginBottom(10f))

            // --- Datos de la herramienta ---
            document.add(Paragraph("• Nombre de la herramienta: ${herramienta.nombre}").setFont(font))
            document.add(Paragraph("• Marca/Modelo (si aplica): ${herramienta.marca} / ${herramienta.modelo}").setFont(font))
            document.add(Paragraph("• Número de serie (si aplica): ${herramienta.codigoInterno}").setFont(font))
            document.add(Paragraph("• Fecha en que ocurrió: ${herramienta.fechaPrestamo}").setFont(font))
            document.add(Paragraph("").setMarginBottom(10f))

            // --- Reconocimiento y compromiso ---
            val compromiso = """
            Reconozco que dicha herramienta fue entregada a mi resguardo y que su pérdida o daño ha sido
            consecuencia de mi uso o responsabilidad.
            Me comprometo a:

            Cubrir el costo de reposición o reparación, el cual asciende a: $${"%.2f".format(herramienta.precio)}
        """.trimIndent()

            document.add(Paragraph(compromiso).setFont(font).setFontSize(12f).setMarginBottom(10f))

            val final = """
            Acepto que esta acción queda asentada y que se tomarán las medidas correspondientes según el
            reglamento establecido.
            Asimismo, manifiesto que el descuento para el pago del artículo se apegará a lo dispuesto por el
            Artículo 110 de la Ley Federal del Trabajo y el Artículo 29 del Reglamento Interno de la Empresa.
        """.trimIndent()

            document.add(Paragraph(final).setFont(font).setFontSize(12f).setMarginBottom(20f))

            // Crear una tabla de una sola columna
            val firmaTabla = Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
            val firmaEmpleadoImage = Image(ImageDataFactory.create(herramienta.firmaEmpleado))
                .scaleToFit(200f, 100f)
                .setRotationAngle(Math.toRadians(270.0)) // Ajusta si se ve girada
                .setHorizontalAlignment(HorizontalAlignment.CENTER)// Aumenta el ancho y alto permitidos
// Imagen de firma y nombre centrado arriba de la línea
            val firmaEmpleadoCell = Cell()
                .add(firmaEmpleadoImage.setHorizontalAlignment(HorizontalAlignment.CENTER))
                .add(Paragraph(herramienta.nombreEmpleado).setTextAlignment(TextAlignment.CENTER))
                .add(Paragraph("__________________________").setTextAlignment(TextAlignment.CENTER))
                .add(Paragraph("Nombre y firma del responsable").setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)

            firmaTabla.addCell(firmaEmpleadoCell)

            document.add(firmaTabla)

            document.close()

            // Abrir el PDF
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", outputFile)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_LONG).show()
        }
    }
    private fun generarReporteInventario(): List<HerramientaReporte> {
        val db = dbHelper.readableDatabase
        val query = """
        SELECT h.${DatabaseHelper.COL_ID_HERRAMIENTA} AS id, 
               h.${DatabaseHelper.COL_CODIGO_INTERNO} AS codigoInterno,
               h.${DatabaseHelper.COL_NOMBRE} AS nombre, 
               h.${DatabaseHelper.COL_ESTADO} AS estado, 
               h.${DatabaseHelper.COL_MARCA} AS marca, 
               h.${DatabaseHelper.COL_MODELO} AS modelo, 
               h.${DatabaseHelper.COL_PRECIO} AS precio, 
               h.activo AS activo,
               (SELECT p.${DatabaseHelper.COL_NOMBRE_EMPLEADO} 
                FROM ${DatabaseHelper.TABLE_PRESTAMOS} pr
                JOIN ${DatabaseHelper.TABLE_EMPLEADOS} p 
                ON pr.${DatabaseHelper.COL_EMPLEADO_ID} = p.${DatabaseHelper.COL_ID_EMPLEADO}
                WHERE pr.${DatabaseHelper.COL_ID_PRESTAMO} IN (
                    SELECT ${DatabaseHelper.COL_PRESTAMO_ID}
                    FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS}
                    WHERE ${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
                )
                ORDER BY pr.${DatabaseHelper.COL_FECHA_PRESTAMO} DESC
                LIMIT 1
               ) AS nombreEmpleado,
               (SELECT pr.${DatabaseHelper.COL_FECHA_PRESTAMO} 
                FROM ${DatabaseHelper.TABLE_PRESTAMOS} pr
                WHERE pr.${DatabaseHelper.COL_ID_PRESTAMO} IN (
                    SELECT ${DatabaseHelper.COL_PRESTAMO_ID}
                    FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS}
                    WHERE ${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
                )
                ORDER BY pr.${DatabaseHelper.COL_FECHA_PRESTAMO} DESC
                LIMIT 1
               ) AS fecha_prestamo,
               (SELECT p.${DatabaseHelper.COL_FIRMA} 
 FROM ${DatabaseHelper.TABLE_PRESTAMOS} pr
 JOIN ${DatabaseHelper.TABLE_EMPLEADOS} p 
 ON pr.${DatabaseHelper.COL_EMPLEADO_ID} = p.${DatabaseHelper.COL_ID_EMPLEADO}
 WHERE pr.${DatabaseHelper.COL_ID_PRESTAMO} IN (
     SELECT ${DatabaseHelper.COL_PRESTAMO_ID}
     FROM ${DatabaseHelper.TABLE_PRESTAMO_HERRAMIENTAS}
     WHERE ${DatabaseHelper.COL_HERRAMIENTA_ID} = h.${DatabaseHelper.COL_ID_HERRAMIENTA}
 )
 ORDER BY pr.${DatabaseHelper.COL_FECHA_PRESTAMO} DESC
 LIMIT 1
) AS firmaEmpleado
        FROM ${DatabaseHelper.TABLE_HERRAMIENTAS} h
        ORDER BY 
            CASE h.${DatabaseHelper.COL_ESTADO}
                WHEN 'Prestada' THEN 1
                WHEN 'Disponible' THEN 2
                ELSE 3
            END
    """
        val cursor = db.rawQuery(query, null)
        val herramientas = mutableListOf<HerramientaReporte>()

        while (cursor.moveToNext()) {
            // 🔹 Convertir fecha de milisegundos a formato legible
            val fechaMillis = cursor.getLongOrNull("fecha_prestamo") ?: 0L
            val fechaPrestamo = if (fechaMillis > 0) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaMillis))
            } else {
                "N/A"
            }

            herramientas.add(
                HerramientaReporte(
                    id = cursor.getIntOrNull("id") ?: 0,
                    codigoInterno = cursor.getStringOrNull("codigoInterno") ?: "N/A",
                    nombre = cursor.getStringOrNull("nombre") ?: "N/A",
                    estado = cursor.getStringOrNull("estado") ?: "Desconocido",
                    marca = cursor.getStringOrNull("marca") ?: "Sin marca",
                    modelo = cursor.getStringOrNull("modelo") ?: "Sin modelo",
                    precio = cursor.getDoubleOrNull("precio") ?: 0.0,
                    nombreEmpleado = cursor.getStringOrNull("nombreEmpleado") ?: "N/A",
                    fechaPrestamo = fechaPrestamo,
                    activo = cursor.getIntOrNull("activo") ?: 0,
                    firmaEmpleado = cursor.getBlobOrNull(cursor.getColumnIndexOrThrow("firmaEmpleado"))

                )
            )
        }
        cursor.close()
        return herramientas
    }
}
// Extensión para obtener String o null
fun Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index != -1 && !isNull(index)) getString(index) else null
}

// Extensión para obtener Int o null
fun Cursor.getIntOrNull(columnName: String): Int? {
    val index = getColumnIndex(columnName)
    return if (index != -1 && !isNull(index)) getInt(index) else null
}

// Extensión para obtener Double o null
fun Cursor.getDoubleOrNull(columnName: String): Double? {
    val index = getColumnIndex(columnName)
    return if (index != -1 && !isNull(index)) getDouble(index) else null
}
// Extensión para obtener Long o null (Útil para fechas en milisegundos)
fun Cursor.getLongOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index != -1 && !isNull(index)) getLong(index) else null
}
