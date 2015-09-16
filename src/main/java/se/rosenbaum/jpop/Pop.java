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

/**
 * This is the central data structure as described in BIP120. It is a subclass of Transaction because
 * it has the exact same format as a transaction, but it's not supposed to ever appear on the bitcoin p2p network.
 *
 * There are two constructors. One for the proving party, generating the PoP, and one for the validating party.
 */
public class Pop extends Transaction {
    private static final int POP_LOCK_TIME = 499999999;
    private static final long POP_SEQ_NR = 0L;

    /**
     * This constructor is intended for the validating party. The validator will construct a PoP from an incoming
     * message, for example through http POST
     *
     * @param payloadBytes the raw pop as received by the validator.
     */
    public Pop(NetworkParameters params, byte[] payloadBytes) {
        super(params, payloadBytes);
    }

    /**
     * This constuctor is intended for the PoP generating party. The generating party typically has selected a
     * transaction to generate a PoP for.
     * @param payloadBytes the raw transaction to generate a PoP for.
     * @param nonce the nonce as requested by the validating party.
     * @throws ProtocolException
     */
    public Pop(NetworkParameters params, byte[] payloadBytes, byte[] nonce) throws ProtocolException {
        this(params, payloadBytes); // This will create a copy of the transaction to prove
        Sha256Hash txidToProve = getHash(); // Remember the hash of the transaction to prove

        // Now, PoPify this "transaction". Set lock_time and sequence numbers, keep all the inputs, replace all outputs
        // with a single PoP output.

        // Set lock_time to maximum allowed block height. This is to ensure
        // it does not ever (well, thousands of years) end up in a block.
        setLockTime(POP_LOCK_TIME);
        for (TransactionInput input : getInputs()) {
            // Set sequence number to less than ffff so that lock_time has effect.
            input.setSequenceNumber(POP_SEQ_NR);
        }
        clearOutputs();

        // Create the PoP output "OP_RETURN <version> <txid> <nonce>"
        ByteBuffer byteBuffer = ByteBuffer.allocate(41);
        byteBuffer.put((byte) ScriptOpCodes.OP_RETURN);

        // version 0x01 0x00 (1 little endian)
        byteBuffer.put((byte)1);
        byteBuffer.put((byte)0);

        byteBuffer.put(txidToProve.getBytes()); // txid

        byteBuffer.put(nonce);

        TransactionOutput output = new TransactionOutput(params, this, Coin.ZERO, byteBuffer.array());

        // Add the PoP output as the only output.
        addOutput(output);
    }
}
