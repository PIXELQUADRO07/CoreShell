package com.example.coreshell.ui.servers

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreshell.databinding.FragmentSavedServersBinding
import com.example.coreshell.ui.connection.ConnectionViewModel

class SavedServersFragment : Fragment() {

    private var _binding: FragmentSavedServersBinding? = null
    private val binding get() = _binding!!

    private val savedServersViewModel: SavedServersViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedServersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SavedServerAdapter(
            onConnect = { server ->
                connectionViewModel.connect(server)
                findNavController().popBackStack()
            },
            onDelete = { server -> savedServersViewModel.deleteServer(server) }
        )

        binding.rvServers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        savedServersViewModel.servers.observe(viewLifecycleOwner) { servers ->
            adapter.submitList(servers)
            binding.tvEmpty.visibility = if (servers.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
