package com.p2c.thelife;

import java.security.acl.Group;
import java.util.ArrayList;

import org.json.JSONObject;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.AbstractDS.DSRefreshedListener;
import com.p2c.thelife.model.GroupModel;


/**
 * Show the owner's groups.
 * 
 * This class does not poll the server for changes in the user's list of groups, 
 * because this data doesn't change unless the user initiates the add group/delete group.
 * So instead of polling, just refresh when an add/delete operation occurs.
 * Another alternative would be to use GCM.
 * @author clarence
 *
 */
public class GroupsFragment extends NavigationDrawerFragment
    implements OnItemLongClickListener, OnItemClickListener, Server.ServerListener, GroupCreateDialog.Listener, DSRefreshedListener {

    private static final String TAG = "GroupsFragment";

    private GroupsAdapter m_adapter = null;
    private ProgressDialog m_progressDialog = null;
    private GroupModel m_group = null;
    private View m_noGroupsView = null;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        DrawerActivity activity = (DrawerActivity) getActivity();
        activity.setTitle(getString(R.string.title_groups));
        activity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // attach the list view and adapter
        ListView groupsList = (ListView)getActivity().findViewById(R.id.groups_list);
        m_adapter = new GroupsAdapter(getActivity(), android.R.layout.simple_list_item_1);
        groupsList.setAdapter(m_adapter);

        // show a message if there are no events
        m_noGroupsView = (TextView)getActivity().findViewById(R.id.groups_none);
        setNoGroupsVisibility();

        groupsList.setOnItemClickListener(this);
        groupsList.setOnItemLongClickListener(this);

        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Activity in view, so start the data store refresh mechanism.
     */
    @Override
    public void onResume() {
		super.onResume();

        // load the database from the server in the background
        if (TheLifeConfiguration.getOwnerDS().isValidOwner()) {
            TheLifeConfiguration.getGroupsDS().addDSChangedListener(m_adapter);
            TheLifeConfiguration.getGroupsDS().addDSRefreshedListener(this);
            TheLifeConfiguration.getGroupsDS().forceRefresh(null);
        }
    }

    /**
     * Activity out of view, so stop the data store refresh mechanism.
     */
    @Override
    public void onPause() {
        super.onPause();

        TheLifeConfiguration.getGroupsDS().removeDSChangedListener(m_adapter);
        TheLifeConfiguration.getGroupsDS().removeDSRefreshedListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.groups, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            DrawerActivity activity = (DrawerActivity) getActivity();
            activity.showHelpDialog("Help", R.string.activity_groups_help);
        } else if (item.getItemId() == R.id.action_search) {
            Intent intent = new Intent("com.p2c.thelife.GroupsSearch");
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_new) {
            GroupCreateDialog dialog = new GroupCreateDialog();
            dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());
        } else if (item.getItemId() == android.R.id.home) {
//			m_support.slideOpen();
        }

        return true;
    }

    @Override
    public void notifyAttemptingServerAccess(String indicator) {
        if (indicator.equals("groupCreate")) {
            m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.waiting), getResources().getString(R.string.creating_new_group), true, true);
        } else if (indicator.equals("groupDelete")) {
            m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.waiting), getResources().getString(R.string.deleting_group), true, true);
        }
    }

    @Override
    public void notifyServerResponseAvailable(String indicator, int httpCode, JSONObject jsonObject, String errorString) {

        if (Utilities.isSuccessfulHttpCode(httpCode)) {

            // successful server call (createGroup, deleteGroup)

            // server call "deleteGroup" does not return the deleted id, so it can't be handled
            if (indicator.equals("createGroup")) {
                int groupId = jsonObject.optInt("id", 0);
                if (groupId != 0) {
                    String name = jsonObject.optString("name", "");
                    String description = jsonObject.optString("full_description", "");

                    // add the group to the list of known groups
                    ArrayList<Integer> memberIds = new ArrayList<Integer>();
                    memberIds.add(TheLifeConfiguration.getOwnerDS().getId());
                    GroupModel group = new GroupModel(groupId, name, description, TheLifeConfiguration.getOwnerDS().getId(), memberIds);
                    TheLifeConfiguration.getGroupsDS().add(group);
                    TheLifeConfiguration.getGroupsDS().notifyDSChangedListeners();
                }
            }

            // refresh the cache
            TheLifeConfiguration.getGroupsDS().forceRefresh(null);
        }

        if (m_progressDialog != null) {
            m_progressDialog.dismiss();
        }
    }

    @Override
    public void notifyDSRefreshed(String indicator) {
        setNoGroupsVisibility();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        // get the group associated with this view
        GroupModel group = (GroupModel)arg1.getTag();

        Fragment newFragment = GroupFragment.newInstance(group.id);
        getFragmentManager().beginTransaction()
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.flContent, newFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        // get the group associated with this view
        m_group = (GroupModel)arg1.getTag();

        GroupDeleteDialog dialog = new GroupDeleteDialog();
        dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());

        return true; // consumed

    }

    public GroupModel getSelectedGroup() {
        return m_group;
    }

    private void setNoGroupsVisibility() {
        if (m_adapter.getCount() > 0)
            m_noGroupsView.setVisibility(View.GONE);
        else
            m_noGroupsView.setVisibility(View.VISIBLE);
    }

}
