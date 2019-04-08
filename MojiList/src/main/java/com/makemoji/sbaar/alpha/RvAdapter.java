package com.makemoji.sbaar.alpha;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.model.MojiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by s_baa on 11/13/2017.
 */

public class RvAdapter extends RecyclerView.Adapter<RvAdapter.Holder> {

    List<MojiMessage> items = new ArrayList<>();

    public void addItem(MojiMessage mojiModel){
        items.add(mojiModel);
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item,parent,false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        MojiMessage message = items.get(position);
        if (message.html!=null)Moji.setText(message.html,holder.textView,true);
        else if (message.plainText!=null)Moji.setText(Moji.plainTextToSpanned(message.plainText),holder.textView);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class Holder extends RecyclerView.ViewHolder{
        TextView textView;

        public Holder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_message_tv);
        }
    }
}
