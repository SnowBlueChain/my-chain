package org.erachain.core.account;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.block.Block;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.ItemAssetBalanceMap;
import org.erachain.datachain.OrderMapImpl;
import org.erachain.datachain.ReferenceMapImpl;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.lang.Lang;
import org.erachain.utils.NumberAsString;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple4;
import org.mapdb.Fun.Tuple5;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

//import org.erachain.core.crypto.Base64;

//04/01 +-

/**
 * обработка ключей и криптографии
 */
public class Account {

    public static final int ADDRESS_SHORT_LENGTH = 20;
    public static final int ADDRESS_LENGTH = 25;
    // private static final long ERA_KEY = Transaction.RIGHTS_KEY;
    ///private static final long FEE_KEY = Transaction.FEE_KEY;
    // public static final long ALIVE_KEY = StatusCls.ALIVE_KEY;
    // public static String EMPTY_PUBLICK_ADDRESS = new PublicKeyAccount(new
    // byte[PublicKeyAccount.PUBLIC_KEY_LENGTH]).getAddress();

    protected String address;
    protected byte[] bytes;
    protected byte[] shortBytes;
    // private long generatingBalance; //used for forging balance
    // нельзя тут запминать так как при откате данные не будут очищены Tuple4<Long, Integer, Integer, Integer> personDuration;
    Tuple2<Integer, PersonCls> person;
    int viewBalancePosition = 0;

    public Account(String address) {
        this.bytes = Base58.decode(address);
        this.shortBytes = Arrays.copyOfRange(this.bytes, 1, this.bytes.length - 4);
        this.address = address;
    }

    public Account(byte[] addressBytes) {
        if (addressBytes.length == ADDRESS_SHORT_LENGTH) {
            // AS SHORT BYTES
            this.shortBytes = addressBytes;
            this.bytes = Crypto.getInstance().getAddressFromShortBytes(addressBytes);
        } else if (addressBytes.length == ADDRESS_LENGTH) {
            // AS FULL 25 byres
            this.bytes = addressBytes;
            this.shortBytes = Arrays.copyOfRange(addressBytes, 1, this.bytes.length - 4);

        } else {
            assert(addressBytes.length == ADDRESS_LENGTH);
        }

        /// make on demand this.address = Base58.encode(bytes);
    }

    public static byte[] makeShortBytes(String address) {
        return Arrays.copyOfRange(Base58.decode(address), 1, ADDRESS_LENGTH - 4);

    }
    public static Account makeAccountFromShort(byte[] addressShort) {

        String address = Crypto.getInstance().getAddressFromShort(addressShort);
        return new Account(address);
    }

    public static Account makeAccountFromShort(BigInteger addressShort) {

        String address = Crypto.getInstance().getAddressFromShort(addressShort.toByteArray());
        return new Account(address);
    }

    public static Tuple2<Account, String> tryMakeAccount(String address) {

        if (address == null || address.length() < ADDRESS_LENGTH)
            return new Tuple2<Account, String>(null, "Wrong Address or PublicKey");

        if (address.startsWith("+")) {
            if (PublicKeyAccount.isValidPublicKey(address)) {
                // MAY BE IT BASE.32 +
                return new Tuple2<Account, String>(new PublicKeyAccount(address), null);
            } else {
                return new Tuple2<Account, String>(null, "Wrong Address or PublicKey");
            }
        }

        boolean isBase58 = !Base58.isExtraSymbols(address);

        if (isBase58) {
            // ORDINARY RECIPIENT
            if (Crypto.getInstance().isValidAddress(address)) {
                return new Tuple2<Account, String>(new Account(address), null);
            } else if (PublicKeyAccount.isValidPublicKey(address)) {
                return new Tuple2<Account, String>(new PublicKeyAccount(address), null);
            } else {
                return new Tuple2<Account, String>(null, "Wrong Address or PublicKey");
            }
        } else {
            return new Tuple2<Account, String>(null, "The name is not registered");
        }

    }

    // make TYPE of transactionAmount by signs of KEY and AMOUNT
    public static int balancePosition(long key, BigDecimal amount, boolean isBackward) {
        if (key == 0l || amount == null || amount.signum() == 0)
            return 0;

        int type;
        int amount_sign = amount.signum();
        if (key > 0) {
            if (amount_sign > 0) {
                // OWN SEND or PLEDGE
                type = isBackward ? TransactionAmount.ACTION_PLEDGE : TransactionAmount.ACTION_SEND;
            } else {
                // HOLD in STOCK or PLEDGE
                type = isBackward ? TransactionAmount.ACTION_HOLD : TransactionAmount.ACTION_RESERCED_6;
            }
        } else {
            if (amount_sign > 0) {
                // give CREDIT or BORROW CREDIT
                type = TransactionAmount.ACTION_DEBT;
            } else {
                // PRODUCE or SPEND
                type = TransactionAmount.ACTION_SPEND;
            }
        }

        return type;

    }

    public static String getDetailsForEncrypt(String address, long itemKey, boolean forEncrypt) {

        if (address.isEmpty()) {
            return "";
        }

        // CHECK IF RECIPIENT IS VALID ADDRESS
        if (Crypto.getInstance().isValidAddress(address)) {
            if (forEncrypt && null == Controller.getInstance().getPublicKeyByAddress(address)) {
                return "address is unknown - cant't encrypt for it, please use public key instead";
            }
            if (itemKey > 0) {
                Account account = new Account(address);
                if (account.isPerson()) {
                    return account.getPerson().b.toString() + " " + account.getBalance(itemKey).a.b.toPlainString();
                }
                return " + " + account.getBalance(itemKey).a.b.toPlainString();
            }
            return "address is OK";
        } else {
            // Base58 string len = 33-34 for ADDRESS and 40-44 for PubKey
            if (PublicKeyAccount.isValidPublicKey(address)) {
                if (itemKey > 0) {
                    Account account = new PublicKeyAccount(address);
                    if (account.isPerson()) {
                        return account.getPerson().b.toString() + " " + account.getBalance(itemKey).a.b.toPlainString();
                    }
                    return " + " + account.getBalance(itemKey).a.b.toPlainString();
                }
                return "public key is OK";
            } else {
                return "address or public key is invalid";
            }
        }

    }

    public static String getDetails(String address, long assetKey) {

        String out = "";

        if (address.isEmpty()) {
            return out;
        }

        boolean statusBad = Controller.getInstance().getStatus() != Controller.STATUS_OK;

        Account account = null;

        // CHECK IF RECIPIENT IS VALID ADDRESS
        if (Crypto.getInstance().isValidAddress(address)) {
            account = new Account(address);
        } else {
            if (PublicKeyAccount.isValidPublicKey(address)) {
                account = new PublicKeyAccount(address);
            } else {
                return (statusBad ? "??? " : "") + "ERROR";
            }
        }

        if (account.getBalanceUSE(assetKey).compareTo(BigDecimal.ZERO) == 0
                && account.getBalanceUSE(Transaction.FEE_KEY).compareTo(BigDecimal.ZERO) == 0) {
            return Lang.getInstance().translate("Warning!") + " " + (statusBad ? "???" : "")
                    + account.toString(assetKey);
        } else {
            return (statusBad ? "???" : "") + account.toString(assetKey);
        }

    }

    public static String getDetails(String address, AssetCls asset) {
        return getDetails(address, asset.getKey());
    }


    public static Map<byte[], BigDecimal> getKeyBalancesWithForks(DCSet dcSet, long key,
                                                                  Map<byte[], BigDecimal> values) {
        ItemAssetBalanceMap map = dcSet.getAssetBalanceMap();

        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> ballance;

        if (true) {
            // здесь нужен протокольный итератор! Берем TIMESTAMP_INDEX
            for (byte[] mapKey : map.keySet()) {
                if (ItemAssetBalanceMap.getAssetKeyFromKey(mapKey) == key) {
                    ballance = map.get(mapKey);
                    values.put(ItemAssetBalanceMap.getShortAccountFromKey(mapKey), ballance.a.b);
                }
            }

        } else {

            // здесь нужен протокольный итератор! его нету у балансов поэтому через перебор ключей
            try (IteratorCloseable<byte[]> iterator = map.getIterator(0, true)) {

                byte[] bytesKey;
                while (iterator.hasNext()) {
                    bytesKey = iterator.next();
                    if (ItemAssetBalanceMap.getAssetKeyFromKey(bytesKey) == key) {
                        ballance = map.get(bytesKey);
                        values.put(ItemAssetBalanceMap.getShortAccountFromKey(bytesKey), ballance.a.b);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        DCSet dcParent = dcSet.getParent();
        if (dcParent != null) {
            values = getKeyBalancesWithForks(dcParent, key, values);
        }

        return values;

    }

    public static Map<byte[], BigDecimal> getKeyOrdersWithForks(DCSet dcSet, long key, Map<byte[], BigDecimal> values) {

        OrderMapImpl map = dcSet.getOrderMap();
        Order order;
        try (IteratorCloseable<Long> iterator = map.getIterator(0, true)) {
            while (iterator.hasNext()) {
                order = map.get(iterator.next());
                if (order.getHaveAssetKey() == key) {
                    byte[] address = order.getCreator().getShortAddressBytes();
                    values.put(address, values.get(address).add(order.getAmountHave()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        DCSet dcParent = dcSet.getParent();
        if (dcParent != null) {
            values = getKeyOrdersWithForks(dcParent, key, values);
        }

        return values;

    }

    // top balance + orders values
    public static byte[] getRichWithForks(DCSet dcSet, long key) {

        Map<byte[], BigDecimal> values = new TreeMap<byte[], BigDecimal>();

        values = getKeyBalancesWithForks(dcSet, key, values);

        // add ORDER values
        values = getKeyOrdersWithForks(dcSet, key, values);

        // search richest address
        byte[] rich = null;
        BigDecimal maxValue = BigDecimal.ZERO;
        for (Map.Entry<byte[], BigDecimal> entry : values.entrySet()) {
            BigDecimal value = entry.getValue();
            if (value.compareTo(maxValue) > 0) {
                maxValue = value;
                rich = entry.getKey();
            }
        }

        return rich;

    }

    /*
     * public BigDecimal getBalance(long key) { if (key < 0) key = -key; return
     * this.getBalance(key, DBSet.getInstance()); } public BigDecimal
     * getBalance(long key, DBSet db) { int type = 1; // OWN if (key < 0) { type
     * = 2; // RENT key = -key; } Tuple3<BigDecimal, BigDecimal, BigDecimal>
     * balance = db.getAssetBalanceMap().get(getAddress(), key);
     *
     * if (type == 1) return balance.a; else if (type == 2) return balance.b;
     * else return balance.c; }
     *
     * public Integer setConfirmedPersonStatus(long personKey, long statusKey,
     * int end_date, DBSet db) { return
     * db.getPersonStatusMap().addItem(personKey, statusKey, end_date); }
     */

    // SET
    /*
     * public void setConfirmedBalance(BigDecimal amount) {
     * this.setConfirmedBalance(amount, DBSet.getInstance()); } public void
     * setConfirmedBalance(BigDecimal amount, DBSet db) { //UPDATE BALANCE IN DB
     * db.getAssetBalanceMap().set(getAddress(), Transaction.FEE_KEY, amount); }
     * // public void setBalance(long key, BigDecimal balance) {
     * this.setBalance(key, balance, DBSet.getInstance()); }
     *
     * // TODO in_OWN in_RENT on_HOLD public void setBalance(long key,
     * BigDecimal balance, DBSet db) {
     *
     * int type = 1; if (key < 0) { key = -key; type = 2; }
     *
     * Tuple3<BigDecimal, BigDecimal, BigDecimal> value =
     * db.getAssetBalanceMap().get(getAddress(), key); //UPDATE BALANCE IN DB if
     * (type == 1) { value = new Tuple3<BigDecimal, BigDecimal,
     * BigDecimal>(balance, value.b, value.c); } else { // SET RENT balance
     * value = new Tuple3<BigDecimal, BigDecimal, BigDecimal>(value.a, balance,
     * value.c); } db.getAssetBalanceMap().set(getAddress(), key, value); }
     */

    public String getAddress() {
        if (address == null) {
            this.address = Base58.encode(bytes);
        }
        return address;
    }

    public void setViewBalancePosition(int viewBalancePosition) {
        this.viewBalancePosition = viewBalancePosition;
    }

    public byte[] getAddressBytes() {
        return bytes;
    }

    public byte[] getShortAddressBytes() {
        return this.shortBytes;
    }

    // BALANCE
    public Tuple3<BigDecimal, BigDecimal, BigDecimal> getUnconfirmedBalance(long key) {
        return Controller.getInstance().getWalletUnconfirmedBalance(this, key);
    }

    /*
     * public BigDecimal getConfirmedBalance() { return
     * this.getConfirmedBalance(DBSet.getInstance()); } public BigDecimal
     * getConfirmedBalance(DBSet db) { return
     * db.getAssetBalanceMap().get(getAddress(), Transaction.FEE_KEY); }
     */
    public BigDecimal getBalanceUSE(long key) {
        return this.getBalanceUSE(key, DCSet.getInstance());
    }

    /**
     *
     * @param key asset key (long)
     * @param db database Set
     * @return (BigDecimal) balance.a + balance.b
     */
    public BigDecimal getBalanceUSE(long key, DCSet db) {
        if (key < 0)
            key = -key;
        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance = this
                .getBalance(db, key);

        return balance.a.b.add(balance.b.b);
    }

    /*
     * public void setBalance3(long key, Tuple3<BigDecimal, BigDecimal,
     * BigDecimal> balance, DBSet db) { if (key < 0) key = -key;
     *
     * db.getAssetBalanceMap().set(getAddress(), key, balance); }
     *
     * public void addBalanceOWN(long key, BigDecimal value, DBSet db) {
     * Tuple3<BigDecimal, BigDecimal, BigDecimal> balance =
     * this.getBalance3(key, db); Tuple3<BigDecimal, BigDecimal, BigDecimal>
     * balance_new = new Tuple3<BigDecimal, BigDecimal,
     * BigDecimal>(balance.a.add(value), balance.b, balance.c);
     *
     * this.setBalance3(key, balance_new, db); }
     */

    // STATUS
    /*
     * public void setConfirmedPersonStatus(long personKey, long statusKey,
     * Integer days) { this.setConfirmedPersonStatus(personKey, statusKey, days,
     * DBSet.getInstance()); }
     *
     * public void setConfirmedPersonStatus(long personKey, long statusKey,
     * Integer days, DBSet db) { //UPDATE PRIMARY TIME IN DB
     * db.getPersonStatusMap().set(personKey, statusKey, days); }
     */

    /**
     * позиция баланса предустанавливается - нужно для Сравнителей - utils.AccountBalanceComparator#compare
     * @param key
     * @return
     */
    public Tuple2<BigDecimal, BigDecimal> getBalanceInSettedPosition(long key) {
        return getBalanceInPosition(DCSet.getInstance(), key, this.viewBalancePosition);
    }

    /**
     * в заданной позиции баланс взять
     * @param key
     * @param position
     * @return
     */
    public Tuple2<BigDecimal, BigDecimal> getBalanceInPosition(long key, int position) {
        return getBalanceInPosition(DCSet.getInstance(), key, position);
    }

    public Tuple2<BigDecimal, BigDecimal> getBalanceInPosition(DCSet dcSet, long key, int position) {
        switch (position) {
            case TransactionAmount.ACTION_SEND:
                return this.getBalance(dcSet, key).a;
            case TransactionAmount.ACTION_DEBT:
            case TransactionAmount.ACTION_REPAY_DEBT:
                return this.getBalance(dcSet, key).b;
            case TransactionAmount.ACTION_HOLD:
                return this.getBalance(dcSet, key).c;
            case TransactionAmount.ACTION_SPEND:
                return this.getBalance(dcSet, key).d;
            case TransactionAmount.ACTION_PLEDGE:
                return this.getBalance(dcSet, key).e;
            case TransactionAmount.ACTION_RESERCED_6:
                return new Tuple2<>(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return null;
    }

    public static BigDecimal balanceInPositionAndSide(Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance,
                                                      int position, int side) {
        switch (position) {
            case TransactionAmount.ACTION_SEND:
                switch (side) {
                    case TransactionAmount.BALANCE_SIDE_CREDIT:
                        return balance.a.a;
                    case TransactionAmount.BALANCE_SIDE_LEFT:
                        return balance.a.b;
                    case TransactionAmount.BALANCE_SIDE_DEBIT:
                        return balance.a.a.subtract(balance.a.b);
                }
            case TransactionAmount.ACTION_DEBT:
                switch (side) {
                    case TransactionAmount.BALANCE_SIDE_CREDIT:
                        return balance.b.a;
                    case TransactionAmount.BALANCE_SIDE_LEFT:
                        return balance.b.b;
                    case TransactionAmount.BALANCE_SIDE_DEBIT:
                        return balance.b.a.subtract(balance.b.b);
                }
            case TransactionAmount.ACTION_HOLD:
                switch (side) {
                    case TransactionAmount.BALANCE_SIDE_CREDIT:
                        return balance.c.a;
                    case TransactionAmount.BALANCE_SIDE_LEFT:
                        return balance.c.b;
                    case TransactionAmount.BALANCE_SIDE_DEBIT:
                        return balance.c.a.subtract(balance.c.b);
                }
            case TransactionAmount.ACTION_SPEND:
                switch (side) {
                    case TransactionAmount.BALANCE_SIDE_CREDIT:
                        return balance.d.a;
                    case TransactionAmount.BALANCE_SIDE_LEFT:
                        return balance.d.b;
                    case TransactionAmount.BALANCE_SIDE_DEBIT:
                        return balance.d.a.subtract(balance.d.b);
                }
            case TransactionAmount.ACTION_PLEDGE:
                switch (side) {
                    case TransactionAmount.BALANCE_SIDE_CREDIT:
                        return balance.e.a;
                    case TransactionAmount.BALANCE_SIDE_LEFT:
                        return balance.e.b;
                    case TransactionAmount.BALANCE_SIDE_DEBIT:
                        return balance.e.a.subtract(balance.e.b);
                }
        }

        return null;
    }

    static public Tuple2<BigDecimal, BigDecimal> getBalanceInPosition(Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance,
                                                                      int position) {
        switch (position) {
            case TransactionAmount.ACTION_SEND:
                return balance.a;
            case TransactionAmount.ACTION_DEBT:
                return balance.b;
            case TransactionAmount.ACTION_HOLD:
                return balance.c;
            case TransactionAmount.ACTION_SPEND:
                return balance.d;
            case TransactionAmount.ACTION_PLEDGE:
                return balance.e;
        }

        return null;
    }


    public Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> getBalance(
            long key) {
        return this.getBalance(DCSet.getInstance(), key);
    }

    public BigDecimal getForSale(DCSet dcSet, long key, int height, boolean withCredit) {
        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance = this
                .getBalance(dcSet, key);
        BigDecimal ownVol = balance.a.b;

        if (!BlockChain.ERA_COMPU_ALL_UP && key == Transaction.RIGHTS_KEY && height > BlockChain.FREEZE_FROM) {
            int[][] item = BlockChain.FREEZED_BALANCES.get(this.getAddress());
            if (item != null) {
                if (item[0][0] < 0) {
                    return BigDecimal.ZERO;
                }

                // int height = dcSet.getBlocksHeadMap().size();
                BigDecimal freeze = BigDecimal.ZERO;
                for (int[] point : item) {
                    if (height < point[0]) {
                        freeze = new BigDecimal(point[1]);
                        break;
                    }
                }
                ownVol = ownVol.subtract(freeze);
            }
        }

        BigDecimal inDebt = balance.b.b;
        if (inDebt.signum() < 0 && withCredit) {
            ownVol = ownVol.add(inDebt);
        }
        return ownVol;
    }

    /*
     * private void updateGeneratingBalance(DBSet db) { //CHECK IF WE NEED TO
     * RECALCULATE if(this.lastBlockSignature == null) { this.lastBlockSignature
     * = db.getBlocksHeadMap().getLastBlockSignature();
     * calculateGeneratingBalance(db); } else { //CHECK IF WE NEED TO
     * RECALCULATE if(!Arrays.equals(this.lastBlockSignature,
     * db.getBlocksHeadMap().getLastBlockSignature())) { this.lastBlockSignature =
     * db.getBlocksHeadMap().getLastBlockSignature(); calculateGeneratingBalance(db);
     * } } }
     *
     * // take current balance public void calculateGeneratingBalance(DBSet db)
     * { long balance = this.getConfirmedBalance(ERA_KEY,
     * db).setScale(0).longValue(); this.generatingBalance = balance; }
     *
     * // balance FOR generation public void
     * calculateGeneratingBalance_old(DBSet db) { //CONFIRMED BALANCE + ALL
     * NEGATIVE AMOUNTS IN LAST 9 BLOCKS - for ERA_KEY only BigDecimal balance =
     * this.getConfirmedBalance(ERA_KEY, db);
     *
     * Block block = db.getBlocksHeadMap().getLastBlock();
     *
     * int penalty_koeff = 1000000; int balance_penalty = penalty_koeff;
     *
     * // icreator X 10 // not resolve first 100 blocks for(int i=1;
     * i<GenesisBlock.GENERATING_RETARGET * 10 && block != null &&
     * block.getHeight(db) > 100; i++) { for(Transaction transaction:
     * block.getTransactions()) { if(transaction.isInvolved(this) & transaction
     * instanceof TransactionAmount) { TransactionAmount ta =
     * (TransactionAmount)transaction;
     *
     * if(ta.getKey() == ERA_KEY &
     * transaction.getAmount(this).compareTo(BigDecimal.ZERO) == 1) { balance =
     * balance.subtract(transaction.getAmount(this)); } } }
     * LinkedHashMap<Tuple2<Integer,Integer>,ATTransaction> atTxs =
     * db.getATTransactionMap().getATTransactions(block.getHeight(db));
     * Iterator<ATTransaction> iter = atTxs.values().iterator(); while (
     * iter.hasNext() ) { ATTransaction key = iter.next(); if (
     * key.getRecipient().equals( this.getAddress() ) ) { balance =
     * balance.subtract( BigDecimal.valueOf(key.getAmount()) ); } }
     *
     * // icreator X 0.9 for each block generated if (balance_penalty > 0.1 *
     * penalty_koeff && block.getCreator().getAddress().equals(this.address)) {
     * balance_penalty *= Settings.GENERATE_CONTINUOUS_PENALTY * 0.001; } else {
     * // reset balance_penalty = penalty_koeff; } block = block.getParent(db);
     * }
     *
     * //DO NOT GO BELOW 0 if(balance.compareTo(BigDecimal.ZERO) == -1) {
     * balance = BigDecimal.ZERO; }
     *
     * // use penalty this.generatingBalance = balance.multiply(new
     * BigDecimal(balance_penalty / penalty_koeff));
     *
     * }
     */


    // Добавляем величины для тестовых режимов
    public BigDecimal addDEVAmount(long key) {
        if (BlockChain.ERA_COMPU_ALL_UP && key == 1)
            return BigDecimal.valueOf(( 512000 + 500 * this.getShortAddressBytes()[10]) >> 6);
        else if (BlockChain.ERA_COMPU_ALL_UP && key == 2)
            return new BigDecimal("100.0");

        return BigDecimal.ZERO;

    }
    public Tuple2<BigDecimal, BigDecimal> balAaddDEVAmount(long key, Tuple2<BigDecimal, BigDecimal> balA) {
        BigDecimal addAmount = addDEVAmount(key);
        if (addAmount.signum() == 0)
            return balA;

        return new Tuple2<>(balA.a, balA.b.add(addAmount));

    }

    public Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
        balanceAddDEVAmount(long key, Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                            balance) {
        BigDecimal addAmount = addDEVAmount(key);
        if (addAmount.signum() == 0)
            return balance;

        return new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    new Tuple2<BigDecimal, BigDecimal>(balance.a.a, balance.a.b.add(addAmount)),
                    balance.b, balance.c, balance.d, balance.e);
    }

    public Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> getBalance(
            DCSet db, long key) {
        if (key < 0)
            key = -key;

        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                balance = db.getAssetBalanceMap().get(getShortAddressBytes(), key);
        if (BlockChain.ERA_COMPU_ALL_UP) {
            return balanceAddDEVAmount(key, balance);
        }
        return balance;

    }

    public Tuple2<BigDecimal, BigDecimal> getBalance(DCSet db, long key, int actionType) {
        if (key < 0)
            key = -key;

        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance = db
                .getAssetBalanceMap().get(getShortAddressBytes(), key);

        if (actionType == TransactionAmount.ACTION_SEND) {
            if (BlockChain.ERA_COMPU_ALL_UP ) {
                return new Tuple2<BigDecimal, BigDecimal>(balance.a.a, balance.a.b.add(addDEVAmount(key)));
            }

            return balance.a;

        } else if (actionType == TransactionAmount.ACTION_DEBT)
            return balance.b;
        else if (actionType == TransactionAmount.ACTION_HOLD)
            return balance.c;
        else if (actionType == TransactionAmount.ACTION_SPEND)
            return balance.d;
        else
            return balance.e;

    }

    public void changeCOMPUBonusBalances(DCSet dcSet, boolean substract, BigDecimal amount, int side) {
        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                balance = dcSet.getAssetBalanceMap().get(getShortAddressBytes(), Transaction.FEE_KEY);

        if (side == Transaction.BALANCE_SIDE_DEBIT) {
            // учтем Всего бонусы
            // это Баланс 4-й сторона 1
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a, balance.b, balance.c,
                    substract ? new Tuple2<BigDecimal, BigDecimal>(balance.d.a.subtract(amount), balance.d.b)
                            : new Tuple2<BigDecimal, BigDecimal>(balance.d.a.add(amount), balance.d.b),
                    balance.e);
        } else if (side == Transaction.BALANCE_SIDE_CREDIT) {
            // учтем что Всего потратили
            // это Баланс 4-й сторона 1
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a, balance.b, balance.c,
                    !substract ? new Tuple2<BigDecimal, BigDecimal>(balance.d.a, balance.d.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(balance.d.a, balance.d.b.add(amount)),
                    balance.e);
        } else if (side == Transaction.BALANCE_SIDE_FORGED) {
            // учтем что Всего нафоржили
            // это Баланс 5-й
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a, balance.b, balance.c, balance.d,
                    substract ? new Tuple2<BigDecimal, BigDecimal>(balance.e.a, balance.e.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(balance.e.a, balance.e.b.add(amount))
            );
        } else {
            return;
        }

        dcSet.getAssetBalanceMap().put(getShortAddressBytes(), Transaction.FEE_KEY, balance);

    }

    public BigDecimal getCOMPUBonusBalances(DCSet dcSet, boolean substract, BigDecimal amount, int side) {
        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                balance = dcSet.getAssetBalanceMap().get(getShortAddressBytes(), Transaction.FEE_KEY);

        if (side == Transaction.BALANCE_SIDE_DEBIT) {
            // БОНУСЫ всего полученные
            return balance.d.a;
        } else if (side == Transaction.BALANCE_SIDE_CREDIT) {
            // все потрачено на комиссии
            return balance.d.b;
        } else if (side == Transaction.BALANCE_SIDE_FORGED) {
            // всего нафоржено
            return balance.e.b;
        }

        return balance.e.a;
    }

    /*
     * public void setLastReference(Long timestamp) {
     * this.setLastReference(timestamp, DBSet.getInstance()); }
     */

    // change BALANCE - add or subtract amount by KEY + AMOUNT = TYPE
    public Tuple3<BigDecimal, BigDecimal, BigDecimal> changeBalance(DCSet db, boolean substract, boolean isBackward, long key,
                                                                    BigDecimal amount_in, boolean notUpdateIncomed, boolean spendUpdate) {

        int actionType = balancePosition(key, amount_in, isBackward);

        ItemAssetBalanceMap map = db.getAssetBalanceMap();

        BigDecimal amount = amount_in.abs();
        long absKey;
        if (key > 0) {
            absKey = key;
        } else {
            absKey = -key;
        }

        // for DEBUG
        /*
        if (false
                && this.equals("77HyuCsr8u7f6znj2Lq8gXjK6DCG7osehs") && absKey == 1 && !db.isFork()
                && (actionType == TransactionAmount.ACTION_SEND || actionType == TransactionAmount.ACTION_DEBT)
                && true) {
            ;
        }
        */

        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance =
                map.get(getShortAddressBytes(), absKey);

        boolean updateIncomed = !notUpdateIncomed;

        Tuple2<BigDecimal, BigDecimal> spendBalance;
        if (spendUpdate) {
            // обновим Потрачено = Произведено одновременно
            if (substract) {
                spendBalance = new Tuple2<BigDecimal, BigDecimal>(balance.d.a,  balance.d.b.add(amount));
            } else {
                // входит сумма плюс учет
                spendBalance = new Tuple2<BigDecimal, BigDecimal>(balance.d.a.add(amount),  balance.d.b);
            }
        } else {
            spendBalance = balance.d;
        }

        if (actionType == TransactionAmount.ACTION_SEND) {
            // OWN + property
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    substract ? new Tuple2<BigDecimal, BigDecimal>(
                            updateIncomed ? balance.a.a.subtract(amount) : balance.a.a, balance.a.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(updateIncomed ? balance.a.a.add(amount) : balance.a.a,
                            balance.a.b.add(amount)),
                    balance.b, balance.c, spendBalance, balance.e);
        } else if (actionType == TransactionAmount.ACTION_DEBT) {
            // DEBT + CREDIT
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a,
                    substract ? new Tuple2<BigDecimal, BigDecimal>(
                            updateIncomed ? balance.b.a.subtract(amount) : balance.b.a, balance.b.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(updateIncomed ? balance.b.a.add(amount) : balance.b.a,
                            balance.b.b.add(amount)),
                    balance.c, spendBalance, balance.e);
        } else if (actionType == TransactionAmount.ACTION_HOLD) {
            // HOLD + STOCK
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a, balance.b,
                    substract ? new Tuple2<BigDecimal, BigDecimal>(
                            updateIncomed ? balance.c.a.subtract(amount) : balance.c.a, balance.c.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(updateIncomed ? balance.c.a.add(amount) : balance.c.a,
                            balance.c.b.add(amount)),
                    spendBalance, balance.e);
        } else if (actionType == TransactionAmount.ACTION_SPEND) {
            // TODO - SPEND + PRODUCE
            balance = new Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>(
                    balance.a, balance.b, balance.c,
                    substract ? new Tuple2<BigDecimal, BigDecimal>(
                            updateIncomed ? balance.d.a.subtract(amount) : balance.d.a, balance.d.b.subtract(amount))
                            : new Tuple2<BigDecimal, BigDecimal>(updateIncomed ? balance.d.a.add(amount) : balance.d.a,
                            balance.d.b.add(amount)),
                    balance.e);
        }

        map.put(getShortAddressBytes(), absKey, balance);

        ////////////// DEBUG TOTAL COMPU
        // несотыковка из-за ордеров на бирже
        if (false && absKey == 2l && this.equals("73EotEbxvAo39tyugJSyL5nbcuMWs4aUpS")) {
            Collection<byte[]> addrs = db.getAssetBalanceMap().keySet();
            BigDecimal total = BigDecimal.ZERO;
            for (byte[] mapKey : addrs) {
                if (ItemAssetBalanceMap.getAssetKeyFromKey(mapKey) == 2l) {
                    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> ball =
                            map.get(mapKey);

                    total = total.add(ball.a.b);
                }
            }
            if (total.signum() != 0) {
                Long error = null;
                error++;
            }
        }

        return new Tuple3<BigDecimal, BigDecimal, BigDecimal>(balance.a.b, balance.b.b, balance.c.b);
    }

    public Tuple3<BigDecimal, BigDecimal, BigDecimal> getConfBalance3(int confirmations, long key) {
        return this.getConfBalance3(confirmations, key, DCSet.getInstance());
    }

    public Tuple3<BigDecimal, BigDecimal, BigDecimal> getConfBalance3(int confirmations, long key, DCSet db) {
        // CHECK IF UNCONFIRMED BALANCE
        if (confirmations <= 0) {
            return this.getUnconfirmedBalance(key);
        }

        // IF 1 CONFIRMATION
        if (confirmations == 1) {
            Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance = this
                    .getBalance(db, key);
            return new Tuple3<BigDecimal, BigDecimal, BigDecimal>(balance.a.b, balance.b.b, balance.c.b);
        }

        // GO TO PARENT BLOCK 10
        Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance = this
                .getBalance(db, key);
        BigDecimal own = balance.a.b;
        BigDecimal rent = balance.b.b;
        BigDecimal hold = balance.c.b;

        Block block = db.getBlockMap().last();

        for (int i = 1; i < confirmations && block != null && block.getVersion() > 0; i++) {
            for (Transaction transaction : block.getTransactions()) {

                transaction.setDC(db); // need for Involved

                if (transaction.isInvolved(this)) {
                    if (transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {

                        int actionType = ((TransactionAmount)transaction).getActionType();
                        if (actionType == TransactionAmount.ACTION_SEND) {
                            own = own.subtract(transaction.getAmount(this));
                        } else {
                            rent = own.subtract(transaction.getAmount(this));
                        }
                    }

                }
            }

            block = block.getParent(db);
        }

        // RETURN
        return new Tuple3<BigDecimal, BigDecimal, BigDecimal>(own, rent, hold);
    }

    public static BigDecimal totalForAddresses(DCSet dcSet, Set<String> addresses, Long assetKey, int pos) {

        BigDecimal eraBalanceA = BigDecimal.ZERO;
        for (String address : addresses) {

            Account account = new Account(address);
            Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>> balance
                    = account.getBalance(dcSet, assetKey);

            switch (pos) {
                case 1:
                    eraBalanceA = eraBalanceA.add(balance.a.b);
                    break;
                case 2:
                    eraBalanceA = eraBalanceA.add(balance.b.b);
                    break;
                case 3:
                    eraBalanceA = eraBalanceA.add(balance.c.b);
                    break;
                case 4:
                    eraBalanceA = eraBalanceA.add(balance.d.b);
                    break;
                case 5:
                    eraBalanceA = eraBalanceA.add(balance.e.b);
                    break;
            }
        }

        return eraBalanceA;

    }

    public long[] getLastTimestamp() {
        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0)
            return null;
        return this.getLastTimestamp(DCSet.getInstance());
    }

    /**
     * account.address -> LAST[TX.timestamp + TX.dbRef]
     *
     * @param dcSet
     * @return
     */
    public long[] getLastTimestamp(DCSet dcSet) {
        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0)
            return null;
        return dcSet.getReferenceMap().get(shortBytes);
    }

    /**
     * @param currentPoint [timestamp, dbRef]
     * @param dcSet        DCSet
     */
    public void setLastTimestamp(long[] currentPoint, DCSet dcSet) {

        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0)
            return;

        ReferenceMapImpl map = dcSet.getReferenceMap();

        if (BlockChain.NOT_STORE_REFFS_HISTORY) {
            // SET NEW REFERENCE
            map.put(shortBytes, currentPoint);
            return;
        }

        // GET CURRENT REFERENCE
        long[] reference = map.get(shortBytes);

        // MAKE KEY for this TIMESTAMP
        byte[] keyCurrentPoint = Bytes.concat(shortBytes, Longs.toByteArray(currentPoint[0]));

        if (reference != null) {
            // set NEW LAST TIMESTAMP as REFERENCE
            map.put(keyCurrentPoint, reference);
        }

        // SET NEW REFERENCE
        map.put(shortBytes, currentPoint);

    }

    public void removeLastTimestamp(DCSet dcSet) {

        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0) {
            return;
        }

        ReferenceMapImpl map = dcSet.getReferenceMap();

        if (BlockChain.NOT_STORE_REFFS_HISTORY) {
            map.delete(shortBytes);
            return;
        }

        // GET LAST TIMESTAMP
        long[] lastPoint = map.get(shortBytes);

        if (lastPoint == null)
            return;

        // MAKE KEY for this TIMESTAMP
        byte[] keyPrevPoint = Bytes.concat(shortBytes, Longs.toByteArray(lastPoint[0]));

        // GET REFERENCE
        // DELETE TIMESTAMP - REFERENCE
        long[] reference = map.remove(keyPrevPoint);
        if (reference == null) {
            map.delete(shortBytes);
        } else {
            // PUT OLD REFERENCE
            map.put(shortBytes, reference);
        }
    }

    public void removeLastTimestamp(DCSet dcSet, long timestamp) {

        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0) {
            return;
        }

        ReferenceMapImpl map = dcSet.getReferenceMap();

        if (BlockChain.NOT_STORE_REFFS_HISTORY) {
            map.delete(shortBytes);
            return;
        }

        // MAKE KEY for this TIMESTAMP
        byte[] keyPrevPoint = Bytes.concat(shortBytes, Longs.toByteArray(timestamp));

        // GET REFERENCE
        // DELETE TIMESTAMP - REFERENCE
        long[] reference = map.remove(keyPrevPoint);
        if (reference == null) {
            map.delete(shortBytes);
        } else {
            // PUT OLD REFERENCE
            map.put(shortBytes, reference);
        }
    }

    // TOSTRING
    public String personChar(Tuple2<Integer, PersonCls> personRes) {
        if (personRes == null)
            return "";

        PersonCls person = personRes.b;
        if (!person.isAlive(0l))
            return "☗"; // "☗"; ☻

        int key = personRes.a;
        if (key == -1)
            return "-"; // "☺";
        else if (key == 1)
            return "♥"; // "♥"; //"☺"; //"☑"; 9829
        else
            return "";

    }

    public String viewFEEbalance() {

        long result = this.getBalanceUSE(Transaction.FEE_KEY).unscaledValue().longValue();
        result /= BlockChain.FEE_PER_BYTE;
        result >>= 8;

        if (result > 1000)
            return "+4";
        else if (result > 100)
            return "+3";
        else if (result > 10)
            return "+2";
        else if (result > 1)
            return "+1";
        else
            return "0";

    }

    @Override
    public String toString() {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        String personStr;
        String addressStr;
        if (personRes == null) {
            personStr = "";
            addressStr = this.getAddress();
        } else {
            personStr = personChar(personRes) + personRes.b.getShort();
            addressStr = this.getAddress().substring(1, 8);
        }

        return " {"
                // + NumberAsString.formatAsString(this.getBalanceUSE(FEE_KEY))
                + viewFEEbalance()
                + "}" + " " + addressStr + "" + personStr;
    }

    public String toString(long key) {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        String personStr;
        String addressStr;
        if (personRes == null) {
            personStr = "";
            addressStr = GenesisBlock.CREATOR.equals(this) ? "GENESIS" : this.getAddress();
        } else {
            personStr = personChar(personRes) + personRes.b.getShort();
            addressStr = this.getAddress().substring(1, 8);
        }

        boolean statusBad = Controller.getInstance().getStatus() != Controller.STATUS_OK;
        Tuple2<BigDecimal, BigDecimal> balance = this.getBalanceInSettedPosition(key);

        return (statusBad ? "??? " : "")
                + (balance == null? "" : NumberAsString.formatAsString(balance.b) + " ")
                + (key == Transaction.FEE_KEY?" " : "{" + viewFEEbalance() + "} ")
                + addressStr + "" + personStr;
    }

    //////////
    public String viewPerson() {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        if (personRes == null) {
            if (this.getAddress() != null) {
                return this.getAddress();

            } else {
                return "";
            }
        } else {
            String personStr = personChar(personRes) + personRes.b.toString();
            return personStr;
        }

    }

    public String getPersonAsString() {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        if (personRes == null) {
            return GenesisBlock.CREATOR.equals(this) ? "GENESIS" : this.getAddress();
        } else {
            String personStr = personChar(personRes) + personRes.b.getShort();
            String addressStr = this.getAddress().substring(1, 7);
            return addressStr + "" + personStr;
        }
    }

    public String getPersonAsString(int cutAddress) {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        if (personRes == null) {
            return GenesisBlock.CREATOR.equals(this) ? "GENESIS" : this.getAddress().substring(0, cutAddress) + "..";
        } else {
            String personStr = personChar(personRes) + personRes.b.getShort();
            String addressStr = this.getAddress().substring(1, 5);
            return addressStr + "" + personStr;
        }
    }

    public String getPersonOrShortAddress(int max) {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        if (personRes == null) {
            return GenesisBlock.CREATOR.equals(this) ? "GENESIS" : this.getAddress().substring(0, max) + "~";
        } else {
            return "[" + personRes.b.getKey() + "]" + personRes.b.getName();
        }
    }

    public String getPersonAsString_01(boolean shrt) {
        Tuple2<Integer, PersonCls> personRes = this.getPerson();
        if (personRes == null) {
            return "";
        } else {
            return shrt ? personRes.b.getShort() : personRes.b.getName();
        }
    }

    @Override
    public int hashCode() {
        return Ints.fromByteArray(shortBytes);
    }

    // EQUALS
    @Override
    public boolean equals(Object b) {
        if (b instanceof Account) {
            return Arrays.equals(this.shortBytes, ((Account) b).getShortAddressBytes());
        } else if (b instanceof String) {
            return this.getAddress().equals(b);
        } else if (b instanceof byte[]) {
            byte[] bs = (byte[]) b;
            if (bs.length == ADDRESS_LENGTH) {
                return Arrays.equals(this.bytes, bs);
            } else {
                return Arrays.equals(this.shortBytes, bs);
            }
        }

        return false;
    }

    //public void resetPersonDuration() {
    //    this.personDuration = null;
    //}

    public Tuple4<Long, Integer, Integer, Integer> getPersonDuration(DCSet db) {
        //    if (this.personDuration == null) {
        //        нельзя использовать старые значения так как при откатах они не будут чиститься
        //        this.personDuration = db.getAddressPersonMap().getItem(shortBytes);
        //    }

        //return this.personDuration;

        return db.getAddressPersonMap().getItem(shortBytes);

    }

    public boolean isPerson(DCSet dcSet, int forHeight, Tuple4<Long, Integer, Integer, Integer> addressDuration) {

        // IF DURATION ADDRESS to PERSON IS ENDED
        if (addressDuration == null)
            return false;

        // TEST TIME and EXPIRE TIME
        long current_time = Controller.getInstance().getBlockChain().getTimestamp(forHeight);

        // TEST TIME and EXPIRE TIME for PERSONALIZE address
        int days = addressDuration.b;
        if (days < 0)
            return false;
        if (days * (long) 86400000 < current_time)
            return false;

        // IF PERSON ALIVE
        Long personKey = addressDuration.a;
        // TODO by deth day if
        /*
         * //Tuple5<Long, Long, byte[], Integer, Integer> personDuration =
         * db.getPersonStatusMap().getItem(personKey, ALIVE_KEY); // TEST TIME
         * and EXPIRE TIME for ALIVE person Long end_date = personDuration.b; if
         * (end_date == null ) return true; // permanent active if (end_date <
         * current_time + 86400000l ) return false; // - 1 day
         */

        return true;

    }

    public boolean isPerson(DCSet dcSet, int forHeight) {

        // IF DURATION ADDRESS to PERSON IS ENDED
        Tuple4<Long, Integer, Integer, Integer> addressDuration =
                this.getPersonDuration(dcSet);
        if (addressDuration == null)
            return false;

        return isPerson(dcSet, forHeight, addressDuration);
    }

    public boolean isPerson() {
        return isPerson(DCSet.getInstance(), Controller.getInstance().getMyHeight());
    }


    /**
     * Обновляет данные о персоне даже если они уже были записаны
     *
     * @param dcSet
     * @param forHeight
     * @return
     */
    public Tuple2<Integer, PersonCls> getPerson(DCSet dcSet, int forHeight, Tuple4<Long, Integer, Integer, Integer> addressDuration) {

        // IF DURATION ADDRESS to PERSON IS ENDED
        if (addressDuration == null)
            return null;

        // TEST TIME and EXPIRE TIME
        long current_time = Controller.getInstance().getBlockChain().getTimestamp(forHeight);

        // get person
        Long personKey = addressDuration.a;
        PersonCls person = (PersonCls) Controller.getInstance().getItem(dcSet, ItemCls.PERSON_TYPE, personKey);

        // TEST ADDRESS is ACTIVE?
        int days = addressDuration.b;
        // TODO x 1000 ?
        if (days < 0 || days * (long) 86400000 < current_time)
            return new Tuple2<Integer, PersonCls>(-1, person);

        // IF PERSON is ALIVE
        // TODO by DEATH day
        /*
         * Tuple5<Long, Long, byte[], Integer, Integer> personDuration =
         * db.getPersonStatusMap().getItem(personKey, ALIVE_KEY); // TEST TIME
         * and EXPIRE TIME for ALIVE person if (personDuration == null) return
         * new Tuple2<Integer, PersonCls>(-2, person); Long end_date =
         * personDuration.b; if (end_date == null ) // permanent active return
         * new Tuple2<Integer, PersonCls>(0, person); else if (end_date <
         * current_time + 86400000l ) // ALIVE expired return new Tuple2<Integer,
         * PersonCls>(-1, person);
         */

        return new Tuple2<Integer, PersonCls>(1, person);

    }

    public Tuple2<Integer, PersonCls> getPerson(DCSet dcSet, int forHeight) {

        // IF DURATION ADDRESS to PERSON IS ENDED
        Tuple4<Long, Integer, Integer, Integer> addressDuration = this.getPersonDuration(dcSet);
        if (addressDuration == null)
            return null;

        return getPerson(dcSet, forHeight, addressDuration);
    }

    /**
     * берет данные из переменной локальной если там что-то было
     *
     * @return
     */
    public Tuple2<Integer, PersonCls> getPerson() {
        if (person == null) {
            person = getPerson(DCSet.getInstance(), Controller.getInstance().getMyHeight());
        }
        return person;
    }

    // previous forging block or changed ERA volume
    public Tuple3<Integer, Integer, Integer> getForgingData(DCSet db, int height) {
        return db.getAddressForging().get(getAddress(), height);
    }

    public void setForgingData(DCSet db, int height, int forgingBalance) {
        db.getAddressForging().putAndProcess(getAddress(), height, forgingBalance);
    }

    public void delForgingData(DCSet db, int height) {
        db.getAddressForging().deleteAndProcess(getAddress(), height);
    }

    public Tuple3<Integer, Integer, Integer> getLastForgingData(DCSet db) {
        return db.getAddressForging().getLast(getAddress());
    }

    public static Tuple3<String, String, String> getFromFavorites(String address) {
        return Controller.getInstance().wallet.database.getFavoriteAccountsMap().get(address);

    }

    public Tuple3<String, String, String> getFromFavorites() {
        return getFromFavorites(getAddress());
    }

    public int getAccountNo() {
        return Controller.getInstance().wallet.database.getAccountMap().getAccountNo(getAddress());
    }

}
