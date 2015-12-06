package com.p2c.thelife;

import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.OwnerDS;
import com.p2c.thelife.model.UserModel;
import com.p2c.thelife.view.RoundedImageView;

/**
 * @author jordan
 */

public class DrawerActivity extends AppCompatActivity implements OwnerDS.DSChangedListener {
  private FragmentNavigationDrawer dlDrawer;
  private static final String TAG = "DrawerActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_drawer);

    // Set a Toolbar to replace the ActionBar.
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Find our drawer view
    dlDrawer = (FragmentNavigationDrawer) findViewById(R.id.drawer_layout);
    // Setup drawer view
    dlDrawer.setupDrawerConfiguration((LinearLayout) findViewById(R.id.drawerView), (ListView) findViewById(R.id.lvDrawer), toolbar,
        R.layout.drawer_nav_item, R.id.flContent);
    // Add nav items
    dlDrawer.addNavItem(getString(R.string.title_community), R.drawable.menu_community, getString(R.string.title_community), EventsForCommunityFragment.class);
    dlDrawer.addNavItem(getString(R.string.title_friends), R.drawable.menu_friends, getString(R.string.title_friends), FriendsFragment.class);
    dlDrawer.addNavItem(getString(R.string.title_group), R.drawable.menu_groups, getString(R.string.title_group), GroupFragment.class);
    // Select default
    if (savedInstanceState == null) {
      dlDrawer.selectDrawerItem(0);
    }
  }


  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // If the nav drawer is open, hide action items related to the content
    if (dlDrawer.isDrawerOpen()) {
      // Uncomment to hide menu items
      // menu.findItem(R.id.mi_test).setVisible(false);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    // Uncomment to inflate menu items to Action Bar
    // inflater.inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // The action bar home/up action should open or close the drawer.
    // ActionBarDrawerToggle will take care of this.
    if (dlDrawer.getDrawerToggle().onOptionsItemSelected(item)) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    dlDrawer.getDrawerToggle().syncState();
    showOwner();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Pass any configuration change to the drawer toggles
    dlDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
  }

  @Override
  public void notifyOwnerDSChanged() {
    showOwner();
  }

  public void showDialog(CharSequence title, CharSequence body) {
    DialogFragment newFragment = TheLifeDialogFragment.newInstance(title, body);
    newFragment.show(getSupportFragmentManager(), "dialog");
  }

  public void showHelpDialog(CharSequence title, CharSequence msg) {
    DialogFragment newFragment = HelpDialogFragment.newInstance(title, msg);
    newFragment.show(getSupportFragmentManager(), "dialog");
  }

  public void showHelpDialog(int title, int msg) {
    showHelpDialog(getString(title), getString(msg));
  }

  public void showHelpDialog(int title, CharSequence msg) {
    showHelpDialog(getString(title), msg);
  }

  public void showHelpDialog(CharSequence title, int msg) {
    showHelpDialog(title, getString(msg));
  }

  public boolean isCurrentFragment(Fragment f) {
    return dlDrawer.isCurrentFragment(f);
  }

  private void showOwner() {
    // show the app user
    if (TheLifeConfiguration.getOwnerDS().isValidOwner()) {

      // listener for the owner views
      View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Fragment newFragment = new EventsForOwnerFragment();

          if (!isCurrentFragment(newFragment)) {
            getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.flContent, newFragment)
                .addToBackStack(null)
                .commit();
          }
          dlDrawer.closeDrawers();
        }
      };

      RoundedImageView imageView = (RoundedImageView) dlDrawer.findViewById(R.id.drawer_user_image);
      imageView.setImageInfo(UserModel.getImage(TheLifeConfiguration.getOwnerDS().getId()),
          getResources().getColor(R.color.app_menu_owner_background),
          getResources().getDimension(R.dimen.app_menu_owner_side),
          getResources().getDimension(R.dimen.app_menu_owner_radius));
      imageView.setOnClickListener(listener);

      TextView textView = (TextView) dlDrawer.findViewById(R.id.drawer_user_name);
      textView.setText(TheLifeConfiguration.getOwnerDS().getOwner().getFullName());
      textView.setOnClickListener(listener);
    }
  }

}