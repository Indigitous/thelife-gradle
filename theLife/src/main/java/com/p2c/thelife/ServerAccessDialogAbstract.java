package com.p2c.thelife;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.util.List;

/**
 * Superclass of dialogs that send a message to the server and wait for a response.
 * Uses a dialog fragment as per Android doc, using support library for Androids < 3.0.
 * @author clarence
 *
 */
public abstract class ServerAccessDialogAbstract extends DialogFragment {
	private static final String TAG = "SADAstract";

	public interface Listener {
		public void notifyAttemptingServerAccess(String indicator);
	}	
	
	protected Object m_listener = null;	
	
	@Override
	public abstract Dialog onCreateDialog(Bundle savedInstanceState);
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		AppCompatActivity a = (AppCompatActivity) activity;
		FragmentManager fm = a.getSupportFragmentManager();
		List<Fragment> fragments = fm.getFragments();

		m_listener = (Server.ServerListener) fragments.get(fragments.size() - 1);
	}

}
