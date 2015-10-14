package com.p2c.thelife;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.EventsDS;
import com.p2c.thelife.model.UserModel;

/**
 * @author jordan
 */
public class EventsForOwnerFragment extends NavigationDrawerFragment implements EventsDS.DSRefreshedListener {

  private static final String TAG = "EventsForOwnerFragment";

  private ListView mListView = null;
  private EventsForUserAdapter mAdapter = null;
  private TextView mNoEventsView = null;

  // refresh the events list view
  private Runnable mDisplayRefreshRunnable = null;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_events_for_owner, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().setTitle(getString(R.string.title_friends));
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    UserModel owner = TheLifeConfiguration.getOwnerDS().getOwner();

    if (owner != null) {
      ImageView imageView = (ImageView) getActivity().findViewById(R.id.activity_owner_image);
      imageView.setImageBitmap(UserModel.getImage(owner.id));

      TextView nameView = (TextView) getActivity().findViewById(R.id.activity_owner_name);
      nameView.setText(owner.getFullName());
    }

    // attach the event list view
    mListView = (ListView) getActivity().findViewById(R.id.activity_owner_events);
    mAdapter = new EventsForUserAdapter(getActivity(), android.R.layout.simple_list_item_1, TheLifeConfiguration.getOwnerDS().getId());
    mListView.setAdapter(mAdapter);

    // show messaage if there are no events
    mNoEventsView = (TextView) getActivity().findViewById(R.id.events_for_owner_none);
    setNoEventsVisibility();

    // runnable to refresh the timestamps in the events list view
    mDisplayRefreshRunnable = new Runnable() {
      @Override
      public void run() {
        if (mAdapter != null && mListView != null) {
          mAdapter.notifyDataSetChanged();

          // refresh the display again in one minute
          mListView.postDelayed(mDisplayRefreshRunnable, 60 * 1000);
        }
      }
    };

    Button editProfileButton = (Button) getActivity().findViewById(R.id.edit_profile_button);
    editProfileButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        editProfile(v);
      }
    });

    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    // data may have changed (e.g. push notifications, so redisplay
    mAdapter.notifyDSChanged(null, null);

    // load the db from the server in background
    if (TheLifeConfiguration.getOwnerDS().isValidOwner()) {
      TheLifeConfiguration.getEventsDS().addDSChangedListener(mAdapter);
      TheLifeConfiguration.getEventsDS().addDSRefreshedListener(this);
      TheLifeConfiguration.getEventsDS().refresh(null);
    }

    // refresh the display every 60 seconds
    mListView.postDelayed(mDisplayRefreshRunnable, 60 * 1000);
  }

  @Override
  public void notifyDSRefreshed(String indicator) {
    setNoEventsVisibility();
  }

  @Override
  public void onPause() {
    super.onPause();

    // stop receiving events in the background
    TheLifeConfiguration.getFriendsDS().removeDSChangedListener(mAdapter);
    TheLifeConfiguration.getFriendsDS().removeDSRefreshedListener(this);

    // remove the display refresh
    mListView.removeCallbacks(mDisplayRefreshRunnable);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.events_for_owner, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_help) {
      DrawerActivity activity = (DrawerActivity) getActivity();
      activity.showHelpDialog("Help", R.string.activity_events_for_owner_help);
    }

    return true;
  }

  public void editProfile(View view) {
    UserModel owner = TheLifeConfiguration.getOwnerDS().getOwner();
    if (owner != null) {
      Log.d(TAG, "Edit Profile");
      Fragment newFragment = new SettingsFragment();
      getFragmentManager().beginTransaction()
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .replace(R.id.flContent, newFragment)
          .addToBackStack(null)
          .commit();
    }
  }

  private void setNoEventsVisibility() {
    if (mAdapter.getCount() > 0)
      mNoEventsView.setVisibility(View.GONE);
    else
      mNoEventsView.setVisibility(View.VISIBLE);
  }
}
