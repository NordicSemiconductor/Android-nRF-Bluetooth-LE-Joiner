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

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.nrfblejoiner.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;
import no.nordicsemi.android.nrfblejoiner.settings.AboutActivity;
import no.nordicsemi.android.nrfblejoiner.wifi.ConfigureWifiActivity;
import no.nordicsemi.android.nrfblejoiner.wifi.WifiFragmentDefault;
import no.nordicsemi.android.nrfblejoiner.wifi.WifiFragmentMessage;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity implements ConfigureBleFragment.BleConfigFragmentInteractionListener,
        WifiFragmentDefault.OnFragmentInteractionListener,
        WifiFragmentMessage.WifiFragmentMessageListener,
        PermissionRationaleFragment.PermissionDialogListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_LOCATION_SERVICES = 211;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 212;
    private static final long CONNECTION_TIMEOUT = 10000;
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final String TAG = "BLE";
    private final static ParcelUuid NODE_CONFIGURATION_SERVICE = ParcelUuid.fromString("54207799-8F40-4FE5-BEBE-6BB7022D3E73");
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BleDeviceAdapter mBleDeviceListAdapter;
    private BluetoothLeScannerCompat mScanner;
    private ArrayList<ScanFilter> mScanFilterList;
    private DatabaseHelper mDbHelper;
    private String mDeviceName, mDeviceAddress;
    private BLEService mBLEService;
    private ProgressDialog mProgressDialog;
    private ConfigureBleFragment mConfigureBleFragment = null;
    private RelativeLayout mRelativeLayout;
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequestBalancedPowerAccuracy;
    private boolean mLocationServicesRequestApproved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scanner);
        mDbHelper = new DatabaseHelper(this);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));
        mRelativeLayout = (RelativeLayout) findViewById(R.id.ble_rel);

        final ListView listViewDevices = (ListView)findViewById(R.id.listDevices);
        mProgressDialog =  new ProgressDialog(this);

        mBleDeviceListAdapter = new BleDeviceAdapter(this);
        mHandler = new Handler();
        listViewDevices.setAdapter(mBleDeviceListAdapter);

        prepareForScan();

    }

    private void prepareForScan(){
        if(isBleSupported()) {
            final ParcelUuid uuid = NODE_CONFIGURATION_SERVICE;
            mScanFilterList = new ArrayList<>();
            mScanFilterList.add(new ScanFilter.Builder().setServiceUuid(uuid).build());
            mScanner = BluetoothLeScannerCompat.getScanner();

            if (checkIfVersionIsMarshmallowOrAbove()) {
                startLocationModeChangeReceiver();
                connectToGoogleApiClient();
            } else {
                if (!isBleEnabled()) {
                    final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
                } else {
                    startLeScan();
                }
            }
        } else {
            showError(getString(R.string.ble_not_supported), false);
        }
    }

    private void connectToGoogleApiClient(){
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        else
            createLocationRequestForResult();
    }

    private void checkForLocationPermissionsAndScan(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (!isBleEnabled()) {
                final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
            } else {
                startLeScan();
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showPermissionRationaleFragment(R.string.rationale_location_message,REQUEST_ACCESS_COARSE_LOCATION);
                return;
            }
            onRequestPermission(REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private void showPermissionRationaleFragment(int resId, int permissionType){
        final PermissionRationaleFragment persmissionFragment = PermissionRationaleFragment.getInstance(resId, permissionType);
        persmissionFragment.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForLocationPermissionsAndScan();
                } else {
                    showError(getString(R.string.rationale_location_permission_denied), true);
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(checkIfVersionIsMarshmallowOrAbove()) {
            unregisterReceiver(mLocationProviderChangedReceiver);
            disconnectFromGoogleApiClient();
        }
    }

    private void disconnectFromGoogleApiClient(){
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isBleSupported())
            return false;

        // Inflate the menu; this adds items to the action bar if it is present.
        if(!mScanning) {
            getMenuInflater().inflate(R.menu.menu_start_scan, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_stop_scan, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case R.id.action_start_scan:
                if (checkIfVersionIsMarshmallowOrAbove()) {
                    if (mLocationServicesRequestApproved)
                        checkForLocationPermissionsAndScan();
                    else {
                        createLocationRequestForResult();
                    }
                } else {
                    if (!isBleEnabled()) {
                        final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
                    } else {
                        startLeScan();
                    }
                }
                return true;
            case R.id.action_stop_scan:
                stopLeScan();
                return true;
            case R.id.action_configure_wifi:
                if(mScanning)
                    stopLeScan();
                final Intent configureWifi = new Intent(this, ConfigureWifiActivity.class);
                startActivity(configureWifi);
                return true;
            case R.id.action_settings:
                if(mScanning)
                    stopLeScan();
                final Intent settings = new Intent(this, AboutActivity.class);
                startActivity(settings);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(mScanning)
            stopLeScan();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    startLeScan();
                }
                break;
            case REQUEST_LOCATION_SERVICES:
                if(resultCode == RESULT_OK){
                    mLocationServicesRequestApproved = true;
                    checkForLocationPermissionsAndScan();
                } else {
                    showPermissionRationaleFragment(R.string.rationale_location_message, REQUEST_LOCATION_SERVICES);
                }
                break;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEService.ACTION_CHARACTERISTIC_WRITE_COMPLETE);
        return intentFilter;
    }

    private boolean isBleEnabled(){
        boolean flag = false;
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter != null){
            flag = mBluetoothAdapter.isEnabled();
        }
        return flag;
    }

    private void startLeScan(){
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        // Refresh the devices list every second
                .setReportDelay(1000)
                        // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                        // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                    /*.setUseHardwareBatchingIfSupported(false)*/
                .build();
        mHandler.postDelayed(mStopScanningTask, SCAN_PERIOD);
        mScanning = true;
        mBleDeviceListAdapter.clear();
        mScanner.startScan(mScanFilterList, settings, scanCallback);
        invalidateOptionsMenu();
    }

    private void stopLeScan(){
        mHandler.removeCallbacks(mStopScanningTask);
        mScanning = false;
        mScanner.stopScan(scanCallback);
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            boolean newDeviceFound = false;
            for (final ScanResult result : results) {
                if (!mBleDeviceListAdapter.hasDevice(result)) {
                    newDeviceFound = true;
                    mBleDeviceListAdapter.addDevice(new ExtendedBluetoothDevice(result));
                }
            }

            if (newDeviceFound)
                mBleDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    public void configureBleNode(String opCode, String actionDelay, String identityModeDuration, String nextMode, String label) {
        mBLEService.writeSsidCharacteristic(opCode, actionDelay, identityModeDuration, nextMode, label);
    }

    @Override
    public void identifyMode() {
        mBLEService.mIdentifySelected = true;
        mBLEService.writeControlPointCharacteristic();
    }

    @Override
    public void advancedIdentifyMode(String opCode, String actionDelay, String identityModeDuration, String nextMode, String label) {
        mBLEService.mIdentifySelected = true;
        mBLEService.advancedIdentifyModeControlPointCharacteristics(opCode, actionDelay, identityModeDuration, nextMode, label);
    }

    @Override
    public void disconnectFromDevice() {
        mBLEService.disconnect();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mBleServiceConnection);
        if(mConfigureBleFragment != null) {
            mConfigureBleFragment.dismiss();
            mConfigureBleFragment = null;
        }
        mBleDeviceListAdapter.clear();
        mBleDeviceListAdapter.notifyDataSetChanged();
        Toast.makeText(this, getString(R.string.disconnected) + " " + mDeviceName, Toast.LENGTH_SHORT).show();
        checkForLocationPermissionsAndScan();
    }

    @Override
    public void connectToBleDevice() {
        showProgressDialog();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        final Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mBleServiceConnection, Context.BIND_AUTO_CREATE);
        mHandler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT);
    }

    @Override
    public void onWifiFragmentMessageDismiss() {
        startLeScan();
    }

    @Override
    public void onRequestPermission(int permissionType) {
        switch (permissionType) {
            case REQUEST_ACCESS_COARSE_LOCATION:
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
                break;
            case REQUEST_LOCATION_SERVICES:
                connectToGoogleApiClient();
                break;
        }
    }

    @Override
    public void onCancelRequestPermission() {
        showError(getString(R.string.rationale_location_cancel_message), true);
    }

    private void createLocationRequestForResult(){
        mLocationRequestBalancedPowerAccuracy = new LocationRequest();
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequestBalancedPowerAccuracy)
                .setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                Log.v("BLE", locationSettingsResult.getStatus().getStatusMessage());
                LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();
                if(states.isLocationUsable()) {
                    checkForLocationPermissionsAndScan();
                    return;
                }

                final Status status = locationSettingsResult.getStatus();
                switch(status.getStatusCode()){
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        mLocationServicesRequestApproved = false;
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SERVICES);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SUCCESS:
                        mLocationServicesRequestApproved = true;
                        checkForLocationPermissionsAndScan();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        showPermissionRationaleFragment(R.string.rationale_location_cancel_message, 0);
                        break;
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        createLocationRequestForResult();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    class BleDeviceAdapter extends BaseAdapter {

        private ArrayList<ExtendedBluetoothDevice> mDevices;
        private LayoutInflater mInflator;
        private Context context;

        public void addDevice(ExtendedBluetoothDevice device) {
            if (!mDevices.contains(device)) {
                mDevices.add(device);
            }
        }

        public BleDeviceAdapter(Context context) {
            super();
            mDevices = new ArrayList<>();
            this.context = context;
            mInflator = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mDevices.get(position).getBluetoothDevice();
        }

        public ExtendedBluetoothDevice getDevice(int position) {
            return mDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void clear() {
            mDevices.clear();
        }

        public boolean hasDevice(ScanResult result) {
            for (ExtendedBluetoothDevice device : mDevices) {
                if (device.matches(result))
                    return true;
            }
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ExtendedBluetoothDevice device = mDevices.get(position);
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHandler.removeCallbacks(mStopScanningTask);
                    if(mScanning)
                        stopLeScan();
                    if (mDbHelper.getAllWifiNetworks().size() > 0) {
                        if (isBleEnabled()) {
                            final ExtendedBluetoothDevice device = getDevice(position);
                            if (device == null) return;
                            mDeviceName = device.getName();
                            mDeviceAddress = device.getAddress();
                            if (mDbHelper.getDefaultWifiNetwork() == null) {
                                WifiFragmentDefault def = WifiFragmentDefault.newInstance();
                                def.show(getSupportFragmentManager(), null);
                            } else {
                                connectToBleDevice();
                            }
                        } else
                            Toast.makeText(context, context.getString(R.string.enable_bluetooth), Toast.LENGTH_SHORT).show();
                    } else {
                        WifiFragmentMessage wifiFragmentMessage = WifiFragmentMessage.newInstance();
                        wifiFragmentMessage.show(getSupportFragmentManager(), null);
                    }
                }
            });

            return convertView;
        }
    }

    private Runnable connectionTimeout = new Runnable() {
        @Override
        public void run() {
            if(mProgressDialog != null && mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
                disconnectFromDevice();
            }
        }
    };

    private Runnable mStopScanningTask = new Runnable() {
        @Override
        public void run() {
            stopLeScan();
        }
    };

    private void showProgressDialog(){
        mProgressDialog.setTitle("Connecting to device");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if(mBLEService != null){
                if(!mBLEService.initializeBluetooth())
                {
                    Log.v(TAG, "Unable to initialise bluetooth");
                }

                boolean flag = mBLEService.connect(mDeviceAddress);

                Log.v(TAG, "Connection...." + flag);
            } else {
                Log.v(TAG, "Unable to run ble service");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBLEService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                mProgressDialog.setTitle("Discovering services");
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if(mProgressDialog != null && mProgressDialog.isShowing())
                    mProgressDialog.dismiss();

                disconnectFromDevice();
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mProgressDialog.dismiss();
                mHandler.removeCallbacks(connectionTimeout);
                mConfigureBleFragment = ConfigureBleFragment.newInstance(mDeviceName, mDeviceAddress);
                mConfigureBleFragment.show(getSupportFragmentManager(), null);
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
            } else if (BLEService.ACTION_CHARACTERISTIC_WRITE_COMPLETE.equals(action)){

                if(mBLEService.mIdentifySelected)
                {
                    mBLEService.mIdentifySelected = false;
                    return;
                }

                if(mConfigureBleFragment != null){
                    mBLEService.disconnect();
                    unregisterReceiver(mGattUpdateReceiver);
                    unbindService(mBleServiceConnection);
                    mConfigureBleFragment.dismiss();
                    mConfigureBleFragment = null;
                }
            } else if (BLEService.ACTION_DISMISS_DIALOG.equals(action)) {
                if(mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
            } else if (BLEService.ACTION_GATT_ERROR.equals(action)){
                final Uri data = intent.getData();
                if(data != null){
                    Toast.makeText(MainActivity.this, "Gatt Error occurred: Error " + data.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private void showError(final String error, boolean setAction){

        final Snackbar snackbar = Snackbar
                .make(mRelativeLayout, error, Snackbar.LENGTH_LONG);
        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        final TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        if(setAction)
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorPrimary)).setAction(R.string.action_settings, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
        snackbar.show();
    }

    /**
      * Since Marshmallow location services must be enabled in order to scan.
      * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF. It always returns true on Android versions prior to Marshmellow.
      */
    public boolean isLocationEnabled(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;

            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (final Settings.SettingNotFoundException e) {
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return true;
    }

    final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(!isLocationEnabled()){
                stopLeScan();
            }
        }
    };

    private void startLocationModeChangeReceiver(){
        registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
    }

    private boolean checkIfVersionIsMarshmallowOrAbove(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            return true;
        }
        return false;
    }

    private boolean isBleSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }
}
