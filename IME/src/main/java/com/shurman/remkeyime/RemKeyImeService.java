package com.shurman.remkeyime;

import android.bluetooth.BluetoothAdapter;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.shurman.remkeyime.qbluetooth.QuickBluetooth;

import java.util.UUID;

public class RemKeyImeService extends InputMethodService
        implements View.OnClickListener,
        QuickBluetooth.OnStateChangedListener,
        QuickBluetooth.ConnectionControl        {

    private static final String SERVICE_NAME = "RemKeyImeService";
    public static final UUID SERVICE_UUID = UUID.fromString("af4d04d6-064a-482a-9ed2-6b517bdf425a");

    private QuickBluetooth qb;
    private Interpreter inter;
    private ImageView btBT;
    private TextView tvDev;

    @Override
    public void onCreate() {
        super.onCreate();

        qb = new QuickBluetooth(this, SERVICE_NAME, SERVICE_UUID, 32);//TODO buf size
        qb.setConnectionControlCallback(this);
        qb.setOnStateChangedListener(this);
        inter = new Interpreter();

        l("On Create");
    }

    @Override
    public View onCreateInputView() {
        View input = getLayoutInflater().inflate(R.layout.input_bar, null);
        btBT = input.findViewById(R.id.button);
        btBT.setOnClickListener(this);
        tvDev = input.findViewById(R.id.text);

        l("On CreateInputView");
        return input;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        inter.setInputConnection(getCurrentInputConnection());

        qb.startServerWithCheck(getApplicationContext(), true, inter);
        setColor();

        l("OnStart");
    }

    @Override
    public void onClick(View v) {
        qb.forceReStartServer(this, true, inter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        qb.kill(this);
        l("On Destroy");
    }

    private void setColor() {
        int colorRes = R.color.bt_off;
        switch (qb.getQuickState()) {
            case IDLE:
            case ACCEPTING:
                colorRes = R.color.bt_wait;
                break;
            case CONNECTED:
                colorRes = R.color.bt_conn;
                break;
        }
        ((GradientDrawable) btBT.getBackground()).setColor(getResources().getColor(colorRes));
        tvDev.setText(qb.getConnectedDeviceName());
    }

    @Override
    public void onStateChanged(int newState, int previousState) {
        if (newState == BluetoothAdapter.STATE_ON)
            qb.startServerWithCheck(this, true, inter);
        else if (newState == BluetoothAdapter.STATE_OFF) {
            qb.close();
            setColor();
        }
        l("State:" + previousState + " -> " + newState);
    }

    @Override
    public void onConnectionCreated() {
        setColor();
        l("Connection Created");
    }

    @Override
    public void onConnectionClosed() {
        qb.startServerWithCheck(this, false, inter);
        setColor();
        l("Connection Closed");
    }

    @Override
    public void onConnectionFailed() { l("Connection Failed"); }

    @Override
    public void onConnectionStarted() { l("Connection Started"); }

    @Override
    public void onConnectionStopped() { l("Connection Stopped"); }

    private static void l(String text) {//TODO
        Log.d("LOG_TAG::RKIS: >>> ", text);
    }
}
