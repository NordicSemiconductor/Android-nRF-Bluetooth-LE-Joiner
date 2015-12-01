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

package no.nordicsemi.android.nrfblejoiner.wifi;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;
import no.nordicsemi.android.nrfblejoiner.settings.AboutActivity;
import no.nordicsemi.android.nrfblejoiner.widgets.DividerItemDecoration;
import no.nordicsemi.android.nrfblejoiner.widgets.ItemClickHelperAdapter;
import no.nordicsemi.android.nrfblejoiner.widgets.SimpleItemTouchHelperCallBack;

public class ConfigureWifiActivity extends AppCompatActivity implements WifiFragmentAddFromScan.OnFragmentInteractionListener,WifiFragmentEdit.onWifiEditListener, WifiFragmentSelect.returnAddWifiMethodChoice, WifiFragmentAddManually.WifiInformation {

    private WifiManager wifiManager;
    private ArrayList<String> wifis;
    private WifiScanReceiver wifiReciever;
    private ProgressDialog wifiProgress;
    private ArrayList<WifiNetwork> otherWifiNetworks = new ArrayList<>();
    private ArrayList<WifiNetwork> defaultWifiNetwork = new ArrayList<>();
    private ArrayList<WifiNetwork> wifiNetworks = new ArrayList<>();
    private WifiRecyclerAdapter defaultWifiRecyclerAdapter;
    private WifiRecyclerAdapter otherWifiRecyclerAdapter;
    private RecyclerView defaultRecyclerView;
    private DatabaseHelper dbHelper;
    private ItemClickHelperAdapter helper;
    private LinearLayout layoutWifiContent;
    private RelativeLayout layoutNoWifiContent;
    private TextView tvNDefaultWifi;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_wifi);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        defaultRecyclerView = (RecyclerView) findViewById(R.id.defaultList);

        final RecyclerView otherRecyclerView = (RecyclerView) findViewById(R.id.otherlist);
        tvNDefaultWifi = (TextView) findViewById(R.id.tv_no_default_wifi);
        layoutWifiContent = (LinearLayout) findViewById(R.id.layout_wifi_content);
        layoutNoWifiContent = (RelativeLayout) findViewById(R.id.layout_no_wifi);
        wifiManager =(WifiManager)getSystemService(Context.WIFI_SERVICE);

        dbHelper = new DatabaseHelper(this);
        prepareWifiNetworkList();

        helper = new ItemClickHelperAdapter() {
            @Override
            public void onWifiItemClick(WifiNetwork wifiNetwork) {
                WifiFragmentEdit wifiFragmentEdit = WifiFragmentEdit.newInstance(wifiNetwork.getId(), wifiNetwork.getSsid(), wifiNetwork.getPassword(), wifiNetwork.isDefaultRouter());
                wifiFragmentEdit.show(getSupportFragmentManager(), null);
            }

            @Override
            public void updateUiOnItemDismiss() {
                prepareWifiNetworkList();
            }
        };

        defaultWifiRecyclerAdapter = new WifiRecyclerAdapter(this, defaultWifiNetwork, helper);
        defaultRecyclerView.setAdapter(defaultWifiRecyclerAdapter);
        defaultRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ItemTouchHelper.Callback itemTouchHelperCallback = new SimpleItemTouchHelperCallBack(defaultWifiRecyclerAdapter);
        final ItemTouchHelper touchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        touchHelper.attachToRecyclerView(defaultRecyclerView);
        defaultRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        otherWifiRecyclerAdapter = new WifiRecyclerAdapter(this, otherWifiNetworks, helper);
        otherRecyclerView.setAdapter(otherWifiRecyclerAdapter);
        otherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ItemTouchHelper.Callback itemTouchHelperCallback1 = new SimpleItemTouchHelperCallBack(otherWifiRecyclerAdapter);
        final ItemTouchHelper touchHelper1 = new ItemTouchHelper(itemTouchHelperCallback1);
        touchHelper1.attachToRecyclerView(otherRecyclerView);
        otherRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

    }

    private void prepareWifiNetworkList(){
        wifiNetworks = dbHelper.getAllWifiNetworks();
        defaultWifiNetwork.clear();
        otherWifiNetworks.clear();
        for(int i = 0; i < wifiNetworks.size(); i++){
            if(wifiNetworks.get(i).isDefaultRouter().equalsIgnoreCase("true"))
                defaultWifiNetwork.add(wifiNetworks.get(i));
            else
                otherWifiNetworks.add(wifiNetworks.get(i));
        }
        setUi();
    }

    private void setUi(){
        if (wifiNetworks.size()==0){
            layoutWifiContent.setVisibility(View.GONE);
            layoutNoWifiContent.setVisibility(View.VISIBLE);
        } else {
            layoutWifiContent.setVisibility(View.VISIBLE);
            layoutNoWifiContent.setVisibility(View.GONE);

            if(checkIfDefaultWifiExist()){
                tvNDefaultWifi.setVisibility(View.GONE);
                defaultRecyclerView.setVisibility(View.VISIBLE);
            }
            else {
                tvNDefaultWifi.setVisibility(View.VISIBLE);
                defaultRecyclerView.setVisibility(View.GONE);
            }
        }
    }

    private boolean checkIfDefaultWifiExist(){
        for(int i = 0; i < wifiNetworks.size(); i++){
            if(wifiNetworks.get(i).isDefaultRouter().equalsIgnoreCase("true")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_configure_wifi, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId())
        {
            case R.id.action_configure_wifi:
                final WifiFragmentSelect wifiFragmentSelect = WifiFragmentSelect.newInstance();
                wifiFragmentSelect.show(getSupportFragmentManager(), null);
                break;
            case R.id.action_settings:
                final Intent settings = new Intent(this, AboutActivity.class);
                startActivity(settings);
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWifiNetworkSavedToDb() {
        prepareWifiNetworkList();
        defaultWifiRecyclerAdapter.notifyDataSetChanged();
        otherWifiRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEditWifiNetwork() {

        prepareWifiNetworkList();
        defaultWifiRecyclerAdapter.notifyDataSetChanged();
        otherWifiRecyclerAdapter.notifyDataSetChanged();

    }

    private boolean scanWifiNetworks(){
        final boolean isAirplaneEnabled = Settings.System.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if((isAirplaneEnabled && wifiManager.isWifiEnabled()) || wifiManager.isWifiEnabled()) {
            wifiReciever = new WifiScanReceiver();
            registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            showWifiProgressbar();
            this.wifiManager.startScan();
            return true;
        } else return false;
    }

    @Override
    public boolean wifiAddMethod(boolean scanForWifi) {
        //if scanForWifi is true start scanner else add wifi network manually
        if(scanForWifi){
            return scanWifiNetworks();
        }else{
            final WifiFragmentAddManually addManually = WifiFragmentAddManually.newInstance();
            addManually.show(getSupportFragmentManager(),null);
            return true;
        }
    }

    @Override
    public void returnWifiInformation() {
        prepareWifiNetworkList();
        defaultWifiRecyclerAdapter.notifyDataSetChanged();
        otherWifiRecyclerAdapter.notifyDataSetChanged();
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            wifiProgress.dismiss();
            final List<ScanResult> wifiScanList = wifiManager.getScanResults();
            wifis = new ArrayList<>();
            ScanResult scanResult;

            for(int i = 0; i < wifiScanList.size(); i++) {
                scanResult = wifiScanList.get(i);
                if (!scanResult.SSID.isEmpty()) {
                    wifis.add((wifiScanList.get(i)).SSID.trim());
                }
            }

            unregisterReceiver(wifiReciever);
            final int size = defaultWifiNetwork.size() + otherWifiNetworks.size();
            final WifiFragmentAddFromScan wifiFragmentAddFromScan = WifiFragmentAddFromScan.newInstance(wifis, size);
            wifiFragmentAddFromScan.show(getSupportFragmentManager(), null);
            wifiReciever = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        defaultWifiRecyclerAdapter.notifyDataSetChanged();
        otherWifiRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        prepareWifiNetworkList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(wifiReciever != null) {
            unregisterReceiver(wifiReciever);
            wifiReciever = null;
        }

        if(wifiProgress != null && wifiProgress.isShowing()){
            wifiProgress.dismiss();
        }
    }

    private void showWifiProgressbar(){
        wifiProgress = new ProgressDialog(this);
        wifiProgress.setTitle(getString(R.string.wifi_scan_title));
        wifiProgress.setMessage(getString(R.string.wifi_scan_message));
        wifiProgress.setCancelable(false);
        wifiProgress.show();
    }
}
