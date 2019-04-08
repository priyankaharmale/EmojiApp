package com.makemoji.sbaar.alpha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.makemoji.mojilib.HyperMojiListener;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.ParsedAttributes;
import com.makemoji.mojilib.ReactionsLayout;
import com.makemoji.mojilib.model.ReactionsData;

import java.util.List;

/**
 * Created by Scott Baar on 12/3/2015.
 */
public class ReactionsAdapter extends ArrayAdapter<MojiMessage> {
    Context context;
    List<MojiMessage> messages;
    public ReactionsAdapter(Context context, List<MojiMessage> messages){
        super(context,R.layout.message_item,messages);
        this.context = context;
        this.messages = messages;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Holder holder;
        MojiMessage message = getItem(position);
        if (convertView==null){
            convertView = LayoutInflater.from(context).inflate(R.layout.reactions_list_item,parent,false);
            holder = new Holder();
            holder.message = (TextView) convertView.findViewById(R.id.item_message_tv);
            holder.reactionsLayout = (ReactionsLayout) convertView.findViewById(R.id.reactions_layout);
            convertView.setTag(holder);
        }
        holder = (Holder) convertView.getTag();
        if (!message.id.equals(holder.id)){
            holder.id = message.id;
            Moji.setText(message.html,holder.message,true);

            //cache this in your item data so it's not fetched on every scroll.
            if (message.reactionsData==null) message.reactionsData = new ReactionsData(message.id);
            holder.reactionsLayout.setReactionsData(message.reactionsData);
        }
        return convertView;
    }

    static class Holder{
        public TextView message;
        public String id;
        public ReactionsLayout reactionsLayout;
    }
}
