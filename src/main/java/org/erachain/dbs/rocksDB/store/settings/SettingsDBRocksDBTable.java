package org.erachain.rocksDB.store.settings;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.tuple.Tuple2;
import org.erachain.rocksDB.common.RocksDB;
import org.erachain.rocksDB.transformation.Byteable;
import org.erachain.rocksDB.transformation.ByteableAtomicLong;
import org.erachain.rocksDB.transformation.ByteableInteger;
import org.erachain.rocksDB.transformation.ByteableString;
import org.mapdb.Atomic;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
@Slf4j
public class SettingsDBRocksDBTable {
    //  интерфейс доступа к БД
    private RocksDB db;
    private List<Tuple2<String, Byteable>> namesByteables;
    private ByteableAtomicLong byteableAtomicLong = new ByteableAtomicLong();
    private ByteableString byteableString = new ByteableString();
    public SettingsDBRocksDBTable(List<Tuple2<String, Byteable>> namesByteables, String nameTable) {
        this.namesByteables = namesByteables;
        db = new RocksDB(nameTable);
    }

    public Object get(String name) {
        Tuple2<String, Byteable> nameByteable = receiveTuple2ByName(name);
        byte[] bytes = db.get(byteableString.toBytesObject(name));
        return nameByteable.f1.receiveObjectFromBytes(bytes);
    }

    public AtomicLong getAtomicLong(String name) {
        byte[] bytes = db.get(byteableString.toBytesObject(name));
        if (bytes == null) {
            db.put(byteableString.toBytesObject(name), new byte[]{0,0,0,0,0,0,0,0});// 0L - нулевое байтовое представление long
            return new AtomicLong(0);
        }
        return byteableAtomicLong.receiveObjectFromBytes(bytes);
    }
    public AtomicBoolean getAtomicBoolean(String name) {
        byte[] bytes = db.get(byteableString.toBytesObject(name));
        if (bytes == null) {
            db.put(byteableString.toBytesObject(name), new byte[]{0});
            return new AtomicBoolean(false);
        }
        if (Arrays.equals(bytes, new byte[]{0})) {
            return new AtomicBoolean(false);
        }
        return new AtomicBoolean(true);
    }
    public void put(String name, Object value) {
        Tuple2<String, Byteable> nameByteable = receiveTuple2ByName(name);
        db.put(byteableString.toBytesObject(name), nameByteable.f1.toBytesObject(value));
    }

    public void putAtomicLong(String name, AtomicLong value) {
        db.put(byteableString.toBytesObject(name), byteableAtomicLong.toBytesObject(value));
    }
    public void putAtomicBoolean(String name, AtomicBoolean value) {
        //если false
        if (!value.get()) {
            db.put(byteableString.toBytesObject(name), new byte[]{0});
        } else {
            db.put(byteableString.toBytesObject(name), new byte[]{1});
        }

    }


    private Tuple2<String, Byteable> receiveTuple2ByName(String name) {
        return namesByteables.stream().filter((tuple2) -> (tuple2.f0.equals(name))).findFirst().get();
    }

    public void close(){
        db.close();
    }

    public void putByteArray(String name, byte[] value) {
        db.put(byteableString.toBytesObject(name),value);
    }

    public byte[] getByteArray(String name) {
        byte[] bytes = db.get(byteableString.toBytesObject(name));
        if (bytes == null) {
            byte[] value = {0};
            db.put(byteableString.toBytesObject(name), value);
            return value;
        }
        return bytes;
    }
}
