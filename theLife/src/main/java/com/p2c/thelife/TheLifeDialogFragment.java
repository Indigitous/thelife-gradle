package com.p2c.thelife;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * @author jordan
 */
public class TheLifeDialogFragment extends DialogFragment {

  static TheLifeDialogFragment newInstance(CharSequence title, CharSequence body) {
    TheLifeDialogFragment f = new TheLifeDialogFragment();

    Bundle args = new Bundle();
    args.putCharSequence("title", title);
    args.putCharSequence("body", body);
    f.setArguments(args);

    return f;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    CharSequence body = getArguments().getCharSequence("body");

    View v = inflater.inflate(R.layout.fragment_the_life_dialog, container, false);
    WebView webView = (WebView) v.findViewById(R.id.dialog_webview);
    webView.loadData(body.toString(), "text/html", "UTF-8");

    // Watch for button clicks.
//    Button button = (Button)v.findViewById(R.id.show);
//    button.setOnClickListener(new View.OnClickListener() {
//      public void onClick(View v) {
//      }
//    });

    return v;
  }
}