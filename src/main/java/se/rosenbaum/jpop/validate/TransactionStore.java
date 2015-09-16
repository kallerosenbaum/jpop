package se.rosenbaum.jpop.validate;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

/**
 * This is an interface the must be implemented by the user of this library. When validating a pop a TransactionStore
 * is needed to fetch the proven transaction and all it's dependencies. It's up the the user how to get the transactions.
 */
public interface TransactionStore {
    Transaction getTransaction(Sha256Hash txid);
}
