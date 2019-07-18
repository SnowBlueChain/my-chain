package org.erachain.gui.models;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.block.Block;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.erachain.utils.DateTimeFormat;
import org.erachain.utils.ObserverMessage;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("serial")
public class BlocksTableModel extends TimerTableModelCls<Block.BlockHead> {

    private static final int maxSize = 100;

    public static final int COLUMN_HEIGHT = 0;
    public static final int COLUMN_TARGET = 1;
    public static final int COLUMN_TIMESTAMP = 2;
    public static final int COLUMN_GENERATOR = 3;
    public static final int COLUMN_GB = 4;
    public static final int COLUMN_DH = 5;
    public static final int COLUMN_WV = 6;
    public static final int COLUMN_dtWV = 7;
    public static final int COLUMN_TRANSACTIONS = 8;
    public static final int COLUMN_FEE = 9;

    public BlocksTableModel() {
        super(new String[]{"Height", "Target", "Timestamp creation block", "Creator account", "Gen.Balance", "Delta Height", "WV", "dtWV", "Transactions", "Fee"}, false);

        addObservers();
        resetRows();

    }

    @Override
    public Object getValueAt(int row, int column) {
        try {
            if (list == null || list.size() - 1 < row) {
                return null;
            }
            Block.BlockHead block = list.get(row);
            Tuple2<Integer, Integer> forgingPoint = block.creator.getForgingData(DCSet.getInstance(), block.heightBlock);
            switch (column) {
                case COLUMN_HEIGHT:
                    return block.heightBlock + "";
                case COLUMN_TARGET:
                    return block.target + "";
                case COLUMN_TIMESTAMP:
                    return DateTimeFormat.timestamptoString(block.getTimestamp());
                case COLUMN_GENERATOR:
                    return block.creator.getPersonAsString();
                case COLUMN_GB:
                    if (block.target == 0) {
                        return "GENESIS";
                    }
                    return forgingPoint.b + " ";
                case COLUMN_DH:
                    return (block.heightBlock - forgingPoint.a) + "";
                case COLUMN_WV:
                    return block.winValue + "";
                case COLUMN_dtWV:
                    return String.format("%10.3f%%", (100f * (block.winValue - block.target) / block.target));
                case COLUMN_TRANSACTIONS:
                    return block.transactionsCount;
                case COLUMN_FEE:
                    return BigDecimal.valueOf(block.totalFee, BlockChain.FEE_SCALE);
            }

            return null;

        } catch (Exception e) {
            logger.error(e.getMessage() + "\n row:" + row, e);
            return null;
        }
    }

    @Override
    public synchronized void syncUpdate(Observable o, Object arg) {
        ObserverMessage message = (ObserverMessage) arg;
        int type = message.getType();
        if (type == ObserverMessage.CHAIN_LIST_BLOCK_TYPE) {
            //CHECK IF NEW LIST
            logger.error("gui.models.BlocksTableModel.syncUpdate- CHAIN_LIST_BLOCK_TYPE");
            if (list == null) {
                resetRows();
                fireTableDataChanged();
            }
        } else if (type == ObserverMessage.CHAIN_ADD_BLOCK_TYPE) {
            //CHECK IF LIST UPDATED
            Block block = (Block) message.getValue();
            list.add(0, block.blockHead);
            fireTableRowsInserted(0, 0);
            while (list.size() > maxSize) {
                list.remove(maxSize);
                fireTableRowsDeleted(maxSize, maxSize);
            }
        } else if (type == ObserverMessage.CHAIN_REMOVE_BLOCK_TYPE) {
            //CHECK IF LIST UPDATED
            try {
                list.remove(0);
            } catch (Exception e) {
                resetRows();
                fireTableDataChanged();
                return;
            }
            if (list.size() > 10) {
                fireTableRowsDeleted(0, 0);
            } else {
                resetRows();
                fireTableDataChanged();
            }
        } else if (type == ObserverMessage.CHAIN_RESET_BLOCK_TYPE) {
            //CHECK IF LIST UPDATED
            logger.error("gui.models.BlocksTableModel.syncUpdate- CHAIN_RESET_BLOCK_TYPE");
            resetRows();
            fireTableDataChanged();
        }
    }

    private void resetRows() {
        list = new ArrayList<>();
        Controller controller = Controller.getInstance();
//        DCSet dcSet = DCSet.getInstance();
        Block.BlockHead head = controller.getLastBlock().blockHead;
        int i = 0;
        while (i++ <= maxSize) {
            if (head == null) {
                return;
            }
            list.add(head);
            head = controller.getBlockHead(head.heightBlock - 1);
        }
    }

}
