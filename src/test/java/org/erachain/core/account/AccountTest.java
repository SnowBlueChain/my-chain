package org.erachain.core.account;

import org.erachain.core.BlockChain;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.transaction.GenesisTransferAssetTransaction;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.ItemAssetBalanceMap;
import org.junit.Test;
import org.mapdb.Fun;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class AccountTest {

    DCSet db;
    GenesisBlock gb;
    Account account;

    void init() {
        db = DCSet.createEmptyHardDatabaseSet(0);
        gb = new GenesisBlock();

        try {
            gb.process(db);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Test
    public void setLastTimestamp() {
        init();

        account = ((GenesisTransferAssetTransaction)gb.getTransactions().get(100)).getRecipient();
        long[] time = account.getLastTimestamp(db);

        // MAKE COPY hoe hasCode() check
        Account newAccount = new Account(account.getAddress());
        Fun.Tuple3<Integer, Integer, Integer> point = newAccount.getLastForgingData(db);
        assertEquals(point != null, true);
        long[] reference = newAccount.getLastTimestamp(db);
        assertEquals(reference != null, true);
    }

    @Test
    public void getBalance() {
        init();

        /// Tuple2[323980.18401546, 323980.18401546] //
        Account account = new Account("76ACGgH8c63VrrgEw1wQA4Dno1JuPLTsWe");

        Fun.Tuple5 balance5 = account.getBalance(db, Transaction.RIGHTS_KEY);
        assertEquals(((BigDecimal)((Fun.Tuple2)balance5.a).a).intValue(), 323980);
        assertEquals(((BigDecimal)((Fun.Tuple2)balance5.a).b).intValue(), 331417);

        Fun.Tuple2 balance = account.getBalance(db, Transaction.RIGHTS_KEY, 1);

        assertEquals(((BigDecimal)balance.a).intValue(), 323980);
        assertEquals(((BigDecimal)balance.b).intValue(), 331417);

        Fun.Tuple2 balance2 = account.getBalance(db, Transaction.RIGHTS_KEY, 1);

        assertEquals(((BigDecimal)balance2.a).intValue(), 323980);
        assertEquals(((BigDecimal)balance2.b).intValue(), 331417);

        ItemAssetBalanceMap map = db.getAssetBalanceMap();
        map.clear();

        Fun.Tuple2 balance3 = account.getBalance(db, Transaction.RIGHTS_KEY, 1);

        assertEquals(((BigDecimal) balance3.a).intValue(), 0);
        assertEquals(((BigDecimal) balance3.b).intValue(), BlockChain.ERA_COMPU_ALL_UP ?
                account.addDEVAmount(Transaction.RIGHTS_KEY).intValue() : 0);

    }
}