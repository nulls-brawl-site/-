package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PolygonalLoader(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    sides: Int = 5,
    strokeWidth: Float = 6f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "polygon_loading")

    // Rotation angle animation
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteTransitionSpec(durationMillis = 3000),
        label = "rotation"
    )

    // Breathing scale animation
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteTransitionSpec(durationMillis = 1500, repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    // Morphing factor to slightly alter polygon radius offsets
    val morphFactor by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteTransitionSpec(durationMillis = 1000, repeatMode = RepeatMode.Reverse),
        label = "morph"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val outerRadius = (size.toPx() / 2) * 0.8f * scaleFactor

        // Draw outer polygon
        drawPolygon(
            center = center,
            sides = sides,
            radius = outerRadius,
            rotateDeg = rotateAngle,
            color = primaryColor,
            style = Stroke(width = strokeWidth, pathEffect = PathEffect.cornerPathEffect(16f))
        )

        // Draw inner reverse-rotating polygon
        drawPolygon(
            center = center,
            sides = sides,
            radius = outerRadius * 0.65f,
            rotateDeg = -rotateAngle * 1.5f + morphFactor,
            color = tertiaryColor,
            style = Stroke(width = strokeWidth * 0.7f, pathEffect = PathEffect.cornerPathEffect(8f))
        )

        // Draw a tiny solid center core polygon
        drawPolygon(
            center = center,
            sides = sides,
            radius = outerRadius * 0.25f,
            rotateDeg = rotateAngle * 2f,
            color = secondaryColor,
            style = Stroke(width = strokeWidth * 0.5f)
        )
    }
}

private fun infiniteTransitionSpec(
    durationMillis: Int,
    repeatMode: RepeatMode = RepeatMode.Restart
): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        repeatMode = repeatMode
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolygon(
    center: Offset,
    sides: Int,
    radius: Float,
    rotateDeg: Float,
    color: androidx.compose.ui.graphics.Color,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle
) {
    val path = Path()
    val radianOffset = Math.toRadians(rotateDeg.toDouble())

    for (i in 0 until sides) {
        val angle = 2 * Math.PI * i / sides + radianOffset
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    drawPath(path = path, color = color, style = style)
}
