package org.erachain.rocksDB.transformation;

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionFactory;
import org.erachain.rocksDB.exceptions.WrongParseException;

@Slf4j
public class ByteableTransaction implements Byteable<Transaction> {

    @Override
    public Transaction receiveObjectFromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return TransactionFactory.getInstance().parse(bytes, Transaction.FOR_DB_RECORD);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new WrongParseException(e);
        }
    }


    @Override
    public byte[] toBytesObject(Transaction value) {
        return value.toBytes(Transaction.FOR_DB_RECORD, true);
    }
}
