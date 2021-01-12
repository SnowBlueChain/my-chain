package org.erachain.gui.items.records;

import org.erachain.controller.Controller;
import org.erachain.core.exdata.ExData;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.MainFrame;
import org.erachain.gui.SplitPanel;
import org.erachain.gui.items.statement.IssueDocumentPanel;
import org.erachain.gui.library.ASMakeHashMenuItem;
import org.erachain.gui.library.Library;
import org.erachain.gui.library.MTable;
import org.erachain.gui.library.SignLibraryPanel;
import org.erachain.gui.models.SearchTransactionsTableModel;
import org.erachain.gui.records.toSignRecordDialog;
import org.erachain.gui.transaction.TransactionDetailsFactory;
import org.erachain.gui2.MainPanel;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.MenuPopupUtil;
import org.erachain.utils.TableMenuPopupUtil;
import org.erachain.utils.URLViewer;
import org.mapdb.Fun.Tuple2;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * search transactions
 */
public class SearchTransactionsSplitPanel extends SplitPanel {

    public static String NAME = "SearchTransactionsSplitPanel";
    public static String TITLE = "Search Transactions";

    public JPanel info_Panel;
    public SignLibraryPanel voush_Library_Panel;
    SearchTransactionsTableModel transactionsTableModel;
    JScrollPane jScrollPane4;
    private JTextField searchString;

    public SearchTransactionsSplitPanel() {
        super(NAME, TITLE);

        this.searchToolBar_LeftPanel.setVisible(true);
        jScrollPane4 = new JScrollPane();

        this.searthLabelSearchToolBarLeftPanel.setText(Lang.T("Height or seqNo") + ": ");
        this.toolBarLeftPanel.add(new JLabel(Lang.T("Search") + ": "));
        searchString = new JTextField();
        searchString.setToolTipText("");

        searchString.setMinimumSize(new java.awt.Dimension(350, UIManager.getFont("Label.font").getSize() + UIManager.getFont("Label.font").getSize() / 2));
        searchString.setName(""); // NOI18N
        searchString.setPreferredSize(new java.awt.Dimension(350, UIManager.getFont("Label.font").getSize() + UIManager.getFont("Label.font").getSize() / 2));
        searchString.setMaximumSize(new java.awt.Dimension(2000, UIManager.getFont("Label.font").getSize() + UIManager.getFont("Label.font").getSize() / 2));

        MenuPopupUtil.installContextMenu(searchString);

        this.toolBarLeftPanel.add(searchString);
        searchString.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                transactionsTableModel.clear();
                transactionsTableModel.find(searchString.getText(), null);

            }

        });


        this.button1ToolBarLeftPanel.setVisible(false);
        this.button2ToolBarLeftPanel.setVisible(false);
        this.searchFavoriteJCheckBoxLeftPanel.setVisible(false);
        this.searchMyJCheckBoxLeftPanel.setVisible(false);
        this.jButton1_jToolBar_RightPanel.setVisible(false);
        this.jButton2_jToolBar_RightPanel.setVisible(false);

        // make hash item from popup menu
        ASMakeHashMenuItem makeHashButton = new ASMakeHashMenuItem(searchString);
        this.toolBarLeftPanel.add(makeHashButton);

        MenuPopupUtil.installContextMenu(searchString);
        MenuPopupUtil.installContextMenu(this.searchTextFieldSearchToolBarLeftPanelDocument);


        this.searchTextFieldSearchToolBarLeftPanelDocument.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                //searchString.setText("");
                transactionsTableModel.clear();
                transactionsTableModel.setBlockNumber(searchTextFieldSearchToolBarLeftPanelDocument.getText());

            }

        });

        //TRANSACTIONS TABLE MODEL
        this.transactionsTableModel = new SearchTransactionsTableModel();
        this.jTableJScrollPanelLeftPanel = new MTable(this.transactionsTableModel);
        TableColumnModel columnModel = jTableJScrollPanelLeftPanel.getColumnModel();
        columnModel.getColumn(transactionsTableModel.COLUMN_FAVORITE).setMaxWidth((100));


        this.jTableJScrollPanelLeftPanel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // MENU
        JPopupMenu mainMenu = new JPopupMenu();

        JMenuItem vouch_menu = new JMenuItem(Lang.T("Sign / Vouch"));
        vouch_menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int row = jTableJScrollPanelLeftPanel.getSelectedRow();
                row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);
                Transaction trans = transactionsTableModel.getItem(row);
                DCSet db = DCSet.getInstance();
                new toSignRecordDialog(trans.getBlockHeight(), trans.getSeqNo());

            }
        });

        mainMenu.add(vouch_menu);

        // save jsot transactions
        JMenuItem item_Save = new JMenuItem(Lang.T("Save"));
        item_Save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int row = jTableJScrollPanelLeftPanel.getSelectedRow();
                row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);
                Transaction trans = transactionsTableModel.getItem(row);
                if (trans == null) return;
                // save
                Library.saveTransactionJSONtoFileSystem(getParent(), trans);
            }


        });

        mainMenu.add(item_Save);

        JMenuItem linkMenu = new JMenuItem(Lang.T("Append / Reply"));
        linkMenu.addActionListener(e -> {
            int row = jTableJScrollPanelLeftPanel.getSelectedRow();
            row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);
            Transaction transaction = (Transaction) transactionsTableModel.getItem(row);
            MainPanel.getInstance().insertNewTab(
                    Lang.T("For # для") + " " + transaction.viewHeightSeq(),
                    new IssueDocumentPanel(null, ExData.LINK_APPENDIX_TYPE, transaction.viewHeightSeq(), null));

        });
        mainMenu.add(linkMenu);

        mainMenu.addSeparator();

        JMenuItem setSeeInBlockexplorer = new JMenuItem(Lang.T("Check in Blockexplorer"));

        setSeeInBlockexplorer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int row = jTableJScrollPanelLeftPanel.getSelectedRow();
                row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);
                Transaction trans = transactionsTableModel.getItem(row);

                try {
                    URLViewer.openWebpage(new URL(Settings.getInstance().getBlockexplorerURL()
                            + "/index/blockexplorer.html"
                            + "?tx=" + trans.viewHeightSeq()));
                } catch (MalformedURLException e1) {
                    logger.error(e1.getMessage(), e1);
                }
            }
        });
        mainMenu.add(setSeeInBlockexplorer);

        TableMenuPopupUtil.installContextMenu(this.jTableJScrollPanelLeftPanel, mainMenu);  // SELECT ROW ON WHICH CLICKED RIGHT BUTTON

        this.jTableJScrollPanelLeftPanel.getSelectionModel().addListSelectionListener(new search_listener());

        //TRANSACTION DETAILS
        this.jTableJScrollPanelLeftPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    //GET ROW
                    int row = jTableJScrollPanelLeftPanel.getSelectedRow();
                    row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);

                    //GET TRANSACTION
                    Transaction transaction = transactionsTableModel.getItem(row);

                    //SHOW DETAIL SCREEN OF TRANSACTION
                    TransactionDetailsFactory.getInstance().createTransactionDetail(transaction);
                }
            }
        });

        jTableJScrollPanelLeftPanel.addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) {

                if (jTableJScrollPanelLeftPanel
                        .columnAtPoint(e.getPoint()) == transactionsTableModel.COLUMN_FAVORITE) {

                    jTableJScrollPanelLeftPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    jTableJScrollPanelLeftPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }

            public void mouseDragged(MouseEvent e) {
            }
        });

        jTableJScrollPanelLeftPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int row = jTableJScrollPanelLeftPanel.rowAtPoint(p);

                if (e.getClickCount() == 1 & e.getButton() == MouseEvent.BUTTON1) {

                    if (jTableJScrollPanelLeftPanel
                            .getSelectedColumn() == transactionsTableModel.COLUMN_FAVORITE) {
                        row = jTableJScrollPanelLeftPanel.convertRowIndexToModel(row);
                        Transaction transaction = transactionsTableModel.getItem(row);
                        favorite_set(transaction);
                    }
                }
            }
        });

        this.jScrollPanelLeftPanel.setViewportView(this.jTableJScrollPanelLeftPanel);

    }

    @Override
    public void onClose() {
    }

    public void listener() {
        transactionsTableModel.setBlockNumber(searchTextFieldSearchToolBarLeftPanelDocument.getText());
    }

    // listener select row
    class search_listener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent arg0) {
            // устанавливаем формат даты
            Transaction transaction = null;
            if (jTableJScrollPanelLeftPanel.getSelectedRow() >= 0) {
                try {
                    transaction = transactionsTableModel.getItem(jTableJScrollPanelLeftPanel
                            .convertRowIndexToModel(jTableJScrollPanelLeftPanel.getSelectedRow()));
                } catch (Exception e) {

                }

                info_Panel = new JPanel();
                info_Panel.setLayout(new GridBagLayout());

                //TABLE GBC
                GridBagConstraints tableGBC = new GridBagConstraints();
                tableGBC.fill = GridBagConstraints.BOTH;
                tableGBC.anchor = GridBagConstraints.FIRST_LINE_START;
                tableGBC.weightx = 1;
                tableGBC.weighty = 1;
                tableGBC.gridx = 0;
                tableGBC.gridy = 0;
                info_Panel.add(TransactionDetailsFactory.getInstance().createTransactionDetail(transaction), tableGBC);

                Tuple2<BigDecimal, List<Long>> keys = DCSet.getInstance().getVouchRecordMap().get(Transaction.makeDBRef(transaction.getBlockHeight(), transaction.getSeqNo()));
                GridBagConstraints gridBagConstraints = null;
                if (keys != null) {

                    JLabel jLabelTitlt_Table_Sign = new JLabel(Lang.T("Signatures") + ":");
                    gridBagConstraints = new java.awt.GridBagConstraints();
                    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
                    gridBagConstraints.weightx = 0.1;
                    gridBagConstraints.insets = new java.awt.Insets(12, 11, 0, 11);
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy = 1;
                    info_Panel.add(jLabelTitlt_Table_Sign, gridBagConstraints);


                    gridBagConstraints = new java.awt.GridBagConstraints();
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy = 2;
                    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
                    gridBagConstraints.weightx = 1.0;
                    gridBagConstraints.weighty = 1.0;
                    voush_Library_Panel = new SignLibraryPanel(transaction);
                    info_Panel.add(voush_Library_Panel, gridBagConstraints);

                }

                jScrollPaneJPanelRightPanel.setViewportView(info_Panel);

            }
        }
    }

    public void favorite_set(Transaction transaction) {

        // CHECK IF FAVORITES
        if (Controller.getInstance().isTransactionFavorite(transaction)) {
            int dd = JOptionPane.showConfirmDialog(MainFrame.getInstance(), Lang.T("Delete from favorite") + "?", Lang.T("Delete from favorite"), JOptionPane.OK_CANCEL_OPTION);

            if (dd == 0) Controller.getInstance().removeTransactionFavorite(transaction);
        } else {

            Controller.getInstance().addTransactionFavorite(transaction);
        }
        jTableJScrollPanelLeftPanel.repaint();

    }
}