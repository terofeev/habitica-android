package com.habitrpg.android.habitica.ui.helpers

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.extensions.consumeWindowInsetsAbove30
import com.habitrpg.common.habitica.helpers.EmptyItem
import com.habitrpg.common.habitica.helpers.RecyclerViewState
import com.habitrpg.common.habitica.helpers.RecyclerViewStateAdapter

class RecyclerViewEmptySupport
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    var onRefresh: (() -> Unit)?
        get() = emptyAdapter.onRefresh
        set(value) {
            emptyAdapter.onRefresh = value
        }

    var state: RecyclerViewState = RecyclerViewState.LOADING
        set(value) {
            field = value
            when (field) {
                RecyclerViewState.DISPLAYING_DATA -> updateAdapter(actualAdapter)
                else -> {
                    updateAdapter(emptyAdapter)
                    emptyAdapter.state = value
                }
            }
        }

    private fun updateAdapter(newAdapter: Adapter<*>?) {
        if (adapter != newAdapter) {
            super.setAdapter(newAdapter)
        }
    }

    var emptyItem: EmptyItem?
        get() = emptyAdapter.emptyItem
        set(value) {
            emptyAdapter.emptyItem = value
        }

    private var actualAdapter: Adapter<*>? = null
    private val emptyAdapter = RecyclerViewStateAdapter()

    private var windowInsetTop = false
    private var windowInsetBottom = true
    private var windowInsetStart = true
    private var windowInsetEnd = true

    init {
        context.theme?.obtainStyledAttributes(attrs, R.styleable.RecyclerViewEmptySupport, 0, 0)?.let {
            windowInsetTop = it.getBoolean(R.styleable.RecyclerViewEmptySupport_windowInsetTop, false)
            windowInsetBottom = it.getBoolean(R.styleable.RecyclerViewEmptySupport_windowInsetBottom, true)
            windowInsetStart = it.getBoolean(R.styleable.RecyclerViewEmptySupport_windowInsetStart, true)
            windowInsetEnd = it.getBoolean(R.styleable.RecyclerViewEmptySupport_windowInsetEnd, true)
        }

        clipToPadding = false
        val topPadding = paddingTop
        val bottomPadding = paddingBottom
        val leftPadding = paddingLeft
        val rightPadding = paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = (if (windowInsetStart) bars.left else 0) + leftPadding,
                top = (if (windowInsetTop) bars.top else 0) + topPadding,
                right = (if (windowInsetEnd) bars.right else 0) + rightPadding,
                bottom = (if (windowInsetBottom) bars.bottom else 0) + bottomPadding,
            )
            consumeWindowInsetsAbove30(insets)
        }
    }

    private val observer =
        object : AdapterDataObserver() {
            override fun onChanged() {
                updateState()
            }

            override fun onItemRangeInserted(
                positionStart: Int,
                itemCount: Int
            ) {
                updateState()
            }

            override fun onItemRangeRemoved(
                positionStart: Int,
                itemCount: Int
            ) {
                updateState()
            }
        }

    internal fun updateState(isInitial: Boolean = false) {
        state =
            if (actualAdapter != null && !isInitial) {
                val emptyViewVisible = actualAdapter?.itemCount == 0
                if (emptyViewVisible) {
                    RecyclerViewState.EMPTY
                } else {
                    RecyclerViewState.DISPLAYING_DATA
                }
            } else {
                RecyclerViewState.LOADING
            }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        val oldAdapter = actualAdapter
        oldAdapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        actualAdapter = adapter
        updateState(true)
    }
}
