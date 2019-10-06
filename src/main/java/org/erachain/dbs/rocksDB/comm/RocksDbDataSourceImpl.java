package org.erachain.dbs.rocksDB.comm;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.erachain.dbs.rocksDB.common.RockStoreIterator;
import org.erachain.dbs.rocksDB.common.RockStoreIteratorFilter;
import org.erachain.dbs.rocksDB.common.RocksDbSettings;
import org.erachain.dbs.rocksDB.indexes.IndexDB;
import org.erachain.dbs.rocksDB.utils.ByteUtil;
import org.erachain.dbs.rocksDB.utils.FileUtil;
import org.erachain.settings.Settings;
import org.mapdb.Fun;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.erachain.dbs.rocksDB.utils.ConstantsRocksDB.ROCKS_DB_FOLDER;

/**
 * Самый низкий уровень доступа к функциям RocksDB
 */
@Slf4j
public abstract class RocksDbDataSourceImpl implements RocksDbDataSource
        // DB<byte[], byte[]>, Flusher, DbSourceInter<byte[]>
{
    protected String dataBaseName;
    public static final byte[] SIZE_BYTE_KEY = new byte[]{0};

    //Глеб * эта переменная позаимствована из проекта "tron" нужна для создания каких-то настроек
    // Это включает логирование данных на диск синхронизированно - защищает от утрат при КРАХЕ но чуть медленне работает
    // Если ЛОЖЬ то данные утрачиваются при КРАХЕ
    //protected boolean dbSync = true;

    public WriteOptions writeOptions;

    /**
     * Base RocksDB
     */
    @Getter
    public RocksDB dbCore;
    Options options;

    protected boolean alive;
    protected String pathName;
    List<IndexDB> indexes;
    RocksDbSettings settings;
    protected boolean create = false;
    protected String sizeDescriptorName = "size";
    @Getter
    protected List<ColumnFamilyHandle> columnFamilyHandles;

    protected ColumnFamilyHandle columnFamilyFieldSize;

    protected ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    public RocksDbDataSourceImpl(RocksDB dbCore,
                                 String pathName, String name, List<IndexDB> indexes, RocksDbSettings settings,
                                 WriteOptions writeOptions) {
        this.dataBaseName = name;
        this.pathName = pathName;
        this.indexes = indexes;
        this.settings = settings;
        this.writeOptions = writeOptions;
        this.dbCore = dbCore;
    }

    public RocksDbDataSourceImpl(String pathName, String name, List<IndexDB> indexes, RocksDbSettings settings,
                                 WriteOptions writeOptions) {
        this.dataBaseName = name;
        this.pathName = pathName;
        this.indexes = indexes;
        this.settings = settings;
        this.writeOptions = writeOptions;
    }

    public RocksDbDataSourceImpl(String pathName, String name, List<IndexDB> indexes, RocksDbSettings settings) {
        this(pathName, name, indexes, settings, new WriteOptions().setSync(true).setDisableWAL(false));
    }

    public RocksDbDataSourceImpl(String name, List<IndexDB> indexes, RocksDbSettings settings) {
        this(Settings.getInstance().getDataDir() + ROCKS_DB_FOLDER, name, indexes, settings);
    }

    abstract protected void createDB(Options options, List<ColumnFamilyDescriptor> columnFamilyDescriptors) throws RocksDBException;

    abstract protected void openDB(DBOptions dbOptions, List<ColumnFamilyDescriptor> columnFamilyDescriptors) throws RocksDBException;

    public void initDB() {
        resetDbLock.writeLock().lock();
        try {
            if (isAlive()) {
                return;
            }

            Preconditions.checkNotNull(dataBaseName, "no name set to the dbStore");
            try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
                final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
                columnFamilyHandles = new ArrayList<>();
                addIndexColumnFamilies(indexes, cfOpts, columnFamilyDescriptors);
                BlockBasedTableConfig dbCoreCfgCf = settingsBlockBasedTable(settings);
                cfOpts.setTableFormatConfig(dbCoreCfgCf);
                try (Options options = new Options()) {

                    if (settings.isEnableStatistics()) {
                        options.setStatistics(new Statistics());
                        options.setStatsDumpPeriodSec(60);
                    }
                    options.setCreateIfMissing(true);
                    options.setIncreaseParallelism(3);
                    options.setLevelCompactionDynamicLevelBytes(true);
                    options.setMaxOpenFiles(settings.getMaxOpenFiles());

                    options.setNumLevels(settings.getLevelNumber());
                    options.setMaxBytesForLevelMultiplier(settings.getMaxBytesForLevelMultiplier());
                    options.setMaxBytesForLevelBase(settings.getMaxBytesForLevelBase());
                    options.setMaxBackgroundCompactions(settings.getCompactThreads());
                    options.setLevel0FileNumCompactionTrigger(settings.getLevel0FileNumCompactionTrigger());
                    options.setTargetFileSizeMultiplier(settings.getTargetFileSizeMultiplier());
                    options.setTargetFileSizeBase(settings.getTargetFileSizeBase());

                    BlockBasedTableConfig dbCoreCfg = settingsBlockBasedTable(settings);
                    options.setTableFormatConfig(dbCoreCfg);
                    options.setAllowConcurrentMemtableWrite(true);
                    options.setMaxManifestFileSize(0);
                    options.setWalTtlSeconds(0);
                    options.setWalSizeLimitMB(0);
                    options.setLevel0FileNumCompactionTrigger(1);
                    options.setMaxBackgroundFlushes(4);
                    options.setMaxBackgroundCompactions(8);
                    options.setMaxSubcompactions(4);
                    options.setMaxWriteBufferNumber(3);
                    options.setMinWriteBufferNumberToMerge(2);

                    int dbWriteBufferSize = 512 * 1024 * 1024;
                    options.setDbWriteBufferSize(dbWriteBufferSize);
                    options.setParanoidFileChecks(false);
                    options.setCompressionType(CompressionType.NO_COMPRESSION);

                    options.setParanoidChecks(false);
                    options.setAllowMmapReads(true);
                    options.setAllowMmapWrites(true);
                    try {
                        logger.info("Opening database");
                        final Path dbPath = getDbPathAndFile();
                        if (!Files.isSymbolicLink(dbPath.getParent())) {
                            Files.createDirectories(dbPath.getParent());
                        }

                        try (final DBOptions dbOptions = new DBOptions()) {
                            try {

                                // MAKE DATABASE
                                // USE transactions
                                createDB(options, columnFamilyDescriptors);

                                // MAKE COLUMNS FAMILY
                                columnFamilyHandles.add(dbCore.getDefaultColumnFamily());
                                for (ColumnFamilyDescriptor columnFamilyDescriptor : columnFamilyDescriptors) {
                                    ColumnFamilyHandle columnFamilyHandle = dbCore.createColumnFamily(columnFamilyDescriptor);
                                    columnFamilyHandles.add(columnFamilyHandle);
                                }

                                create = true;
                                logger.info("database created");
                            } catch (RocksDBException e) {
                                dbOptions.setCreateIfMissing(true);
                                dbOptions.setCreateMissingColumnFamilies(true);
                                dbOptions.setIncreaseParallelism(3);
                                dbOptions.setMaxOpenFiles(settings.getMaxOpenFiles());
                                dbOptions.setMaxBackgroundCompactions(settings.getCompactThreads());
                                dbOptions.setAllowConcurrentMemtableWrite(true);
                                dbOptions.setMaxManifestFileSize(0);
                                dbOptions.setWalTtlSeconds(0);
                                dbOptions.setWalSizeLimitMB(0);
                                dbOptions.setMaxBackgroundFlushes(4);
                                dbOptions.setMaxBackgroundCompactions(8);
                                dbOptions.setMaxSubcompactions(4);
                                dbOptions.setDbWriteBufferSize(dbWriteBufferSize);

                                dbOptions.setParanoidChecks(false);
                                dbOptions.setAllowMmapReads(true);
                                dbOptions.setAllowMmapWrites(true);

                                columnFamilyDescriptors.clear();
                                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts));
                                addIndexColumnFamilies(indexes, cfOpts, columnFamilyDescriptors);

                                // MAKE DATABASE
                                // USE transactions
                                openDB(dbOptions, columnFamilyDescriptors);

                                if (indexes != null && !indexes.isEmpty()) {
                                    for (int i = 0; i < indexes.size(); i++) {
                                        indexes.get(i).setColumnFamilyHandle(columnFamilyHandles.get(i));
                                    }
                                }

                                logger.info("database opened");
                            }

                        } catch (RocksDBException e) {
                            logger.error(e.getMessage(), e);
                            throw new RuntimeException("Failed to initialize database", e);
                        }
                        alive = true;
                    } catch (IOException ioe) {
                        logger.error(ioe.getMessage(), ioe);
                        throw new RuntimeException("Failed to initialize database", ioe);
                    }

                    columnFamilyFieldSize = columnFamilyHandles.get(columnFamilyHandles.size() - 1);

                    if (create) {
                        // Нужно для того чтобы в базе дае у транзакционных был Размер уже
                        try {
                            dbCore.put(columnFamilyFieldSize, SIZE_BYTE_KEY, new byte[]{0, 0, 0, 0});
                        } catch (RocksDBException edb) {
                        }
                    }

                    logger.info("RocksDbDataSource.initDB(): " + dataBaseName);
                }
            }
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    @Override
    public Path getDbPathAndFile() {
        return Paths.get(pathName, dataBaseName);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void close() {
        resetDbLock.writeLock().lock();
        try {
            if (!isAlive()) {
                return;
            }
            alive = false;
            dbCore.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    protected boolean quitIfNotAlive() {
        if (!isAlive()) {
            logger.warn("db is not alive");
        }
        return !isAlive();
    }

    @Override
    public Set<byte[]> keySet() throws RuntimeException {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        Set<byte[]> result = new TreeSet(Fun.BYTE_ARRAY_COMPARATOR);
        try (final RocksIterator iter = dbCore.newIterator()) {
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                result.add(iter.key());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> values() throws RuntimeException {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        List<byte[]> result = new ArrayList<byte[]>();
        try (final RocksIterator iter = dbCore.newIterator()) {
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public RocksIterator getIterator() {
        if (quitIfNotAlive()) {
            return null;
        }

        return dbCore.newIterator();
    }

    @Override
    public RocksIterator getIterator(ColumnFamilyHandle indexDB) {
        if (quitIfNotAlive()) {
            return null;
        }

        return dbCore.newIterator(indexDB);
    }

    @Override
    public Set<byte[]> filterApprropriateKeys(byte[] filter) throws RuntimeException {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        Set<byte[]> result = new TreeSet<>(Fun.BYTE_ARRAY_COMPARATOR);
        try (final RocksIterator iter = dbCore.newIterator()) {
            for (iter.seek(filter); iter.isValid() && new String(iter.key()).startsWith(new String(filter)); iter.next()) {
                result.add(iter.key());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> filterApprropriateValues(byte[] filter) throws RuntimeException {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        List<byte[]> result = new ArrayList<byte[]>();
        try (final RocksIterator iter = dbCore.newIterator()) {
            for (iter.seek(filter); iter.isValid() && new String(iter.key()).startsWith(new String(filter)); iter.next()) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> filterApprropriateValues(byte[] filter, ColumnFamilyHandle indexDB) throws RuntimeException {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        List<byte[]> result = new ArrayList<byte[]>();
        try (final RocksIterator iter = dbCore.newIterator(indexDB)) {
            for (iter.seek(filter); iter.isValid() && new String(iter.key()).startsWith(new String(filter)); iter.next()) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> filterApprropriateValues(byte[] filter, int indexDB) throws RuntimeException {
        return filterApprropriateValues(filter, columnFamilyHandles.get(indexDB));
    }

    @Override
    public String getDBName() {
        return dataBaseName;
    }

    private BlockBasedTableConfig settingsBlockBasedTable(RocksDbSettings settings) {
        BlockBasedTableConfig dbCoreCfg = new BlockBasedTableConfig();
        dbCoreCfg.setBlockCache(new LRUCache(128 << 20));
        dbCoreCfg.setBlockSize(settings.getBlockSize());
        dbCoreCfg.setBlockSize(1024);
        dbCoreCfg.setBlockCacheSize(settings.getBlockCacheSize());
        dbCoreCfg.setBlockCacheSize(64 << 20);
        dbCoreCfg.setCacheIndexAndFilterBlocks(true);
        dbCoreCfg.setPinL0FilterAndIndexBlocksInCache(true);
        dbCoreCfg.setFilter(new BloomFilter(10, false));
        return dbCoreCfg;
    }

    private void addIndexColumnFamilies(List<IndexDB> indexes, ColumnFamilyOptions cfOpts, List<ColumnFamilyDescriptor> columnFamilyDescriptors) {
        if (indexes != null) {
            for (IndexDB index : indexes) {
                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(index.getNameIndex().getBytes(StandardCharsets.UTF_8), cfOpts));
            }
            columnFamilyDescriptors.add(new ColumnFamilyDescriptor(sizeDescriptorName.getBytes(StandardCharsets.UTF_8), cfOpts));
        }
    }

    @Override
    public void put(byte[] key, byte[] value) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.put(key, value);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void put(ColumnFamilyHandle columnFamilyHandle, byte[] key, byte[] value) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.put(columnFamilyHandle, key, value);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void put(byte[] key, byte[] value, WriteOptions writeOptions) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.put(writeOptions, key, value);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    final StringBuilder inCache = new StringBuilder();

    @Override
    public boolean contains(byte[] key) {
        if (quitIfNotAlive()) {
            return false;
        }
        resetDbLock.readLock().lock();
        try {
            return dbCore.keyMayExist(key, inCache);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
        return false;
    }

    @Override
    public boolean contains(ColumnFamilyHandle columnFamilyHandle, byte[] key) {
        if (quitIfNotAlive()) {
            return false;
        }
        resetDbLock.readLock().lock();
        try {
            return dbCore.keyMayExist(columnFamilyHandle, key, inCache);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
        return false;
    }

    @Override
    public byte[] get(byte[] key) {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        try {
            return dbCore.get(key);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public byte[] get(ColumnFamilyHandle columnFamilyHandle, byte[] key) {
        if (quitIfNotAlive()) {
            return null;
        }
        resetDbLock.readLock().lock();
        try {
            return dbCore.get(columnFamilyHandle, key);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void remove(byte[] key) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.delete(key);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(ColumnFamilyHandle columnFamilyHandle, byte[] key) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.delete(columnFamilyHandle, key);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] key, WriteOptions writeOptions) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            dbCore.delete(writeOptions, key);
        } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public RockStoreIterator iterator(boolean descending) {
        return new RockStoreIterator(dbCore.newIterator(), descending, false);
    }

    @Override
    public RockStoreIterator indexIterator(boolean descending, ColumnFamilyHandle columnFamilyHandle) {
        return new RockStoreIterator(dbCore.newIterator(columnFamilyHandle), descending, true);
    }

    @Override
    public RockStoreIteratorFilter indexIteratorFilter(boolean descending, byte[] filter) {
        return new RockStoreIteratorFilter(dbCore.newIterator(), descending, true, filter);
    }

    @Override
    public RockStoreIteratorFilter indexIteratorFilter(boolean descending, ColumnFamilyHandle columnFamilyHandle, byte[] filter) {
        return new RockStoreIteratorFilter(dbCore.newIterator(columnFamilyHandle), descending, true, filter);
    }

    @Override
    public RockStoreIterator indexIterator(boolean descending, int indexDB) {
        return new RockStoreIterator(dbCore.newIterator(columnFamilyHandles.get(indexDB)), descending, true);
    }

    private void updateByBatchInner(Map<byte[], byte[]> rows) throws Exception {
        if (quitIfNotAlive()) {
            return;
        }
        try (WriteBatch batch = new WriteBatch()) {
            for (Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            dbCore.write(new WriteOptions(), batch);
        }
    }

    private void updateByBatchInner(Map<byte[], byte[]> rows, WriteOptions options)
            throws Exception {
        if (quitIfNotAlive()) {
            return;
        }
        try (WriteBatch batch = new WriteBatch()) {
            for (Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            dbCore.write(options, batch);
        }
    }

    @Override
    public void updateByBatch(Map<byte[], byte[]> rows) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            updateByBatchInner(rows);
        } catch (Exception e) {
            try {
                updateByBatchInner(rows);
            } catch (Exception e1) {
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void updateByBatch(Map<byte[], byte[]> rows, WriteOptions writeOptions) {
        if (quitIfNotAlive()) {
            return;
        }
        resetDbLock.readLock().lock();
        try {
            updateByBatchInner(rows, writeOptions);
        } catch (Exception e) {
            try {
                updateByBatchInner(rows, writeOptions);
            } catch (Exception e1) {
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    /**
     * Unsorted!
     *
     * @param key
     * @param limit
     * @return
     */
    @Override
    public Map<byte[], byte[]> getNext(byte[] key, long limit) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return Collections.emptyMap();
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator()) {
            Map<byte[], byte[]> result = new HashMap<>();
            long i = 0;
            for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
                result.put(iter.key(), iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> getLatestValues(long limit) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return new ArrayList<>();
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator()) {
            List<byte[]> result = new ArrayList<>();
            long i = 0;
            for (iter.seekToLast(); iter.isValid() && i < limit; iter.prev(), i++) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> getValuesPrevious(byte[] key, long limit) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return new ArrayList<>();
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator()) {
            List<byte[]> result = new ArrayList<>();
            long i = 0;
            byte[] data = get(key);
            if (Objects.nonNull(data)) {
                result.add(data);
                i++;
            }
            for (iter.seekForPrev(key); iter.isValid() && i < limit; iter.prev(), i++) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public List<byte[]> getValuesNext(byte[] key, long limit) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return new ArrayList<>();
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator()) {
            List<byte[]> result = new ArrayList<>();
            long i = 0;
            for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Set<byte[]> getKeysNext(byte[] key, long limit) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return new TreeSet<>(Fun.BYTE_ARRAY_COMPARATOR);
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator()) {
            Set<byte[]> result = new TreeSet<>(Fun.BYTE_ARRAY_COMPARATOR);
            long i = 0;
            for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
                result.add(iter.key());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Set<byte[]> getKeysNext(byte[] key, long limit, ColumnFamilyHandle columnFamilyHandle) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0) {
            return new TreeSet(Fun.BYTE_ARRAY_COMPARATOR);
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iter = dbCore.newIterator(columnFamilyHandle)) {
            Set<byte[]> result = new TreeSet<>(Fun.BYTE_ARRAY_COMPARATOR);
            long i = 0;
            for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
                result.add(iter.value());
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    /**
     * Unsorted!
     *
     * @param key
     * @param limit
     * @param precision
     * @return
     */
    @Override
    public Map<byte[], byte[]> getPrevious(byte[] key, long limit, int precision) {
        if (quitIfNotAlive()) {
            return null;
        }
        if (limit <= 0 || key.length < precision) {
            return Collections.emptyMap();
        }
        resetDbLock.readLock().lock();
        try (RocksIterator iterator = dbCore.newIterator()) {
            Map<byte[], byte[]> result = new HashMap<>();
            long i = 0;
            for (iterator.seekToFirst(); iterator.isValid() && i++ < limit; iterator.next()) {
                if (iterator.key().length >= precision) {
                    if (ByteUtil.less(ByteUtil.parseBytes(key, 0, precision),
                            ByteUtil.parseBytes(iterator.key(), 0, precision))) {
                        break;
                    }
                    result.put(iterator.key(), iterator.value());
                }
            }
            return result;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void backup(String dir) throws RocksDBException {
        Checkpoint cp = Checkpoint.create(dbCore);
        cp.createCheckpoint(dir + getDBName());
    }

    @Override
    public boolean deleteDbBakPath(String dir) {
        return FileUtil.deleteDir(new File(dir + getDBName()));
    }

    @Override
    public int size() {
        byte[] sizeBytes = get(columnFamilyFieldSize, SIZE_BYTE_KEY);
        return Ints.fromBytes(sizeBytes[0], sizeBytes[1], sizeBytes[2], sizeBytes[3]);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void flush(Map<byte[], byte[]> rows) {
        updateByBatch(rows, writeOptions);

    }

    @Override
    public void flush() throws RocksDBException {
        FlushOptions flushOptions = new FlushOptions();
        dbCore.flush(flushOptions);
    }

}