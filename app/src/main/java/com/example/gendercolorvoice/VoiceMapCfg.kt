package com.example.gendercolorvoice

import android.content.Context
import org.json.JSONObject

data class VoiceMapCfg(
    val maleX: Float,
    val femaleX: Float,
    val vbW: Float,
    val vbH: Float
) {
    companion object {
        fun load(context: Context): VoiceMapCfg? {
            return try {
            // Prefer data-driven anchors if present; fallback to legacy voice_map.json
            val fileOrder = listOf("voice_map_data_driven.json", "voice_map.json")
            var o: JSONObject? = null
            for (fn in fileOrder) {
                try {
                    val txt = context.assets.open(fn).bufferedReader().use { it.readText() }
                    o = JSONObject(txt)
                    break
                } catch (_: Throwable) { /* try next */ }
            }
            if (o == null) return null
            val svg = o.getJSONObject("svg")
            val vb = svg.getJSONObject("viewBox")
            val w = vb.getDouble("w").toFloat()
            val h = vb.getDouble("h").toFloat()
            val pts = svg.getJSONObject("points")
            val fem = pts.getJSONObject("female_avg")
            val mal = pts.getJSONObject("male_avg")
            // If normalized coordinates are present, use them; else compute from px
            var fx = when {
                fem.has("x_norm") -> fem.getDouble("x_norm").toFloat()
                fem.has("x") -> fem.getDouble("x").toFloat() / w
                else -> 0.7f // sensible default
            }
            var mx = when {
                mal.has("x_norm") -> mal.getDouble("x_norm").toFloat()
                mal.has("x") -> mal.getDouble("x").toFloat() / w
                else -> 0.3f
            }
            var span = kotlin.math.abs(fx - mx)
            // Safety: if span is suspiciously small (<0.2), fallback to svg_mapping_info.json anchors
            if (span < 0.2f) {
                try {
                    val mapTxt = context.assets.open("svg_mapping_info.json").bufferedReader().use { it.readText() }
                    val m = JSONObject(mapTxt)
                    val rec = m.optJSONObject("recommended_centers_for_voice_csv")
                    if (rec != null) {
                        val f = rec.getJSONObject("female").getJSONObject("norm")
                        val male = rec.getJSONObject("male").getJSONObject("norm")
                        fx = f.getDouble("x").toFloat()
                        mx = male.getDouble("x").toFloat()
                        // span recomputed only for guard; value not used further
                        span = kotlin.math.abs(fx - mx)
                    }
                } catch (_: Throwable) {}
            }
            VoiceMapCfg(maleX = mx.coerceIn(0f,1f), femaleX = fx.coerceIn(0f,1f), vbW = w, vbH = h)
            } catch (_: Throwable) { null }
        }
    }
}
