package csbslovenia.com.DInkan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

public class verifySignature extends Activity {
    private final String TAG = verifySignature.class.getSimpleName();
    private String SIGNATURE_ALGORITHM;


    //private static Charset CHARSET = StandardCharsets.UTF_8;
    private static String CHARSET = "UTF-8";

    String B64doc;
    String B64signature;
    String B64certificate;

    // Colors for background
    final int GREEN = Color.argb(100,0,255,191);            // trueForRoot
    final int YELLOW = Color.argb(100,247,146,5);           // true
    final int RED = Color.argb(100,255,0,0);                // failed
    final int NOCOLOR = Color.argb(100,0,0,0);                // failed


    // Verification Result - failed by default
    String verified = "default";         // possible is true and trueForRoot

    public HashMap<String,String> info = new HashMap<>();        // booleand doesnt work because its primitive
    public HashMap<String,Integer> verifyResults = new HashMap<>();        // booleand doesnt work because its primitive



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_signature);

        // Set all values in MAP to false (nothing is verified yet).
        //resetVerificationResults();

        /**Make textviews Scrollable**/
        TextView TVdocument = (TextView)findViewById(R.id.TVdocument);
        TVdocument.setMovementMethod(new ScrollingMovementMethod());

        // Scan and display the document (raw)
        scanDocument();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.verify_signature, menu);
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

    // Reset Certificate info:
    private void resetInfo() {

        // 0 Not checked, 1 Okay, 2, Failed
        this.verifyResults.put("sbtd",0);           // Signature belongs to document
        this.verifyResults.put("sbtpk",0);          // Signatrue belongs to public key
        this.verifyResults.put("pkc",0);            // public key is Certified
        this.verifyResults.put("cit",0);            // certificate is trusted
        this.verifyResults.put("cinr",0);           // certificate is not revoked
        this.verifyResults.put("dt",0);             // document is timestamped
        this.verifyResults.put("dswcv",0);          // document signed while certificate valid
    }

    // Reset Certificate info:
    private void resetVerifyResults() {
        String a = "--info not available--";
        this.info.put("subjectDN",a);
        this.info.put("issuerDN",a);
        this.info.put("notBefore",a);
        this.info.put("notAfter",a);
        this.info.put("certRevoked",a);
    }

    //  information about Public certificate. This is only done when scanning with a certificate. And it should only be called when the root cert has confirmed it.
    private void setInfo(X509Certificate publicCertificate) {
        String subjectDN = substringBetween(publicCertificate.getSubjectDN().toString(),"CN=",",");
        String notBefore = publicCertificate.getNotBefore().toString();
        String notAfter = publicCertificate.getNotAfter().toString();

        this.info.put("subjectDN",subjectDN);
        this.info.put("notBefore",notBefore);
        this.info.put("notAfter",notAfter);
    }

    // informatio about ROOT certificate
    private void setRootInfo(X509Certificate rootCertificate ) {
        String rootDN = substringBetween(rootCertificate.getIssuerDN().toString(),"OU=",",");
        this.info.put("rootDN",rootDN);
    }

    // Start ZXing for scanning the document
    private void scanDocument() {
        int requestCode = 1;
        ZxingIntent zxingIntent = new ZxingIntent(this);
        zxingIntent.setScanningMessage(getResources().getString(R.string.scanDocumentContents));
        zxingIntent.setRequestCode(requestCode);
        zxingIntent.initiateScan(ZxingIntent.QR_CODE_TYPES);
    }

    // start Zxing for scanning the signature
    public void scanSignature(View view) {

        // Reset the verification
        resetVerifyResults();
        resetInfo();
        this.verified = "default";

        int requestCode = 2;
        ZxingIntent zxingIntent = new ZxingIntent(this);
        zxingIntent.setScanningMessage(getResources().getString(R.string.findSignature));
        zxingIntent.setRequestCode(requestCode);
        zxingIntent.initiateScan(ZxingIntent.QR_CODE_TYPES);
    }

    // start Zxing for scanning the signature
    public void scanCertificate() {
        int requestCode = 3;
        ZxingIntent zxingIntent = new ZxingIntent(this);
        zxingIntent.setScanningMessage(getResources().getString(R.string.scanCertificate));
        zxingIntent.setRequestCode(requestCode);
        zxingIntent.initiateScan(ZxingIntent.QR_CODE_TYPES);
    }

    /**Acitivity result**/
    // Handle result from ZXing
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        ZxingResult scanResult = ZxingIntent.parseActivityResult(requestCode, resultCode, intent);

        // if scan result successful and message at least 30 characters long. With BIG QR messages it sometimes reads some small number by accident.
        if (scanResult.getContents() != null && scanResult.getContents().length() > 2) {
            // Where did the intent come from?
            if (resultCode == RESULT_CANCELED) {
                // action cancelled
                Log.d(TAG,"Awww.. result cancelled :(");
            } else if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    // From scanning the private key
                    /** CODE FROM SCANNING THE DOCUMENT **/
                    case 1: {
                        this.B64doc = scanResult.getContents();

                        // decode data from document
                        byte[] BYdoc = Base64.decode(scanResult.getContents(),Base64.NO_WRAP);
                        String STdoc = "";
                        try {
                           STdoc = new String(BYdoc, CHARSET);
                        } catch (Exception e) {
                            Log.d(TAG,"Charset not supported.");
                        }

                        //show data
                        TextView textView = (TextView)findViewById(R.id.TVdocument);
                        textView.setText(STdoc);


                        Log.d(TAG, "onActivityResult. in case 1.");
                        break;
                    }
                    /** CODE FROM SCANNING THE SIGNATURE **/
                    case 2: {
                        // verify if user scanned the signature or not.
                            // Dont know how to do that :(
                            // if true, continue, if false, scan signature again

                        // save key
                        this.B64signature = scanResult.getContents();

                        // find out the signature type (from size), and set ti to that
                        getSignatureSizeAndSetSignatureAlgorithm(scanResult.getContents());


                        // Scan certificate or public key.
                        scanCertificate();
                        Log.d(TAG, "onActivityResult. in case 2");
                        break;
                    }
                    /** CODE FROM SCANNING THE CERTIFICATE**/
                    case 3: {
                        this.verified="failed";     // Just in case.
                        /**Certificate was scanned?**/
                        if (verifyIfQRCertificate(scanResult.getContents())) {
                            // save key
                            this.B64certificate = scanResult.getContents();
                            // verify the signature
                            try {
                                // Check if Signature - User Certificate and Cocument match
                                boolean verified = verifySignCertToDoc(B64doc, B64signature, B64certificate);

                                // Checks if certificate is verified (agains root certificate)
                                boolean verifiedCert = checkUserCertAgainstRootCertFromAssets(B64certificate);

                                // download CRL.
                                // verify CRL signature against root.
                                // verify for revoked.
                                // if cant download, verify from already downloaded crl.


                                // Verify Certificate againts certificates in our store:
                                if (verified && verifiedCert) {
                                    this.verified = "trueForRoot";
                                } else if (verified) {
                                    this.verified = "true";
                                } else {
                                    this.verified = "failed";
                                }

                                notifyAboutVerifying();

                            } catch (Exception e) {
                                Log.e(TAG, "Could not verify signature");
                                customToast(getResources().getString(R.string.canNotVerify));
                            }
                            break;

                         /**Public key was scanned?**/
                        // If it wasnt the certificate, verify if it was a Public key only (try extracting the public key)
                        // Pretty much takes anything
                        } else if (verifyIfQRPublicKey(scanResult.getContents())) {     //for now allways truze
                            try {
                                // verify using publicKey only
                                boolean verified = verifySignKeyToDoc(B64doc, B64signature, scanResult.getContents());

                                if (verified) {
                                    this.verified = "true";
                                }

                                notifyAboutVerifying();

                            } catch (Exception e) {
                                Log.e(TAG, "Could not verify signature");
                                customToast("could not verify with the public key");
                            }
                            break;

                        } else {
                            // Scan certificate again
                            //scanCertificate();

                            Button button = (Button)findViewById(R.id.BUTverify);
                            button.performClick();

                            //customToast(getResources().getString(R.string.notASignature));
                            customToast("Nothing worked");
                            Log.d(TAG,getResources().getString(R.string.notASignature));
                        }

                        Log.d(TAG, "onActiviryResult. in case 3");
                        break;
                    }
                    default: {
                        customToast("Awww, intent ID not known :/");
                    }
                }
            } else {
                customToast(getResources().getString(R.string.clueless));
            }
        } else if (scanResult.getContents() != null) {
            customToast(getResources().getString(R.string.invalidQRcode));
        } else {
            Log.d(TAG, "ZXing cancelled or no idea what the problem is");
            finish();
        }
    }

    // Get certificate from a string
    public static X509Certificate getMyCertificateFromString(String STcertificate) {

        InputStream fis = null;
        X509Certificate cert = null;					// Declaration
        ByteArrayInputStream bais = null;				// Declaration

        try {
            // Korensko sigenca
            fis = new ByteArrayInputStream(STcertificate.getBytes(CHARSET));
            byte value[] = new byte[fis.available()];		// get Bytes from fileInput stream (the certificate)
            fis.read(value);								// read the value of the bytes, put to InputStream.
            bais = new ByteArrayInputStream(value);			// make new bytArrayInput stream of this value?
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            cert = (X509Certificate)cf.generateCertificate(bais);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        finally {
            try {
                fis.close();
                bais.close();
            }
            catch(Exception ex) {
            }
        }
        return cert;
    }

    // verify if String is a Certificate or not.
    private boolean verifyIfQRCertificate(String QRdata) {
        // Check if it is Base64 encoded
        try {
            String STdata = QRdata.substring(0, 27);
            // Check if data contains "-----BEGIN CERTIFICATE-----"
            String testString = "-----BEGIN CERTIFICATE-----";
            if (STdata.equals(testString)) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,"Can not verify if it this was a public key or not, therefore false");
            return false;
        }
        return false;
    }

    // verify if QR is a public key or not. Always true for now.
    private boolean verifyIfQRPublicKey(String QRdata) {
        // Try extracting it, if it goes, OKAY.
        return true;
    }

    // Verify everything using key: Signature to Data, Certificate (or public key) to signature
    private Boolean verifySignKeyToDoc(String doc, String sign, String data) throws NoSuchAlgorithmException {
        // Decode document and certificate
        byte[] BYdoc = Base64.decode(doc,Base64.NO_WRAP);
        byte[] BYsign = Base64.decode(sign,Base64.NO_WRAP);
        Boolean signatureCheck = false;
        RSAPublicKey publicKey = null;
        X509Certificate x509cert;

            // Create the Java type RSA public key.
            CertificateExtractor certificateExtractor = new CertificateExtractor();
            Log.e(TAG,"Trying with public Key only (not a certificate)");

            byte[] BYkey;
            // Try recreating the key from Base64
            try {
                BYkey = Base64.decode(data, Base64.NO_WRAP);
            } catch (Exception e) {
                Log.e(TAG, "could not regenerate the..key?");
                return false;
            }

            // test if the recreated is not some garbage (By testing the first int number). To prevent out of memory error.
            if (!certificateExtractor.verifyFirstModLenght(BYkey)) {
               return false;
            }

            // Create RSAPublicKey from the scanned byte[]
            try {
                publicKey = certificateExtractor.byteToPublicKey(BYkey);
                Log.e(TAG,"key creatio sucsessful. NOWAY!");

            } catch (Exception e) {
                Log.e(TAG,"verifySignKeyToDoc - can not create public key from Base64. Only key was scanned");
                return false;                                       // return false if you couldn't even recreate the key
            }


        // Check signature
        Signature signature = Signature.getInstance(this.SIGNATURE_ALGORITHM);
        // Use What public key?
        try {
            signature.initVerify(publicKey);
        } catch (Exception e) {
            // If public key wasnt valid (wrong QR code)
            customToast(getResources().getString(R.string.notValidRSAkey));
            Log.e(TAG, getResources().getString(R.string.notValidRSAkey));
        }

        // Use What data raw (creates hash)?
        try {
            signature.update(BYdoc);
        } catch (Exception e) {
            Log.e(TAG,"Unable to put data in signatur.update");
        }

        // Check for validity
        try {
            signatureCheck = signature.verify(BYsign);
        } catch (Exception e) {
            Log.e(TAG,"signature exception. signature(verify)");
        }


        /**Checked, now what**/
        if (signatureCheck) {
            // Set verificationResult
            this.verifyResults.put("sbtd",1);           // Signature belongs to document
            this.verifyResults.put("sbtpk",1);          // Signatrue belongs to public key
            this.verifyResults.put("pkc",2);            // public key is Certified


            // Notify
            return true;
        }

        // Signature failed
        return false;
    }

    // Verify Signature using a whole certificate
    private Boolean verifySignCertToDoc(String doc, String sign, String cert) throws NoSuchAlgorithmException {
        // Decode document and certificate
        byte[] BYdoc = Base64.decode(doc,Base64.NO_WRAP);
        byte[] BYsign = Base64.decode(sign,Base64.NO_WRAP);
        Boolean signatureCheck = false;
        RSAPublicKey publicKey = null;
        X509Certificate x509cert = null;

        // Get the certificate in X509Certificate form
        try {
            x509cert = getMyCertificateFromString(cert);
            //publicKey = (RSAPublicKey) x509cert.getPublicKey();

            // Extract information from the certificate to present to the user.
            resetInfo();
            setInfo(x509cert);
            Log.d(TAG,x509cert.toString());
        } catch (Exception e) {
            Log.e(TAG,"could load certificate from the scanned QR code");
        }

        // Check signature
        Signature signature = Signature.getInstance(this.SIGNATURE_ALGORITHM);

        // Use What public key?
        try {
            signature.initVerify(x509cert);
        } catch (Exception e) {
            // If public key wasnt valid (wrong QR code)
            customToast(getResources().getString(R.string.notValidRSAkey));
            Log.e(TAG, getResources().getString(R.string.notValidRSAkey));

        }

        // Use What data raw (creates hash)?
        try {
            signature.update(BYdoc);
        } catch (Exception e) {
            Log.e(TAG,"Unable to put data in signature.update");
        }

        // Check for validity
        try {
            signatureCheck = signature.verify(BYsign);
        } catch (Exception e) {
            Log.e(TAG,"signature exception. signature(verify)");
        }

        /**Checked, now what**/
        if (signatureCheck) {
            // Set verificationResult
            this.verifyResults.put("sbtd",1);           // Signature belongs to document
            this.verifyResults.put("sbtpk",1);          // Signatrue belongs to public key
            this.verifyResults.put("pkc",1);            // public key is Certified

            //
            return true;
        }

        // Signature failed
        return false;
    }
    // check user signature against the truested root certificate in assets

    private boolean checkUserCertAgainstRootCertFromAssets(String STx509userCert) throws NoSuchAlgorithmException {
        X509Certificate x509userCert = getMyCertificateFromString(STx509userCert);

        // ICheck if this program supports the signature algorithm. This is stupid, have to remove it
        String signatureAlgorithm = x509userCert.getSigAlgName();
        if (    !signatureAlgorithm.equalsIgnoreCase("SHA1WithRSA") &&
                !signatureAlgorithm.equalsIgnoreCase("SHA256WithRSA") &&
                !signatureAlgorithm.equalsIgnoreCase("SHA512WithRSA")) {
            Log.d(TAG, "Unsuported signature algorithm: " + signatureAlgorithm);
            return false;
        }

        // Get root cert issuer name (root certificae must have the same name!!)
        String rootDN = substringBetween(x509userCert.getIssuerDN().toString(),"OU=",",");

        // Check if Trusted root exists in the store (in assets). Return fasle otherwise
        X509Certificate x509root = getX509certificateFromAssets(rootDN);
        if (x509root == null) {
            return false;
        }

        /**I have both certificates at this point**/
        try {
            x509userCert.verify(x509root.getPublicKey());
            Log.d(TAG, "Verification successful. Certificate is trusted!");
            // Information about root certificate
            setRootInfo(x509root);

            this.verifyResults.put("cit",1);            // certificate is trusted

            return true;

        } catch (Exception d) {
            this.verifyResults.put("cit",2);            // certificate is trusted
            Log.d(TAG,"certificate could not be checked to root");
        }

        return false;
    }

    // get X509 Certificate from Assets, provided the name of the file (without extentions!)
    private X509Certificate getX509certificateFromAssets(String fileName) {
        try {
            InputStream ISroot = getAssets().open("rootCertificates/" + fileName + ".crt");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509root = (X509Certificate) cf.generateCertificate(ISroot);
            Log.d(TAG, x509root.toString());
            return x509root;
        } catch (Exception e) {
            Log.e(TAG, "Cant load the root certificate from assets. Doesnt exist?");
        }
        return null;
    }

    // Get signature size and set signatrue algorithm. (To support multiple signature types)
    private void getSignatureSizeAndSetSignatureAlgorithm(String B64signature) {
        int size = Base64.decode(B64signature, Base64.NO_WRAP).length;        // size in bytes
        size = size * 8;           // size in bits

        // size of the signature is the same as size of the RSA key. It is kinda of my personal
        // decision, that only theese 2 will be used to maintain compatibility. I cant use SHA512 with 512 bit testing keys.
        if (500 < size && size < 524) {
            this.SIGNATURE_ALGORITHM = "SHA256withRSA";
        } else {
            this.SIGNATURE_ALGORITHM = "SHA512withRSA";
        }
        Log.d(TAG,"Signature algorithm: " + this.SIGNATURE_ALGORITHM);

    }

    // Handle verification  results. How to notify user?
    public void explain(View view) {
        notifyAboutVerifying();
    }

    private void notifyAboutVerifying() {
        int color;
        String verified = this.verified;        // Only for colour purposes. (main guideline on how much is it to be trusted)
        String message;

        switch (verified) {
            case "failed":
                message = getResources().getString(R.string.documentNotAuthentic);
                color = this.RED;
                break;

            case "true":
                message = getResources().getString(R.string.documentAuthentic);
                color = this.YELLOW;
                break;

            case "trueForRoot":
                message = getResources().getString(R.string.documentAuthenticForRoot);
                color = this.GREEN;
                break;

            default:
                message = "could not verify";
                color = this.NOCOLOR;
                break;
        }

        // Change background color
        changeBackgroundColor(color);

        // Rearange buttons
        changeButtons();

        // Display results:
        Intent intent = new Intent(this,verificationResult.class);
        intent.putExtra("info",this.info);
        intent.putExtra("results",this.verifyResults);
        intent.putExtra("message",message);
        startActivity(intent);

    }
    // Tell user if signature failed or nott
    private void notifyAboutVerifyingbackup() {
        int color;
        String verified = this.verified;        // Only for colour purposes. (main guideline on how much is it to be trusted)
        String message;
        String notAvailable = "-|-not available-|-\n";


        switch (verified) {
            case "failed":
                message = getResources().getString(R.string.documentNotAuthentic);
                color = this.RED;
                break;

            case "true":
                message = getResources().getString(R.string.documentAuthentic);
                message = message.concat("\n\n");
                message = message.concat("ROOT cert info: " + notAvailable);
                message = message.concat("Identity voucher: "  + notAvailable + "\n\n");

                message = message.concat("Certificate info: " + notAvailable);
                message = message.concat("Subject Name:: "  + notAvailable);
                message = message.concat("Cert not valid before: "  + notAvailable);
                message = message.concat("Cert not valid after: "  + notAvailable);

                message = message.concat("Revoking info updated?: " + notAvailable);
                message = message.concat("Certificate revoked?"  + notAvailable + "\n\n");

                message = message.concat("Timestamp voucher: " + notAvailable);
                message = message.concat("Document timestamped?: " + notAvailable);
                message = message.concat("Timestamp verified?: "+ notAvailable);


                color = this.YELLOW;
                break;
            case "trueForRoot":
                message = getResources().getString(R.string.documentAuthenticForRoot);
                message = message.concat("\n\n");
                message = message.concat("ROOT CERT INFO\n");
                message = message.concat("Root IS trusted by this app.\n");
                message = message.concat("Identity voucher: " + info.get("rootDN") + "\n\n");

                message = message.concat("CERTIFICATE INFO:\n");
                message = message.concat("Subject Name:: " + this.info.get("subjectDN") + "\n");
                message = message.concat("Cert not valid before: " + info.get("notBefore") + "\n");
                message = message.concat("Cert not valid after: " + info.get("notAfter") + "\n\n");

                message = message.concat("Revoking info updated?: " + notAvailable);
                message = message.concat("Certificate revoked?" + info.get("certRevoked") + "\n\n");

                message = message.concat("Timestamp voucher: " + notAvailable);
                message = message.concat("Document timestamped?: " + notAvailable);
                message = message.concat("Timestamp verified?: " + notAvailable);


                color = this.GREEN;
                break;
            default:
                message = "could not verify";
                color = this.NOCOLOR;
                break;
        }

        changeBackgroundColor(color);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Verification result:")

                //.setMessage(message)
                .setMessage(Html.fromHtml(
                                    "<u>" + message + "</u>" +
                                    "<i>" + message + "</i>" +
                                    "<h2>" + message + "</h2>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>" +
                                    "<u>" + message + "</u>"
                ))
                .setPositiveButton("I understand", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();

        // Rearange buttons
        changeButtons();
    }

    // Make EXPLAIN button visible after verification
    private void changeButtons() {
        Button button = (Button)findViewById(R.id.BUTexplain);
        button.setVisibility(View.VISIBLE);
    }

    // To make it clear how verification went. Green, Yellow, Red
    private void changeBackgroundColor(Integer color) {
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.LL_verifySignature);
        linearLayout.setBackgroundColor(color);
    }

    // Substring from - to certain string
    private String substringBetween(String text, String start, String stop) {
        // First check if there even is what we are searching for
        int INstart = 0;
        //if (text.indexOf(start) >= 0) {
        if (text.contains(start)) {
            INstart = text.indexOf(start);
            INstart = INstart + start.length();
        } else {
            // Returnt just everything
            return text;
        }

        // Cut everything before the text
        String textAfterStart = text.substring(INstart);

        // STOPing index
        // if user even gave a stopping index
        if (stop.equals("")) {
            return textAfterStart;
            // if found
        } else if (textAfterStart.indexOf(stop) > 0) {
            return textAfterStart.substring(0,textAfterStart.indexOf(stop));
        } else {
            return textAfterStart;
        }
    }

    // Custom toast
    public void customToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
