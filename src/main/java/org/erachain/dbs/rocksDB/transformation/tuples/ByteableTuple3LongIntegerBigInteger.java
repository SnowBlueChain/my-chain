package org.erachain.dbs.rocksDB.transformation.tuples;

import org.erachain.dbs.rocksDB.transformation.*;

public class ByteableTuple3LongIntegerBigInteger extends ByteableTuple3<String, Long, String> {

    public ByteableTuple3LongIntegerBigInteger() {
        setByteables(new Byteable[]{new ByteableLong(),new ByteableInteger(),new ByteableBigInteger()});
    }

    @Override
    public int[] sizeElements() {
        return new int[]{Long.BYTES,Integer.BYTES};
    }


}
