package com.example.coreshell.ui.servers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coreshell.R
import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.databinding.ItemServerBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedServerAdapter(
    private val onConnect: (ServerEntity) -> Unit,
    private val onDelete: (ServerEntity) -> Unit
) : ListAdapter<ServerEntity, SavedServerAdapter.ServerViewHolder>(ServerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ItemServerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ServerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ServerViewHolder(private val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(server: ServerEntity) {
            val context = binding.root.context
            binding.tvNickname.text = server.nickname
            binding.tvAddress.text = "${server.username}@${server.host}:${server.port}"
            binding.tvLastConnected.text = server.lastConnected?.let {
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                context.getString(R.string.msg_last_connection, dateStr)
            } ?: context.getString(R.string.msg_never_connected)

            binding.btnConnect.setOnClickListener { onConnect(server) }
            binding.btnDelete.setOnClickListener { onDelete(server) }
            binding.root.setOnClickListener { onConnect(server) }
        }
    }

    class ServerDiffCallback : DiffUtil.ItemCallback<ServerEntity>() {
        override fun areItemsTheSame(old: ServerEntity, new: ServerEntity) = old.id == new.id
        override fun areContentsTheSame(old: ServerEntity, new: ServerEntity) = old == new
    }
}
