/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
package im.vector.app.core.platform

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.MavericksView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.EntryPointAccessors
import im.vector.app.core.di.ActivityEntryPoint
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import io.github.hyuwah.draggableviewlib.Utils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber

/**
 * Add Mavericks capabilities, handle DI and bindings.
 */
abstract class VectorBaseBottomSheetDialogFragment<VB : ViewBinding> : BottomSheetDialogFragment(), MavericksView {
    /* ==========================================================================================
     * Analytics
     * ========================================================================================== */

    protected var analyticsScreenName: MobileScreen.ScreenName? = null

    protected lateinit var analyticsTracker: AnalyticsTracker

    /* ==========================================================================================
     * View
     * ========================================================================================== */

    private var _binding: VB? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val views: VB
        get() = _binding!!

    abstract fun getBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val activityViewModelProvider
        get() = ViewModelProvider(requireActivity(), viewModelFactory)

    protected val fragmentViewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    /* ==========================================================================================
     * BottomSheetBehavior
     * ========================================================================================== */

    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    val vectorBaseActivity: VectorBaseActivity<*> by lazy {
        activity as VectorBaseActivity<*>
    }

    open val showExpanded = false

    interface ResultListener {
        fun onBottomSheetResult(resultCode: Int, data: Any?)

        companion object {
            const val RESULT_OK = 1
            const val RESULT_CANCEL = 0
        }
    }

    var resultListener: ResultListener? = null
    var bottomSheetResult: Int = ResultListener.RESULT_CANCEL
    var bottomSheetResultData: Any? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        resultListener?.onBottomSheetResult(bottomSheetResult, bottomSheetResultData)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = getBinding(inflater, container)
        return views.root
    }

    @CallSuper
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        val activityEntryPoint = EntryPointAccessors.fromActivity(vectorBaseActivity, ActivityEntryPoint::class.java)
        viewModelFactory = activityEntryPoint.viewModelFactory()
        val singletonEntryPoint = context.singletonEntryPoint()
        analyticsTracker = singletonEntryPoint.analyticsTracker()
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume BottomSheet ${javaClass.simpleName}")
        analyticsScreenName?.let {
            analyticsTracker.screen(MobileScreen(screenName = it))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            val dialog = this as? BottomSheetDialog
            bottomSheetBehavior = dialog?.behavior
            bottomSheetBehavior?.setPeekHeight(DimensionConverter(resources).dpToPx(400), false)
            if (showExpanded) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // This ensures that invalidate() is called for static screens that don't
        // subscribe to a ViewModel.
        postInvalidate()
    }

    @CallSuper
    override fun invalidate() {
        forceExpandState()
    }

    protected fun setPeekHeightAsScreenPercentage(@FloatRange(from = 0.0, to = 1.0) percentage: Float) {
        context?.let {
            val screenHeight = Utils.getScreenHeight(it)
            bottomSheetBehavior?.setPeekHeight((screenHeight * percentage).toInt(), true)
        }
    }

    protected fun forceExpandState() {
        if (showExpanded) {
            // Force the bottom sheet to be expanded
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args.toMvRxBundle()
    }

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    protected fun View.debouncedClicks(onClicked: () -> Unit) {
        clicks()
                .onEach { onClicked() }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /* ==========================================================================================
     * ViewEvents
     * ========================================================================================== */

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(
            observer: (T) -> Unit,
    ) {
        val tag = this@VectorBaseBottomSheetDialogFragment::class.simpleName.toString()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewEvents
                        .stream(tag)
                        .collect {
                            observer(it)
                        }
            }
        }
    }
}
