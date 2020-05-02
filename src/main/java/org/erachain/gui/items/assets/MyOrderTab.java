package org.erachain.gui.items.assets;

import org.erachain.controller.Controller;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.transaction.CreateOrderTransaction;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.MainFrame;
import org.erachain.gui.SplitPanel;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.library.MTable;
import org.erachain.gui.library.SetIntervalPanel;
import org.erachain.gui.models.WalletOrdersTableModel;
import org.erachain.gui.transaction.CreateOrderDetailsFrame;
import org.erachain.gui2.MainPanel;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.TableMenuPopupUtil;
import org.mapdb.Fun;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MyOrderTab extends SplitPanel {

    private static String iconFile = Settings.getInstance().getPatnIcons() + "MyOrderTab.png";
    private static final long serialVersionUID = 1L;
    protected int row;
    /**
     *
     */
    WalletOrdersTableModel ordersModel;
    private SetIntervalPanel setIntervalPanel;

    @SuppressWarnings("rawtypes")
    public MyOrderTab() {
        super("MyOrderTab");
        this.setName(Lang.getInstance().translate("My Orders"));
        searthLabelSearchToolBarLeftPanel.setText(Lang.getInstance().translate("Search") + ":  ");
        // not show buttons
        button1ToolBarLeftPanel.setVisible(false);
        button2ToolBarLeftPanel.setVisible(false);
        jButton1_jToolBar_RightPanel.setVisible(false);
        jButton2_jToolBar_RightPanel.setVisible(false);

        // set interval panel
        setIntervalPanel = new SetIntervalPanel(Controller.getInstance().wallet.database.getOrderMap());
        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
        leftPanel.add(setIntervalPanel, gridBagConstraints);

        setIntervalPanel.jButtonSetInterval.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                setInterval();
            }
        });

        // TABLE
        ordersModel = new WalletOrdersTableModel();
        // set interval
        setInterval();
        // table = new MTable(ordersModel);

        // add listener
        // jTableJScrollPanelLeftPanel.getSelectionModel().addListSelectionListener(table);
        // show
        this.jTableJScrollPanelLeftPanel = new MTable(ordersModel);
        // this.jTableJScrollPanelLeftPanel = table;
        jTableJScrollPanelLeftPanel.getSelectionModel().addListSelectionListener(new search_listener());
        jTableJScrollPanelLeftPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point point = e.getPoint();
                int row = jTableJScrollPanelLeftPanel.rowAtPoint(point);
                jTableJScrollPanelLeftPanel.setRowSelectionInterval(row, row);

                Fun.Tuple2<Long, Order> itemTableSelected = ordersModel.getItem(row);

                if (e.getClickCount() == 2) {
                    tableMouse2Click(itemTableSelected.b);
                }

            }
        });

        jScrollPanelLeftPanel.setViewportView(jTableJScrollPanelLeftPanel);

        // UPDATE FILTER ON TEXT CHANGE
        searchTextFieldSearchToolBarLeftPanelDocument.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange();
            }

            public void onChange() {

                // SET FILTER

            }
        });

        // MENU
        JPopupMenu orderMenu = new JPopupMenu();
        orderMenu.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent arg0) {
                // TODO Auto-generated method stub
                row = jTableJScrollPanelLeftPanel.getSelectedRow();
                if (row < 1) {
                    return;
                }

                row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);

            }

            @Override
            public void ancestorMoved(AncestorEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void ancestorRemoved(AncestorEvent arg0) {
                // TODO Auto-generated method stub

            }

        });

        JMenuItem orderDetails = new JMenuItem(Lang.getInstance().translate("Details"));
        orderDetails.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (ordersModel.getSortableList().isEmpty())
                    return;

                Order order = ordersModel.getItem(row).b;
                Transaction createOrder = DCSet.getInstance().getTransactionFinalMap().get(order.getId());

                IssueConfirmDialog dd = new IssueConfirmDialog(MainFrame.getInstance(), true, createOrder,
                        (int) (MainFrame.getInstance().getWidth() / 1.2),
                        (int) (MainFrame.getInstance().getHeight() / 1.2),
                        "");

                CreateOrderDetailsFrame ww = new CreateOrderDetailsFrame((CreateOrderTransaction) createOrder);
                dd.jScrollPane1.setViewportView(ww);
                dd.setLocationRelativeTo(null);
                dd.pack();
                dd.setVisible(true);

            }
        });
        orderMenu.add(orderDetails);

        // MENU on MY ORDERS
        JMenuItem orderTrades = new JMenuItem(Lang.getInstance().translate("Trades"));
        orderTrades.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Order order = ordersModel.getItem(row).b;
                if (order != null)
                    new TradesFrame(order, false);
            }
        });
        orderMenu.add(orderTrades);

        JMenuItem cancelOrder = new JMenuItem(Lang.getInstance().translate("Cancel"));
        cancelOrder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Order order = ordersModel.getItem(row).b;
                new CancelOrderFrame(order);
            }
        });
        orderMenu.add(cancelOrder);

        JMenuItem exchange = new JMenuItem(Lang.getInstance().translate("Exchange"));
        exchange.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                tableMouse2Click(ordersModel.getItem(row).b);

            }
        });
        orderMenu.add(exchange);


        // table.setComponentPopupMenu(assetsMenu);
        TableMenuPopupUtil.installContextMenu(this.jTableJScrollPanelLeftPanel, orderMenu); // SELECT
                                                                                               // ROW
                                                                                               // ON
                                                                                               // WHICH
                                                                                               // CLICKED
                                                                                               // RIGHT
                                                                                               // BUTTON

        // MOUSE ADAPTER
        this.jTableJScrollPanelLeftPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int row = jTableJScrollPanelLeftPanel.rowAtPoint(p);
                jTableJScrollPanelLeftPanel.setRowSelectionInterval(row, row);
            }
        });

        this.jTableJScrollPanelLeftPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int row = jTableJScrollPanelLeftPanel.rowAtPoint(p);
                jTableJScrollPanelLeftPanel.setRowSelectionInterval(row, row);

                Order order = ordersModel.getItem(row).b;
                if (order.isActive(DCSet.getInstance()))
                    orderMenu.getComponent(2).setEnabled(true);
                else
                    orderMenu.getComponent(2).setEnabled(false);

                /*
                 * if(e.getClickCount() == 2) { row =
                 * table.convertRowIndexToModel(row); AssetCls asset =
                 * assetsModel.getAsset(row); new AssetFrame(asset); }
                 * if(e.getClickCount() == 1 & e.getButton() == e.BUTTON1) {
                 * 
                 * if (table.getSelectedColumn() ==
                 * WalletItemAssetsTableModel.COLUMN_FAVORITE){ row =
                 * table.convertRowIndexToModel(row); AssetCls asset =
                 * orderModel.getAsset(row); favoriteSet( table);
                 * 
                 * 
                 * 
                 * }
                 * 
                 * 
                 * }
                 */
            }
        });

    }

    // CreateOrderDetailsFrame
    // listener select row
    class search_listener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent arg0) {
            try {
                Order order = null;
                int i = 0;
                if (jTableJScrollPanelLeftPanel.getSelectedRow() >= 0)
                    i = jTableJScrollPanelLeftPanel
                            .convertRowIndexToModel(jTableJScrollPanelLeftPanel.getSelectedRow());
                order = ordersModel.getItem(i).b;
                if (order == null)
                    return;
                jScrollPaneJPanelRightPanel.setViewportView(new OrderInfoPanel(order));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void setInterval() {
        Integer start = 0;
        try {
            start = Integer.valueOf(setIntervalPanel.jTextFieldStart.getText());
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            return;
        }
        Integer end = 0;
        try {
            end = Integer.valueOf(setIntervalPanel.jTextFieldEnd.getText());
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            return;
        }
        if (end > start) {
            int step = end - start;
            ordersModel.setInterval(start, step);
            ordersModel.fireTableDataChanged();
        }
    }
    
    @Override
    public void onClose() {
        ordersModel.deleteObservers();
        setIntervalPanel.deleteObservers();
        
    }

    protected void tableMouse2Click(Order order) {

        String action = null;
        AssetCls haveAsset = Controller.getInstance().getAsset(order.getHaveAssetKey());
        AssetCls wantAsset = Controller.getInstance().getAsset(order.getWantAssetKey());
        ExchangePanel panel = new ExchangePanel(haveAsset, wantAsset, action, "");
        panel.setName(haveAsset.getTickerName() + "/" + wantAsset.getTickerName());
        MainPanel.getInstance().insertTab(Lang.getInstance().translate("Exchange"),
                panel, ExchangePanel.getIcon());
    }

    public static Image getIcon() {
        {
            try {
                return Toolkit.getDefaultToolkit().getImage(iconFile);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
