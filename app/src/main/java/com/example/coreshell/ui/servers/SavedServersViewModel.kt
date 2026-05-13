package com.example.coreshell.ui.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.data.repository.ServerRepository
import kotlinx.coroutines.launch

class SavedServersViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ServerRepository(application)

    val servers = repo.getAllServers().asLiveData()

    fun saveServer(server: ServerEntity) {
        viewModelScope.launch { repo.saveServer(server) }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch { repo.deleteServer(server) }
    }
}
