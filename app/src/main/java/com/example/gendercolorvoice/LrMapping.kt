package com.example.gendercolorvoice

object LrMapping {
    fun clamp01(x: Float): Float = when {
        x < 0f -> 0f
        x > 1f -> 1f
        else -> x
    }

    // x in [0,1] as LR probability; returns calibrated and clamped [0,1]
    fun calibrate(x: Float, scale: Float, bias: Float): Float {
        val y = 0.5f + scale * (x - 0.5f) + bias
        return clamp01(y)
    }

    // Map probability to anchors span with optional inversion depending on what class LR predicts
    // predicts: "male" means x is P(male), so we use (1-x) as female probability
    fun mapToAnchors(xProb: Float, predicts: String, maleX: Float, femaleX: Float): Float {
        val p = if (predicts.equals("male", ignoreCase = true)) (1f - xProb) else xProb
        val span = (femaleX - maleX)
        return clamp01(maleX + p * span)
    }

    // Apply global X gain around 0.5 and clamp
    fun applyGain(x: Float, gain: Float): Float = clamp01(0.5f + gain * (x - 0.5f))
}

