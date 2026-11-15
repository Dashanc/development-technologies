package com.example.mobile.data.transfer

import com.example.mobile.data.local.LocalDataTransferStorage
import com.example.mobile.data.local.PassportStorage
import com.example.mobile.data.local.model.LocalDataTransfer
import com.example.mobile.data.local.model.TransferRequest
import com.example.mobile.data.local.toDomain
import com.example.mobile.data.local.toLocal
import com.example.mobile.data.remote.RemoteFailure
import com.example.mobile.data.remote.RemoteSession
import com.example.mobile.data.remote.RemoteTransferSource
import com.example.mobile.data.remote.remoteResult
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.model.AccountSession
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.LocalDataTransferPreview
import com.example.mobile.domain.repository.LocalDataTransferProgress
import com.example.mobile.domain.repository.LocalDataTransferRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LocalDataTransferRepositoryImpl(
    private val passport: PassportStorage,
    private val storage: LocalDataTransferStorage,
    private val remote: RemoteTransferSource,
    private val sessions: StateFlow<RemoteSession?>,
    private val accounts: StateFlow<AccountSession>,
    private val server: String,
    private val online: () -> Boolean,
    private val refresh: () -> Unit,
    private val linkPhotos: suspend (RemoteSession, String, Map<String, String>) -> RepositoryResult<Unit>,
) : LocalDataTransferRepository {
    private val mutex = Mutex()

    override suspend fun preview() = mutex.withLock {
        remoteResult {
            val session = session()
            val source = value(passport.observe { it }.first())
            val owner = source.owner ?: return@remoteResult null
            val existing = value(storage.read()).transfers.firstOrNull {
                it.server == server && it.ownerId == session.ownerId && it.snapshot.owner?.id == owner.id
            }
            val transfer = if (existing?.approved == true && !existing.reviewRequired) existing else {
                requireOnline()
                val target = value(remote.owner(session))
                val snapshot = source.copy(pets = source.pets.map { it.copy(photoUri = null) })
                val updated = if (existing?.approved == true) {
                    val pets = existing.snapshot.pets.filter { it.id in existing.petIds } + snapshot.pets.filter { it.id !in existing.petIds }
                    val petIds = pets.map { it.id }.toSet()
                    existing.copy(reviewId = UUID.randomUUID().toString(),
                        serverOwner = target.toLocal(), ownerRevision = target.revision ?: throw RemoteFailure(RepositoryError.INVALID_RESPONSE),
                        snapshot = snapshot.copy(owner = if (existing.ownerTransferred) existing.snapshot.owner else snapshot.owner,
                            pets = pets, records = existing.snapshot.records.filter { it.id in existing.recordIds } +
                                snapshot.records.filter { it.id !in existing.recordIds && it.petId in petIds }))
                } else LocalDataTransfer(
                    id = UUID.randomUUID().toString(), server = server, ownerId = session.ownerId,
                    snapshot = snapshot,
                    serverOwner = target.toLocal(), ownerRevision = target.revision ?: throw RemoteFailure(RepositoryError.INVALID_RESPONSE),
                )
                updated.also { save(session, it) }
            }
            check(session)
            transfer.pending?.let { checkRetryTime(it) }
            if (transfer.completed) {
                value(linkPhotos(session, owner.id, transfer.petIds))
                refresh()
            }
            transfer.preview()
        }
    }

    override suspend fun transfer(previewId: String, includeOwner: Boolean, progress: (LocalDataTransferProgress) -> Unit) = mutex.withLock {
        try {
            remoteResult {
                val session = session()
                requireOnline()
                var transfer = value(storage.read()).transfers.firstOrNull {
                    it.reviewId == previewId && it.server == server && it.ownerId == session.ownerId
                } ?: throw RemoteFailure(RepositoryError.INVALID_REFERENCE)
                if (transfer.completed) return@remoteResult Unit
                if (transfer.approved && transfer.includeOwner != includeOwner) throw RemoteFailure(RepositoryError.CONFLICT)
                if (transfer.reviewRequired) {
                    transfer = transfer.copy(reviewRequired = false)
                    save(session, transfer)
                }
                if (!transfer.approved) {
                    transfer = transfer.copy(approved = true, includeOwner = includeOwner)
                    save(session, transfer)
                }
                progress(transfer.progress())
                if (transfer.includeOwner && !transfer.ownerTransferred) {
                    val local = requireNotNull(transfer.snapshot.owner).toDomain()
                    progress(transfer.progress(local.name))
                    val current = value(remote.owner(session))
                    if (!current.matches(local)) {
                        if (current.revision != transfer.ownerRevision) {
                            save(session, transfer.copy(reviewRequired = true, reviewId = UUID.randomUUID().toString()))
                            throw RemoteFailure(RepositoryError.VERSION_CONFLICT)
                        }
                        confirmed(session, transfer, remote.saveOwner(session, local.copy(id = session.ownerId, revision = transfer.ownerRevision)))
                    }
                    transfer = transfer.copy(ownerTransferred = true)
                    save(session, transfer)
                }
                for (pet in transfer.snapshot.pets) {
                    if (pet.id in transfer.petIds) continue
                    progress(transfer.progress(pet.name))
                    transfer = prepareRequest(session, transfer, "pet:${pet.id}")
                    val saved = confirmed(session, transfer, remote.createPet(session, pet.toDomain().copy(ownerId = session.ownerId),
                        "transfer:${transfer.id}:pet:${pet.id}"))
                    transfer = transfer.copy(petIds = transfer.petIds + (pet.id to saved.id), pending = null)
                    save(session, transfer)
                    progress(transfer.progress())
                }
                for (record in transfer.snapshot.records) {
                    if (record.id in transfer.recordIds) continue
                    val pet = transfer.snapshot.pets.first { it.id == record.petId }
                    progress(transfer.progress("${pet.name} · ${record.date}"))
                    val petId = transfer.petIds[record.petId] ?: throw RemoteFailure(RepositoryError.INVALID_REFERENCE)
                    val mapped = when (val model = record.toDomain()) {
                        is WeightRecord -> model.copy(petId = petId)
                        is VaccinationRecord -> model.copy(petId = petId)
                        is VetVisitRecord -> model.copy(petId = petId)
                    }
                    transfer = prepareRequest(session, transfer, "record:${record.id}")
                    val saved = confirmed(session, transfer, remote.createRecord(session, mapped, "transfer:${transfer.id}:record:${record.id}"))
                    transfer = transfer.copy(recordIds = transfer.recordIds + (record.id to saved.id), pending = null)
                    save(session, transfer)
                    progress(transfer.progress())
                }
                check(session)
                value(linkPhotos(session, requireNotNull(transfer.snapshot.owner).id, transfer.petIds))
                transfer = transfer.copy(completed = true)
                save(session, transfer)
                progress(transfer.progress())
            }
        } finally {
            refresh()
        }
    }

    private suspend fun prepareRequest(session: RemoteSession, transfer: LocalDataTransfer, item: String): LocalDataTransfer {
        check(session)
        requireOnline()
        val pending = transfer.pending
        if (pending != null) {
            if (pending.itemId != item) throw RemoteFailure(RepositoryError.CORRUPT_DATA)
            checkRetryTime(pending)
            return transfer
        }
        return transfer.copy(pending = TransferRequest(item, Instant.now().toEpochMilli())).also { save(session, it) }
    }

    private fun checkRetryTime(pending: TransferRequest) {
        val now = Instant.now().toEpochMilli()
        if (now < pending.startedAt || now - pending.startedAt >= 23L * 60 * 60 * 1000) {
            throw RemoteFailure(RepositoryError.TRANSFER_RETRY_EXPIRED)
        }
    }

    private fun session(): RemoteSession = sessions.value?.takeIf { accounts.value.mode == AccountMode.SERVER }
        ?: throw RemoteFailure(RepositoryError.AUTHENTICATION_REQUIRED)

    private fun check(session: RemoteSession) {
        if (sessions.value !== session || accounts.value.mode != AccountMode.SERVER) throw RemoteFailure(RepositoryError.AUTHENTICATION_REQUIRED)
    }

    private fun requireOnline() {
        if (!online()) throw RemoteFailure(RepositoryError.OFFLINE_WRITE_NOT_ALLOWED)
    }

    private suspend fun save(session: RemoteSession, transfer: LocalDataTransfer) {
        value(storage.save(transfer) { sessions.value === session && accounts.value.mode == AccountMode.SERVER })
    }

    private suspend fun <T> confirmed(session: RemoteSession, transfer: LocalDataTransfer, result: RepositoryResult<T>): T {
        if (result is RepositoryResult.Failure && result.error in setOf(
                RepositoryError.INVALID_DATA, RepositoryError.IDENTITY_CONFLICT, RepositoryError.VERSION_CONFLICT)) {
            save(session, transfer.copy(pending = null, reviewRequired = true, reviewId = UUID.randomUUID().toString()))
        }
        return value(result)
    }

    private fun LocalDataTransfer.preview() = LocalDataTransferPreview(
        reviewId, requireNotNull(snapshot.owner).toDomain(), serverOwner.toDomain(), snapshot.pets.size, snapshot.records.size,
        approved, includeOwner, completed, progress(),
    )

    private fun LocalDataTransfer.progress(item: String? = null) = LocalDataTransferProgress(
        petIds.size + recordIds.size + if (includeOwner && ownerTransferred) 1 else 0,
        snapshot.pets.size + snapshot.records.size + if (includeOwner) 1 else 0, item,
    )

    private fun Owner.matches(other: Owner) = name == other.name.trim() && email == other.email.trim().lowercase(java.util.Locale.ROOT) && phone == other.phone.trim()

    private fun <T> value(result: RepositoryResult<T>): T = when (result) {
        is RepositoryResult.Success -> result.value
        is RepositoryResult.Failure -> throw RemoteFailure(result.error, result.fieldErrors)
    }
}