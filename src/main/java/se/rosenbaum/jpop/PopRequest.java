package se.rosenbaum.jpop;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;

public class PopRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] nonce;

    private String destination;

    private String label;

    private Coin amount;

    private String message;

    private Sha256Hash txid;

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Coin getAmount() {
        return amount;
    }

    public void setAmount(Coin amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public void setTxid(Sha256Hash txid) {
        this.txid = txid;
    }

}
