package com.swearjar.ui.jar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
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
 * @param fillFraction 0 is empty, 1 is at the goal, above 1 overflows (see `GoalRules.fillFraction`)
 * @param dropKey change this to drop a coin in; typically the number of entries in the round
 * @param celebrate true while the goal-reached celebration should play
 */
@Composable
fun JarCanvas(
    fillFraction: Float,
    modifier: Modifier = Modifier,
    dropKey: Int = 0,
    celebrate: Boolean = false,
) {
    val fill by animateFloatAsState(
        targetValue = fillFraction.coerceIn(0f, MAX_FILL),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "jarFill",
    )

    val drop = remember { Animatable(1f) }
    LaunchedEffect(dropKey) {
        if (dropKey != 0) {
            drop.snapTo(0f)
            drop.animateTo(1f, tween(durationMillis = 550, easing = FastOutLinearInEasing))
        }
    }

    val confetti = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        confetti.snapTo(0f)
        if (celebrate) confetti.animateTo(1f, tween(durationMillis = 2400, easing = LinearEasing))
    }

    val coins = remember { coinLayout() }
    val spill = remember { spillLayout() }
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
        val fullLine = FULL_LINE * h
        val level = bottom - fill.coerceAtMost(1f) * (bottom - fullLine)
        val coinRadius = COIN_RADIUS * w

        // Glass body.
        drawPath(jar, glass.copy(alpha = 0.08f))

        clipPath(jar) {
            // A wash behind the coins so the gaps between them are not bare glass.
            drawRect(
                color = Gold.copy(alpha = 0.35f),
                topLeft = Offset(0f, level),
                size = Size(w, (bottom - level).coerceAtLeast(0f)),
            )
            for (coin in coins) {
                val cy = coin.y * h
                // Each coin sits a little above or below the line so the surface looks heaped.
                if (cy + coin.jitter * coinRadius >= level) {
                    drawCoin(Offset(coin.x * w, cy), coinRadius, coin.silver)
                }
            }
            // The coin on its way in.
            if (drop.value < 1f) {
                val from = -coinRadius
                val to = (level - coinRadius).coerceAtLeast(NECK_TOP * h)
                drawCoin(Offset(w / 2f, from + (to - from) * drop.value), coinRadius, silver = false)
            }
        }

        // Past the goal: coins heap up out of the mouth.
        if (fill > 1f) {
            val shown = (((fill - 1f) / (MAX_FILL - 1f)) * spill.size).roundToInt()
            for (coin in spill.take(shown)) {
                drawCoin(Offset(coin.x * w, coin.y * h), coinRadius, coin.silver)
            }
        }

        // Glass outline, rim and a highlight, on top of the contents.
        val line = 0.022f * w
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
private const val FULL_LINE = 0.19f
private const val NECK_LEFT = 0.27f
private const val RIM_LEFT = 0.23f
private const val BODY_LEFT = 0.07f
private const val COIN_RADIUS = 0.058f

private val Gold = Color(0xFFF2B705)
private val GoldDark = Color(0xFFB87F00)
private val GoldLight = Color(0xFFFFE08A)
private val Silver = Color(0xFFC9CED6)
private val SilverDark = Color(0xFF8C939E)
private val SilverLight = Color(0xFFF1F3F6)
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

private class Coin(val x: Float, val y: Float, val jitter: Float, val silver: Boolean)

/** Staggered rows of coins covering the whole jar interior; the fill level decides which show. */
private fun coinLayout(): List<Coin> {
    val random = Random(7)
    val coins = mutableListOf<Coin>()
    val stepX = COIN_RADIUS * 1.75f
    val stepY = COIN_RADIUS * 1.5f * JAR_ASPECT
    var y = BOTTOM - COIN_RADIUS * JAR_ASPECT * 0.8f
    var row = 0
    while (y > NECK_TOP) {
        var x = BODY_LEFT + (if (row % 2 == 0) 0f else stepX / 2f)
        while (x < 1f - BODY_LEFT + stepX) {
            coins += Coin(
                x = x + (random.nextFloat() - 0.5f) * stepX * 0.3f,
                y = y + (random.nextFloat() - 0.5f) * stepY * 0.3f,
                jitter = random.nextFloat() * 2f - 1f,
                silver = random.nextInt(6) == 0,
            )
            x += stepX
        }
        y -= stepY
        row++
    }
    // Draw from the top down so lower coins overlap the ones behind and above them.
    return coins.sortedBy { it.y }
}

/** A mound above the mouth, built from the middle outwards, for an overflowing jar. */
private fun spillLayout(): List<Coin> {
    val random = Random(11)
    val rows = listOf(5, 4, 3, 2)
    val coins = mutableListOf<Coin>()
    rows.forEachIndexed { rowIndex, count ->
        val y = NECK_TOP - 0.02f - rowIndex * COIN_RADIUS * 1.3f * JAR_ASPECT
        val order = (0 until count).sortedBy { kotlin.math.abs(it - (count - 1) / 2f) }
        for (i in order) {
            val x = 0.5f + (i - (count - 1) / 2f) * COIN_RADIUS * 1.7f
            coins += Coin(x, y, 0f, silver = random.nextInt(6) == 0)
        }
    }
    return coins
}

private fun DrawScope.drawCoin(center: Offset, radius: Float, silver: Boolean) {
    val face = if (silver) Silver else Gold
    val edge = if (silver) SilverDark else GoldDark
    val shine = if (silver) SilverLight else GoldLight
    drawCircle(face, radius, center)
    drawCircle(edge, radius, center, style = Stroke(width = radius * 0.16f))
    drawCircle(shine, radius * 0.58f, center, style = Stroke(width = radius * 0.12f))
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
