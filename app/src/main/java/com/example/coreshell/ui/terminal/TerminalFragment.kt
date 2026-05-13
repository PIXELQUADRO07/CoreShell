package com.example.coreshell.ui.terminal

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.coreshell.databinding.FragmentTerminalBinding
import com.example.coreshell.ui.connection.ConnectionViewModel
import com.example.coreshell.ui.terminal.TerminalViewModelFactory
import com.example.coreshell.utils.AnsiColorParser

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels()
    private val terminalViewModel: TerminalViewModel by viewModels {
        TerminalViewModelFactory(requireActivity().application, connectionViewModel.sshRepo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand()
                true
            } else false
        }

        binding.btnSendCommand.setOnClickListener { sendCommand() }

        binding.fabClear.setOnClickListener { terminalViewModel.clearTerminal() }

        terminalViewModel.terminalOutput.observe(viewLifecycleOwner) {
            binding.scrollTerminal.post {
                binding.scrollTerminal.fullScroll(View.FOCUS_DOWN)
            }
        }

        terminalViewModel.promptText.observe(viewLifecycleOwner) {
            binding.tvPrompt.text = it
        }

        terminalViewModel.currentHistoryCommand.observe(viewLifecycleOwner) {
            binding.etCommand.setText(it)
            binding.etCommand.setSelection(it.length)
        }

        connectionViewModel.connectedServer.observe(viewLifecycleOwner) { server ->
            if (server != null) {
                terminalViewModel.openPersistentShell()
            } else {
                terminalViewModel.closeShellSession()
            }
            renderSavedCommands(server?.savedCommands ?: emptyList())
        }
    }

    private fun renderSavedCommands(commands: List<String>) {
        binding.tvSavedCommandsLabel.isVisible = commands.isNotEmpty()
        binding.scrollQuickCommands.isVisible = commands.isNotEmpty()
        binding.quickCommandsContainer.removeAllViews()

        commands.forEach { command ->
            val chip = Button(requireContext()).apply {
                text = command
                setAllCaps(false)
                setOnClickListener { terminalViewModel.sendCommand(command) }
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 12
                layoutParams = params
            }
            binding.quickCommandsContainer.addView(chip)
        }
    }

    private fun observeViewModel() {
        terminalViewModel.terminalOutput.observe(viewLifecycleOwner) { output ->
            binding.tvTerminalOutput.text = AnsiColorParser.parseToSpannable(output)
        }

        terminalViewModel.commandState.observe(viewLifecycleOwner) { state ->
            binding.btnSendCommand.isEnabled = state !is CommandState.Running
        }
    }

    private fun sendCommand() {
        val command = binding.etCommand.text.toString()
        terminalViewModel.sendCommand(command)
        binding.etCommand.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
