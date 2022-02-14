package com.shurman.remkey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.shurman.remkey.qbluetooth.QuickBluetooth;

import java.util.UUID;

public class RemKeyActivity extends AppCompatActivity
            implements QuickBluetooth.ConnectionControl, QuickBluetooth.OnStateChangedListener {
    public static final String SERVICE_NAME = "RemKeyService";
    public static final UUID SERVICE_UUID = UUID.fromString("af4d04d6-064a-482a-9ed2-6b517bdf425a");

    private ImageView bluetoothButton;
    private Spinner spinner;
    private QuickBluetooth qBluetooth;
    private KeyboardLayoutManager keyboard;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remkey);

        RemKeyActivityViewModel viewModel = new ViewModelProvider(this)
                                                .get(RemKeyActivityViewModel.class);
        qBluetooth = viewModel.getBluetooth();
        qBluetooth.setConnectionControlCallback(this);
        qBluetooth.setOnStateChangedListener(this);
        keyboard = viewModel.getKeyboard();

        bluetoothButton = findViewById(R.id.button);

        spinner = findViewById(R.id.spinner);
        DevicesAdapter adapter = new DevicesAdapter(this,
                                viewModel.getBluetooth().getPairedDevicesList());
        spinner.setAdapter(adapter);
        setSpinnerSelectionInAccordanceToSettings();

        ((LinearLayout) findViewById(R.id.keyboard)).addView(
                keyboard.reInstateKeyboard(this)
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        setIndicator();
    }

    public void btClick(View v) {
        if (qBluetooth.turnOn(this)) return;
        qBluetooth.switchAsClientIfEnabled(
                                (BluetoothDevice)spinner.getSelectedItem(),
                                null
        );
        setIndicator();
    }

    private void setIndicator() {
        int colorRes = R.color.bt_off;
        switch (qBluetooth.getQuickState()) {
            case IDLE:
                colorRes = R.color.bt_idling;
                spinner.setEnabled(true);
                break;
            case CONNECTING:
                colorRes = R.color.bt_attempt;
                spinner.setEnabled(false);
                break;
            case CONNECTED:
                colorRes = R.color.bt_connected;
                spinner.setEnabled(false);
                break;
        }
        ((GradientDrawable) bluetoothButton.getBackground())
                                .setColor( getResources().getColor(colorRes) );
    }

//------------------------------------------------------------------------------------------------
    public void onLetterClick(View view) {
        qBluetooth.write(
                keyboard.letterPressed( ((Button) view).getText().toString() )
        );
    }

    public void onSymbolClick(View view) {
        qBluetooth.write(
                keyboard.symbolPressed( ((Button) view).getText().toString() )
        );
    }

    public void onCommandClick(View view) {
        qBluetooth.write(
                keyboard.commandPressed( view.getTag().toString() )
        );
    }

    public void onSpacerClick(View view) {
        qBluetooth.write(
                keyboard.spacerPressed( view.getTag().toString() )
        );
    }

    public void onShiftClick(View view) {
        keyboard.shiftPressed();
    }

//------------------------------------------------------------------------------------------------
    @Override
    public void onConnectionCreated() {
        setIndicator();
        AppPreferences.saveLastConnectedMac(this,
                qBluetooth.getConnectedDeviceAddress()
        );
        l("Connection Created");
    }

    @Override
    public void onConnectionFailed() {
        qBluetooth.close();
        setIndicator();
        l("Connection Failed");
    }

    @Override
    public void onConnectionClosed() {
        qBluetooth.close();
        setIndicator();
        l("Connection Closed");
    }

    @Override
    public void onConnectionStarted() { l("Connection Started"); }

    @Override
    public void onConnectionStopped() { l("Connection Stopped"); }

    @Override
    public void onStateChanged(int newState, int previousState) {
        if (newState == BluetoothAdapter.STATE_ON) {
            setIndicator();
        } else if (newState == BluetoothAdapter.STATE_OFF) {
            qBluetooth.close();
            setIndicator();
        }
        l(previousState + "->" + newState);
    }

    private void setSpinnerSelectionInAccordanceToSettings() {
        int position = ((DevicesAdapter) spinner.getAdapter()).getItemPositionByMacAddress(
                AppPreferences.getKeyLastConnectedMac(this)
        );
        if (-1 == position) position++;
        spinner.setSelection(position);
    }

    private static void l(String text) {//TODO
        Log.d("LOG_TAG::RKA >>> ", text);
    }
}
