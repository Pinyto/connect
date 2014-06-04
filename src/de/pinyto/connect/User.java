package de.pinyto.connect;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.widget.EditText;

/**
 * This class fetches Userdata from the interfaces and creates data which is
 * necessary to connect to the Pinyto-Cloud
 * 
 * @author yonjuni
 */
public class User {

	EditText usernameField;
	EditText passphraseField;

	Activity activity;

	// Userdata
	private String username = "test"; // TODO make dynamic
	private String passphrase;
	private BigInteger keyN;
	private int keyE;

	public void setKeyE(int keyE) {
		this.keyE = keyE;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	public void setKeyN(String keyN) {
		this.keyN = new BigInteger(keyN);
	}

	public int getKeyE() {
		return this.keyE;
	}

	public BigInteger getKeyN() {
		return this.keyN;
	}
	
	public String getPassphrase() {
		return this.passphrase;
	}

	public User(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Fetches Userdata from the UI
	 * 
	 * @param activity
	 *            Is needed to find the EditText-Fields
	 */
	public void getUserData(Activity activity) {

		usernameField = (EditText) activity.findViewById(R.id.username);
		passphraseField = (EditText) activity.findViewById(R.id.passphrase);

		username = usernameField.getText().toString();
		passphrase = passphraseField.getText().toString();
	}

	/**
	 * @return The first 10 characters of the hashed Key which consists of N and
	 *         e
	 * @throws NoSuchAlgorithmException
	 *             if SHA-256 could not be used as algorithm
	 * @throws UnsupportedEncodingException
	 *             if hex-coding is not possible
	 */
	public String computeKeyHash() throws NoSuchAlgorithmException,
			UnsupportedEncodingException {

		// TODO Change to dynamic
		setKeyE(65537);
		setKeyN("4679143535854885578217558454097697814598683892095686067633985415348814571066447646431575084447883206915019035425893784140990048547841057816368834127953583033357507654776551407842825360008140095039774998728029492079767692134634272829213443705659287148078631526117308688597598038571140879665475213852905523091112240745733861468921032433638635003806356640264454020681597877487474788798636035752292336580902340661515760743247235955056374185729884148376087025459530601692802643521884642806918456612059838226480663640204831963694214933063297503551777146902628008721978583021738629811577692813732756210167193627658382822840274923706732029054818940736073594803788622609459008450928477820775548553997305777492517065781625292455497583781012987471402098403969663198230494698769996841883170413707286498230851772763572730326294399618077491199046288246121191878170710243580289179237269526080356103570355831616478620272623443312330292725329");

		String toHash = getKeyN().toString() + String.valueOf(getKeyE());

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(toHash.getBytes("UTF-8"));

		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);

		}

		String keyHash = hexString.substring(0, 10);
		return keyHash.toString();

	}

	/**
	 * @return Formdata-String in the form of username=test&keyhash=1234567890
	 * @throws NoSuchAlgorithmException
	 *             if SHA-256 could not be used as algorithm
	 * @throws UnsupportedEncodingException
	 *             if hex-coding is not possible
	 */
	public String createFormdata() throws NoSuchAlgorithmException,
			UnsupportedEncodingException {

		String keyHash = computeKeyHash();
		String formData = "username=" + username.toString() + "&keyhash="
				+ keyHash;
		return formData;
	}

}
