package csbslovenia.com.DInkan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

import csbslovenia.com.DInkan.R;

public class verificationResult extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_result);

        // Ge Results
        Intent intent = getIntent();
        HashMap<String, Integer> verifyResults = (HashMap<String, Integer>)intent.getSerializableExtra("results");
        HashMap<String, String> info = (HashMap<String, String>)intent.getSerializableExtra("info");
        String message = intent.getExtras().getString("message");

        // ImageViews
        ImageView IVsbtd = (ImageView)findViewById(R.id.IVsbtd);
        ImageView IVsbtpk = (ImageView)findViewById(R.id.IVsbtpk);
        ImageView IVpkc = (ImageView)findViewById(R.id.IVpkc);
        ImageView IVcit = (ImageView)findViewById(R.id.IVcit);
        ImageView IVcinr = (ImageView)findViewById(R.id.IVcinr);
        ImageView IVdt = (ImageView)findViewById(R.id.IVdt);

        TextView TVmessage = (TextView)findViewById(R.id.TVmessage);

        // set icon to an appropriate...icon?
        TVmessage.setText(message);

        setIcon(IVsbtd,verifyResults.get("sbtd"));
        setIcon(IVsbtpk,verifyResults.get("sbtpk"));
        setIcon(IVpkc,verifyResults.get("pkc"));
        setIcon(IVcit,verifyResults.get("cit"));
        setIcon(IVcinr,verifyResults.get("cinr"));
        setIcon(IVdt,verifyResults.get("dt"));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.verification_result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setIcon(ImageView imageView, Integer integer) {
        switch (integer) {
            case 0: {
                imageView.setImageResource(R.drawable.alert50);
                break;
            }
            case 1: {
                //setInfo(imageView.toString());
                imageView.setImageResource(R.drawable.yes50);
                break;
            }
            case 2: {
                imageView.setImageResource(R.drawable.fail50);
                break;
            }
        }
    }


}
