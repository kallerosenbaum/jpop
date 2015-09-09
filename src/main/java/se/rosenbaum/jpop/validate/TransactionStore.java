package se.rosenbaum.jpop.validate;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

public interface TransactionStore {
    Transaction getTransaction(Sha256Hash txid);
}
