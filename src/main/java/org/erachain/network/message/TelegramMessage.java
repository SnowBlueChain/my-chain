package org.erachain.network.message;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionFactory;
import org.json.simple.JSONObject;

import java.util.Arrays;

public class TelegramMessage extends Message {

    private Transaction transaction;

    public TelegramMessage(Transaction transaction) {
        super(TELEGRAM_TYPE);
        this.transaction = transaction;
    }

    @Override
    public Long getHash() {
        return Longs.fromByteArray(this.transaction.getSignature());
    }

    public static TelegramMessage parse(byte[] data) throws Exception {
        //PARSE TRANSACTION
        int length = data.length;

        Transaction transaction = TransactionFactory.getInstance().parse(data, Transaction.FOR_NETWORK);

        return new TelegramMessage(transaction);
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public boolean isRequest() {
        return false;
    }

    public byte[] toBytes() {
        byte[] data = new byte[0];

        //WRITE BLOCK
        byte[] telegramBytes = this.transaction.toBytes(Transaction.FOR_NETWORK, true);
        data = Bytes.concat(data, telegramBytes);

        //ADD CHECKSUM
        data = Bytes.concat(super.toBytes(), this.generateChecksum(data), data);

        return data;
    }


    public TelegramMessage copy() {
        try {
            byte[] data = this.toBytes();
            int position = Message.MAGIC_LENGTH + TYPE_LENGTH + 1 + MESSAGE_LENGTH + CHECKSUM_LENGTH;
            data = Arrays.copyOfRange(data, position, data.length);
            return TelegramMessage.parse(data);
        } catch (Exception e) {
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    public JSONObject toJson() {

        //DCSet localDCSet = DCSet.getInstance();
        JSONObject telegram = new JSONObject();

        telegram.put("transaction", transaction.toJson());

        return telegram;

    }

    @Override
    public int getDataLength() {
        return this.transaction.getDataLength(Transaction.FOR_NETWORK, true);
    }

}
