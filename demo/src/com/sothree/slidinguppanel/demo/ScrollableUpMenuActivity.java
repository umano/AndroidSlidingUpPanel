package com.sothree.slidinguppanel.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;


public class ScrollableUpMenuActivity extends ActionBarActivity {
    SlidingUpPanelLayout rootView;
    ListView mainContentList;
    MenuListView menuList;

    private void constructListViews() {
        String[] items = { "Milk", "Butter", "Yogurt", "Toothpaste", "Ice Cream", "Milk", "Butter", "Yogurt", "Toothpaste", "Ice Cream",  "Milk", "Butter", "Yogurt", "Toothpaste", "Ice Cream", "Milk", "Butter", "Yogurt", "Toothpaste", "Ice Cream" };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);

        mainContentList.setAdapter(adapter);
        menuList.setAdapter(adapter);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollable_up_menu);

        rootView = (SlidingUpPanelLayout)findViewById(R.id.drawer_layout);
        mainContentList = (ListView)findViewById(R.id.list1);
        menuList = (MenuListView)findViewById(R.id.list2);

        constructListViews();
        rootView.setNegotiator(menuList);
        rootView.setEnableDragViewTouchEvents(true);
        rootView.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {

            }

            @Override
            public void onPanelCollapsed(View view) {
                menuList.disableScroll(true);
            }

            @Override
            public void onPanelExpanded(View view) {
                menuList.disableScroll(false);

            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });
    }



}
