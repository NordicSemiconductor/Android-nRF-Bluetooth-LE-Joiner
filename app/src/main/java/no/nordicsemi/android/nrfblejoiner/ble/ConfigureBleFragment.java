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

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;

import no.nordicsemi.android.nrfblejoiner.R;

public class ConfigureBleFragment extends DialogFragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = "BLE";
    private TextView tvOpcode, tvNextMode;
    private CheckBox chkAdvanced;
    private EditText etActionDelay, etIdentificationDuration, etDeviceLabel;
    private TextInputLayout tiActionDelay, tiIdentificationDuration, tiDeviceLabel;
    private Spinner spOpCode, spNextMode;
    private BleConfigFragmentInteractionListener mListener;
    private BleNodeSettingsAdapter opCodesAdapter, nextModeAdapter;
    private String opCode, nextMode;
    private Button cancel, configure, identify;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private boolean advancedIdentifyMode = false;

    public ConfigureBleFragment() {
        // Required empty public constructor
    }

    public static ConfigureBleFragment newInstance(String mDeviceName, String mDeviceAddress) {
        ConfigureBleFragment fragment = new ConfigureBleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, mDeviceName);
        args.putString(ARG_PARAM2, mDeviceAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDeviceName = getArguments().getString(ARG_PARAM1);
            mDeviceAddress = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_configure_ble, null);

        final TextView tvTitle = (TextView) dialogView.findViewById(R.id.tv_ble_frag_title);
        tvTitle.setText("Configure " + mDeviceName);
        tvOpcode = (TextView) dialogView.findViewById(R.id.tv_opcode_message);
        tvNextMode = (TextView) dialogView.findViewById(R.id.tv_next_mode_message);
        spOpCode = (Spinner) dialogView.findViewById(R.id.sp_op_code);
        spNextMode = (Spinner) dialogView.findViewById(R.id.sp_next_mode);
        etActionDelay = (EditText) dialogView.findViewById(R.id.et_action_delay);
        etIdentificationDuration = (EditText) dialogView.findViewById(R.id.et_identity_mode_duration);
        etDeviceLabel = (EditText) dialogView.findViewById(R.id.et_ble_label);
        tiActionDelay = (TextInputLayout) dialogView.findViewById(R.id.ti_action_delay);
        tiIdentificationDuration = (TextInputLayout) dialogView.findViewById(R.id.ti_identity_mode_duration);
        tiDeviceLabel = (TextInputLayout) dialogView.findViewById(R.id.ti_device_label);
        chkAdvanced = (CheckBox) dialogView.findViewById(R.id.chk_edit);
        opCodesAdapter = new BleNodeSettingsAdapter(getActivity(), R.layout.custom_spinner, populateBleOpCodes());
        spOpCode.setAdapter(opCodesAdapter);

        nextModeAdapter = new BleNodeSettingsAdapter(getActivity(), R.layout.custom_spinner, populateBleNextMode());
        spNextMode.setAdapter(nextModeAdapter);

        alertDialogBuilder.setView(dialogView).setNeutralButton(getString(R.string.identify), null).setNegativeButton(getString(R.string.cancel), null).setPositiveButton(getString(R.string.configure), null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);
        setCancelable(false);

        configure = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        cancel = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        identify = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        configure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    if (chkAdvanced.isChecked()) {
                        mListener.configureBleNode(opCode, etActionDelay.getText().toString(), etIdentificationDuration.getText().toString(), nextMode, etDeviceLabel.getText().toString());
                    } else {
                        Resources resources = getActivity().getResources();
                        mListener.configureBleNode(resources.getString(R.string.ble_connection_mode_value), resources.getString(R.string.action_delay_default), resources.getString(R.string.connection_duration_default), resources.getString(R.string.configuration_mode_value), "");
                    }
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.disconnectFromDevice();
            }
        });

        identify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (advancedIdentifyMode) {
                    mListener.advancedIdentifyMode(opCode, etActionDelay.getText().toString(), etIdentificationDuration.getText().toString(), nextMode, etDeviceLabel.getText().toString());
                } else {
                    mListener.identifyMode();
                }
            }
        });

        etActionDelay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tiActionDelay.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etIdentificationDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tiIdentificationDuration.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etDeviceLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tiDeviceLabel.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        spOpCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                opCode = parent.getSelectedItem().toString();
                advancedIdentifyMode = false;
                if (opCode.equalsIgnoreCase(getActivity().getString(R.string.ble_connection_mode)))
                    opCode = getActivity().getString(R.string.ble_connection_mode_value);
                else if (opCode.equalsIgnoreCase(getActivity().getString(R.string.ble_configuration_mode)))
                    opCode = getActivity().getString(R.string.ble_configuration_mode_value);
                else if (opCode.equalsIgnoreCase(getActivity().getString(R.string.ble_identification_mode)))
                    opCode = getActivity().getString(R.string.ble_identification_mode_value);
                updateUiOnIdentificationMode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spNextMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                advancedIdentifyMode = false;
                nextMode = parent.getSelectedItem().toString();

                if (nextMode.equalsIgnoreCase(getActivity().getString(R.string.no_state_change)))
                    nextMode = getActivity().getString(R.string.no_state_change_value);
                else if (nextMode.equalsIgnoreCase(getActivity().getString(R.string.power_off)))
                    nextMode = getActivity().getString(R.string.power_off_value);
                else
                    nextMode = getActivity().getString(R.string.configuration_mode_value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        chkAdvanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAdvancedSettings(isChecked);
            }
        });

        return alertDialog;
    }

    private boolean validateInput() {

        if (spOpCode.getVisibility() == View.VISIBLE && opCode == null) {
            Toast.makeText(getActivity(), getString(R.string.op_code_error), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spNextMode.getVisibility() == View.VISIBLE && nextMode == null) {
            Toast.makeText(getActivity(), getString(R.string.backup_mode_error), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etActionDelay.getVisibility() == View.VISIBLE && etActionDelay.getText().toString().equalsIgnoreCase("")) {
            tiActionDelay.setError(getString(R.string.action_delay_error));
            return false;
        }
        if (etIdentificationDuration.getVisibility() == View.VISIBLE && etIdentificationDuration.getText().toString().equalsIgnoreCase("")) {
            tiIdentificationDuration.setError(getString(R.string.identification_duration_error));
            return false;
        }

        if (etDeviceLabel.getVisibility() == View.VISIBLE && etDeviceLabel.getText().toString().equalsIgnoreCase("")) {
            tiDeviceLabel.setError(getString(R.string.device_label_error));
            return false;
        }

        return true;
    }

    private ArrayList<String> populateBleOpCodes() {
        ArrayList<String> configurationList = new ArrayList<>();
        String[] bleSettings = getActivity().getResources().getStringArray(R.array.op_codes);

        Collections.addAll(configurationList, bleSettings);

        return configurationList;
    }

    private ArrayList<String> populateBleNextMode() {
        final ArrayList<String> configurationList = new ArrayList<>();
        final String[] bleSettings = getActivity().getResources().getStringArray(R.array.next_mode);

        Collections.addAll(configurationList, bleSettings);

        return configurationList;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BleConfigFragmentInteractionListener) activity;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("BLE", "Dismiss called");
    }

    private void updateUiOnIdentificationMode() {
        if (opCode.equalsIgnoreCase(getActivity().getString(R.string.ble_identification_mode_value))) {
            etDeviceLabel.setVisibility(View.VISIBLE);
            configure.setEnabled(false);
            advancedIdentifyMode = true;
        } else {
            configure.setEnabled(true);
            etDeviceLabel.setVisibility(View.GONE);
        }
    }

    private void showAdvancedSettings(boolean flag) {
        if (flag) {
            tvOpcode.setVisibility(View.VISIBLE);
            tvNextMode.setVisibility(View.VISIBLE);
            etActionDelay.setVisibility(View.VISIBLE);
            etIdentificationDuration.setVisibility(View.VISIBLE);
            spNextMode.setVisibility(View.VISIBLE);
            spOpCode.setVisibility(View.VISIBLE);
            etDeviceLabel.setVisibility(View.GONE);
            spOpCode.setSelection(0, false);
            spNextMode.setSelection(2, false);
        } else {
            tvOpcode.setVisibility(View.GONE);
            tvNextMode.setVisibility(View.GONE);
            etActionDelay.setVisibility(View.GONE);
            etIdentificationDuration.setVisibility(View.GONE);
            spNextMode.setVisibility(View.GONE);
            spOpCode.setVisibility(View.GONE);
            etDeviceLabel.setVisibility(View.GONE);
            configure.setEnabled(true);
            advancedIdentifyMode = false;
        }
    }

    public interface BleConfigFragmentInteractionListener {

        void configureBleNode(String characteristic, String actionDelay, String identityModeDuration, String nextMode, String label);

        void identifyMode();

        void advancedIdentifyMode(String characteristic, String actionDelay, String identityModeDuration, String nextMode, String label);

        void disconnectFromDevice();
    }
}
