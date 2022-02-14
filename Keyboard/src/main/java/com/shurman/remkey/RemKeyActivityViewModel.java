package com.shurman.remkey;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.shurman.remkey.qbluetooth.QuickBluetooth;

import org.jetbrains.annotations.NotNull;

public class RemKeyActivityViewModel extends AndroidViewModel {
    private MutableLiveData<QuickBluetooth> bluetooth;
    private MutableLiveData<KeyboardLayoutManager> keyboard;

    public RemKeyActivityViewModel(@NonNull @NotNull Application application) {
        super(application);
    }

    public QuickBluetooth getBluetooth() {
        if (null == bluetooth) {
            bluetooth = new MutableLiveData<>();
            bluetooth.setValue(new QuickBluetooth( getApplication(),
                                    RemKeyActivity.SERVICE_NAME,
                                    RemKeyActivity.SERVICE_UUID)
            );
        }
        return bluetooth.getValue();
    }

    public KeyboardLayoutManager getKeyboard() {
        if (null == keyboard) {
            keyboard = new MutableLiveData<>();
            keyboard.setValue(new KeyboardBasic());
        }
        return keyboard.getValue();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getBluetooth().kill(getApplication());
    }
}
