package org.erachain.datachain;

import com.google.common.primitives.UnsignedBytes;
import org.erachain.core.transaction.Transaction;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;

import java.util.TreeMap;

public class DeployATMap extends DCUMap<byte[], Long> {

    public DeployATMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);
    }

    public DeployATMap(DeployATMap parent) {
        super(parent, null);
    }

    protected void createIndexes() {
    }

    @Override
    protected void openMap() {
        //OPEN MAP
        map = database.createTreeMap("DeployATOrphanData")
                .keySerializer(BTreeKeySerializer.BASIC)
                .comparator(UnsignedBytes.lexicographicalComparator())
                .makeOrGet();
    }

    @Override
    protected void getMemoryMap() {
        map = new TreeMap<byte[], Long>(UnsignedBytes.lexicographicalComparator());
    }

    @Override
    protected Long getDefaultValue() {
        return -1l;
    }

    public Long get(Transaction transaction) {
        return this.get(transaction.getSignature());
    }

    public void set(Transaction transaction, Long key) {
        this.set(transaction.getSignature(), key);
    }

    public void delete(Transaction transaction) {
        this.remove(transaction.getSignature());
    }
}
