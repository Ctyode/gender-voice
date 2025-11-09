package com.example.gendercolorvoice

import android.content.Context
import org.json.JSONObject

/**
 * Loads per-feature mean/std from voice.json if present.
 */
object VoiceFeatureStats {
    data class Stat(val mean: Double, val std: Double)
    @Volatile private var cache: Map<String, Stat>? = null

    fun load(context: Context): Map<String, Stat> {
        cache?.let { return it }
        val m = try {
            val txt = context.assets.open("voice.json").bufferedReader().use { it.readText() }
            val o = JSONObject(txt).optJSONObject("features_summary") ?: JSONObject()
            val map = mutableMapOf<String, Stat>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = o.optJSONObject(k) ?: continue
                // Use pooled std as average of female/male std for robustness
                val f = obj.optJSONObject("female")
                val mObj = obj.optJSONObject("male")
                if (f != null && mObj != null && f.has("mean") && f.has("std") && mObj.has("mean") && mObj.has("std")) {
                    val mean = 0.5 * (f.getDouble("mean") + mObj.getDouble("mean"))
                    val std = 0.5 * (f.getDouble("std") + mObj.getDouble("std"))
                    map[k] = Stat(mean, std)
                }
            }
            map
        } catch (_: Throwable) { emptyMap() }
        cache = m
        return m
    }
}

