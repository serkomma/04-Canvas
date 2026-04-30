package otus.homework.customview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import otus.homework.customview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Других идей не было
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("data_key", "already_populated")
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.sample_pie_chart_view)
        val view: PieChartView = findViewById(R.id.chart)
        if (savedInstanceState == null) {
            // Первый раз, populate
            view.populate()
        } else {
            // Не первый раз
        }
    }
}