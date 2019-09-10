package org.erachain.rocksDB.transformation;

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.block.Block;
import org.erachain.rocksDB.exceptions.WrongParseException;

@Slf4j
public class ByteableBlockHead implements Byteable<Block.BlockHead> {
    @Override
    public Block.BlockHead receiveObjectFromBytes(byte[] bytes) {
        try {
            return Block.BlockHead.parse(bytes);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new WrongParseException(e);
        }
    }

    @Override
    public byte[] toBytesObject(Block.BlockHead value) {
        return value.toBytes();
    }
}
