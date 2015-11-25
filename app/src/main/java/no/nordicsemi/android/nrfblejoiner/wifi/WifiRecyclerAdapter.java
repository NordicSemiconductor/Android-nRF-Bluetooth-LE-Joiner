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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import no.nordicsemi.android.nrfblejoiner.database.DatabaseHelper;
import no.nordicsemi.android.nrfblejoiner.widgets.ItemClickHelperAdapter;
import no.nordicsemi.android.nrfblejoiner.widgets.ItemTouchHelperAdapter;
import no.nordicsemi.android.nrfblejoiner.R;

public class WifiRecyclerAdapter extends RecyclerView.Adapter<WifiRecyclerAdapter.CustomViewHolder> implements ItemTouchHelperAdapter {

    private ArrayList<WifiNetwork> wifiNetworkList;
    private final LayoutInflater inflater;
    private DatabaseHelper dbHelper;
    private ItemClickHelperAdapter mItemClickHelper;

    public WifiRecyclerAdapter(Context context, ArrayList<WifiNetwork> networkList, ItemClickHelperAdapter clickListener) {
        this.inflater = LayoutInflater.from(context);
        this.wifiNetworkList = networkList;
        this.dbHelper = new DatabaseHelper(context);
        this.mItemClickHelper = clickListener;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {

        View view = inflater.inflate(R.layout.recycler_view_item_wifi, viewGroup, false);

        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, final int i) {

        customViewHolder.textView.setText(wifiNetworkList.get(i).getSsid());

        customViewHolder.imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickHelper.onWifiItemClick(wifiNetworkList.get(i));
            }
        });
    }

    @Override
    public int getItemCount() {
        return wifiNetworkList.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemDismiss(int position) {
        dbHelper.deleteWifiNetwork(wifiNetworkList.get(position).getId());
        wifiNetworkList.remove(position);
        notifyItemRemoved(position);
        mItemClickHelper.updateUiOnItemDismiss();
    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView textView;
        protected ImageView imageView;
        protected ImageView imgEdit;

        public CustomViewHolder(View view) {
            super(view);
            this.textView = (TextView) view.findViewById(R.id.tv_recycler_view_item);
            this.imageView = (ImageView) view.findViewById(R.id.img_wifi);
            this.imgEdit = (ImageView) view.findViewById(R.id.img_edit);
        }
    }


}
