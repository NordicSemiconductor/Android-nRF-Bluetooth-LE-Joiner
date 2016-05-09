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
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import no.nordicsemi.android.nrfblejoiner.R;
import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;

public class WifiFragmentAddManually extends DialogFragment {

    private WifiInformation mListener;
    private boolean defaultRouter = false;
    private DatabaseHelper mDbHelper;
    private EditText etSsid;
    private EditText etPassword;
    private TextInputLayout tiSsid, tiPassword;

    public static WifiFragmentAddManually newInstance() {
        WifiFragmentAddManually fragment = new WifiFragmentAddManually();
        return fragment;
    }

    public WifiFragmentAddManually() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDbHelper = new DatabaseHelper(getActivity());

        setCancelable(false);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_wifi_add_manually, null);
        tiSsid = (TextInputLayout) dialogView.findViewById(R.id.ti_ssid);
        tiPassword = (TextInputLayout) dialogView.findViewById(R.id.ti_password);
        etSsid = (EditText) dialogView.findViewById(R.id.et_ssid);
        etPassword = (EditText) dialogView.findViewById(R.id.et_password);
        final CheckBox chkViewPassword = (CheckBox) dialogView.findViewById(R.id.chk_view_password);
        final CheckBox chkDefault = (CheckBox) dialogView.findViewById(R.id.chk_default);

        alertDialogBuilder.setTitle(getString(R.string.add_wifi_title));
        alertDialogBuilder.setView(dialogView).setPositiveButton(getString(R.string.save), null).setNegativeButton(getString(R.string.cancel), null);
        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.setCanceledOnTouchOutside(false);

        chkViewPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    etPassword.setTransformationMethod(new HideReturnsTransformationMethod());
                else
                    etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        chkDefault.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                defaultRouter = isChecked;
            }
        });

        if(mDbHelper.getDefaultWifiNetwork() == null){
            chkDefault.setChecked(true);
        }

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()){
                    if(defaultRouter) {
                        changeDefaultWifiNetwork();
                        mDbHelper.insertWifiNetwork(etSsid.getText().toString(), etPassword.getText().toString(), String.valueOf(defaultRouter));
                    } else {
                        mDbHelper.insertWifiNetwork(etSsid.getText().toString(), etPassword.getText().toString(), String.valueOf(defaultRouter));
                    }
                    dismiss();
                    mListener.returnWifiInformation();
                }
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        etSsid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tiSsid.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 16 || s.toString().length() < 6){
                    tiSsid.setError(getString(R.string.ssid_length_error));
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
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
        final String ssid = etSsid.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        if(ssid.isEmpty()){
            tiSsid.setError(getString(R.string.empty_ssid));
            return false;
        } else if(ssid.length() > 16){
            tiSsid.setError(getString(R.string.ssid_length_error));
            return false;
        } else if (password.length() > 16){
            showToast(tiPassword.getError().toString());
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
            mListener = (WifiInformation) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement WifiInformation");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface WifiInformation {

        void returnWifiInformation();
    }

    private void changeDefaultWifiNetwork(){
        final WifiNetwork wifiNetwork = mDbHelper.getDefaultWifiNetwork();

        if(wifiNetwork != null){
            mDbHelper.updateWifiNetwork(wifiNetwork.getId(), wifiNetwork.getSsid(), wifiNetwork.getPassword(), "false");
        }
    }
}
