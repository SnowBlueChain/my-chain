package org.erachain.gui.library;

import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.crypto.Base32;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.gui.items.accounts.*;
import org.erachain.gui.items.mails.MailSendPanel;
import org.erachain.gui.models.AccountsTableModel;
import org.erachain.gui2.MainPanel;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.URLViewer;
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;

public class DealsPopupMenu extends JPopupMenu {

    protected Logger logger;

    public AccountsTableModel tableModel;
    protected JComboBox<ItemCls> assetSelector;
    protected AssetCls asset;
    protected PublicKeyAccount pubKey;
    protected MTable table;

    private JMenuItem sendMail;

    private JMenuItem sendAsset;
    private JMenuItem sendAssetBackward;

    private JMenuItem debtAsset;
    private JMenuItem debtAssetReturn;
    private JMenuItem debtAssetBackward;

    private JMenuItem holdAsset;
    private JMenuItem holdAssetBackward;

    private JMenuItem spendAsset;
    private JMenuItem spendAssetBackward;

    public DealsPopupMenu(AccountsTableModel tableModel, MTable table, JComboBox<ItemCls> assetSelector) {

        logger = LoggerFactory.getLogger(getClass());

        this.tableModel = tableModel;
        this.table = table;
        this.assetSelector = assetSelector;

        sendMail = new JMenuItem(Lang.getInstance().translate("Send mail"));
        sendMail.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.getInstance().insertNewTab(Lang.getInstance().translate("Send mail"),
                        new MailSendPanel(pubKey, null, null));
            }
        });
        this.add(sendMail);

        this.addSeparator();

        sendAsset = new JMenuItem(Lang.getInstance().translate("Send"));
        sendAsset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // AccountAssetLendPanel
                MainPanel.getInstance().insertNewTab(sendAsset.getText() + ":" + asset.getKey(),
                        new AccountAssetSendPanel(asset, pubKey, null, null, null, false));

            }
        });
        this.add(sendAsset);

        sendAssetBackward = new JMenuItem(Lang.getInstance().translate("Backward"));
        sendAssetBackward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // AccountAssetLendPanel
                MainPanel.getInstance().insertNewTab(sendAssetBackward.getText() + ":" + asset.getKey(),
                        new AccountAssetSendPanel(asset, pubKey, null, null, null, true));

            }
        });
        this.add(sendAssetBackward);

        this.addSeparator();

        debtAsset = new JMenuItem(Lang.getInstance().translate("Lend"));
        debtAsset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //new AccountLendDialog(asset, pubKey);
                MainPanel.getInstance().insertNewTab(debtAsset.getText() + ":" + asset.getKey(),
                        new AccountAssetLendPanel(asset, pubKey, null, null));

            }
        });
        this.add(debtAsset);

        debtAssetReturn = new JMenuItem(Lang.getInstance().translate("Repay Debt"));
        debtAssetReturn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(debtAssetReturn.getText() + ":" + asset.getKey(),
                        new AccountAssetRepayDebtPanel(asset, pubKey, null, null));

            }
        });
        this.add(debtAssetReturn);

        debtAssetBackward = new JMenuItem(Lang.getInstance().translate("Confiscate Debt"));
        debtAssetBackward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(debtAssetBackward.getText() + ":" + asset.getKey(),
                        new AccountAssetConfiscateDebtPanel(asset, pubKey, null, null));

            }
        });
        this.add(debtAssetBackward);

        this.addSeparator();

        holdAsset = new JMenuItem(Lang.getInstance().translate("Hold"));
        holdAsset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(holdAsset.getText() + ":" + asset.getKey(),
                        new AccountAssetHoldPanel(asset, pubKey, null, null, true));

            }
        });
        this.add(holdAsset);

        holdAssetBackward = new JMenuItem(Lang.getInstance().translate("Backward Hold"));
        holdAssetBackward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(holdAssetBackward.getText() + ":" + asset.getKey(),
                        new AccountAssetHoldPanel(asset, pubKey, null, null, false));

            }
        });
        this.add(holdAssetBackward);

        this.addSeparator();

        spendAsset = new JMenuItem(Lang.getInstance().translate("Spend"));
        spendAsset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(spendAsset.getText() + ":" + asset.getKey(),
                        new AccountAssetSpendPanel(asset, pubKey, null, null, null, false));

            }
        });
        this.add(spendAsset);

        spendAssetBackward = new JMenuItem(Lang.getInstance().translate("Backward Spend"));
        spendAssetBackward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                MainPanel.getInstance().insertNewTab(spendAssetBackward.getText() + ":" + asset.getKey(),
                        new AccountAssetSpendPanel(asset, pubKey, null, null, null, true));

            }
        });
        this.add(spendAssetBackward);

        this.addSeparator();

        JMenuItem copyAddress = new JMenuItem(Lang.getInstance().translate("Copy Account"));
        copyAddress.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //      int row = table.getSelectedRow();
                //      if (row < 1 ) return;

                //      row = table.convertRowIndexToModel(row);
                //      Account account = tableModel.getAccount(row);

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection value = new StringSelection(pubKey.getAddress());
                clipboard.setContents(value, null);
            }
        });
        this.add(copyAddress);

        JMenuItem copyBalance = new JMenuItem(Lang.getInstance().translate("Copy Balance"));
        copyBalance.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                //long key = cbxFavorites.getItemAt(cbxFavorites.getSelectedIndex()).getKey();
                long key = asset.getKey();
                StringSelection value = new StringSelection(pubKey.getBalance(key).toString());
                clipboard.setContents(value, null);
            }
        });

        this.add(copyBalance);

        this.addSeparator();

        JMenuItem copyPublicKey = new JMenuItem(Lang.getInstance().translate("Copy Public Key"));
        copyPublicKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection value = new StringSelection(pubKey.getBase58());
                clipboard.setContents(value, null);
            }
        });
        this.add(copyPublicKey);

        JMenuItem copyBankKey = new JMenuItem(Lang.getInstance().translate("Copy Public Key for BANK"));
        copyBankKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String bankKeyAccount = "+" + Base32.encode(pubKey.getPublicKey());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection value = new StringSelection(bankKeyAccount);
                clipboard.setContents(value, null);
            }
        });
        this.add(copyBankKey);

        JMenuItem set_name = new JMenuItem(Lang.getInstance().translate("Edit name"));
        set_name.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new AccountSetNameDialog(pubKey.getAddress());
                table.repaint();
            }
        });
        this.add(set_name);

        this.addSeparator();

        JMenuItem setSeeInBlockexplorer = new JMenuItem(Lang.getInstance().translate("Check in Blockexplorer"));

        setSeeInBlockexplorer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    URLViewer.openWebpage(new URL(Settings.getInstance().getBlockexplorerURL()
                            + "/index/blockexplorer.html"
                            + "?address=" + pubKey.getAddress()));
                } catch (MalformedURLException e1) {
                    logger.error(e1.getMessage(), e1);
                }
            }
        });

        add(setSeeInBlockexplorer);

        this.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
                // TODO Auto-generated method stub

                // TODO Auto-generated method stub

                asset = (AssetCls) assetSelector.getSelectedItem();

                int row = table.getSelectedRow();
                if (row < 0)
                    return;
                row = table.convertRowIndexToModel(row);

                pubKey = tableModel.getItem(row);
                init();

            }


        });

    }

    public void init() {

        boolean isCreatorOwner = asset != null && pubKey.equals(asset.getOwner());
        boolean isSelfManaged = asset.isSelfManaged();
        boolean isUnlimited = isSelfManaged || asset.isUnlimited(pubKey);

        this.sendAsset.setEnabled(true);
        this.holdAsset.setEnabled(true);
        this.debtAsset.setEnabled(true);
        this.debtAssetReturn.setEnabled(true);
        this.debtAssetBackward.setEnabled(true);
        this.spendAsset.setEnabled(true);

        if (isSelfManaged) {
            if (isCreatorOwner) {
                this.sendAssetBackward.setVisible(true);
                this.holdAssetBackward.setVisible(true);
                this.spendAssetBackward.setVisible(true);
            } else {
                this.sendAsset.setEnabled(false);
                this.holdAsset.setEnabled(false);
                this.debtAsset.setEnabled(false);
                this.debtAssetReturn.setEnabled(false);
                this.debtAssetBackward.setEnabled(false);
                this.spendAsset.setEnabled(false);
            }
        } else {
            this.sendAssetBackward.setVisible(false);
            this.holdAssetBackward.setVisible(false);
            this.spendAssetBackward.setVisible(false);
        }

        this.sendMail.setText(Lang.getInstance().translate("Send Mail"));

        String actionName = asset.viewAssetTypeAction(false, TransactionAmount.ACTION_SEND, false);
        if (actionName == null) {
            this.sendAsset.setVisible(false);
        } else {
            this.sendAsset.setText(Lang.getInstance().translate(actionName));
            this.sendAsset.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(true, TransactionAmount.ACTION_SEND, false);
        if (actionName == null) {
            this.sendAssetBackward.setVisible(false);
        } else {
            this.sendAssetBackward.setText(Lang.getInstance().translate(actionName));
            this.sendAssetBackward.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(true, TransactionAmount.ACTION_HOLD, isCreatorOwner);
        if (actionName == null) {
            this.holdAsset.setVisible(false);
        } else {
            this.holdAsset.setText(Lang.getInstance().translate(actionName));
            this.holdAsset.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(false, TransactionAmount.ACTION_HOLD, isCreatorOwner);
        if (actionName == null) {
            this.holdAssetBackward.setVisible(false);
        } else {
            this.holdAssetBackward.setText(Lang.getInstance().translate(actionName));
            this.holdAssetBackward.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(false, TransactionAmount.ACTION_DEBT, isCreatorOwner);
        if (actionName == null) {
            this.debtAsset.setVisible(false);
        } else {
            this.debtAsset.setText(Lang.getInstance().translate(actionName));
            this.debtAsset.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(false, TransactionAmount.ACTION_REPAY_DEBT, isCreatorOwner);
        if (actionName == null) {
            this.debtAssetReturn.setVisible(false);
        } else {
            this.debtAssetReturn.setText(Lang.getInstance().translate(actionName));
            this.debtAssetReturn.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(true, TransactionAmount.ACTION_DEBT, isCreatorOwner);
        if (actionName == null) {
            this.debtAssetBackward.setVisible(false);
        } else {
            this.debtAssetBackward.setText(Lang.getInstance().translate(actionName));
            this.debtAssetBackward.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(false, TransactionAmount.ACTION_SPEND, isCreatorOwner);
        if (actionName == null) {
            this.spendAsset.setVisible(false);
        } else {
            this.spendAsset.setText(Lang.getInstance().translate(actionName));
            this.spendAsset.setVisible(true);
        }

        actionName = asset.viewAssetTypeAction(true, TransactionAmount.ACTION_SPEND, isCreatorOwner);
        if (actionName == null) {
            this.spendAssetBackward.setVisible(false);
        } else {
            this.spendAssetBackward.setText(Lang.getInstance().translate(actionName));
            this.spendAssetBackward.setVisible(true);
        }

        Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>>
                balance = pubKey.getBalance(asset.getKey());

        if (!isUnlimited && balance.a.b.signum() == 0) {
            this.sendAsset.setEnabled(false);
            this.debtAsset.setEnabled(false);
        }

        if (!isUnlimited && balance.b.b.signum() == 0) {
            this.debtAssetReturn.setEnabled(false);
        }

        switch (this.asset.getAssetType()) {

            case AssetCls.AS_BANK_GUARANTEE:

                balance = pubKey.getBalance(asset.getKey());
                if (balance.a.b.signum() > 0) {
                    this.holdAsset.setEnabled(false);
                    this.debtAssetReturn.setEnabled(false);
                    if (balance.b.b.signum() < 0) {
                        this.debtAsset.setEnabled(false);
                    } else {
                        this.debtAssetBackward.setEnabled(false);
                    }
                } else {
                    this.sendAsset.setEnabled(false);
                    this.debtAsset.setEnabled(false);
                    this.debtAssetBackward.setEnabled(false);
                    if (balance.b.b.signum() <= 0) {
                        this.debtAssetReturn.setEnabled(false);
                        this.holdAsset.setEnabled(false);
                    } else {
                        if (balance.c.b.signum() > 0) {
                            this.holdAsset.setEnabled(false);
                        }
                    }
                }

                break;

            case AssetCls.AS_BANK_GUARANTEE_TOTAL:

                balance = pubKey.getBalance(asset.getKey());
                if (pubKey.equals(asset.getOwner()) || balance.a.b.signum() > 0) {

                } else {
                    this.sendAsset.setEnabled(false);
                    this.debtAsset.setEnabled(false);
                    this.debtAssetReturn.setEnabled(false);
                }

                break;

        }

        // ALL OUTSIDE ASSETS
        if (asset.isOutsideType()) {

            this.debtAssetReturn.setEnabled(false);

            if (pubKey.equals(asset.getOwner())) {
                this.holdAsset.setEnabled(false);
                this.debtAsset.setEnabled(false);
                this.debtAssetBackward.setEnabled(false);
                this.spendAsset.setEnabled(false);
            } else {
                if (balance.a.b.signum() <= 0) {
                    this.sendAsset.setEnabled(false);
                    this.holdAsset.setEnabled(false);
                    this.debtAsset.setEnabled(false);
                    this.debtAssetBackward.setEnabled(false);
                    this.spendAsset.setEnabled(false);
                } else {
                    if (balance.b.b.signum() >= 0) {
                        this.debtAssetBackward.setEnabled(false);
                    }
                    if (balance.a.b.add(balance.b.b).signum() <= 0) {
                        this.debtAsset.setEnabled(false);
                    }
                }

            }
        }
    }
}