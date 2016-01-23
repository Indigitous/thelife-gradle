package com.p2c.thelife;

import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.GroupModel;
import com.p2c.thelife.model.GroupUsersDS;
import com.p2c.thelife.model.UserModel;


/**
 * Show the users in the given group.
 * @author clarence
 *
 */
public class GroupFragment extends NavigationDrawerFragment implements Server.ServerListener, GroupDeleteUserDialog.Listener, OnItemClickListener, OnItemLongClickListener {
	
	private static final String TAG = "GroupFragment";
	private static  String KEY_GROUP_ID = "group_id";
	
	private GroupModel m_group = null;	
	private GroupAdapter m_adapter = null;
	private UserModel m_user = null;
	private ProgressDialog m_progressDialog = null;	
	private GroupUsersDS m_groupUsersDS = null;

    // Factory to create new instance of fragment with group ID bundled
    static GroupFragment newInstance(int groupId) {
        GroupFragment f = new GroupFragment();
        Bundle args = new Bundle();

        args.putInt(KEY_GROUP_ID, groupId);
        f.setArguments(args);

        return f;
    }

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_group, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		getActivity().setTitle(getString(R.string.title_group));
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		// Get the group
		int groupId = getArguments().getInt(KEY_GROUP_ID, -1);
		m_group = TheLifeConfiguration.getGroupsDS().findById(groupId);

		// Show the group
		if (m_group != null) {
			TextView textView = (TextView)getActivity().findViewById(R.id.activity_group_name);
			textView.setText(m_group.name);
			textView = (TextView)getActivity().findViewById(R.id.activity_group_description);
			textView.setText(m_group.description);

			// data store of users in this group
			m_groupUsersDS = new GroupUsersDS(getActivity(), null, m_group.id);

			// attach the users-in-group view
			GridView usersView = (GridView)getActivity().findViewById(R.id.activity_group_users);
			m_adapter = new GroupAdapter(getActivity(), android.R.layout.simple_list_item_1, m_group, m_groupUsersDS);
			usersView.setAdapter(m_adapter);

			usersView.setOnItemClickListener(this);
			usersView.setOnItemLongClickListener(this);
		}
	}
	
	/**
	 * Activity in view, so start the data store refresh mechanism.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		if (m_group != null) {
			// load the database from the server in the background
			m_groupUsersDS.addDSChangedListener(m_adapter);
			m_groupUsersDS.forceRefresh(null);
			
			// listen for user bitmaps
			TheLifeConfiguration.getBitmapNotifier().addUserBitmapListener(m_adapter);
		}
	}		
	
	/**
	 * Activity out of view, so stop the data store refresh mechanism.
	 */
	@Override
	public void onPause() {
		super.onPause();
		
		if (m_group != null) {
			m_groupUsersDS.removeDSChangedListener(m_adapter);
			
			// stop listening for user bitmaps
			TheLifeConfiguration.getBitmapNotifier().removeUserBitmapListener(m_adapter);			
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.group, menu);
		
		// if the user is not the group leader then don't let the user add to the group
		if (m_group != null && m_group.leader_id != TheLifeConfiguration.getOwnerDS().getId()) {
			MenuItem MenuItem = menu.findItem(R.id.action_new);
			MenuItem.setVisible(false);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_help) {
			Intent intent = new Intent("com.p2c.thelife.HelpContainer");
			intent.putExtra("layout", R.layout.activity_group_help);
			intent.putExtra("position", SlidingMenuSupport.GROUPS_POSITION);
			intent.putExtra("home", "com.p2c.thelife.Group");
			if (m_group != null) {
				intent.putExtra("group_id", m_group.id);
			}
			startActivity(intent);
		} else if (item.getItemId() == R.id.action_new) {
			if (m_group != null) {
				Intent intent = new Intent("com.p2c.thelife.GroupInvite");
				intent.putExtra("group_id", m_group.id);
				startActivity(intent);
			}
		}  else if (item.getItemId() == android.R.id.home) {
			Intent intent = new Intent("com.p2c.thelife.Groups");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);			
			startActivity(intent);			
		}
		
		return true;
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		m_user = (UserModel)arg1.getTag();
		if (m_user != null) {
			Intent intent = new Intent("com.p2c.thelife.EventsForUser");
			intent.putExtra("group_id", m_group.id);			
			JSONObject userJSON = m_user.toJSON();
			if (userJSON != null) {
				intent.putExtra("user_json", userJSON.toString());
			}
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);			
			startActivity(intent);
		}
	}
	
	
	/**
	 * User has been selected for deletion.
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		m_user = (UserModel)arg1.getTag();
		GroupDeleteUserDialog dialog = new GroupDeleteUserDialog();
		dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());
		
		return true;
	}


	/**
	 * Delete the user in the group.
	 * @param view
	 */
	public void selectUser(View view) {
		m_user = (UserModel)view.getTag();
		
		GroupDeleteUserDialog dialog = new GroupDeleteUserDialog();
		dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());
	}
	
	
	public GroupModel getSelectedGroup() {
		return m_group;
	}
	
	
	public UserModel getSelectedUser() {
		return m_user;
	}
	
	
	@Override
	public void notifyAttemptingServerAccess(String indicator) {
		if (indicator.equals("deleteUserFromGroup")) {
			m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.waiting), getResources().getString(R.string.deleting_user), true, true);
		}
	}

	
	@Override
	public void notifyServerResponseAvailable(String indicator, int httpCode, JSONObject jsonObject, String errorString) {
		
		// deleteUserFromGroup does not return the deleted id
		
		m_groupUsersDS.forceRefresh(null);		
		
		if (m_progressDialog != null) {
			m_progressDialog.dismiss();
		}				
	}

}
