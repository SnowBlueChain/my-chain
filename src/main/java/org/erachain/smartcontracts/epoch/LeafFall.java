package org.erachain.smartcontracts.epoch;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.AssetVenture;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.SmartContractValues;
import org.mapdb.Fun;

import java.math.BigDecimal;

/**
 * Ctrl+Shift-T (IntellijIDEA) - make test unit
 */
public class LeafFall extends EpochSmartContract {

    public static final int ID = 1;
    static public final PublicKeyAccount MAKER = new PublicKeyAccount("" + ID);

    private int count;
    private long keyInit;
    private long leafKey;

    /**
     * list of assets for this smart-contract
     */
    private static long[] leafs = new long[]{123456L, 123L, 234L, 2354L, 345L, 34L, 5L, 345L};

    static final Fun.Tuple2 COUNT_KEY = new Fun.Tuple2(ID, "c");

    public LeafFall(int count) {
        super(ID, MAKER);
        this.count = count;
    }

    public LeafFall(int count, long keyInit, long leafKey) {
        super(ID, MAKER);
        this.count = count;
        this.keyInit = keyInit;
        this.leafKey = leafKey;
    }

    public int getCount() {
        return count;
    }

    public long getKeyInit() {
        return keyInit;
    }

    private long getLeafKey(Block block, Transaction transaction) {
        int hash = Byte.toUnsignedInt((byte) (block.getSignature()[5] + transaction.getSignature()[5]));
        int level;
        if (hash < 2)
            level = 7;
        else if (hash < 4)
            level = 6;
        else if (hash < 8)
            level = 5;
        else if (hash < 16)
            level = 4;
        else if (hash < 32)
            level = 3;
        else if (hash < 64)
            level = 2;
        else if (hash < 128)
            level = 1;
        else
            level = 0;

        return leafs[level];
    }

    @Override
    public Object[][] getItemsKeys() {
        if (keyInit == 0) {
            // not confirmed yet
            return null;
        }

        Object[][] itemKeys = new Object[count][];

        int i = 0;
        do {
            itemKeys[i] = new Object[]{ItemCls.ASSET_TYPE, keyInit - i};
        } while (++i < count);

        return itemKeys;

    }

    @Override
    public int length(int forDeal) {
        if (forDeal == Transaction.FOR_DB_RECORD)
            return 24;

        return 8;
    }

    @Override
    public byte[] toBytes(int forDeal) {

        byte[] data = Ints.toByteArray(id);
        data = Bytes.concat(data, Ints.toByteArray(count));

        if (forDeal == Transaction.FOR_DB_RECORD) {
            return Bytes.concat(Bytes.concat(data, Longs.toByteArray(keyInit)),
                    Longs.toByteArray(leafKey));
        }

        return data;

    }

    public static LeafFall Parse(byte[] data, int pos, int forDeal) {

        // skip ID
        pos += 4;

        byte[] countBuffer = new byte[4];
        System.arraycopy(data, pos, countBuffer, 0, 4);
        pos += 4;

        if (forDeal == Transaction.FOR_DB_RECORD) {
            // возьмем в базе готовый ключ актива
            byte[] keyBuffer = new byte[8];
            System.arraycopy(data, pos, keyBuffer, 0, 8);
            pos += 8;

            // GET LEAF KEY
            byte[] leafBuffer = new byte[8];
            System.arraycopy(data, pos, leafBuffer, 0, 8);

            return new LeafFall(Ints.fromByteArray(countBuffer), Longs.fromByteArray(keyBuffer),
                    Longs.fromByteArray(leafBuffer));
        }

        return new LeafFall(Ints.fromByteArray(countBuffer));
    }

    private void action(DCSet dcSet, Block block, Transaction transaction, boolean asOrphan) {
        if (leafKey == 0)
            leafKey = getLeafKey(block, transaction);

        transaction.getCreator().changeBalance(dcSet, asOrphan, false, leafKey,
                BigDecimal.ONE, false, false, false);
        maker.changeBalance(dcSet, !asOrphan, false, leafKey,
                BigDecimal.ONE, false, false, false);
    }

    private void init(DCSet dcSet, Transaction transaction) {

        /**
         * for accounting total leaf for person
         */
        AssetVenture leafSum = new AssetVenture(null, maker, "LeafFall_sum", null, null,
                null, AssetCls.AS_SELF_MANAGED_ACCOUNTING, 0, 0);
        leafSum.setReference(transaction.getSignature(), transaction.getDBRef());

        //INSERT INTO DATABASE
        keyInit = dcSet.getItemAssetMap().incrementPut(leafSum);

    }

    @Override
    public boolean process(DCSet dcSet, Block block, Transaction transaction) {

        /**
         * use this state storage if many variables used in smart-contract
         */
        //SmartContractState stateMap = dcSet.getSmartContractState();

        /**
         * Use this values storage if several variables used in smart-contract
         *  and orphans values not linked to previous state
         */
        SmartContractValues valuesMap = dcSet.getSmartContractValues();
        if (keyInit == 0) {
            init(dcSet, transaction);
        } else {
            count = (Integer) valuesMap.get(COUNT_KEY);
        }

        action(dcSet, block, transaction, false);

        valuesMap.put(COUNT_KEY, ++count);

        return false;
    }

    private void wipe(DCSet dcSet) {
        dcSet.getItemAssetMap().decrementDelete(keyInit);
        SmartContractValues valuesMap = dcSet.getSmartContractValues();
        valuesMap.delete(COUNT_KEY);
    }

    @Override
    public boolean orphan(DCSet dcSet, Transaction transaction) {

        SmartContractValues valuesMap = dcSet.getSmartContractValues();
        count = (Integer) valuesMap.get(COUNT_KEY);

        // leafKey already calculated OR get from DB
        action(dcSet, null, transaction, true);

        if (count == 1) {
            /**
             * remove all data from db
             */
            wipe(dcSet);
        } else {
            valuesMap.put(COUNT_KEY, --count);
        }

        return false;
    }

}
