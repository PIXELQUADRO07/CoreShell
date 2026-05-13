package com.example.coreshell.ui.filemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coreshell.R
import com.example.coreshell.databinding.ItemRemoteFileBinding
import com.example.coreshell.ssh.RemoteFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemoteFileAdapter(
    private val onFileClick: (RemoteFile) -> Unit,
    private val onOptionsClick: (RemoteFile) -> Unit
) : ListAdapter<RemoteFile, RemoteFileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemRemoteFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(private val binding: ItemRemoteFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(file: RemoteFile) {
            binding.tvFileName.text = file.name
            binding.tvFileInfo.text = buildString {
                if (file.isDirectory) {
                    append("Cartella")
                } else {
                    append(file.sizeFormatted)
                    append("  ·  ")
                    append(dateFormat.format(Date(file.lastModified)))
                }
                if (file.permissions.isNotBlank()) {
                    append("  ·  ${file.permissions}")
                }
            }

            val iconRes = when {
                file.isDirectory -> R.drawable.ic_folder
                file.name.endsWith(".txt") || file.name.endsWith(".log") -> R.drawable.ic_file_text
                file.name.endsWith(".jpg") || file.name.endsWith(".png") ||
                        file.name.endsWith(".jpeg") -> R.drawable.ic_file_image
                file.name.endsWith(".zip") || file.name.endsWith(".tar") ||
                        file.name.endsWith(".gz") -> R.drawable.ic_file_archive
                file.name.endsWith(".sh") -> R.drawable.ic_file_code
                else -> R.drawable.ic_file
            }
            binding.ivFileIcon.setImageResource(iconRes)

            binding.cardFile.setOnClickListener { onFileClick(file) }
            binding.btnFileOptions.setOnClickListener { onOptionsClick(file) }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<RemoteFile>() {
        override fun areItemsTheSame(old: RemoteFile, new: RemoteFile) = old.path == new.path
        override fun areContentsTheSame(old: RemoteFile, new: RemoteFile) = old == new
    }
}
