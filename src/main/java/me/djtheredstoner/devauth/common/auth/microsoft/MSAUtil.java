package me.djtheredstoner.devauth.common.auth.microsoft;

import me.djtheredstoner.devauth.common.auth.microsoft.token.Token;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class MSAUtil {

    public static String buildQuery(Map<String, String> params) {
        try {
            StringJoiner queryString = new StringJoiner("&");

            for (Map.Entry<String, String> param : params.entrySet()) {
                queryString.add(param.getKey() + "=" + URLEncoder.encode(param.getValue(), "UTF-8"));
            }

            return queryString.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();

        try {
            for (String part : query.split("&")) {
                String[] kv = part.split("=");
                map.put(
                    URLDecoder.decode(kv[0], "UTF-8"),
                    URLDecoder.decode(kv[1], "UTF-8")
                );
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    public static boolean isValid(Token token) {
        return token != null && !token.isExpired();
    }
}
