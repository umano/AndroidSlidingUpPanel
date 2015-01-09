package com.sothree.slidinguppanel.demo;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

public class DemoActivity extends ActionBarActivity {
    private static final String TAG = "DemoActivity";

    private SlidingUpPanelLayout mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));


        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        //mLayout.attachFloatingActionButton(findViewById(R.id.fab));
        mLayout.setPanelSlideListener(new PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelExpanded(View panel) {
                Log.i(TAG, "onPanelExpanded");

            }

            @Override
            public void onPanelCollapsed(View panel) {
                Log.i(TAG, "onPanelCollapsed");

            }

            @Override
            public void onPanelAnchored(View panel) {
                Log.i(TAG, "onPanelAnchored");
            }

            @Override
            public void onPanelHidden(View panel) {
                Log.i(TAG, "onPanelHidden");
            }

            @Override
            public void onPanelHiddenExecuted(View panel) {
                Log.i(TAG, "onPanelHiddenExecuted");
            }

            @Override
            public void onPanelShownExecuted(View panel) {
                Log.i(TAG, "onPanelShownExecuted");
            }

            @Override
            public void onPanelExpandedStateY(View panel, boolean reached) {
                Log.i(TAG, "onPanelExpandedStateY" + (reached ? "reached" : "left"));
            }

            @Override
            public void onPanelCollapsedStateY(View panel, boolean reached) {
                Log.i(TAG, "onPanelCollapsedStateY" + (reached ? "reached" : "left"));
                LinearLayout titleBar = (LinearLayout) findViewById(R.id.titlebar);
                if (reached) {
                    titleBar.setBackgroundColor(Color.WHITE);
                } else {
                    titleBar.setBackgroundColor(Color.parseColor("#ffff9431"));
                }
            }

            @Override
            public void onPanelExpandedStateYLayout(View panel) {
                Log.i(TAG, "onPanelExpandedStateYLayout");
                LinearLayout titleBar = (LinearLayout) findViewById(R.id.titlebar);
                titleBar.setBackgroundColor(Color.parseColor("#ffff9431"));
            }

            @Override
            public void onPanelCollapsedStateYLayout(View panel) {
                Log.i(TAG, "onPanelCollapsedStateYLayout");
                LinearLayout titleBar = (LinearLayout) findViewById(R.id.titlebar);
                titleBar.setBackgroundColor(Color.WHITE);
            }
        });

        TextView t = (TextView) findViewById(R.id.main);
        t.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayout.collapsePanel();
            }
        });

        t = (TextView) findViewById(R.id.name);
        t.setText(Html.fromHtml(getString(R.string.hello)));
        Button f = (Button) findViewById(R.id.follow);
        f.setText(Html.fromHtml(getString(R.string.follow)));
        f.setMovementMethod(LinkMovementMethod.getInstance());
        f.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.twitter.com/umanoapp"));
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.demo, menu);
        MenuItem item = menu.findItem(R.id.action_toggle);
        if (mLayout != null) {
            if (mLayout.isPanelHidden()) {
                item.setTitle(R.string.action_show);
            } else {
                item.setTitle(R.string.action_hide);
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_toggle: {
                if (mLayout != null) {
                    if (!mLayout.isPanelHidden()) {
                        mLayout.hidePanel();
                        item.setTitle(R.string.action_show);
                    } else {
                        mLayout.showPanel();
                        item.setTitle(R.string.action_hide);
                    }
                }
                return true;
            }
            case R.id.action_anchor: {
                if (mLayout != null) {
                    if (mLayout.getAnchorPoint() == 1.0f) {
                        mLayout.setAnchorPoint(0.7f);
                        mLayout.expandPanel(0.7f);
                        item.setTitle(R.string.action_anchor_disable);
                    } else {
                        mLayout.setAnchorPoint(1.0f);
                        mLayout.collapsePanel();
                        item.setTitle(R.string.action_anchor_enable);
                    }
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null && mLayout.isPanelExpanded() || mLayout.isPanelAnchored()) {
            mLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }
}
