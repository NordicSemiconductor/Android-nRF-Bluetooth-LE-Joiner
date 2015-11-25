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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import no.nordicsemi.android.nrfblejoiner.R;

public class WifiFragmentSelect extends DialogFragment {

    private Button ok, cancel;
    private returnAddWifiMethodChoice mListener;
    private boolean selection;
    private boolean selectionFlag = false;

    public static WifiFragmentSelect newInstance() {
        WifiFragmentSelect fragment = new WifiFragmentSelect();
        return fragment;
    }

    public WifiFragmentSelect() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getActivity().getString(R.string.add_wifi_title));
        final String [] array = getActivity().getResources().getStringArray(R.array.add_wifi_method);

        alertDialogBuilder.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectionFlag = true;
                switch(which){
                    case 0:
                        selection = false;
                        break;
                    case 1:
                        selection = true;
                        break;
                }
            }
        });
        alertDialogBuilder.setPositiveButton(getString(R.string.ok), null).setNegativeButton(getString(R.string.cancel), null);
        AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.setCanceledOnTouchOutside(false);

        ok = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        cancel = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectionFlag) {
                    boolean flag = mListener.wifiAddMethod(selection);
                    if(flag)
                        dismiss();
                    else
                        Toast.makeText(getActivity(), getString(R.string.check_wifi_enabled), Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getActivity(), getString(R.string.select_option), Toast.LENGTH_SHORT).show();
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
            mListener = (returnAddWifiMethodChoice) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement returnAddWifiMethodChoice");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface returnAddWifiMethodChoice {

        boolean wifiAddMethod(boolean scanForWifi);
    }

}
