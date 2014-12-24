package csbslovenia.com.DInkan;

import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Enumeration;

import javax.crypto.SecretKey;

/**
 * Created by Cveto on 21.8.2014.
 */
public class CertificateExtractor {
private final String TAG = CertificateExtractor.class.getSimpleName();          // Why is getClass(); so hard to implement. Google, really!
private static KeyStore myStore = null;


    // decrypts Private key in String form using AESCrypto. returns key in RSA format.
    public RSAPrivateKey decryptKeyFromQR(char[] password, String ciphertext) throws NoSuchAlgorithmException,InvalidKeySpecException {
        // Decrypt bytes
        byte[] keysDecrypted = AESCrypto.decryptPbkdf2toBytes(ciphertext,password);
        // Recreate Key
        RSAPrivateKey key = byteToKey(keysDecrypted);
        return key;
    }

    // accepts private key, extracts modulus and privateExponent, retruns those 2 in byte form.
    private byte[] keysToByte(RSAPrivateKey pk) {
        // get byte for Private key modulus and Private Exponent
        byte[] BYpkMod = pk.getModulus().toByteArray();
        byte[] BYpkPe = pk.getPrivateExponent().toByteArray();

        // Get their sizes in byte form
        byte[] BYpkModSize = intToByte(BYpkMod.length);
        byte[] BYpkPeSize = intToByte(BYpkPe.length);

        // concatenates those 4 together. ModSize - Mod - PeSize - Pe.
        byte[] BYconcencated = new byte[4+BYpkMod.length+4+BYpkPe.length];      // size of an int is 4 bytes (32 bits)
        ByteBuffer target = ByteBuffer.wrap(BYconcencated);
        target.put(BYpkModSize);
        target.put(BYpkMod);
        target.put(BYpkPeSize);
        target.put(BYpkPe);

        return BYconcencated;
    }

    // accepts Private key in byte form (Digi-Kami private key), and returns the Recreated (reduced sized) key.
        // comes from QR private key
    private RSAPrivateKey byteToKey(byte[] BYsource) throws NoSuchAlgorithmException,InvalidKeySpecException{
        int intSize = 4;

        // Read 4 bytes - get length of Mod:
        byte[] BYmodLenght = new byte[intSize];
        System.arraycopy(BYsource, 0, BYmodLenght, 0, intSize);
        Integer modLenght = byteToInt(BYmodLenght);

        // Get Modulus
        byte[] BYmod = new byte[modLenght];
        System.arraycopy(BYsource,intSize,BYmod,0,modLenght);     //start from 4 to mod length
        BigInteger mod = new BigInteger(BYmod);

        // Get Private Exponent Length
        byte[] BYpeLenght = new byte[intSize];
        System.arraycopy(BYsource, intSize+modLenght, BYpeLenght, 0, intSize);
        Integer peLenght = byteToInt(BYpeLenght);// ERROR?

        // Get Private Exponent
        byte[] BYpe = new byte[peLenght];
        System.arraycopy(BYsource,intSize+modLenght+intSize,BYpe,0,peLenght);     //start from 4 to mod length
        BigInteger pe = new BigInteger(BYpe);

        // Recreate the private Key
        RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(mod,pe);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKeyR = (RSAPrivateKey) factory.generatePrivate(privateSpec);

        return privateKeyR;
    }

    // accepts PublicKey in byte form, returns (Digi-Kami byte form). returns the Recreated (reduce sized) public key.
        // comes from QR public key
    public RSAPublicKey byteToPublicKey(byte[] BYsource) throws NoSuchAlgorithmException,InvalidKeySpecException{
        int intSize = 4;

        // get Modulus Length (4)
        byte[] BYmodLenght = new byte[intSize];
        System.arraycopy(BYsource, 0, BYmodLenght, 0, intSize);         // Copies number in first 4 bytes to integer.
        Integer modLenght = byteToInt(BYmodLenght);
        //Log.e(TAG,"modLength " + Integer.toString(modLenght));

        // Get Modulus
        byte[] BYmod = new byte[modLenght];
        System.arraycopy(BYsource,intSize,BYmod,0,modLenght);           // Copies number written in position between intSize to modLength
        BigInteger mod = new BigInteger(BYmod);
        //Log.e(TAG,"mod " + mod.toString());

        // Get Public Exponent Length
        byte[] BYpeLenght = new byte[intSize];
        System.arraycopy(BYsource, intSize+modLenght, BYpeLenght, 0, intSize);
        Integer peLenght = byteToInt(BYpeLenght);
        // Log.e(TAG,"peLength " + Integer.toString(peLenght));

        // Get Public Exponent
        byte[] BYpe = new byte[peLenght];
        System.arraycopy(BYsource,intSize+modLenght+intSize,BYpe,0,peLenght);     //start from 4 to mod length
        BigInteger pe = new BigInteger(BYpe);
        // Log.e(TAG,"private Exponent " + pe.toString());

        // Recreate public key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod,pe);
        RSAPublicKey publicKeyRecreated = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);


        return publicKeyRecreated;
    }

    // Checks whether the byte that we have is actually public or private key? (first 4 bytes length, then key, then length....)
        // This was necessary, becasue when a person scans a signature, there is no control of what he scans. It can not be automatically put to byteToPublic key, since out of meomory.
    public boolean verifyFirstModLenght(byte[] BYsource) {
        int intSize = 4;

        // Key length is max 4096 long.
        int maxModLength = 2048*2/8+1;
        try {
            // get Modulus Length (4)
            byte[] BYmodLenght = new byte[intSize];
            System.arraycopy(BYsource, 0, BYmodLenght, 0, intSize);         // Copies number in first 4 bytes to integer.
            Integer modLenght = byteToInt(BYmodLenght);
            Log.d(TAG,"ModLength: " + Integer.toString(modLenght));

            if (modLenght <= maxModLength) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Takes an Integer in byte form and returns the Integer.
    private Integer byteToInt(byte[] BYsource) {
        ByteBuffer wrapped = ByteBuffer.wrap(BYsource);
        Integer result = wrapped.getInt();
        return result;
    }

    // Takes an Integer in Integer form and returns its (4) bytes.
    private byte[] intToByte(Integer number) {
        // put size of it to byte array:
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);		// since integer has 4 bytes.
        byteBuffer.putInt(number);		// 257 probably. I am saving the number 257 to 4 bytes
        byte[] BYnumber = byteBuffer.array();
        return BYnumber;
    }

    /** Code from CertificateExtractor below. Not used in this app**/
    /* Unlock the Container and set the store. (Private key is still protected after this)*/
    public String setKeyStore(InputStream isCert, char[] certPassword) {
        try {
            KeyStore myStore = KeyStore.getInstance("PKCS12");
            myStore.load(isCert, certPassword);

            /**Find a certificate in the PKCS12 container (should be just one in Sigen-ca)**/
            Enumeration<String> eAliases = myStore.aliases();

            while (eAliases.hasMoreElements()) {
                String strAlias = eAliases.nextElement();
                Log.d(TAG, "Alias: " + strAlias);

                if (myStore.isKeyEntry(strAlias)) {
                    Log.d(TAG, "KeyStore is a key entry and was saved. " + strAlias);
                    this.myStore = myStore;
                    return strAlias;
                }
            }
        } catch(Exception e){
                Log.d(TAG, "Store unreachable");
        }
        return null;
    }
    private RSAPrivateKey getPrivateKey(String strAlias, char[] password) throws KeyStoreException,UnrecoverableKeyException,NoSuchAlgorithmException,InvalidKeySpecException{
        // get private exponent and modulus out of the Store,
        RSAPrivateCrtKey pk = (RSAPrivateCrtKey) this.myStore.getKey(strAlias, password);
        return pk;
    }
    public String getPemCert(String strAlias) throws KeyStoreException,CertificateEncodingException{

        // get Certificate from container
        X509Certificate cert =(X509Certificate) this.myStore.getCertificate(strAlias);

        // Transformt certificate to PEM
        String pemCert = Base64.encodeToString(cert.getEncoded(),Base64.NO_WRAP);
        String cert_begin = "-----BEGIN CERTIFICATE-----";
        String end_cert = "-----END CERTIFICATE-----";

        // Add Linebreaks to make a valid PEM (just header is enough)
        //pemCert = cert_begin + "\\n" + pemCert.replaceAll("(.{64})", "$1\\\\n") + "\\n" + end_cert;           //Line breaks everywhere
        //pemCert = cert_begin + "\n" + pemCert.replaceAll("(.{64})", "$1\n") + "\n" + end_cert;           //Real line breaks
        pemCert = cert_begin + "\\n" + pemCert + "\\n" + end_cert;                                            //Line breaks after Header and Footer only

        return pemCert;
    }
    public String getB64EncryptedPrivateKeyFromKeystore (char[] newPassword, char[] p12Password, String strAlias) throws KeyStoreException,UnrecoverableKeyException,NoSuchAlgorithmException,InvalidKeySpecException
    {

        // Get the key from PKCS12
        RSAPrivateKey key = getPrivateKey(strAlias,p12Password);

        // Turn the Modulus and Private Exponent from key to bytes
        byte[] byteKeys = keysToByte(key);

        // generate salt for Encryption
        final byte[] salt = AESCrypto.generateSalt();

        // create key from password and salt. Name it DK for Derived Key.
        SecretKey DK = AESCrypto.deriveKeyPbkdf2(salt,newPassword);

        // Encrypt using byte, key and salt. You shall receive ||| salt ] IV ] ciphertext ||||in BASE_64_NoWrap readable form.
        String st_cipherText = AESCrypto.encryptBytes(byteKeys,DK,salt);

        /** Concatenate title to the ciphertext**/
        // st_chiperText = st_chiperText.concat("]"+Crypto.toBase64(st_title.getBytes(Charset.forName("UTF-8"))));

        return st_cipherText;
    }
    // Unlock Certificate
    public void work(InputStream isCert,char[] certPassword) {
        try {

            KeyStore myStore = KeyStore.getInstance("PKCS12");
            myStore.load(isCert, certPassword);
            /**Find a certificate in the PKCS12 container (should be just one in Sigen-ca)**/
            Enumeration<String> eAliases = myStore.aliases();

            while (eAliases.hasMoreElements()) {
                String strAlias =  eAliases.nextElement();
                Log.d(TAG, "Alias: " + strAlias);

                if (myStore.isKeyEntry(strAlias)) {
                    Log.d(TAG,"this alias is key entry: " + strAlias);

                    /**Private Key**/
                    // get private exponent and modulus out of the X509 certificate. :
                    RSAPrivateCrtKey pk = (RSAPrivateCrtKey) myStore.getKey(strAlias, "prasicaklatijenujnovsakozimo".toCharArray());
                    Log.d(TAG,"Private key modulus: "+pk.getModulus().toString(36));
                    Log.d(TAG,"Private exponent: "+pk.getPrivateExponent().toString(36));

                    // Simulate putting private key to and from Base64 (saving to QR code)
                    String STpkModulus = Base64.encodeToString(pk.getModulus().toByteArray(), Base64.NO_WRAP);
                    BigInteger BIpkModulus = new BigInteger(Base64.decode(STpkModulus,Base64.NO_WRAP));

                    // Recreate the key (getting from QR code)
                    RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(BIpkModulus,pk.getPrivateExponent());
                    KeyFactory keyfactory = KeyFactory.getInstance("RSA");
                    RSAPrivateKey pkR = (RSAPrivateKey) keyfactory.generatePrivate(privateSpec);
                    Log.d(TAG,"Private key modulus: "+pkR.getModulus().toString(36));
                    Log.d(TAG,"Private exponent: "+pkR.getPrivateExponent().toString(36));

                    /**Public certificate**/
                    // get Certificate from container
                    X509Certificate cert =(X509Certificate) myStore.getCertificate(strAlias);

                    // Transformt certificate to PEM
                    String pemCert = Base64.encodeToString(cert.getEncoded(),Base64.NO_WRAP);
                    String cert_begin = "-----BEGIN CERTIFICATE-----";
                    String end_cert = "-----END CERTIFICATE-----";

                    // Linebreaks to make a valid PEM (just header is enough)
                    //pemCert = cert_begin + "\\n" + pemCert.replaceAll("(.{64})", "$1\\\\n") + "\\n" + end_cert;           //Line breaks everywhere
                    pemCert = cert_begin + "\n" + pemCert.replaceAll("(.{64})", "$1\n") + "\n" + end_cert;           //Real line breaks
                    //pemCert = cert_begin + "\\n" + pemCert + "\\n" + end_cert;                                            //Line breaks after Header and Footer only
                    Log.d(TAG,"X509 - Certificate \n"+pemCert);




                }
            }
            Log.d(TAG, "JE RATAL :)--------------------------------------------------------");
        } catch (Exception e) {
            Log.d(TAG, "NI RATAL :(--------------------------------------------------------");
        }
    }
    public String getX509Certificate(String b) {
        String a = "a";
        if (b.equals(a)) {
            return "you typed a";
        } else {
            return "kurac";
        }
    }
}
