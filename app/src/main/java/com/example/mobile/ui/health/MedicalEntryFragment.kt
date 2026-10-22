package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

abstract class MedicalEntryFragment(
    @LayoutRes layout: Int,
    private val typeIndex: Int,
) : ScreenFragment(layout) {
    private var recordType: Spinner? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.returnWithoutSaving(R.id.save)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        recordType = requireView().findViewById<Spinner>(R.id.record_type).apply {
            setSelection(typeIndex, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == typeIndex || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
                    val screen = when (position) {
                        1 -> Screen.VACCINATION_ENTRY
                        2 -> Screen.VET_VISIT_ENTRY
                        else -> Screen.WEIGHT_ENTRY
                    }
                    navigator.replace(screen, petId)
                }
            }
        }
    }

    override fun onDestroyView() {
        recordType?.onItemSelectedListener = null
        recordType = null
        super.onDestroyView()
    }
}