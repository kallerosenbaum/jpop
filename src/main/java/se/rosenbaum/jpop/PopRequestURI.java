package se.rosenbaum.jpop;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.StringTokenizer;

public class PopRequestURI implements Serializable {
    private byte[] n;
    private Long amountSatoshis;
    private String label;
    private String message;
    private Sha256Hash txid;
    private String p;
    private String r;

    public PopRequestURI(String input) {
        if (!input.startsWith("btcpop:?")) {
            throw new IllegalArgumentException("URI must start with 'btcpop:?':" + input);
        }

        String query = input.substring("btcpop:?".length());
        if (query == null) {
            throw new IllegalArgumentException("No query string");
        }
        StringTokenizer parameters = new StringTokenizer(query, "&", false);
        while (parameters.hasMoreTokens()) {
            String token = parameters.nextToken();
            if (token.startsWith("=")) {
                throw new IllegalArgumentException("Empty parameter name in: " + token);
            }
            if (!token.contains("=")) {
                throw new IllegalArgumentException("No '=' in: " + token);
            }
            String[] paramPair = token.split("=");
            if (paramPair.length > 2) {
                throw new IllegalArgumentException("More than 2 '=' characters in: " + token);
            }
            String key = paramPair[0];
            String value = null;
            if (paramPair.length == 2) {
                value = PopURIEncodeDecode.popURIDecode(paramPair[1]);
            }
            if ("n".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("Nonce must not be empty");
                }
                try {
                    n = Base58.decode(value);
                } catch (AddressFormatException e) {
                    throw new IllegalArgumentException("Can't Base58 decode value '" + value + "'", e);
                }
                if (n == null) {
                    throw new IllegalArgumentException("Nonce " + value + " cannot be base58 decoded");
                }
                if (n.length < 1) {
                    throw new IllegalArgumentException("Nonce too short");
                }
            } else if ("p".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("Pop URL must not be empty");
                }
                p = value;
            } else if ("r".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("r must not be empty");
                }
                r = value;
            } else if ("label".equals(key)) {
                label = value;
            } else if ("message".equals(key)) {
                message = value;
            } else if ("amount".equals(key)) {
                if (value != null) {
                    // Expect amount in BTC as in BIP0021
                    amountSatoshis = new BigDecimal(value).movePointRight(8).toBigIntegerExact().longValue();
                    if (amountSatoshis < 0) {
                        throw new IllegalArgumentException("Negative amount not allowed");
                    }
                    if (amountSatoshis > 2100000000000000L) {
                        throw new IllegalArgumentException("Too high amount: " + amountSatoshis);
                    }
                }
            } else if ("txid".equals(key)) {
                if (value == null) {
                    continue;
                }
                byte[] bytes;
                try {
                    bytes = Base58.decode(value);
                } catch (AddressFormatException e) {
                    throw new IllegalArgumentException("Can't Base58 decode value " + value, e);
                }
                if (bytes == null) {
                    throw new IllegalArgumentException("Can't Base58 decode value " + value);
                }
                if (bytes.length != 32) {
                    throw new IllegalArgumentException("Bad transaction id size " + bytes.length + ". Expected 32");
                }
                txid = Sha256Hash.wrap(bytes);
            }
        }
        if (r == null && (p == null || n == null)) {
            throw new IllegalArgumentException("p and n must be set if r is unset");
        }
    }

    public byte[] getN() {
        return n;
    }

    public Long getAmountSatoshis() {
        return amountSatoshis;
    }

    public String getLabel() {
        return label;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public String getP() {
        return p;
    }

    public String toString() {
        return "txid=" + getTxid() + ", label=" + getLabel() + ", amount=" + getAmountSatoshis();
    }
}
