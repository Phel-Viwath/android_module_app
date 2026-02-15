package com.viwath.practice_module_app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PracticeModule(){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ValentineFlower()
    }
}

@Composable
fun ValentineFlower(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(250.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 3)

        // Petal size
        val petalRadius = size.minDimension / 8
        val petalDistance = petalRadius * 1.2f

        // Colors
        val petalColor = Color(0xFFFF4D6D) // pink-red
        val centerColor = Color(0xFFFFD60A) // yellow
        val stemColor = Color(0xFF2D6A4F) // green

        // Draw petals (5 petals)
        val angles = listOf(0f, 72f, 144f, 216f, 288f)

        angles.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            val x = center.x + (petalDistance * cos(rad)).toFloat()
            val y = center.y + (petalDistance * sin(rad)).toFloat()

            drawCircle(
                color = petalColor,
                radius = petalRadius,
                center = Offset(x, y)
            )
        }

        // Draw center circle
        drawCircle(
            color = centerColor,
            radius = petalRadius * 0.8f,
            center = center
        )

        // Draw stem
        drawLine(
            color = stemColor,
            start = Offset(center.x, center.y + petalRadius),
            end = Offset(center.x, size.height * 0.9f),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )

        // Draw left leaf
        drawOval(
            color = stemColor,
            topLeft = Offset(center.x - 90f, size.height * 0.65f),
            size = Size(120f, 60f)
        )

        // Draw right leaf
        drawOval(
            color = stemColor,
            topLeft = Offset(center.x - 30f, size.height * 0.72f),
            size = Size(120f, 60f)
        )
    }
}
@Preview(showBackground = true)
@Composable
fun ValentineFlowerPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ValentineFlower()
    }
}
