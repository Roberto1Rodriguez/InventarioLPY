package com.example.inventariolpy


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "inventario.db"
        const val DATABASE_VERSION = 1

        // Tabla de empleados
        const val TABLE_EMPLEADOS = "empleados"
        const val COL_ID_EMPLEADO = "id"
        const val COL_NOMBRE_EMPLEADO = "nombre"
        const val COL_CONTACTO = "contacto"
        const val COL_CLAVE_ACCESO = "clave_acceso"

        // Tabla de herramientas
        const val TABLE_HERRAMIENTAS = "herramientas"
        const val COL_ID_HERRAMIENTA = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_ESTADO = "estado"
        const val COL_FECHA_ADQUISICION = "fecha_adquisicion"

        // Tabla de préstamos
        const val TABLE_PRESTAMOS = "prestamos"
        const val COL_ID_PRESTAMO = "id"
        const val COL_EMPLEADO_ID = "empleado_id"  // Relación con la tabla empleados
        const val COL_FECHA_PRESTAMO = "fecha_prestamo"
        const val COL_ESTADO_PRESTAMO = "estado_prestamo"
        const val COL_FIRMA = "firma"
        const val COL_FECHA_DEVOLUCION = "fecha_devolucion" // Fecha de devolución de la herramienta
        const val COL_REPORTE_DEVOLUCION = "reporte_devolucion"

        // Tabla intermedia para relacionar préstamos con herramientas
        const val TABLE_PRESTAMO_HERRAMIENTAS = "prestamo_herramientas"
        const val COL_PRESTAMO_ID = "prestamo_id"
        const val COL_HERRAMIENTA_ID = "herramienta_id"
    }


        override fun onCreate(db: SQLiteDatabase) {
            // Crear la tabla de empleados
            val createEmpleadosTable = """
            CREATE TABLE $TABLE_EMPLEADOS (
                $COL_ID_EMPLEADO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE_EMPLEADO TEXT,
                $COL_CONTACTO TEXT,
                $COL_CLAVE_ACCESO TEXT
            )
        """.trimIndent()

            // Crear la tabla de herramientas
            val createHerramientasTable = """
            CREATE TABLE $TABLE_HERRAMIENTAS (
                $COL_ID_HERRAMIENTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT,
                $COL_DESCRIPCION TEXT,
                $COL_ESTADO TEXT DEFAULT 'Disponible',
                $COL_FECHA_ADQUISICION TEXT
            )
        """.trimIndent()

            // Crear la tabla de préstamos
            val createPrestamosTable = """
            CREATE TABLE $TABLE_PRESTAMOS (
                $COL_ID_PRESTAMO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EMPLEADO_ID INTEGER,  
                $COL_FECHA_PRESTAMO TEXT,
                $COL_ESTADO_PRESTAMO TEXT,
                $COL_FIRMA TEXT,
                $COL_FECHA_DEVOLUCION TEXT, 
                $COL_REPORTE_DEVOLUCION TEXT,
                FOREIGN KEY($COL_EMPLEADO_ID) REFERENCES $TABLE_EMPLEADOS($COL_ID_EMPLEADO)
            )
        """.trimIndent()

            // Crear la tabla intermedia para relacionar préstamos y herramientas
            val createPrestamoHerramientasTable = """
            CREATE TABLE $TABLE_PRESTAMO_HERRAMIENTAS (
                $COL_PRESTAMO_ID INTEGER,
                $COL_HERRAMIENTA_ID INTEGER,
                FOREIGN KEY($COL_PRESTAMO_ID) REFERENCES $TABLE_PRESTAMOS($COL_ID_PRESTAMO),
                FOREIGN KEY($COL_HERRAMIENTA_ID) REFERENCES $TABLE_HERRAMIENTAS($COL_ID_HERRAMIENTA),
                PRIMARY KEY ($COL_PRESTAMO_ID, $COL_HERRAMIENTA_ID)
            )
        """.trimIndent()

            // Ejecutar las consultas SQL para crear las tablas
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