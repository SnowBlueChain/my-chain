package org.erachain.core.epoch;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.AssetUnique;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.SmartContractValues;
import org.mapdb.Fun;

import java.math.BigDecimal;

public class DogePlanet extends SmartContract {

    static private final PublicKeyAccount MAKER = new PublicKeyAccount("1");
    private int count;
    private long keyEnd;

    DogePlanet(int count) {
        super(DOGE_PLANET_1, MAKER);
        this.count = count;
    }

    DogePlanet(int count, long keyEnd) {
        super(DOGE_PLANET_1, MAKER);
        this.count = count;
        this.keyEnd = keyEnd;
    }

    public int getCount() {
        return count;
    }

    public long getKeyEnd() {
        return keyEnd;
    }

    @Override
    public Object[][] getItemsKeys() {
        if (keyEnd == 0) {
            // not confirmed yet
            return null;
        }

        Object[][] itemKeys = new Object[count][];

        int i = 0;
        do {
            itemKeys[i] = new Object[]{ItemCls.ASSET_TYPE, keyEnd - i};
        } while (++i < count);

        return itemKeys;

    }

    @Override
    public int length(int forDeal) {
        if (forDeal == Transaction.FOR_DB_RECORD)
            return 16;

        return 8;
    }

    @Override
    public byte[] toBytes(int forDeal) {

        byte[] data = Ints.toByteArray(id);
        data = Bytes.concat(data, Ints.toByteArray(count));

        if (forDeal == Transaction.FOR_DB_RECORD) {
            return Bytes.concat(data, Longs.toByteArray(keyEnd));
        }

        return data;

    }

    static DogePlanet Parse(byte[] data, int pos, int forDeal) {

        // skip ID
        pos += 4;

        byte[] countBuffer = new byte[4];
        System.arraycopy(data, pos, countBuffer, 0, 4);
        pos += 4;

        if (forDeal == Transaction.FOR_DB_RECORD) {
            // возьмем в базе готовый ключ актива
            byte[] keyBuffer = new byte[8];
            System.arraycopy(data, pos, keyBuffer, 0, 8);
            return new DogePlanet(Ints.fromByteArray(countBuffer), Longs.fromByteArray(keyBuffer));
        }

        return new DogePlanet(Ints.fromByteArray(countBuffer));
    }

    /**
     * Эпохальный смарт-контракт
     *
     * @return
     */
    public boolean isEpoch() {
        return true;
    }


    @Override
    public boolean process(DCSet dcSet, Block block, Transaction transaction) {

        AssetUnique planet;
        int i = count;

        SmartContractValues valuesMap = dcSet.getSmartContractValues();
        Fun.Tuple2 countValueKey = new Fun.Tuple2(id, "c");
        Integer totalIssuedObj = (Integer) valuesMap.get(countValueKey);
        int totalIssued;
        if (totalIssuedObj == null)
            totalIssued = 0;
        else
            totalIssued = totalIssuedObj;

        do {

            totalIssued++;

            planet = new AssetUnique(null, maker, "Doge Planet #" + totalIssued, null, null,
                    null, AssetCls.AS_NON_FUNGIBLE);
            planet.setReference(transaction.getSignature(), transaction.getDBRef());

            //INSERT INTO DATABASE
            keyEnd = dcSet.getItemAssetMap().incrementPut(planet);
            transaction.getCreator().changeBalance(dcSet, false, false, keyEnd,
                    BigDecimal.ONE, false, false, false);

        } while (--i > 0);

        valuesMap.put(countValueKey, totalIssued);


        return false;
    }


    @Override
    public boolean orphan(DCSet dcSet, Transaction transaction) {

        SmartContractValues valuesMap = dcSet.getSmartContractValues();
        Fun.Tuple2 countValueKey = new Fun.Tuple2(id, "c");
        Integer totalIssued = (Integer) valuesMap.get(countValueKey);

        int i = 0;
        do {

            transaction.getCreator().changeBalance(dcSet, true, false, keyEnd,
                    BigDecimal.ONE, false, false, false);

            //DELETE FROM DATABASE
            dcSet.getItemAssetMap().decrementDelete(keyEnd - i);
        } while (++i < count);

        valuesMap.put(countValueKey, totalIssued - count);

        return false;
    }

}
