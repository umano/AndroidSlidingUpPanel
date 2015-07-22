package com.sothree.slidinguppanel.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecyclerDemoActivity extends AppCompatActivity {
	private static final String TAG = "RecyclerDemoActivity";

	private SlidingUpPanelLayout mLayout;
	private RecyclerView         recyclerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo_recycler);

		setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));

		recyclerView = (RecyclerView) findViewById(R.id.recycler);

		List<String> your_array_list = Arrays.asList(
				"This", "Is", "An", "Example", "RecyclerView", "That", "You", "Can", "Scroll", ".",
				"It", "Shows", "How", "Any", "Scrollable", "View",
				"Can", "Be", "Included", "As", "A", "Child", "Of", "SlidingUpPanelLayout",
				"This", "Is", "An", "Example", "RecyclerView", "That", "You", "Can", "Scroll", ".",
				"It", "Shows", "How", "Any", "Scrollable", "View",
				"Can", "Be", "Included", "As", "A", "Child", "Of", "SlidingUpPanelLayout",
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum pulvinar tellus odio, a accumsan lacus facilisis sed. Fusce vulputate mauris ac convallis congue. Proin pulvinar nulla in dictum ultricies. In erat velit, commodo gravida dui nec, feugiat mollis lacus. Proin eu sem non orci euismod aliquet. Quisque tortor massa, varius ut purus sed, vehicula gravida magna. Phasellus pulvinar nulla eget ipsum dapibus gravida. Ut est libero, aliquet at libero ut, volutpat mollis lacus. Praesent ac gravida eros. Etiam placerat, nibh eu malesuada fringilla, dolor turpis aliquet ipsum, ut tempor nisi turpis non lectus. Vestibulum porta facilisis nisi at sagittis. Pellentesque mattis dui enim, at pellentesque lectus hendrerit hendrerit.\n"
		);

		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setHasFixedSize(true);
		recyclerView.setAdapter(
				new MyAdapter(
						your_array_list, new MyOnClickListener()
				)
		);

		mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		mLayout.setPanelSlideListener(
				new PanelSlideListener() {
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
				}
		);

		TextView t = (TextView) findViewById(R.id.name);
		t.setText(Html.fromHtml(getString(R.string.hello)));
		Button f = (Button) findViewById(R.id.follow);
		f.setText(Html.fromHtml(getString(R.string.follow)));
		f.setMovementMethod(LinkMovementMethod.getInstance());
		f.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse("http://www.twitter.com/umanoapp"));
						startActivity(i);
					}
				}
		);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.demo, menu);
		MenuItem item = menu.findItem(R.id.action_toggle);
		if (mLayout != null) {
			if (mLayout.getPanelState() == PanelState.HIDDEN) {
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
		switch (item.getItemId()) {
			case R.id.action_toggle: {
				if (mLayout != null) {
					if (mLayout.getPanelState() != PanelState.HIDDEN) {
						mLayout.setPanelState(PanelState.HIDDEN);
						item.setTitle(R.string.action_show);
					} else {
						mLayout.setPanelState(PanelState.COLLAPSED);
						item.setTitle(R.string.action_hide);
					}
				}
				return true;
			}
			case R.id.action_anchor: {
				if (mLayout != null) {
					if (mLayout.getAnchorPoint() == 1.0f) {
						mLayout.setAnchorPoint(0.7f);
						mLayout.setPanelState(PanelState.ANCHORED);
						item.setTitle(R.string.action_anchor_disable);
					} else {
						mLayout.setAnchorPoint(1.0f);
						mLayout.setPanelState(PanelState.COLLAPSED);
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
		if (mLayout != null &&
				(mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED)) {
			mLayout.setPanelState(PanelState.COLLAPSED);
		} else {
			super.onBackPressed();
		}
	}

	public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.StringHolder> {
		private List<String> dataList = new ArrayList<String>();
		private OnClickListener listener;

		public MyAdapter(List<String> your_array_list, OnClickListener listener) {
			dataList = your_array_list;
			this.listener = listener;
		}

		public static class StringHolder extends RecyclerView.ViewHolder {
			public TextView data;
			public Button   button;

			public StringHolder(View v) {
				super(v);
				data = (TextView) v.findViewById(R.id.card_title);
				button = (Button) v.findViewById(R.id.button);
			}
		}

		@Override
		public int getItemCount() {
			return dataList.size();
		}

		@Override
		public void onBindViewHolder(MyAdapter.StringHolder vho, final int pos) {
			String pi = dataList.get(pos);
			vho.data.setText(pi);
		}

		@Override
		public MyAdapter.StringHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview, parent, false);
			v.setOnClickListener(listener);
			StringHolder vh = new StringHolder(v);
			return vh;
		}
	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			int itemPosition = recyclerView.getChildAdapterPosition(v);
			Log.e(RecyclerDemoActivity.class.getName(), String.valueOf(itemPosition));
			Toast.makeText(v.getContext(), "onItemClick #" + itemPosition, Toast.LENGTH_SHORT).show();
		}
	}
}
