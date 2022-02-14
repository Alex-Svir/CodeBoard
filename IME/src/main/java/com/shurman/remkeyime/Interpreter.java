package com.shurman.remkeyime;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import com.shurman.remkeyime.qbluetooth.QuickBluetooth;

public class Interpreter implements QuickBluetooth.DataReceivedCallback {
    private static final String COMMAND_LEFT = "-=left=-";
    private static final String COMMAND_UP = "-=up=-";
    private static final String COMMAND_RIGHT = "-=right=-";
    private static final String COMMAND_DOWN = "-=down=-";
    private static final String COMMAND_BACKSPACE = "-=bs=-";

    private InputConnection ic;

    public void setInputConnection(InputConnection inputConnection) {
        ic = inputConnection;
    }

    @Override
    public void onDataReceived(byte[] buffer, int bytes) {
        String s = new String(buffer, 0, bytes);
        l("commit \"" + s + "\" to " + ic.toString() + " from Inter " + this.toString());

        switch (s) {
            case COMMAND_LEFT:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                break;
            case COMMAND_UP:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                break;
            case COMMAND_DOWN:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                break;
            case COMMAND_RIGHT:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                break;
            case COMMAND_BACKSPACE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                break;
            default:
                ic.commitText(s, 1);//TODO cursor position
        }
    }

    private static void l(String text) {
        Log.d("LOG_TAG::Inter >>> ", text);
    }
}
