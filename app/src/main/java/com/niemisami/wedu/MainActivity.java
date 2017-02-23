package com.niemisami.wedu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.niemisami.wedu.utilities.ToolbarUpdater;

public class MainActivity extends AppCompatActivity implements ToolbarUpdater{

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.app_name));
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
