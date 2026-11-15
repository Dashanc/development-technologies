package com.example.mobile.domain.repository

import com.example.mobile.domain.model.Owner

data class LocalDataTransferPreview(
    val id: String,
    val localOwner: Owner,
    val serverOwner: Owner,
    val petCount: Int,
    val recordCount: Int,
    val started: Boolean,
    val includeOwner: Boolean,
    val completed: Boolean,
    val progress: LocalDataTransferProgress,
)

data class LocalDataTransferProgress(val transferred: Int = 0, val total: Int = 0, val item: String? = null)

interface LocalDataTransferRepository {
    suspend fun preview(): RepositoryResult<LocalDataTransferPreview?>
    suspend fun transfer(
        previewId: String,
        includeOwner: Boolean,
        progress: (LocalDataTransferProgress) -> Unit,
    ): RepositoryResult<Unit>
}