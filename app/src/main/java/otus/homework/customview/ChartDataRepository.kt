package otus.homework.customview

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import otus.homework.customview.dto.ChartData
import otus.homework.customview.dto.ChartLineData

class ChartDataRepository(context: Context) {
    private val appContext = context.applicationContext
    fun consumeChartData(): Flow<ChartData> {
        val inputStream = appContext.resources.openRawResource(R.raw.payload)
        val json = inputStream.bufferedReader().use { it.readText() }
        val userListType = object : TypeToken<List<ChartLineData>>() {}.type
        val chartData = ChartData(Gson().fromJson(json, userListType))
        val categorizedChartData = chartData.data
            .groupBy ({ it.category }, { it.amount })
            .map { ChartLineData(id = 1, name = it.key, amount = it.value.sum(), category = it.key, time = 0) }
        return flow {
            emit(ChartData(categorizedChartData))
        }
    }
}