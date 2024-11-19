package com.example.inventariolpy


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "inventario.db"
        const val DATABASE_VERSION = 2  // Incrementamos la versión para ejecutar `onUpgrade()`

        // Tabla de empleados
        const val TABLE_EMPLEADOS = "empleados"
        const val COL_ID_EMPLEADO = "id"
        const val COL_NOMBRE_EMPLEADO = "nombre"
        const val COL_DIRECCION = "direccion"
        const val COL_CURP = "curp"
        const val COL_FOTO = "foto"
        const val COL_FIRMA = "firma"
        const val COL_RFC = "rfc"
        const val COL_CONTACTO = "contacto"
        const val COL_NFC_ID = "nfc_id"
        const val COL_QR_IDENTIFICADOR = "qr_identificador"

        // Tabla de herramientas
        const val TABLE_HERRAMIENTAS = "herramientas"
        const val COL_ID_HERRAMIENTA = "id"
        const val COL_NOMBRE="nombre"
        const val COL_MARCA = "marca"
        const val COL_MODELO = "modelo"
        const val COL_SERIE = "serie"
        const val COL_CODIGO_INTERNO = "codigo_interno"
        const val COL_FOTO_HERRAMIENTA = "foto_herramienta"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_ESTADO = "estado"
        const val COL_PRECIO = "precio"

        // Tabla de préstamos
        const val TABLE_PRESTAMOS = "prestamos"
        const val COL_ID_PRESTAMO = "id"
        const val COL_EMPLEADO_ID = "empleado_id"  // Relación con la tabla empleados
        const val COL_FECHA_PRESTAMO = "fecha_prestamo"
        const val COL_ESTADO_PRESTAMO = "estado_prestamo"
        const val COL_FECHA_DEVOLUCION = "fecha_devolucion" // Fecha de devolución de la herramienta
        const val COL_REPORTE_DEVOLUCION = "reporte_devolucion"

        // Tabla intermedia para relacionar préstamos con herramientas
        const val TABLE_PRESTAMO_HERRAMIENTAS = "prestamo_herramientas"
        const val COL_PRESTAMO_ID = "prestamo_id"
        const val COL_HERRAMIENTA_ID = "herramienta_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createEmpleadosTable = """
            CREATE TABLE $TABLE_EMPLEADOS (
                $COL_ID_EMPLEADO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE_EMPLEADO TEXT,
                $COL_DIRECCION TEXT,
                $COL_CURP TEXT,
                $COL_FOTO BLOB,
                $COL_FIRMA BLOB,
                $COL_RFC TEXT,
                $COL_CONTACTO TEXT,
                $COL_NFC_ID TEXT,
                $COL_QR_IDENTIFICADOR TEXT
            )
        """.trimIndent()

        val createHerramientasTable = """
            CREATE TABLE $TABLE_HERRAMIENTAS (
                $COL_ID_HERRAMIENTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT,
                $COL_MARCA TEXT,
                $COL_MODELO TEXT,
                $COL_SERIE TEXT,
                $COL_CODIGO_INTERNO TEXT,
                $COL_FOTO_HERRAMIENTA BLOB,
                $COL_DESCRIPCION TEXT,
                $COL_PRECIO REAL DEFAULT 0,
                $COL_ESTADO TEXT DEFAULT 'Disponible'
            )
        """.trimIndent()

        val createPrestamosTable = """
            CREATE TABLE $TABLE_PRESTAMOS (
                $COL_ID_PRESTAMO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EMPLEADO_ID INTEGER,
                $COL_FECHA_PRESTAMO TEXT,
                $COL_ESTADO_PRESTAMO TEXT,
                $COL_FECHA_DEVOLUCION TEXT,
                $COL_REPORTE_DEVOLUCION TEXT,
                FOREIGN KEY($COL_EMPLEADO_ID) REFERENCES $TABLE_EMPLEADOS($COL_ID_EMPLEADO)
            )
        """.trimIndent()

        val createPrestamoHerramientasTable = """
            CREATE TABLE $TABLE_PRESTAMO_HERRAMIENTAS (
                $COL_PRESTAMO_ID INTEGER,
                $COL_HERRAMIENTA_ID INTEGER,
                FOREIGN KEY($COL_PRESTAMO_ID) REFERENCES $TABLE_PRESTAMOS($COL_ID_PRESTAMO),
                FOREIGN KEY($COL_HERRAMIENTA_ID) REFERENCES $TABLE_HERRAMIENTAS($COL_ID_HERRAMIENTA),
                PRIMARY KEY ($COL_PRESTAMO_ID, $COL_HERRAMIENTA_ID)
            )
        """.trimIndent()

        db.execSQL(createEmpleadosTable)
        db.execSQL(createHerramientasTable)
        db.execSQL(createPrestamosTable)
        db.execSQL(createPrestamoHerramientasTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRESTAMO_HERRAMIENTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRESTAMOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HERRAMIENTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMPLEADOS")

        onCreate(db)
    }
}