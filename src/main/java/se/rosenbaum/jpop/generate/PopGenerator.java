package se.rosenbaum.jpop.generate;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import se.rosenbaum.jpop.Pop;

import java.util.List;

/**
 * This is the class to use for the proving party.
 */
public class PopGenerator {
    private Wallet payerWallet;

    public PopGenerator(Wallet payerWallet) {
        this.payerWallet = payerWallet;
    }

    /**
     * Creates a PoP of the given transaction. The transaction to prove must
     * have all it's inputs connected.
     *
     * @param transaction a fully connected transaction. Must not be null.
     * @return a signed Pop
     * @throws PopGenerationException if failure to create a pop for some reason, for example, if the transaction is not
     * connected, or some key is missing.
     * @throws NullPointerException if transaction is null
     */
    public Pop createPop(Transaction transaction, byte[] nonce) throws PopGenerationException {
        if (transaction == null) {
            throw new NullPointerException("Transaction must not be null");
        }
        Pop pop = new Pop(payerWallet.getParams(), transaction.bitcoinSerialize(), nonce);

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

        try {
            // The PoP is signed using the exact same signing as for an ordinary transaction
            payerWallet.signTransaction(Wallet.SendRequest.forTx(pop));
        } catch (Exception e) {
            throw new PopGenerationException("Could not sign pop", e);
        }
        return pop;
    }
}
