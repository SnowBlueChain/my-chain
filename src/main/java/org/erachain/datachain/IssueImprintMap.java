package org.erachain.datachain;

import com.google.common.primitives.UnsignedBytes;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;

/**
 * see datachain.IssueItemMap
 */

public class IssueImprintMap extends IssueItemMap {

    public IssueImprintMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);
    }

    public IssueImprintMap(IssueImprintMap parent, DCSet dcSet) {
        super(parent, dcSet);
    }

    @Override
    public void openMap() {
        //OPEN MAP
        map = database.createTreeMap("imprint_OrphanData")
                .keySerializer(BTreeKeySerializer.BASIC)
                .comparator(UnsignedBytes.lexicographicalComparator())
                .counterEnable()
                .makeOrGet();
    }

}
