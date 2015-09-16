package se.rosenbaum.jpop.validate;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;

import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.rosenbaum.jpop.Pop;

import java.util.Arrays;
import java.util.List;

/**
 * This class validates a PoP according to BIP120 (as in section "Validating a PoP" of https://github.com/bitcoin/bips/blob/master/bip-0120.mediawiki)
 */
public class PopValidator {
    private static final long LOCK_TIME = 499999999;
    Logger logger = LoggerFactory.getLogger(PopValidator.class);
    TransactionStore transactionStore;

    /**
     *
     * @param transactionStore The validator will need access to few transactions upon validating a Pop. First, it's
     *                         the proven transaction, second it's the input transactions of the proven transaction. All
     *                         these transactions must be available through the transactionStore.
     */
    public PopValidator(TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
    }

    /**
     * This will check the PoP according to the
     * <a href="https://github.com/bitcoin/bips/blob/master/bip-0120.mediawiki">specification</a>
     * Note that step 7 and 8 in the specification is not performed by this method. It should instead be done by the
     * user of this class AFTER this method is called.
     * @param pop The pop to validate
     * @param nonce The requested nonce to be checked against the nonce in the PoP
     * @return the transaction that the pop proves.
     * @throws InvalidPopException If the pop is invalid.
     */
    public Transaction validatePop(Pop pop, byte[] nonce) throws InvalidPopException {
        // 1 Basic checks
        if (pop == null) {
            throw new InvalidPopException("Pop is null");
        }
        try {
            pop.verify();
        } catch (VerificationException e) {
            throw new InvalidPopException("Basic verification failed.", e);
        }

        // 2 Check lock_time
        checkLockTime(pop);

        // 3 Check the "PoP output"
        byte[] data = checkOutput(pop);

        // 4 Check nonce
        checkNonce(data, nonce);

        byte[] txidBytes = new byte[32];
        System.arraycopy(data, 3, txidBytes, 0, 32);
        Sha256Hash txid = new Sha256Hash(txidBytes);
        Transaction provenTransaction = getTransaction(txid);

        // 5 Check inputs
        // 6 Check signatures
        checkInputsAndSignatures(pop, provenTransaction);

        // No exceptions, means PoP valid.
        return provenTransaction;
    }

    private void checkLockTime(Pop pop) throws InvalidPopException {
        if (pop.getLockTime() != LOCK_TIME) {
            throw new InvalidPopException("Invalid lock_time. Expected " + LOCK_TIME);
        }
    }

    private Transaction getTransaction(Sha256Hash txid) throws InvalidPopException {
        Transaction localTransaction = transactionStore.getTransaction(txid);
        if (localTransaction == null) {
            throw new InvalidPopException("Unknown transaction");
        }
        return localTransaction;
    }

    /**
     * The last 6 bytes of the pop output is the nonce
     * @param data the full 41 bytes pop output script
     * @param popRequestNonce The requested nonce to be checked against the nonce in the PoP
     * @throws InvalidPopException
     */
    private void checkNonce(byte[] data, byte[] popRequestNonce) throws InvalidPopException {
        byte[] nonceBytes = new byte[6];
        System.arraycopy(data, 35, nonceBytes, 0, 6);

        if (!Arrays.equals(nonceBytes, popRequestNonce)) {
            throw new InvalidPopException("Wrong nonce");
        }
    }

    /**
     * This implements step 5 and 6 of the validation process. Inputs of the PoP must match the inputs of the proven
     * transaction and the sequence numbers must all be 0. Finally the scripts are executed on all inputs. All
     * scripts must return true for the pop to be valid.
     */
    private void checkInputsAndSignatures(Pop pop, Transaction provenTransaction) throws InvalidPopException {
        List<TransactionInput> popInputs = pop.getInputs();
        List<TransactionInput> blockchainTxInputs = provenTransaction.getInputs();
        if (popInputs.size() != blockchainTxInputs.size()) {
            throw new InvalidPopException("Wrong number of inputs");
        }

        for (int i = 0; i < blockchainTxInputs.size(); i++) {
            // Here we check that the inputs of the pop are in the same order
            // as in the payment transaction.
            TransactionInput popInput = popInputs.get(i);
            TransactionInput bcInput = blockchainTxInputs.get(i);
            if (!popInput.getOutpoint().equals(bcInput.getOutpoint())) {
                throw new InvalidPopException("Mismatching inputs");
            }
            // Also check the sequence number of the pop input.
            if (popInput.getSequenceNumber() != 0L) {
                throw new InvalidPopException("Invalide sequence number. Must be 0.");
            }
        }


        for (int i = 0; i < blockchainTxInputs.size(); i++) {
            TransactionInput popInput = popInputs.get(i);
            TransactionInput txInput = blockchainTxInputs.get(i);
            // Check signature
            if (txInput.getConnectedOutput() == null || popInput.getConnectedOutput() == null) {
                // connect the input to the right transaction:
                Sha256Hash hash = txInput.getOutpoint().getHash();
                Transaction inputTx = transactionStore.getTransaction(hash);

                if (inputTx == null) {
                    String message = "Could not find input tx: " + hash;
                    logger.debug(message);
                    throw new InvalidPopException(message);
                }
                txInput.connect(inputTx, TransactionInput.ConnectMode.ABORT_ON_CONFLICT);
                popInput.connect(inputTx, TransactionInput.ConnectMode.ABORT_ON_CONFLICT);
            }
            try {
                popInput.verify();
            } catch (VerificationException e) {
                logger.debug("Failed to verify input", e);
                throw new InvalidPopException("Signature verification failed", e);
            }
        }
    }

    /**
     * The pop output must have the format "OP_RETURN <version 2 bytes> <38 bytes txid+nonce>"
     * @param pop
     * @return
     * @throws InvalidPopException
     */
    private byte[] checkOutput(Pop pop) throws InvalidPopException {
        List<TransactionOutput> outputs = pop.getOutputs();
        if (outputs == null || outputs.size() != 1) {
            throw new InvalidPopException("Wrong number of outputs. Expected 1.");
        }
        TransactionOutput output = outputs.get(0);

        if (!Coin.ZERO.equals(output.getValue())) {
            throw new InvalidPopException("Invalid value of PoP output. Must be 0");
        }

        byte[] scriptBytes = output.getScriptBytes();
        if (scriptBytes == null || scriptBytes.length != 41) {
            throw new InvalidPopException("Invalid script length. Expected 41");
        }
        if (scriptBytes[0] != ScriptOpCodes.OP_RETURN) {
            throw new InvalidPopException("Wrong opcode: " + scriptBytes[0]);
        }

        if (scriptBytes[1] != 1 || scriptBytes[2] != 0) {
            throw new InvalidPopException("Wrong version: " + scriptBytes[1] + " " + scriptBytes[2] + ". Expected 0x01 0x00");
        }

        return scriptBytes;
    }
}
