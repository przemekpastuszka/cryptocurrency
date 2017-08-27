import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.PublicKey;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestTxHandler {
    private byte[] tx1Hash = new byte[]{0};
    private byte[] tx2Hash = new byte[]{1};
    private byte[] unknownHash = new byte[]{2};
    @Mock
    private PublicKey publicKey1, publicKey2, unknownPK;
    @Mock
    private Crypto crypto;

    private TxHandler handler;

    @Before
    public void setUp() {
        UTXOPool pool = Utils.createPoolOf(asList(
                Utils.bT().
                        addOutput(10, publicKey1).
                        addOutput(20, publicKey1).
                        build(tx1Hash),
                Utils.bT().
                        addOutput(1, publicKey2).
                        addOutput(2, publicKey2).
                        build(tx2Hash)
        ));
        handler = new TxHandler(pool, crypto);

        when(publicKey1.getEncoded()).thenReturn(tx1Hash);
        when(crypto.verifySignature(eq(publicKey1), any(byte[].class), eq(tx1Hash))).thenReturn(true);
        when(crypto.verifySignature(eq(publicKey2), any(byte[].class), eq(tx2Hash))).thenReturn(true);
    }


    @Test
    public void testVerificationOfAllInputsBeingInThePool() {
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 1).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 2).build())).isFalse();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx2Hash, 0).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx2Hash, 1).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx2Hash, 2).build())).isFalse();
        assertThat(handler.isValidTx(Utils.bT().addInput(unknownHash, 0).build())).isFalse();
    }

    @Test
    public void testVerificationOfInputSignatures() {
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0, unknownHash).build())).isFalse();
    }

    @Test
    public void testVerificationFailsIfTheSameUTXOReferencedMoreThanOnce() {
        assertThat(handler.isValidTx(Utils.bT().
                addInput(tx1Hash, 0).
                addInput(tx1Hash, 0).
                build()
        )).isFalse();
    }

    @Test
    public void testAllOutputsAreNonNegative() {
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0).addOutput(0, publicKey1).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0).addOutput(2, publicKey1).build())).isTrue();
        assertThat(handler.isValidTx(Utils.bT().addInput(tx1Hash, 0).addOutput(-1, publicKey1).build())).isFalse();
    }

    @Test
    public void testYouCannotProduceMoreValueThenPresentInInput() {
        assertThat(handler.isValidTx(Utils.bT().
                addInput(tx1Hash, 0).
                addInput(tx2Hash, 0).
                addOutput(0, publicKey1).
                addOutput(11, publicKey1).
                build())).
                isTrue();

        assertThat(handler.isValidTx(Utils.bT().
                addInput(tx1Hash, 0).
                addInput(tx2Hash, 0).
                addOutput(0, publicKey1).
                addOutput(12, publicKey1).
                build())).
                isFalse();
    }

}
