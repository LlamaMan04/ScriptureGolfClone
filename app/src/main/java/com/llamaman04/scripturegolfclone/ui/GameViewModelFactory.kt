package com.llamaman04.scripturegolfclone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.llamaman04.scripturegolfclone.data.ScriptureRepository

class GameViewModelFactory(
    private val repository: ScriptureRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(GameViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GameViewModel(repository) as T
    }
}
