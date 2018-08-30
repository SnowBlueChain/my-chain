package database.serializer;

import core.block.Block;
import core.block.BlockFactory;
import org.apache.log4j.Logger;
import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public class BlockHeadSerializer implements Serializer<Block.BlockHead>, Serializable {
    private static final long serialVersionUID = -6538913048331349777L;
    static Logger LOGGER = Logger.getLogger(BlockHeadSerializer.class.getName());

    @Override
    public void serialize(DataOutput out, Block.BlockHead value) throws IOException {
        out.writeInt(Block.BlockHead.BASE_LENGTH);
        out.write(value.toBytes());
    }

    @Override
    public Block.BlockHead deserialize(DataInput in, int available) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        try {
            return Block.BlockHead.parse(bytes);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
