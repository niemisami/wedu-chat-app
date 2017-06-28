package com.niemisami.wedu;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.niemisami.wedu.question.QuestionsFragment;
import com.niemisami.wedu.utils.FabUpdater;
import com.niemisami.wedu.utils.ToolbarUpdater;

public class MainActivity extends AppCompatActivity implements ToolbarUpdater, FabUpdater {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private OnFabClickListener mOnFabClickListener;
    private FloatingActionButton mFab;
    private FabUpdater mFabUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inflateFragment();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.app_name));

        mFab = (FloatingActionButton) findViewById(R.id.add_question_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFab();
                mOnFabClickListener.onFabClicked();
            }
        });
    }


    private void inflateFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new QuestionsFragment());
        fragmentTransaction.commit();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof OnFabClickListener) {
            mFabUpdater = this;
            mOnFabClickListener = (OnFabClickListener) fragment;
        }
    }

    @Override
    public void setTitle(String title) {
        if (mToolbar != null)
            mToolbar.setTitle(title);
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (mToolbar != null) {
            mToolbar.setSubtitle(subtitle);
        }
    }

    @Override
    public void clearSubtitle() {
        if (mToolbar != null) {
            mToolbar.setSubtitle(null);
        }
    }

    @Override
    public void showFab() {
        mFab.show();

    }

    @Override
    public void hideFab() {
        mFab.hide();

    }

    public interface OnFabClickListener {
        void onFabClicked();
    }

}
