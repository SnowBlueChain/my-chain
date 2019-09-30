package org.erachain.dbs.rocksDB.common;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;

/**
 * Самый низкий уровень доступа к функциям RocksDB
 */
@Slf4j
@NoArgsConstructor
public class RocksDbComTransact implements RocksDbCom
{
    public Transaction dbCore;
    public TransactionDB dbCoreParent;
    WriteOptions writeOptions;
    ReadOptions readOptions;

    public RocksDbComTransact(TransactionDB dbCoreParent, WriteOptions writeOptions, ReadOptions readOptions) {
        this.dbCoreParent = dbCoreParent;
        this.writeOptions = writeOptions;
        this.readOptions = readOptions;
        dbCore = dbCoreParent.beginTransaction(writeOptions);
    }


    @Override
    public void put(byte[] key, byte[] value) throws RocksDBException {
        dbCore.put(key, value);
    }

    @Override
    public void put(ColumnFamilyHandle columnFamilyHandle, byte[] key, byte[] value) throws RocksDBException {
        dbCore.put(columnFamilyHandle, key, value);
    }

    @Override
    public void put(byte[] key, byte[] value, WriteOptions writeOptions) throws RocksDBException {
        dbCore.put(key, value);
    }

    @Override
    public byte[] get(byte[] key) throws RocksDBException {
        return dbCore.get(readOptions, key);
    }

    @Override
    public byte[] get(ColumnFamilyHandle columnFamilyHandle, byte[] key) throws RocksDBException {
        return dbCore.get(columnFamilyHandle, readOptions, key);
    }

    @Override
    public void remove(byte[] key) throws RocksDBException {
        dbCore.delete(key);
    }

    @Override
    public void remove(ColumnFamilyHandle columnFamilyHandle, byte[] key) throws RocksDBException {
        dbCore.delete(columnFamilyHandle, key);
    }

    @Override
    public void remove(byte[] key, WriteOptions writeOptions) throws RocksDBException {
        dbCore.delete(key);
    }

    @Override
    public RocksIterator getIterator() {
        return dbCore.getIterator(readOptions, dbCoreParent.getDefaultColumnFamily());
    }

    @Override
    public RocksIterator getIterator(ColumnFamilyHandle indexDB) {
        return dbCore.getIterator(readOptions, indexDB);
    }

    @Override
    public void close() {
        try {
            dbCore.commit();
            dbCore.close();
            writeOptions.dispose();
            dbCoreParent.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}