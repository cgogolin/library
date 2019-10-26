package com.cgogolin.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class GroupsActivity extends AppCompatActivity {

    private ArrayList<String> groupList = null;
    private ArrayAdapter<String> groupsArrayAdapter = null;
    private ListView lv = null;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.groupslist);
        groupList = (ArrayList<String>) getIntent().getSerializableExtra("group list");
        groupList.add(0, "All");
        groupsArrayAdapter = new ArrayAdapter<String> (this, R.layout.grouplistentry, R.id.group_name, groupList);
        lv = (ListView) findViewById(R.id.bibtex_grouplist_view);
        lv.setAdapter(groupsArrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter, View v, int position, long id){
                String selectedGroup = "";
                if(position != 0) {
                    selectedGroup = (String) adapter.getItemAtPosition(position);
                }
                Intent resultIntent = new Intent();
                resultIntent.putExtra("group", selectedGroup);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

    }

}
