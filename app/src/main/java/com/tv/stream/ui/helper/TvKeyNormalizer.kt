package com.tv.stream.ui.helper

import android.view.KeyEvent

object TvKeyNormalizer {
    fun isSelectKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> true
        else -> false
    }

    fun isBackKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_BUTTON_B -> true
        else -> false
    }

    fun isQuickSourceMenuKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_INFO,
        KeyEvent.KEYCODE_CAPTIONS,
        KeyEvent.KEYCODE_DPAD_DOWN -> true
        else -> false
    }
}
