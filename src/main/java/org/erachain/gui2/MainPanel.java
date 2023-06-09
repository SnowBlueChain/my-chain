package org.erachain.gui2;

import org.erachain.core.BlockChain;
import org.erachain.gui.IconPanel;
import org.erachain.gui.Wallets.WalletsManagerSplitPanel;
import org.erachain.gui.bank.IssueSendPaymentOrder;
import org.erachain.gui.bank.MyOrderPaymentsSplitPanel;
import org.erachain.gui.items.accounts.FavoriteAccountsSplitPanel;
import org.erachain.gui.items.accounts.MyAccountsSplitPanel;
import org.erachain.gui.items.accounts.MyLoansSplitPanel;
import org.erachain.gui.items.assets.*;
import org.erachain.gui.items.imprints.ImprintsFavoriteSplitPanel;
import org.erachain.gui.items.imprints.ImprintsSearchSplitPanel;
import org.erachain.gui.items.imprints.IssueImprintPanel;
import org.erachain.gui.items.imprints.MyImprintsTab;
import org.erachain.gui.items.link_hashes.IssueLinkedHashPanel;
import org.erachain.gui.items.mails.IncomingMailsSplitPanel;
import org.erachain.gui.items.mails.MailSendPanel;
import org.erachain.gui.items.mails.OutcomingMailsSplitPanel;
import org.erachain.gui.items.other.OtherConsolePanel;
import org.erachain.gui.items.other.OtherSearchBlocks;
import org.erachain.gui.items.other.OtherSplitPanel;
import org.erachain.gui.items.persons.*;
import org.erachain.gui.items.polls.IssuePollPanel;
import org.erachain.gui.items.polls.PollsFavoriteSplitPanel;
import org.erachain.gui.items.polls.PollsMySplitPanel;
import org.erachain.gui.items.polls.SearchPollsSplitPanel;
import org.erachain.gui.items.records.FavoriteTransactionsSplitPanel;
import org.erachain.gui.items.records.MyTransactionsSplitPanel;
import org.erachain.gui.items.records.SearchTransactionsSplitPanel;
import org.erachain.gui.items.records.UnconfirmedTransactionsPanel;
import org.erachain.gui.items.statement.FavoriteStatementsSplitPanel;
import org.erachain.gui.items.statement.IssueDocumentPanel;
import org.erachain.gui.items.statement.SearchStatementsSplitPanel;
import org.erachain.gui.items.statement.StatementsMySplitPanel;
import org.erachain.gui.items.statuses.IssueStatusPanel;
import org.erachain.gui.items.statuses.MyStatusesTab;
import org.erachain.gui.items.statuses.SearchStatusesSplitPanel;
import org.erachain.gui.items.statuses.StatusesFavoriteSplitPanel;
import org.erachain.gui.items.templates.IssueTemplatePanel;
import org.erachain.gui.items.templates.SearchTemplatesSplitPanel;
import org.erachain.gui.items.templates.TemplateMySplitPanel;
import org.erachain.gui.items.templates.TemplatesFavoriteSplitPanel;
import org.erachain.gui.items.unions.IssueUnionPanel;
import org.erachain.gui.items.unions.MyUnionsTab;
import org.erachain.gui.items.unions.SearchUnionSplitPanel;
import org.erachain.gui.items.unions.UnionsFavoriteSplitPanel;
import org.erachain.gui.library.MSplitPane;
import org.erachain.gui.telegrams.ALLTelegramPanel;
import org.erachain.gui.telegrams.TelegramSplitPanel;
import org.erachain.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

/**
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 * @author ����
 */
public class MainPanel extends javax.swing.JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static MainPanel instance;
    public MainLeftPanel mlp;
    public MSplitPane jSplitPane1;
    public MTabbedPanel jTabbedPane1;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private MainPanel() {
        initComponents();
    }

    /**
     * Creates new form split_1
     */

    public static MainPanel getInstance() {
        if (instance == null) {
            instance = new MainPanel();
        }

        return instance;

    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jSplitPane1 = new MSplitPane();
        jSplitPane1.buttonOrientation.setVisible(false);
        jSplitPane1.set_CloseOnOneTouch(jSplitPane1.ONE_TOUCH_CLOSE_LEFT_TOP); // set
        jTabbedPane1 = new MTabbedPanel(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        mlp = new MainLeftPanel();
        jTabbedPane1.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mousePressed(MouseEvent arg0) {
                // TODO Auto-generated method stub
                Component cc = arg0.getComponent();
                if (cc.getClass().getSimpleName().equals("MTabbedPanel")) {
                    MTabbedPanel mt = (MTabbedPanel) cc;

                    // find path from name node
                    int index = mt.getSelectedIndex();
                    if (index >= 0) {
                        Component aa;
                        DefaultMutableTreeNode ss = getNodeByName(jTabbedPane1.getComponentAt(mt.getSelectedIndex()).getClass().getSimpleName(),
                                (DefaultMutableTreeNode) mlp.tree.tree.getModel().getRoot());
                        // set select from tree
                        if (ss != null)
                            mlp.tree.tree.setSelectionPath(new TreePath(ss.getPath()));
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

        });

        mlp.tree.tree.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                // TODO Auto-generated method stub
                if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                    // find path from name node

                    addTab(mlp.tree.tree.getLastSelectedPathComponent().toString());
                }
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void keyTyped(KeyEvent arg0) {
                // TODO Auto-generated method stub

            }

        });

        setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setRightComponent(jTabbedPane1);

        mlp.tree.tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // TODO Auto-generated method stub

            }

        });

        mlp.tree.tree.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                // TODO Auto-generated method stub
                if (arg0.getClickCount() == 1) {

                    Component aa = arg0.getComponent();
                    if (aa.getClass().getSimpleName().equals("JTree")) {
                        JTree tr = ((JTree) aa);
                        if (tr.getLastSelectedPathComponent() == null)
                            return;

                        addTab(tr.getLastSelectedPathComponent().toString());

                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mousePressed(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

        });

        mlp.setMinimumSize(new Dimension(0, 0));
        jSplitPane1.setLeftComponent(mlp);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(jSplitPane1, gridBagConstraints);
        jSplitPane1.setDividerLocation(250);
    }

    public void addTab(String str) {

        try {
            if (str.equals(Lang.T("Send payment order")) || str.equals("IssueSendPaymentOrder")) {
                insertTab(new IssueSendPaymentOrder());
                return;
            }

            if (str.equals(Lang.T("My Payments Orders"))
                    || str.equals("MyOrderPaimentsSplitPanel")) {
                insertTab(new MyOrderPaymentsSplitPanel());
                return;
            }

            /////////// PERSONS
            if (str.equals(Lang.T("Favorite Persons")) || str.equals("PersonsFavoriteSplitPanel")) {
                insertTab(new PersonsFavoriteSplitPanel());
                return;
            }
            if (str.equals(Lang.T("My Persons")) || str.equals("PersonsMySplitPanel")) {
                insertTab(new PersonsMySplitPanel());
                return;
            }
            if (str.equals(Lang.T("Search Persons")) || str.equals("SearchPersonsSplitPanel")) {
                insertTab(new SearchPersonsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Issue Person")) || str.equals("IssuePersonPanel")) {
                insertTab(new IssuePersonPanel());
                return;
            }

            if (str.equals(Lang.T("Insert Person")) || str.equals("InsertPersonPanel")) {
                insertTab(new InsertPersonPanel());
                return;
            }
            if (str.equals(Lang.T("Issue Union")) || str.equals("IssuePersonsUnionPanel")) {
                insertTab(new IssuePersonsUnionPanel());
                return;
            }

            if (str.equals(Lang.T("My Accounts")) || str.equals("MyAccountsSplitPanel")) {
                insertTab(new MyAccountsSplitPanel());
                return;

            }

            if (str.equals(Lang.T("My Loans")) || str.equals("MyLoansSplitPanel")) {
                insertTab(new MyLoansSplitPanel());
                return;

            }

            if (str.equals(Lang.T("Favorite Accounts"))
                    || str.equals("FavoriteAccountsSplitPanel")) {
                insertTab(new FavoriteAccountsSplitPanel());
                return;

            }

            // STATEMENTS
            if (str.equals(Lang.T("Favorite Documents"))
                    || str.equals("FavoriteStatementsSplitPanel")) {
                insertTab(new FavoriteStatementsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("My Documents")) || str.equals("StatementsMySplitPanel")) {
                insertTab(new StatementsMySplitPanel());
                return;

            }
            if (str.equals(Lang.T("Search Documents"))
                    || str.equals("SearchStatementsSplitPanel")) {
                insertTab(new SearchStatementsSplitPanel());
                return;

            }
            if (str.equals(Lang.T("Issue Document")) || str.equals("IssueDocumentPanel")) {
                insertTab(new IssueDocumentPanel());
                return;

            }

            /// MAILS
            if (str.equals(Lang.T("Incoming Mails")) || str.equals("IncomingMailsSplitPanel")) {
                insertTab(new IncomingMailsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Outcoming Mails")) || str.equals("OutcomingMailsSplitPanel")) {
                insertTab(new OutcomingMailsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Send Mail")) || str.equals("MailSendPanel")) {
                insertTab(new MailSendPanel(null, null, null));
                return;
            }

            if (str.equals(Lang.T("Favorite Assets")) || str.equals("AssetsFavoriteSplitPanel")) {
                insertTab(new AssetsFavoriteSplitPanel());
                return;
            }

            if (str.equals(Lang.T("My Assets")) || str.equals("AssetsMySplitPanel")) {
                ///insertTab(Lang.T("My Assets"), new MyAssetsTab(), MyAssetsTab.getIcon());
                insertTab(new AssetsMySplitPanel());
                return;
            }
            if (str.equals(Lang.T("Search Assets")) || str.equals("SearchAssetsSplitPanel")) {
                insertTab(new SearchAssetsSplitPanel(true));
                return;
            }
            if (str.equals(Lang.T("My Balance")) || str.equals("MyBalanceTab")) {
                insertTab(new MyBalanceTab());
                return;
            }
            if (str.equals(Lang.T("My Orders")) || str.equals("MyOrderTab")) {
                insertTab(new MyOrderTab());
                return;
            }
            if (str.equals(Lang.T("Issue Asset")) || str.equals("IssueAssetPanel")) {
                insertTab(new IssueAssetPanel());
                return;
            } else if (str.equals(Lang.T("Issue Series")) || str.equals("IssueAssetCopyPanel")) {
                insertTab(new IssueAssetCopyPanel());
                return;
            } else if (str.equals(Lang.T("Exchange")) || str.equals("ExchangePanel")) {
                insertTab(new ExchangePanel(null, null, null, null));
                return;
            } else if (str.equals(Lang.T("Withdraw Exchange")) || str.equals(WithdrawExchange.class.getSimpleName())) {
                insertTab(new WithdrawExchange(null, null));
                return;
            }

            if (str.equals(Lang.T("Deposit Exchange")) || str.equals(DepositExchange.class.getSimpleName())) {
                insertTab(new DepositExchange(null, null, null));
                return;
            }


            if (str.equals(Lang.T("My Templates")) || str.equals("TemplateMySplitPanel")) {
                insertTab(new TemplateMySplitPanel());
                return;
            }
            if (str.equals(Lang.T("Search Templates")) || str.equals("SearchTemplatesSplitPanel")) {
                insertTab(new SearchTemplatesSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Favorite Templates"))
                    || str.equals("TemplatesFavoriteSplitPanel")) {
                insertTab(new TemplatesFavoriteSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Issue Template")) || str.equals("IssueTemplatePanel")) {
                insertTab(new IssueTemplatePanel());
                return;
            }
            if (str.equals(Lang.T("Create Status")) || str.equals("IssueStatusPanel")) {
                insertTab(new IssueStatusPanel());
                return;
            }
            if (str.equals(Lang.T("Favorite Statuses"))
                    || str.equals("StatusesFavoriteSplitPanel")) {
                insertTab(new StatusesFavoriteSplitPanel());
                return;
            }
            if (str.equals(Lang.T(MyStatusesTab.TITLE))
                    || str.equals("MyStatusesTab")) {
                insertTab(new MyStatusesTab());
                return;
            }
            if (str.equals(Lang.T("Search Statuses")) || str.equals("SearchStatusesSplitPanel")) {
                insertTab(new SearchStatusesSplitPanel());
                return;
            }
            if (BlockChain.TEST_MODE) {
                if (str.equals(Lang.T(UnionsFavoriteSplitPanel.TITLE)) || str.equals(UnionsFavoriteSplitPanel.NAME)) {
                    insertTab(new UnionsFavoriteSplitPanel());
                    return;
                }
                if (str.equals(Lang.T("My Unions")) || str.equals("MyUnionsTab")) {
                    insertTab(new MyUnionsTab());
                    return;
                }
                if (str.equals(Lang.T("Search Unions")) || str.equals("SearchUnionSplitPanel")) {
                    insertTab(new SearchUnionSplitPanel());
                    return;
                }
                if (str.equals(Lang.T("Issue Union")) || str.equals("IssueUnionPanel")) {
                    insertTab(new IssueUnionPanel());
                    return;
                }
            }

            /////// POLLS
            if (str.equals(Lang.T("Favorite Polls")) || str.equals("PollsFavoriteSplitPanel")) {
                insertTab(new PollsFavoriteSplitPanel());
                return;
            }
            if (str.equals(Lang.T(PollsMySplitPanel.NAME)) || str.equals(PollsMySplitPanel.TITLE)) {
                insertTab(new PollsMySplitPanel());
                return;
            }
            if (str.equals(Lang.T("Search Polls")) || str.equals("SearchPollsSplitPanel")) {
                insertTab(new SearchPollsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Issue Poll")) || str.equals("IssuePollPanel")) {
                insertTab(new IssuePollPanel());
                return;
            }


            //////// TRANSACTIONS
            if (str.equals(Lang.T("Favorite Records")) || str.equals("FavoriteTransactionsSplitPanel")) {
                insertTab(new FavoriteTransactionsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("My Records")) || str.equals("MyTransactionsSplitPanel")) {
                insertTab(MyTransactionsSplitPanel.getInstance());
                return;
            }
            if (str.equals(Lang.T("Search Records")) || str.equals("SearchTransactionsSplitPanel")) {
                insertTab(new SearchTransactionsSplitPanel());
                return;
            }
            if (str.equals(Lang.T("Unconfirmed Records"))
                    || str.equals("UnconfirmedTransactionsPanel")) {
                insertTab(new UnconfirmedTransactionsPanel());
                return;
            }
            if (str.equals(Lang.T("Other")) || str.equals("OtherSplitPanel")) {
                insertTab(new OtherSplitPanel());

                return;
            }

            if (str.equals(Lang.T("Console")) || str.equals("OtherConsolePanel")) {
                insertTab(new OtherConsolePanel());

                return;
            }

            if (str.equals(Lang.T("Blocks")) || str.equals("OtherSearchBlocks")) {
                insertTab(new OtherSearchBlocks());

                return;
            }

            /// UNIQUE HASHES
            if (str.equals(Lang.T("Favorite Unique Hashes"))
                    || str.equals("ImprintsFavoriteSplitPanel")) {
                insertTab(new ImprintsFavoriteSplitPanel());

                return;
            }
            if (str.equals(Lang.T("My Unique Hashes")) || str.equals("MyImprintsTab")) {
                insertTab(new MyImprintsTab());

                return;
            }
            if (str.equals(Lang.T("Search Unique Hashes"))
                    || str.equals("ImprintsSearchSplitPanel")) {
                insertTab(new ImprintsSearchSplitPanel());

                return;
            }
            if (str.equals(Lang.T("Issue Unique Hash")) || str.equals("IssueImprintPanel")) {
                insertTab(new IssueImprintPanel());

                return;
            }

            if (str.equals(Lang.T("Issue Linked Hash")) || str.equals("IssueLinkedHashPanel")) {
                insertTab(new IssueLinkedHashPanel());

                return;
            }

            if (str.equals(Lang.T("Search Linked Hash")) || str.equals("SearchTransactionsSplitPanel")) {
                insertTab(new SearchTransactionsSplitPanel());
                return;
            }


            if (BlockChain.TEST_MODE) {
                if (str.equals(Lang.T("Wallets Manager"))
                        || str.equals("WalletsManagerSplitPanel")) {
                    insertTab(new WalletsManagerSplitPanel());
                    return;
                }

            }

            if (str.equals(Lang.T("Telegrams Panel"))
                    || str.equals("TelegramSplitPanel")) {
                insertTab(new TelegramSplitPanel());

                return;
            }
            if (str.equals(Lang.T("All Telegrams Panel"))
                    || str.equals("ALLTelegramPanel")) {
                insertTab(new ALLTelegramPanel());
                return;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * If already opened - show it
     *
     * @param iconPanel
     * @return
     */
    public boolean insertTab(IconPanel iconPanel) {
        int index = jTabbedPane1.indexOfTab(iconPanel.getTitle());
        boolean inserted = false;
        if (index == -1) {
            jTabbedPane1.addTabWithCloseButton(iconPanel.getTitle(), iconPanel.getIcon(), iconPanel);
            index = jTabbedPane1.indexOfTab(iconPanel.getTitle());
            inserted = true;
        }
        jTabbedPane1.setSelectedIndex(index);

        return inserted;

    }

    /**
     * If already opened - close first it and open anew
     *
     * @param str
     * @param pp
     */
    public void insertNewTab(String str, IconPanel pp) {
        int index = jTabbedPane1.indexOfTab(str);
        if (index >= 0) {
            jTabbedPane1.remove(index);
        }
        jTabbedPane1.addTabWithCloseButton(str, pp.getIcon(), pp);
        index = jTabbedPane1.indexOfTab(str);
        jTabbedPane1.setSelectedIndex(index);

    }


    // insert tab in tabbedpane
    public void renameTab(String oldTitle, String newTitle) {
        int index = jTabbedPane1.indexOfTab(oldTitle);
        if (index > 0) {
            jTabbedPane1.setTitleAt(index, newTitle);
            jTabbedPane1.getComponentAt(index).setName(newTitle);
        }

    }

    public void removeTab(String title) {
        int index = jTabbedPane1.indexOfTab(title);
        if (index > 0) {
            jTabbedPane1.remove(index);
        }
    }

    public Component getTabComponent(String title) {
        int index = jTabbedPane1.indexOfTab(title);
        if (index > 0) {
            return jTabbedPane1.getComponentAt(index);
        }
        return null;
    }

    // get node by name
    private DefaultMutableTreeNode getNodeByName(String sNodeName, DefaultMutableTreeNode parent) {
        if (parent != null)
            for (Enumeration e = parent.breadthFirstEnumeration(); e.hasMoreElements(); ) {
                DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
                if (sNodeName.equals(current.getUserObject())) {
                    return current;
                }
            }
        return null;
    }

}
