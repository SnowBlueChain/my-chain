package org.erachain.gui.library;

import org.erachain.controller.Controller;
import org.erachain.core.item.assets.Order;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.wallet.Wallet;
import org.erachain.database.DBMap;
import org.erachain.database.SortableList;
import org.erachain.database.wallet.DWSet;
import org.erachain.gui.Gui;
import org.erachain.lang.Lang;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.Pair;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class SetIntervalPanel extends JPanel implements Observer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    DBMap map;
    private long size;
    private boolean needUpdate;

    static Logger LOGGER = LoggerFactory.getLogger(SetIntervalPanel.class.getName());

    /**
     * В динамическом режиме перерисовывается при каждом прилете записи.<br>
     * Без динамического режима перерисовывается по внешнему таймеру из
     * gui.GuiTimer - только если было обновление
     */
    public SetIntervalPanel(int type) {
        this.type=type;
        jLabelTotal = new JLabel();
        addObservers();
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabelStart = new javax.swing.JLabel();
        jTextFieldStart = new javax.swing.JTextField();
        jLabelEnd = new javax.swing.JLabel();
        jTextFieldEnd = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

       // jLabelStart.setText(Lang.getInstance().translate("Interval") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(6, 5, 8, 0);
        add(jLabelStart, gridBagConstraints);

        jTextFieldStart.setText("0");
        jTextFieldStart.setMinimumSize(new java.awt.Dimension(50, 20));
        jTextFieldStart.setName(""); // NOI18N
        jTextFieldStart.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 8, 0);
        add(jTextFieldStart, gridBagConstraints);

        jLabelEnd.setText(Lang.getInstance().translate("") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(6, 7, 8, 0);
        add(jLabelEnd, gridBagConstraints);

        jTextFieldEnd.setText("50");
        jTextFieldEnd.setMinimumSize(new java.awt.Dimension(50, 20));
        jTextFieldEnd.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 8, 0);
        add(jTextFieldEnd, gridBagConstraints);

        jButtonSetInterval = new javax.swing.JButton();
        jButtonSetInterval.setText(Lang.getInstance().translate("View"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 5, 5, 0);
        add(jButtonSetInterval, gridBagConstraints);

       
      //  jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" );
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 5, 5, 0);
        add(jLabelTotal, gridBagConstraints);
    }// </editor-fold>

    // Variables declaration - do not modify
    private javax.swing.JLabel jLabelEnd;
    private javax.swing.JLabel jLabelStart;
    public javax.swing.JTextField jTextFieldEnd;
    public javax.swing.JTextField jTextFieldStart;
    public javax.swing.JButton jButtonSetInterval;
    JLabel jLabelTotal;
    //private SortableList<Tuple2<String, Long>, Order> orders;
    //private SortableList<Tuple2<String, String>, Transaction> transactions;
    public int type;

    // End of variables declaration
    @Override
    public void update(Observable arg0, Object arg1) {
        // TODO Auto-generated method stub
        try {
            this.syncUpdate(arg0, arg1);
        } catch (Exception e) {
            // GUI ERROR
            LOGGER.error(e.getMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    public synchronized void syncUpdate(Observable o, Object arg) {
        ObserverMessage message = (ObserverMessage) arg;

        // CHECK IF NEW LIST

        // order transactions
        if (type == Transaction.CREATE_ORDER_TRANSACTION) {

            if (message.getType() == ObserverMessage.GUI_REPAINT) {
                // это режим нединамического отображения - раз в 2 сек делаем

                if (!needUpdate)
                    return;

                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);
                needUpdate = true;

            } else if (message.getType() == ObserverMessage.WALLET_RESET_ORDER_TYPE) {

                size = 0;

                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);

            } else if (message.getType() == ObserverMessage.WALLET_LIST_ORDER_TYPE) {

                Collection list = (Collection) message.getValue();
                size = list.size();

                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);

            } else if (message.getType() == ObserverMessage.WALLET_COUNT_ORDER_TYPE) {

                if (map == null)
                    map = (DBMap) message.getValue();

                size = map.size();
                needUpdate = true;

            } else if (message.getType() == ObserverMessage.WALLET_ADD_ORDER_TYPE) {
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + ++size);

            } else if (message.getType() == ObserverMessage.WALLET_REMOVE_ORDER_TYPE) {
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + --size);

            }
        // all transactions
        } else if (type  == Transaction.EXTENDED) {

            if (message.getType() == ObserverMessage.GUI_REPAINT) {
                // это режим нединамического отображения - раз в 2 сек делаем
                if (!needUpdate)
                    return;

                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);
                needUpdate = true;

            } else if (message.getType() == ObserverMessage.WALLET_RESET_TRANSACTION_TYPE) {
                size = 0;
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);

            } else if (message.getType() == ObserverMessage.WALLET_LIST_TRANSACTION_TYPE) {
                Collection list = (Collection) message.getValue();
                size = list.size();
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + size);

            } else if (message.getType() == ObserverMessage.WALLET_COUNT_TRANSACTION_TYPE) {

                if (map == null)
                    map = (DBMap) message.getValue();

                size = map.size();
                needUpdate = true;

            } else if (message.getType() == ObserverMessage.WALLET_ADD_TRANSACTION_TYPE) {
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + ++size);
            } else if (message.getType() == ObserverMessage.WALLET_REMOVE_TRANSACTION_TYPE) {
                jLabelTotal.setText(Lang.getInstance().translate("Total") + ":" + --size);

            }

        }

    }

    public void addObservers() {
        Controller.getInstance().addWalletListener(this);
        Controller.getInstance().guiTimer.addTimerObserver(this); // нужно для перерисовки раз в 2 сек

    }
    public void removeObservers() {
        Controller.getInstance().deleteWalletObserver(this);
        Controller.getInstance().guiTimer.removeTimerObserver(this); // нужно для перерисовки раз в 2 сек
    }

}
