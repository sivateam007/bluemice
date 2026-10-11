package com.example.bluemice.model

import java.util.Locale

data class LangKey(
    val code: Byte,
    val base: String,
    val shift: String = "",
    val alt: String = "",
    val altShift: String = ""
)

data class KeyboardLanguage(
    val name: String,
    val locale: String,
    val rows: List<List<LangKey>>
) {
    fun keyFor(code: Byte): LangKey? {
        return rows.asSequence().flatten().firstOrNull { it.code == code }
    }

    fun charAtLevel(k: LangKey, level: Int): String = when (level) {
        0 -> k.base
        1 -> k.shift.ifEmpty { k.base }
        2 -> k.alt.ifEmpty { k.base }
        3 -> k.altShift.ifEmpty { k.shift.ifEmpty { k.base } }
        else -> k.base
    }

    fun findChar(char: String, levels: IntArray = intArrayOf(0, 1, 2, 3)): Pair<Byte, Int>? {
        for (code in GRID_CODES) {
            val k = keyFor(code) ?: continue
            for (level in levels) {
                if (charAtLevel(k, level) == char) return Pair(code, level)
            }
        }
        return null
    }
}

private const val TOP_CODES =
    "0x35,0x1E,0x1F,0x20,0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x2D,0x2E"
private const val R1_CODES =
    "0x14,0x1A,0x08,0x15,0x17,0x1C,0x18,0x0C,0x12,0x13,0x2F,0x30,0x31"
private const val R2_CODES =
    "0x04,0x16,0x07,0x09,0x0A,0x0B,0x0D,0x0E,0x0F,0x33,0x34"
private const val R3_CODES =
    "0x1D,0x1B,0x06,0x19,0x05,0x11,0x10,0x36,0x37,0x38"

val GRID_CODES: List<Byte> by lazy {
    (TOP_CODES + "," + R1_CODES + "," + R2_CODES + "," + R3_CODES)
        .split(",").map { it.trim().removePrefix("0x").toInt(16).toByte() }
}

object KeyboardLanguageCatalog {
    private fun codes(s: String): List<Int> = s.split(",").map { it.trim().removePrefix("0x").toInt(16) }

    private fun lk(code: Int, base: String, shift: String = "", alt: String = "", altShift: String = ""): LangKey {
        val sh = if (shift.isEmpty() && base.length == 1 && base[0].isLowerCase()) {
            base[0].uppercaseChar().toString()
        } else shift
        return LangKey(code.toByte(), base, sh, alt, altShift)
    }

    private fun build(
        name: String,
        locale: String,
        top: String,
        topShift: String,
        r1: String,
        r2: String,
        r3: String,
        altTop: String = "",
        altR1: String = "",
        altR2: String = "",
        altR3: String = ""
    ): KeyboardLanguage {
        fun clean(c: Char?): String = c?.takeIf { it != '\u0000' }?.toString() ?: ""
        fun row(cs: List<Int>, base: String, shift: String = "", alt: String = ""): List<LangKey> {
            return cs.mapIndexed { i, c ->
                lk(c, clean(base.getOrNull(i)).ifEmpty { " " }, clean(shift.getOrNull(i)), clean(alt.getOrNull(i)))
            }
        }
        val tc = codes(TOP_CODES)
        val r1c = codes(R1_CODES)
        val r2c = codes(R2_CODES)
        val r3c = codes(R3_CODES)
        return KeyboardLanguage(
            name = name,
            locale = locale,
            rows = listOf(
                row(tc, top, topShift, altTop),
                row(r1c, r1, "", altR1),
                row(r2c, r2, "", altR2),
                row(r3c, r3, "", altR3)
            )
        )
    }

    private val qwertyR1 = "qwertyuiop[]\\"
    private val qwertyR2 = "asdfghjkl;'"
    private val qwertyR3 = "zxcvbnm,./"

    val all: List<KeyboardLanguage> by lazy { buildCatalog() }

    fun byLocale(locale: String): KeyboardLanguage {
        val lang = locale.substringBefore("_").substringBefore("-").lowercase(Locale.ROOT)
        return all.firstOrNull { it.locale == lang } ?: all.first()
    }

    private fun buildCatalog(): List<KeyboardLanguage> {
        val list = mutableListOf<KeyboardLanguage>()

        fun add(l: KeyboardLanguage) { list.add(l) }

        // ---- Latin QWERTY family (US-style letters, different symbol rows) ----
        add(build("English (US)", "en", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3,
            altTop = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
            altR1 = "\u0000\u0000€", altR2 = "\u0000\u0000\u0000\u0000æœ"))
        add(build("English (UK)", "en-gb", "`1234567890-=", "¬!\"£$%^&*()_+", qwertyR1, qwertyR2, qwertyR3,
            altR1 = "\u0000\u0000€", altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"))
        add(build("English (Australia)", "en-au", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("English (Canada)", "en-ca", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Indonesian", "id", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Malay", "ms", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Filipino", "tl", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Vietnamese", "vi", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ă"))
        add(build("Afrikaans", "af", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Swahili", "sw", "`1234567890-=", "~!@#$%^&*()_+", qwertyR1, qwertyR2, qwertyR3))
        add(build("Italian", "it", "\\1234567890'ì", "|!\"£$%&/()=?^", qwertyR1, qwertyR2, qwertyR3,
            altR1 = "\u0000\u0000€", altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000@"))

        // ---- AZERTY family ----
        add(build("French (AZERTY)", "fr", "²&é\"'(-è_çà)=", "~1234567890°+",
            "azertyuiop^\$ù", "qsdfghjklmù", "wxcvbnm,;:!",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000€"))
        add(build("French (Canada) CSA", "fr-ca", "`1234567890-=", "~!@#$%^&*()_+",
            "azertyuiop^\$ù", "qsdfghjklmù", "wxcvbnm,;:!",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000àç"))

        // ---- QWERTZ family ----
        add(build("German (Germany)", "de", "´1234567890ß´", "`!\"§$%&/()=?",
            "qwertzuiopü+#", "asdfghjklöä", "yxcvbnm,.-",
            altR1 = "@\u0000€", altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ß"))
        add(build("German (Austria)", "de-at", "`1234567890ß´", "`!\"§$%&/()=?",
            "qwertzuiopü+#", "asdfghjklöä", "yxcvbnm,.-"))
        add(build("German (Switzerland)", "de-ch", "§1234567890´´", "°+\"*%&/()=?!",
            "qwertzuiopü¨$", "asdfghjklöä", "yxcvbnm,.-"))
        add(build("Czech", "cs", ";1234567890´=", "°+ěščřžýáíé=(",
            "qwertzuiopú)", "asdfghjklů§", "yxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ß"))
        add(build("Slovak", "sk", ";1234567890´=", "°+ľščťžýáíé=(",
            "qwertzuiopú)", "asdfghjklô§", "yxcvbnm,.-",
            altR1 = "\u0000\u0000€"))
        add(build("Hungarian", "hu", "0öüó123456789", "§'\"+!%/=()*",
            "qwertzuiopőú", "asdfghjkléáű", "yxcvbnm,.-"))
        add(build("Polish (Programmer)", "pl", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ą"))
        add(build("Romanian", "ro", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ăî"))
        add(build("Slovenian", "sl", "`1234567890-=", "~!\"#$%&/()=?",
            "qwertzuiop[]\\", "asdfghjkl;'", "yxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000č", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000žšđ"))
        add(build("Croatian", "hr", "`1234567890-=", "~!\"#$%&/()=?",
            "qwertzuiop[]\\", "asdfghjkl;'", "yxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000č", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000žšđ"))
        add(build("Serbian (Latin)", "sr-latn", "`1234567890-=", "~!\"#$%&/()=?",
            "qwertzuiop[]\\", "asdfghjkl;'", "yxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000č", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000žšđ"))

        // ---- Iberian ----
        add(build("Spanish (Spain)", "es", "º1234567890'¡", "ª!\"·$%&/()=?¿",
            "qwertyuiop`+", "asdfghjklñ´", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000áéíóú"))
        add(build("Spanish (Latin America)", "es-mx", "`1234567890-=", "~!@#$%^&*()_+",
            "qwertyuiop[]\\", "asdfghjklñ;'", "zxcvbnm,.-"))
        add(build("Portuguese (Portugal)", "pt", "\\1234567890'«", "|!\"#$%&/()=?»",
            "qwertyuiop[]\\", "asdfghjklç;'", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ã"))
        add(build("Portuguese (Brazil)", "pt-br", "'1234567890-=", "\"!@#$%&*()_+",
            "qwertyuiop[]\\", "asdfghjklç;'", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ã"))
        add(build("Catalan", "ca", "º1234567890'¡", "ª!\"·$%&/()=?¿",
            "qwertyuiop`+", "asdfghjklç´", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000àèéíï", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000óòúü"))

        // ---- Nordic ----
        add(build("Danish", "da", "½1234567890+´", "§!\"#$%&/()=?`",
            "qwertyuiopå¨'", "asdfghjklæø", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000@"))
        add(build("Norwegian", "no", "|1234567890+\\", "§!\"#¤%&/()=?`",
            "qwertyuiopå¨'", "asdfghjkløæ", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000@"))
        add(build("Swedish", "sv", "§1234567890+´", "½!\"#¤%&/()=?`",
            "qwertyuiopå¨'", "asdfghjklöä", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000@"))
        add(build("Finnish", "fi", "§1234567890+´", "½!\"#¤%&/()=?`",
            "qwertyuiopå¨'", "asdfghjklöä", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000@"))
        add(build("Icelandic", "is", "`1234567890'´", "~!\"#$%&/()=?`",
            "qwertyuiopð'", "asdfghjklæö", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000áéíóúýþ"))
        add(build("Estonian", "et", "`1234567890+´", "~!\"#$%&/()=?`",
            "qwertyuiopüõ'", "asdfghjklöä", "zxcvbnm,.-",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000šž"))
        add(build("Latvian", "lv", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"))
        add(build("Lithuanian", "lt", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ąčę"))

        // ---- Cyrillic ----
        add(build("Russian", "ru", "1234567890-=", "!\"№;%:?*()_+",
            "йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю",
            altTop = "ё"))
        add(build("Ukrainian", "uk", "1234567890-=", "!\"№;%:?*()_+",
            "йцукенгшщзхї", "фівапролджє", "ячсмитьбю"))
        add(build("Belarusian", "be", "1234567890-=", "!\"№;%:?*()_+",
            "йцукенгшўзх'", "фывапролджэ", "ячсмітьбю"))
        add(build("Serbian (Cyrillic)", "sr", "1234567890-=", "!\"№;%:?*()_+",
            "љњертѕуиопшђ", "асдфгхјклчћ", "зџцвбнм,./",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000ж"))
        add(build("Macedonian", "mk", "1234567890-=", "!\"№;%:?*()_+",
            "љњертѕуиопшѓ", "асдфгхјклчќ", "зџцвбнм,./"))
        add(build("Bulgarian (Phonetic)", "bg", "1234567890-=", "!\"№;%:?*()_+",
            "яшертъуюиопщ", "асдфгхйклжз", "ьцчвбнм,./"))
        add(build("Kazakh", "kk", "1234567890-=", "!\"№;%:?*()_+",
            "йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю",
            altR1 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000әғ"))
        add(build("Mongolian", "mn", "1234567890-=", "!\"№;%:?*()_+",
            "фцукенгшщзхъ", "ывапролджэ", "ячсмитьбю"))

        // ---- Greek ----
        add(build("Greek", "el", "`1234567890'´", "~!\"$%^&*()_+",
            ";ςερτυθιοπ[]\\", "ασδφγηξκλ;'", "ζχψωβνμ,./",
            altR2 = "\u0000\u0000\u0000άέή", altR3 = "\u0000\u0000\u0000\u0000ίόύώ"))

        // ---- Semitic ----
        add(build("Hebrew", "he", ";1234567890-=", "~!@#$%^&*()_+",
            "/'קרטףגכ][\\", "שדגכעיחלך;'", "זסבהנמצתץ,."))
        add(build("Arabic", "ar", "1234567890-=", "!@#$%^&*()_+",
            "ضصثقفغعهخحجد", "شسيبلاتنمكطئ", "ءؤرىةوزظ",
            altR1 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000إأ"))
        add(build("Persian", "fa", "1234567890-=", "!@#$%^&*()_+",
            "ضصثقفغعهخحجد", "شسيبلاتنمکگطظ", "ءؤرىةوزژ",
            altR1 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000پچ"))
        add(build("Urdu", "ur", "1234567890-=", "!@#$%^&*()_+",
            "طصثقفغعهخحجد", "شسيبلاتنمکگطظ", "ءؤرىةوزژ",
            altR1 = "\u0000\u0000\u0000\u0000\u0000\u0000ڈڑ"))

        // ---- Indic (Devanagari family) ----
        add(build("Hindi (Devanagari)", "hi", "1234567890-=", "!@#$%^&*()_+",
            "ौैाीूबहगदज", "ोे्।परकतचट", "यूॉंमनवलस,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ड़ढ़"))
        add(build("Marathi (Devanagari)", "mr", "1234567890-=", "!@#$%^&*()_+",
            "ौैाीूबहगदज", "ोे्।परकतचट", "यूॉंमनवलस,."))
        add(build("Nepali (Devanagari)", "ne", "1234567890-=", "!@#$%^&*()_+",
            "ौैाीूबहगदज", "ोे्।परकतचट", "यूॉंमनवलस,."))
        add(build("Bengali", "bn", "1234567890-=", "!@#$%^&*()_+",
            "ৌৈাীূবহগদজ", "োে্।পরকতচট", "যূাঃংমনবলস,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ডঢ"))
        add(build("Gujarati", "gu", "1234567890-=", "!@#$%^&*()_+",
            "ૌૈાીૂબહગદજ", "ોએ્।પરકતચટ", "યૂઅંમનવલસ,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ડઢ"))
        add(build("Tamil", "ta", "1234567890-=", "!@#$%^&*()_+",
            "ௌைாீூபஹகதஜ", "ோே்।பரகதசட", "யூஅஂமனவலஸ,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000டஷ"))
        add(build("Telugu", "te", "1234567890-=", "!@#$%^&*()_+",
            "ౌైాీూబహగదజ", "ోే్।పరకతచట", "యూఅం్మనవలస,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000డష"))
        add(build("Kannada", "kn", "1234567890-=", "!@#$%^&*()_+",
            "ೌೈಾೀೂಬಹಗದಜ", "ೋೇ್।ಪರಕತಚಟ", "ಯೂಅಂಮನವಲಸ,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ಡಷ"))
        add(build("Malayalam", "ml", "1234567890-=", "!@#$%^&*()_+",
            "ൌൈാീൂബഹഗദജ", "ോേ്।പരകതചട", "യൂഅംമനവലസ,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ഡഷ"))
        add(build("Punjabi (Gurmukhi)", "pa", "1234567890-=", "!@#$%^&*()_+",
            "ੌੈਾੀੂਬਹਗਦਜ", "ੋੇ੍।ਪਰਕਤਚਟ", "ਯੂਅੰਮਨਵਲਸ,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ਡਢ"))
        add(build("Sinhala", "si", "1234567890-=", "!@#$%^&*()_+",
            "ෞෛාීූබහගදජ", "ොේ්।පරකතචට", "යූඅංමනවලස,.",
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000ඩෂ"))

        // ---- Southeast Asia ----
        add(build("Thai (Kedmanee)", "th", "ๆ1234567890-=", "!@#$%^&*()_+",
            "ไำพะัีรนยบลฃ", "ฟหกดเาสวงผ", "ปแอทมใฝ,."))
        add(build("Khmer", "km", "1234567890-=", "!@#$%^&*()_+",
            "ៈ៉៊ឺើៗផ័្", "ូួោៃឹែីះ", "ឿ឵៖៕៖៓។៕"))

        // ---- Turkish ----
        add(build("Turkish (Q)", "tr", "1234567890*_-", "!'^+%&(/)=?",
            "qwertyuıopğü", "asdfghjklşi,", "zxcvbnmöç.\\"))
        add(build("Turkish (F)", "tr-f", "1234567890*_-", "!'^+%&(/)=?",
            "fgğıodrhntkm", "eaiuüstkşzly", "cvböçnjxq,."))

        // ---- East Asia ----
        add(build("Korean (Dubeolsik)", "ko", "`1234567890-=", "~!@#$%^&*()_+",
            "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ",
            altR2 = "\u0000\u0000\u0000\u0000\u0000ㅃㅉㄸ"))
        add(build("Japanese (Romaji)", "ja", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000あいうえお"))
        add(build("Chinese (Pinyin)", "zh", "`1234567890-=", "~!@#$%^&*()_+",
            qwertyR1, qwertyR2, qwertyR3,
            altR2 = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000āáǎà", altR3 = "\u0000\u0000\u0000\u0000\u0000\u0000ēéěè"))

        return list.toList()
    }
}
