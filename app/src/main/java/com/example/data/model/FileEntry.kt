package com.example.data.model

data class FileEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val modifiedTime: String,
    var content: String = ""
)
