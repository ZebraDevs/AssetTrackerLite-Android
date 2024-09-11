package com.symbol.assettrackerlite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends BaseAdapter{
    List<ArrayList<String>> result;
    Context context;
    private static LayoutInflater inflater=null;

    public CustomAdapter(InventoryActivity mainActivity,  List<ArrayList<String>> prgmNameList) {
        // TODO Auto-generated constructor stub
        result=prgmNameList;
        context=mainActivity;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return result.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        TextView desc;
        TextView barcode;
        TextView price;
        TextView qty;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder=new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.program_list, null);
        holder.desc=(TextView) rowView.findViewById(R.id.descTextView);
        holder.barcode=(TextView) rowView.findViewById(R.id.barcodeTextView);
        holder.price=(TextView) rowView.findViewById(R.id.priceTextView);
        holder.qty=(TextView) rowView.findViewById(R.id.qtyTextView);

        holder.desc.setText(result.get(position).get(0));
        holder.barcode.setText(result.get(position).get(1));
        holder.price.setText(result.get(position).get(2));
        holder.qty.setText(result.get(position).get(3));

        return rowView;
    }

} 