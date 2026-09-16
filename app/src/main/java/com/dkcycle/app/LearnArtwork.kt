package com.dkcycle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class LearnArt {
    OVERVIEW,
    MENSTRUAL,
    FOLLICULAR,
    OVULATION,
    LUTEAL
}

@Composable
internal fun LearnIllustration(art: LearnArt, title: String) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    val accent = MaterialTheme.colorScheme.secondaryContainer

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(background)
            .semantics { contentDescription = "$title illustration" }
    ) {
        drawCircle(
            color = accent.copy(alpha = 0.55f),
            radius = size.minDimension * 0.46f,
            center = Offset(size.width * 0.83f, size.height * 0.15f)
        )
        drawCircle(
            color = primary.copy(alpha = 0.08f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.08f, size.height * 0.88f)
        )

        when (art) {
            LearnArt.OVERVIEW -> drawOverview(primary, secondary, surface)
            LearnArt.MENSTRUAL -> drawMenstrual(primary, secondary, surface)
            LearnArt.FOLLICULAR -> drawFollicular(primary, secondary, surface)
            LearnArt.OVULATION -> drawOvulation(primary, secondary, surface)
            LearnArt.LUTEAL -> drawLuteal(primary, secondary, surface)
        }
    }
}

private fun DrawScope.drawOverview(primary: Color, secondary: Color, surface: Color) {
    val diameter = size.minDimension * 0.67f
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val stroke = diameter * 0.095f
    val colours = listOf(primary, secondary, primary.copy(alpha = 0.55f), secondary.copy(alpha = 0.55f))

    colours.forEachIndexed { index, colour ->
        drawArc(
            color = colour,
            startAngle = -86f + (index * 90f),
            sweepAngle = 78f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }

    drawCircle(surface.copy(alpha = 0.88f), diameter * 0.26f, center)
    drawCircle(primary.copy(alpha = 0.18f), diameter * 0.13f, center)
    drawCircle(primary, diameter * 0.035f, center)
}

private fun DrawScope.drawMenstrual(primary: Color, secondary: Color, surface: Color) {
    val c = Offset(size.width * 0.5f, size.height * 0.49f)
    val bodyW = size.width * 0.27f
    val bodyH = size.height * 0.36f

    drawUterus(c, bodyW, bodyH, primary, secondary, surface)

    val dropY = size.height * 0.80f
    listOf(-0.12f, 0f, 0.12f).forEachIndexed { index, xOffset ->
        val x = size.width * (0.5f + xOffset)
        val r = size.minDimension * if (index == 1) 0.042f else 0.032f
        val path = Path().apply {
            moveTo(x, dropY - r * 1.8f)
            cubicTo(x - r * 1.2f, dropY - r * 0.3f, x - r, dropY + r, x, dropY + r * 1.2f)
            cubicTo(x + r, dropY + r, x + r * 1.2f, dropY - r * 0.3f, x, dropY - r * 1.8f)
            close()
        }
        drawPath(path, primary.copy(alpha = if (index == 1) 0.9f else 0.58f))
    }
}

private fun DrawScope.drawFollicular(primary: Color, secondary: Color, surface: Color) {
    val ovaryCenter = Offset(size.width * 0.36f, size.height * 0.52f)
    val ovarySize = Size(size.width * 0.28f, size.height * 0.33f)
    drawOval(
        color = surface.copy(alpha = 0.9f),
        topLeft = Offset(ovaryCenter.x - ovarySize.width / 2f, ovaryCenter.y - ovarySize.height / 2f),
        size = ovarySize
    )
    drawOval(
        color = secondary.copy(alpha = 0.75f),
        topLeft = Offset(ovaryCenter.x - ovarySize.width / 2f, ovaryCenter.y - ovarySize.height / 2f),
        size = ovarySize,
        style = Stroke(width = size.minDimension * 0.018f)
    )

    val follicles = listOf(
        Triple(-0.07f, -0.06f, 0.030f),
        Triple(0.03f, -0.10f, 0.043f),
        Triple(-0.02f, 0.08f, 0.053f),
        Triple(0.085f, 0.035f, 0.073f)
    )
    follicles.forEachIndexed { index, (dx, dy, rr) ->
        val center = Offset(ovaryCenter.x + size.width * dx, ovaryCenter.y + size.height * dy)
        drawCircle(primary.copy(alpha = 0.14f + index * 0.12f), size.minDimension * rr, center)
        drawCircle(primary.copy(alpha = 0.78f), size.minDimension * rr, center, style = Stroke(width = size.minDimension * 0.012f))
    }

    val chartLeft = size.width * 0.61f
    val chartBottom = size.height * 0.72f
    val chartTop = size.height * 0.29f
    val chartRight = size.width * 0.88f
    drawLine(secondary.copy(alpha = 0.3f), Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom), size.minDimension * 0.008f)
    drawLine(secondary.copy(alpha = 0.3f), Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), size.minDimension * 0.008f)

    val rise = Path().apply {
        moveTo(chartLeft, chartBottom * 0.96f)
        cubicTo(
            size.width * 0.69f, size.height * 0.67f,
            size.width * 0.76f, size.height * 0.56f,
            chartRight, size.height * 0.34f
        )
    }
    drawPath(rise, primary, style = Stroke(width = size.minDimension * 0.026f, cap = StrokeCap.Round))
    drawCircle(primary, size.minDimension * 0.027f, Offset(chartRight, size.height * 0.34f))
}

private fun DrawScope.drawOvulation(primary: Color, secondary: Color, surface: Color) {
    val ovary = Offset(size.width * 0.36f, size.height * 0.54f)
    val ovarySize = Size(size.width * 0.28f, size.height * 0.31f)
    drawOval(
        color = surface.copy(alpha = 0.9f),
        topLeft = Offset(ovary.x - ovarySize.width / 2f, ovary.y - ovarySize.height / 2f),
        size = ovarySize
    )
    drawOval(
        color = secondary.copy(alpha = 0.72f),
        topLeft = Offset(ovary.x - ovarySize.width / 2f, ovary.y - ovarySize.height / 2f),
        size = ovarySize,
        style = Stroke(width = size.minDimension * 0.018f)
    )

    val egg = Offset(size.width * 0.68f, size.height * 0.50f)
    drawLine(
        color = primary.copy(alpha = 0.48f),
        start = Offset(ovary.x + ovarySize.width * 0.43f, ovary.y),
        end = Offset(egg.x - size.minDimension * 0.08f, egg.y),
        strokeWidth = size.minDimension * 0.02f,
        cap = StrokeCap.Round
    )
    drawCircle(primary.copy(alpha = 0.14f), size.minDimension * 0.13f, egg)
    drawCircle(surface, size.minDimension * 0.082f, egg)
    drawCircle(primary, size.minDimension * 0.082f, egg, style = Stroke(width = size.minDimension * 0.018f))
    drawCircle(primary.copy(alpha = 0.55f), size.minDimension * 0.021f, egg)

    repeat(10) { index ->
        val angle = Math.toRadians((index * 36.0) - 90.0)
        val inner = size.minDimension * 0.15f
        val outer = size.minDimension * 0.19f
        drawLine(
            primary.copy(alpha = 0.42f),
            Offset(egg.x + cos(angle).toFloat() * inner, egg.y + sin(angle).toFloat() * inner),
            Offset(egg.x + cos(angle).toFloat() * outer, egg.y + sin(angle).toFloat() * outer),
            size.minDimension * 0.009f,
            StrokeCap.Round
        )
    }
}

private fun DrawScope.drawLuteal(primary: Color, secondary: Color, surface: Color) {
    val ovary = Offset(size.width * 0.38f, size.height * 0.53f)
    val ovarySize = Size(size.width * 0.31f, size.height * 0.34f)
    drawOval(
        color = surface.copy(alpha = 0.9f),
        topLeft = Offset(ovary.x - ovarySize.width / 2f, ovary.y - ovarySize.height / 2f),
        size = ovarySize
    )
    drawOval(
        color = secondary.copy(alpha = 0.72f),
        topLeft = Offset(ovary.x - ovarySize.width / 2f, ovary.y - ovarySize.height / 2f),
        size = ovarySize,
        style = Stroke(width = size.minDimension * 0.018f)
    )

    val bloom = Offset(ovary.x + size.width * 0.025f, ovary.y)
    repeat(8) { index ->
        val angle = Math.toRadians(index * 45.0)
        val petalCenter = Offset(
            bloom.x + cos(angle).toFloat() * size.minDimension * 0.055f,
            bloom.y + sin(angle).toFloat() * size.minDimension * 0.055f
        )
        drawCircle(primary.copy(alpha = 0.60f), size.minDimension * 0.042f, petalCenter)
    }
    drawCircle(secondary, size.minDimension * 0.036f, bloom)

    val wave = Path().apply {
        moveTo(size.width * 0.61f, size.height * 0.66f)
        cubicTo(
            size.width * 0.67f, size.height * 0.42f,
            size.width * 0.73f, size.height * 0.35f,
            size.width * 0.79f, size.height * 0.38f
        )
        cubicTo(
            size.width * 0.84f, size.height * 0.41f,
            size.width * 0.86f, size.height * 0.55f,
            size.width * 0.89f, size.height * 0.62f
        )
    }
    drawPath(wave, primary, style = Stroke(width = size.minDimension * 0.026f, cap = StrokeCap.Round))
    drawLine(
        secondary.copy(alpha = 0.25f),
        Offset(size.width * 0.60f, size.height * 0.71f),
        Offset(size.width * 0.90f, size.height * 0.71f),
        size.minDimension * 0.008f
    )
}

private fun DrawScope.drawUterus(
    center: Offset,
    bodyW: Float,
    bodyH: Float,
    primary: Color,
    secondary: Color,
    surface: Color
) {
    val stroke = size.minDimension * 0.020f
    val body = Path().apply {
        moveTo(center.x - bodyW * 0.26f, center.y - bodyH * 0.22f)
        cubicTo(
            center.x - bodyW * 0.38f, center.y + bodyH * 0.05f,
            center.x - bodyW * 0.18f, center.y + bodyH * 0.34f,
            center.x, center.y + bodyH * 0.40f
        )
        cubicTo(
            center.x + bodyW * 0.18f, center.y + bodyH * 0.34f,
            center.x + bodyW * 0.38f, center.y + bodyH * 0.05f,
            center.x + bodyW * 0.26f, center.y - bodyH * 0.22f
        )
        cubicTo(
            center.x + bodyW * 0.14f, center.y - bodyH * 0.34f,
            center.x - bodyW * 0.14f, center.y - bodyH * 0.34f,
            center.x - bodyW * 0.26f, center.y - bodyH * 0.22f
        )
        close()
    }
    drawPath(body, surface.copy(alpha = 0.92f))
    drawPath(body, primary.copy(alpha = 0.82f), style = Stroke(width = stroke, cap = StrokeCap.Round))

    val leftOvary = Offset(center.x - bodyW * 0.69f, center.y - bodyH * 0.28f)
    val rightOvary = Offset(center.x + bodyW * 0.69f, center.y - bodyH * 0.28f)
    drawLine(primary, Offset(center.x - bodyW * 0.23f, center.y - bodyH * 0.22f), leftOvary, stroke, StrokeCap.Round)
    drawLine(primary, Offset(center.x + bodyW * 0.23f, center.y - bodyH * 0.22f), rightOvary, stroke, StrokeCap.Round)
    drawCircle(secondary.copy(alpha = 0.75f), size.minDimension * 0.045f, leftOvary)
    drawCircle(secondary.copy(alpha = 0.75f), size.minDimension * 0.045f, rightOvary)
    drawLine(primary, Offset(center.x, center.y + bodyH * 0.38f), Offset(center.x, center.y + bodyH * 0.58f), stroke, StrokeCap.Round)
}
