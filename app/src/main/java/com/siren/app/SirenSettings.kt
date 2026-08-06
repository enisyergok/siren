package com.siren.app

import android.content.Context
import android.content.SharedPreferences

enum class MapStyle(val label: String, val key: String) {
    OSM("OpenStreetMap", "osm"),
    CARTO("Carto Voyager", "carto"),
    SATELLITE("ESRI Uydu", "esri")
}

enum class SpeedUnit(val label: String, val key: String, val factor: Double) {
    KNOTS("Knot", "kts", 1.94384),
    KMH("km/h", "kmh", 3.6)
}

enum class TrackColor(val label: String, val hex: String, val key: String) {
    YELLOW("Sari", "#F2C94C", "yellow"),
    BLUE("Mavi", "#38BDF8", "blue"),
    RED("Kirmizi", "#E5484D", "red"),
    GREEN("Yesil", "#2ECC71", "green")
}

class SirenSettings(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("siren_prefs", Context.MODE_PRIVATE)

    var mapStyle: MapStyle
        get() = MapStyle.values().firstOrNull { it.key == sp.getString("map_style", MapStyle.OSM.key) } ?: MapStyle.OSM
        set(v) { sp.edit().putString("map_style", v.key).apply() }

    var speedUnit: SpeedUnit
        get() = SpeedUnit.values().firstOrNull { it.key == sp.getString("speed_unit", SpeedUnit.KNOTS.key) } ?: SpeedUnit.KNOTS
        set(v) { sp.edit().putString("speed_unit", v.key).apply() }

    var trackColor: TrackColor
        get() = TrackColor.values().firstOrNull { it.key == sp.getString("track_color", TrackColor.YELLOW.key) } ?: TrackColor.YELLOW
        set(v) { sp.edit().putString("track_color", v.key).apply() }

    var fullscreen: Boolean
        get() = sp.getBoolean("fullscreen", false)
        set(v) { sp.edit().putBoolean("fullscreen", v).apply() }
}
