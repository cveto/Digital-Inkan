package csbslovenia.com.DInkan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;


public class Main extends Activity {
    public Map<String,String> info = new HashMap<>();        // booleand doesnt work because its primitive


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // To play around with how the document will look like
        //startTestingIntent();
        //getCRL();
        //Intent intent = new Intent(this,verificationResult.class);
        //startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        setInfo();
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

    // Create A document without a signature.
    public void createDocumentOnly(View view) {
        Intent createDocActivity = new Intent(this,createDocument.class);
        String whatDo = "createOnly";
        createDocActivity.putExtra("whatDo",whatDo);
        startActivity(createDocActivity);
    }

    // Reads a document from QR and asks for signing it
    public void signDocumentOnly(View view) {
        Intent signDocActivity = new Intent(this,createDocument.class);
        String whatDo = "signOnly";
        signDocActivity.putExtra("whatDo",whatDo);
        startActivity(signDocActivity);
    }

    // Creates a document and at the same time askes you to sign it.
    public void createAndSignDocument(View view) {
        Intent createDocActivity = new Intent(this,createDocument.class);
        String whatDo = "createAndSign";
        createDocActivity.putExtra("whatDo",whatDo);
        startActivity(createDocActivity);
    }

    // Verifies the signature. Needs accesible document, signature or Public Key or Certificate.
    public void verifySignature(View view) {
        Intent intent = new Intent(this, verifySignature.class);
        startActivity(intent);
    }

    /**TESTING**/
    // Hashmap
    // Set Info:
    private void setInfo() {
        this.info.put("subjectDN","asfasf");
        this.info.put("subjectDN2","");

    }

    public void crazyIvan(View view) {
        String a = this.info.get("subjectDN");
        Log.d("a.normal",a);
        String b = this.info.get("subjectDN2");
        Log.d("b.empty",b);
        //String c = this.info.get("subjectDN3");
        //Log.d("c.uninitialized",c);


    }

    // For thesting the layout of the bitmap. (Shows the created bitmap with dummy data emidiately)
    private void startTestingIntent() {
        Intent inCreateBitmap = new Intent(this, displayBitmap.class);
        inCreateBitmap.putExtra("B64docUnsigned",getResources().getString(R.string.TEST));
        inCreateBitmap.putExtra("B64signature",getResources().getString(R.string.TEST));
        inCreateBitmap.putExtra("documentContent",getResources().getString(R.string.TEST));
        startActivity(inCreateBitmap);
        finish();
    }

    // Download CRL
    public void getCRL(View view) throws  IOException {
        // Force downloading on main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Where is the crl?
        // String stURL = "http://www.sigen-ca.si/crl/sigen-ca.crl";
        String stURL = "http://www.trustcenter.de/crl/v2/tcclass0.crl";
        URL url = new URL(stURL);

        // Open connection
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            // Get .crtFile
            //InputStream in = getAssets().open("CRL/" + "sigen-ca" + ".crl");
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509CRL crl = (X509CRL) cf.generateCRL(in);
            Log.d("All revoked: ", crl.getRevokedCertificates().toString());

            // Test what certificate? Serial number
            String STcertSN = "49";
            BigInteger certSN = new BigInteger(STcertSN,16);
            Log.d("Serial number in decimal: ", certSN.toString());

            // See if revoked
            X509CRLEntry isRevoked = crl.getRevokedCertificate(certSN);

            if (isRevoked != null) {
                Log.d("Revoking: ", isRevoked.toString());
            } else {
                Log.d("Revoing: ","Was not revoked");
            }

        } catch (Exception e) {
            Log.e("uri","Failed at everything");
        }
        finally {
                urlConnection.disconnect();
        }
    }

}
