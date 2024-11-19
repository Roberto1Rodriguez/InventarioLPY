package com.example.inventariolpy

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.text.InputType
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

        val nombreEmpleado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE_EMPLEADO))
        val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACTO))
        val fotoByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO))

        val tvNombreEmpleado = view.findViewById<TextView>(R.id.tvNombreEmpleado)
        val tvContactoEmpleado = view.findViewById<TextView>(R.id.tvContactoEmpleado)
        val imgFotoEmpleado = view.findViewById<ImageView>(R.id.imgFotoEmpleado)
        val btnEditarEmpleado = view.findViewById<ImageButton>(R.id.btnEditarEmpleado)

        tvNombreEmpleado.text = nombreEmpleado
        tvContactoEmpleado.text = contacto

        // Convertir la foto almacenada en byte array a Bitmap y establecerla en ImageView
        if (fotoByteArray != null) {
            val fotoBitmap = BitmapFactory.decodeByteArray(fotoByteArray, 0, fotoByteArray.size)
            imgFotoEmpleado.setImageBitmap(fotoBitmap)
        } else {
            imgFotoEmpleado.setImageResource(R.drawable.ic_placeholder) // Imagen por defecto si no hay foto
        }

        // Configurar el clic en el botón de edición
        btnEditarEmpleado.setOnClickListener {
            mostrarDialogoClaveAcceso { claveCorrecta ->
                if (claveCorrecta) {
                    val intent = Intent(context, EditarEmpleadoActivity::class.java)
                    intent.putExtra("empleadoId", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID_EMPLEADO)))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Clave incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun mostrarDialogoClaveAcceso(onClaveVerificada: (Boolean) -> Unit) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Ingrese la clave de acceso"
        }

        AlertDialog.Builder(context)
            .setTitle("Verificación de Clave")
            .setMessage("Ingrese la clave de acceso para editar el empleado")
            .setView(input)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val claveIngresada = input.text.toString()
                val claveCorrecta = claveIngresada == "Balance9731"
                onClaveVerificada(claveCorrecta)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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