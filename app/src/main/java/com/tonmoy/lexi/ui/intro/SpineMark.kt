package com.tonmoy.lexi.ui.intro

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/** Same geometry as the launcher icon, on its 108-unit grid. */
private const val BOOKMARK = "M42,20h24a4,4 0 0,1 4,4v64l-16,-11 -16,11V24a4,4 0 0,1 4,-4z"

/**
 * "ləxy" in Fraunces Bold at 21 units, one path per letter, centred on x = 0 with the baseline
 * at y = 0. Generated from the font so the intro matches the launcher icon exactly.
 */
private val LETTERS = listOf(
    // l
    "M-14.75 -15.23V-1.34Q-14.75 -1.06 -14.68 -0.93Q-14.61 -0.8 -14.46 -0.76L-14.06 -0.65Q-13.91 -0.6 -13.85 -0.52Q-13.78 -0.44 -13.78 -0.32Q-13.78 -0.17 -13.88 -0.09Q-13.98 0 -14.2 0H-18.76Q-18.97 0 -19.07 -0.09Q-19.18 -0.17 -19.18 -0.32Q-19.18 -0.43 -19.11 -0.52Q-19.04 -0.6 -18.89 -0.66L-18.49 -0.76Q-18.33 -0.81 -18.27 -0.93Q-18.2 -1.06 -18.2 -1.33V-13.56Q-18.2 -13.76 -18.26 -13.85Q-18.33 -13.94 -18.46 -13.96L-18.93 -13.99Q-19.07 -14.02 -19.13 -14.09Q-19.19 -14.17 -19.19 -14.28Q-19.19 -14.41 -19.11 -14.49Q-19.03 -14.57 -18.84 -14.64L-16.07 -15.52Q-15.75 -15.64 -15.58 -15.68Q-15.41 -15.73 -15.27 -15.73Q-15.01 -15.73 -14.88 -15.59Q-14.75 -15.45 -14.75 -15.23Z",
    // ə
    "M-12.82 -3.86Q-12.82 -4.25 -12.6 -4.47Q-12.38 -4.69 -11.95 -4.69H-5.49V-4.08H-9.01Q-9.4 -4.08 -9.4 -3.7Q-9.4 -2.05 -8.97 -1.29Q-8.55 -0.53 -7.93 -0.53Q-7.45 -0.53 -7.08 -0.87Q-6.71 -1.21 -6.5 -1.92Q-6.29 -2.64 -6.29 -3.74Q-6.29 -5.8 -7.19 -6.8Q-8.09 -7.79 -9.58 -7.79Q-10.54 -7.79 -11.22 -7.37Q-11.91 -6.96 -12.18 -6.18Q-12.31 -6.03 -12.39 -5.98Q-12.48 -5.92 -12.55 -5.92Q-12.67 -5.92 -12.73 -6.03Q-12.79 -6.14 -12.78 -6.3Q-12.76 -7.31 -12.17 -8.14Q-11.57 -8.97 -10.55 -9.46Q-9.54 -9.95 -8.22 -9.95Q-6.75 -9.95 -5.61 -9.33Q-4.47 -8.72 -3.81 -7.62Q-3.16 -6.52 -3.16 -5.06Q-3.16 -3.46 -3.79 -2.27Q-4.41 -1.07 -5.59 -0.41Q-6.76 0.25 -8.37 0.25Q-9.78 0.25 -10.78 -0.28Q-11.77 -0.81 -12.3 -1.74Q-12.82 -2.67 -12.82 -3.86Z",
    // x
    "M1.5 -5.06 2.11 -4.76 0.2 -2Q-0.13 -1.51 -0.06 -1.22Q0.02 -0.93 0.41 -0.78L0.76 -0.67Q0.9 -0.61 0.97 -0.54Q1.04 -0.46 1.04 -0.34Q1.04 -0.19 0.93 -0.1Q0.81 -0.01 0.62 -0.01H-2.12Q-2.31 -0.01 -2.42 -0.1Q-2.54 -0.19 -2.54 -0.34Q-2.54 -0.46 -2.46 -0.54Q-2.39 -0.63 -2.21 -0.71Q-1.84 -0.81 -1.59 -1Q-1.33 -1.18 -1.07 -1.49Q-0.82 -1.8 -0.47 -2.28ZM2.58 -8.28 4.49 -5.42 4.59 -5.3 7.12 -1.64Q7.44 -1.19 7.66 -0.98Q7.87 -0.76 8.08 -0.69Q8.23 -0.62 8.29 -0.54Q8.35 -0.46 8.35 -0.34Q8.35 -0.18 8.23 -0.09Q8.12 0 7.91 0H2.75Q2.54 0 2.42 -0.09Q2.3 -0.19 2.3 -0.35Q2.3 -0.47 2.35 -0.55Q2.41 -0.62 2.55 -0.66L3.04 -0.75Q3.34 -0.81 3.39 -1.01Q3.44 -1.2 3.19 -1.58L1.34 -4.35L1.24 -4.48L-1.28 -8.29Q-1.49 -8.61 -1.71 -8.76Q-1.92 -8.9 -2.14 -8.96Q-2.32 -9.02 -2.39 -9.11Q-2.47 -9.21 -2.47 -9.35Q-2.47 -9.52 -2.34 -9.61Q-2.22 -9.7 -2.02 -9.7H2.95Q3.14 -9.7 3.25 -9.61Q3.36 -9.53 3.36 -9.37Q3.36 -9.26 3.32 -9.19Q3.27 -9.11 3.15 -9.06L2.73 -8.94Q2.5 -8.86 2.45 -8.7Q2.4 -8.54 2.58 -8.28ZM4.25 -4.69 3.66 -5.02 5.42 -7.62Q5.76 -8.15 5.67 -8.52Q5.59 -8.89 5.19 -8.98L4.88 -9.06Q4.74 -9.11 4.68 -9.19Q4.61 -9.26 4.61 -9.37Q4.61 -9.53 4.73 -9.62Q4.84 -9.7 5.03 -9.7H7.76Q7.95 -9.7 8.06 -9.61Q8.17 -9.53 8.17 -9.37Q8.17 -9.26 8.1 -9.18Q8.03 -9.1 7.84 -9.01Q7.28 -8.85 6.94 -8.48Q6.61 -8.11 6.09 -7.37Z",
    // y
    "M15.8 -2.26 14.02 1.42 9.83 -7.86Q9.6 -8.39 9.35 -8.67Q9.11 -8.94 8.75 -9.04Q8.58 -9.1 8.52 -9.18Q8.46 -9.26 8.46 -9.38Q8.46 -9.53 8.57 -9.62Q8.68 -9.7 8.86 -9.7H14.08Q14.26 -9.7 14.37 -9.62Q14.48 -9.53 14.48 -9.38Q14.48 -9.25 14.41 -9.18Q14.34 -9.1 14.17 -9.06L13.7 -8.97Q13.34 -8.89 13.28 -8.64Q13.21 -8.39 13.44 -7.85ZM13.79 1.46 14.41 -0.08 14.73 -0.75 17.42 -7.66Q17.65 -8.25 17.55 -8.53Q17.45 -8.81 17.03 -8.92L16.57 -9.04Q16.41 -9.09 16.33 -9.17Q16.26 -9.25 16.26 -9.38Q16.26 -9.53 16.36 -9.62Q16.47 -9.7 16.65 -9.7H19.44Q19.63 -9.7 19.73 -9.62Q19.84 -9.53 19.84 -9.38Q19.84 -9.27 19.77 -9.19Q19.71 -9.11 19.55 -9.04Q19.04 -8.9 18.77 -8.63Q18.49 -8.36 18.26 -7.75L14.68 1.34Q14.12 2.74 13.69 3.55Q13.26 4.36 12.73 4.7Q12.2 5.04 11.35 5.04Q10.18 5.04 9.49 4.38Q8.79 3.72 8.79 2.77Q8.79 2.06 9.15 1.66Q9.52 1.26 10.09 1.26Q10.63 1.26 10.94 1.58Q11.26 1.91 11.47 2.48L11.64 2.95Q11.75 3.3 11.93 3.54Q12.11 3.78 12.35 3.78Q12.57 3.78 12.76 3.56Q12.96 3.33 13.2 2.82Q13.45 2.31 13.79 1.46Z",
)

val SpineInk = Color(0xFF08304F)

/**
 * The Spine logo: a ribbon bookmark with "ləxy" running up it.
 * [drop] slides the ribbon in from above (-1 = off the top, 0 = resting); [letter] gives each
 * letter's 0 → 1 entrance progress, from the bottom of the spine up. Both are read only while
 * drawing, so animating them redraws the mark without recomposing anything.
 */
@Composable
fun SpineMark(drop: () -> Float, letter: (Int) -> Float, modifier: Modifier = Modifier) {
    val bookmark = remember { PathParser().parsePathString(BOOKMARK).toPath() }
    val glyphs = remember { LETTERS.map { PathParser().parsePathString(it).toPath() } }
    val centers = remember { glyphs.map { it.getBounds().center } }

    Canvas(modifier) {
        // The bookmark spans 38..70 × 20..88; frame 18..90 around it.
        val unit = size.minDimension / 72f
        scale(unit, pivot = Offset.Zero) {
            translate(-18f, -18f + drop() * 80f) {
                drawPath(bookmark, Color.White)
                // Text reads bottom-to-top: rotate -90° about the spine centre, baseline 7 units right.
                withTransform({
                    rotate(-90f, pivot = Offset(54f, 54f))
                    translate(54f, 61f)
                }) {
                    glyphs.forEachIndexed { i, glyph ->
                        val p = letter(i)
                        if (p <= 0f) return@forEachIndexed
                        val c = centers[i]
                        withTransform({
                            // Rise up from below the baseline while growing in.
                            translate(0f, (1f - p) * 10f)
                            scale(0.5f + 0.5f * p, pivot = c)
                        }) {
                            drawPath(glyph, SpineInk.copy(alpha = p.coerceIn(0f, 1f)))
                        }
                    }
                }
            }
        }
    }
}
