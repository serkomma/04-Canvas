package otus.homework.customview

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.sample_pie_chart_view)
        val view: PieChartView = findViewById(R.id.chart)
        view.setOnCategoryClick {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        initView(view)
    }

    fun initView(view: PieChartView) {
        val repository = ChartDataRepository(this)
        lifecycleScope.launch {
            repository.consumeChartData().collect {
                view.populate(it)
            }
        }
    }
}