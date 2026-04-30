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
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.customview.dto.ChartData
import otus.homework.customview.dto.ChartLineData
import otus.homework.customview.utils.Chart
import kotlin.math.max

class PieChartView @JvmOverloads constructor (
    myContext: Context,
    attrs: AttributeSet? = null,
) : View(myContext, attrs) {
    private lateinit var chart: Chart

    private val startTime = System.currentTimeMillis()

    private lateinit var chartData: ChartData

    fun populate(){
        val inputStream = resources.openRawResource(R.raw.payload)
        val json = inputStream.bufferedReader().use { it.readText() }
        val userListType = object : TypeToken<List<ChartLineData>>() {}.type
        chartData = ChartData(Gson().fromJson(json, userListType))
        chart = Chart(chartData.data)
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
            chart = Chart(chartData.data)
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
            else -> desiredWidth
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val centerX = contentWidth / 2f
        val centerY = contentHeight / 2f
        val orientation = resources.configuration.orientation
        val side: Float = if (orientation == Configuration.ORIENTATION_PORTRAIT)
            contentWidth * CHART_SIZE_RATIO
        else contentHeight * CHART_SIZE_RATIO

        val elapsedTime = System.currentTimeMillis() - startTime
        var animationInProcess = false
        val animationTime = if (elapsedTime <= 1000) {
            animationInProcess = true
            elapsedTime / 1000f
        } else 1f
        chart.draw(canvas, centerX, centerY, side, animationTime)
        if (animationInProcess) invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val centerX = contentWidth / 2f
        val centerY = contentHeight / 2f
        val side: Float = contentWidth * 0.5f
        event?.let {
            chart.getCategoryByCoordinates(event.x, event.y, centerX, centerY, side)?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
        return super.onTouchEvent(event)
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