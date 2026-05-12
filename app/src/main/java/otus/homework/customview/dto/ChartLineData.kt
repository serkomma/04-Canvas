package otus.homework.customview.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChartLineData(
    val id: Long,
    val name: String,
    val amount: Int,
    val category: String,
    val time: Long
) : Parcelable

@Parcelize
data class ChartData(
    val data: List<ChartLineData>
) : Parcelable {
    companion object {
        val EMPTY_DATA = ChartData(emptyList())
    }
}
