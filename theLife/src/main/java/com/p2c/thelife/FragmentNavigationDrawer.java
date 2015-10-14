package com.p2c.thelife;


/*
** FragmentNavigationDrawer object for use with support-v7 library
** using compatibility fragments and support actionbar
** @author Jordan
*/

import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class FragmentNavigationDrawer extends DrawerLayout {
  private ActionBarDrawerToggle drawerToggle;
  private LinearLayout drawer;
  private ListView lvDrawer;
  private Toolbar toolbar;
  private NavDrawerListAdapter drawerAdapter;
  private ArrayList<NavDrawerItem> navDrawerItems;
  private ArrayList<FragmentNavItem> drawerNavItems;
  private int drawerContainerRes;

  public void setupDrawerConfiguration(LinearLayout drawerView, ListView drawerListView, Toolbar drawerToolbar,
                                       int drawerItemRes, int drawerContainerResId) {
    // Setup navigation items array
    drawerNavItems = new ArrayList<>();
    navDrawerItems = new ArrayList<>();
    drawerContainerRes = drawerContainerResId;
    // Setup drawer list view
    drawer = drawerView;
    lvDrawer = drawerListView;
    toolbar = drawerToolbar;
    // Setup item listener
    lvDrawer.setOnItemClickListener(new FragmentDrawerItemListener());
    // ActionBarDrawerToggle ties together the the proper interactions
    // between the sliding drawer and the action bar app icon
    drawerToggle = setupDrawerToggle();
    setDrawerListener(drawerToggle);
    // Setup action buttons
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
  }

  // addNavItem("First", R.mipmap.ic_one, "First Fragment", FirstFragment.class)
  public void addNavItem(String navTitle, int icon, String windowTitle, Class<? extends Fragment> fragmentClass) {
    // adding nav drawer items to array
    navDrawerItems.add(new NavDrawerItem(navTitle, icon));
    // Set the adapter for the list view
    drawerAdapter = new NavDrawerListAdapter(getActivity(), navDrawerItems);
    lvDrawer.setAdapter(drawerAdapter);
    drawerNavItems.add(new FragmentNavItem(windowTitle, fragmentClass));
  }

  public FragmentNavigationDrawer(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public FragmentNavigationDrawer(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FragmentNavigationDrawer(Context context) {
    super(context);
  }

  /**
   * Swaps fragments in the main content view
   */
  public void selectDrawerItem(int position) {
    // Create a new fragment and specify the planet to show based on
    // position
    FragmentNavItem navItem = drawerNavItems.get(position);
    Fragment fragment = null;
    try {
      fragment = navItem.getFragmentClass().newInstance();
      Bundle args = navItem.getFragmentArgs();
      if (args != null) {
        fragment.setArguments(args);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }


    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

    if (!isCurrentFragment(fragment)) {
      // Clear the back stack in case navigating from a sub fragment
      fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

      // Insert the fragment by replacing any existing fragment
      fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(drawerContainerRes, fragment).commit();

      // Highlight the selected item, update the title, and close the drawer
      lvDrawer.setItemChecked(position, true);
    }

    closeDrawer(drawer);
  }


  public ActionBarDrawerToggle getDrawerToggle() {
    return drawerToggle;
  }

  public boolean isCurrentFragment(Fragment f) {
    FragmentManager fm = getActivity().getSupportFragmentManager();
    List<Fragment> fragments = fm.getFragments();

    // TODO fix null pointer
//    if (fragments != null && f != null) {
//      Fragment currentFragment = fragments.get(fragments.size() - 1);
//      return currentFragment.getClass() == f.getClass();
//    }

    return false;
  }

  private FragmentActivity getActivity() {
    return (FragmentActivity) getContext();
  }

  private ActionBar getSupportActionBar() {
    return ((AppCompatActivity) getActivity()).getSupportActionBar();
  }

  protected void setTitle(CharSequence title) {
    getSupportActionBar().setTitle(title);
  }

  private class FragmentDrawerItemListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      selectDrawerItem(position);
    }
  }

  private class FragmentNavItem {
    private Class<? extends Fragment> fragmentClass;
    private String title;
    private Bundle fragmentArgs;

    public FragmentNavItem(String title, Class<? extends Fragment> fragmentClass) {
      this(title, fragmentClass, null);
    }

    public FragmentNavItem(String title, Class<? extends Fragment> fragmentClass, Bundle args) {
      this.fragmentClass = fragmentClass;
      this.fragmentArgs = args;
      this.title = title;
    }

    public Class<? extends Fragment> getFragmentClass() {
      return fragmentClass;
    }

    public String getTitle() {
      return title;
    }

    public Bundle getFragmentArgs() {
      return fragmentArgs;
    }
  }

  private ActionBarDrawerToggle setupDrawerToggle() {
    return new ActionBarDrawerToggle(getActivity(), this, toolbar, R.string.drawer_open, R.string.drawer_close);
  }

  public boolean isDrawerOpen() {
    return isDrawerOpen(drawer);
  }
}
