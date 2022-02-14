package com.shurman.remkey;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class KeyboardBasic implements KeyboardLayoutManager {
    private static final int LAYOUT_RES = R.layout.frag_ime_basic_2;

    private View keyboardView;

    private boolean shift = false;
    private boolean capsLock = false;

    @Override
    public View reInstateKeyboard(Context context) {
        keyboardView =  ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(LAYOUT_RES, null, false);
        return keyboardView;
    }
    @Override
    public byte[] letterPressed(String code) {
        if (checkShiftAndAdjust()) return code.toUpperCase().getBytes();
        return code.getBytes();
    }

    @Override
    public byte[] symbolPressed(String code) {
        checkShiftAndAdjust();
        return code.getBytes();
    }

    @Override
    public byte[] spacerPressed(String code) {
        checkShiftAndAdjust();
        return code.getBytes();
    }

    @Override
    public byte[] commandPressed(String code) {
        return code.getBytes();
    }

    @Override
    public void shiftPressed() {
        if (capsLock) {
            shift = false;
            capsLock = false;
        } else if (shift) {
            capsLock = true;
        } else {
            shift = true;
        }
    }

    private boolean checkShiftAndAdjust() {
        if (capsLock) return true;
        if (!shift) return false;
        shift = false;
        return true;
    }
}
