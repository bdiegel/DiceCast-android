package com.honu.dicecast;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class InfoActivity extends ListActivity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      List<Pair<String, String>> data;
      data = new ArrayList();
      data.add(new Pair("Version", BuildConfig.VERSION_NAME));
      data.add(new Pair("License", "BSD-3"));
      data.add(new Pair("Feedback", "Send feedback"));
      data.add(new Pair("Author", "Honu Apps"));

      ListAdapter adapter = new PairAdapter(this, data);
      this.setListAdapter(adapter);

      ListView view = this.getListView();
      view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
         public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
               case 0:
                  break;
               case 1:
                  showLicenseInfo();
                  break;
               case 2:
                  sendEmail();
                  break;
               case 3:
                  break;
            }
         }
      });
   }

   protected void showLicenseInfo() {
      AlertDialog.Builder alert = new AlertDialog.Builder(this);
      alert.setTitle("License information");

      TextView tv = new TextView(this);
      tv.setText(Html.fromHtml(getString(R.string.bsd3_license)));
      tv.setMovementMethod(new ScrollingMovementMethod());
      tv.setPadding(12, 0, 12, 0);
      alert.setView(tv);

      alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
         }
      });
      alert.show();
   }

   protected void sendEmail() {
      final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
      intent.setType("message/rfc822");
      intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.mail_feedback_email)});
      intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.mail_feedback_subject));
      intent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.mail_feedback_message));

      try {
         startActivity(Intent.createChooser(intent, getString(R.string.title_send_feedback)));
         finish();
      } catch (android.content.ActivityNotFoundException ex) {
         Toast.makeText(InfoActivity.this,
               "There is no email client installed.", Toast.LENGTH_SHORT).show();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      return super.onOptionsItemSelected(item);
   }

   class PairAdapter extends BaseAdapter {

      private Context context;
      private List<Pair<String, String>> data;

      public PairAdapter(Context context, List<Pair<String, String>> data) {
         this.context = context;
         this.data = data;
      }

      /**
       * How many items are in the data set represented by this Adapter.
       *
       * @return Count of items.
       */
      @Override
      public int getCount() {
         return data.size();
      }

      /**
       * Get the data item associated with the specified position in the data set.
       *
       * @param position Position of the item whose data we want within the adapter's
       *                 data set.
       * @return The data at the specified position.
       */
      @Override
      public Object getItem(int position) {
         return data.get(position);
      }

      /**
       * Get the row id associated with the specified position in the list.
       *
       * @param position The position of the item within the adapter's data set whose row id we want.
       * @return The id of the item at the specified position.
       */
      @Override
      public long getItemId(int position) {
         return position;
      }

      /**
       * Get a View that displays the data at the specified position in the data set. You can either
       * create a View manually or inflate it from an XML layout file. When the View is inflated, the
       * parent View (GridView, ListView...) will apply default layout parameters unless you use
       * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
       * to specify a root view and to prevent attachment to the root.
       *
       * @param position    The position of the item within the adapter's data set of the item whose view
       *                    we want.
       * @param convertView The old view to reuse, if possible. Note: You should check that this view
       *                    is non-null and of an appropriate type before using. If it is not possible to convert
       *                    this view to display the correct data, this method can create a new view.
       *                    Heterogeneous lists can specify their number of view types, so that this View is
       *                    always of the right type (see {@link #getViewTypeCount()} and
       *                    {@link #getItemViewType(int)}).
       * @param parent      The parent that this view will eventually be attached to
       * @return A View corresponding to the data at the specified position.
       */
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View rowView = inflater.inflate(R.layout.info_item, parent, false);
         TextView titleView = (TextView) rowView.findViewById(R.id.title_view);
         TextView valueView = (TextView) rowView.findViewById(R.id.value_view);

         Pair<String, String> p = data.get(position);
         titleView.setText(p.first);
         valueView.setText(p.second);

         return rowView;
      }
   }

}
