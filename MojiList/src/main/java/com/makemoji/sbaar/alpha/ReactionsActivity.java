package com.makemoji.sbaar.alpha;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.makemoji.mojilib.model.ReactionsData;

import org.json.JSONArray;

import java.util.ArrayList;

public class ReactionsActivity extends AppCompatActivity {
    ListView lv;
    ReactionsAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reactions);
        lv = (ListView)findViewById(R.id.list_view);
        adapter = new ReactionsAdapter(this,new ArrayList<MojiMessage>());
        for (int i = 0; i < Sample.sample.length; i++) {
            adapter.add(new MojiMessage(Sample.sample[i]));
        }
        lv.setAdapter(adapter);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ReactionsData.onActivityResult(requestCode,resultCode,data);
    }
}
