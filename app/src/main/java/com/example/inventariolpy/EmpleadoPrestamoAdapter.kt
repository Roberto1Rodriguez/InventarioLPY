package com.example.inventariolpy

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmpleadoPrestamoAdapter(
    private val context: Context,
    private var empleados: List<Empleado>,
    private val onEmpleadoSeleccionado: (Empleado) -> Unit
) : RecyclerView.Adapter<EmpleadoPrestamoAdapter.EmpleadoViewHolder>() {

    private var seleccionado: Int = -1 // ID del empleado seleccionado

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_empleado_prestamo, parent, false)
        return EmpleadoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
        val empleado = empleados[position]
        holder.bind(empleado, empleado.id == seleccionado)
    }

    override fun getItemCount(): Int = empleados.size

    inner class EmpleadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgFotoEmpleado: ImageView = itemView.findViewById(R.id.imgFotoEmpleado)
        private val tvNombreEmpleado: TextView = itemView.findViewById(R.id.tvNombreEmpleado)
        private val radioSeleccionarEmpleado: RadioButton = itemView.findViewById(R.id.radioSeleccionarEmpleado)

        fun bind(empleado: Empleado, isSelected: Boolean) {
            tvNombreEmpleado.text = empleado.nombre
            radioSeleccionarEmpleado.isChecked = isSelected

            // Cargar imagen si tiene foto, de lo contrario, poner un placeholder
            if (empleado.foto != null) {
                val fotoBitmap = BitmapFactory.decodeByteArray(empleado.foto, 0, empleado.foto.size)
                imgFotoEmpleado.setImageBitmap(fotoBitmap)
            } else {
                imgFotoEmpleado.setImageResource(R.drawable.ic_placeholder)
            }

            // Manejar selección del empleado
            itemView.setOnClickListener {
                seleccionado = empleado.id
                notifyDataSetChanged()
                onEmpleadoSeleccionado(empleado) // Enviar empleado seleccionado a la actividad
            }

            radioSeleccionarEmpleado.setOnClickListener {
                seleccionado = empleado.id
                notifyDataSetChanged()
                onEmpleadoSeleccionado(empleado)
            }
        }
    }

    fun actualizarLista(nuevaLista: List<Empleado>) {
        empleados = nuevaLista
        notifyDataSetChanged()
    }
}