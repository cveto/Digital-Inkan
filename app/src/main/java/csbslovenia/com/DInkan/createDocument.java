package csbslovenia.com.DInkan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;


public class createDocument extends Activity {
    private final String TAG = createDocument.class.getSimpleName();

    private String B64key;

    // Support APIS less than 19  (cant use StandarsCharset). How the hell is this only available with API19? Where are the priorities!?
    private final int API_VER= Build.VERSION.SDK_INT;
    //private static Charset CHARSET = StandardCharsets.UTF_8;
    private static String CHARSET = "UTF-8";

    // Signature Algorithm (must be RSA though)
    private String SIGNATURE_ALGORITHM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_document);

        /**Make EditText Scrollable**/
        EditText ETdocumentContent = (EditText) findViewById(R.id.ETdocumentContent);
        ETdocumentContent.setMovementMethod(new ScrollingMovementMethod());

        Intent previousIntent = getIntent();
        String whatDo = previousIntent.getExtras().getString("whatDo");

        switch (whatDo) {
            // Create and sign a document. (Buttons do all the work)
            case "createAndSign":
                break;
            // Scan the document, put it in the Edit text ant make EditText ineditable. Then buttons do all the work.
            case "signOnly":
                // Scan document and place contents to EditText, then make EditText uneditable.
                    // This way it is "Sign only".
                scanDocument();
                break;
            case "createOnly":
                // Change the buton "Sign" to "Create". That button doesnt ask to sign before creating a document
                Button BUTcreateOnly = (Button)findViewById(R.id.BUTcreateOnly);
                Button BUTsign = (Button)findViewById(R.id.BUTsign);
                BUTcreateOnly.setVisibility(View.VISIBLE);              // Show the Create button
                BUTsign.setVisibility(View.GONE);                       // Hide the Sign button
                break;
            // By default its create and sign, so it just displays the raw layout.
            default:
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_document, menu);
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

    // Starts ZXING to get Private key. RequestCode = 1
    public void scanPrivateKey(View view) {
        EditText editText = (EditText)findViewById(R.id.ETdocumentContent);

        if (!editText.getText().toString().equals("")) {
            // Start Zxing barcode scanner
            int requestCode = 1;
            ZxingIntent zxingIntent = new ZxingIntent(this);
            zxingIntent.setScanningMessage(getResources().getString(R.string.findPrivateKey));
            zxingIntent.setRequestCode(requestCode);
            zxingIntent.initiateScan(ZxingIntent.QR_CODE_TYPES);
        } else {
            customToast(getResources().getString(R.string.pleaseEnterText));
        }

    }

    // Starts ZXING to read Document. Request code = 2.
    private void scanDocument() {
        // Start Zxing barcode scanner
        int requestCode = 2;
        ZxingIntent zxingIntent = new ZxingIntent(this);
        zxingIntent.setScanningMessage(getResources().getString(R.string.findDocument));
        zxingIntent.setRequestCode(requestCode);
        zxingIntent.initiateScan(ZxingIntent.QR_CODE_TYPES);
    }

    // Reads the EditText and just creates the document. No BarcodeScanner, no signature.
    public void createUnsignedDoc(View view) {
        String documentContent = getETDocumentContent();
        if (!documentContent.equals("")) {
            String B64docUnsigned = null;
            try {
                B64docUnsigned = getB64EditTextContent();
            } catch (Exception e) {
                Log.e(TAG,"Because of UTF-8");
            }

            Intent intent = new Intent(this,displayBitmap.class);
            intent.putExtra("B64docUnsigned",B64docUnsigned);
            intent.putExtra("documentContent",documentContent);

            // These 2 must exist for no errors.
            intent.putExtra("B64signature","");

            startActivity(intent);
        } else {
            customToast(getResources().getString(R.string.pleaseEnterText));
        }


    }

    /** Zxing activity result **/
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        ZxingResult scanResult = ZxingIntent.parseActivityResult(requestCode, resultCode, intent);
        // if scan result successful and message at least 30 characters long. With BIG QR messages it sometimes reads some small number by accident.
        if (scanResult.getContents() != null && scanResult.getContents().length() > 30) {
            // Where did the intent come from?
            if (resultCode == RESULT_CANCELED) {
                // ??? Untested
                finish();
            } else if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    // From scanning the private key
                    case 1: {
                        // save ENCRYPTED private key to variable:
                        this.B64key = scanResult.getContents();
                        // Ask for password for the key
                        askForPassword();
                        Log.d(TAG,"onActivityResult. Request code 1. Success");
                        break;
                    }
                    case 2: {
                        // From scanning the document.
                        String STcontent = null;
                        try {
                            byte[] BYcontent = Base64.decode(scanResult.getContents(),Base64.NO_WRAP);
                            STcontent = new String(BYcontent,CHARSET);
                        } catch (Exception e) {
                            Log.e(TAG,"Unsuported encoding exception");
                            finish();
                        }

                        // Set text to Edittext and make it uneditable.
                        EditText ETdocumentContent = (EditText)findViewById(R.id.ETdocumentContent);
                        ETdocumentContent.setText(STcontent);
                        ETdocumentContent.setEnabled(false);
                        Log.d(TAG, "onActivityResult. Request code 2. Success");
                        break;
                    }
                }
            // Request code is something funny
            } else {
                Log.e(TAG, "It wastn RESULT_OK not RESULT_CANCELLED");
                customToast("No idea what the problem is");
            }
        // Do not accept the scanned QR code because it is too short to be comming from this app.
        } else if (scanResult.getContents() != null) {
            customToast(getResources().getString(R.string.invalidQRcode));
            Log.e(TAG,"Code too short to be comming from this app.");
        // If scanning was cancelled
        } else {
            finish();
        }
    }

    // The Popup (alert Dialog) which asks for password.
    public void askForPassword(){
        // Initialize a password type EditText
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Call AlertDialog
        passwordAlertDialog(input);
    }

    // Alert Dialog for Password.
    private void passwordAlertDialog(final EditText input) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.keyPasswordNeeded))
                .setMessage(getResources().getString(R.string.pleaseInsertPassword))
                .setView(input)
                .setPositiveButton(getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            // For Testing. Password pre-defined.
                            //input.setText("prasicaklatijenujnovsakozimo");

                            // try signing the document. If you failed, probably wrong password.
                            signDocument(input.getText().toString().toCharArray());
                        } catch (Exception e) {
                            customToast("Try again. wrong password");
                            askForPassword();             // infinite loop until password correct
                            Log.e(TAG, "stuck in askForPassword, alert Dialog");
                        }
                    }
                }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing, just get away.
                    }
                }).show();
    }

    // Accepts password and signes the document. Key and Document are Global variables.
        // call the displayBitmap class afterwards
    private void signDocument(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        // Decrypt the key
        CertificateExtractor certificateExtractor1 = new CertificateExtractor();
        RSAPrivateKey keyR = certificateExtractor1.decryptKeyFromQR(password, this.B64key);
        Log.d(TAG, "size of private key: " + keyR.getPrivateExponent().bitLength());

        // Based on key size, set the Signature algorithm. Because if I use RSA512, SHA512 cant work on it. Also, sometimes 2048 key is 2047 bits long
        int keyBitLenght = keyR.getPrivateExponent().bitLength();
        if (500 < keyBitLenght && keyBitLenght < 536) {
            this.SIGNATURE_ALGORITHM = "SHA256withRSA";
        } else {
            this.SIGNATURE_ALGORITHM = "SHA512withRSA";
        }
        Log.d(TAG,"Signature algorithm: " + this.SIGNATURE_ALGORITHM);


        /** Sign the document with that private key **/
        // What am I signing? Document.
        String STcontent = getETDocumentContent();

        // Put Document to byte.
        byte[] BYcontent = STcontent.getBytes(this.CHARSET);

        /** Signing procedure **/
        // Sign with what algorithm?
        Signature signature = Signature.getInstance(this.SIGNATURE_ALGORITHM);               // No such algorithm Exception

        // With what private key?
        signature.initSign(keyR);

        // What Document?
        signature.update(BYcontent);

        // sign and return signed bytes.
        byte[] BYsignedContents = signature.sign();

        /** Prepairing data for QR **/
        // Encode signature bytes to Base64
        String B64signature = Base64.encodeToString(BYsignedContents,Base64.NO_WRAP);

        // Encode unsigned document to Base64
        String B64docUnsigned = Base64.encodeToString(BYcontent,Base64.NO_WRAP);

        /** Create send the document and signature to the new intent **/
        Intent inCreateBitmap = new Intent(this, displayBitmap.class);
        inCreateBitmap.putExtra("B64docUnsigned",B64docUnsigned);
        inCreateBitmap.putExtra("B64signature",B64signature);
        inCreateBitmap.putExtra("documentContent",STcontent);
        startActivity(inCreateBitmap);
        finish();
    }

    // get String from ETdocumentContent.
    private String getETDocumentContent() {
        EditText ETcontent = (EditText) findViewById(R.id.ETdocumentContent);
        return  ETcontent.getText().toString();
    }

    // get B64 encoded String from ETdocumentContent in UTF-8.
    public String getB64EditTextContent() throws UnsupportedEncodingException{
        String STcontent = getETDocumentContent();
        byte[] BYcontent = STcontent.getBytes(CHARSET);
        return Base64.encodeToString(BYcontent,Base64.NO_WRAP);
    }

    // customToast
    public void customToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
   }
