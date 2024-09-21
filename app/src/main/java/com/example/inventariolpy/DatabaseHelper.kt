package com.example.inventariolpy


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "inventario.db"
        private const val DATABASE_VERSION = 1

        // Tabla de herramientas
        const val TABLE_HERRAMIENTAS = "herramientas"
        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_ESTADO = "estado"
        const val COL_FECHA_ADQUISICION = "fecha_adquisicion"

        // Tabla de prestamos
        const val TABLE_PRESTAMOS = "prestamos"
        const val COL_NOMBRE_EMPLEADO = "nombre_empleado"
        const val COL_CONTACTO = "contacto"
        const val COL_FIRMA = "firma"
        const val COL_FECHA_PRESTAMO = "fecha_prestamo"
        const val COL_FECHA_DEVOLUCION = "fecha_devolucion"
        const val COL_HERRAMIENTA_ID = "herramienta_id"
        const val COL_ESTADO_PRESTAMO = "estado" // Activo o Devuelto
        const val COL_REPORTE_DEVOLUCION = "reporte_devolucion"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createHerramientasTable = """
            CREATE TABLE $TABLE_HERRAMIENTAS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT,
                $COL_DESCRIPCION TEXT,
                $COL_ESTADO TEXT DEFAULT 'Disponible',
                $COL_FECHA_ADQUISICION TEXT
            )
        """.trimIndent()

        val createPrestamosTable = """
            CREATE TABLE $TABLE_PRESTAMOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE_EMPLEADO TEXT NOT NULL,
                $COL_CONTACTO TEXT,
                $COL_FIRMA TEXT,
                $COL_FECHA_PRESTAMO TEXT NOT NULL,
                $COL_FECHA_DEVOLUCION TEXT,
                $COL_HERRAMIENTA_ID INTEGER,
                $COL_ESTADO_PRESTAMO TEXT DEFAULT 'Activo',
                $COL_REPORTE_DEVOLUCION TEXT,
                FOREIGN KEY($COL_HERRAMIENTA_ID) REFERENCES $TABLE_HERRAMIENTAS($COL_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createHerramientasTable)
        db.execSQL(createPrestamosTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HERRAMIENTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRESTAMOS")
        onCreate(db)
    }
}