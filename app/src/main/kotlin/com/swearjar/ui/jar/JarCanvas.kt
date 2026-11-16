package com.swearjar.ui.jar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swearjar.R
import com.swearjar.ui.theme.SwearjarTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The jar. Drawn entirely from state, so it can be previewed and tested without a backend.
 *
 * The contents are a flat fill that rises to a dashed goal line. Past the goal the fill
 * carries on up the neck to the rim and deepens in colour.
 *
 * @param fillFraction 0 is empty, 1 is at the goal, above 1 overflows (see `GoalRules.fillFraction`)
 * @param celebrate true while the goal-reached celebration should play
 */
@Composable
fun JarCanvas(
    fillFraction: Float,
    modifier: Modifier = Modifier,
    celebrate: Boolean = false,
) {
    val fill by animateFloatAsState(
        targetValue = fillFraction.coerceIn(0f, MAX_FILL),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "jarFill",
    )

    val confetti = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        confetti.snapTo(0f)
        if (celebrate) confetti.animateTo(1f, tween(durationMillis = 2400, easing = LinearEasing))
    }

    val particles = remember { confettiLayout() }
    val glass = MaterialTheme.colorScheme.outline
    val description = stringResource(
        R.string.jar_content_description,
        (fillFraction.coerceAtLeast(0f) * 100).roundToInt(),
    )

    Canvas(
        modifier
            .aspectRatio(JAR_ASPECT)
            .semantics { contentDescription = description },
    ) {
        val w = size.width
        val h = size.height
        val jar = jarPath(w, h)
        val bottom = BOTTOM * h
        val goalLine = GOAL_LINE * h
        val rim = NECK_TOP * h
        // 0..1 runs from the bottom to the goal line; 1..MAX_FILL from there up to the rim.
        val over = ((fill - 1f) / (MAX_FILL - 1f)).coerceIn(0f, 1f)
        val level = if (fill <= 1f) bottom - fill * (bottom - goalLine) else goalLine - over * (goalLine - rim)
        val line = 0.022f * w
        val contents = lerp(Gold, GoldOver, over)

        // Glass body.
        drawPath(jar, glass.copy(alpha = 0.08f))

        clipPath(jar) {
            if (fill > 0f) {
                drawRect(
                    color = contents,
                    topLeft = Offset(0f, level),
                    size = Size(w, bottom - level),
                )
                // The surface.
                drawLine(
                    color = lerp(GoldDark, GoldOverDark, over),
                    start = Offset(0f, level),
                    end = Offset(w, level),
                    strokeWidth = line * 0.8f,
                )
            }
            // Where the goal is, until the contents reach it.
            if (fill < 1f) {
                drawLine(
                    color = glass.copy(alpha = 0.7f),
                    start = Offset(0f, goalLine),
                    end = Offset(w, goalLine),
                    strokeWidth = line * 0.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 2.5f, line * 2f)),
                )
            }
        }

        // Glass outline, rim and a highlight, on top of the contents.
        drawPath(jar, glass, style = Stroke(width = line, join = StrokeJoin.Round))
        drawRoundRect(
            color = glass,
            topLeft = Offset(RIM_LEFT * w, (NECK_TOP - 0.02f) * h),
            size = Size((1f - 2 * RIM_LEFT) * w, 0.04f * h),
            cornerRadius = CornerRadius(0.02f * h),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(0.17f * w, 0.40f * h),
            end = Offset(0.17f * w, 0.78f * h),
            strokeWidth = line * 1.4f,
            cap = StrokeCap.Round,
        )

        if (confetti.value > 0f && confetti.value < 1f) {
            drawConfetti(particles, confetti.value, w, h)
        }
    }
}

// ---- Geometry, as fractions of the canvas ----

private const val JAR_ASPECT = 0.72f
private const val MAX_FILL = 1.5f
private const val NECK_TOP = 0.14f
private const val SHOULDER = 0.25f
private const val BOTTOM = 0.97f
private const val GOAL_LINE = 0.30f
private const val NECK_LEFT = 0.27f
private const val RIM_LEFT = 0.23f
private const val BODY_LEFT = 0.07f

private val Gold = Color(0xFFF2B705)
private val GoldDark = Color(0xFFB87F00)
private val GoldOver = Color(0xFFE8590C)
private val GoldOverDark = Color(0xFFA63D00)
private val ConfettiColors = listOf(
    Color(0xFFE5484D), Color(0xFF3E9BFF), Color(0xFF46A758), Color(0xFFF2B705), Color(0xFF8E4EC6),
)

private fun jarPath(w: Float, h: Float): Path = Path().apply {
    val neckL = NECK_LEFT * w
    val neckR = w - neckL
    val bodyL = BODY_LEFT * w
    val bodyR = w - bodyL
    val neckTop = NECK_TOP * h
    val shoulder = SHOULDER * h
    val bottom = BOTTOM * h
    val r = 0.14f * w
    moveTo(neckL, neckTop)
    lineTo(neckL, shoulder - 0.04f * h)
    cubicTo(neckL, shoulder, bodyL, shoulder, bodyL, shoulder + 0.09f * h)
    lineTo(bodyL, bottom - r)
    quadraticTo(bodyL, bottom, bodyL + r, bottom)
    lineTo(bodyR - r, bottom)
    quadraticTo(bodyR, bottom, bodyR, bottom - r)
    lineTo(bodyR, shoulder + 0.09f * h)
    cubicTo(bodyR, shoulder, neckR, shoulder, neckR, shoulder - 0.04f * h)
    lineTo(neckR, neckTop)
    close()
}

private class Particle(val angle: Float, val speed: Float, val spin: Float, val color: Color, val size: Float)

private fun confettiLayout(): List<Particle> {
    val random = Random(3)
    return List(48) {
        Particle(
            // Mostly upwards, fanned out to both sides.
            angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 0.9f).toFloat(),
            speed = 0.6f + random.nextFloat() * 0.7f,
            spin = (random.nextFloat() - 0.5f) * 1440f,
            color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            size = 0.018f + random.nextFloat() * 0.018f,
        )
    }
}

private fun DrawScope.drawConfetti(particles: List<Particle>, t: Float, w: Float, h: Float) {
    val origin = Offset(w / 2f, NECK_TOP * h)
    val alpha = (1f - t * t).coerceIn(0f, 1f)
    for (p in particles) {
        val distance = p.speed * t * h
        val x = origin.x + cos(p.angle) * distance * 0.8f
        val y = origin.y + sin(p.angle) * distance + 1.1f * t * t * h
        val side = p.size * w
        rotate(degrees = p.spin * t, pivot = Offset(x, y)) {
            drawRect(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(x - side / 2f, y - side / 4f),
                size = Size(side, side / 2f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun JarEmptyPreview() = SwearjarTheme { JarCanvas(0f, Modifier.padding(16.dp).width(180.dp)) }

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun JarHalfPreview() = SwearjarTheme { JarCanvas(0.5f, Modifier.padding(16.dp).width(180.dp)) }

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun JarFullPreview() = SwearjarTheme { JarCanvas(1f, Modifier.padding(16.dp).width(180.dp)) }

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun JarOverflowPreview() = SwearjarTheme { JarCanvas(1.5f, Modifier.padding(16.dp).width(180.dp)) }
