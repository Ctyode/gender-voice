package com.example.gendercolorvoice

import android.content.Context
import org.json.JSONObject

object KaggleCoefficients {
    @Volatile private var cached: Map<String, Double>? = null
    // Fallback map in case asset loading fails
    private val defaultCoeffs: Map<String, Double> = mapOf(
        "meanfun" to -4.927502015869142,
        "sfm" to -1.6280976137646412,
        "IQR" to 1.3425369998020633,
        "sp.ent" to 1.3423335958179192,
        "Q25" to -0.8699599284108105,
        "Q75" to 0.631356047597979,
        "minfun" to 0.5222923943531044,
        "modindx" to -0.34758880626510796,
        "kurt" to -0.33945562102351334,
        "skew" to -0.25278650840735245,
        "sd" to 0.14407405084087058,
        "meanfreq" to -0.12117718824317832,
        "centroid" to -0.12117718824317832,
        "meandom" to 0.08321484733715298,
        "mode" to 0.055914923487978205,
        "median" to -0.046055103089267954,
        "mindom" to -0.044278276019513105,
        "dfrange" to 0.027372976410010404,
        "maxdom" to 0.026568851228527552,
        "maxfun" to 0.017921251755893777,
    )

    fun load(context: Context): Map<String, Double> {
        cached?.let { return it }
        val m = try {
            val txt = context.assets.open("voice_lr_coeffs.json").bufferedReader().use { it.readText() }
            val root = JSONObject(txt)
            val obj = root.getJSONObject("features")
            val map = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getDouble(k)
            }
            if (map.isEmpty()) defaultCoeffs else map
        } catch (_: Throwable) {
            defaultCoeffs
        }
        cached = m
        return m
    }
}
