package org.erachain.datachain;

import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.SimpleFileVisitorForRecursiveFolderDeletion;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.assertEquals;

@Slf4j
public class TransactionTabImplTest {
    int[] TESTED_DBS = new int[]{DCSet.DBS_ROCK_DB, DCSet.DBS_MAP_DB, DCSet.DBS_NATIVE_MAP
            ,DCSet.DBS_ROCK_DB};

    String testsPath = Settings.getInstance().getDataTempDir();
    DCSet dcSet;
    Account account1 = new Account("7CzxxwH7u9aQtx5iNHskLQjyJvybyKg8rF");
    Account account2 = new Account("73EotEbxvAo39tyugJSyL5nbcuMWs4aUpS");
    Account account3 = new Account("7Ca34FCVKEgVy7ZZqRB41Wzj3aSFvtfvDp");

    HashSet<Account> accounts = new HashSet();

    BigDecimal balA = new BigDecimal("0.1");
    BigDecimal balB = new BigDecimal("0.2");
    BigDecimal balC = new BigDecimal("3.0001");
    BigDecimal balD = new BigDecimal("0.005");

    TransactionTab map;

    private synchronized void init(int dbs) {

        logger.info("DBS_TEST: " + dbs);

        try {
            // NEED DELETE RocksDB file !!!
            ///File tempDir = new File(Settings.getInstance().getDataDir() + ConstantsRocksDB.ROCKS_DB_FOLDER);
            File tempDir = new File(testsPath);
            Files.walkFileTree(tempDir.toPath(), new SimpleFileVisitorForRecursiveFolderDeletion());
        } catch (Throwable e) {
        }

        dcSet = DCSet.createEmptyHardDatabaseSetWithFlush(testsPath, dbs);
        map = dcSet.getTransactionTab();

        accounts.add(account1);
        accounts.add(account2);
        accounts.add(account3);

    }

    public void make() {
        Random random = new Random();

        int max = 10;
        int counter = max;
        List<PrivateKeyAccount> test2Creators = new ArrayList<>();
        while (counter-- > 0) {
            test2Creators.add(new PrivateKeyAccount(Crypto.getInstance()
                    .digest(Longs.toByteArray(random.nextLong()))));
        }

        long timestamp = NTP.getTime();

        int records = max << 2;
        counter = records;
        do {

            try {

                PrivateKeyAccount creator = test2Creators.get(random.nextInt(test2Creators.size()));
                Account recipient;
                do {
                    recipient = test2Creators.get(random.nextInt(test2Creators.size()));
                } while (recipient.equals(creator));

                BigDecimal amount = new BigDecimal(counter + "." + counter);

                String address = creator.getAddress();
                Transaction messageTx = new RSend(creator, (byte) 0, recipient, 1L, amount,
                        "title test", null, new byte[]{(byte) 1},
                        new byte[]{(byte) 0}, timestamp + random.nextInt(10000), 0l);
                messageTx.sign(creator, Transaction.FOR_NETWORK);

                map.add(messageTx);

            } catch (Exception e10) {
            }

        } while (--counter > 0);

        assertEquals(map.size(), records);
    }

    @Test
    public void getFromToKeys() {
        for (int dbs :
                //TESTED_DBS
                new int[]{DCSet.DBS_MAP_DB}
                ) {
            init(dbs);

            make();

            map.clear();
            assertEquals(map.size(), 0);

            make();

            dcSet.close();
        }
    }

    @Test
    public void getTimestampIterator() {
        for (int dbs : TESTED_DBS) {
            init(dbs);

            make();

            long ntp = NTP.getTime();
            long timestampTMP = 0;
            Iterator<Long> iterator = map.getTimestampIterator(descending);
            while (iterator.hasNext()) {
                Long key = iterator.next();
                Transaction item = map.get(key);
                long timestamp = item.getTimestamp();
                if (timestampTMP > 0 && timestampTMP > timestamp) {
                    logger.error((timestampTMP - ntp) + " err : " + (timestamp - ntp));
                    assertEquals(timestampTMP, timestamp);
                }
                timestampTMP = timestamp;

            }

            dcSet.close();
        }
    }

    @Test
    public void findTransactionsKeys() {
    }

    @Test
    public void findTransactions() {
    }

    @Test
    public void getUnconfirmedTransaction() {
    }

    @Test
    public void getTransactionsByAddressFast100() {
    }

    @Test
    public void getTransactionsByAddress() {
    }

    @Test
    public void getTransactions() {
    }

    @Test
    public void getIncomedTransactions() {
    }
}