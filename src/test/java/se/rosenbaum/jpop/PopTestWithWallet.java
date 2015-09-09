package se.rosenbaum.jpop;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.testing.FakeTxBuilder;
import org.bitcoinj.testing.TestWithWallet;
import se.rosenbaum.jpop.validate.TransactionStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PopTestWithWallet extends TestWithWallet {
    protected Wallet payerWallet;

    protected static byte[] bLength(int times, int value) {
        byte[] bytes = new byte[times];
        for (int i = 0; i < times; i++) {
            bytes[i] = (byte)value;
        }
        return bytes;

    }

    protected static byte[] b(int... byteValues) {
        byte[] bytes = new byte[byteValues.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)byteValues[i];
        }
        return bytes;
    }

    public void setup() throws Exception {
        setUp();
        payerWallet = new Wallet(params);
    }

    protected Transaction createPaymentToProve(List<Transaction> funding, Coin fee, int... outputValues) throws InsufficientMoneyException {

        Wallet.SendRequest request = Wallet.SendRequest.to(wallet.currentReceiveAddress(), Coin.ZERO);
        request.ensureMinRequiredFee = false;
        request.fee = fee;
        request.shuffleOutputs = false;
        request.tx.clearOutputs();
        for (int outputValue : outputValues) {
            if (outputValue <= 0) {
                // <= 0 indicates that we want an OP_RETURN output with value=-outputValue
                Script script = new Script(new byte[] {ScriptOpCodes.OP_RETURN});
                request.tx.addOutput(Coin.valueOf(-outputValue, 0), script);
                request.tx.getOutput(request.tx.getOutputs().size() - 1).toString();
            } else {
                Address destination = wallet.freshReceiveAddress();
                request.tx.addOutput(Coin.valueOf(outputValue, 0), destination);
            }
        }

        Transaction txToProve = payerWallet.sendCoinsOffline(request);
        wallet.receivePending(txToProve, funding);
        return txToProve;
    }

    protected List<Transaction> createFundingTransaction(int... values) throws IOException {
        List<Transaction> fundingTransactions = new ArrayList<Transaction>();
        for (int value : values) {
            Transaction fakeTx = FakeTxBuilder.createFakeTx(params, Coin.valueOf(value, 0), payerWallet.freshReceiveAddress());
            Transaction sentTx = sendMoneyToWallet(payerWallet, fakeTx, AbstractBlockChain.NewBlockType.BEST_CHAIN);
            fundingTransactions.add(sentTx);
        }
        return fundingTransactions;
    }

    public class FakeWalletTransactionStore implements TransactionStore {
        public Transaction getTransaction(Sha256Hash txid) {
            Transaction transaction = wallet.getTransaction(txid);
            if (transaction != null) {
                return transaction;
            }
            transaction = payerWallet.getTransaction(txid);
            return transaction;
        }
    }
}
