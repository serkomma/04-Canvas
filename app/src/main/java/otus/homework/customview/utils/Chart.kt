package otus.homework.customview.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import otus.homework.customview.dto.ChartLineData
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random


class Chart(
    private val data: List<ChartLineData>,
    centerX: Float,
    centerY: Float,
    side: Float
){
    private val rectF = RectF()

    private val rectFTexts: MutableList<RectF> = mutableListOf()

    private val paints: MutableList<Paint> = mutableListOf()

    private val textPaths: MutableList<Path> = mutableListOf()

    private val chartSum
        get() = data.sumOf { it.amount }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 20f
        isAntiAlias = true
    }

    private val colorsForGradient: MutableList<Pair<Int, Int>> = mutableListOf()

    private val gradients: List<RadialGradient>

    init {
        var previousColor: Int? = null
        var randomColor: Int? = null
        data.map {
            Paint().apply {
                when (COLOR_MODE) {
                    ColorMode.ONE -> color = COLOR
                    ColorMode.RANDOM -> {
                        while (randomColor == previousColor) {
                            randomColor = COLORS.random()
                        }
                        previousColor = randomColor
                        color = randomColor!!
                    }
                    else -> {}
                }
                strokeWidth = if (CHART_CIRCLE_MIN_THICKNESS == CHART_CIRCLE_MAX_THICKNESS)
                    CHART_CIRCLE_MIN_THICKNESS.toFloat()
                else
                    Random.nextInt(CHART_CIRCLE_MIN_THICKNESS, CHART_CIRCLE_MAX_THICKNESS).toFloat()
                style = Paint.Style.STROKE
            }
        }.let { paints.addAll(it) }

        data.map {
            Path()
        }.let { textPaths.addAll(it) }

        data.map {
            RectF()
        }.let { rectFTexts.addAll(it) }

        data.map {
            COLORS.random() to COLORS.random()
        }.let { colorsForGradient.addAll(it) }

        gradients = List(data.size) { index ->
            val colors = colorsForGradient[index]
            RadialGradient(
                centerX,
                centerY,
                side,
                colors.first,
                colors.second,
                Shader.TileMode.CLAMP
            )
        }
    }

    data class TextProcessing(
        var value: Float,
        var ratio: Float,
    )

    fun draw(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        side: Float,
        currentAnimationTimeRatio: Float = 1f,
    ){
        rectF.set(
            centerX - side / 2,
            centerY - side / 2,
            centerX + side / 2,
            centerY + side / 2
        )

        // Расчёты параметров квадратов для рисования текстовой дуги
        val textMeasures: MutableList<TextProcessing> = mutableListOf()
        var cumulativeSectorAngle = 0f
        rectFTexts.mapIndexed { index, line ->
            val dataLine = data[index]
            val textMeasure = textPaint.measureText(dataLine.name)
            val sectorAngle = dataLine.amount.toFloat() / chartSum * 360
            var ratio = FIRST_LEVEL_TEXT_DISTANCE_RATIO
            if (textMeasures.none { it.ratio == THIRD_LEVEL_TEXT_DISTANCE_RATIO })
                ratio = THIRD_LEVEL_TEXT_DISTANCE_RATIO
            if (textMeasures.none { it.ratio == SECOND_LEVEL_TEXT_DISTANCE_RATIO })
                ratio = SECOND_LEVEL_TEXT_DISTANCE_RATIO
            if (textMeasures.none { it.ratio == FIRST_LEVEL_TEXT_DISTANCE_RATIO })
                ratio = FIRST_LEVEL_TEXT_DISTANCE_RATIO

            // arc length = pi*r*n/180
            val sectorChartArcLength = (Math.PI * (side / 2 * ratio) * sectorAngle / 180).toFloat()

            // Предотвращение соприкосновений текста на втором круге
            // n = 180*l/(pi*R)
            val textAngleOnSecondCircle = 180 * textMeasure / (Math.PI * (side / 2 * ratio)) +
                    cumulativeSectorAngle - 360
            if (textMeasure > sectorChartArcLength
                && textAngleOnSecondCircle > 0) {
                ratio = if (textAngleOnSecondCircle < data[0].amount.toFloat() / chartSum * 360
                    && textMeasures.none { it.ratio == SECOND_LEVEL_TEXT_DISTANCE_RATIO })
                    SECOND_LEVEL_TEXT_DISTANCE_RATIO
                else THIRD_LEVEL_TEXT_DISTANCE_RATIO
            }

            line.set(
                centerX - side / 2 * ratio,
                centerY - side / 2 * ratio,
                centerX + side / 2 * ratio,
                centerY + side / 2 * ratio
            )

            for (i in textMeasures.indices) {
                val currentValue = textMeasures[i]
                val valueDecrease = currentValue.value - sectorChartArcLength
                textMeasures[i] = currentValue.copy(value = valueDecrease,)
            }

            textMeasures.removeIf { it.value <= TEXT_OVERLAY }

            if ((textMeasure - sectorChartArcLength) > 0)
                textMeasures.add(
                    TextProcessing(textMeasure - sectorChartArcLength, ratio)
                )
            cumulativeSectorAngle += sectorAngle
        }

        var startAngle = DEGREE_OF_BEGINNING
        val startTextAngle = DEGREE_OF_BEGINNING
        var textAngleAccumulated = 0f
        var zeroSegment = false
        data.forEachIndexed { index, line ->
            // Рисование диаграммы
            val pureSectorAngle = line.amount.toFloat() / chartSum * 360
            val sectorAngle = (pureSectorAngle - TEAR / 2).let {
                if (it <= 0) { zeroSegment = true; pureSectorAngle } else { zeroSegment = false; it }
            }
            canvas.drawArc(
                rectF,
                startAngle,
                sectorAngle * currentAnimationTimeRatio,
                false,
                paints[index].applyIf(COLOR_MODE == ColorMode.GRADIENT) {
                    val colors = colorsForGradient[index]
                    // Градиент, как я понял, заранее создать нельзя
                    shader = gradients[index]
                } )
            startAngle += sectorAngle
            if (!zeroSegment) startAngle += TEAR / 2

            // Рисование линий и подписей
            val textAngle = textAngleAccumulated
            val lineAngle = textAngleAccumulated.normalizeDegrees() + (DEGREE_OF_BEGINNING - 90)
            textAngleAccumulated += pureSectorAngle
            val lineBeginning = side / 2 - CHART_CIRCLE_MIN_THICKNESS
            val lineEnding = side / 2 * FIRST_LEVEL_TEXT_DISTANCE_RATIO
            val (textXBeg, textYBeg) = if (LINES_IN_CENTRE) {
                centerX to centerY
            } else {
                centerX + lineBeginning * cos(toRadians(lineAngle.toDouble())).toFloat() to
                        centerY + lineBeginning * sin(toRadians(lineAngle.toDouble())).toFloat()
            }
            val sideWithAnimationRatio = (lineEnding - lineBeginning) * currentAnimationTimeRatio + lineBeginning
            val textX = centerX + sideWithAnimationRatio * cos(toRadians(lineAngle.toDouble())).toFloat()
            val textY = centerY + sideWithAnimationRatio * sin(toRadians(lineAngle.toDouble())).toFloat()
            canvas.drawLine(textXBeg, textYBeg, textX, textY, textPaint)
            textPaths[index].reset()
            textPaths[index].addArc(rectFTexts[index], startTextAngle + textAngle, TEXT_ARC_LENGTH)
//            val getAnimatedText = line.name.slice(0..<(line.name.length * currentAnimationTimeRatio).toInt())
            if (currentAnimationTimeRatio == 1f)
                canvas.drawTextOnPath(
                    // Визуальная пометка для очень маленьких сегментов
                    if (zeroSegment) "<${line.name}" else line.name,
                    textPaths[index],
                    0f,
                    0f,
                    textPaint
                )
        }
    }

    fun getCategoryByCoordinates(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        side: Float
    ): String? {
        // Проверка, что точка в круге
        if ((x - centerX).pow(2) + (y - centerY).pow(2)
            > (side / 2 + CHART_CIRCLE_MAX_THICKNESS / 2).pow(2)) return null

        // Работа с сектором
        val clickDegrees = toDegrees(atan2(y - centerY, x - centerX).toDouble())
            .toFloat()
            .normalizeDegrees()

        var startAngle = 0f
        data.forEach { line ->
            val sectorAngle = line.amount.toFloat() / chartSum * 360
            val sectorAngleTo = startAngle + sectorAngle
            if (clickDegrees > startAngle && clickDegrees < sectorAngleTo)
                return line.category
            startAngle = sectorAngleTo
        }

        return "Непредвиденная ошибка"
    }

    companion object {
        // Угол начала рисования диаграммы (270 - на 12 часов)
        var DEGREE_OF_BEGINNING = 270f

        // Минимальная толщина кольца диаграммы
        var CHART_CIRCLE_MIN_THICKNESS = 50

        // Максимальная толщина кольца диаграммы
        var CHART_CIRCLE_MAX_THICKNESS = 150

        // Разрыв между сегментами диаграммы
        var TEAR = 3f

        // Коэффициент расстояния текста от центра диаграммы (1.0 - середина кольца)
        var FIRST_LEVEL_TEXT_DISTANCE_RATIO = 1.3f

        // Коэффициент расстояния текста на втором уровне от диаграммы (1.0 - середина кольца)
        // Используется, когда текст не помещается в сегмент
        var SECOND_LEVEL_TEXT_DISTANCE_RATIO = 1.37f

        // Коэффициент расстояния текста на третьем уровне от диаграммы (1.0 - середина кольца)
        // Используется, когда текст не помещался в предыдущий и не помещается в текущий сегмент
        var THIRD_LEVEL_TEXT_DISTANCE_RATIO = 1.44f

        // Длина дуги, выделенной под текст надписи
        var TEXT_ARC_LENGTH = 50f

        // Линии от центра диаграммы
        var LINES_IN_CENTRE = false

        // Допустимое наложение соседних текстов
        // При увеличении возможны коллизии текстов на соседних секторах
        // При уменьшении до 0 - слишком ранний переход текста на соседний уровень и худшая работа в очень плотной диаграмме
        var TEXT_OVERLAY = 0.05f

        // Цвет диаграммы, используется, если COLOR_MODE = ColorMode.ONE
        var COLOR = Color.DKGRAY

        // Режим цветов диаграммы.
        // ONE -> диаграмма одного цвета
        // RANDOM -> сегменты диаграммы случайных цветов из списка COLORS
        // GRADIENT -> сегменты диаграммы выкрашены в градиент двух случайных цветов из списка COLORS
        var COLOR_MODE = ColorMode.GRADIENT

        // Список цветов, используется при раскраске диаграммы в случайные цвета и для составления градиентов
        val COLORS = mutableListOf(
            Color.RED,
            Color.BLACK,
            Color.BLUE,
            Color.GRAY,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.YELLOW,
            Color.GREEN,
            Color.DKGRAY
        )
        enum class ColorMode {
            ONE,
            RANDOM,
            GRADIENT
        }
    }
}