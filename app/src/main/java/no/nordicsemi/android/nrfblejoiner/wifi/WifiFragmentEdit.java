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

public class WifiFragmentEdit extends DialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_SSID = "ssid";
    private static final String ARG_PASSWORD = "password";
    private static final String ARG_IS_DEFAULT = "isdefault";


    private int id;
    private String ssid;
    private String password;
    private String isDefaultRouter;
    private onWifiEditListener mListener;
    private boolean isDefaultRouterFlag = false;
    private DatabaseHelper mDbHelper;
    private EditText etPassword, etSsid;
    private TextInputLayout tiSsid, tiPassword;

    public static WifiFragmentEdit newInstance(int id, String ssid, String password, String isDefaultRouter) {
        WifiFragmentEdit fragment = new WifiFragmentEdit();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, id);
        args.putString(ARG_SSID, ssid);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_IS_DEFAULT, isDefaultRouter);
        fragment.setArguments(args);
        return fragment;
    }

    public WifiFragmentEdit() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt(ARG_ID);
            ssid = getArguments().getString(ARG_SSID);
            password = getArguments().getString(ARG_PASSWORD);
            isDefaultRouter = getArguments().getString(ARG_IS_DEFAULT);
            isDefaultRouterFlag = Boolean.valueOf(isDefaultRouter);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_wifi_dialog_edit, null);
        tiSsid = (TextInputLayout) dialogView.findViewById(R.id.ti_ssid);
        tiPassword = (TextInputLayout) dialogView.findViewById(R.id.ti_password);
        etSsid = (EditText) dialogView.findViewById(R.id.et_ssid);
        etPassword = (EditText) dialogView.findViewById(R.id.et_password);
        final CheckBox chkPassword = (CheckBox) dialogView.findViewById(R.id.chk_view_password);
        final CheckBox chkDefaultRouter = (CheckBox) dialogView.findViewById(R.id.chk_default);

        mDbHelper = new DatabaseHelper(getActivity());

        alertDialogBuilder.setTitle(getString(R.string.edit_wifi_title));
        etSsid.setText(ssid);
        etPassword.setText(password);
        chkDefaultRouter.setChecked(Boolean.valueOf(isDefaultRouter));
        alertDialogBuilder.setView(dialogView).setPositiveButton(getString(R.string.save), null).setNegativeButton(getString(R.string.cancel), null);
        final AlertDialog alertDialog = alertDialogBuilder.setView(dialogView).show();
        alertDialog.setCanceledOnTouchOutside(false);

        chkDefaultRouter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isDefaultRouter = String.valueOf(isChecked);
            }
        });

        chkPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    etPassword.setTransformationMethod(new HideReturnsTransformationMethod());
                else
                    etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    updateWifiList();
                    dismiss();
                    mListener.onEditWifiNetwork();

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
                if (s.toString().length() > 16 || s.toString().length() < 6)
                    tiSsid.setError(getString(R.string.ssid_length_error));

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
            tiSsid.setError(getActivity().getString(R.string.empty_ssid));
            return false;
        } else if(ssid.length() > 16 || ssid.length() < 6){
            tiSsid.setError(getString(R.string.ssid_length_error));
            return false;
        } else if (password.length() > 16){
            showToast(getString(R.string.password_too_long_error));
            return false;
        }
        return true;
    }

    private void showToast(String error){
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    private void updateWifiList(){
        final WifiNetwork wifiNetwork = mDbHelper.getDefaultWifiNetwork();

        if(wifiNetwork != null && wifiNetwork.getId() == id){
            mDbHelper.updateWifiNetwork(wifiNetwork.getId(), etSsid.getText().toString(), etPassword.getText().toString().trim(), isDefaultRouter);
        } else {
            mDbHelper.changeDefaultWifiNetworkToOther();
            mDbHelper.updateWifiNetwork(id, etSsid.getText().toString().trim(), etPassword.getText().toString().trim(), isDefaultRouter);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (onWifiEditListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onWifiEditListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public interface onWifiEditListener {

        void onEditWifiNetwork();
    }

}
