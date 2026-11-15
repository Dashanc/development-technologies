package com.example.mobile.data

import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.model.AccountSession
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountSource(private val session: StateFlow<AccountSession>) {
    val local get() = session.value.mode == AccountMode.LOCAL

    fun <T> observe(empty: T, local: () -> Flow<RepositoryResult<T>>, server: () -> Flow<RepositoryResult<T>>) =
        session.flatMapLatest { state -> when (state.mode) {
            AccountMode.LOCAL -> local()
            AccountMode.SERVER -> server()
            AccountMode.RESTORING -> emptyFlow()
            else -> flowOf(RepositoryResult.Success(empty))
        } }
}

internal class AccountOwnerRepository(private val source: AccountSource, private val local: OwnerRepository, private val server: OwnerRepository) : OwnerRepository {
    override fun observeOwner() = source.observe<Owner?>(null, local::observeOwner, server::observeOwner)
    override suspend fun save(owner: Owner) = (if (source.local) local else server).save(owner)
}

internal class AccountPetRepository(private val source: AccountSource, private val local: PetRepository, private val server: PetRepository) : PetRepository {
    override fun observePets(ownerId: String) = source.observe(emptyList(), { local.observePets(ownerId) }, { server.observePets(ownerId) })
    override fun observePet(petId: String) = source.observe<Pet?>(null, { local.observePet(petId) }, { server.observePet(petId) })
    override suspend fun save(pet: Pet) = (if (source.local) local else server).save(pet)
    override suspend fun update(pet: Pet) = (if (source.local) local else server).update(pet)
    override suspend fun delete(petId: String, revision: String?) = (if (source.local) local else server).delete(petId, revision)
}

internal class AccountMedicalRecordRepository(private val source: AccountSource, private val local: MedicalRecordRepository, private val server: MedicalRecordRepository) : MedicalRecordRepository {
    override fun observeRecords(petId: String) = source.observe(emptyList(), { local.observeRecords(petId) }, { server.observeRecords(petId) })
    override fun observeRecord(recordId: String) = source.observe<MedicalRecord?>(null, { local.observeRecord(recordId) }, { server.observeRecord(recordId) })
    override suspend fun save(record: MedicalRecord) = (if (source.local) local else server).save(record)
    override suspend fun delete(recordId: String, revision: String?) = (if (source.local) local else server).delete(recordId, revision)
}