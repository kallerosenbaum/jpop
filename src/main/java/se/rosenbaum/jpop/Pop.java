package se.rosenbaum.jpop;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.ScriptOpCodes;

import java.nio.ByteBuffer;

public class Pop extends Transaction {
    private static final int POP_LOCK_TIME = 499999999;
    private static final long POP_SEQ_NR = 0L;

    public Pop(NetworkParameters params, byte[] payloadBytes) {
        super(params, payloadBytes);
    }

    public Pop(NetworkParameters params, byte[] payloadBytes, byte[] nonce) throws ProtocolException {
        this(params, payloadBytes);
        Sha256Hash txidToProve = getHash();
        setLockTime(POP_LOCK_TIME);
        for (TransactionInput input : getInputs()) {
            input.setSequenceNumber(POP_SEQ_NR);
        }
        clearOutputs();

        ByteBuffer byteBuffer = ByteBuffer.allocate(41);
        byteBuffer.put((byte) ScriptOpCodes.OP_RETURN);

        // version 0x01 0x00 (1 little endian)
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte)0);

        byteBuffer.put(txidToProve.getBytes()); // txid

        byteBuffer.put(nonce);

        TransactionOutput output = new TransactionOutput(params, this, Coin.ZERO, byteBuffer.array());

        addOutput(output);
    }
}
