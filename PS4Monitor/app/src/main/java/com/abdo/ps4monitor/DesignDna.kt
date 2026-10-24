package com.abdo.ps4monitor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Stable, serialisable design vocabulary derived from a theme and optional artwork analysis. */
@Immutable
data class DesignDna(
    val schemaVersion: Int = 1,
    val identity: String = "default",
    val confidence: Float = 1f,
    val atmosphere: Atmosphere = Atmosphere(),
    val geometry: Geometry = Geometry(),
    val ornament: Ornament = Ornament(),
    val motion: Motion = Motion(),
    val accessibility: Accessibility = Accessibility(),
)

@Immutable
data class Atmosphere(
    val background: Color = Color(0xFF0A1022),
    val glow: Color = Color(0xFF7C6BFF),
    val secondaryGlow: Color = Color(0xFF4FC3F7),
    val intensity: Float = .16f,
    val grain: Float = .035f,
)

@Immutable
data class Geometry(val cornerRadius: Float = 18f, val borderWidth: Float = 1f, val angularity: Float = .25f)
@Immutable
data class Ornament(val kind: OrnamentKind = OrnamentKind.Arcs, val density: Float = .35f, val seed: Int = 17)
@Immutable
data class Motion(val enabled: Boolean = true, val durationMs: Int = 700)
@Immutable
data class Accessibility(val minTextContrast: Double = 4.5, val largeTextContrast: Double = 3.0)
enum class OrnamentKind { Arcs, Diagonals, Waves, Dots }

object DesignDnaFactory {
    fun from(theme: AppTheme, analysis: CoverAnalysis? = null): DesignDna {
        val a = analysis
        val h = ColorMath.toHsl(theme.accent)[0]
        val kind = when {
            a != null && a.edgeDensity > .56f -> OrnamentKind.Diagonals
            a != null && a.warmBalance > .62f -> OrnamentKind.Waves
            a != null && a.negativeSpace > .42f -> OrnamentKind.Arcs
            else -> OrnamentKind.Dots
        }
        return DesignDna(
            identity = theme.id,
            confidence = (a?.confidence ?: 1f).coerceIn(0f, 1f),
            atmosphere = Atmosphere(Color(theme.bg), Color(theme.accent), Color(theme.accent2),
                intensity = (.10f + (a?.contrast ?: .18f) * .22f).coerceIn(.08f, .28f),
                grain = if ((a?.edgeDensity ?: 0f) > .65f) .018f else .032f),
            geometry = Geometry(cornerRadius = (12f + (1f - (a?.edgeDensity ?: .3f)) * 12f).coerceIn(12f, 24f),
                angularity = ((a?.edgeDensity ?: .3f) * .9f).coerceIn(.1f, .85f)),
            ornament = Ornament(kind, density = (.18f + (a?.complexity ?: .3f) * .42f).coerceIn(.12f, .5f), seed = theme.id.hashCode() xor h.toInt()),
        )
    }
}

/** A restrained, deterministic atmosphere layer. It is drawn once per surface and does not affect hit testing. */
@Composable
fun DnaBackdrop(dna: DesignDna, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().background(dna.atmosphere.background)) {
            val alpha = dna.atmosphere.intensity
            drawRect(Brush.radialGradient(listOf(dna.atmosphere.glow.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width * .18f, size.height * .05f), radius = maxOf(size.width, size.height) * .9f))
            drawRect(Brush.linearGradient(listOf(Color.Transparent, dna.atmosphere.secondaryGlow.copy(alpha = alpha * .55f)),
                start = Offset.Zero, end = Offset(size.width, size.height)))
            val count = (dna.ornament.density * 10f).toInt().coerceIn(1, 6)
            repeat(count) { i ->
                val phase = (dna.ornament.seed + i * 31) * .017f
                val y = size.height * (.16f + i * .14f)
                when (dna.ornament.kind) {
                    OrnamentKind.Diagonals -> drawLine(dna.atmosphere.glow.copy(alpha = .045f), Offset(-40f, y), Offset(size.width * .72f, y - size.width * .25f), 1.5f)
                    OrnamentKind.Waves -> {
                        val p = Path(); for (x in 0..12) { val xx = size.width * x / 12f; val yy = y + sin(phase + x * .75f) * 10f; if (x == 0) p.moveTo(xx, yy) else p.lineTo(xx, yy) }
                        drawPath(p, dna.atmosphere.secondaryGlow.copy(alpha = .035f), style = Stroke(2.dp.toPx()))
                    }
                    OrnamentKind.Dots -> drawCircle(dna.atmosphere.secondaryGlow.copy(alpha = .045f), 2.5f, Offset(size.width * (.12f + i * .17f), y))
                    OrnamentKind.Arcs -> drawArc(dna.atmosphere.glow.copy(alpha = .05f), 180f + i * 7f, 70f, false, Offset(size.width * .55f, y - 80f), androidx.compose.ui.geometry.Size(size.width * .8f, 160f), style = Stroke(1.5f))
                }
            }
        }
        content()
    }
}
