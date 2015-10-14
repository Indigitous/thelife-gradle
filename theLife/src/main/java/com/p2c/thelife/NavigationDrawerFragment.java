package com.p2c.thelife;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.p2c.thelife.config.TheLifeConfiguration;

/**
 * @author jordan
 */
public class NavigationDrawerFragment extends Fragment {
  @Override
  public void onResume() {
    super.onResume();

    // guard against a not-valid user
    if (!TheLifeConfiguration.getOwnerDS().isValidOwner()) {
      Intent intent = new Intent("com.p2c.thelife.Setup");
      startActivity(intent);
      getActivity().finish();
    }

    // defensive programming: in case of problems with GCM, refresh requests datastore
    TheLifeConfiguration.getRequestsDS().refresh("application");
  }
}
