package edu.uiuc.ncsa.security.oauth_2_0.server;

import edu.uiuc.ncsa.security.core.util.DebugUtil;
import edu.uiuc.ncsa.security.delegation.server.ServiceTransaction;
import edu.uiuc.ncsa.security.delegation.server.request.ATResponse;
import edu.uiuc.ncsa.security.delegation.token.AccessToken;
import edu.uiuc.ncsa.security.delegation.token.RefreshToken;
import edu.uiuc.ncsa.security.delegation.token.Verifier;
import edu.uiuc.ncsa.security.oauth_2_0.IDTokenUtil;
import edu.uiuc.ncsa.security.oauth_2_0.UserInfo;
import edu.uiuc.ncsa.security.util.jwk.JSONWebKey;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static edu.uiuc.ncsa.security.oauth_2_0.OA2Constants.*;
import static edu.uiuc.ncsa.security.oauth_2_0.server.OA2Claims.*;

/**
 * OIDC server response for request for access token
 * <p>Created by Jeff Gaynor<br>
 * on 6/4/13 at  5:10 PM
 */
public class ATIResponse2 extends IResponse2 implements ATResponse {

    public ATIResponse2(AccessToken accessToken, RefreshToken refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    ScopeHandlerFactory scopeHandlerFactory;
    ServiceTransaction serviceTransaction;

    public ServiceTransaction getServiceTransaction() {
        return serviceTransaction;
    }

    public void setServiceTransaction(ServiceTransaction serviceTransaction) {
        this.serviceTransaction = serviceTransaction;
    }

    public JSONWebKey getJsonWebKey() {
        return jsonWebKey;
    }

    public void setJsonWebKey(JSONWebKey jsonWebKey) {
        this.jsonWebKey = jsonWebKey;
    }

    JSONWebKey jsonWebKey;

    public boolean isSignToken() {
        return signToken;
    }

    public void setSignToken(boolean signToken) {
        this.signToken = signToken;
    }

    boolean signToken = false;

    Collection<? extends ScopeHandler> scopeHandlers;

    public Collection<? extends ScopeHandler> getScopeHandlers() {
        return scopeHandlers;
    }

    public void setScopeHandlers(Collection<? extends ScopeHandler> scopeHandler) {
        this.scopeHandlers = scopeHandler;
    }

    /**
     * The server must decide which scopes to return if any.
     *
     * @return
     */
    public Collection<String> getSupportedScopes() {
        return supportedScopes;
    }

    public void setSupportedScopes(Collection<String> supportedScopes) {
        this.supportedScopes = supportedScopes;
    }

    Collection<String> supportedScopes = new ArrayList<>();
    RefreshToken refreshToken;

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    AccessToken accessToken;
    Verifier verifier;

    /**
     * Getter for access token
     *
     * @return access token
     */
    public AccessToken getAccessToken() {
        return accessToken;
    }

    /**
     * Getter for verifier
     * This shouldn't be called in OIDC, but it's here temporarily
     *
     * @return verifier (should be null)
     */
    public Verifier getVerifier() {
        return verifier;
    }

    /**
     * Setter for access token
     *
     * @param accessToken Access token
     */
    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Setter for verifier
     * This needs to go away since OIDC doesn't use verifiers
     *
     * @param verifier Verifier object (probably null)
     */
    public void setVerifier(Verifier verifier) {
        this.verifier = verifier;
    }

    /**
     * Write JSON response to response's output stream
     *
     * @param response Response to write to
     */
    public void write(HttpServletResponse response) throws IOException {
        Writer osw = response.getWriter();
        HashMap m = new HashMap();
        m.put(ACCESS_TOKEN, accessToken.getToken());
        m.put(TOKEN_TYPE, "Bearer");
        if (getRefreshToken() != null && getRefreshToken().getToken() != null) {
            m.put(REFRESH_TOKEN, getRefreshToken().getToken());
            m.put(EXPIRES_IN, (getRefreshToken().getExpiresIn() / 1000));
        }
        //m.put(SCOPE, "openid"); // all we support in base protocol.
        if (!getSupportedScopes().isEmpty()) {
            // construct the scope response.
            String ss = "";
            boolean firstPass = true;
            for (String s : getSupportedScopes()) {
                ss = ss + (firstPass ? "" : " ") + s;
                if (firstPass) {
                    firstPass = false;
                }
            }
            m.put(SCOPE, ss);
        }
        JSONObject claims = new JSONObject();
        // All of these are required.
        claims.put(ISSUER, parameters.get(ISSUER)); // Issuer - url of server
        claims.put(SUBJECT, parameters.get(SUBJECT)); // subject - unique user id, i.e., username
        claims.put(EXPIRATION, System.currentTimeMillis() / 1000 + 15 * 60); // expiration is in SECONDS from the epoch.
        claims.put(AUDIENCE, parameters.get(CLIENT_ID)); // audience = client id.
        claims.put(ISSUED_AT, System.currentTimeMillis() / 1000); // issued at = current time in seconds.
        claims.put(NONCE, parameters.get(NONCE)); // nonce must match that in authz request.
/*        DebugUtil.dbg(this,"REMOVE email from claims");
        claims.put("email","gaynor@illinois.edu");*/
        // Optional claims the handler may over-write the default claims as needed.
        if (parameters.containsKey(AUTHORIZATION_TIME)) {
            claims.put(AUTHORIZATION_TIME, parameters.get(AUTHORIZATION_TIME));
        }
        if (getScopeHandlers() != null) {

            UserInfo userInfo = new UserInfo();
            userInfo.setMap(claims);
            if (getScopeHandlers() != null) {
                for (ScopeHandler scopeHandler : getScopeHandlers()) {
                    scopeHandler.process(userInfo, getServiceTransaction());
                }
            }
            claims = userInfo.toJSon();
        } else {
            DebugUtil.dbg(this, "NO scope handler");
        }


        try {
            String idTokken = null;
            if(isSignToken()) {
                idTokken = IDTokenUtil.createIDToken(claims, getJsonWebKey());
            }else{
                idTokken = IDTokenUtil.createIDToken(claims);
            }
            m.put(ID_TOKEN, idTokken);
        } catch (Throwable e) {
            throw new IllegalStateException("Error: cannot create token", e);
        }

        JSONObject json = JSONObject.fromObject(m);
        json.write(osw);
        osw.flush();
        osw.close();
    }


}
