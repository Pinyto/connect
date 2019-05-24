package de.pinyto.pinyto_connect

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import java.security.Signature


class PinytoKeyManager {
    private val pinytoN = BigInteger(
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
        "668991726977737508228377398684622376557220679678700827641")
    private val pinytoE = BigInteger("65537")
    private var N: BigInteger? = null
    private var e: BigInteger? = null
    private var d: BigInteger? = null

    fun keyExists(): Boolean {
        if (N == null || e == null || d == null) return false
        return N!! > BigInteger("2").pow(4095) &&
                e!! > BigInteger.ONE &&
                d!! > BigInteger("2").pow(4095)
    }

    fun getKeyHash(): String {
        var hashString = N.toString() + e.toString()
        val hasher = MessageDigest.getInstance("SHA-256")
        hashString = hasher.digest(hashString.toByteArray(StandardCharsets.UTF_8)).toHexString()
        return hashString.substring(0, 10)
    }

    fun getPublicKeyData(): JSONObject {
        val publicKeyData = JSONObject()
        publicKeyData.put("N", N.toString())
        publicKeyData.put("e", e.toString())
        return publicKeyData
    }

    fun generateNewKeys() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096)
        val kp = kpg.genKeyPair()
        val publicKey = kp.public as RSAPublicKey
        N = publicKey.modulus
        e = publicKey.publicExponent
        val privateKey = kp.private as RSAPrivateKey
        d = privateKey.privateExponent
        saveKeyToPrefs()
        Log.i("PinytoKeyManager", "saved key to prefs:\n${prefs.jsonKeyPair}")
        prefs.savedKeyIsRegistered = false
    }

    fun loadKeyFromPrefs() {
        val keyData = prefs.jsonKeyPair
        if (keyData.has("N") && keyData.has("e") && keyData.has("d")) {
            N = BigInteger(keyData.getString("N"))
            e = BigInteger(keyData.getString("e"))
            d = BigInteger(keyData.getString("d"))
        }
    }

    fun saveKeyToPrefs() {
        val jsonKeyPair = JSONObject()
        jsonKeyPair.put("N", N.toString())
        jsonKeyPair.put("e", e.toString())
        jsonKeyPair.put("d", d.toString())
        prefs.jsonKeyPair = jsonKeyPair
    }

    private fun decryptToken(encryptedToken: String): ByteArray {
        val privateKeySpec = RSAPrivateKeySpec(N, d)
        val factory = KeyFactory.getInstance("RSA")
        val privateKey = factory.generatePrivate(privateKeySpec) as RSAPrivateKey
        val cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(Base64.decode(encryptedToken.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP))
    }

    private fun encryptTokenWithPinytoKey(token: ByteArray): String {
        val factory = KeyFactory.getInstance("RSA")
        val cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding")
        val pinytoPublicKeySpec = RSAPublicKeySpec(pinytoN, pinytoE)
        val pinytoPublicKey = factory.generatePublic(pinytoPublicKeySpec) as RSAPublicKey
        cipher.init(Cipher.ENCRYPT_MODE, pinytoPublicKey)
        return String(Base64.encode(cipher.doFinal(token), Base64.NO_WRAP), StandardCharsets.UTF_8)
    }

    fun calculateToken(encryptedToken: String): String {
        val decryptedTokenByteArray = this.decryptToken(encryptedToken)
        return this.encryptTokenWithPinytoKey(decryptedTokenByteArray)
    }

    fun checkSignature(encryptedToken: String, signature: String): Boolean {
        val pinytoPublicKeySpec = RSAPublicKeySpec(pinytoN, pinytoE)
        val factory = KeyFactory.getInstance("RSA")
        val pinytoPublicKey = factory.generatePublic(pinytoPublicKeySpec) as RSAPublicKey
        for (s in java.security.Security.getAlgorithms("Signature")) println(s)
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(pinytoPublicKey)
        verifier.update(encryptedToken.toByteArray(charset("UTF-8")))
        return verifier.verify(Base64.decode(signature.toByteArray(charset("UTF-8")), Base64.NO_WRAP))
    }
}
