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
    private val data: List<ChartLineData>
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
    }

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
        var prevRatio = 0f
        var lastOneWasLongButNotElevated = false
        rectFTexts.mapIndexed { index, line ->
            var ratio = TEXT_DISTANCE_RATIO
            val dataLine = data[index]
            if (dataLine.name.length / (dataLine.amount.toFloat() / chartSum * 360) > LETTERS_PER_DEGREE_RATIO) {
                ratio = ELEVATED_TEXT_DISTANCE_RATIO
                if (ratio == prevRatio) {
                    ratio = TEXT_DISTANCE_RATIO
                    lastOneWasLongButNotElevated = true
                } else {
                    lastOneWasLongButNotElevated = false
                }
            } else {
                if (lastOneWasLongButNotElevated){
                    ratio = ELEVATED_TEXT_DISTANCE_RATIO
                }
                lastOneWasLongButNotElevated = false
            }
            line.set(
                centerX - side / 2 * ratio,
                centerY - side / 2 * ratio,
                centerX + side / 2 * ratio,
                centerY + side / 2 * ratio
            )
            prevRatio = ratio
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
                    shader = RadialGradient(
                        centerX,
                        centerY,
                        side,
                        colors.first,
                        colors.second,
                        Shader.TileMode.CLAMP
                    )
                } )
            startAngle += sectorAngle
            if (!zeroSegment) startAngle += TEAR / 2

            // Рисование линий и подписей
            val textAngle = textAngleAccumulated
            val lineAngle = textAngleAccumulated.normalizeDegrees() + (DEGREE_OF_BEGINNING - 90)
            textAngleAccumulated += pureSectorAngle
            val lineBeginning = side / 2 - CHART_CIRCLE_MIN_THICKNESS
            val lineEnding = side / 2 * TEXT_DISTANCE_RATIO
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
            textPaths[index].addArc(rectFTexts[index], startTextAngle + textAngle, TEXT_ARC_LENGTH)
//            val getAnimatedText = line.name.slice(0..<(line.name.length * currentAnimationTimeRatio).toInt())
            if (currentAnimationTimeRatio == 1f) canvas.drawTextOnPath(line.name, textPaths[index], 0f, 0f, textPaint)
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
        var TEXT_DISTANCE_RATIO = 1.3f

        // Коэффициент расстояния текста на верхнем уровне от диаграммы (1.0 - середина кольца)
        // Используется, когда текст не помещается в сегмент
        var ELEVATED_TEXT_DISTANCE_RATIO = 1.37f

        // Коэффициент, отражающий, сколько символов поместится на одном градусе окружности
        var LETTERS_PER_DEGREE_RATIO = 0.5f

        // Длина дуги, выделенной под текст надписи
        var TEXT_ARC_LENGTH = 50f

        // Линии от центра диаграммы
        var LINES_IN_CENTRE = false

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