package edu.uiuc.ncsa.security.oauth_2_0;

import edu.uiuc.ncsa.security.core.exceptions.GeneralException;
import edu.uiuc.ncsa.security.core.util.DebugUtil;
import edu.uiuc.ncsa.security.util.jwk.JSONWebKey;
import edu.uiuc.ncsa.security.util.jwk.JSONWebKeyUtil;
import edu.uiuc.ncsa.security.util.jwk.JSONWebKeys;
import edu.uiuc.ncsa.security.util.pkcs.KeyUtil;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Creates JWT tokens. This is for <b>unsigned</b> and <b>/b>unverified</b> tokens which
 * can be sent over a secure connection. The format is to have a header that describes the
 * content, including algorithm (fixed at "none" here) and a payload of claims. Both of these
 * are in JSON. The token then consists of based64 encoding both of these and <br/><br>
 * encoded header + "."   + encoded payload + "."<br/><br>
 * The last period is manadatory and must end this. Normally if there is signing or verification
 * used, this last field would contain information pertaining to that. It must be omitted because we
 * do neither of these.
 * <p>Created by Jeff Gaynor<br>
 * on 2/9/15 at  10:45 AM
 */

// Fixes OAUTH-164, adding id_token support.
public class IDTokenUtil {
    public static String TYPE = "typ";
    public static String KEY_ID = "kid";
    public static String ALGORITHM = "alg";

    /**
     * Creates an unsigned token.
     *
     * @param payload
     * @return
     */
    public static String createIDToken(JSONObject payload) {
        JSONObject header = new JSONObject();
        header.put(TYPE, "JWT");
        header.put(ALGORITHM, NONE_JWT);
        return concat(header, payload) + "."; // as per spec.
    }

    public static String createIDToken(JSONObject payload, JSONWebKey jsonWebKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeySpecException, InvalidKeyException, IOException {
        JSONObject header = new JSONObject();
        header.put(TYPE, "JWT");
        header.put(KEY_ID, jsonWebKey.id);
        String signature = null;

        header.put(ALGORITHM, jsonWebKey.algorithm);

        if (jsonWebKey.algorithm.equals(NONE_JWT)) {
            signature = ""; // as per spec

        } else {
            DebugUtil.dbg(IDTokenUtil.class, "Signing ID token with algorithm = " + jsonWebKey.algorithm);
            signature = sign(header, payload, jsonWebKey);
        }
        String x = concat(header, payload);
        return x + "." + signature;

    }


    protected static String concat(JSONObject header, JSONObject payload) {

        return Base64.encodeBase64URLSafeString(header.toString().getBytes()) + "." +
                Base64.encodeBase64URLSafeString(payload.toString().getBytes());

/*
        return Base64.encodeBase64String(header.toString().getBytes()) + "." +
                Base64.encodeBase64String(payload.toString().getBytes());
*/

    }

    public static final String NONE_JWT = "none";
    public static final int NONE_KEY = 100;


    public static final String RS256_JWT = "RS256";
    public static final String RS256_JAVA = "SHA256withRSA";
    public static final int RS256_KEY = 101;

    public static final String RS384_JWT = "RS384";
    public static final String RS384_JAVA = "SHA384withRSA";
    public static final int RS384_KEY = 102;

    public static final String RS512_JWT = "RS512";
    public static final String RS512_JAVA = "SHA512withRSA";
    public static final int RS512_KEY = 103;

    protected static String sign(JSONObject header,
                                 JSONObject payload,
                                 JSONWebKey webkey) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        return sign(concat(header, payload), webkey);
    }

    protected static String sign(String x, JSONWebKey webkey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException {
       /*
       JWT alg name             Java name
                                MD2withRSA
                                MD5withRSA
       RS256                    SHA256withRSA
       RS348                    SHA384withRSA
       RS512                    SHA512withRSA
        */

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(webkey.privateKey.getEncoded());
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        Signature signature = Signature.getInstance(getJavaSignatureName(webkey.algorithm));
        signature.initSign(privateKey);
        byte[] content = x.getBytes();
        signature.update(content);
        byte[] signatureBytes = signature.sign();
        return Base64.encodeBase64URLSafeString(signatureBytes);

    }

    protected static String getJavaSignatureName(String algorithm) {
        if (algorithm.equals(NONE_JWT)) {
            return NONE_JWT;
        }
        if (algorithm.equals(RS256_JWT)) {
            return RS256_JAVA;
        }
        if (algorithm.equals(RS384_JWT)) {
            return RS384_JAVA;
        }
        if (algorithm.equals(RS512_JWT)) {
            return RS512_JAVA;
        }
        throw new IllegalArgumentException("Error: unknow algorithm \"" + algorithm + "\"");

    }

    public static boolean verify(JSONObject header, JSONObject payload, String sig, JSONWebKey webKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {

        Object alg = header.get(ALGORITHM);
        if (alg == null || !(alg instanceof String)) {
            throw new IllegalStateException("Unknown algorithm");
        }
        String algorithm = (String) alg;
        DebugUtil.dbg(IDTokenUtil.class, "Verifying ID token with algorithm = " + algorithm);
        Signature signature = null;
        if (algorithm.equals(NONE_JWT)) {
            return true;
        }
        signature = Signature.getInstance(getJavaSignatureName(algorithm));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(webKey.publicKey.getEncoded());
        RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

        signature.initVerify(pubKey);
        signature.update(concat(header, payload).getBytes());
        boolean rc = signature.verify(Base64.decodeBase64(sig));
        DebugUtil.dbg(IDTokenUtil.class, "Verification ok? " + rc);
        return rc;
    }

    /**
     * This returns header, payload and signature as the three elements of an array.
     *
     * @param idToken
     * @return
     */
    protected static String[] decat(String idToken) {
        int firstPeriod = idToken.indexOf(".");
        int lastPeriod = idToken.lastIndexOf(".");
        String header = idToken.substring(0, firstPeriod);
        String payload = idToken.substring(firstPeriod + 1, lastPeriod);
        String signature = idToken.substring(lastPeriod + 1);
        return new String[]{header, payload, signature};
    }


    /**
     * This reads and optionally verifies the ID token, using the provides JSON
     * webKeys. In case the webKeys is null, no verification is tried.
     * @param idToken
     * @param webKeys
     * @return payload (claims)
     */
    public static JSONObject verifyAndReadIDToken(String idToken, JSONWebKeys webKeys) {
        String[] x = decat(idToken);
        JSONObject h = JSONObject.fromObject(new String(Base64.decodeBase64(x[0])));
        JSONObject p = JSONObject.fromObject(new String(Base64.decodeBase64(x[1])));
        DebugUtil.dbg(IDTokenUtil.class, "header = " + h);
        DebugUtil.dbg(IDTokenUtil.class, "payload = " + p);
        if (h.get(ALGORITHM) == null) {
            throw new IllegalArgumentException("Error: no algorithm.");
        }

	if (h.get(ALGORITHM).equals(NONE_JWT)) {
	    DebugUtil.dbg(IDTokenUtil.class, "Unsigned id token. Returning payload");

	    return p;
	}

        if (!h.get(TYPE).equals("JWT")) {
	    throw new GeneralException("Unsupported token type.");
	}
        Object keyID = h.get(KEY_ID);
        DebugUtil.dbg(IDTokenUtil.class, "key_id = " + keyID);

        if (keyID == null || !(keyID instanceof String)) {
            throw new IllegalArgumentException("Error: Unknown algorithm");
        }

	if (webKeys == null)  {
            DebugUtil.dbg(IDTokenUtil.class, "No webKeys available, skipping verification");

	    return p;
	}

	boolean isOK = false;
	try {
	    isOK = verify(h, p, x[2], webKeys.get(h.getString(KEY_ID)));
	} catch (Throwable t) {
	    throw new IllegalStateException("Error: could not verify signature", t);
	}
	if (!isOK) {
	    throw new IllegalStateException("Error: could not verify signature");
	}

        return p;
    }


    // Strictly for testing.
    public static void main(String[] args) {
        try {
            //firstTest();
            //firstTestB();
            //  otherTest();
            //testSigning();
            //  testSigningDirectly();
            //testJWT_IO();
            //printKeys();
            generateAndSign();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void otherTest() throws Exception {
        JSONWebKeys keys = JSONWebKeyUtil.fromJSON(new File("/home/ncsa/dev/csd/config/keys.jwk"));
        JSONObject claims = verifyAndReadIDToken(ID_TOKKEN, keys);
        System.out.println("claims=" + claims);

    }

    public static void testSigning() throws Exception {
        String h = "{\"typ\":\"JWT\",\"kid\":\"9k0HPG3moXENne\",\"alg\":\"RS256\"}";
        String p = "{\"iss\":\"https://ashigaru.ncsa.uiuc.edu:9443\",\"sub\":\"jgaynor\",\"exp\":1484764744,\"aud\":\"myproxy:oa4mp,2012:/client_id/14649e2f468450dac0c1834811dbd4c7\",\"iat\":1484763844,\"nonce\":\"0ZIi-EuxeC_X8AgB3VifOoqKiXWsz_NlXSzIu7h8rzU\",\"auth_time\":\"1484763843\"}\n";
        JSONObject header = JSONObject.fromObject(h);
        System.out.println("header=" + header);
        JSONObject payload = JSONObject.fromObject(p);
        System.out.println("payload=" + payload);
        System.out.println("base 64=" + concat(header, payload));
        String keyID = "9k0HPG3moXENne";
        JSONWebKeys keys = JSONWebKeyUtil.fromJSON(new File("/home/ncsa/dev/csd/config/keys.jwk"));

        String idTokken = createIDToken(payload, keys.get(keyID));
        System.out.println(idTokken);
        JSONObject claims = verifyAndReadIDToken(idTokken, keys);
        System.out.println("claims = " + claims);
    }

    public static void firstTest() throws Exception {
        JSONObject header = new JSONObject();
        header.put(TYPE, "JWT");
        header.put(ALGORITHM, "RS256");
        KeyPair keyPair = KeyUtil.generateKeyPair();
        JSONWebKey webKey = new JSONWebKey();
        webKey.algorithm = "RS256";
        webKey.privateKey = keyPair.getPrivate();
        webKey.publicKey = keyPair.getPublic();
        webKey.id = "qwert";
        webKey.type = "sig";
        JSONObject payload = new JSONObject();
        payload.put("name", "jeff");
        payload.put("id", "sukjfhusdfsdjkfh");
        payload.put("other_claim", "skjdf93489ghiovs 98sd89wehi ws");
        payload.put("another_claim", "l;kfg8934789dfio9v 92w89 98wer");
        String tokken = createIDToken(payload, webKey);

        System.out.println("JWT=" + tokken);
        JSONWebKeys keys = new JSONWebKeys(null);
        keys.put(webKey.id, webKey);
        System.out.println("claims=" + verifyAndReadIDToken(tokken, keys));
        System.out.println("-----");
        // note that if the this last call
        // works it is because the verification works too.
    }

    public static void signAndVerify(JSONWebKeys keys, String keyID) throws Exception {
        String h = "{" +
                "  \"typ\": \"JWT\"," +
                "  \"kid\": \"9k0HPG3moXENne\"," +
                "  \"alg\": \"RS256\"" +
                "}";

        String p = "{\n" +
                "  \"iss\": \"https://ashigaru.ncsa.uiuc.edu:9443\"," +
                "  \"sub\": \"jgaynor\"," +
                "  \"exp\": 1484764744," +
                "  \"aud\": \"myproxy:oa4mp,2012:/client_id/14649e2f468450dac0c1834811dbd4c7\"," +
                "  \"iat\": 1484763844," +
                "  \"nonce\": \"0ZIi-EuxeC_X8AgB3VifOoqKiXWsz_NlXSzIu7h8rzU\"," +
                "  \"auth_time\": \"1484763843\"" +
                "}";
        JSONObject header = JSONObject.fromObject(h);
        JSONObject payload = JSONObject.fromObject(p);
        JSONWebKey key = keys.get(keyID);
        String signature = sign(header, payload, key);
        System.out.println(concat(header, payload) + "." + signature);
        System.out.println(KeyUtil.toX509PEM(key.publicKey));

        System.out.println("verified?" + verify(header, payload, signature, key));

    }

    public static void generateAndSign() throws Exception {
        String keyID = "aQEiCy2fJcVgkOft";
        KeyPair keyPair = KeyUtil.generateKeyPair();

        JSONWebKeys keys = new JSONWebKeys(keyID);
        JSONWebKey key = new JSONWebKey();
        key.privateKey = keyPair.getPrivate();
        key.publicKey = keyPair.getPublic();
        key.algorithm = RS256_JWT;
        key.id = keyID;
        key.use = "sig";
        key.type = "RSA";
        keys.put(key);
        System.out.println("Generating keys and signing.");
        signAndVerify(keys, keyID);

        JSONObject jsonKeys = JSONWebKeyUtil.toJSON(keys);
        JSONWebKeys keys2 = JSONWebKeyUtil.fromJSON(jsonKeys.toString(2));

        JSONWebKey webKey = keys2.get(keyID);
        System.out.println("Serializing, deserializing then signing.");

        signAndVerify(keys2, keyID);

    }


    public static void printKeys() throws Exception {
        String text = "eyJ0eXAiOiJKV1QiLCJraWQiOiI5azBIUEczbW9YRU5uZSIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJodHRwczovL2FzaGlnYXJ1Lm5jc2EudWl1Yy5lZHU6OTQ0MyIsInN1YiI6ImpnYXlub3IiLCJleHAiOjE0ODQ3NjQ3NDQsImF1ZCI6Im15cHJveHk6b2E0bXAsMjAxMjovY2xpZW50X2lkLzE0NjQ5ZTJmNDY4NDUwZGFjMGMxODM0ODExZGJkNGM3IiwiaWF0IjoxNDg0NzYzODQ0LCJub25jZSI6IjBaSWktRXV4ZUNfWDhBZ0IzVmlmT29xS2lYV3N6X05sWFN6SXU3aDhyelUiLCJhdXRoX3RpbWUiOiIxNDg0NzYzODQzIn0";
        String keyID = "aQEiCy2fJcVgkOft";
        KeyPair keyPair = KeyUtil.generateKeyPair();

        JSONWebKeys keys = new JSONWebKeys(keyID);
        JSONWebKey key = new JSONWebKey();
        key.privateKey = keyPair.getPrivate();
        key.publicKey = keyPair.getPublic();
        key.algorithm = "RS256";
        key.id = keyID;
        key.use = "sig";
        key.type = "RSA";
        keys.put(key);

        System.out.println("----- START keys");
        System.out.println(KeyUtil.toX509PEM(keyPair.getPublic()));
        System.out.println(KeyUtil.toPKCS1PEM(keyPair.getPrivate()));
        System.out.println(KeyUtil.toPKCS8PEM(keyPair.getPrivate()));
        System.out.println("----- END keys\n");


        JSONObject jsonKeys = JSONWebKeyUtil.toJSON(keys);
        JSONWebKeys keys2 = JSONWebKeyUtil.fromJSON(jsonKeys.toString(2));

        JSONWebKey webKey = keys2.get(keyID);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(webKey.privateKey.getEncoded());
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        System.out.println(KeyUtil.toX509PEM(webKey.publicKey));
        System.out.println(KeyUtil.toPKCS1PEM(privateKey));
        System.out.println(KeyUtil.toPKCS8PEM(privateKey));

    }

    public static void firstTestB() throws Exception {
        String keyID = "9k0HPG3moXENne";
        JSONWebKeys keys = JSONWebKeyUtil.fromJSON(new File("/home/ncsa/dev/csd/config/keys.jwk"));

        JSONObject payload = new JSONObject();
        payload.put("name", "jeff");
        payload.put("id", "sukjfhusdfsdjkfh");
        payload.put("other_claim", "skjdf93489ghiovs 98sd89wehi ws");
        payload.put("another_claim", "l;kfg8934789dfio9v 92w89 98wer");
        JSONWebKey webKey = keys.get(keyID);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(webKey.privateKey.getEncoded());
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        System.out.println(KeyUtil.toX509PEM(webKey.publicKey));
        System.out.println(KeyUtil.toPKCS1PEM(privateKey));
        System.out.println(KeyUtil.toPKCS8PEM(privateKey));
        String tokken = createIDToken(payload, keys.get(keyID));

        System.out.println("JWT=" + tokken);
        System.out.println("claims=" + verifyAndReadIDToken(tokken, keys));
        System.out.println("-----");

        // note that if the this last call
        // works it is because the verification works too.
    }

    public static void testSigningDirectly() throws Exception {
        String keyID = "9k0HPG3moXENne";
        JSONWebKeys keys = JSONWebKeyUtil.fromJSON(new File("/home/ncsa/dev/csd/config/keys.jwk"));

        JSONWebKey jsonWebKey = keys.get(keyID);

        JSONObject payload = new JSONObject();
        payload.put("name", "jeff");
        payload.put("id", "sukjfhusdfsdjkfh");
        payload.put("other_claim", "skjdf93489ghiovs 98sd89wehi ws");
        payload.put("another_claim", "l;kfg8934789dfio9v 92w89 98wer");
        JSONObject header = new JSONObject();
        header.put(TYPE, "JWT");
        header.put(KEY_ID, jsonWebKey.id);

        header.put(ALGORITHM, jsonWebKey.algorithm);

        // create signature
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(jsonWebKey.privateKey.getEncoded());
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(getJavaSignatureName(jsonWebKey.algorithm));
        Signature signature1 = Signature.getInstance(getJavaSignatureName(jsonWebKey.algorithm));
//         signature.initSign(jsonWebKey.privateKey);
        signature.initSign(privateKey);
        byte[] content = concat(header, payload).getBytes();
        signature.update(content);
        byte[] signatureBytes = signature.sign();

        JSONWebKeys pubKeys = JSONWebKeyUtil.makePublic(keys);
        JSONWebKey jsonWebKey1 = pubKeys.get(keyID);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(jsonWebKey1.publicKey.getEncoded());
        RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

        signature1.initVerify(pubKey);
        signature1.update(content);

        System.out.println("sig verifies?=" + signature1.verify(signatureBytes));

    }

    public static void testJWT_IO() throws Exception {
        String header = "{" +
                "  \"typ\": \"JWT\"," +
                "  \"kid\": \"9k0HPG3moXENne\"," +
                "  \"alg\": \"RS256\"" +
                "}";

        String payload = "{\n" +
                "  \"iss\": \"https://ashigaru.ncsa.uiuc.edu:9443\"," +
                "  \"sub\": \"jgaynor\"," +
                "  \"exp\": 1484764744," +
                "  \"aud\": \"myproxy:oa4mp,2012:/client_id/14649e2f468450dac0c1834811dbd4c7\"," +
                "  \"iat\": 1484763844," +
                "  \"nonce\": \"0ZIi-EuxeC_X8AgB3VifOoqKiXWsz_NlXSzIu7h8rzU\"," +
                "  \"auth_time\": \"1484763843\"" +
                "}";
        String keyID = "9k0HPG3moXENne";
        JSONWebKeys keys = JSONWebKeyUtil.fromJSON(new File("/home/ncsa/dev/csd/config/keys.jwk"));

        JSONWebKey jsonWebKey = keys.get(keyID);
        JSONObject h = JSONObject.fromObject(header);
        JSONObject p = JSONObject.fromObject(payload);
        String signature = sign(h, p, jsonWebKey);
        System.out.println(signature);

    }

    public static String ID_TOKKEN = "eyJ0eXAiOiJKV1QiLCJraWQiOiI5azBIUEczbW9YRU5uZSIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJodHRwczovL2FzaGlnYXJ1Lm5jc2EudWl1Yy5lZHU6OTQ0MyIsInN1YiI6ImpnYXlub3IiLCJleHAiOjE0ODQ3NjQ3NDQsImF1ZCI6Im15cHJveHk6b2E0bXAsMjAxMjovY2xpZW50X2lkLzE0NjQ5ZTJmNDY4NDUwZGFjMGMxODM0ODExZGJkNGM3IiwiaWF0IjoxNDg0NzYzODQ0LCJub25jZSI6IjBaSWktRXV4ZUNfWDhBZ0IzVmlmT29xS2lYV3N6X05sWFN6SXU3aDhyelUiLCJhdXRoX3RpbWUiOiIxNDg0NzYzODQzIn0.PXxUPRJ1aPQmcgfidz1xf28Ip3g3TCWldAPT25JVhsu5kJw75mDjPFVaHvcGOnxO121PAlisQlqARqpx3ytW720odRHEhv3JmVjvoRyKeCHzAP7va75cZmgOWDUI9SONDuNcuomRbUrRyLwrgH2CtBrKr05AowYojkJspRf3a5z6K5s-6ahbUlm7AAmYFDceNtQBeiutCZBfP4_gMLAxdQb7pHfyocKslAV0CwtAKYvqUpkIHuUYsc5CXYuan2Ox0If_pMJC4uV3Ov4banMNLwKeQPRUyWhHLnhrMl5KeoaEtL2nW4X7JIqK8EX-esmjQmr_NVI7DP8DV1C3OjHkpA";
}
