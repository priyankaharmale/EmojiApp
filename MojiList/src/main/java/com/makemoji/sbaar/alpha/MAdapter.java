package com.makemoji.sbaar.alpha;

import android.content.Context;
import android.text.Spanned;
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

import java.util.List;

/**
 * Created by Scott Baar on 12/3/2015.
 */
public class MAdapter extends ArrayAdapter<MojiMessage> {
    Context context;
    List<MojiMessage> messages;
    private float mTextSize = -1;
    private boolean mSimple = true;
    public MAdapter (Context context, List<MojiMessage> messages, boolean simple){
        super(context,R.layout.message_item,messages);
        this.context = context;
        this.messages = messages;
        mSimple = simple;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        final MojiMessage message = getItem(position);
        if (convertView==null){
           convertView = LayoutInflater.from(context).inflate(R.layout.message_item,parent,false);
            Holder h = new Holder();
            h.messageTV = (TextView)convertView.findViewById(R.id.item_message_tv);
            h.fromIV = (ImageView) convertView.findViewById(R.id.from_iv);
            h.toIV = (ImageView) convertView.findViewById(R.id.to_iv);
            convertView.setTag(h);

            h.messageTV.setTag(R.id._makemoji_hypermoji_listener_tag_id, new HyperMojiListener() {
                @Override
                public void onClick(String url) {
                    Toast.makeText(getContext(),"hypermoji clicked from adapter url " + url,Toast.LENGTH_SHORT).show();
                }
            });
            h.messageTV.setTag(R.id._makemoji_text_watcher, Moji.getDefaultTextWatcher());

            if (mTextSize== -1) mTextSize = h.messageTV.getTextSize();
        }
        final Holder holder = (Holder) convertView.getTag();

        if (!message.id.equals(holder.id)){
            holder.id = message.id;
            ParsedAttributes parsedAttributes = message.parsedAttributes;
            if (parsedAttributes==null && message.html!=null) {
                //if simple is true, do not set things like text color. We can set those later from Parsed Attributes.
                parsedAttributes = Moji.parseHtml(message.html, holder.messageTV, mSimple);
                message.parsedAttributes =parsedAttributes;

                //make sure the text watcher put in place for plain text messages in code below is removed, since this view can be a recycled one.
                holder.messageTV.setTag(R.id._makemoji_text_watcher,null);
                Moji.setText(message.parsedAttributes.spanned,holder.messageTV);
            }
            else if (parsedAttributes==null && message.plainText!=null){
                Spanned spanned = Moji.plainTextToSpanned(message.plainText);
                holder.messageTV.setTag(R.id._makemoji_text_watcher,Moji.getDefaultTextWatcher());
                Moji.setText(spanned,holder.messageTV);
            }
        }
        return convertView;
    }

    static class Holder{
        public TextView messageTV;
        public String id;
        public ImageView fromIV;
        public ImageView toIV;
        public boolean simple;
    }
}
