package com.shurman.remkey;

import android.content.Context;
import android.view.View;

public interface KeyboardLayoutManager {
    View reInstateKeyboard(Context context);
    byte[] letterPressed(String code);
    byte[] symbolPressed(String code);
    byte[] spacerPressed(String code);
    byte[] commandPressed(String code);
    void shiftPressed();
}
