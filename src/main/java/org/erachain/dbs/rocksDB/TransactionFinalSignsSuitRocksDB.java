package org.erachain.dbs.rocksDB;

import lombok.extern.slf4j.Slf4j;
import org.erachain.database.DBASet;
import org.erachain.datachain.TransactionFinalMapSignsSuit;
import org.erachain.dbs.rocksDB.common.RocksDbSettings;
import org.erachain.dbs.rocksDB.integration.DBRocksDBTable;
import org.erachain.dbs.rocksDB.transformation.ByteableLong;
import org.erachain.dbs.rocksDB.transformation.ByteableTrivial;
import org.mapdb.DB;

@Slf4j
public class TransactionFinalSignsSuitRocksDB extends DBMapSuit<byte[], Long> implements TransactionFinalMapSignsSuit {

    private final String NAME_TABLE = "TRANS_FINAL_SIGNS_TABLE";

    public TransactionFinalSignsSuitRocksDB(DBASet databaseSet, DB database) {
        super(databaseSet, database, logger);
    }

    @Override
    protected void getMap() {

        map = new DBRocksDBTable<>(new ByteableTrivial(), new ByteableLong(), NAME_TABLE, indexes,
                RocksDbSettings.initCustomSettings(7, 64, 32,
                        256, 10,
                        1, 256, 32, false),
                databaseSet);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void createIndexes() {
    }
}
