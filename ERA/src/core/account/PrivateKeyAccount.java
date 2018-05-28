package core.account;

import core.crypto.Base58;
import core.crypto.Crypto;
import utils.Pair;

import java.util.Arrays;

public class PrivateKeyAccount extends PublicKeyAccount {

    private byte[] seed;
    private Pair<byte[], byte[]> keyPair;

    public PrivateKeyAccount(byte[] seed) {
        this.seed = seed;
        this.keyPair = Crypto.getInstance().createKeyPair(seed);
        this.publicKey = keyPair.getB();
        this.address = Crypto.getInstance().getAddress(this.publicKey);
        this.bytes = Base58.decode(address);
        this.shortBytes = Arrays.copyOfRange(this.bytes, 5, this.bytes.length);
    }

    public byte[] getSeed() {
        return this.seed;
    }

    public byte[] getPrivateKey() {
        return this.keyPair.getA();
    }

    public Pair<byte[], byte[]> getKeyPair() {
        return this.keyPair;
    }

}
