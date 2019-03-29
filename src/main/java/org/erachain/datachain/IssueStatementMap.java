package org.erachain.datachain;

import java.util.Map;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;

import com.google.common.primitives.UnsignedBytes;

/**
 * see datachain.IssueItemMap
 */

public class IssueStatementMap extends IssueItemMap {

    public IssueStatementMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);
    }

    public IssueStatementMap(IssueStatementMap parent) {
        super(parent);
    }

    @Override
    protected Map<byte[], Long> getMap(DB database) {
        //OPEN MAP
        return database.createTreeMap("statement_OrphanData")
                .keySerializer(BTreeKeySerializer.BASIC)
                .comparator(UnsignedBytes.lexicographicalComparator())
                .counterEnable()
                .makeOrGet();
    }

}
