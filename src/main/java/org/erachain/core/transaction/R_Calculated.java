package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.crypto.Base58;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/*
 ## typeBytes
 0 - record type
 1 - record version
 2 - property 1
 3 = property 2

 #### PROPERTY 1
 typeBytes[2].0 = -128 if NO AMOUNT - check sign

 #### PROPERTY 2
 typeBytes[3].3-7 = point accuracy: -16..16 = BYTE - 16
 *
 */
public class R_Calculated extends TransactionAmount {

    private static final byte TYPE_ID = (byte) Transaction.CALCULATED_TRANSACTION;
    private static final String NAME_ID = "Calculated";
    protected String message;

    public R_Calculated(byte[] typeBytes, Account recipient, long key,
                        BigDecimal amount, String message, long txReference) {
        super(typeBytes, NAME_ID, null, (byte)0, recipient, amount, key, 0l, txReference);

        this.message = message;
        if (message == null)
            this.message = "";

    }

    public R_Calculated(byte[] typeBytes, Account recipient, long key,
                        BigDecimal amount, String message, long txReference, byte[] signature) {
        this(typeBytes, recipient, key, amount, message, txReference);
        this.signature = signature;
    }

    public R_Calculated(Account recipient, long key,
                        BigDecimal amount, String message, long txReference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, recipient, key, amount, message, txReference);
    }

    public R_Calculated(Account recipient, long key,
                        BigDecimal amount, String message, long txReference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, recipient, key, amount, message, txReference);
        this.signature = signature;
    }

    // GETTERS/SETTERS

    @Override
    public boolean hasPublicText() {
        return false;
    }


    // PARSE/CONVERT

    public static Transaction Parse(byte[] data) throws Exception {

        // READ TYPE
        byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
        int position = TYPE_LENGTH;

        //READ REFERENCE
        byte[] referenceBytes = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
        long txReference = Longs.fromByteArray(referenceBytes);
        position += REFERENCE_LENGTH;

        ///////////////// LOAD

        // READ RECIPIENT
        byte[] recipientBytes = Arrays.copyOfRange(data, position, position + RECIPIENT_LENGTH);
        Account recipient = new Account(Base58.encode(recipientBytes));
        position += RECIPIENT_LENGTH;

        long key = 0;
        BigDecimal amount = null;
        if (typeBytes[2] >= 0) {
            // IF here is AMOUNT

            // READ KEY
            byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
            key = Longs.fromByteArray(keyBytes);
            position += KEY_LENGTH;

            // READ AMOUNT
            byte[] amountBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
            amount = new BigDecimal(new BigInteger(amountBytes), BlockChain.AMOUNT_DEDAULT_SCALE);
            position += AMOUNT_LENGTH;

            // CHECK ACCURACY of AMOUNT
            if (typeBytes[3] != -1) {
                // not use old FLAG from vers 2
                int accuracy = typeBytes[3] & SCALE_MASK;
                if (accuracy > 0) {
                    if (accuracy >= TransactionAmount.SCALE_MASK_HALF) {
                        accuracy -= TransactionAmount.SCALE_MASK + 1;
                    }

                    // RESCALE AMOUNT
                    amount = amount.scaleByPowerOfTen(-accuracy);
                }
            }

        }

        // MESSAGE LEN
        int messageLen = Byte.toUnsignedInt(data[position]);
        position++;
        // MESSAGE
        byte[] messageBytes = Arrays.copyOfRange(data, position, position + messageLen);
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        position += messageLen;

        return new R_Calculated(typeBytes, recipient, key, amount, message, txReference);

    }

    public String getMessage() {
        return this.message;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        // GET BASE
        JSONObject transaction = this.getJsonBase();

        if (message.length() > 0) {
            transaction.put("message", this.message);
        }

        return transaction;
    }

    @Override
    public byte[] toBytes(int forDeal, boolean withSignature) {

        byte[] data = new byte[0];

        // WRITE TYPE
        data = Bytes.concat(data, this.typeBytes);

        // WRITE REFERENCE
        byte[] referenceBytes = Longs.toByteArray(this.reference);
        referenceBytes = Bytes.ensureCapacity(referenceBytes, REFERENCE_LENGTH, 0);
        data = Bytes.concat(data, referenceBytes);

        // WRITE RECIPIENT
        data = Bytes.concat(data, Base58.decode(this.recipient.getAddress()));

        if (this.amount != null) {

            // WRITE KEY
            byte[] keyBytes = Longs.toByteArray(this.key);
            keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
            data = Bytes.concat(data, keyBytes);

            // CALCULATE ACCURACY of AMMOUNT
            int different_scale = this.amount.scale() - BlockChain.AMOUNT_DEDAULT_SCALE;
            BigDecimal amountBase;
            if (different_scale != 0) {
                // RESCALE AMOUNT
                amountBase = this.amount.scaleByPowerOfTen(different_scale);
                if (different_scale < 0)
                    different_scale += TransactionAmount.SCALE_MASK + 1;

                // WRITE ACCURACY of AMMOUNT
                data[3] = (byte) (data[3] | different_scale);
            } else {
                amountBase = this.amount;
            }

            // WRITE AMOUNT
            byte[] amountBytes = Longs.toByteArray(amountBase.unscaledValue().longValue());
            amountBytes = Bytes.ensureCapacity(amountBytes, AMOUNT_LENGTH, 0);
            data = Bytes.concat(data, amountBytes);
        }

        ///////////////////
        // WRITE HEAD
        byte[] messageBytes = this.message.getBytes(StandardCharsets.UTF_8);
        // HEAD SIZE
        data = Bytes.concat(data, new byte[]{(byte) messageBytes.length});
        // HEAD
        data = Bytes.concat(data, messageBytes);

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {

        return TYPE_LENGTH + REFERENCE_LENGTH + RECIPIENT_LENGTH + 1
                + message.getBytes(StandardCharsets.UTF_8).length;
    }

}