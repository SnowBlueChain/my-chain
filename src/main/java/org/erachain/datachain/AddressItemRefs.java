package org.erachain.datachain;

import org.erachain.core.crypto.Base58;
import org.erachain.database.DBMap;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import java.util.Map;
import java.util.TreeMap;

/**
 * учет времени последней транзакции данного вида, посланной со счета.
 * Используется для проверки валидности транзакций.
 * Если ключ создан без времени, то хранит ссылку на последнюютранзакцию с этого счета
 * Ключ - счет (20 байт) + время (Long)
 * Значение  - массив байтов
 * Используется как супер класс для AddressStatementRefs (которая сейчас не используется?) - видимо для быстрого поиска записей данного вида для данного счета
 */
public class AddressItemRefs extends DCMap<Tuple2<byte[], Long>, byte[]> {
    protected String name;

    public AddressItemRefs(DCSet databaseSet, DB database, String name,
                           int observeReset, int observeAdd, int observeRemove, int observeList
    ) {
        super(databaseSet, database);
        this.name = name;

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBMap.NOTIFY_RESET, observeReset);
            this.observableData.put(DBMap.NOTIFY_LIST, observeList);
            this.observableData.put(DBMap.NOTIFY_ADD, observeAdd);
            this.observableData.put(DBMap.NOTIFY_REMOVE, observeRemove);
        }

    }


    public AddressItemRefs(AddressItemRefs parent, DCSet dcSet) {
        super(parent, dcSet);
    }

    protected void createIndexes(DB database) {
    }

    @Override
    protected void getMap(DB database) {
        //OPEN MAP
        map = database.createTreeMap("address_" + this.name + "_refs")
                //.keySerializer(BTreeKeySerializer.TUPLE2)
                //.comparator(UnsignedBytes.lexicographicalComparator())
                .comparator(new Fun.Tuple2Comparator(Fun.BYTE_ARRAY_COMPARATOR, Fun.COMPARATOR)) // - for Tuple2<byte[]m byte[]>
                .counterEnable()
                .makeOrGet();
    }

    @Override
    protected void getMemoryMap() {
        //return new TreeMap<Tuple2<byte[], Long>, byte[]>(UnsignedBytes.lexicographicalComparator());
        map = new TreeMap<Tuple2<byte[], Long>, byte[]>();
    }

    @Override
    protected byte[] getDefaultValue() {
        return null;
    }

    public byte[] get(String address, Long key) {
        return this.get(new Tuple2<byte[], Long>(Base58.decode(address), key));
    }

    public void set(String address, Long key, byte[] ref) {
        this.set(new Tuple2<byte[], Long>(Base58.decode(address), key), ref);
    }

    public void delete(String address, Long key) {
        this.delete(new Tuple2<byte[], Long>(Base58.decode(address), key));
    }
}
