package com.example.gendercolorvoice

import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Builds a lightweight live LR score from available window features and
 * the user-provided Kaggle coefficients. Only overlapping features are used.
 * The result is mapped to x in [0..1] via a centered clip like G/clip.
 */
object KaggleLiveScorer {
    data class Result(val g: Float, val x01: Float)

    private fun clamp01(x: Float) = when {
        x < 0f -> 0f
        x > 1f -> 1f
        else -> x
    }

    @Suppress("UNUSED_PARAMETER")
    fun scoreToX(
        unusedContext: android.content.Context,
        coeffs: Map<String, Double>,
        wf: WindowFeatures,
        unusedResCfg: ResonanceAxisCfg? = null
    ): Result {
        // Prepare live feature values roughly aligned with Kaggle feature names
        // meanfun: mean F0 (kHz) over the window
        val f0k = if (wf.f0Valid.isNotEmpty()) (wf.f0Valid.average().toFloat() / 1000f).toDouble() else 0.0
        // minfun/maxfun: min/max F0 (kHz)
        val minfun = if (wf.f0Valid.isNotEmpty()) (wf.f0Valid.minOrNull()!! / 1000f).toDouble() else 0.0
        val maxfun = if (wf.f0Valid.isNotEmpty()) (wf.f0Valid.maxOrNull()!! / 1000f).toDouble() else 0.0
        // median/mode approximations from available track (mode≈median for our purposes)
        val median = if (wf.f0Valid.isNotEmpty()) wf.f0Valid.sorted()[wf.f0Valid.size/2] / 1000f else 0f
        val mode = median
        val meandom = f0k // proxy
        val mindom = minfun
        val maxdom = maxfun
        val dfrange = (maxdom - mindom)

        // centroid/meanfreq: use spectral centroid in kHz to match voice.csv scale (~0.15..0.25)
        val centroidK = (wf.scHz / 1000f).toDouble()

        // sfm, sp.ent already in [0..1]
        val sfm = (wf.sfm ?: 0f).toDouble()
        val spent = (wf.spEnt ?: 0f).toDouble()

        // Assemble dot product only for keys we have a value for
        fun vRaw(name: String): Double? = when (name) {
            "meanfun" -> f0k
            "minfun" -> minfun
            "maxfun" -> maxfun
            "median" -> median.toDouble()
            "mode" -> mode.toDouble()
            "meandom" -> meandom
            "mindom" -> mindom
            "maxdom" -> maxdom
            "dfrange" -> dfrange
            "centroid", "meanfreq" -> centroidK
            "sfm" -> sfm
            "sp.ent" -> spent
            // Not available live with current pipeline: Q25,Q75,IQR,kurt,skew,sd,modindx
            else -> null
        }

        // Use raw feature scales matching voice.csv (kHz for frequency features, 0..1 for descriptors)
        fun v(name: String): Double? = vRaw(name)

        var g = 0.0
        var wsum = 0.0
        for ((k, w) in coeffs) {
            val x = v(k)
            if (x != null && x.isFinite()) {
                g += w * x
                wsum += kotlin.math.abs(w)
            }
        }
        // Map G -> x via logistic; center at 0.5
        val xLog = (1.0 / (1.0 + kotlin.math.exp(-g))).toFloat()
        return Result(g.toFloat(), clamp01(xLog))
    }
}
