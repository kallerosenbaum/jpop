package se.rosenbaum.jpop.generate;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.KeyCrypterException;
import org.spongycastle.crypto.params.KeyParameter;
import se.rosenbaum.jpop.Pop;

import java.util.List;

/**
 * This is the class to use for the proving party.
 */
public class PopGenerator {

    /**
     * Creates an unsigned PoP of the given transaction. The transaction to prove must
     * have all it's inputs connected.
     *
     * @param transaction a fully connected transaction. Must not be null.
     * @param nonce The nonce that the server requires.
     * @return an unsigned Pop
     * @throws PopGenerationException if failure to create a pop for some reason, for example, if the transaction is not
     * connected.
     * @throws NullPointerException if transaction is null
     */
    public Pop createPop(Transaction transaction, byte[] nonce) throws PopGenerationException {
        if (transaction == null) {
            throw new NullPointerException("Transaction must not be null");
        }
        Pop pop;
        try {
            pop = new Pop(transaction.getParams(), transaction.bitcoinSerialize(), nonce);
        } catch (Exception e) {
            throw new PopGenerationException("Could not create PoP: " + e.getMessage(), e);
        }

        // In order to sign the PoP, all inputs must be connected. This is done by copying the connected outputs from
        // the proven transaction to the Pop.
        List<TransactionInput> txInputs = transaction.getInputs();
        long inputSize = txInputs.size();
        for (int i = 0; i < inputSize; i++) {
            // Connect the pop with the same input transactions as the payment.
            TransactionOutput connectedOutput = txInputs.get(i).getOutpoint().getConnectedOutput();
            if (connectedOutput == null) {
                throw new PopGenerationException("Transaction to prove is not fully connected");
            }
            TransactionInput popInput = pop.getInput(i);
            popInput.connect(connectedOutput.getParentTransaction(), TransactionInput.ConnectMode.ABORT_ON_CONFLICT);
        }
        return pop;
    }

    /**
     * This is a convenience method to sign a pop. Users who want more control over the signing should do this
     * themselves.
     * @param pop The unsigned Pop to sign.
     * @param wallet The wallet to sign the pop with
     * @param decryptionKey The key to decrypt the signing keys with.
     * @return the signed Pop.
     * @throws PopSigningException
     */
    public void signPop(Pop pop, Wallet wallet, KeyParameter decryptionKey) throws PopSigningException {
        try {
            // The PoP is signed using the exact same signing as for an ordinary transaction
            Wallet.SendRequest sendRequest = Wallet.SendRequest.forTx(pop);
            sendRequest.aesKey = decryptionKey;
            wallet.signTransaction(sendRequest);
        } catch (KeyCrypterException e) {
            throw new PopSigningException("Couldn't sign pop: " + e.getMessage(), e, true);
        } catch (Exception e) {
            throw new PopSigningException("Could not sign pop: " + e.getMessage(), e);
        }
    }
}
