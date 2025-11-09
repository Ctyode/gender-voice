package com.example.gendercolorvoice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SchemeMappingTest {
    private data class Anchors(val maleX: Float, val maleY: Float, val femaleX: Float, val femaleY: Float, val rx: Float, val ry: Float)

    private fun readAnchors(): Anchors {
        fun locate(path: String): File {
            val candidates = listOf(
                // When Gradle test runs from module dir
                File("src/main/assets/$path"),
                // When run from root dir
                File("app/src/main/assets/$path"),
                // Fallback: absolute-ish from working dir
                File(path)
            )
            return candidates.firstOrNull { it.exists() }
                ?: throw java.io.FileNotFoundException("Cannot find asset: $path; tried ${candidates.joinToString()}")
        }
        val txt = locate("svg_mapping_info.json").readText()
        val o = JSONObject(txt)
        // Prefer recommended_centers, fallback to anchors_svg_as_is
        val femNorm: JSONObject
        val maleNorm: JSONObject
        if (o.has("recommended_centers_for_voice_csv")) {
            val rec = o.getJSONObject("recommended_centers_for_voice_csv")
            femNorm = rec.getJSONObject("female").getJSONObject("norm")
            maleNorm = rec.getJSONObject("male").getJSONObject("norm")
        } else {
            val anc = o.getJSONObject("anchors_svg_as_is")
            femNorm = anc.getJSONObject("female").getJSONObject("norm")
            maleNorm = anc.getJSONObject("male").getJSONObject("norm")
        }
        // Radii can be provided normalized or in px; support both, else use sane defaults
        val (rx, ry) = if (o.has("ellipses_norm_radii")) {
            val rad = o.getJSONObject("ellipses_norm_radii")
            rad.getDouble("rx").toFloat() to rad.getDouble("ry").toFloat()
        } else if (o.has("ellipse_radii_px") && o.has("svg_space")) {
            val radPx = o.getJSONObject("ellipse_radii_px")
            val svg = o.getJSONObject("svg_space")
            val w = svg.getDouble("width").toFloat().coerceAtLeast(1f)
            val h = svg.getDouble("height").toFloat().coerceAtLeast(1f)
            (radPx.getDouble("rx").toFloat() / w) to (radPx.getDouble("ry").toFloat() / h)
        } else 0.18f to 0.18f
        return Anchors(
            maleX = maleNorm.getDouble("x").toFloat(),
            maleY = maleNorm.getDouble("y").toFloat(),
            femaleX = femNorm.getDouble("x").toFloat(),
            femaleY = femNorm.getDouble("y").toFloat(),
            rx = rx,
            ry = ry
        )
    }

    @Test
    fun lr_probability_maps_between_anchors_and_inverts_for_male() {
        val a = readAnchors()
        // Example: LR returns 0.35 with predicts=male => closer to femaleX after inversion
        val x = LrMapping.mapToAnchors(0.35f, "male", a.maleX, a.femaleX)
        assertTrue("x should be between anchors", x in 0f..1f && x >= a.maleX && x <= a.femaleX)
        val dF = kotlin.math.abs(x - a.femaleX)
        val dM = kotlin.math.abs(x - a.maleX)
        assertTrue("mapped x should be closer to female anchor", dF < dM)
    }

    @Test
    fun pitch_mapping_is_monotonic_and_respects_range() {
        // Use 80..300 Hz as in svg mapping
        PitchToGender.setRange(80f, 300f)
        val y1 = PitchToGender.scoreFromF0(100f)
        val y2 = PitchToGender.scoreFromF0(200f)
        val y3 = PitchToGender.scoreFromF0(280f)
        assertTrue(y1 in 0f..1f && y2 in 0f..1f && y3 in 0f..1f)
        assertTrue("Higher F0 should yield higher Y", y1 < y2 && y2 < y3)
        // Boundaries clamp
        assertEquals(0f, PitchToGender.scoreFromF0(80f), 1e-6f)
        assertEquals(1f, PitchToGender.scoreFromF0(300f), 1e-6f)
    }

    @Test
    fun ellipse_proximity_classification_aligns_with_scheme() {
        val a = readAnchors()
        // Construct points near each anchor and compare normalized squared distances
        fun d2(x: Float, y: Float, cx: Float, cy: Float) : Float {
            val dx = (x - cx) / a.rx
            val dy = (y - cy) / a.ry
            return dx*dx + dy*dy
        }
        // Female-like sample
        val xF = LrMapping.mapToAnchors(0.8f, "female", a.maleX, a.femaleX)
        val yF = 0.8f // high pitch
        val d2F_to_F = d2(xF, yF, a.femaleX, a.femaleY)
        val d2F_to_M = d2(xF, yF, a.maleX, a.maleY)
        assertTrue("female-like point closer to female ellipse", d2F_to_F < d2F_to_M)

        // Male-like sample
        val xM = LrMapping.mapToAnchors(0.8f, "male", a.maleX, a.femaleX) // inverts → near male
        val yM = 0.25f // low pitch
        val d2M_to_M = d2(xM, yM, a.maleX, a.maleY)
        val d2M_to_F = d2(xM, yM, a.femaleX, a.femaleY)
        assertTrue("male-like point closer to male ellipse", d2M_to_M < d2M_to_F)
    }
}
