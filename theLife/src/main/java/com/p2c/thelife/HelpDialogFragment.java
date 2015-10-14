package com.p2c.thelife;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * @author jordan
 */
public class HelpDialogFragment extends DialogFragment {

  public static HelpDialogFragment newInstance(CharSequence title, CharSequence msg) {
    HelpDialogFragment frag = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putCharSequence("title", title);
    args.putCharSequence("msg", msg);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    CharSequence title = getArguments().getCharSequence("title");
    CharSequence message = getArguments().getCharSequence("msg");

    setStyle(DialogFragment.STYLE_NORMAL, 0);

    return new AlertDialog.Builder(getActivity())
        .setIcon(R.drawable.action_help)
        .setTitle(title)
        .setMessage(message)
        .create();
  }
}