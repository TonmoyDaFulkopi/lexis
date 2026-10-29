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
 * "ləxis" in Fraunces Bold at 21 units, one path per letter, centred on x = 0 with the baseline
 * at y = 0. Generated from the font so the intro matches the launcher icon exactly.
 */
private val LETTERS = listOf(
    // l
    "M-18.15 -15.23V-1.34Q-18.15 -1.06 -18.08 -0.93Q-18.01 -0.8 -17.86 -0.76L-17.47 -0.65Q-17.31 -0.6 -17.25 -0.52Q-17.18 -0.44 -17.18 -0.32Q-17.18 -0.17 -17.28 -0.09Q-17.38 0 -17.6 0H-22.17Q-22.38 0 -22.48 -0.09Q-22.58 -0.17 -22.58 -0.32Q-22.58 -0.43 -22.51 -0.52Q-22.44 -0.6 -22.29 -0.66L-21.9 -0.76Q-21.74 -0.81 -21.67 -0.93Q-21.6 -1.06 -21.6 -1.33V-13.56Q-21.6 -13.76 -21.67 -13.85Q-21.73 -13.94 -21.86 -13.96L-22.33 -13.99Q-22.47 -14.02 -22.53 -14.09Q-22.59 -14.17 -22.59 -14.28Q-22.59 -14.41 -22.51 -14.49Q-22.43 -14.57 -22.24 -14.64L-19.47 -15.52Q-19.16 -15.64 -18.98 -15.68Q-18.81 -15.73 -18.67 -15.73Q-18.41 -15.73 -18.28 -15.59Q-18.15 -15.45 -18.15 -15.23Z",
    // ə
    "M-15.8 -3.85Q-15.8 -4.25 -15.58 -4.47Q-15.35 -4.69 -14.93 -4.69H-9.33Q-9.49 -6.05 -10.17 -6.79Q-11.07 -7.79 -12.57 -7.79Q-13.53 -7.79 -14.21 -7.37Q-14.89 -6.96 -15.16 -6.18Q-15.29 -6.02 -15.37 -5.97Q-15.46 -5.92 -15.53 -5.92Q-15.65 -5.92 -15.7 -6.03Q-15.76 -6.14 -15.76 -6.31Q-15.74 -7.31 -15.15 -8.14Q-14.55 -8.96 -13.54 -9.45Q-12.52 -9.94 -11.21 -9.94Q-9.74 -9.94 -8.59 -9.33Q-7.45 -8.72 -6.8 -7.62Q-6.14 -6.52 -6.14 -5.06Q-6.14 -3.45 -6.77 -2.26Q-7.39 -1.07 -8.56 -0.41Q-9.74 0.25 -11.35 0.25Q-12.76 0.25 -13.76 -0.28Q-14.75 -0.82 -15.28 -1.74Q-15.8 -2.66 -15.8 -3.85ZM-11.99 -4.08Q-12.38 -4.08 -12.38 -3.7Q-12.38 -2.06 -11.96 -1.29Q-11.53 -0.52 -10.91 -0.52Q-10.44 -0.52 -10.07 -0.86Q-9.69 -1.21 -9.48 -1.92Q-9.27 -2.63 -9.27 -3.75Q-9.27 -3.91 -9.27 -4.08Z",
    // x
    "M0.02 -8.27 1.65 -5.84 2.86 -7.62Q3.2 -8.15 3.11 -8.52Q3.03 -8.88 2.63 -8.98L2.32 -9.05Q2.18 -9.1 2.12 -9.18Q2.05 -9.26 2.05 -9.37Q2.05 -9.53 2.16 -9.62Q2.27 -9.7 2.47 -9.7H5.19Q5.39 -9.7 5.5 -9.61Q5.61 -9.52 5.61 -9.37Q5.61 -9.26 5.54 -9.18Q5.47 -9.09 5.28 -9.01Q4.72 -8.84 4.38 -8.48Q4.05 -8.12 3.52 -7.37L2.06 -5.25L4.56 -1.65Q4.88 -1.2 5.09 -0.98Q5.31 -0.76 5.52 -0.69Q5.66 -0.62 5.73 -0.54Q5.79 -0.46 5.79 -0.35Q5.79 -0.19 5.68 -0.09Q5.56 0 5.35 0H0.19Q-0.03 0 -0.15 -0.09Q-0.27 -0.19 -0.27 -0.36Q-0.27 -0.47 -0.21 -0.55Q-0.15 -0.62 -0.02 -0.66L0.48 -0.75Q0.78 -0.81 0.83 -1.01Q0.88 -1.21 0.62 -1.58L-0.98 -3.99L-2.37 -2Q-2.69 -1.51 -2.62 -1.22Q-2.55 -0.93 -2.15 -0.79L-1.81 -0.67Q-1.66 -0.61 -1.6 -0.54Q-1.53 -0.46 -1.53 -0.35Q-1.53 -0.19 -1.64 -0.1Q-1.75 -0.01 -1.94 -0.01H-4.69Q-4.88 -0.01 -4.99 -0.1Q-5.1 -0.19 -5.1 -0.35Q-5.1 -0.46 -5.02 -0.55Q-4.95 -0.63 -4.77 -0.7Q-4.4 -0.81 -4.15 -0.99Q-3.89 -1.18 -3.63 -1.49Q-3.38 -1.81 -3.03 -2.28L-1.39 -4.59L-3.84 -8.28Q-4.06 -8.61 -4.27 -8.76Q-4.48 -8.9 -4.7 -8.97Q-4.88 -9.02 -4.95 -9.11Q-5.02 -9.21 -5.02 -9.35Q-5.02 -9.52 -4.9 -9.61Q-4.78 -9.7 -4.58 -9.7H0.38Q0.57 -9.7 0.69 -9.61Q0.8 -9.52 0.8 -9.37Q0.8 -9.26 0.76 -9.19Q0.71 -9.11 0.59 -9.05L0.17 -8.94Q-0.06 -8.86 -0.11 -8.7Q-0.16 -8.55 0.02 -8.27Z",
    // i
    "M11.28 -9.44V-1.34Q11.28 -1.06 11.35 -0.93Q11.42 -0.8 11.58 -0.76L11.97 -0.65Q12.12 -0.6 12.19 -0.52Q12.25 -0.44 12.25 -0.32Q12.25 -0.17 12.15 -0.09Q12.05 0 11.83 0H7.27Q7.06 0 6.96 -0.09Q6.86 -0.17 6.86 -0.32Q6.86 -0.43 6.92 -0.52Q6.99 -0.6 7.14 -0.65L7.54 -0.76Q7.7 -0.81 7.77 -0.93Q7.83 -1.06 7.83 -1.33V-7.78Q7.83 -7.97 7.77 -8.06Q7.71 -8.15 7.57 -8.17L7.11 -8.2Q6.97 -8.23 6.91 -8.31Q6.85 -8.38 6.85 -8.49Q6.85 -8.63 6.92 -8.7Q7 -8.78 7.2 -8.85L9.96 -9.74Q10.28 -9.85 10.45 -9.9Q10.62 -9.94 10.76 -9.94Q11.02 -9.94 11.15 -9.8Q11.28 -9.66 11.28 -9.44ZM9.41 -11.07Q8.49 -11.07 7.94 -11.57Q7.39 -12.08 7.39 -12.87Q7.39 -13.66 7.94 -14.16Q8.5 -14.67 9.41 -14.67Q10.33 -14.67 10.89 -14.16Q11.46 -13.66 11.46 -12.87Q11.46 -12.08 10.89 -11.57Q10.33 -11.07 9.41 -11.07Z",
    // s
    "M18.01 -0.49Q18.66 -0.49 19.02 -0.81Q19.37 -1.13 19.37 -1.62Q19.37 -1.95 19.23 -2.23Q19.08 -2.5 18.68 -2.73Q18.27 -2.97 17.51 -3.16Q15.95 -3.55 15.09 -4.07Q14.23 -4.59 13.9 -5.26Q13.57 -5.94 13.57 -6.78Q13.57 -8.28 14.6 -9.12Q15.64 -9.95 17.52 -9.95Q18.23 -9.95 18.67 -9.83Q19.11 -9.71 19.37 -9.59Q19.63 -9.47 19.78 -9.47Q19.96 -9.47 20.18 -9.59Q20.39 -9.71 20.61 -9.83Q20.82 -9.96 21 -9.96Q21.12 -9.96 21.23 -9.88Q21.33 -9.8 21.39 -9.58L21.95 -7.04Q22.04 -6.75 22 -6.57Q21.97 -6.4 21.8 -6.33Q21.63 -6.27 21.5 -6.33Q21.37 -6.39 21.25 -6.57Q20.67 -7.55 20.11 -8.14Q19.54 -8.72 18.95 -8.98Q18.36 -9.24 17.71 -9.24Q16.96 -9.24 16.57 -8.91Q16.19 -8.57 16.19 -8.02Q16.19 -7.66 16.36 -7.37Q16.54 -7.08 17.04 -6.84Q17.53 -6.59 18.46 -6.37Q19.77 -6.06 20.55 -5.57Q21.32 -5.08 21.66 -4.41Q22 -3.75 22 -2.89Q22 -1.94 21.51 -1.23Q21.02 -0.53 20.19 -0.14Q19.35 0.24 18.28 0.24Q17.62 0.24 17.19 0.1Q16.76 -0.04 16.49 -0.18Q16.23 -0.33 16.06 -0.33Q15.89 -0.33 15.67 -0.19Q15.45 -0.05 15.22 0.08Q15 0.22 14.83 0.22Q14.7 0.22 14.6 0.12Q14.5 0.01 14.46 -0.23L14.07 -2.49Q14.02 -2.81 14.06 -2.96Q14.1 -3.11 14.26 -3.18Q14.42 -3.25 14.55 -3.17Q14.68 -3.09 14.81 -2.89Q15.62 -1.59 16.41 -1.04Q17.21 -0.49 18.01 -0.49Z",
)

val SpineInk = Color(0xFF08304F)

/**
 * The Spine logo: a ribbon bookmark with "ləxis" running up it.
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
                // Text reads bottom-to-top: rotate -90° about the spine centre, baseline 7 units right,
                // centred at y = 51.5 so the word clears the notch.
                withTransform({
                    rotate(-90f, pivot = Offset(54f, 54f))
                    translate(56.5f, 61f)
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
