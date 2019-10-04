package org.erachain.dbs.rocksDB.integration;

import lombok.extern.slf4j.Slf4j;
import org.erachain.database.DBASet;
import org.erachain.dbs.Transacted;
import org.erachain.dbs.rocksDB.comm.RocksDbDataSourceDBCommitAsBath;
import org.erachain.dbs.rocksDB.common.RocksDbSettings;
import org.erachain.dbs.rocksDB.indexes.IndexDB;
import org.erachain.dbs.rocksDB.transformation.Byteable;
import org.erachain.dbs.rocksDB.transformation.ByteableTrivial;
import org.rocksdb.WriteOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Данный класс представляет собой основной доступ и функционал к таблице БД RocksDB
 * Тут происходит обработка настроенных вторичных индексов.
 * вызывается из SUIT
 *
 * @param <K>
 * @param <V>
 */
@Slf4j
public class DBRocksDBTableDBCommitedAsBath<K, V> extends DBRocksDBTable<K, V>
        implements Transacted {

    public DBRocksDBTableDBCommitedAsBath(Byteable byteableKey, Byteable byteableValue, String NAME_TABLE, List<IndexDB> indexes,
                                          RocksDbSettings settings, WriteOptions writeOptions, DBASet dbaSet) {
        super(byteableKey, byteableValue, NAME_TABLE, indexes, settings, writeOptions, dbaSet);
        openSource();
        afterOpen();
    }

    public DBRocksDBTableDBCommitedAsBath(Byteable byteableKey, Byteable byteableValue, String NAME_TABLE, List<IndexDB> indexes,
                                          DBASet dbaSet) {
        super(byteableKey, byteableValue, NAME_TABLE, indexes, dbaSet);
    }

    public DBRocksDBTableDBCommitedAsBath(String NAME_TABLE) {
        this(new ByteableTrivial(), new ByteableTrivial(), NAME_TABLE,
                new ArrayList<>(), RocksDbSettings.getDefaultSettings(),
                new WriteOptions().setSync(true).setDisableWAL(false), null);
    }

    @Override
    public void openSource() {
        dbSource = new RocksDbDataSourceDBCommitAsBath(this.root, NAME_TABLE, indexes, settings, writeOptions);

    }

    /*
    @Override
    public int size() {
        return super.size() + ((Transacted) dbSource).parentSize();
    }
     */

    @Override
    public int parentSize() {
        return dbSource.size();
    }

    @Override
    public void commit() {
        ///super.setSize(super.size() + ((Transacted) dbSource).parentSize());
        ((Transacted) dbSource).commit();
    }

    @Override
    public void rollback() {
        ((Transacted) dbSource).rollback();
    }
}
