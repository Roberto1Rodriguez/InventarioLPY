package com.example.inventariolpy

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class EmpleadoAdapter(private val context: Context, private var cursor: Cursor) : BaseAdapter() {

    private var originalCursor: Cursor = cursor

    override fun getCount(): Int {
        return cursor.count
    }

    override fun getItem(position: Int): Any {
        cursor.moveToPosition(position)
        return cursor
    }

    override fun getItemId(position: Int): Long {
        cursor.moveToPosition(position)
        return cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO))
    }
    fun changeCursor(newCursor: Cursor) {
        cursor.close() // Cerrar el cursor anterior para evitar fugas de memoria
        cursor = newCursor
        notifyDataSetChanged() // Notificar que los datos han cambiado
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_empleado, parent, false)
        cursor.moveToPosition(position)

        val empleadoId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO))
        Log.d("EmpleadoAdapter", "getView - Posición: $position, Empleado ID: $empleadoId")

        val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
        val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO))
        val fotoByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO))

        val tvNombreEmpleado = view.findViewById<TextView>(R.id.tvNombreEmpleado)
        val tvContactoEmpleado = view.findViewById<TextView>(R.id.tvContactoEmpleado)
        val imgFotoEmpleado = view.findViewById<ImageView>(R.id.imgFotoEmpleado)
        val btnEditarEmpleado = view.findViewById<ImageButton>(R.id.btnEditarEmpleado)
        val btnEliminarEmpleado = view.findViewById<ImageButton>(R.id.btnEliminarHerramienta)

        tvNombreEmpleado.text = nombreEmpleado
        tvContactoEmpleado.text = contacto

        if (fotoByteArray != null) {
            val fotoBitmap = BitmapFactory.decodeByteArray(fotoByteArray, 0, fotoByteArray.size)
            imgFotoEmpleado.setImageBitmap(fotoBitmap)
        } else {
            imgFotoEmpleado.setImageResource(R.drawable.ic_placeholder) // Imagen por defecto si no hay foto
        }


        // Configurar el clic en el botón de edición
        btnEditarEmpleado.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Confirmación")
                .setMessage("¿Desea editar al empleado \"$nombreEmpleado\"?")
                .setPositiveButton("Sí") { _, _ ->
                    cursor.moveToPosition(position)
                    val intent = Intent(context, EditarEmpleadoActivity::class.java)
                    intent.putExtra(
                        "empleadoId",
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO))
                    )
                    context.startActivity(intent)
                }
                .setNegativeButton("No", null)
                .show()
        }
        btnEliminarEmpleado.setOnClickListener {
            validarYEliminarEmpleado(empleadoId, nombreEmpleado)
        }

        return view
    }
    private fun validarYEliminarEmpleado(empleadoId: Int, nombreEmpleado: String) {
        val dbHelper = DatabaseHelper(context)
        val db = dbHelper.readableDatabase

        // Verificar si el empleado tiene préstamos activos
        val query = """
        SELECT COUNT(*) AS prestamosActivos 
        FROM ${DatabaseHelper.TABLE_PRESTAMOS}
        WHERE ${DatabaseHelper.COL_EMPLEADO_ID} = ? AND ${DatabaseHelper.COL_ESTADO_PRESTAMO} = 'Activo'
    """
        val cursor = db.rawQuery(query, arrayOf(empleadoId.toString()))
        var prestamosActivos = 0
        if (cursor.moveToFirst()) {
            prestamosActivos = cursor.getInt(cursor.getColumnIndexOrThrow("prestamosActivos"))
        }
        cursor.close()

        if (prestamosActivos > 0) {
            // Mostrar mensaje de error si hay préstamos activos
            Toast.makeText(context, "No se puede eliminar. El empleado tiene préstamos activos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Si no hay préstamos activos, mostrar cuadro de confirmación
        AlertDialog.Builder(context)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a $nombreEmpleado?")
            .setPositiveButton("Sí") { dialog, _ ->
                eliminarEmpleado(empleadoId)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun eliminarEmpleado(empleadoId: Int) {
        val dbHelper = DatabaseHelper(context)
        val db = dbHelper.writableDatabase

        val contentValues = ContentValues().apply {
            put("estado", "Inactivo") // Cambiar el estado a "Inactivo"
        }
        db.update(DatabaseHelper.TABLE_EMPLEADOS, contentValues, "id = ?", arrayOf(empleadoId.toString()))
        Toast.makeText(context, "Empleado eliminado correctamente", Toast.LENGTH_SHORT).show()

        // Recargar la lista después de la eliminación
        val newCursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            arrayOf(DatabaseHelper.COL_ID_EMPLEADO, DatabaseHelper.COL_NOMBRE_EMPLEADO, DatabaseHelper.COL_CONTACTO, DatabaseHelper.COL_FOTO),
            "estado != ?", // Solo empleados activos
            arrayOf("Inactivo"),
            null,
            null,
            null
        )
        changeCursor(newCursor)
    }


    // Método para filtrar los datos
    fun filter(query: String) {
        val dbHelper = DatabaseHelper(context)
        val db = dbHelper.readableDatabase
        val selection = "${DatabaseHelper.COL_NOMBRE_EMPLEADO} LIKE ? OR ${DatabaseHelper.COL_CONTACTO} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%")

        cursor = db.query(
            DatabaseHelper.TABLE_EMPLEADOS,
            arrayOf(
                DatabaseHelper.COL_ID_EMPLEADO,
                DatabaseHelper.COL_NOMBRE_EMPLEADO,
                DatabaseHelper.COL_CONTACTO,
                DatabaseHelper.COL_FOTO
            ),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        notifyDataSetChanged()
    }
}