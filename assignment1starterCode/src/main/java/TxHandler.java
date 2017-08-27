
import java.util.*;

public class TxHandler {

    private final UTXOPool pool;
    private final Crypto crypto;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool pool) {
        this(pool, new Crypto());
    }

    public TxHandler(UTXOPool utxoPool, Crypto crypto) {
        this.pool = new UTXOPool(utxoPool);
        this.crypto = crypto;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        try {
            getClaimedUTXOs(tx);
            return true;
        } catch (InvalidTransaction invalidTransaction) {
            return false;
        }
    }


    private static class InvalidTransaction extends Exception {

    }

    public Collection<UTXO> getClaimedUTXOs(Transaction tx) throws InvalidTransaction {
        Set<UTXO> claimedUTXOs = new HashSet<>();

        double sumOfInputs = 0;
        int i = 0;
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxoCandidate = new UTXO(input.prevTxHash, input.outputIndex);
            if (claimedUTXOs.contains(utxoCandidate)) {
                throw new InvalidTransaction();
            }

            if (!pool.contains(utxoCandidate)) {
                throw new InvalidTransaction();
            }

            Transaction.Output createdBy = pool.getTxOutput(utxoCandidate);
            if (!crypto.verifySignature(createdBy.address, tx.getRawDataToSign(i++), input.signature)) {
                throw new InvalidTransaction();
            }

            claimedUTXOs.add(utxoCandidate);
            sumOfInputs += createdBy.value;
        }

        if (tx.getOutputs().stream().anyMatch(o -> o.value < 0)) {
            throw new InvalidTransaction();
        }

        double sumOfOutputs = tx.getOutputs().stream().mapToDouble(o -> o.value).sum();


        if (sumOfInputs < sumOfOutputs) {
            throw new InvalidTransaction();
        }

        return claimedUTXOs;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> appliedTransactions = new LinkedList<>();
        for (Transaction tx : possibleTxs) {
            try {
                Collection<UTXO> claimedUTXOs = getClaimedUTXOs(tx);
                for (UTXO claimedUTXO : claimedUTXOs) {
                    pool.removeUTXO(claimedUTXO);
                }

                int i = 0;
                for (Transaction.Output output : tx.getOutputs()) {
                    pool.addUTXO(new UTXO(tx.getHash(), i++), output);
                }
                appliedTransactions.add(tx);
            } catch (InvalidTransaction invalidTransaction) {
            }
        }
        return appliedTransactions.toArray(new Transaction[0]);
    }

}
