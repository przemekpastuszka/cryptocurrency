import java.security.PublicKey;
import java.util.List;

public class Utils {

    public static TransactionBuilder bT() {
        return new TransactionBuilder();
    }

    static class TransactionBuilder {
        private final Transaction transaction = new Transaction();


        public TransactionBuilder addInput(byte[] hash, int index) {
            return addInput(hash, index, hash);
        }

        public TransactionBuilder addInput(byte[] hash, int index, byte[] signature) {
            transaction.addInput(hash, index);
            int inputIndex = transaction.getInputs().size() - 1;
            transaction.addSignature(signature, inputIndex);
            return this;
        }

        public TransactionBuilder addOutput(double v, PublicKey address) {
            transaction.addOutput(v, address);
            return this;
        }

        public Transaction build() {
            return transaction;
        }

        public Transaction build(byte[] hash) {
            transaction.setHash(hash);
            return transaction;
        }
    }

    static UTXOPool createPoolOf(List<Transaction> transactions) {
        UTXOPool pool = new UTXOPool();
        for (Transaction transaction : transactions) {
            int i = 0;
            for (Transaction.Output output : transaction.getOutputs()) {
                pool.addUTXO(new UTXO(transaction.getHash(), i++), output);
            }
        }
        return pool;
    }
}
