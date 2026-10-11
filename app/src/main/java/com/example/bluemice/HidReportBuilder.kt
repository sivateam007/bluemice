package com.example.bluemice

object HidReportBuilder {
    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_MOUSE: Byte = 2
    const val REPORT_ID_GAMEPAD: Byte = 3
    const val REPORT_ID_CONSUMER: Byte = 4

    val reportDescriptor: ByteArray by lazy { buildReportDescriptor() }

    private fun buildReportDescriptor(): ByteArray {
        return byteArrayOf(
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x06.toByte(),
            0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_KEYBOARD,
            0x05.toByte(), 0x07.toByte(),
            0x19.toByte(), 0xE0.toByte(),
            0x29.toByte(), 0xE7.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x08.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x81.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x06.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x65.toByte(),
            0x05.toByte(), 0x07.toByte(),
            0x19.toByte(), 0x00.toByte(),
            0x29.toByte(), 0x65.toByte(),
            0x81.toByte(), 0x00.toByte(),
            0xC0.toByte(),

            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x02.toByte(),
            0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_MOUSE,
            0x09.toByte(), 0x01.toByte(),
            0xA1.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x09.toByte(),
            0x19.toByte(), 0x01.toByte(),
            0x29.toByte(), 0x03.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x03.toByte(),
            0x75.toByte(), 0x01.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x05.toByte(),
            0x81.toByte(), 0x01.toByte(),
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x30.toByte(),
            0x09.toByte(), 0x31.toByte(),
            0x09.toByte(), 0x38.toByte(),
            0x15.toByte(), 0x81.toByte(),
            0x25.toByte(), 0x7F.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x03.toByte(),
            0x81.toByte(), 0x06.toByte(),
            0xC0.toByte(),
            0xC0.toByte(),

            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x05.toByte(),
            0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_GAMEPAD,
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x30.toByte(),
            0x09.toByte(), 0x31.toByte(),
            0x09.toByte(), 0x32.toByte(),
            0x09.toByte(), 0x35.toByte(),
            0x15.toByte(), 0x81.toByte(),
            0x25.toByte(), 0x7F.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x04.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0x05.toByte(), 0x09.toByte(),
            0x19.toByte(), 0x01.toByte(),
            0x29.toByte(), 0x10.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x10.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0xC0.toByte(),

            0x05.toByte(), 0x0C.toByte(),
            0x09.toByte(), 0x01.toByte(),
            0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_CONSUMER,
            0x1A.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x2A.toByte(), 0xFF.toByte(), 0x02.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x26.toByte(), 0xFF.toByte(), 0x02.toByte(),
            0x75.toByte(), 0x10.toByte(),
            0x95.toByte(), 0x01.toByte(),
            0x81.toByte(), 0x00.toByte(),
            0xC0.toByte()
        )
    }

    fun buildMouseReport(buttons: Int, deltaX: Int, deltaY: Int, wheel: Int): ByteArray {
        return byteArrayOf(
            REPORT_ID_MOUSE,
            buttons.toByte(),
            deltaX.coerceIn(-127, 127).toByte(),
            deltaY.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
    }

    fun buildKeyboardReportSingleKey(modifiers: Int, key: Byte): ByteArray {
        return byteArrayOf(
            REPORT_ID_KEYBOARD,
            modifiers.toByte(), 0, key, 0, 0, 0, 0, 0
        )
    }

    fun buildEmptyKeyboardReport(): ByteArray = byteArrayOf(REPORT_ID_KEYBOARD, 0, 0, 0, 0, 0, 0, 0, 0)

    fun buildGamepadReport(
        leftX: Int, leftY: Int, rightX: Int, rightY: Int,
        buttons: Int
    ): ByteArray {
        return byteArrayOf(
            REPORT_ID_GAMEPAD,
            leftX.coerceIn(-127, 127).toByte(),
            leftY.coerceIn(-127, 127).toByte(),
            rightX.coerceIn(-127, 127).toByte(),
            rightY.coerceIn(-127, 127).toByte(),
            0, 0,
            (buttons and 0xFF).toByte(),
            ((buttons shr 8) and 0xFF).toByte()
        )
    }

    fun buildEmptyGamepadReport(): ByteArray = byteArrayOf(REPORT_ID_GAMEPAD, 0, 0, 0, 0, 0, 0, 0, 0)

    fun buildConsumerReport(code: Int): ByteArray {
        return byteArrayOf(
            REPORT_ID_CONSUMER,
            (code and 0xFF).toByte(),
            ((code shr 8) and 0xFF).toByte()
        )
    }

    fun buildEmptyConsumerReport(): ByteArray = byteArrayOf(REPORT_ID_CONSUMER, 0, 0)

    object KeyCodes {
        const val KEY_A: Byte = 0x04
        const val KEY_B: Byte = 0x05
        const val KEY_C: Byte = 0x06
        const val KEY_D: Byte = 0x07
        const val KEY_E: Byte = 0x08
        const val KEY_F: Byte = 0x09
        const val KEY_G: Byte = 0x0A
        const val KEY_H: Byte = 0x0B
        const val KEY_I: Byte = 0x0C
        const val KEY_J: Byte = 0x0D
        const val KEY_K: Byte = 0x0E
        const val KEY_L: Byte = 0x0F
        const val KEY_M: Byte = 0x10
        const val KEY_N: Byte = 0x11
        const val KEY_O: Byte = 0x12
        const val KEY_P: Byte = 0x13
        const val KEY_Q: Byte = 0x14
        const val KEY_R: Byte = 0x15
        const val KEY_S: Byte = 0x16
        const val KEY_T: Byte = 0x17
        const val KEY_U: Byte = 0x18
        const val KEY_V: Byte = 0x19
        const val KEY_W: Byte = 0x1A
        const val KEY_X: Byte = 0x1B
        const val KEY_Y: Byte = 0x1C
        const val KEY_Z: Byte = 0x1D
        const val KEY_1: Byte = 0x1E
        const val KEY_2: Byte = 0x1F
        const val KEY_3: Byte = 0x20
        const val KEY_4: Byte = 0x21
        const val KEY_5: Byte = 0x22
        const val KEY_6: Byte = 0x23
        const val KEY_7: Byte = 0x24
        const val KEY_8: Byte = 0x25
        const val KEY_9: Byte = 0x26
        const val KEY_0: Byte = 0x27
        const val KEY_ENTER: Byte = 0x28
        const val KEY_ESCAPE: Byte = 0x29
        const val KEY_BACKSPACE: Byte = 0x2A
        const val KEY_TAB: Byte = 0x2B
        const val KEY_SPACE: Byte = 0x2C
        const val KEY_MINUS: Byte = 0x2D
        const val KEY_EQUAL: Byte = 0x2E
        const val KEY_LEFT_BRACKET: Byte = 0x2F
        const val KEY_RIGHT_BRACKET: Byte = 0x30
        const val KEY_BACKSLASH: Byte = 0x31
        const val KEY_SEMICOLON: Byte = 0x33
        const val KEY_QUOTE: Byte = 0x34
        const val KEY_GRAVE: Byte = 0x35
        const val KEY_COMMA: Byte = 0x36
        const val KEY_DOT: Byte = 0x37
        const val KEY_SLASH: Byte = 0x38
        const val KEY_CAPS_LOCK: Byte = 0x39
        const val KEY_F1: Byte = 0x3A
        const val KEY_F2: Byte = 0x3B
        const val KEY_F3: Byte = 0x3C
        const val KEY_F4: Byte = 0x3D
        const val KEY_F5: Byte = 0x3E
        const val KEY_F6: Byte = 0x3F
        const val KEY_F7: Byte = 0x40
        const val KEY_F8: Byte = 0x41
        const val KEY_F9: Byte = 0x42
        const val KEY_F10: Byte = 0x43
        const val KEY_F11: Byte = 0x44
        const val KEY_F12: Byte = 0x45
        const val KEY_PRINT_SCREEN: Byte = 0x46
        const val KEY_SCROLL_LOCK: Byte = 0x47
        const val KEY_PAUSE: Byte = 0x48
        const val KEY_INSERT: Byte = 0x49
        const val KEY_HOME: Byte = 0x4A
        const val KEY_PAGE_UP: Byte = 0x4B
        const val KEY_DELETE: Byte = 0x4C
        const val KEY_END: Byte = 0x4D
        const val KEY_PAGE_DOWN: Byte = 0x4E
        const val KEY_ARROW_RIGHT: Byte = 0x4F
        const val KEY_ARROW_DOWN: Byte = 0x51
        const val KEY_ARROW_LEFT: Byte = 0x50
        const val KEY_ARROW_UP: Byte = 0x52
        const val KEY_NUM_LOCK: Byte = 0x53
        const val KEY_KP_SLASH: Byte = 0x54
        const val KEY_KP_ASTERISK: Byte = 0x55
        const val KEY_KP_MINUS: Byte = 0x56
        const val KEY_KP_PLUS: Byte = 0x57
        const val KEY_KP_ENTER: Byte = 0x58
        const val KEY_KP_1: Byte = 0x59
        const val KEY_KP_2: Byte = 0x5A
        const val KEY_KP_3: Byte = 0x5B
        const val KEY_KP_4: Byte = 0x5C
        const val KEY_KP_5: Byte = 0x5D
        const val KEY_KP_6: Byte = 0x5E
        const val KEY_KP_7: Byte = 0x5F
        const val KEY_KP_8: Byte = 0x60
        const val KEY_KP_9: Byte = 0x61
        const val KEY_KP_0: Byte = 0x62
        const val KEY_KP_DOT: Byte = 0x63
        const val KEY_POWER: Byte = 0x66
        const val KEY_KP_EQUAL: Byte = 0x67
        const val KEY_F13: Byte = 0x68
        const val KEY_F14: Byte = 0x69
        const val KEY_F15: Byte = 0x6A
        const val KEY_F16: Byte = 0x6B
        const val KEY_F17: Byte = 0x6C
        const val KEY_F18: Byte = 0x6D
        const val KEY_F19: Byte = 0x6E
        const val KEY_F20: Byte = 0x6F
        const val KEY_F21: Byte = 0x70
        const val KEY_F22: Byte = 0x71
        const val KEY_F23: Byte = 0x72
        const val KEY_F24: Byte = 0x73
        const val KEY_OPEN: Byte = 0x74
        const val KEY_HELP: Byte = 0x75
        const val KEY_PROPS: Byte = 0x76
        const val KEY_FRONT: Byte = 0x77
        const val KEY_STOP: Byte = 0x78
        const val KEY_AGAIN: Byte = 0x79
        const val KEY_UNDO: Byte = 0x7A
        const val KEY_CUT: Byte = 0x7B
        const val KEY_COPY: Byte = 0x7C
        const val KEY_PASTE: Byte = 0x7D
        const val KEY_FIND: Byte = 0x7E
        const val KEY_MUTE: Byte = 0x7F
        const val KEY_VOLUME_UP: Byte = -128    // 0x80
        const val KEY_VOLUME_DOWN: Byte = -127   // 0x81
        const val KEY_SLEEP: Byte = -126   // 0x82
        const val KEY_WAKE: Byte = -125    // 0x83
        const val KEY_MEDIA_PLAY: Byte = -80    // 0xB0
        const val KEY_MEDIA_PAUSE: Byte = -79   // 0xB1
        const val KEY_MEDIA_RECORD: Byte = -78  // 0xB2
        const val KEY_MEDIA_FAST_FORWARD: Byte = -77  // 0xB3
        const val KEY_MEDIA_REWIND: Byte = -76  // 0xB4
        const val KEY_MEDIA_NEXT_TRACK: Byte = -75  // 0xB5
        const val KEY_MEDIA_PREV_TRACK: Byte = -74  // 0xB6
        const val KEY_MEDIA_STOP: Byte = -73   // 0xB7
        const val KEY_MEDIA_EJECT: Byte = -72  // 0xB8
        const val KEY_MEDIA_PLAY_PAUSE: Byte = -51  // 0xCD
        const val KEY_BRIGHTNESS_UP: Byte = -50  // 0xCE
        const val KEY_BRIGHTNESS_DOWN: Byte = -49  // 0xCF
        const val KEY_LEFT_SHIFT: Byte = 0xE1.toByte()
        const val KEY_RIGHT_SHIFT: Byte = 0xE5.toByte()

        const val MOD_LEFT_CTRL: Int = 0x01
        const val MOD_LEFT_SHIFT: Int = 0x02
        const val MOD_LEFT_ALT: Int = 0x04
        const val MOD_LEFT_META: Int = 0x08
        const val MOD_RIGHT_CTRL: Int = 0x10
        const val MOD_RIGHT_SHIFT: Int = 0x20
        const val MOD_RIGHT_ALT: Int = 0x40
        const val MOD_RIGHT_META: Int = 0x80

        fun charToKey(c: Char): Pair<Byte, Int> {
            val modifiers = if (c.isUpperCase()) MOD_LEFT_SHIFT else 0
            val lower = c.lowercaseChar()
            val key = when (lower) {
                'a' -> KEY_A; 'b' -> KEY_B; 'c' -> KEY_C; 'd' -> KEY_D
                'e' -> KEY_E; 'f' -> KEY_F; 'g' -> KEY_G; 'h' -> KEY_H
                'i' -> KEY_I; 'j' -> KEY_J; 'k' -> KEY_K; 'l' -> KEY_L
                'm' -> KEY_M; 'n' -> KEY_N; 'o' -> KEY_O; 'p' -> KEY_P
                'q' -> KEY_Q; 'r' -> KEY_R; 's' -> KEY_S; 't' -> KEY_T
                'u' -> KEY_U; 'v' -> KEY_V; 'w' -> KEY_W; 'x' -> KEY_X
                'y' -> KEY_Y; 'z' -> KEY_Z
                '0' -> KEY_0; '1' -> KEY_1; '2' -> KEY_2; '3' -> KEY_3
                '4' -> KEY_4; '5' -> KEY_5; '6' -> KEY_6; '7' -> KEY_7
                '8' -> KEY_8; '9' -> KEY_9
                ' ' -> KEY_SPACE; '-' -> KEY_MINUS; '=' -> KEY_EQUAL
                '[' -> KEY_LEFT_BRACKET; ']' -> KEY_RIGHT_BRACKET
                '\\' -> KEY_BACKSLASH; ';' -> KEY_SEMICOLON
                '\'' -> KEY_QUOTE; '`' -> KEY_GRAVE
                ',' -> KEY_COMMA; '.' -> KEY_DOT; '/' -> KEY_SLASH
                '\n' -> KEY_ENTER; '\t' -> KEY_TAB
                else -> 0
            }
            val specialShifted = when (c) {
                '!' -> KEY_1; '@' -> KEY_2; '#' -> KEY_3; '$' -> KEY_4
                '%' -> KEY_5; '^' -> KEY_6; '&' -> KEY_7; '*' -> KEY_8
                '(' -> KEY_9; ')' -> KEY_0
                '_' -> KEY_MINUS; '+' -> KEY_EQUAL
                '{' -> KEY_LEFT_BRACKET; '}' -> KEY_RIGHT_BRACKET
                '|' -> KEY_BACKSLASH; ':' -> KEY_SEMICOLON
                '"' -> KEY_QUOTE; '~' -> KEY_GRAVE
                '<' -> KEY_COMMA; '>' -> KEY_DOT; '?' -> KEY_SLASH
                else -> 0
            }
            if (specialShifted != 0.toByte()) return Pair(specialShifted, MOD_LEFT_SHIFT)
            return Pair(key, modifiers)
        }
    }
}
