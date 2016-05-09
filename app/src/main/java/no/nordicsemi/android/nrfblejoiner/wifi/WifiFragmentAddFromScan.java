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
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;


public class WifiFragmentAddFromScan extends DialogFragment {

    private static final String ARG_WIFI_RESULTS = "wifiresults";
    private static final String ARG_WIFI_NETWORK_COUNT = "savedwifinetworkcount";
    private ArrayList<String> wifi_results;
    private WifiResultsAdapter wifi_results_adapter;
    private OnFragmentInteractionListener mListener;
    private int numberOfSavedNetworks;
    private boolean isDefaultRouter = false;
    private DatabaseHelper mDbHelper;
    private Spinner spinner;
    private EditText etWifiPassword;
    private TextInputLayout tiPassword;


    public static WifiFragmentAddFromScan newInstance(ArrayList<String> wifi_results, int count) {
        WifiFragmentAddFromScan fragment = new WifiFragmentAddFromScan();
        final Bundle args = new Bundle();
        args.putStringArrayList(ARG_WIFI_RESULTS, wifi_results);
        args.putInt(ARG_WIFI_NETWORK_COUNT, count);
        fragment.setArguments(args);
        return fragment;
    }

    public WifiFragmentAddFromScan() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            wifi_results = getArguments().getStringArrayList(ARG_WIFI_RESULTS);
            numberOfSavedNetworks = getArguments().getInt(ARG_WIFI_NETWORK_COUNT);
            mDbHelper = new DatabaseHelper(getActivity());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        setCancelable(false);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_wifi_add_scanner, null);

        spinner = (Spinner) dialogView.findViewById(R.id.wifi_spinner);
        tiPassword = (TextInputLayout) dialogView.findViewById(R.id.ti_password);
        etWifiPassword = (EditText) dialogView.findViewById(R.id.et_wifi_password);
        final CheckBox chkViewPassword = (CheckBox) dialogView.findViewById(R.id.chk_view_password);
        final CheckBox chkDefaultRouter = (CheckBox) dialogView.findViewById(R.id.chk_default);

        alertDialogBuilder.setTitle(getString(R.string.select_wifi_title));
        alertDialogBuilder.setMessage(getString(R.string.select_wifi_messge));
        wifi_results.add(0, "Select");
        wifi_results_adapter = new WifiResultsAdapter(getActivity(), R.layout.custom_spinner, wifi_results);
        spinner.setAdapter(wifi_results_adapter);

        chkViewPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    etWifiPassword.setTransformationMethod(new HideReturnsTransformationMethod());
                else
                    etWifiPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        chkDefaultRouter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isDefaultRouter = isChecked;
            }
        });

        if (numberOfSavedNetworks == 0) {
            chkDefaultRouter.setChecked(true);
        }


        final AlertDialog alertDialog = alertDialogBuilder.setView(dialogView).setPositiveButton(getString(R.string.save), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    saveWifiToDb();
                    dismiss();
                    mListener.onWifiNetworkSavedToDb();
                }
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        etWifiPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tiPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 16)
                    tiPassword.setError(getString(R.string.password_too_long_error));
            }
        });

        return alertDialog;
    }

    private boolean validateInput(){
        if(spinner.getSelectedItem().toString().equalsIgnoreCase("Select")){
            showToast(getString(R.string.please_select_wifi));
            return false;
        } else if(spinner.getSelectedItem().toString().length() < 6 || spinner.getSelectedItem().toString().length() > 16){
            showToast(getString(R.string.error_selected_wifi));
        } else if (etWifiPassword.getText().length() > 16){
            tiPassword.setError(tiPassword.getError().toString());
            return false;
        }
        return true;
    }

    private void showToast(String error){
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
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
        void onWifiNetworkSavedToDb();
    }

    private void saveWifiToDb() {
        final String mWifiNetwork = spinner.getSelectedItem().toString();
        final String mWifiPass = etWifiPassword.getText().toString();
        String mDefaultRouter = "false";
        if (mWifiNetwork != null && !mWifiNetwork.equalsIgnoreCase("Select")) {

            if (isDefaultRouter) {
                mDefaultRouter = "true";
                isDefaultRouter = false;
            }

            mDbHelper.changeDefaultWifiNetworkToOther();
            mDbHelper.insertWifiNetwork(mWifiNetwork, mWifiPass, mDefaultRouter);
        }
    }
}
