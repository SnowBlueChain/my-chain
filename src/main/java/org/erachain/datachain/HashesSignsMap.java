package org.erachain.datachain;

import com.google.common.primitives.UnsignedBytes;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple3;

import java.util.Stack;
import java.util.TreeMap;

//import org.mapdb.Fun.Tuple2;
//import org.mapdb.Fun.Tuple3;

// hash[byte] -> Stack person + block.height + transaction.seqNo
// Example - database.AddressPersonMap
/** Набор хэшей - по хэшу поиск записи в котрой он участвует и
 * используется в транзакции org.erachain.core.transaction.RHashes
 hash[byte] -> Stack person + block.height + transaction.seqNo

 * Ключ: хэш<br>
 * Значение: список - номер персоны (Если это персона создала запись, ссылка на запись)<br>
 // TODO укротить до 20 байт адрес и ссылку на Long

 */

public class HashesSignsMap extends DCUMap<byte[], Stack<Tuple3<
        Long, // person key
        Integer, // block height
        Integer>>> // transaction index
{

    public HashesSignsMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);
    }

    public HashesSignsMap(HashesSignsMap parent, DCSet dcSet) {
        super(parent, dcSet);
    }

    protected void createIndexes() {
    }

    @Override
    public void openMap() {
        //OPEN MAP
        map = database.createTreeMap("hashes_signs")
                .keySerializer(BTreeKeySerializer.BASIC)
                .comparator(UnsignedBytes.lexicographicalComparator())
                .makeOrGet();
    }

    @Override
    protected void getMemoryMap() {
        map = new TreeMap<byte[], Stack<Tuple3<Long, Integer, Integer>>>(UnsignedBytes.lexicographicalComparator());
    }

    @Override
    public Stack<Tuple3<Long, Integer, Integer>> getDefaultValue() {
        return new Stack<Tuple3<Long, Integer, Integer>>();
    }

    ///////////////////////////////
    @SuppressWarnings("unchecked")
    public void addItem(byte[] hash, Tuple3<Long, Integer, Integer> item) {

        Stack<Tuple3<Long, Integer, Integer>> value = this.get(hash);

        Stack<Tuple3<Long, Integer, Integer>> value_new;

        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (Stack<Tuple3<Long, Integer, Integer>>) value.clone();
        }

        value_new.push(item);

        this.put(hash, value_new);

    }

    public Tuple3<Long, Integer, Integer> getItem(byte[] hash) {
        Stack<Tuple3<Long, Integer, Integer>> value = this.get(hash);
        return value == null || value.isEmpty() ? null : value.peek();
    }

    @SuppressWarnings("unchecked")
    public void removeItem(byte[] hash) {
        Stack<Tuple3<Long, Integer, Integer>> value = this.get(hash);
        if (value == null || value.isEmpty()) return;

        Stack<Tuple3<Long, Integer, Integer>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (Stack<Tuple3<Long, Integer, Integer>>) value.clone();
        }

        value_new.pop();

        this.put(hash, value_new);

    }

}
