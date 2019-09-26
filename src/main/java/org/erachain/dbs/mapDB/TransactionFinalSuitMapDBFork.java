package org.erachain.dbs.mapDB;

//04/01 +- 

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.DBASet;
import org.erachain.database.serializer.TransactionSerializer;
import org.erachain.datachain.TransactionFinalMap;
import org.erachain.datachain.TransactionFinalSuit;
import org.mapdb.BTreeKeySerializer.BasicKeySerializer;
import org.mapdb.BTreeMap;

import java.util.Iterator;

//import java.math.BigDecimal;

/**
 * Транзакции занесенные в цепочку
 * <p>
 * block.id + tx.ID in this block -> transaction
 * * <hr>
 * Здесь вторичные индексы создаются по несколько для одной записи путем создания массива ключей,
 * см. typeKey и recipientKey. Они используются для API RPC block explorer.
 * Нужно огрничивать размер выдаваемого списка чтобы не перегружать ноду.
 * <br>
 * Вторичные ключи:
 * ++ senderKey
 * ++ recipientKey
 * ++ typeKey
 * <hr>
 * (!!!) для создания уникальных ключей НЕ нужно добавлять + val.viewTimestamp(), и так работант, а почему в Ордерах не работало?
 * <br>в БИНДЕ внутри уникальные ключи создаются добавлением основного ключа
 */
@Slf4j
public class TransactionFinalSuitMapDBFork extends DBMapSuitFork<Long, Transaction>
        implements TransactionFinalSuit {

    public TransactionFinalSuitMapDBFork(TransactionFinalMap parent, DBASet databaseSet) {
        super(parent, databaseSet, logger);
    }

    @Override
    protected void getMap() {
        // OPEN MAP
        // TREE MAP for sortable search
        map = database.createTreeMap("height_seq_transactions")
                .keySerializer(BasicKeySerializer.BASIC)
                //.keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(new TransactionSerializer())
                .counterEnable()
                .makeOrGet();

    }

    @Override
    protected Transaction getDefaultValue() {
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterator<Long> getBlockIterator(Integer height) {
        // GET ALL TRANSACTIONS THAT BELONG TO THAT ADDRESS
         return  ((BTreeMap<Long, Transaction>) map)
                .subMap(Transaction.makeDBRef(height, 0),
                        Transaction.makeDBRef(height, Integer.MAX_VALUE)).keySet().iterator();

    }

    @Override
    public Iterator<Long> getIteratorByRecipient(String address) {
        return null;
    }

    @Override
    public Iterator<Long> getIteratorBySender(String address) {
        return null;
    }

    @Override
    public Iterator<Long> getIteratorByAddressAndType(String address, Integer type) {
        return null;
    }

    @Override
    public Iterator<Long> getIteratorByTitleAndType(String filter, boolean asFilter, Integer type) {
        return null;
    }

    @Override
    public Iterator<Long> getIteratorByAddress(String address) {
        return null;
    }

    @Override
    public Iterator findTransactionsKeys(String address, String sender, String recipient, final int minHeight,
                                         final int maxHeight, int type, final int service, boolean desc, int offset, int limit) {
        return null;
    }

}
