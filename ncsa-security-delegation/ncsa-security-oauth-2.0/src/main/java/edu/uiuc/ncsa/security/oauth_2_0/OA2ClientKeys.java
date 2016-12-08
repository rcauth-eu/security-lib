package edu.uiuc.ncsa.security.oauth_2_0;


import edu.uiuc.ncsa.security.delegation.storage.ClientKeys;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/14/14 at  1:05 PM
 */
public class OA2ClientKeys extends ClientKeys {
    public OA2ClientKeys() {
        super();
        identifier("client_id");
        secret("public_key");
    }


    String callback_uri = "callback_uri";

    public String callbackUri(String... x) {
        if (0 < x.length) callback_uri = x[0];
        return callback_uri;
    }

    String rtLifetime = "rt_lifetime";

    public String rtLifetime(String... x) {
        if (0 < x.length) rtLifetime = x[0];
        return rtLifetime;
    }

    String scopes = "scopes";

    public String scopes(String... x) {
        if (0 < x.length) scopes = x[0];
        return scopes;
    }

    @Override
    public List<String> allKeys() {
        List<String> allKeys = super.allKeys();
        allKeys.add(callbackUri());
        allKeys.add(rtLifetime());
        allKeys.add(scopes());
        return allKeys;
    }
}
