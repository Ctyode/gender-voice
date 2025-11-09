package com.example.gendercolorvoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrMappingTest {
    @Test
    fun calibrate_clamps_and_expands_around_mid() {
        // Around 0.5, scale expands distance
        val x = 0.60f
        val y = LrMapping.calibrate(x, scale = 2.0f, bias = 0.0f)
        // 0.5 + 2*(0.1) = 0.7
        assertEquals(0.7f, y, 1e-3f)

        // Extreme scale clamps to 1
        val y2 = LrMapping.calibrate(0.9f, scale = 30f, bias = 0.0f)
        assertEquals(1.0f, y2, 1e-6f)

        // Extreme negative side clamps to 0
        val y3 = LrMapping.calibrate(0.1f, scale = 30f, bias = 0.0f)
        assertEquals(0.0f, y3, 1e-6f)
    }

    @Test
    fun mapToAnchors_respects_predicts_flag_and_span() {
        val maleX = 0.27f
        val femaleX = 0.72f
        // If LR predicts male, probability is P(male) and we invert
        val pMale = 0.35f
        val xForMalePredicts = LrMapping.mapToAnchors(pMale, "male", maleX, femaleX)
        // 1-0.35=0.65 -> 0.27 + 0.65*(0.45) = 0.5625
        assertEquals(0.5625f, xForMalePredicts, 1e-3f)

        // If LR predicts female, direct mapping
        val pFemale = 0.35f
        val xForFemalePredicts = LrMapping.mapToAnchors(pFemale, "female", maleX, femaleX)
        // 0.27 + 0.35*0.45 = 0.4275
        assertEquals(0.4275f, xForFemalePredicts, 1e-3f)

        // Monotonicity: higher prob leads to higher x for predicts=female
        val xLow = LrMapping.mapToAnchors(0.2f, "female", maleX, femaleX)
        val xHigh = LrMapping.mapToAnchors(0.8f, "female", maleX, femaleX)
        assertTrue(xHigh > xLow)
    }

    @Test
    fun applyGain_expands_around_mid_and_clamps() {
        val x = 0.6f
        val y = LrMapping.applyGain(x, gain = 2.0f)
        // 0.5 + 2*(0.1) = 0.7
        assertEquals(0.7f, y, 1e-3f)
        val y2 = LrMapping.applyGain(0.99f, gain = 5f)
        assertEquals(1.0f, y2, 1e-6f)
    }

    @Test
    fun end_to_end_example_matches_expectation() {
        val maleX = 0.27f
        val femaleX = 0.72f
        // Example from your case: x_lr≈0.35, large scale
        val xlr = 0.35f
        val cal = LrMapping.calibrate(xlr, scale = 30f, bias = 0.0f) // -> clamps near 0
        // With predicts=male we invert after calibration mapping
        val mapped = LrMapping.mapToAnchors(cal, "male", maleX, femaleX)
        // cal≈0 -> invert -> 1 -> near femaleX
        assertEquals(femaleX, mapped, 1e-3f)
        // Apply gain to allow going even further toward 1 if anchors are full span
        val gained = LrMapping.applyGain(mapped, 1.95f)
        assertTrue(gained >= mapped)
    }
}

