package com.example.coreshell.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.coreshell.R
import com.example.coreshell.databinding.FragmentConnectionBinding

class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionViewModel by activityViewModels()
    private var hostKeyDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            if (validateInputs()) {
                viewModel.connect(
                    host = binding.etHost.text.toString().trim(),
                    port = binding.etPort.text.toString().toIntOrNull() ?: 22,
                    username = binding.etUsername.text.toString().trim(),
                    password = binding.etPassword.text.toString()
                )
            }
        }

        binding.btnSaveServer.setOnClickListener {
            if (validateInputs()) {
                findNavController().navigate(R.id.action_connection_to_saveServerDialog)
            }
        }

        binding.btnSavedServers.setOnClickListener {
            findNavController().navigate(R.id.action_connection_to_savedServers)
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ConnectionState.Idle -> {
                    binding.progressConnect.isVisible = false
                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = getString(R.string.btn_connect)
                    binding.tvError.isVisible = false
                }
                is ConnectionState.Connecting -> {
                    binding.progressConnect.isVisible = true
                    binding.btnConnect.isEnabled = false
                    binding.btnConnect.text = getString(R.string.connecting)
                    binding.tvError.isVisible = false
                }
                is ConnectionState.Connected -> {
                    binding.progressConnect.isVisible = false
                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = getString(R.string.btn_connect)
                    findNavController().navigate(R.id.action_connection_to_main)
                }
                is ConnectionState.Error -> {
                    binding.progressConnect.isVisible = false
                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = getString(R.string.btn_connect)
                    binding.tvError.text = state.message
                    binding.tvError.isVisible = true
                }
            }
        }

        viewModel.hostKeyPrompt.observe(viewLifecycleOwner) { prompt ->
            if (prompt == null) {
                hostKeyDialog?.dismiss()
                hostKeyDialog = null
                return@observe
            }

            if (hostKeyDialog?.isShowing == true) return@observe

            val fingerprint = extractHostKeyFingerprint(prompt.message)
            val message = fingerprint?.let {
                getString(R.string.host_key_prompt_message, it)
            } ?: prompt.message

            hostKeyDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.host_key_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.host_key_prompt_accept) { _, _ ->
                    viewModel.respondToHostKeyPrompt(true)
                }
                .setNegativeButton(R.string.host_key_prompt_reject) { _, _ ->
                    viewModel.respondToHostKeyPrompt(false)
                }
                .setOnCancelListener {
                    viewModel.respondToHostKeyPrompt(false)
                }
                .show()
        }
    }

    private fun validateInputs(): Boolean {
        var valid = true
        with(binding) {
            if (etHost.text.isNullOrBlank()) {
                tilHost.error = getString(R.string.error_empty_host)
                valid = false
            } else tilHost.error = null

            val port = etPort.text.toString().toIntOrNull()
            if (port == null || port !in 1..65535) {
                tilPort.error = getString(R.string.error_invalid_port)
                valid = false
            } else tilPort.error = null

            if (etUsername.text.isNullOrBlank()) {
                tilUsername.error = getString(R.string.error_empty_username)
                valid = false
            } else tilUsername.error = null

            if (etPassword.text.isNullOrBlank()) {
                tilPassword.error = getString(R.string.error_empty_password)
                valid = false
            } else tilPassword.error = null
        }
        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hostKeyDialog?.dismiss()
        _binding = null
    }

    private fun extractHostKeyFingerprint(message: String): String? {
        val regex = Regex("SHA256:[A-Za-z0-9+/=]+")
        return regex.find(message)?.value
    }
}
