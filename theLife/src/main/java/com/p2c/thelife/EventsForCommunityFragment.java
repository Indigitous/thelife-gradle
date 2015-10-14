package com.p2c.thelife;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.EventModel;
import com.p2c.thelife.model.EventsDS;

import org.json.JSONObject;

/**
 * @author jordan
 */
public class EventsForCommunityFragment extends NavigationDrawerFragment implements EventsDS.DSRefreshedListener, Server.ServerListener {

  private static final String TAG = "EventsForCommunityFragment";

  private ListView mListView = null;
  private EventsForCommunityAdapter mAdapter = null;
  private View mNoEventsView = null;

  // Refresh the events list view
  private Runnable mDisplayRefreshRunnable = null;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_events_for_community, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().setTitle(getString(R.string.title_events));
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    mListView = (ListView) view.findViewById(R.id.events_for_community_list);
    mAdapter = new EventsForCommunityAdapter(getActivity(), android.R.layout.simple_list_item_1);
    mListView.setAdapter(mAdapter);

    mNoEventsView = view.findViewById(R.id.events_for_community_none);
    setNoEventsViewVisibility();

    // If this activity is shown, there are no notified events
    TheLifeConfiguration.getEventsDS().setNumEventsNotified(0);

    // Refresh timestamps inside the listview's events
    // All local, no server involved
    mDisplayRefreshRunnable = new Runnable() {
      @Override
      public void run() {
        if (mAdapter != null && mListView != null) {
          mAdapter.notifyDataSetChanged();

          // Refresh display again in 60 seconds
          mListView.postDelayed(mDisplayRefreshRunnable, 60 * 1000);
        }
      }
    };

    Button button = (Button) mNoEventsView.findViewById(R.id.events_for_community_none_button);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Fragment newFragment = new FriendsImportFragment();
        getFragmentManager().beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.flContent, newFragment)
            .addToBackStack(null)
            .commit();
      }
    });

    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    // Data may have changed, so redisplay
    mAdapter.notifyDSChanged(null, null);

    if (TheLifeConfiguration.getOwnerDS().isValidOwner()) {
      TheLifeConfiguration.getEventsDS().addDSChangedListener(mAdapter);
      TheLifeConfiguration.getEventsDS().addDSRefreshedListener(this);
      TheLifeConfiguration.getEventsDS().refresh(null);
      TheLifeConfiguration.getBitmapNotifier().addUserBitmapListener(mAdapter);
      TheLifeConfiguration.getBitmapNotifier().addFriendBitmapListener(mAdapter);
    }

    // Refresh the display every 60 seconds
    mListView.postDelayed(mDisplayRefreshRunnable, 60 * 1000);
  }

  @Override
  public void notifyDSRefreshed(String indicator) {
    // Show a message if there are no events
    setNoEventsViewVisibility();
  }

  @Override
  public void onPause() {
    super.onPause();

    TheLifeConfiguration.getEventsDS().removeDSRefreshedListener(this);
    TheLifeConfiguration.getEventsDS().removeDSChangedListener(mAdapter);
    TheLifeConfiguration.getBitmapNotifier().removeUserBitmapListener(mAdapter);
    TheLifeConfiguration.getBitmapNotifier().removeFriendBitmapListener(mAdapter);

    // remove the display refresh
    mListView.removeCallbacks(mDisplayRefreshRunnable);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.events_for_community, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_help) {
      DrawerActivity activity = (DrawerActivity) getActivity();
      activity.showHelpDialog("Help", R.string.activity_events_for_community_help);
    }
    return true;
  }

  /**
   * Owner has pledged to pray for the event.
   */
  public void pledgeToPray(View view) {

    // update the event immediately (optimistically expect the event will succeed at server)
    EventModel event = (EventModel)view.getTag();
    event.hasPledged = true;
    event.pledgeCount++;

    // redisplay
    mAdapter.notifyDataSetChanged();

    // send the pledge to the server
    Server server = new Server(getActivity());
    server.pledgeToPray(event.id, this, "pledgeToPray");
  }

  @Override
  public void notifyServerResponseAvailable(String indicator,	int httpCode, JSONObject jsonObject, String errorString) {

    if (!Utilities.isSuccessfulHttpCode(httpCode)) {
      new AlertDialog.Builder(getActivity())
          .setMessage(getResources().getString(R.string.pledge_error_from_server))
          .setNegativeButton(R.string.cancel, null).show();
    }
  }

  private void setNoEventsViewVisibility() {
    if (mAdapter.getCount() == 0) {
      mNoEventsView.setVisibility(View.VISIBLE);
      Button button = (Button) mNoEventsView.findViewById(R.id.events_for_community_none_button);
      button.setVisibility(View.VISIBLE);
    } else {
      mNoEventsView.setVisibility(View.GONE);
    }
  }
}
