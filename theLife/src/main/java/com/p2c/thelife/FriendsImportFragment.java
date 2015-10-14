package com.p2c.thelife;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.p2c.thelife.config.TheLifeConfiguration;
import com.p2c.thelife.model.AbstractDS;
import com.p2c.thelife.model.FriendModel;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author jordan
 */
public class FriendsImportFragment extends NavigationDrawerFragment {

  private static String TAG = "FriendsImportActivity";

  private static final int REQUESTCODE_IMPORT_FROM_CONTACTS = 1;
  private static final int REQUESTCODE_IMPORT_FROM_FACEBOOK = 2;

  private Bitmap m_bitmap = null;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_friends_import, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().setTitle(getString(R.string.title_friends_import));

    Button internalContactButton = (Button) getActivity().findViewById(R.id.button1);
    Button facebookContactButton = (Button) getActivity().findViewById(R.id.button2);
    Button manualContactButton = (Button) getActivity().findViewById(R.id.button3);

    internalContactButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        importFriendsByInternalContact(v);
      }
    });

    facebookContactButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        importFriendsByFacebook(v);
      }
    });

    manualContactButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        importFriendManually(v);
      }
    });
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }


  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.friends_import, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // TODO make the help work
    if (item.getItemId() == R.id.action_help) {
      Intent intent = new Intent("com.p2c.thelife.HelpContainer");
      intent.putExtra("layout", R.layout.activity_events_for_community_help);
      intent.putExtra("position", SlidingMenuSupport.COMMUNITY_POSITION);
      intent.putExtra("home", "com.p2c.thelife.EventsForCommunity");
      startActivity(intent);
    }

    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent contactData) {

    if (resultCode == getActivity().RESULT_OK) {
      if (requestCode == REQUESTCODE_IMPORT_FROM_CONTACTS) {
        // user has selected a contact

        Cursor cursor = null;
        try {
          // ask for the contact information from the provider
          Uri selectedContact = contactData.getData();
          cursor = getActivity().getContentResolver().query(
              selectedContact,
              new String[] { ContactsContract.Contacts._ID },
              null,
              null,
              null);

          if (cursor == null || !cursor.moveToNext()) {
            Utilities.showErrorToast(getActivity(), getResources().getString(R.string.import_friend_error), Toast.LENGTH_SHORT);
          } else {
            // get the contact id
            int contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            cursor.close();
            cursor = null;

            // get the name information
            cursor = getActivity().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Data._ID, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME },
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "= '" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'",                    // Selection criteria
                new String[] { String.valueOf(contactId) },
                null);

            if (cursor == null || !cursor.moveToNext()) {
              Utilities.showErrorToast(getActivity(), getResources().getString(R.string.import_friend_error), Toast.LENGTH_SHORT);
            } else {

              // success: access the name information
              int fnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
              int lnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
              String firstName = cursor.getString(fnIndex);
              String lastName = cursor.getString(lnIndex);
              cursor.close();
              cursor = null;

              // email and mobile aren't necessary
              String email = null;
              String mobile = null;

              // try to get photo
              m_bitmap = null;
              cursor = getActivity().getContentResolver().query(
                  ContactsContract.Data.CONTENT_URI,
                  new String[] { ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Photo.PHOTO },
                  ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "= '" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'",                    // Selection criteria
                  new String[] { String.valueOf(contactId) },
                  null);
              if (cursor != null && cursor.moveToNext()) {
                int pIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO);
                byte[] photoBlob = cursor.getBlob(pIndex);
                if (photoBlob != null) {
                  m_bitmap = Utilities.makeSquare(BitmapFactory.decodeByteArray(photoBlob, 0, photoBlob.length));
                }
              }
//              cursor.close();
//              cursor = null;

              // now create the friend on the server
              Log.i(TAG, "Create a friend from contacts: " + firstName + ", " + lastName + ", " + email + ", " + mobile + ", " + ((m_bitmap != null) ? " with photo" : " without photo"));
              FriendsImportSupport support = new FriendsImportSupport((AppCompatActivity) getActivity(), null);
              support.addFriendsStart(R.string.adding_friend);
              support.addFriend(firstName, lastName, email, mobile, FriendModel.Threshold.NewContact, m_bitmap);
              support.addFriendsFinish();
            }

          }
        } catch (Exception e) {
          Log.e(TAG, "onActivityResult()", e);
          Utilities.showErrorToast(getActivity(), getResources().getString(R.string.import_friend_error), Toast.LENGTH_SHORT);
        } finally {
          if (cursor != null) {
            cursor.close();
          }
        }
      }
//      else if (requestCode == REQUESTCODE_IMPORT_FROM_FACEBOOK) {
//        // user has selected Facebook friends
//
//        ArrayList<String> facebookIds = contactData.getStringArrayListExtra("facebook_ids");
//        if (facebookIds.size() > 0) {
//          FriendsImportFacebookSupport support = new FriendsImportFacebookSupport(getActivity());
//          support.addFriendsFromFacebook(facebookIds);
//        }
//      }
    }
  }

  public void importFriendsByInternalContact(View view) {
    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI); // all contacts
    startActivityForResult(intent, REQUESTCODE_IMPORT_FROM_CONTACTS);
  }

  // TODO convert activity
  public void importFriendsByFacebook(View view) {
//    Intent intent = new Intent("com.p2c.thelife.FriendsImportFacebook");
//    startActivityForResult(intent, REQUESTCODE_IMPORT_FROM_FACEBOOK);
  }

  // TODO convert activity
  public void importFriendManually(View view) {
//    Intent intent = new Intent("com.p2c.thelife.FriendImportManually");
//    startActivity(intent);
  }
}
