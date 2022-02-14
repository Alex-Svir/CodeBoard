package com.shurman.remkeyime.qbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;
//TODO cancelDiscovery to ANY start, connect, close etc.
public class QuickBluetooth {
    public interface DataReceivedCallback {
        void onDataReceived(byte[] buffer, int bytes);
    }

    public interface ConnectionControl {
        void onConnectionCreated(/*Connection connection*/);
        void onConnectionFailed();
        void onConnectionStarted();
        void onConnectionStopped();
        void onConnectionClosed();
    }

    public interface OnStateChangedListener {
        void onStateChanged(int newState, int previousState);
    }

    public interface OnConnectionStateChangedListener {
        void onConnectionStateChanged(int newState, int previousState);
    }

    private enum Action {
        START_SERVER,
        CONNECT_TO_REMOTE_DEVICE
    }

    public enum State {
        OFF,
        IDLE,
        ACCEPTING,
        CONNECTING,
        CONNECTED
    }

    private final BluetoothAdapter btAdapter;
    private final String _serviceName;
    private final UUID _uuid;
    private final ReAction reAction;
    private OnStateChangedListener onStateChangedListener;
    private OnConnectionStateChangedListener onConnectionStateChangedListener;
    //private ConnectionControl connectionControl;
    private final ConnectionControlHandler ccHandler;

    public QuickBluetooth(Context context, String serviceName, UUID uuid, int bufferSize) {
        _serviceName = serviceName;
        _uuid = uuid;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        context.registerReceiver(receiver, filterStateControl);
        context.registerReceiver(receiver, filterConnStateControl);
        ccHandler = new ConnectionControlHandler();
        reAction = new ReAction();
        Connection.setBufferSize(bufferSize);
    }

    public QuickBluetooth(Context context, String serviceName, UUID uuid) {
        this(context, serviceName, uuid, Connection.DEFAULT_BUFFER_SIZE);
    }

    //**********************************************************************************************
    //PUBLIC BLOCK

    public void startServerWithCheck(Context context, boolean requestOn, DataReceivedCallback listener) {
        if (btReady()) {
            checkToStartServer(listener);
        } else {
            close();
            if (requestOn) {
                reAction.activateAsSever(listener);
                btRequestOn(context);
            }
        }
    }

    public void forceReStartServer(Context context, boolean requestOn, DataReceivedCallback listener) {
        close();
        if (btReady())
            Server.accept(btAdapter, _serviceName, _uuid, ccHandler, listener);
        else if (requestOn) {
            reAction.activateAsSever(listener);
            btRequestOn(context);
        }
    }

    public void connectWithCheck(Context context, boolean requestOn, BluetoothDevice device, DataReceivedCallback listener) {
        if (btReady()) {
            checkToAttemptConnection(device, listener);
        } else {
            close();
            if (requestOn) {
                reAction.activateAsClient(device, listener);
                btRequestOn(context);
            }
        }
    }

    public void connectWithCheck(Context context, boolean requestOn, String macAddress, DataReceivedCallback listener) {
        BluetoothDevice device = getBluetoothDeviceByMacAddressOrNull(macAddress);
        if (device != null)
            connectWithCheck(context, requestOn, device, listener);
    }

    public void forceReConnect(Context context, boolean requestOn, BluetoothDevice device, DataReceivedCallback listener) {
        close();
        if (btReady())
            Client.connect(device, _uuid, ccHandler, listener);
        else if (requestOn) {
            reAction.activateAsClient(device, listener);
            btRequestOn(context);
        }
    }

    public void forceReConnect(Context context, boolean requestOn, String macAddress, DataReceivedCallback listener) {
        BluetoothDevice device = getBluetoothDeviceByMacAddressOrNull(macAddress);
        if (device != null)
            forceReConnect(context, requestOn, device, listener);
    }

    public void switchAsServerIfEnabled(DataReceivedCallback listener) {
        if (!btReady()) return;
        if (!Connection.inactive() || !Server.inactive() || !Client.inactive()) {
            close();
        } else {
            btAdapter.cancelDiscovery();
            Server.accept(btAdapter, _serviceName, _uuid, ccHandler, listener);
        }
    }

    public void switchAsClientIfEnabled(BluetoothDevice device, DataReceivedCallback listener) {
        if (!btReady()) return;
        if (!Connection.inactive() || !Server.inactive() || !Client.inactive()) {
            close();
        } else {
            btAdapter.cancelDiscovery();
            Client.connect(device, _uuid, ccHandler, listener);
        }
    }

    public void switchAsClientIfEnabled(String macAddress, DataReceivedCallback listener) {
        BluetoothDevice device = getBluetoothDeviceByMacAddressOrNull(macAddress);
        if (device != null)
            switchAsClientIfEnabled(device, listener);
    }

    public State getQuickState() {
        if (!btReady()) return State.OFF;
        if (!Connection.inactive()) return State.CONNECTED;
        if (!Client.inactive()) return State.CONNECTING;
        if (!Server.inactive()) return State.ACCEPTING;
        return State.IDLE;
    }

    public void write(byte[] buffer) {
        Connection.write(buffer);
    }

    public Set<BluetoothDevice> getPairedDevicesList() {
        return btAdapter.getBondedDevices();
    }

    public String getConnectedDeviceName() {
        return Connection.getDeviceName();
    }

    public String getConnectedDeviceAddress() {
        return Connection.getDeviceAddress();
    }

    public void close() {
        btAdapter.cancelDiscovery();
        Server.close();
        Client.close();
        Connection.close();
    }

    public void kill(Context context) {
        close();
        context.unregisterReceiver(receiver);
    }

    public boolean turnOn(Context context) {
        if (btReady()) return false;
        btRequestOn(context);
        return true;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.onStateChangedListener = listener;
    }

    public void setOnConnectionStateChangedListener(OnConnectionStateChangedListener listener) {
        this.onConnectionStateChangedListener = listener;
    }

    public void setConnectionControlCallback(ConnectionControl callback) {
        ccHandler.setConnectionControlCallback(callback);
    }


    //PUBLIC BLOCK
    //**********************************************************************************************
    //SERVICE BLOCK
    private void checkToStartServer(DataReceivedCallback listener) {
        btAdapter.cancelDiscovery();
        if (!Client.inactive()) {
            close();
            Server.accept(btAdapter, _serviceName, _uuid, ccHandler, listener);
            return;
        }
        if (Server.inactive() && Connection.inactive())
            Server.accept(btAdapter, _serviceName, _uuid, ccHandler, listener);
    }

    private void checkToAttemptConnection(BluetoothDevice device, DataReceivedCallback listener) {
        btAdapter.cancelDiscovery();
        if (!Server.inactive()) {
            close();
            Client.connect(device, _uuid, ccHandler, listener);
            return;
        }
        if (Client.inactive() && Connection.inactive())
            Client.connect(device, _uuid, ccHandler, listener);
    }

    private boolean btReady() {
        if (btAdapter == null) return false;
        return btAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private void btRequestOn(Context context) {
        Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(in);
    }

    private BluetoothDevice getBluetoothDeviceByMacAddressOrNull(String macAddress) {
        for (BluetoothDevice bd : getPairedDevicesList()) {
            if (bd.getAddress().equalsIgnoreCase(macAddress))
                return bd;
        }
        return null;
    }

    //SERVICE BLOCK
    //**********************************************************************************************

    private class ReAction {
        private boolean active = false;
        private Action action;
        private BluetoothDevice device;
        private DataReceivedCallback listener;

        public void activateAsClient(BluetoothDevice device, DataReceivedCallback listener) {
            this.device = device;
            this.listener = listener;
            action = Action.CONNECT_TO_REMOTE_DEVICE;
            active = true;
        }

        public void activateAsSever(DataReceivedCallback listener) {
            this.listener = listener;
            action = Action.START_SERVER;
            active = true;
        }

        public void trigger(Context context) {
            if (!active) return;
            switch (action) {
                case CONNECT_TO_REMOTE_DEVICE:
                    QuickBluetooth.this.connectWithCheck(context, true, device, listener);
                    break;
                case START_SERVER:
                    QuickBluetooth.this.startServerWithCheck(context, true, listener);
                    break;
            }
            active = false;
        }
    }

    IntentFilter filterStateControl = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    IntentFilter filterConnStateControl = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        int state=-1, prev_state=-1, conn_state=-1, prev_conn_state=-1;

        @Override
        public void onReceive(Context context, Intent intent) {
            int new_value, prev_value;
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                new_value=intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                prev_value=intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                if (state == new_value && prev_state == prev_value) return;
                if (onStateChangedListener != null)
                    onStateChangedListener.onStateChanged(state = new_value, prev_state = prev_value);
                if (new_value == BluetoothAdapter.STATE_ON)
                    reAction.trigger(context);
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                new_value=intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                prev_value=intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, -1);
                if (conn_state == new_value && prev_conn_state == prev_value) return;
                if (onConnectionStateChangedListener != null)
                    onConnectionStateChangedListener.onConnectionStateChanged(conn_state = new_value,
                            prev_conn_state = prev_value);
            }
        }
    };

    private static class ConnectionControlHandler extends Handler {//TODO
        private static final int CALLBACK_CREATED = 0;
        private static final int CALLBACK_FAILED = 1;
        private static final int CALLBACK_STARTED = 2;
        private static final int CALLBACK_STOPPED = 3;
        private static final int CALLBACK_CLOSED = 4;

        private WeakReference<ConnectionControl> wRef;

        public void setConnectionControlCallback(ConnectionControl connectionControl) {
            wRef = new WeakReference<>(connectionControl);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (wRef == null) return;
            ConnectionControl callback = wRef.get();
            if (callback == null) return;
            switch (msg.what) {
                case CALLBACK_CREATED:
                    callback.onConnectionCreated();
                    break;
                case CALLBACK_FAILED:
                    callback.onConnectionFailed();
                    break;
                case CALLBACK_STARTED:
                    callback.onConnectionStarted();
                    break;
                case CALLBACK_STOPPED:
                    callback.onConnectionStopped();
                    break;
                case CALLBACK_CLOSED:
                    callback.onConnectionClosed();
                    break;
            }
        }
    }

    private static class Server extends Thread {
        private static Server instance = null;

        public static void accept(BluetoothAdapter adapter,
                                  String name,
                                  UUID uuid,
                                  ConnectionControlHandler handler,
                                  DataReceivedCallback listener) {
            if (instance != null) return;
            try {
                instance = new Server(adapter, name, uuid, handler, listener);
                instance.start();
            } catch (IOException e) {}
        }
        public static void close() {
            if (instance == null) return;
            try { instance.btServer.close(); }
            catch (IOException e) {}
        }

        public static boolean inactive() {
            return instance == null;
        }

        private final BluetoothServerSocket btServer;
        private final DataReceivedCallback listener;
        private final ConnectionControlHandler handler;

        private Server(BluetoothAdapter adapter,
                       String name,
                       UUID uuid,
                       ConnectionControlHandler handler,
                       DataReceivedCallback listener) throws IOException {
            btServer = adapter.listenUsingRfcommWithServiceRecord(name, uuid);
            this.listener = listener;
            this.handler = handler;
        }

        private void onSuccess(BluetoothSocket socket) {
            if (Connection.create(socket, handler, listener))
                handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_CREATED);
            else
                handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_FAILED);
        }

        private void onFail() {
            handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_FAILED);
        }

        @Override
        public void run() {
            BluetoothSocket socket;

            while (true) {
                try {
                    socket = btServer.accept();
                } catch (IOException e) {
                    l("Server failed with exception: " + e.toString());
                    onFail();
                    break;
                }
                if (socket != null) {
                    onSuccess(socket);
                    break;
                }
            }

            try { btServer.close(); }
            catch (IOException e) {}
            instance = null;
            l("Server CLOSED");
        }
    }

    private static class Client extends Thread {
        private static Client instance = null;

        public static void connect(BluetoothDevice device,
                                   UUID uuid,
                                   ConnectionControlHandler handler,
                                   DataReceivedCallback listener) {
            if (instance != null) return;
            try {
                instance = new Client(device, uuid, handler, listener);
                instance.start();
            } catch (IOException e) {}
        }

        public static void close() {
            if (instance == null) return;
            try { instance.socket.close(); }
            catch (IOException e) {}
        }

        public static boolean inactive() {
            return instance == null;
        }

        private final BluetoothSocket socket;
        private final DataReceivedCallback listener;
        private final ConnectionControlHandler handler;

        private Client(BluetoothDevice device,
                       UUID uuid,
                       ConnectionControlHandler handler,
                       DataReceivedCallback listener) throws IOException {
            socket = device.createRfcommSocketToServiceRecord(uuid);
            this.listener = listener;
            this.handler = handler;
        }

        private void onSuccess() {
            if (Connection.create(socket, handler, listener))
                handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_CREATED);
            else
                handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_FAILED);
        }

        private void onFail() {
            handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_FAILED);
        }

        @Override
        public void run() {
            try {
                socket.connect();
                onSuccess();
            }
            catch (IOException e) {
                l("Client failed with exception: " + e.toString());
                try { socket.close(); }
                catch (IOException ee) {}
                onFail();
            }
            instance = null;
        }
    }

    private static class Connection extends Thread {
        public static final int DEFAULT_BUFFER_SIZE = 1024;
        private static int buf_size = DEFAULT_BUFFER_SIZE;

        private static Connection instance = null;

        public static boolean create(BluetoothSocket socket,
                                     ConnectionControlHandler handler,
                                     DataReceivedCallback listener) {
            if (instance != null) return false;
            try {
                instance = new Connection(socket, handler, listener);
                instance.start();
                return true;
            } catch (IOException e) {
                try { instance.os.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
                try { instance.is.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
                try {instance.sock.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
            }
            return false;
        }

        public static void close() {
            if (instance == null) return;
            try { instance.os.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
            try { instance.is.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
            try {instance.sock.close(); } catch (IOException ioe) {} catch (NullPointerException npe) {}
        }

        private static void setBufferSize(int bufferSize) {
            buf_size = bufferSize;
        }

        public static boolean inactive() {
            return instance == null;
        }

        private final BluetoothSocket sock;
        private final OutputStream os;
        private final InputStream is;
        private final DataReceivedCallback listener;
        private final ConnectionControlHandler handler;

        private Connection(BluetoothSocket socket,
                           ConnectionControlHandler handler,
                           DataReceivedCallback listener) throws IOException {
            sock = socket;
            os = sock.getOutputStream();
            is = sock.getInputStream();
            this.listener = listener;
            this.handler = handler;
        }

        public static String getDeviceName() {
            if (instance == null) return "";
            if (instance.sock == null) return "";
            BluetoothDevice device = instance.sock.getRemoteDevice();
            if (device == null) return "";
            return device.getName();
        }

        public static String getDeviceAddress() {
            if (instance == null) return "";
            if (instance.sock == null) return "";
            BluetoothDevice device = instance.sock.getRemoteDevice();
            if (device == null) return "";
            return device.getAddress();
        }

        public static void write(byte[] buf) {
            if (instance == null) return;
            try { instance.os.write(buf); }
            catch (IOException e) {}
        }

        private void send(byte[] buf, int length) {
            if (listener != null) listener.onDataReceived(buf, length);
        }

        private void started() {
            handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_STARTED);
        }
        private void stopped() {
            handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_STOPPED);
        }
        private void closed() {
            handler.sendEmptyMessage(ConnectionControlHandler.CALLBACK_CLOSED);
        }

        @Override
        public void run() {
            started();
            byte[] buffer = new byte[buf_size];
            int read;
            while (true) {
                try {
                    read = is.read(buffer);
                    send(buffer, read);
                } catch (IOException e) {
                    break;
                }
            }
            stopped();
            try { is.close(); } catch (IOException e) {}
            try { os.close(); } catch (IOException e) {}
            try { sock.close(); } catch (IOException e) {}
            instance = null;
            closed();
        }
    }

    private static void l(String text) {//TODO
        Log.d("LOG_TAG::QBt >>>> ", text);
    }
}
