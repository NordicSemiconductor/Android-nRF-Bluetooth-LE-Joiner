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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import no.nordicsemi.android.nrfblejoiner.database.DatabaseContract;
import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;

public class WifiFragmentDefault extends DialogFragment {

    private DatabaseHelper mDbHelper;
    private OnFragmentInteractionListener mListener;
    private boolean defaultRouter = false;
    private String ssid;

    public static WifiFragmentDefault newInstance() {
        WifiFragmentDefault fragment = new WifiFragmentDefault();
        return fragment;
    }

    public WifiFragmentDefault() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        mDbHelper = new DatabaseHelper(getActivity());
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_wifi_default, null);
        alertDialogBuilder.setTitle(getString(R.string.select_wifi_title));
        final Cursor cursor = mDbHelper.getAllSsids();
        SimpleCursorAdapter ssidAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_single_choice, cursor, new String[]{DatabaseContract.WifiNetworksEntry.COLUMN_NAME_SSID}, new int []{android.R.id.text1}, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        final ListView ssidList = (ListView) dialogView.findViewById(R.id.list_wifi_ssid);
        ssidList.setAdapter(ssidAdapter);

        //in case there is only one wifi network its automatically picked
        if(cursor.getCount() == 1) {
            ssidList.setItemChecked(0, true);
            defaultRouter = true;
            ssid = cursor.getString(1);
        }

        ssidList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cursor.moveToPosition(position);
                ssid = cursor.getString(1);
            }
        });

        final CheckBox chk_default_router = (CheckBox) dialogView.findViewById(R.id.chk_default_router);
        chk_default_router.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                defaultRouter = isChecked;
            }
        });
        chk_default_router.setChecked(true);

        alertDialogBuilder.setView(dialogView).setNegativeButton(getString(R.string.cancel), null).setPositiveButton(getString(R.string.save), null);
        alertDialogBuilder.setView(dialogView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);

        final Button save = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        final Button cancel = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ssid != null && !ssid.equalsIgnoreCase("")){
                    if(defaultRouter) {
                        mDbHelper.setDefaultWifiNetwork(cursor.getInt(0), defaultRouter);
                    }
                    dismiss();
                    mListener.connectToBleDevice();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.wifi_select_error), Toast.LENGTH_SHORT).show();
                }

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return alertDialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BleConfigFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {

        void connectToBleDevice();
    }

}
