package org.erachain.database.wallet;

import org.erachain.core.account.Account;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.DBMap;
import org.erachain.database.serializer.TransactionSerializer;
import org.erachain.datachain.DCSet;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.Pair;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// memory pool for unconfirmed transaction
// tx.signature -> <<broadcasted peers>, transaction>
// ++ seek by TIMESTAMP
// тут надо запминать каким пирам мы уже разослали транзакцию неподтвержденную
// так что бы при подключении делать автоматически broadcast

public class FavoriteDocument extends DBMap<Tuple2<String, String>, Transaction> implements Observer {

    static Logger LOGGER = LoggerFactory.getLogger(TransactionMap.class.getName());

    public FavoriteDocument(DWSet dWSet, DB database) {
        super(dWSet, database);

        //	DCSet.getInstance().getTransactionFinalMap().addObserver(this);
        this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.RESET_STATEMENT_FAVORITES_TYPE);
        this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.ADD_STATEMENT_FAVORITES_TYPE);
        this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.DELETE_STATEMENT_FAVORITES_TYPE);
        this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.LIST_STATEMENT_FAVORITES_TYPE);
    }

    @SuppressWarnings({})
    protected void createIndexes(DB database) {

    }

    @Override
    protected Map<Tuple2<String, String>, Transaction> getMap(DB database) {
        // OPEN MAP
        return database.createTreeMap("Documents_Favorites").keySerializer(BTreeKeySerializer.TUPLE2)
                .valueSerializer(new TransactionSerializer()).counterEnable().makeOrGet();
    }

    @Override
    protected Map<Tuple2<String, String>, Transaction> getMemoryMap() {
        return new TreeMap<Tuple2<String, String>, Transaction>(Fun.TUPLE2_COMPARATOR);
    }

    @Override
    protected Transaction getDefaultValue() {
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Transaction> get(Account account, int limit) {
        List<Transaction> transactions = new ArrayList<Transaction>();

        try {
            // GET ALL TRANSACTIONS THAT BELONG TO THAT ADDRESS
            /*
             * Map<Tuple2<String, String>, Transaction> accountTransactions =
             * ((BTreeMap) this.map).subMap( Fun.t2(null, account.getAddress()),
             * Fun.t2(Fun.HI(), account.getAddress()));
             */

            Map<Tuple2<String, String>, Transaction> accountTransactions = ((BTreeMap) this.map)
                    .subMap(Fun.t2(account.getAddress(), null), Fun.t2(account.getAddress(), Fun.HI()));

            // GET ITERATOR
            Iterator<Transaction> iterator = accountTransactions.values().iterator();

            // RETURN {LIMIT} TRANSACTIONS
            int counter = 0;
            while (iterator.hasNext() && counter < limit) {
                transactions.add(iterator.next());
                counter++;
            }
        } catch (Exception e) {
            // ERROR
            LOGGER.error(e.getMessage(), e);
        }

        return transactions;
    }

    public List<Pair<Account, Transaction>> get(List<Account> accounts, int limit) {
        List<Pair<Account, Transaction>> transactions = new ArrayList<Pair<Account, Transaction>>();

        try {
            // FOR EACH ACCOUNTS
            synchronized (accounts) {
                for (Account account : accounts) {
                    List<Transaction> accountTransactions = get(account, limit);
                    for (Transaction transaction : accountTransactions) {
                        transactions.add(new Pair<Account, Transaction>(account, transaction));
                    }
                }
            }
        } catch (Exception e) {
            // ERROR
            LOGGER.error(e.getMessage(), e);
        }

        return transactions;
    }

    public void delete(Transaction transaction) {
        this.delete(new Tuple2<String, String>("", new String(transaction.getSignature())));
    }

    public boolean add(Transaction transaction) {
        return this.set(new Tuple2<String, String>("", new String(transaction.getSignature())), transaction);
    }

    public boolean contains(Transaction transaction) {

        return this.map.containsValue(transaction);

    }

    @Override
    public void update(Observable arg0, Object arg1) {
        // TODO Auto-generated method stub
        ObserverMessage message = (ObserverMessage) arg1;

        // chech remove transaction from FinalMap and Favorite Map
        //if (message.getType() == ObserverMessage.REMOVE_TRANSACTION_TYPE) {
        // if(this.contains((Transaction)message.getValue()))
        // this.delete((Transaction)message.getValue());
        //}

    }

    public void removeObserver_FinalMap() {
        DCSet.getInstance().getTransactionFinalMap().deleteObserver(this);

    }

}

