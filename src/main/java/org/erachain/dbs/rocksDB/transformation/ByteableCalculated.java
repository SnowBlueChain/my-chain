package org.erachain.rocksDB.transformation;

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.transCalculated.Calculated;
import org.erachain.core.transCalculated.CalculatedFactory;
import org.erachain.rocksDB.exceptions.WrongParseException;

@Slf4j
public class ByteableCalculated implements Byteable<Calculated> {
    @Override
    public Calculated receiveObjectFromBytes(byte[] bytes) {
        try {
            return  CalculatedFactory.getInstance().parse(bytes);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new WrongParseException(e);
        }
    }

    @Override
    public byte[] toBytesObject(Calculated value) {
        return value.toBytes();
    }
}
