package se.rosenbaum.jpop;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * This will encode and decode strings for use in BIP121 URIs.
 */
public class PopURIEncodeDecode {

    /**
     * This will URLencode certain characters in the supplied value. Note that the whole value will NOT be URLEncoded,
     * because a BIP121 URI is NOT a URL, and thus can support more characters than a URL. For example, the characters
     * '/' and '?' are perfectly fine in BIP121 values. This helps in keeping the URIs short as discussed in the
     * BIP.
     * @param value
     * @return
     */
    static String popURIEncode(String value) {
        try {
            if (value == null) {
                return null;
            }
            StringBuffer buffer = new StringBuffer();
            Character highSurrogate = null;
            for (char c : value.toCharArray()) {
                if (Character.isHighSurrogate(c)) {
                    highSurrogate = c;
                } else if (Character.isLowSurrogate(c)) {
                    if (highSurrogate == null) {
                        throw new RuntimeException("Found low surroggate without preceeding high surrogate!");
                    } else {
                        buffer.append(URLEncoder.encode(new String(new char[]{highSurrogate, c}), "UTF-8"));
                        highSurrogate = null;
                    }
                } else if (c == ' ') {
                    buffer.append("%20");
                } else if (isIllegalCharacter(c)) {
                    buffer.append(URLEncoder.encode(c + "", "UTF-8"));
                } else {
                    buffer.append(c);
                }
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            // will not happen. Famous last words.
            return null;
        }
    }

    /**
     * Will decode the supplied value.
     * @return The decoded value if successful
     * @throws IllegalArgumentException if value contains an illegal character, for example '&' or ' '.
     */
    static String popURIDecode(String value) {
        try {
            if (value == null) {
                return null;
            }
            StringBuffer result = new StringBuffer();

            char[] chars = value.toCharArray();
            StringBuffer encodedCharacter = null;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '%') {
                    if (encodedCharacter == null) {
                        encodedCharacter = new StringBuffer();
                    }
                    if (chars.length < i+3) {
                        throw new IllegalArgumentException("Bad format of input "  + value);
                    }
                    encodedCharacter.append(c);
                    encodedCharacter.append(chars[++i]);
                    encodedCharacter.append(chars[++i]);
                } else if (isIllegalCharacter(c)) {
                    throw new IllegalArgumentException("Illegal character " + c + " in input " + value);
                } else {
                    if (encodedCharacter != null) {
                        result.append(URLDecoder.decode(encodedCharacter.toString(), "UTF-8"));
                        encodedCharacter = null;
                    }
                    result.append(c);
                }
            }
            if (encodedCharacter != null) {
                result.append(URLDecoder.decode(encodedCharacter.toString(), "UTF-8"));
            }
            return result.toString();
        } catch (UnsupportedEncodingException e) {
            // will not happen. Famous last words.
            return null;
        }
    }

    private static boolean isIllegalCharacter(char c) {
        return c < '!' || c == '"' || c == '#' || c == '%' || c == '&' || (c >= '<' && c <= '>')  || (c >= '[' && c <= '^') || c == '`' || (c >= '{' && c <= '}') ||  c > '~';
    }
}
