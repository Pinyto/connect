package de.pinyto.pinyto_connect;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * This class is used for managing keys and checking signatures.
 */
public class PinytoKeyManager {

    private static BigInteger PinytoN = new BigInteger(
            "782948781509853654016334088210162471626369912428477672733185640040201260506861760707" +
            "484802078092926328617773717950663206670942737298085014960670217557853253853192700321" +
            "555558603804036942730397818321662225740982623746713760050717491720444401365859377904" +
            "761509456912858624197383009910897359227163603168055069076298865038863481087702189737" +
            "004361831922401585208039171510900918020127562589611951384082770334531916254649564243" +
            "524834657266481495991546392221003565934006405838982929020427772252736674943250599862" +
            "681955141816261927004696811927915492285857001606967204165474930842200356404798932927" +
            "527349880460038664735604799615657535149297885974095729490474619110669121168730180817" +
            "692328496414219811007307954592150474836720748698738166425030721793649392443450827444" +
            "171715705948513454152940437456340236620666276102597754780342639841679047815006824885" +
            "208959469459587070768606268964437819028000766288835526004124480298771987405845901023" +
            "717002381271218260225466965940603894413663686331831212819660906562579381443545129109" +
            "192486789718145604097939794136149363378482903748307464449991677788573516175081960241" +
            "354902716444405372190562711789562039944011689062773834284439722273182150964910834681" +
            "668991726977737508228377398684622376557220679678700827641");
    private static BigInteger PinytoE = new BigInteger("65537");

    private BigInteger N;
    private BigInteger e;
    private BigInteger d;

    public PinytoKeyManager(BigInteger N, BigInteger e, BigInteger d) {
        this.N = N;
        this.e = e;
        this.d = d;
    }

    public boolean keyExists() {
        return (N.compareTo(new BigInteger("2").pow(4095)) > 0) &&
                (e.compareTo(BigInteger.ONE) > 0) &&
                (d.compareTo(new BigInteger("2").pow(4095)) > 0);
    }

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();
    private static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String getKeyHash() {
        String hashString = N.toString() + e.toString();
        try {
            MessageDigest hasher = MessageDigest.getInstance("SHA-256");
            hashString = bytesToHexString(hasher.digest(hashString.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            Log.d("Key error", "SHA-256 is not implemented.");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            Log.d("Key error", "UTF-8 is not supported.");
            e.printStackTrace();
        }
        return hashString.substring(0, 10);
    }

    public void generateNewKeys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            KeyPair kp = kpg.genKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
            N = publicKey.getModulus();
            e = publicKey.getPublicExponent();
            RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
            d = privateKey.getPrivateExponent();
        } catch (NoSuchAlgorithmException e) {
            Log.d("Key error", "RSA is not implemented.");
            e.printStackTrace();
        }
    }

    public void saveKeysToSettings(SharedPreferences settings) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("N", N.toString());
        editor.putString("e", e.toString());
        editor.putString("d", d.toString());
        editor.apply();
    }

    public JSONObject getPublicKeyData() {
        JSONObject publicKeyData = new JSONObject();
        try {
            publicKeyData.put("N", N.toString());
            publicKeyData.put("e", e.toString());
        } catch (JSONException e) {
            Log.d("Key error", "Unable to costruct JSON key data.");
            e.printStackTrace();
        }
        return publicKeyData;
    }

    public byte[] decryptToken(String encryptedToken) {
        try {
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(N, d);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding");
            //Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            Log.d("encrypted_token", encryptedToken);
            Log.d("token length", Integer.toString(Base64.decode(encryptedToken.getBytes("UTF-8"), Base64.NO_WRAP).length));
            Log.d("token hex", Hextools.bytesToHex(Base64.decode(encryptedToken.getBytes("UTF-8"), Base64.NO_WRAP)));
            return cipher.doFinal(Base64.decode(encryptedToken.getBytes("UTF-8"), Base64.NO_WRAP));
        } catch (InvalidKeySpecException e) {
            Log.d("Token decryption error", "Key data is invalid. This is no RSA key.");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Log.d("Token decryption error", "RSA is not implemented.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d("Token decryption error", "Padding not supported.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d("Token decryption error", "The key is invalid.");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            Log.d("Token decryption error", "UTF-8 is not supported.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.d("Token decryption error", "The token has a invalid block size.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.d("Token decryption error", "Bad padding.");
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public String encryptTokenWithPinytoKey(byte[] token) {
        try {
            Log.d("decrypted", "successfully");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
            RSAPublicKeySpec pinytoPublicKeySpec = new RSAPublicKeySpec(PinytoN, PinytoE);
            RSAPublicKey pinytoPublicKey =
                    (RSAPublicKey) factory.generatePublic(pinytoPublicKeySpec);
            cipher.init(Cipher.ENCRYPT_MODE, pinytoPublicKey);
            return new String(Base64.encode(
                    cipher.doFinal(token),
                    Base64.NO_WRAP), "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            Log.d("Token encryption error", "RSA is not implemented.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d("Token encryption error", "Padding not supported.");
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.d("Token encryption error", "Key data is invalid. This is no RSA key.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d("Token encryption error", "The key is invalid.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.d("Token encryption error", "The token has a invalid block size.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.d("Token encryption error", "Bad padding.");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            Log.d("Token encryption error", "UTF-8 is not supported.");
            e.printStackTrace();
        }
        return "";
    }

    public String calculateToken(String encryptedToken) {
        byte[] decryptedTokenBytes = this.decryptToken(encryptedToken);
        Log.d("decrypted", "successfully");
        return this.encryptTokenWithPinytoKey(decryptedTokenBytes);
    }

    public boolean checkSignature(String encryptedToken, String signature) {
        try {
            RSAPublicKeySpec pinytoPublicKeySpec = new RSAPublicKeySpec(PinytoN, PinytoE);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey pinytoPublicKey =
                    (RSAPublicKey) factory.generatePublic(pinytoPublicKeySpec);
            Log.d("et", encryptedToken);
            Log.d("sig", signature);
            for( String s : java.security.Security.getAlgorithms("Signature") ) System.out.println(s);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            //PSSParameterSpec algSpec = PSSParameterSpec.DEFAULT;
            //verifier.setParameter((AlgorithmParameterSpec) algSpec);
            verifier.initVerify(pinytoPublicKey);
            verifier.update(encryptedToken.getBytes("UTF-8"));
            return verifier.verify(Base64.decode(signature.getBytes("UTF-8"), Base64.NO_WRAP));
        } catch (NoSuchAlgorithmException e) {
            Log.d("Signature error", "RSA is not implemented.");
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.d("Signature error", "Key data is invalid. This is no RSA key.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d("Signature error", "The Pinyto key is invalid.");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            Log.d("Signature error", "UTF-8 is not supported.");
            e.printStackTrace();
        } catch (SignatureException e) {
            Log.d("Signature error", "Bad signature.");
            e.printStackTrace();
        //} catch (InvalidAlgorithmParameterException e) {
        //    Log.d("Signature error", "Wrong padding parameters.");
        //    e.printStackTrace();
        }
        return false;
    }

    /*public KeyPair loadKey() {
        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(N, e);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey pub = (RSAPublicKey) factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            Log.d("Key error", "RSA is not implemented.");
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.d("Key error", "Key data is invalid. This is no RSA key.");
            e.printStackTrace();
        }
    }*/
}
