package com.example.mobile.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private class PresenterHolder(create: (CoroutineScope) -> Any) : ViewModel() {
    val presenter = create(viewModelScope)
}

@Suppress("UNCHECKED_CAST")
internal fun <P : Any> Fragment.retainedPresenter(create: (CoroutineScope) -> P): P {
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PresenterHolder(create) as T
    }
    return ViewModelProvider(this, factory)[PresenterHolder::class.java].presenter as P
}

internal fun <T> Fragment.renderState(state: StateFlow<T>, render: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state.collect { render(it) }
        }
    }
}

internal fun Fragment.whenResumed(view: View, action: () -> Unit) {
    val lifecycle = viewLifecycleOwner.lifecycle
    view.post {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !parentFragmentManager.isStateSaved) {
            action()
        }
    }
}