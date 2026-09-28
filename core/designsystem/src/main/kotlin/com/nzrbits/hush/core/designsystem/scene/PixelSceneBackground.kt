package com.nzrbits.hush.core.designsystem.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nzrbits.hush.core.common.model.PixelScene
import com.nzrbits.hush.core.common.model.SceneDensity
import com.nzrbits.hush.core.designsystem.theme.HushColors
import com.nzrbits.hush.core.designsystem.theme.HushPalettes
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated pixel background. A handful of particles (leaves, snow, rain, fireflies, stars,
 * petals) drawn on a coarse pixel grid so they look like sprites, not smooth shapes.
 *
 * Cost control: at most [SceneDensity.particles] particles, redraw capped at 30 fps, the loop
 * only runs while the lifecycle is RESUMED, and nothing runs when [reduceMotion] is set or the
 * scene is NONE. Stars are static and only twinkle, so they do not move at all.
 */
@Composable
fun PixelSceneBackground(
    scene: PixelScene,
    density: SceneDensity,
    colors: HushColors,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    if (scene == PixelScene.NONE) return
    val pixel = with(LocalDensity.current) { 5.dp.toPx() }
    val world = remember(scene, density) { SceneWorld(scene, density.particles, Random(scene.ordinal * 31 + density.ordinal)) }
    var frame by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    if (!reduceMotion) {
        LaunchedEffect(world, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = 0L
                while (true) {
                    val now = withFrameNanos { it }
                    if (last == 0L) { last = now; continue }
                    val dtMs = (now - last) / 1_000_000L
                    if (dtMs < 33) continue
                    last = now
                    world.step(dtMs.coerceAtMost(100).toFloat() / 1000f)
                    frame++
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION") frame
        world.ensureSized(size)
        drawWorld(world, pixel, palette(scene, colors))
    }
}

private fun palette(scene: PixelScene, colors: HushColors): List<Color> {
    if (colors.isMinimal) {
        return when (scene) {
            PixelScene.STARS, PixelScene.FIREFLIES -> listOf(Color(0xFFDADADA), Color(0xFF9A9A9A), Color(0xFF6A6A6A))
            else -> listOf(Color(0xFF5A5A5A), Color(0xFF444444), Color(0xFF777777))
        }
    }
    return when (scene) {
        PixelScene.LEAVES -> listOf(HushPalettes.Terracotta, HushPalettes.Sand, HushPalettes.Brown, HushPalettes.Honey)
        PixelScene.SNOW -> if (colors.isDark) listOf(Color(0xFFFCF3E3), Color(0xFFE9DBC2)) else listOf(Color(0xFFFFFFFF), Color(0xFFE9DBC2))
        PixelScene.RAIN -> listOf(HushPalettes.Sage.copy(alpha = 0.7f), colors.muted.copy(alpha = 0.5f))
        PixelScene.FIREFLIES -> listOf(HushPalettes.Honey, HushPalettes.Peach)
        PixelScene.STARS -> if (colors.isDark) listOf(Color(0xFFFCF3E3), HushPalettes.Honey) else listOf(HushPalettes.Sand, HushPalettes.Honey)
        PixelScene.PETALS -> listOf(HushPalettes.Peach, Color(0xFFF7DED4), HushPalettes.Terracotta.copy(alpha = 0.8f))
        PixelScene.NONE -> emptyList()
    }
}

/** One particle. Positions in px, speeds in px per second, phase for sway and twinkle. */
internal class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var phase: Float,
    var colorIndex: Int,
    var variant: Int,
    var alpha: Float = 1f,
)

internal class SceneWorld(val scene: PixelScene, val count: Int, private val random: Random) {
    val particles = ArrayList<Particle>(count)
    var width = 0f
    var height = 0f
    private var time = 0f

    fun ensureSized(size: Size) {
        if (size.width == width && size.height == height && particles.isNotEmpty()) return
        width = size.width
        height = size.height
        particles.clear()
        repeat(count) { particles += spawn(initial = true) }
    }

    private fun spawn(initial: Boolean): Particle {
        val x = random.nextFloat() * width
        val y = if (initial) random.nextFloat() * height else -20f
        return when (scene) {
            PixelScene.LEAVES -> Particle(x, y, 0f, 18f + random.nextFloat() * 22f, random.nextFloat() * 6.28f, random.nextInt(4), random.nextInt(3))
            PixelScene.PETALS -> Particle(x, y, 0f, 14f + random.nextFloat() * 16f, random.nextFloat() * 6.28f, random.nextInt(3), random.nextInt(2))
            PixelScene.SNOW -> Particle(x, y, 0f, 10f + random.nextFloat() * 14f, random.nextFloat() * 6.28f, random.nextInt(2), random.nextInt(2))
            PixelScene.RAIN -> Particle(x, y, 0f, 220f + random.nextFloat() * 120f, 0f, random.nextInt(2), 0)
            PixelScene.FIREFLIES -> Particle(
                x, if (initial) y else random.nextFloat() * height,
                (random.nextFloat() - 0.5f) * 14f, (random.nextFloat() - 0.5f) * 10f,
                random.nextFloat() * 6.28f, random.nextInt(2), 0,
            )
            PixelScene.STARS -> Particle(x, random.nextFloat() * height * 0.8f, 0f, 0f, random.nextFloat() * 6.28f, random.nextInt(2), random.nextInt(2))
            PixelScene.NONE -> Particle(x, y, 0f, 0f, 0f, 0, 0)
        }
    }

    fun step(dt: Float) {
        time += dt
        if (width == 0f) return
        for (i in particles.indices) {
            val p = particles[i]
            when (scene) {
                PixelScene.LEAVES, PixelScene.PETALS -> {
                    p.y += p.vy * dt
                    p.x += sin(time * 0.9f + p.phase) * 14f * dt
                    if (p.y > height + 20f) particles[i] = spawn(initial = false)
                }
                PixelScene.SNOW -> {
                    p.y += p.vy * dt
                    p.x += (sin(time * 0.5f + p.phase) * 8f + 6f) * dt
                    if (p.y > height + 20f || p.x > width + 20f) particles[i] = spawn(initial = false)
                }
                PixelScene.RAIN -> {
                    p.y += p.vy * dt
                    if (p.y > height + 20f) particles[i] = spawn(initial = false)
                }
                PixelScene.FIREFLIES -> {
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.alpha = (0.35f + 0.65f * ((sin(time * 1.7f + p.phase) + 1f) / 2f))
                    if (p.x < -10f || p.x > width + 10f || p.y < -10f || p.y > height + 10f) particles[i] = spawn(initial = false)
                }
                PixelScene.STARS -> {
                    p.alpha = 0.3f + 0.7f * ((sin(time * 0.8f + p.phase) + 1f) / 2f)
                }
                PixelScene.NONE -> Unit
            }
        }
    }
}

private fun DrawScope.drawWorld(world: SceneWorld, px: Float, palette: List<Color>) {
    if (palette.isEmpty()) return
    for (p in world.particles) {
        val color = palette[p.colorIndex % palette.size]
        val gx = floor(p.x / px) * px
        val gy = floor(p.y / px) * px
        when (world.scene) {
            PixelScene.LEAVES -> drawLeaf(gx, gy, px, color, p.variant)
            PixelScene.PETALS -> {
                pixel(gx, gy, px, color)
                pixel(gx + px, gy, px, color.copy(alpha = 0.7f))
            }
            PixelScene.SNOW -> {
                pixel(gx, gy, px, color)
                if (p.variant == 1) pixel(gx, gy + px, px, color.copy(alpha = 0.6f))
            }
            PixelScene.RAIN -> {
                pixel(gx, gy, px * 0.6f, color, tall = 3f)
            }
            PixelScene.FIREFLIES -> {
                pixel(gx, gy, px, color.copy(alpha = p.alpha))
                pixel(gx - px, gy, px, color.copy(alpha = p.alpha * 0.25f))
                pixel(gx + px, gy, px, color.copy(alpha = p.alpha * 0.25f))
                pixel(gx, gy - px, px, color.copy(alpha = p.alpha * 0.25f))
                pixel(gx, gy + px, px, color.copy(alpha = p.alpha * 0.25f))
            }
            PixelScene.STARS -> {
                pixel(gx, gy, px, color.copy(alpha = p.alpha))
                if (p.variant == 1) {
                    pixel(gx - px, gy, px, color.copy(alpha = p.alpha * 0.4f))
                    pixel(gx + px, gy, px, color.copy(alpha = p.alpha * 0.4f))
                }
            }
            PixelScene.NONE -> Unit
        }
    }
}

private fun DrawScope.pixel(x: Float, y: Float, px: Float, color: Color, tall: Float = 1f) {
    drawRect(color = color, topLeft = Offset(x, y), size = Size(px, px * tall))
}

/** Three leaf shapes made of 3 to 4 pixels, so they read as leaves at a glance. */
private fun DrawScope.drawLeaf(x: Float, y: Float, px: Float, color: Color, variant: Int) {
    when (variant) {
        0 -> { pixel(x, y, px, color); pixel(x + px, y, px, color); pixel(x + px, y + px, px, color) }
        1 -> { pixel(x, y + px, px, color); pixel(x + px, y, px, color); pixel(x + px, y + px, px, color); pixel(x + 2 * px, y, px, color.copy(alpha = 0.7f)) }
        else -> { pixel(x, y, px, color); pixel(x + px, y + px, px, color); pixel(x + 2 * px, y + 2 * px, px, color.copy(alpha = 0.8f)) }
    }
}
