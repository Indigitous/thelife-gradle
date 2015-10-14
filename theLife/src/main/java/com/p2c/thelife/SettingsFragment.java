package com.p2c.thelife;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.p2c.thelife.Server.ServerListener;
import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.UserModel;
import com.p2c.thelife.push.GCMSupport;

import org.json.JSONObject;

/**
 * Edit the Owner's profile.
 *
 * @author clarence
 */
public class SettingsFragment extends NavigationDrawerFragment implements ServerListener, ImageSelectSupport.Listener {

  private static final String TAG = "SettingsFragment";

  private ProgressDialog m_progressDialog = null;
  private Bitmap m_updatedBitmap = null;
  private UserModel m_updatedUser = null;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_settings, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().setTitle(getString(R.string.title_settings));
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    // show the progress dialog while getting the user profile
    m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.waiting), getResources().getString(R.string.retrieving_account), true, true);
    Server server = new Server(getActivity());
    server.queryUserProfile(TheLifeConfiguration.getOwnerDS().getId(), this, "queryUserProfile");

    // Setup button click listeners
    Button showLicenseButton = (Button) getActivity().findViewById(R.id.show_license);
    ImageView userAvatar = (ImageView) getActivity().findViewById(R.id.settings_image);
    Button updatePhotoPrompt  = (Button) getActivity().findViewById(R.id.settings_update_photo_prompt);

    Button saveButton = (Button) getActivity().findViewById(R.id.button1);

    showLicenseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showLicense(v);
      }
    });

    userAvatar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectImage(v);
      }
    });

    updatePhotoPrompt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectImage(v);
      }
    });
    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setUserProfile(v);
      }
    });

    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.settings, menu);
  }


  public boolean setUserProfile(View view) {

    // set the updated user record
    TextView textView = null;
    textView = (TextView) getActivity().findViewById(R.id.settings_first_name);
    String firstName = textView.getText().toString();
    textView = (TextView) getActivity().findViewById(R.id.settings_last_name);
    String lastName = textView.getText().toString();
    textView = (TextView) getActivity().findViewById(R.id.settings_email);
    String email = textView.getText().toString();
    textView = (TextView) getActivity().findViewById(R.id.settings_phone);
    String phone = textView.getText().toString();
    m_updatedUser = new UserModel(
        TheLifeConfiguration.getOwnerDS().getId(),
        firstName,
        lastName,
        email,
        phone,
        TheLifeConfiguration.getOwnerDS().getProvider());

    // call the server
    m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.waiting), getResources().getString(R.string.storing_account), true, true);
    Server server = new Server(getActivity());
    server.updateUserProfile(
        TheLifeConfiguration.getOwnerDS().getId(),
        firstName, lastName,
        email,
        phone,
        GCMSupport.getInstance().getRegistrationId(), // keep the current value
        this,
        "updateUserProfile");
    return true;
  }


  @Override
  public void notifyServerResponseAvailable(String indicator, int httpCode, JSONObject jsonObject, String errorString) {

    try {
      if (indicator.equals("queryUserProfile")) {

        // Update the UI with the result of the query.
        // But if the query failed, use the local information.

        // use the existing app user record
        UserModel user = TheLifeConfiguration.getOwnerDS().getOwner();

        // update app user record with latest from server
        if (Utilities.isSuccessfulHttpCode(httpCode) && jsonObject != null) {
          user.setFromPartialJSON(jsonObject);
          TheLifeConfiguration.getOwnerDS().setOwner(user);
        }

        // update the UI
        TextView textView = null;
        textView = (TextView) getActivity().findViewById(R.id.settings_first_name);
        textView.setText(user.firstName);
        textView = (TextView) getActivity().findViewById(R.id.settings_last_name);
        textView.setText(user.lastName);
        textView = (TextView) getActivity().findViewById(R.id.settings_email);
        textView.setText(user.email);
        textView = (TextView) getActivity().findViewById(R.id.settings_phone);
        textView.setText(user.mobile);

        // show the update photo prompt if there's no bitmap in the cache
        int ownerId = TheLifeConfiguration.getOwnerDS().getId();
        showUpdatePhotoPrompt(!UserModel.isInCache(ownerId));

        Bitmap bitmap = UserModel.getImage(ownerId);
        ImageView imageView = (ImageView) getActivity().findViewById(R.id.settings_image);
        imageView.setImageBitmap(bitmap);

        closeProgressBar();

      } else if (indicator.equals("updateUserProfile")) {

        if (Utilities.isSuccessfulHttpCode(httpCode)) {

          // update the app user
          TheLifeConfiguration.getOwnerDS().setOwner(m_updatedUser);

          // have updated the user profile, so now update the user profile image if necessary
          if (m_updatedBitmap != null) {
            updateImageOnServer(m_updatedBitmap);
          } else {
            closeProgressBar();
            getActivity().getSupportFragmentManager().popBackStack();
            Utilities.showInfoToast(getActivity(), getResources().getString(R.string.user_profile_updated), Toast.LENGTH_SHORT);
          }
        } else {
          closeProgressBar();
        }

      } else if (indicator.equals("updateImage")) {

        if (Utilities.isSuccessfulHttpCode(httpCode)) {
          TheLifeConfiguration.getOwnerDS().notifyDSChangedListeners();
          m_updatedBitmap = null;
          Utilities.showInfoToast(getActivity(), getResources().getString(R.string.user_profile_updated), Toast.LENGTH_SHORT);
        }
        closeProgressBar();
      }
    } catch (Exception e) {
      Log.e(TAG, "notifyServerResponseAvailable() " + indicator, e);
    }
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_logout) {
      // TODO make logout work
      new AlertDialog.Builder(getActivity())
          .setTitle(getResources().getString(R.string.logout_prompt))
          .setNegativeButton(R.string.cancel, null)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

              // log out of app
              TheLifeConfiguration.getOwnerDS().setOwner(null);

              // go to main screen
              Intent intent = new Intent("com.p2c.thelife.Initial");
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(intent);

            }
          }).show();
    } else if (item.getItemId() == R.id.action_help) {
      DrawerActivity activity = (DrawerActivity) getActivity();
      activity.showHelpDialog("Help", R.string.activity_settings_help);
    }

    return true;
  }


  /**
   * User has selected their image for a possible update.
   *
   * @param view
   */
  public void selectImage(View view) {
    ImageSelectDialog dialog = new ImageSelectDialog();
    dialog.show(getFragmentManager(), dialog.getClass().getSimpleName());
  }


  /**
   * Get the result of the photo gathering intent.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {

    // let the support object handle it
    ImageSelectSupport support = new ImageSelectSupport(getActivity(), this);
    support.onActivityResult(requestCode, resultCode, intent);
  }


  /**
   * Callback from ImageSelectSupport
   */
  @Override
  public void notifyImageSelected(Bitmap bitmap) {
    m_updatedBitmap = bitmap;

    // set the image
    ImageView imageView = (ImageView) getActivity().findViewById(R.id.settings_image);
    imageView.setImageBitmap(m_updatedBitmap);
    showUpdatePhotoPrompt(false);
  }


  /**
   * @param show whether or not to show the update photo prompt
   */
  private void showUpdatePhotoPrompt(boolean show) {
    Button updatePhotoPrompt = (Button) getActivity().findViewById(R.id.settings_update_photo_prompt);
    updatePhotoPrompt.setVisibility(show ? View.VISIBLE : View.GONE);
  }


  /**
   * Update the image on the server.
   *
   * @param bitmap
   */
  private void updateImageOnServer(Bitmap bitmap) {
    BitmapCacheHandler.saveBitmapToCache("users", TheLifeConfiguration.getOwnerDS().getId(), "image", bitmap);
    Server server = new Server(getActivity());
    server.updateImage("users", TheLifeConfiguration.getOwnerDS().getId(), this, "updateImage");
  }


  private void closeProgressBar() {
    if (m_progressDialog != null) {
      m_progressDialog.dismiss();
      m_progressDialog = null;
    }
  }


  public void rotateImageCW(View view) {
    rotateImage(90.0f);
  }


  public void rotateImageCCW(View view) {
    rotateImage(-90.0f);
  }


  private void rotateImage(float angle) {
    ImageView imageView = (ImageView) getActivity().findViewById(R.id.settings_image);

    if (m_updatedBitmap == null) {
      m_updatedBitmap = Utilities.getBitmapFromDrawable(imageView.getDrawable());
    }

    // rotate image in memory
    Matrix matrix = new Matrix();
    matrix.setRotate(angle);
    m_updatedBitmap = Bitmap.createBitmap(m_updatedBitmap, 0, 0, m_updatedBitmap.getWidth(), m_updatedBitmap.getHeight(), matrix, true);

    // save new image bitmap
    imageView.setImageBitmap(m_updatedBitmap);
  }

  public void showLicense(View view) {
    DrawerActivity activity = (DrawerActivity) getActivity();
    activity.showDialog("License Agreement", getString(R.string.license));
  }

}
