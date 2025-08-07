package com.example.inventariolpy

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class HistorialAdapter(private val context: Context, private var empleados: List<Empleado>) :
    BaseAdapter() {

    private var empleadosFiltrados: List<Empleado> = empleados // Lista para los empleados filtrados

    override fun getCount(): Int {
        return empleadosFiltrados.size
    }

    override fun getItem(position: Int): Any {
        return empleadosFiltrados[position]
    }

    override fun getItemId(position: Int): Long {
        return empleadosFiltrados[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_historial, parent, false)

        val tvNombreEmpleado = view.findViewById<TextView>(R.id.tvNombreEmpleado)
        val tvContactoEmpleado = view.findViewById<TextView>(R.id.tvContactoEmpleado)
        val imgFotoEmpleado = view.findViewById<ImageView>(R.id.imgFotoEmpleado)

        val empleado = empleadosFiltrados[position]

        tvNombreEmpleado.text = empleado.nombre
        tvContactoEmpleado.text = empleado.contacto ?: "Sin contacto"

        if (empleado.foto != null) {
            val fotoBitmap = BitmapFactory.decodeByteArray(empleado.foto, 0, empleado.foto.size)
            imgFotoEmpleado.setImageBitmap(fotoBitmap)
        } else {
            imgFotoEmpleado.setImageResource(R.drawable.ic_placeholder)
        }

        // Ahora al hacer clic en el empleado, mostramos el popup con las herramientas prestadas
        view.setOnClickListener {
            (context as HistorialEmpleadoActivity).mostrarPopupHerramientasPrestadas(empleado)
        }

        return view
    }

    fun filter(query: String) {
        empleadosFiltrados = if (query.isEmpty()) {
            empleados
        } else {
            empleados.filter {
                it.nombre.startsWith(query, ignoreCase = true) || // Solo si el nombre empieza con 'query'
                        (it.contacto?.startsWith(query, ignoreCase = true) ?: false) // Solo si el contacto empieza con 'query'
            }
        }
        notifyDataSetChanged()
    }

    fun actualizarLista(nuevaLista: List<Empleado>) {
        empleados = nuevaLista
        empleadosFiltrados = nuevaLista // Asegurar que la lista filtrada también se actualiza
        notifyDataSetChanged()
    }
}