package com.p2c.thelife;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
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
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.AbstractDS;
import com.p2c.thelife.model.FriendModel;

import org.json.JSONObject;

/**
 * @author jordan
 */
public class FriendsFragment extends NavigationDrawerFragment implements AbstractDS.DSRefreshedListener, Server.ServerListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private static final String TAG = "FriendsFragment";

  private FriendsAdapter mAdapter = null;
  private FriendModel mFriend = null; // selected friend
  private ProgressDialog mProgressDialog = null;
  private View mNoFriendsView = null;

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

    mFriend = (FriendModel) view.getTag();

    FriendDeleteDialog dialog = new FriendDeleteDialog();
    dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());

    return true;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    mFriend = (FriendModel) view.getTag();
    Log.d(TAG, "Friend: " + mFriend);

    // TODO fragment transaction for clicking on friend
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_friends, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().setTitle(getString(R.string.title_friends));
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    GridView friendsGrid = (GridView) getActivity().findViewById(R.id.grid_friends);
    mAdapter = new FriendsAdapter(getActivity(), android.R.layout.simple_list_item_1);
    friendsGrid.setAdapter(mAdapter);

    mNoFriendsView = (TextView) getActivity().findViewById(R.id.friends_none);
    setNoFriendsVisibility();

    friendsGrid.setOnItemClickListener(this);
    friendsGrid.setOnItemLongClickListener(this);

    // show help if owner just added a friend for the first time
    if (!TheLifeConfiguration.getOwnerDS().getHasAddedFriend() &&
        TheLifeConfiguration.getFriendsDS().count() > 0) {
      TheLifeConfiguration.getOwnerDS().setHasAddedFriend();
      showFirstTimeAddingFriendHelp();
    }

    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    // load the db from the server in background
    if (TheLifeConfiguration.getOwnerDS().isValidOwner()) {
      TheLifeConfiguration.getFriendsDS().addDSChangedListener(mAdapter);
      TheLifeConfiguration.getFriendsDS().addDSRefreshedListener(this);
      TheLifeConfiguration.getFriendsDS().refresh(null);
      TheLifeConfiguration.getBitmapNotifier().addFriendBitmapListener(mAdapter);
    }
  }

  @Override
  public void notifyDSRefreshed(String indicator) {
    setNoFriendsVisibility();
  }

  @Override
  public void onPause() {
    super.onPause();

    TheLifeConfiguration.getFriendsDS().removeDSChangedListener(mAdapter);
    TheLifeConfiguration.getFriendsDS().removeDSRefreshedListener(this);
    TheLifeConfiguration.getBitmapNotifier().removeFriendBitmapListener(mAdapter);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.friends, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_help) {
      DrawerActivity activity = (DrawerActivity) getActivity();
      activity.showHelpDialog("Help", R.string.activity_friends_help);
    } else if (item.getItemId() == R.id.action_new) {
      Fragment newFragment = new FriendsImportFragment();
      getFragmentManager().beginTransaction()
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .replace(R.id.flContent, newFragment)
          .addToBackStack(null)
          .commit();
    }

    return true;
  }

  // TODO add progress dialog for deleting friend, etc.

  @Override
  public void notifyServerResponseAvailable(String indicator, int httpCode, JSONObject jsonObject, String errorString) {
    // Successful server call (deleteFriend)
    // so refresh the cache
    TheLifeConfiguration.getFriendsDS().forceRefresh(null);
  }

  private void setNoFriendsVisibility() {
    if (mAdapter.getCount() > 0)
      mNoFriendsView.setVisibility(View.GONE);
    else
      mNoFriendsView.setVisibility(View.VISIBLE);
  }

  // TODO make this dialog look better
  private void showFirstTimeAddingFriendHelp() {
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

    LayoutInflater inflater = LayoutInflater.from(getActivity());
    final View view = inflater.inflate(R.layout.dialog_first_time_adding_friend_help, null);
    WebView webView = (WebView)view.findViewById(R.id.dialog_adding_friend_help_message);

    String help = getResources().getString(R.string.style_sheet_help);
    help += getResources().getString(R.string.first_time_adding_friend_help);
    webView.loadDataWithBaseURL(null, help, "text/html", "utf-8", null);
    alertBuilder.setView(view);

    // set the buttons of the alert
    alertBuilder.setNeutralButton(R.string.done, null);

    // display it
    alertBuilder.show();
  }
}
