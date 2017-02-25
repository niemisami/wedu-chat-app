package com.niemisami.wedu;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.niemisami.wedu.utils.ToolbarUpdater;

public class MainActivity extends AppCompatActivity implements ToolbarUpdater{

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.app_name));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_question_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Hello Snackbar!",
                        Snackbar.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void setSubtitle(String subtitle) {
        if(mToolbar != null) {
            mToolbar.setSubtitle(subtitle);
        }
    }

    @Override
    public void clearSubtitle() {
        if(mToolbar != null) {
            mToolbar.setSubtitle(null);
        }
    }
}
