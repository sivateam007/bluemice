package com.example.bluemice

import android.content.Context
import android.content.SharedPreferences
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.PredefinedLayouts
import org.json.JSONArray
import org.json.JSONObject

class LayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("layouts", Context.MODE_PRIVATE)

    var currentLayoutName: String
        get() = prefs.getString("current_keyboard_layout", "QWERTY (US)") ?: "QWERTY (US)"
        set(value) = prefs.edit().putString("current_keyboard_layout", value).apply()

    fun getCurrentLayout(mode: String = "keyboard"): LayoutDefinition {
        return getAllLayouts(mode).firstOrNull { it.name == getCurrentLayoutName(mode) }
            ?: getDefaultLayout(mode)
    }

    fun getCurrentLayoutName(mode: String): String {
        return prefs.getString("current_layout_$mode", getDefaultLayout(mode).name)
            ?: getDefaultLayout(mode).name
    }

    fun setCurrentLayoutName(mode: String, name: String) {
        prefs.edit().putString("current_layout_$mode", name).apply()
    }

    fun getAllLayouts(mode: String = "keyboard"): List<LayoutDefinition> {
        val custom = loadCustomLayouts()
        val modePredefined = PredefinedLayouts.all.filter { it.mode == mode }
        val modeCustom = custom.filter { it.mode == mode }
        return modeCustom + modePredefined
    }

    fun saveCustomLayout(layout: LayoutDefinition) {
        val list = loadCustomLayouts().toMutableList()
        // Scope by mode so a Mouse "Gaming" and Keyboard "Gaming" can coexist.
        val existing = list.indexOfFirst { it.name == layout.name && it.mode == layout.mode }
        if (existing >= 0) list[existing] = layout else list.add(layout)
        saveCustomLayouts(list)
    }

    fun getCustomLayouts(mode: String = "keyboard"): List<LayoutDefinition> {
        return loadCustomLayouts().filter { it.mode == mode }
    }

    /** Deletes only within [mode] so cross-mode names never collide. */
    fun deleteCustomLayout(mode: String, name: String) {
        val list = loadCustomLayouts().toMutableList()
        list.removeAll { it.name == name && it.mode == mode }
        saveCustomLayouts(list)
        // Repoint the current-layout pointer if we just deleted the active one.
        if (getCurrentLayoutName(mode) == name) {
            setCurrentLayoutName(mode, getDefaultLayout(mode).name)
        }
    }

    /** True when [newName] is unused for layouts in [mode]. */
    fun isNameAvailable(mode: String, newName: String): Boolean =
        getAllLayouts(mode).none { it.name == newName }

    fun renameLayout(mode: String, oldName: String, newName: String): Boolean {
        if (newName.isBlank() || newName == oldName) return false
        if (!isNameAvailable(mode, newName)) return false
        val list = loadCustomLayouts().toMutableList()
        val idx = list.indexOfFirst { it.mode == mode && it.name == oldName }
        if (idx < 0) return false
        list[idx] = list[idx].copy(name = newName)
        saveCustomLayouts(list)
        if (getCurrentLayoutName(mode) == oldName) setCurrentLayoutName(mode, newName)
        return true
    }

    private fun getDefaultLayout(mode: String): LayoutDefinition {
        return PredefinedLayouts.all.firstOrNull { it.mode == mode }
            ?: PredefinedLayouts.QWERTY
    }

    fun defaultLayout(mode: String): LayoutDefinition = getDefaultLayout(mode)

    fun defaultLayoutName(mode: String): String = getDefaultLayout(mode).name

    /**
     * Factory-fresh layout for [mode]. Any custom layout squatting on the default's
     * name is removed so the predefined one actually wins again - otherwise the
     * "Restore default" button would silently keep showing the emptied copy.
     */
    fun resetToDefault(mode: String) {
        val name = getDefaultLayout(mode).name
        val list = loadCustomLayouts().toMutableList()
        list.removeAll { it.name == name && it.mode == mode }
        saveCustomLayouts(list)
        setCurrentLayoutName(mode, name)
    }

    private fun loadCustomLayouts(): List<LayoutDefinition> {
        val json = prefs.getString("custom_layouts", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { parseLayout(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveCustomLayouts(layouts: List<LayoutDefinition>) {
        val arr = JSONArray()
        layouts.forEach { arr.put(serializeLayout(it)) }
        prefs.edit().putString("custom_layouts", arr.toString()).apply()
    }

    /** Serializes a single layout. Scoped to [mode] so every screen finds its own layout. */
    fun exportLayout(mode: String, name: String): String? {
        val layout = getAllLayouts(mode).find { it.name == name } ?: return null
        return serializeLayout(layout).toString(2)
    }

    fun importLayout(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val layout = parseLayout(obj)
            saveCustomLayout(layout)
            true
        } catch (e: Exception) { false }
    }

    /** One canonical schema (org.json short keys) used by every export/import path. */
    fun exportAllLayouts(): String {
        val arr = JSONArray()
        loadCustomLayouts().forEach { arr.put(serializeLayout(it)) }
        return JSONObject().apply {
            put("bluemice_export_version", 1)
            put("layouts", arr)
        }.toString(2)
    }

    fun importAllLayouts(json: String): Int {
        return try {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("layouts")
            var count = 0
            (0 until arr.length()).forEach { i ->
                runCatching {
                    saveCustomLayout(parseLayout(arr.getJSONObject(i)))
                    count++
                }
            }
            count
        } catch (e: Exception) { 0 }
    }

    fun duplicateLayout(mode: String, name: String, newName: String): Boolean {
        if (!isNameAvailable(mode, newName)) return false
        val src = getAllLayouts(mode).find { it.name == name } ?: return false
        saveCustomLayout(src.copy(name = newName))
        return true
    }

    /**
     * Creates and activates a brand new layout for [mode], seeded from the
     * predefined layout called [templateName] - or blank when [templateName] is
     * empty, which is what the template picker's "Start Blank" row passes.
     *
     * The copy is given a name that does not collide, so picking a template
     * never overwrites the template itself; picking the same one twice gives
     * you a second, independently editable layout.
     */
    fun createLayout(mode: String, templateName: String): LayoutDefinition {
        val template = PredefinedLayouts.all.find { it.mode == mode && it.name == templateName }
        val rows = template?.rows ?: emptyList()
        val base = template?.name ?: "New Layout"
        var name = base
        var i = 2
        while (!isNameAvailable(mode, name)) { name = "$base $i"; i++ }
        val layout = LayoutDefinition(name, rows, mode)
        saveCustomLayout(layout)
        setCurrentLayoutName(mode, name)
        return layout
    }

    private fun serializeLayout(l: LayoutDefinition): JSONObject {
        val rows = JSONArray()
        l.rows.forEach { row ->
            val keys = JSONArray()
            row.keys.forEach { k ->
                keys.put(JSONObject().apply {
                    put("label", k.label)
                    put("code", k.keyCode.toInt() and 0xFF)
                    put("width", k.width)
                    put("mod", k.isModifier)
                    put("spec", k.isSpecial)
                    put("cc", k.consumerCode)
                    put("mb", k.mouseButton)
                    put("cm", k.comboMods)
                    put("ck", k.comboKey.toInt() and 0xFF)
                    put("gb", k.gamepadButton)
                    if (k.isGamepad) put("gp", true)
                    put("msc", k.mouseScroll)
                    put("kt", k.keyType)
                    put("h", k.height.toDouble())
                    put("bg", k.backgroundColor)
                    k.textColor?.let { put("tc", it) }
                    put("tb", k.textBlock)
                    if (k.macroSteps.isNotEmpty()) {
                        val steps = JSONArray()
                        k.macroSteps.forEach { s ->
                            steps.put(JSONObject().apply {
                                put("type", s.type)
                                put("kc", s.keyCode.toInt() and 0xFF)
                                put("cc", s.consumerCode)
                                put("mb", s.mouseButton)
                                put("dly", s.delayMs)
                                put("txt", s.text)
                            })
                        }
                        put("mac", steps)
                    }
                    if (k.fullWidth) put("fw", true)
                    if (k.align != "start") put("al", k.align)
                    if (k.repeat) put("rp", true)
                    if (k.levels != null) {
                        put("lv", JSONObject().apply {
                            put("b", k.levels.base)
                            put("s", k.levels.shift)
                            put("a", k.levels.alt)
                            put("as", k.levels.altShift)
                        })
                    }
                })
            }
            rows.put(JSONObject().apply { put("keys", keys) })
        }
        return JSONObject().apply {
            put("name", l.name)
            put("mode", l.mode)
            put("rows", rows)
        }
    }

    private fun parseLayout(obj: JSONObject): LayoutDefinition {
        val mode = obj.optString("mode", "keyboard")
        val rows = mutableListOf<com.example.bluemice.model.LayoutRow>()
        val rowsArr = obj.getJSONArray("rows")
        (0 until rowsArr.length()).forEach { ri ->
            val r = rowsArr.getJSONObject(ri)
            val keys = mutableListOf<com.example.bluemice.model.LayoutKey>()
            val keysArr = r.getJSONArray("keys")
            (0 until keysArr.length()).forEach { ki ->
                val k = keysArr.getJSONObject(ki)
                // Macros: org.json writes "mac" with short field names, kotlinx
                // writes "macroSteps" with long names. Accept either.
                val macroArr = if (k.has("mac")) k.optJSONArray("mac")
                               else k.optJSONArray("macroSteps")
                val macroSteps = if (macroArr != null) {
                    (0 until macroArr.length()).map { si ->
                        val s = macroArr.getJSONObject(si)
                        com.example.bluemice.model.MacroStep(
                            type = s.optString("type", "key"),
                            keyCode = s.optInt("kc", s.optInt("keyCode", 0)).toByte(),
                            consumerCode = s.optInt("cc", s.optInt("consumerCode", 0)),
                            mouseButton = s.optInt("mb", s.optInt("mouseButton", 0)),
                            delayMs = s.optInt("dly", s.optInt("delayMs", 0)),
                            text = s.optString("txt", s.optString("text", ""))
                        )
                    }
                } else emptyList()
                // Key levels: org.json writes "lv" with short names, kotlinx
                // writes "levels" with long names. Accept either.
                val lvObj = if (k.has("lv")) k.optJSONObject("lv")
                            else k.optJSONObject("levels")
                val levels = if (lvObj != null) {
                    com.example.bluemice.model.KeyLevels(
                        base = lvObj.optString("b", lvObj.optString("base", "")),
                        shift = lvObj.optString("s", lvObj.optString("shift", "")),
                        alt = lvObj.optString("a", lvObj.optString("alt", "")),
                        altShift = lvObj.optString("as", lvObj.optString("altShift", ""))
                    )
                } else null
                keys.add(com.example.bluemice.model.LayoutKey(
                    label = k.getString("label"),
                    // Tolerate BOTH schemas — org.json short keys ("code", "cc", …)
                    // and kotlinx long keys ("keyCode", "consumerCode", …). Without
                    // the fallbacks a file exported from KeyboardScreen imported
                    // here would silently keep only the labels.
                    keyCode = k.optInt("code", k.optInt("keyCode", 0)).toByte(),
                    width = k.optDouble("width", 1.0).toFloat(),
                    isModifier = k.optBoolean("mod", k.optBoolean("isModifier", false)),
                    isSpecial = k.optBoolean("spec", k.optBoolean("isSpecial", false)),
                    consumerCode = k.optInt("cc", k.optInt("consumerCode", 0)),
                    mouseButton = k.optInt("mb", k.optInt("mouseButton", 0)),
                    comboMods = k.optInt("cm", k.optInt("comboMods", 0)),
                    comboKey = k.optInt("ck", k.optInt("comboKey", 0)).toByte(),
                    gamepadButton = k.optInt("gb", k.optInt("gamepadButton", 0)),
                    isGamepad = k.optBoolean("gp", k.optBoolean("isGamepad", false)),
                    mouseScroll = k.optInt("msc", k.optInt("mouseScroll", 0)),
                    keyType = k.optString("kt", k.optString("keyType", "keyboard")),
                    height = k.optDouble("h", k.optDouble("height", 1.0)).toFloat(),
                    backgroundColor = k.optLong("bg", k.optLong("backgroundColor", 0xFF3A3A3A)),
                    // kotlinx writes an explicit null for a null textColor, so
                    // has() alone would turn "no colour" into transparent black.
                    textColor = when {
                        k.has("tc") && !k.isNull("tc") -> k.optLong("tc")
                        k.has("textColor") && !k.isNull("textColor") -> k.optLong("textColor")
                        else -> null
                    },
                    textBlock = k.optString("tb", k.optString("textBlock", "")),
                    macroSteps = macroSteps,
                    fullWidth = k.optBoolean("fw", k.optBoolean("fullWidth", false)),
                    align = k.optString("al", k.optString("align", "start")),
                    repeat = k.optBoolean("rp", k.optBoolean("repeat", false)),
                    levels = levels
                ))
            }
            rows.add(com.example.bluemice.model.LayoutRow(keys))
        }
        return LayoutDefinition(obj.getString("name"), rows, mode)
    }
}
