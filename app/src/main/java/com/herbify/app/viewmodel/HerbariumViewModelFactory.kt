package com.herbify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.herbify.app.data.HerbariumRepository

class HerbariumViewModelFactory(
    private val repository: HerbariumRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HerbariumViewModel(repository) as T
    }
}