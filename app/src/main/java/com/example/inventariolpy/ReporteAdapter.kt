package com.example.inventariolpy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class ReporteAdapter(private val herramientas: List<HerramientaReporte>) :
    RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val herramienta = herramientas[position]
        holder.bind(herramienta)
    }

    override fun getItemCount(): Int = herramientas.size

    inner class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvMarca: TextView = itemView.findViewById(R.id.tvMarca)
        private val tvModelo: TextView = itemView.findViewById(R.id.tvModelo)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvEmpleado: TextView = itemView.findViewById(R.id.tvEmpleado)
        private val tvFechaPrestamo: TextView = itemView.findViewById(R.id.tvFechaPrestamo)
        private val tvActivo: TextView = itemView.findViewById(R.id.tvActivo)

        fun bind(herramienta: HerramientaReporte) {
            tvTitulo.text = "ID: ${herramienta.id}"
            tvCodigo.text = "Código Interno: ${herramienta.codigoInterno}"
            tvNombre.text = "Nombre: ${herramienta.nombre}"
            tvEstado.text = "Estado: ${herramienta.estado}"
            tvMarca.text = "Marca: ${herramienta.marca}"
            tvModelo.text = "Modelo: ${herramienta.modelo}"
            tvPrecio.text = "Precio: ${herramienta.precio}"
            tvEmpleado.text = "Empleado: ${herramienta.nombreEmpleado}"
            tvFechaPrestamo.text = "Fecha Préstamo: ${herramienta.fechaPrestamo}"
            tvActivo.text = "Estado: ${if (herramienta.activo == 1) "Activo" else "Inactivo"}"

            // Cambiar color del estado dependiendo de si está activo o no
            tvActivo.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (herramienta.activo == 1) android.R.color.holo_green_light else android.R.color.holo_red_light
                )
            )
        }
    }
}