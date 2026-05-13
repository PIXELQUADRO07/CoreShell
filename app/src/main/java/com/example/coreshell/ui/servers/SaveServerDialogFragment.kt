package com.example.coreshell.ui.servers

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.coreshell.R
import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.databinding.DialogSaveServerBinding
import java.io.File
import java.io.FileOutputStream

class SaveServerDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSaveServerBinding? = null
    private val binding get() = _binding!!

    private val savedServersViewModel: SavedServersViewModel by viewModels()

    private val keyPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { copyKeyFromUri(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSaveServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rgAuthType.setOnCheckedChangeListener { _, checkedId ->
            val keyAuth = checkedId == R.id.rbAuthKey
            binding.tilPrivateKeyPath.isVisible = keyAuth
            binding.btnBrowseKey.isVisible = keyAuth
            binding.tilKeyPassphrase.isVisible = keyAuth
            binding.tilPassword.isVisible = !keyAuth
            if (!keyAuth) {
                binding.tilPrivateKeyPath.error = null
            }
        }

        binding.btnBrowseKey.setOnClickListener {
            keyPicker.launch(arrayOf("*/*"))
        }

        binding.btnSave.setOnClickListener {
            val nickname = binding.etNickname.text.toString().trim()
            val host = binding.etHost.text.toString().trim()
            val port = binding.etPort.text.toString().toIntOrNull() ?: 22
            val username = binding.etUsername.text.toString().trim()
            val isKeyAuth = binding.rbAuthKey.isChecked
            val password = if (isKeyAuth) binding.etKeyPassphrase.text.toString() else binding.etPassword.text.toString()
            val privateKeyPath = binding.etPrivateKeyPath.text.toString().trim().takeIf { isKeyAuth }
            val savedCommands = binding.etSavedCommands.text.toString()
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (host.isBlank() || username.isBlank()) return@setOnClickListener
            if (isKeyAuth && privateKeyPath.isNullOrBlank()) {
                binding.tilPrivateKeyPath.error = getString(R.string.error_empty_private_key)
                return@setOnClickListener
            }

            val server = ServerEntity(
                nickname = nickname.ifBlank { host },
                host = host,
                port = port,
                username = username,
                password = password,
                authType = if (isKeyAuth) "key" else "password",
                privateKeyPath = privateKeyPath,
                savedCommands = savedCommands
            )
            savedServersViewModel.saveServer(server)
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun copyKeyFromUri(uri: Uri) {
        val fileName = queryFileName(uri) ?: "id_rsa"
        val targetFile = File(requireContext().cacheDir, "imported_ssh_key_${System.currentTimeMillis()}_$fileName")

        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            binding.etPrivateKeyPath.setText(targetFile.absolutePath)
        } catch (e: Exception) {
            binding.tilPrivateKeyPath.error = getString(R.string.error_invalid_private_key)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndexOpenableColumnDisplayNameOrName()
                if (index != -1) return it.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun Cursor.getColumnIndexOpenableColumnDisplayNameOrName(): Int {
        return when {
            getColumnIndex("displayName") != -1 -> getColumnIndex("displayName")
            getColumnIndex("_display_name") != -1 -> getColumnIndex("_display_name")
            getColumnIndex("name") != -1 -> getColumnIndex("name")
            else -> -1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
