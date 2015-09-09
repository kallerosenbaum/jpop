package se.rosenbaum.jpop.generate;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.testing.FakeTxBuilder;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.jpop.Pop;
import se.rosenbaum.jpop.PopTestWithWallet;
import se.rosenbaum.jpop.validate.PopValidator;

import java.util.ArrayList;
import java.util.List;

public class PopGeneratorTest extends PopTestWithWallet {
    PopGenerator sut;
    PopValidator popValidator;
    private byte[] nonce = b(1, 2, 3, 4, 5, 6);

    @Before
    public void setup() throws Exception {
        super.setup();
        sut = new PopGenerator(payerWallet);
        popValidator = new PopValidator(new FakeWalletTransactionStore());
    }

    @Test
    public void testOk() throws Exception {
        List<Transaction> fundingTransactions = createFundingTransaction(1, 2);
        Transaction paymentToProve = createPaymentToProve(fundingTransactions, Coin.ZERO, 3);
        Pop result = createPop(paymentToProve);
        popValidator.validatePop(result, nonce);
    }

    @Test(expected = NullPointerException.class)
    public void testNullInput() throws PopGenerationException {
        createPop(null);
    }

    @Test(expected = PopGenerationException.class)
    public void testNotConnectedInput() throws Exception {
        List<Transaction> fundingTransactions = createFundingTransaction(1, 2);
        Transaction paymentToProve = createPaymentToProve(fundingTransactions, Coin.ZERO, 3);
        paymentToProve.getInput(1).disconnect();
        createPop(paymentToProve);
    }

    @Test(expected = PopGenerationException.class)
    public void testMissingKey() throws Exception {
        List<Transaction> fundingTransactions = new ArrayList<Transaction>();
        ECKey ecKey = new ECKey();
        payerWallet.importKey(ecKey);
        Transaction fakeTx = FakeTxBuilder.createFakeTx(params, Coin.valueOf(4, 0), ecKey.toAddress(params));
        Transaction sentTx = sendMoneyToWallet(payerWallet, fakeTx, AbstractBlockChain.NewBlockType.BEST_CHAIN);
        fundingTransactions.add(sentTx);
        Transaction paymentToProve = createPaymentToProve(fundingTransactions, Coin.ZERO, 3);
        payerWallet.removeKey(ecKey);

        createPop(paymentToProve);
    }

    private Pop createPop(Transaction paymentToProve) throws PopGenerationException {
        return sut.createPop(paymentToProve, nonce);
    }
}