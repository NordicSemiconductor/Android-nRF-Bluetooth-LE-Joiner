/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfblejoiner.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;
import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.wifi.WifiNetwork;

public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final String TAG = "BLE";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int IDENTIFY_ME_OP_CODE = 3;
    private static final int IDENTIFY_ME_ACTION_DELAY = 2;
    private static final int IDENTIFY_ME_DURATION = 30;
    private static final int IDENTIFY_ME_CONFIGURATION = 0;

    public final static String ACTION_GATT_CONNECTED =
            "no.nordicsemi.android.nrfblejoiner.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "no.nordicsemi.android.nrfblejoiner.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "no.nordicsemi.android.nrfblejoiner.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "no.nordicsemi.android.nrfblejoiner.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "no.nordicsemi.android.nrfblejoiner.EXTRA_DATA";
    public final static String ACTION_CHARACTERISTIC_WRITE_COMPLETE =
            "no.nordicsemi.android.nrfblejoiner.ACTION_CHARACTERISTIC_WRITE_COMPLETE";
    public final static String ACTION_DISMISS_DIALOG =
            "no.nordicsemi.android.nrfblejoiner.ACTION_DISMISS_DIALOG";
    public final static String ACTION_GATT_ERROR =
            "no.nordicsemi.android.nrfblejoiner.ACTION_GATT_ERROR";
    private final static ParcelUuid NODE_CONFIGURATION_SERVICE = ParcelUuid.fromString("54207799-8F40-4FE5-BEBE-6BB7022D3E73");
    public final static UUID COMMISSIONNING_SSID_CHARACTERISTIC = UUID.fromString("542077A9-8F40-4FE5-BEBE-6BB7022D3E73");
    public final static UUID COMMISSIONNING_KEYS_STORE_CHARACTERISTIC = UUID.fromString("542077B9-8F40-4FE5-BEBE-6BB7022D3E73");
    public final static UUID COMMISSIONNING_CONTROL_POINT_CHARACTERISTIC = UUID.fromString("542077C9-8F40-4FE5-BEBE-6BB7022D3E73");

    //IOT characteristics
    private BluetoothGattCharacteristic mSsidCharacteristic;
    private BluetoothGattCharacteristic mKeyStoreCharacteristic;
    private BluetoothGattCharacteristic mControlPointCharacteristic;
    private boolean mRouterId = false;
    private boolean mSessionId = false;
    private boolean mControlPoint = false;
    private DatabaseHelper mDbHelper;
    private WifiNetwork mWifiNetwork;
    private String mOpCode;
    private String mIdentityModeDuration;
    private String deviceLabel;

    private final IBinder mBinder = new LocalBinder();
    private String mActionDelay;
    private String mNextMode;
    public boolean mIdentifySelected = false;

    public class LocalBinder extends Binder{
        BLEService getService() { return BLEService.this;}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDbHelper = new DatabaseHelper(this);
    }

    private final BluetoothGattCallback mBLEGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery");
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                close();
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "GAT success");
                final BluetoothGattService service = gatt.getService(NODE_CONFIGURATION_SERVICE.getUuid());
                if(service != null){
                    mSsidCharacteristic = service.getCharacteristic(COMMISSIONNING_SSID_CHARACTERISTIC);
                    mKeyStoreCharacteristic = service.getCharacteristic(COMMISSIONNING_KEYS_STORE_CHARACTERISTIC);
                    mControlPointCharacteristic = service.getCharacteristic(COMMISSIONNING_CONTROL_POINT_CHARACTERISTIC);
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(mIdentifySelected && status == 0){
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE_COMPLETE);
                return;
            }
            if(mRouterId && status == 0){
                mRouterId = false;
                writeKeystoreCharacteristic();
            } else if(mSessionId && status == 0){
                mSessionId = false;
                writeControlPointCharacteristic();
            } else if(mControlPoint && status == 0){
                mControlPoint = false;
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE_COMPLETE);
            } else {
                broadcastError(status);
            }
        }
    };

    private void broadcastError(final int error) {
        final Intent intent = new Intent(ACTION_GATT_ERROR);
        intent.putExtra(EXTRA_DATA, error);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }

        sendBroadcast(intent);
    }

    public boolean initializeBluetooth(){
        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null){
                Log.v(TAG, "BluetoothManager initialization failed");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.v(TAG, "BluetoothAdapter initalization failed");
            return false;
        }

        return true;
    }

    public boolean connect(final String address){
        if(mBluetoothAdapter == null || address == null){
            Log.v(TAG,"BluetoothAdapter is not initialised or wrong address");
            return false;
        }

        //Previously connected device. Tru to reconnect.
        if(address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null){
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice bleDevice = mBluetoothAdapter.getRemoteDevice(address);
        if(bleDevice == null){
            Log.v(TAG, "Device not found unable to connect");
            return false;
        }

        mBluetoothGatt = bleDevice.connectGatt(this, false, mBLEGattCallBack);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect(){
        if(mBluetoothAdapter == null || mBluetoothGatt == null){
            Log.v(TAG, "Bluetooth adapter not initialised");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close()
    {
        if(mBluetoothGatt == null)
            return;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void writeSsidCharacteristic(String opCode, String actionDelay, String identityModeTimeout, String nextMode, String deviceLabel) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(BLEService.this, getString(R.string.check_bluetooth), Toast.LENGTH_SHORT).show();
            return;
        }
        mOpCode = opCode;
        mIdentityModeDuration = identityModeTimeout;
        mActionDelay = actionDelay;
        mNextMode = nextMode;

        if(opCode.equalsIgnoreCase(getString(R.string.ble_identification_mode_value))){
            this.deviceLabel = deviceLabel;
        }
        mWifiNetwork = mDbHelper.getDefaultWifiNetwork();

        if(mSsidCharacteristic == null){
            Toast.makeText(this, getString(R.string.router_char_error), Toast.LENGTH_SHORT).show();
        }

        if(mWifiNetwork != null) {
            int length = mWifiNetwork.getSsid().getBytes().length;
            byte [] dataByte = new byte [length];
            System.arraycopy(mWifiNetwork.getSsid().getBytes(), 0, dataByte, 0, length);
            boolean flag = mSsidCharacteristic.setValue(dataByte);
            Log.v(TAG, "set value to characteristic: " + flag);
            mRouterId = mBluetoothGatt.writeCharacteristic(mSsidCharacteristic);
        }
    }

    public void writeKeystoreCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(BLEService.this, getString(R.string.check_bluetooth), Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] dataByte = mWifiNetwork.getPassword().getBytes();
        mKeyStoreCharacteristic.setValue(dataByte);
        mSessionId = mBluetoothGatt.writeCharacteristic(mKeyStoreCharacteristic);
    }

    public void writeControlPointCharacteristic() {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(BLEService.this, getString(R.string.check_bluetooth), Toast.LENGTH_SHORT).show();
            return;
        }

        if(mIdentifySelected){
            final byte[] serviceData = new byte[10];
            ByteBuffer bb = ByteBuffer.wrap(serviceData);
            bb.order(ByteOrder.BIG_ENDIAN);
            bb.put(((byte) (IDENTIFY_ME_OP_CODE)));
            bb.putInt(IDENTIFY_ME_ACTION_DELAY);
            bb.putInt(IDENTIFY_ME_DURATION);
            bb.put((byte) IDENTIFY_ME_CONFIGURATION);
            byte[] data = bb.array();
            mControlPointCharacteristic.setValue(data);
            mControlPoint = mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic);
            return;
        }

        final byte[] serviceData = new byte[10];
        ByteBuffer bb = ByteBuffer.wrap(serviceData);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(((byte) Integer.parseInt(mOpCode)));
        bb.putInt(Integer.parseInt(mActionDelay));
        bb.putInt(Integer.parseInt(mIdentityModeDuration));
        bb.put((byte) Integer.parseInt(mNextMode));
        byte[] data = bb.array();
        mControlPointCharacteristic.setValue(data);
        mControlPoint = mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic);

    }

    public void advancedIdentifyModeControlPointCharacteristics(String opCode, String actionDelay, String identityModeDuration, String nextMode, String label) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(BLEService.this, getString(R.string.check_bluetooth), Toast.LENGTH_SHORT).show();
            return;
        }

        final byte[] serviceData = new byte[18];
        ByteBuffer bb = ByteBuffer.wrap(serviceData);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) Integer.parseInt(opCode));
        bb.putInt(Integer.parseInt(actionDelay));
        bb.putInt(Integer.parseInt(identityModeDuration));
        bb.put((byte) Integer.parseInt(nextMode));
        if(!label.equalsIgnoreCase("")) {
            bb.put(label.getBytes());
            byte[] data = bb.array();
            mControlPointCharacteristic.setValue(data);
            mControlPoint = mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic);
        } else {
            byte[] data = bb.array();
            byte [] finalData = new byte[10];
            System.arraycopy(data, 0, finalData, 0, 10);
            mControlPointCharacteristic.setValue(finalData);
            mControlPoint = mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic);
        }
    }
}