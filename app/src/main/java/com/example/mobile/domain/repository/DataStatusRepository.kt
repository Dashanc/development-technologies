package com.example.mobile.domain.repository

import com.example.mobile.domain.model.DataStatus
import kotlinx.coroutines.flow.StateFlow

interface DataStatusRepository {
    val state: StateFlow<DataStatus>
    fun refresh()
}