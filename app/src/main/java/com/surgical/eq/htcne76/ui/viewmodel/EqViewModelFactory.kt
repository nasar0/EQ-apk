package com.surgical.eq.htcne76.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.surgical.eq.htcne76.data.EqDataStore

class EqViewModelFactory(
    private val eqDataStore: EqDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EqViewModel::class.java)) {
            return EqViewModel(eqDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
