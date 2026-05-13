package com.example.coreshell.ui.filemanager

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.coreshell.R
import com.example.coreshell.data.repository.SSHRepository
import com.example.coreshell.ssh.RemoteFile
import com.example.coreshell.ssh.SSHFileException
import kotlinx.coroutines.launch
import java.io.File

class FileManagerViewModel(application: Application, private val sshRepo: SSHRepository) : AndroidViewModel(application) {

    private val _currentPath = MutableLiveData<String>("/")
    val currentPath: LiveData<String> = _currentPath

    private val _files = MutableLiveData<List<RemoteFile>>(emptyList())
    val files: LiveData<List<RemoteFile>> = _files

    private val _fileManagerState = MutableLiveData<FileManagerState>(FileManagerState.Idle)
    val fileManagerState: LiveData<FileManagerState> = _fileManagerState

    private val _transferProgress = MutableLiveData<Int>(0)
    val transferProgress: LiveData<Int> = _transferProgress

    private val pathStack = ArrayDeque<String>()

    init {
        loadDirectory("~")
    }

    fun loadDirectory(path: String) {
        _fileManagerState.value = FileManagerState.Loading
        viewModelScope.launch {
            try {
                val fileList = sshRepo.listDirectory(path)
                _files.value = fileList
                pathStack.addLast(path)
                _currentPath.value = path
                _fileManagerState.value = FileManagerState.Idle
            } catch (e: SSHFileException) {
                _fileManagerState.value = FileManagerState.Error(e.message ?: getApplication<Application>().getString(R.string.error_simple))
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.error_generic, e.message))
            }
        }
    }

    fun navigateTo(remoteFile: RemoteFile) {
        if (remoteFile.isDirectory) {
            loadDirectory(remoteFile.path)
        }
    }

    fun navigateBack(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeLast()
        val previousPath = pathStack.removeLast()
        loadDirectory(previousPath)
        return true
    }

    fun refresh() {
        loadDirectory(_currentPath.value ?: "/")
    }

    fun downloadFile(remoteFile: RemoteFile, destFile: File) {
        _fileManagerState.value = FileManagerState.Transferring(0)
        viewModelScope.launch {
            try {
                val outputStream = destFile.outputStream()
                sshRepo.downloadFile(
                    remotePath = remoteFile.path,
                    localOutputStream = outputStream
                ) { downloaded, total ->
                    val pct = if (total > 0) ((downloaded.toFloat() / total) * 100).toInt() else 0
                    _transferProgress.postValue(pct)
                    _fileManagerState.postValue(FileManagerState.Transferring(pct))
                }
                outputStream.close()
                _fileManagerState.value = FileManagerState.TransferComplete(
                    getApplication<Application>().getString(R.string.msg_download_complete, destFile.name)
                )
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.msg_download_failed, e.message))
            }
        }
    }

    fun uploadFile(context: Context, uri: Uri, remotePath: String) {
        _fileManagerState.value = FileManagerState.Transferring(0)
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception(getApplication<Application>().getString(R.string.msg_error_local_file))
                val fileSize = context.contentResolver.openFileDescriptor(uri, "r")
                    ?.statSize ?: -1L

                sshRepo.uploadFile(
                    localInputStream = inputStream,
                    remotePath = remotePath,
                    fileSize = fileSize
                ) { uploaded, total ->
                    val pct = if (total > 0) ((uploaded.toFloat() / total) * 100).toInt() else 0
                    _transferProgress.postValue(pct)
                    _fileManagerState.postValue(FileManagerState.Transferring(pct))
                }
                inputStream.close()
                _fileManagerState.value = FileManagerState.TransferComplete(getApplication<Application>().getString(R.string.msg_upload_complete))
                refresh()
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.msg_upload_failed, e.message))
            }
        }
    }

    fun createDirectory(name: String) {
        val currentDir = _currentPath.value ?: return
        val newPath = "$currentDir/$name".replace("//", "/")
        viewModelScope.launch {
            try {
                sshRepo.createDirectory(newPath)
                refresh()
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.error_generic, e.message))
            }
        }
    }

    fun deleteFile(remoteFile: RemoteFile) {
        viewModelScope.launch {
            try {
                sshRepo.deleteFile(remoteFile.path)
                refresh()
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.msg_delete_failed, e.message))
            }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        val newPath = oldPath.substringBeforeLast("/") + "/$newName"
        viewModelScope.launch {
            try {
                sshRepo.rename(oldPath, newPath)
                refresh()
            } catch (e: Exception) {
                _fileManagerState.value = FileManagerState.Error(getApplication<Application>().getString(R.string.msg_rename_failed, e.message))
            }
        }
    }
}

sealed class FileManagerState {
    object Idle : FileManagerState()
    object Loading : FileManagerState()
    data class Transferring(val progress: Int) : FileManagerState()
    data class TransferComplete(val message: String) : FileManagerState()
    data class Error(val message: String) : FileManagerState()
}
