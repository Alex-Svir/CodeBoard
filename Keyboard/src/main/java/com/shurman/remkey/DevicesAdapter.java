package com.shurman.remkey;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DevicesAdapter extends BaseAdapter {
    private final List<BluetoothDevice> list;
    private final LayoutInflater inflater;

    public DevicesAdapter(Context context, Set<BluetoothDevice> set) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        list = new ArrayList<>(set);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView==null ?
                inflater.inflate(R.layout.item_device_list, parent, false) :
                convertView;
        ((TextView)view.findViewById(R.id.text)).setText(list.get(position).getName());
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = convertView==null ?
                inflater.inflate(R.layout.item_device_list_dropdown, parent, false) :
                convertView;
        ((TextView)view.findViewById(R.id.tv_name)).setText(list.get(position).getName());
        ((TextView)view.findViewById(R.id.tv_mac)).setText(list.get(position).getAddress());
        return view;
    }

    public int getItemPositionByMacAddress(String address) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getAddress().equals(address)) return i;
        }
        return -1;
    }
}
