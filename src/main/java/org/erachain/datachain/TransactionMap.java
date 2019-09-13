package org.erachain.datachain;

import org.erachain.core.transaction.Transaction;
import org.erachain.dbs.DCMap;

import java.util.*;

/**
 * Храним неподтвержденные транзакции - memory pool for unconfirmed transaction.
 * Signature (as Long) -> Transaction
 * <hr>
 * Здесь вторичные индексы создаются по несколько для одной записи путем создания массива ключей,
 * см. typeKey и recipientKey. Они используются для API RPC block explorer.
 * Нужно огрничивать размер выдаваемого списка чтобы не перегружать ноду.
 * <br>
 * Так же вторичный индекс по времени, который используется в ГУИ TIMESTAMP_INDEX = 0 (default)
 * - он оргнизыется внутри DCMap в списке индексов для сортировок в ГУИ
 *
 * Также хранит инфо каким пирам мы уже разослали транзакцию неподтвержденную так что бы при подключении делать автоматически broadcast
 *
 *  <hr>
 *  (!!!) для создания уникальных ключей НЕ нужно добавлять + val.viewTimestamp(), и так работант, а почему в Ордерах не работало?
 *  <br>в БИНДЕ внутри уникальные ключи создаются добавлением основного ключа
 */
public interface TransactionMap extends DCMap {

    int TIMESTAMP_INDEX = 1;

    Integer deleteObservableData(int index);

    Integer setObservableData(int index, Integer data);

    Iterator<Long> getTimestampIterator();

    Iterator<Long> getCeatorIterator();

    List<Transaction> getSubSet(long timestamp, boolean notSetDCSet, boolean cutDeadTime);

    void clearByDeadTimeAndLimit(long timestamp, boolean cutDeadTime);

    void update(Observable o, Object arg);

    boolean set(Long key, Transaction transaction);
    boolean set(byte[] signature, Transaction transaction);

    boolean add(Transaction transaction);

    void delete(Transaction transaction);
    Transaction delete(byte[] signature);
    Transaction delete(Long key);

    boolean contains(byte[] signature);
    boolean contains(Long key);
    boolean contains(Transaction transaction);

    Transaction get(byte[] signature);

    Collection<Long> getFromToKeys(long fromKey, long toKey);

    @SuppressWarnings({"rawtypes", "unchecked"})
    Iterable findTransactionsKeys(String address, String sender, String recipient,
                                  int type, boolean desc, int offset, int limit, long timestamp);

    List<Transaction> findTransactions(String address, String sender, String recipient,
                                       int type, boolean desc, int offset, int limit, long timestamp);

    List<Transaction> getUnconfirmedTransaction(Iterable keys);

    // TODO выдает ошибку на шаге treeKeys.addAll(Sets.newTreeSet(senderKeys));
    List<Transaction> getTransactionsByAddressFast100(String address);

    // slow?? without index
    List<Transaction> getTransactionsByAddress(String address);

    List<Transaction> getTransactions(int count, boolean descending);

    List<Transaction> getIncomedTransactions(String address, int type, long timestamp, int count, boolean descending);

    public void setTotalDeleted(int value);
    public int getTotalDeleted();

    int size();
    Iterator<Long> getIterator(int index, boolean descending);

    Collection<Transaction> getValues();

    void addObserver(Observer o);
    void deleteObserver(Observer o);

    void reset();

}
