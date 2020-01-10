package org.erachain.dbs.rocksDB.transformation;

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.naming.Name;
import org.erachain.dbs.rocksDB.exceptions.WrongParseException;

@Slf4j
public class ByteableName implements Byteable<Name>{
    @Override
    public Name receiveObjectFromBytes(byte[] bytes) {
        try {
            return Name.Parse(bytes);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new WrongParseException(e);
        }
    }

    @Override
    public byte[] toBytesObject(Name value) {
        return value.toBytes();
    }
}