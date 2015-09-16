package se.rosenbaum.jpop;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 * This is an implementation of BIP121 (https://github.com/bitcoin/bips/blob/master/bip-0121.mediawiki), "Proof
 * of Payment URI scheme". For details on how to interpret the properties of this class, please see BIP121.
 */
public class PopRequestURI implements Serializable {
    private byte[] n;
    private Long amountSatoshis;
    private String label;
    private String message;
    private Sha256Hash txid;
    private String p;

    /**
     * This constructor is typically intended for the validating party, who constructs a PopRequest and generates
     * a PopRequestURI from that. The URI is then transfered to the proving party, by getting the string representation
     * of the URI with toURIString.
     * @param request The PopRequest that this URI should represent.
     */
    public PopRequestURI(PopRequest request) {
        if (request.getNonce() == null) {
            throw new IllegalArgumentException("Nonce must not be null!");
        }
        n = request.getNonce();
        if (request.getDestination() == null) {
            throw new IllegalArgumentException("Destination must not be null!");
        }
        p = request.getDestination();
        if (request.getAmount() != null) {
            amountSatoshis = request.getAmount().longValue();
        }
        label = request.getLabel();
        message = request.getMessage();
        txid = request.getTxid();

    }

    /**
     * This constructor is typically intended for the proving party upon reception of a URI.
     *
     * @param input the URI string as receveived from the validating party.
     */
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
        if (p == null || n == null) {
            throw new IllegalArgumentException("p and n must be set");
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

    public String getMessage() {
        return message;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public String getP() {
        return p;
    }

    /**
     * This will return a string representation of the URI. This string can then be used to request a PoP from the
     * other party.
     */
    public String toURIString() {
        StringBuffer result = new StringBuffer("btcpop:?p=");
        result.append(PopURIEncodeDecode.popURIEncode(getP()));
        addParameter("n", Base58.encode(getN()), result);
        if (getTxid() != null) {
            addParameter("txid", Base58.encode(getTxid().getBytes()), result);
        }
        if (getLabel() != null) {
            addParameter("label", getLabel(), result);
        }
        if (getMessage() != null) {
            addParameter("message", getMessage(), result);
        }
        if (getAmountSatoshis() != null) {
            addParameter("amount", Coin.valueOf(getAmountSatoshis()).toPlainString(), result);
        }
        return result.toString();
    }

    private void addParameter(String key, String unencodedValue, StringBuffer result) {
        result.append('&').append(key).append('=').append(PopURIEncodeDecode.popURIEncode(unencodedValue));
    }

    public String toString() {
        return "txid=" + getTxid() + ", label=" + getLabel() + ", amount=" + getAmountSatoshis();
    }
}
