package otus.homework.customview

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import otus.homework.customview.dto.ChartData
import otus.homework.customview.utils.Chart
import kotlin.math.max

typealias Ass = (String) -> Unit
class PieChartView @JvmOverloads constructor (
    myContext: Context,
    attrs: AttributeSet? = null,
) : View(myContext, attrs) {
    private var chart: Chart? = null

    private val startTime = System.currentTimeMillis()

    private var chartData: ChartData = ChartData.EMPTY_DATA


    private var onCategoryClick: ((String) -> Unit)? = null

    fun setOnCategoryClick(callback: (String) -> Unit) {
        onCategoryClick = callback
    }

    fun populate(data: ChartData){
        chartData = data
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.value = chartData
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            chartData = state.value!!
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        val chartGain = Chart.CHART_CIRCLE_MAX_THICKNESS / 2 * Chart.THIRD_LEVEL_TEXT_DISTANCE_RATIO
        val desiredWidth = (wSize * CHART_SIZE_RATIO + chartGain).toInt()
        val desiredHeight = (hSize * CHART_SIZE_RATIO + chartGain).toInt()
        val width = when (wMode) {
            MeasureSpec.EXACTLY -> wSize
            MeasureSpec.AT_MOST -> max(desiredWidth, wSize)
            else -> desiredWidth
        }
        val height = when (hMode) {
            MeasureSpec.EXACTLY -> hSize
            MeasureSpec.AT_MOST -> max(desiredHeight, hSize)
            else -> desiredHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val (centerX, centerY, side) = calculateCartPositionParameters(w, h)
        chart = Chart(chartData.data, centerX, centerY, side)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val (centerX, centerY, side) = calculateCartPositionParameters()
        val elapsedTime = System.currentTimeMillis() - startTime
        var animationInProcess = false
        val animationTime = if (elapsedTime <= 1000) {
            animationInProcess = true
            elapsedTime / 1000f
        } else 1f
        chart?.draw(canvas, centerX, centerY, side, animationTime)
        if (animationInProcess) invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val (centerX, centerY, side) = calculateCartPositionParameters()
        event?.let {
            chart?.getCategoryByCoordinates(event.x, event.y, centerX, centerY, side)?.let {
                onCategoryClick?.invoke(it)
            }
        }
        return super.onTouchEvent(event)
    }

    // returns Triple(centerX, centerY, side)
    fun calculateCartPositionParameters(
        iHeight: Int? = null,
        iWidth: Int? = null,
    ): Triple<Float, Float, Float> {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = (iWidth ?: width) - paddingLeft - paddingRight
        val contentHeight = (iHeight ?: height) - paddingTop - paddingBottom

        val centerX = contentWidth / 2f
        val centerY = contentHeight / 2f
        val orientation = resources.configuration.orientation
        val side: Float = if (orientation == Configuration.ORIENTATION_PORTRAIT)
            contentWidth * CHART_SIZE_RATIO
        else contentHeight * CHART_SIZE_RATIO
        return Triple(centerX, centerY, side)
    }

    internal class SavedState : BaseSavedState {
        var value: ChartData? = null

        constructor(superState: Parcelable?) : super(superState)
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        constructor(source: Parcel): super(source) {
            value = source.readParcelable(ChartData::class.java.classLoader, ChartData::class.java)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(value, flags)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    companion object {
        const val CHART_SIZE_RATIO = 0.5f
    }
}