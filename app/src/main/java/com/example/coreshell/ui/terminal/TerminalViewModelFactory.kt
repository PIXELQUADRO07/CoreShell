package com.example.coreshell.ui.terminal

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.coreshell.data.repository.SSHRepository

class TerminalViewModelFactory(
    private val application: Application,
    private val sshRepo: SSHRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(TerminalViewModel::class.java)) {
            TerminalViewModel(application, sshRepo) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }
}
