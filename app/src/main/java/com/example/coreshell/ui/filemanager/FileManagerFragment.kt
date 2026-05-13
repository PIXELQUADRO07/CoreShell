package com.example.coreshell.ui.filemanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.example.coreshell.R
import com.example.coreshell.databinding.FragmentFileManagerBinding
import com.example.coreshell.ssh.RemoteFile
import com.example.coreshell.ui.connection.ConnectionViewModel
import java.io.File

class FileManagerFragment : Fragment() {

    private var _binding: FragmentFileManagerBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels()
    private lateinit var fileManagerViewModel: FileManagerViewModel
    private lateinit var adapter: RemoteFileAdapter

    private val uploadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val currentPath = fileManagerViewModel.currentPath.value ?: "/"
                val filename = uri.lastPathSegment?.substringAfterLast("/") ?: "upload"
                val remotePath = "$currentPath/$filename"
                fileManagerViewModel.uploadFile(requireContext(), uri, remotePath)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileManagerViewModel = FileManagerViewModel(requireActivity().application, connectionViewModel.sshRepo)
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RemoteFileAdapter(
            onFileClick = { file -> handleFileClick(file) },
            onOptionsClick = { file -> showFileOptions(file) }
        )
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FileManagerFragment.adapter
        }
        binding.swipeRefresh.setOnRefreshListener { fileManagerViewModel.refresh() }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            if (!fileManagerViewModel.navigateBack()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.fabUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            uploadLauncher.launch(intent)
        }

        binding.fabUpload.setOnLongClickListener {
            showCreateDirectoryDialog()
            true
        }
    }

    private fun observeViewModel() {
        fileManagerViewModel.files.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            binding.swipeRefresh.isRefreshing = false
        }

        fileManagerViewModel.currentPath.observe(viewLifecycleOwner) { path ->
            binding.toolbar.subtitle = path
        }

        fileManagerViewModel.fileManagerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FileManagerState.Idle -> {
                    binding.progressTransfer.isVisible = false
                }
                is FileManagerState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is FileManagerState.Transferring -> {
                    binding.progressTransfer.isVisible = true
                    binding.progressTransfer.progress = state.progress
                }
                is FileManagerState.TransferComplete -> {
                    binding.progressTransfer.isVisible = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is FileManagerState.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressTransfer.isVisible = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleFileClick(file: RemoteFile) {
        if (file.isDirectory) {
            fileManagerViewModel.navigateTo(file)
        } else {
            showFileOptions(file)
        }
    }

    private fun showFileOptions(file: RemoteFile) {
        val popup = PopupMenu(requireContext(), binding.root)
        popup.menuInflater.inflate(R.menu.menu_file_options, popup.menu)
        if (file.isDirectory) {
            popup.menu.findItem(R.id.action_download)?.isVisible = false
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_download -> {
                    downloadFile(file)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(file)
                    true
                }
                R.id.action_rename -> {
                    showRenameDialog(file)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun downloadFile(file: RemoteFile) {
        val destDir = requireContext().getExternalFilesDir(null)
            ?: requireContext().filesDir
        val destFile = File(destDir, file.name)
        fileManagerViewModel.downloadFile(file, destFile)
        Snackbar.make(binding.root, getString(R.string.msg_download_complete, file.name), Snackbar.LENGTH_SHORT).show()
    }

    private fun confirmDelete(file: RemoteFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_msg, file.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> fileManagerViewModel.deleteFile(file) }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showRenameDialog(file: RemoteFile) {
        val input = TextInputEditText(requireContext())
        input.setText(file.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_rename)
            .setView(input)
            .setPositiveButton(R.string.action_rename) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    fileManagerViewModel.renameFile(file.path, newName)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showCreateDirectoryDialog() {
        val input = TextInputEditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_new_folder)
            .setView(input)
            .setPositiveButton(R.string.btn_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) fileManagerViewModel.createDirectory(name)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
