package org.erachain.database.wallet;
//09/03

import com.google.common.primitives.Longs;
import org.erachain.core.account.Account;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.AutoKeyDBMap;
import org.erachain.database.DBMap;
import org.erachain.database.serializer.LongAndTransactionSerializer;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.Pair;
import org.erachain.utils.ReverseComparator;
import org.mapdb.*;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;

/**
 * Транзакции относящиеся к моим счетам. Сюда же записываться должны и неподтвержденные<br>
 * А когда они подтверждаются они будут перезаписываться поверх.
 * Тогда неподтвержденные будут показывать что они не сиполнились.
 * И их пользователь сможет сам удалить вручную или командой - удалить все неподтвержденные.
 * Вообще тут реализация как СТЕК - удалить можно только если это верхний элемент.
 * Добавление вверх или обновляем существующий по AUTOKEY_INDEX
 * <hr>
 * Ключ: первых байт счета + время создания<br>
 * Значение: транзакция
 */
public class TransactionMap extends AutoKeyDBMap<Tuple2<Long, Long>, Tuple2<Long, Transaction>> {

    public static final int TIMESTAMP_INDEX = 1;
    //public static final int ADDRESS_INDEX = 2;
    //public static final int AMOUNT_INDEX = 3;

    static Logger LOGGER = LoggerFactory.getLogger(TransactionMap.class.getName());

    public TransactionMap(DWSet dWSet, DB database) {
        super(dWSet, database);

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.WALLET_RESET_TRANSACTION_TYPE);
            this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.WALLET_LIST_TRANSACTION_TYPE);
            this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.WALLET_ADD_TRANSACTION_TYPE);
            this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.WALLET_REMOVE_TRANSACTION_TYPE);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void createIndexes(DB database) {
        //TIMESTAMP INDEX
        NavigableSet<Tuple2<Long, Tuple2<Long, Long>>> timestampIndex = database.createTreeSet("transactions_index_timestamp")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        NavigableSet<Tuple2<Long, Tuple2<Long, Long>>> descendingTimestampIndex = database.createTreeSet("transactions_index_timestamp_descending")
                .comparator(new ReverseComparator(Fun.COMPARATOR))
                .makeOrGet();

        createIndex(TIMESTAMP_INDEX, timestampIndex, descendingTimestampIndex, new Fun.Function2<Long, Tuple2<Long, Long>, Tuple2<Long, Transaction>>() {
            @Override
            public Long run(Tuple2<Long, Long> key, Tuple2<Long, Transaction> value) {
                return value.b.getTimestamp();
            }
        });

        /* - это как основной индекс можно брать
        //ADDRESS INDEX
        NavigableSet<Tuple2<String, Tuple2<Long, Long>>> addressIndex = database.createTreeSet("transactions_index_address")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        NavigableSet<Tuple2<String, Tuple2<Long, Long>>> descendingAddressIndex = database.createTreeSet("transactions_index_address_descending")
                .comparator(new ReverseComparator(Fun.COMPARATOR))
                .makeOrGet();

        createIndex(ADDRESS_INDEX, addressIndex, descendingAddressIndex, new Fun.Function2<String, Tuple2<Long, Long>, Tuple2<Long, Transaction>>() {
            @Override
            public String run(Tuple2<Long, Long> key, Tuple2<Long, Transaction> value) {
                return key.b;
            }
        });
        */

        /* это вообще не информативнй индекс не нужен
        //AMOUNT INDEX
        NavigableSet<Tuple2<BigDecimal, Tuple2<Long, Long>>> amountIndex = database.createTreeSet("transactions_index_amount")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        NavigableSet<Tuple2<BigDecimal, Tuple2<Long, Long>>> descendingAmountIndex = database.createTreeSet("transactions_index_amount_descending")
                .comparator(new ReverseComparator(Fun.COMPARATOR))
                .makeOrGet();

        createIndex(AMOUNT_INDEX, amountIndex, descendingAmountIndex, new Fun.Function2<BigDecimal, Tuple2<Long, Long>, Tuple2<Long, Transaction>>() {
            @Override
            public BigDecimal run(Tuple2<Long, Long> key, Tuple2<Long, Transaction> value) {
                Account account = new Account(key.a);
                return value.b.getAmount(account);
            }
        });
        */

    }

    @Override
    protected Map<Tuple2<Long, Long>, Tuple2<Long, Transaction>> getMap(DB database) {
        //OPEN MAP
        BTreeMap map = database.createTreeMap("transactions")
                .keySerializer(BTreeKeySerializer.TUPLE2)
                .valueSerializer(new LongAndTransactionSerializer())
                .counterEnable()
                .makeOrGet();

        makeAutoKey(database, map, "dw_transactions");

        return map;
    }

    @Override
    protected Map<Tuple2<Long, Long>, Tuple2<Long, Transaction>> getMemoryMap() {
        return new TreeMap<Tuple2<Long, Long>, Tuple2<Long, Transaction>>(Fun.TUPLE2_COMPARATOR);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Transaction> get(Account account, int limit) {
        List<Transaction> transactions = new ArrayList<Transaction>();

        try {
            //GET ALL TRANSACTIONS THAT BELONG TO THAT ADDRESS
			/*Map<Tuple2<Long, Long>, Tuple2<Long, Transaction>> accountTransactions = ((BTreeMap) this.map).subMap(
					Fun.t2(null, Longs.fromByteArray(account.getShortAddressBytes())),
					Fun.t2(Fun.HI(), Longs.fromByteArray(account.getShortAddressBytes())));*/

            Map<Tuple2<Long, Long>, Tuple2<Long, Transaction>> accountTransactions = ((BTreeMap) this.map).subMap(
                    Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), null),
                    Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), Fun.HI()));

            //GET ITERATOR
            Iterator<Tuple2<Long, Transaction>> iterator = accountTransactions.values().iterator();

            //RETURN {LIMIT} TRANSACTIONS
            int counter = 0;
            while (iterator.hasNext() && counter < limit) {
                transactions.add(iterator.next().b);
                counter++;
            }
        } catch (Exception e) {
            //ERROR
            LOGGER.error(e.getMessage(), e);
        }

        return transactions;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterator<Tuple2<Long, Long>> getAddressIterator(Account account) {

        Set<Tuple2<Long, Long>> accountKeys = ((BTreeMap) this.map).subMap(
                Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), null),
                Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), Fun.HI())).keySet();

        return accountKeys.iterator();
    }

    public List<Pair<Account, Transaction>> get(List<Account> accounts, int limit) {
        List<Pair<Account, Transaction>> transactions = new ArrayList<Pair<Account, Transaction>>();

        try {
            //FOR EACH ACCOUNTS
            synchronized (accounts) {
                for (Account account : accounts) {
                    List<Transaction> accountTransactions = get(account, limit);
                    for (Transaction transaction: accountTransactions) {
                        transactions.add(new Pair<Account, Transaction>(account, transaction));
                    }
                }
            }
        } catch (Exception e) {
            //ERROR
            LOGGER.error(e.getMessage(), e);
        }

        return transactions;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void delete(Account account) {
        //GET ALL TRANSACTIONS THAT BELONG TO THAT ADDRESS
        Map<Tuple2<Long, Long>, Tuple2<Long, Transaction>> accountTransactions = ((BTreeMap) this.map).subMap(
                Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), null),
                Fun.t2(Longs.fromByteArray(account.getShortAddressBytes()), Fun.HI()));

        //DELETE TRANSACTIONS
        for (Tuple2<Long, Long> key : accountTransactions.keySet()) {
            this.delete(key);
        }
    }

    public void delete(Account account, Transaction transaction) {
        ////this.delete(new Tuple2<Long, Long>(account.getAddress(), new String(transaction.getSignature()).substring(KEY_LENGHT)));
        this.delete(new Tuple2<Long, Long>(Longs.fromByteArray(account.getShortAddressBytes()), transaction.getTimestamp()));
    }

    public void deleteAll(List<Account> accounts) {
        for (Account account : accounts) {
            this.delete(account);
        }
    }

    public boolean add(Account account, Transaction transaction) {
        return this.set(new Tuple2<Long, Long>(Longs.fromByteArray(account.getShortAddressBytes()), transaction.getTimestamp()),
                new Tuple2<>(null, transaction));
    }

    public void addAll(Map<Account, List<Transaction>> transactions) {
        //FOR EACH ACCOUNT
        for (Account account : transactions.keySet()) {
            //FOR EACH TRANSACTION
            for (Transaction transaction : transactions.get(account)) {
                this.add(account, transaction);
            }
        }
    }
}
