package org.erachain.gui.items.accounts;

import org.erachain.api.ApiErrorFactory;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.crypto.AEScrypto;
import org.erachain.core.crypto.Base58;
import org.erachain.core.exdata.exLink.ExLink;
import org.erachain.core.exdata.exLink.ExLinkAppendix;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.gui.Gui;
import org.erachain.gui.IconPanel;
import org.erachain.gui.PasswordPane;
import org.erachain.gui.ResultDialog;
import org.erachain.gui.items.assets.AssetInfo;
import org.erachain.gui.items.assets.ComboBoxAssetsModel;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.library.MDecimalFormatedTextField;
import org.erachain.gui.library.RecipientAddress;
import org.erachain.gui.models.AccountsComboBoxModel;
import org.erachain.gui.models.FavoriteComboBoxModel;
import org.erachain.gui.transaction.Send_RecordDetailsFrame;
import org.erachain.lang.Lang;
import org.erachain.utils.Converter;
import org.erachain.utils.MenuPopupUtil;
import org.mapdb.Fun;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

//import org.erachain.gui.AccountRenderer;

public abstract class AccountAssetActionPanelCls extends IconPanel implements RecipientAddress.RecipientAddressInterface {


    public Account creator;

    public Account recipient;

    public String message;

    public byte[] messageBytes;

    public BigDecimal amount;

    public boolean backward;

    public ExLink exLink;

    public int feePow;

    public AssetCls asset;

    public long key;

    public String txTitle;

    public byte[] isTextByte;

    public byte[] encrypted;
    public Integer result;

    public int balancePosition;

    public boolean noReceive;

    public boolean showAssetForm = false;

    /**
     * Creates new form AccountAssetActionPanelCls
     */

    private AccountsComboBoxModel accountsModel;

    public AccountAssetActionPanelCls(String panelName, String title, boolean backward, AssetCls assetIn,
                                      int balancePosition,
                                      Account accountFrom, Account accountTo, String message) {

        super(panelName, title);
        if (assetIn == null)
            this.asset = Controller.getInstance().getAsset(2L);
        else
            this.asset = assetIn;

        this.backward = backward;

        // необходимо входящий параметр отделить так как ниже он по событию изменения актива будет как НУЛь вызваться
        // поэтому тут только приватную переменную юзаем дальше
        if (title == null) {
            this.title = asset.viewAssetTypeActionTitle(backward, balancePosition,
                    accountFrom != null && accountFrom.equals(asset.getOwner()));
        }

        if (panelName == null) {
            this.panelName = title + " [" + asset.getKey() + " ]";
            setName(this.panelName);
        }

        this.creator = accountFrom;

        recipient = accountTo;
        // возможно есть счет по умолчанию
        if (recipient == null && asset != null) {
            recipient = asset.defaultRecipient(balancePosition, backward);
        }

        this.balancePosition = balancePosition;

        initComponents(message);

        this.jlabel_RecipientDetail.setText("");
        this.jTextFieldTXTitle.setText("");

        if (this.asset.defaultAmountAssetType() == null)
            this.jTextField_Amount.setText("0");
        else
            this.jTextField_Amount.setText(this.asset.defaultAmountAssetType().toPlainString());

        // account ComboBox
        this.accountsModel = new AccountsComboBoxModel(balancePosition);
        jComboBoxCreator.setModel(accountsModel);

        if (creator != null) {
            jComboBoxCreator.setSelectedItem(creator);
        }

        jComboBoxCreator.setRenderer(new AccountRenderer(asset.getKey()));

        // favorite combo box
        jComboBox_Asset.setModel(new ComboBoxAssetsModel());
        jComboBox_Asset.setRenderer(new FavoriteComboBoxModel.IconListRenderer());
        jComboBox_Asset.setEditable(false);
        //this.jComboBox_Asset.setEnabled(assetIn != null);

        exLinkDescription.setEditable(false);
        exLinkText.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                viewLinkParent();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                viewLinkParent();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                viewLinkParent();
            }
        });


        if (asset.getKey() > 0 && asset.getKey() < 1000) {
            this.jTextArea_Account_Description.setText(Lang.getInstance().translate(asset.viewDescription()));
        } else {
            this.jTextArea_Account_Description.setText(asset.viewDescription());
        }

        boolean selected = false;
        for (int i = 0; i < jComboBox_Asset.getItemCount(); i++) {
            ItemCls item = jComboBox_Asset.getItemAt(i);
            if (item.getKey() == asset.getKey()) {
                // not worked jComboBox_Asset.setSelectedItem(asset);
                jComboBox_Asset.setSelectedIndex(i);
                selected = true;
                //    jComboBox_Asset.setEnabled(false);// .setEditable(false);
                break;
            } else {
                //    jComboBox_Asset.setEnabled(true);
            }
        }
        if (!selected) {
            jComboBox_Asset.addItem(asset);
            jComboBox_Asset.setSelectedItem(asset);
        }

        this.jComboBox_Fee.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"}));
        jComboBox_Fee.setVisible(Gui.SHOW_FEE_POWER);

        //ON FAVORITES CHANGE

        jComboBoxCreator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                creator = ((Account) jComboBoxCreator.getSelectedItem());
                refreshLabels();

            }
        });


        // default set asset
        ////if (asset == null) asset = ((AssetCls) jComboBox_Asset.getSelectedItem());

        this.jComboBox_Asset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                asset = ((AssetCls) jComboBox_Asset.getSelectedItem());
                refreshLabels();

            }
        });

        jTextField_Amount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    amount = new BigDecimal(jTextField_Amount.getText());
                } catch (Exception exc) {
                    amount = null;
                }
                checkReadyToOK();

            }
        });

        jButton_ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSendClick();
            }
        });

        this.jLabelTXTitle.setText(Lang.getInstance().translate("Title") + ":");
        this.jLabel_Mess.setText(Lang.getInstance().translate("Message") + ":");
        this.jCheckBox_Encrypt.setText(Lang.getInstance().translate("Encrypt message"));
        this.jCheckBox_Encrypt.setSelected(true);
        this.jCheckBox_isText.setText(Lang.getInstance().translate("As Text"));
        this.jCheckBox_isText.setSelected(true);
        this.jLabel_Asset.setText(Lang.getInstance().translate("Asset") + ":");
        this.jLabel_Amount.setText(Lang.getInstance().translate("Amount") + ":");

        this.jLabel_Fee.setText(Lang.getInstance().translate("Fee Power") + ":");
        jLabel_Fee.setVisible(Gui.SHOW_FEE_POWER);

        // CONTEXT MENU
        MenuPopupUtil.installContextMenu(this.jTextField_Amount);
        MenuPopupUtil.installContextMenu(this.jTextArea_Message);
        //MenuPopupUtil.installContextMenu(this.jlabel_RecipientDetail);
        jTextArea_Account_Description.setWrapStyleWord(true);
        jTextArea_Account_Description.setLineWrap(true);
        int scale = asset.getScale();
        jTextField_Amount.setScale(scale);
        if (showAssetForm) {
            jScrollPane2.setViewportView(new AssetInfo(asset, false));
        }

        if (recipient == null) {
            jButton_ok.setEnabled(false);
        } else {
            if (recipient instanceof PublicKeyAccount) {
                recipientAddress.setSelectedAddress(((PublicKeyAccount) recipient).getBase58());
            } else {
                recipientAddress.setSelectedAddress(recipient.getAddress());
            }
            jButton_ok.setEnabled(true);
        }

        refreshLabels();
    }

    private void refreshLabels() {

        checkReadyToOK();

        if (asset == null) {
            return;
        }

        boolean senderIsOwner = false;
        //sender = ((Account) jComboBox_Account.getSelectedItem());
        if (creator != null) {
            ((AccountRenderer) jComboBoxCreator.getRenderer()).setAsset(asset.getKey());
            jComboBoxCreator.repaint();
            senderIsOwner = creator.equals(asset.getOwner());
        }

        boolean recipientIsOwner = false;
        if (recipient != null) {
            recipientIsOwner = recipient.equals(asset.getOwner());
        }

        if (asset.viewAssetTypeAction(backward, balancePosition, senderIsOwner) == null) {
            // Это возможно если был выбран актив уже внутри формы, а у него тип для которого текущего действия нету
            jButton_ok.setEnabled(false);
            jButton_ok.setText(Lang.getInstance().translate("Wrong Action"));
            return;
        }

        String title = Lang.getInstance().translate(asset.viewAssetTypeActionTitle(backward, balancePosition, senderIsOwner));
        String addAssetType = asset.viewAssetTypeAdditionAction(backward, balancePosition, senderIsOwner);
        if (addAssetType == null) {
            jLabel_Title.setText(title + " - " + asset.viewName());
        } else {
            jLabel_Title.setText(title + " - " + asset.viewName()
                    + " (" + Lang.getInstance().translate(addAssetType) + ")");
        }

        setName(title + " ]" + asset.getKey() + " ]");

        jButton_ok.setText(Lang.getInstance().translate(asset.viewAssetTypeActionOK(backward, balancePosition, senderIsOwner)));

        this.jLabelCreator.setText(Lang.getInstance().translate(asset.viewAssetTypeCreator(backward, balancePosition, senderIsOwner)) + ":");

        this.jLabelRecipient.setText(Lang.getInstance().translate(
                asset.viewAssetTypeTarget(backward, balancePosition, recipientIsOwner) + " " + "Account") + ":");
        this.jLabelRecipientDetail.setText(Lang.getInstance().translate("Account Details") + ":");

        // set scale
        int scale = asset.getScale();
        jTextField_Amount.setScale(scale);
        if (showAssetForm) {
            jScrollPane2.setViewportView(new AssetInfo(asset, false));
        }

        this.jLabel_AssetType.setText(Lang.getInstance().translate(asset.viewAssetType()));

        /////////// RECIPIENT DETAILS
        if (recipient != null) {
            String details = Lang.getInstance().translate(
                    Account.getDetailsForEncrypt(recipient.getAddress(), 0,
                            this.jCheckBox_Encrypt.isSelected(), false));

            Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>>
                    balance = recipient.getBalance(asset.getKey());
            if (balance != null) {
                details += (details.isEmpty() ? "" : "<br>") + Lang.getInstance().translate("Balances") + ": "
                        + (balancePosition == TransactionAmount.ACTION_SEND ? ("<b>" + balance.a.b.toPlainString() + "</b>") : balance.a.b.toPlainString())
                        + " / " + (balancePosition == TransactionAmount.ACTION_DEBT ? ("<b>" + balance.b.b.toPlainString() + "</b>") : balance.b.b.toPlainString())
                        + " / " + (balancePosition == TransactionAmount.ACTION_HOLD ? ("<b>" + balance.c.b.toPlainString() + "</b>") : balance.c.b.toPlainString())
                        + " / " + (balancePosition == TransactionAmount.ACTION_SPEND ? ("<b>" + balance.d.b.toPlainString() + "</b>") : balance.d.b.toPlainString());
            }
            this.jlabel_RecipientDetail.setText("<html>" + details);
        }

        updateBalances();
    }

    private void updateBalances() {
        if (creator == null || asset == null) {
            jLabel_Balances.setText("");
            return;
        }

        String details = Lang.getInstance().translate("Balances") + ": ";

        Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>>
                balance = creator.getBalance(asset.getKey());
        if (balance != null) {
            details += (balancePosition == TransactionAmount.ACTION_SEND ? ("<b>" + balance.a.b.toPlainString() + "</b>") : balance.a.b.toPlainString())
                    + " / " + (balancePosition == TransactionAmount.ACTION_DEBT ? ("<b>" + balance.b.b.toPlainString() + "</b>") : balance.b.b.toPlainString())
                    + " / " + (balancePosition == TransactionAmount.ACTION_HOLD ? ("<b>" + balance.c.b.toPlainString() + "</b>") : balance.c.b.toPlainString())
                    + " / " + (balancePosition == TransactionAmount.ACTION_SPEND ? ("<b>" + balance.d.b.toPlainString() + "</b>") : balance.d.b.toPlainString());
        }
        this.jLabel_Balances.setText("<html>" + details);

    }

    protected void checkReadyToOK() {

        if (creator == null || recipient == null || asset == null) {
            jButton_ok.setEnabled(false);
            return;
        }

        jButton_ok.setEnabled(true);

    }

    private void refreshReceiverDetails() {

        Fun.Tuple2<Account, String> resultMake = Account.tryMakeAccount(recipientAddress.getSelectedAddress());
        if (resultMake.b != null) {
            recipient = null;
            this.jlabel_RecipientDetail.setText(Lang.getInstance().translate(resultMake.b));
            checkReadyToOK();
            return;
        }

        recipient = resultMake.a;

        refreshLabels();

    }

    public boolean cheskError() {
        this.jButton_ok.setEnabled(false);

        //READ SENDER
        creator = (Account) jComboBoxCreator.getSelectedItem();
        //CHECK IF WALLET UNLOCKED
        if (!Controller.getInstance().isWalletUnlocked()) {
            //ASK FOR PASSWORD
            String password = PasswordPane.showUnlockWalletDialog(this);
            if (password.equals("")) {
                this.jButton_ok.setEnabled(true);
                return false;
            }
            if (!Controller.getInstance().unlockWallet(password)) {
                //WRONG PASSWORD
                JOptionPane.showMessageDialog(null, Lang.getInstance().translate("Invalid password"), Lang.getInstance().translate("Unlock Wallet"), JOptionPane.ERROR_MESSAGE);

                //ENABLE
                this.jButton_ok.setEnabled(true);
                return false;
            }
        }

        exLink = null;
        Long linkRef = Transaction.parseDBRef(exLinkText.getText());
        if (linkRef != null) {
            exLink = new ExLinkAppendix(linkRef);
        }

        int parsing = 0;

        try {
            //READ AMOUNT
            parsing = 1;

            //READ FEE
            parsing = 2;
            feePow = Integer.parseInt((String) this.jComboBox_Fee.getSelectedItem());
        } catch (Exception e) {
        }

        try {
            //READ AMOUNT
            parsing = 1;
            amount = new BigDecimal(jTextField_Amount.getText());

            //READ FEE
            parsing = 2;
            feePow = Integer.parseInt((String) this.jComboBox_Fee.getSelectedItem());
        } catch (Exception e) {
            //CHECK WHERE PARSING ERROR HAPPENED
            switch (parsing) {
                case 1:

                    JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid amount!"), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
                    break;

                case 2:

                    JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid fee!"), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
                    break;
            }
            //ENABLE
            this.jButton_ok.setEnabled(true);
            return false;
        }

        if (amount.equals(new BigDecimal("0.0"))) {
            JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Amount must be greater 0.0"), Lang.getInstance().translate("Error") + ":  " + Lang.getInstance().translate("Invalid amount!"), JOptionPane.ERROR_MESSAGE);

            //ENABLE
            this.jButton_ok.setEnabled(true);
            return false;
        }

        boolean asText = this.jCheckBox_isText.isSelected();
        isTextByte = (asText) ? new byte[]{1} : new byte[]{0};

        this.message = jTextArea_Message.getText();
        messageBytes = null;
        if (message != null && message.length() > 0) {
            if (isTextByte[0] > 0) {
                messageBytes = message.getBytes(StandardCharsets.UTF_8);
            } else {
                try {
                    messageBytes = Converter.parseHexString(message);
                } catch (Exception g) {
                    try {
                        messageBytes = Base58.decode(message);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Message format is not base58 or hex!"), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);

                        //ENABLE
                        this.jButton_ok.setEnabled(true);
                        return false;
                    }
                }
            }
        }
        // if no TEXT - set null
        if (messageBytes != null && messageBytes.length == 0) messageBytes = null;
        // if amount = 0 - set null
        if (amount.compareTo(BigDecimal.ZERO) == 0) amount = null;

        boolean encryptMessage = this.jCheckBox_Encrypt.isSelected();
        encrypted = (encryptMessage) ? new byte[]{1} : new byte[]{0};

        if (amount != null) {
            //CHECK IF PAYMENT OR ASSET TRANSFER
            asset = (AssetCls) this.jComboBox_Asset.getSelectedItem();
            key = asset.getKey();
        }

        if (messageBytes != null) {
            if (messageBytes.length > BlockChain.MAX_REC_DATA_BYTES) {
                JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Message size exceeded!") + " <= MAX", Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);

                //ENABLE
                this.jButton_ok.setEnabled(true);
                return false;
            }

            if (encryptMessage) {
                //sender
                PrivateKeyAccount account = Controller.getInstance().getWalletPrivateKeyAccountByAddress(creator.getAddress());
                byte[] privateKey = account.getPrivateKey();

                //recipient
                byte[] publicKey;
                if (recipient instanceof PublicKeyAccount) {
                    publicKey = ((PublicKeyAccount) recipient).getPublicKey();
                } else {
                    publicKey = Controller.getInstance().getPublicKeyByAddress(recipient.getAddress());
                }

                if (publicKey == null) {
                    JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate(
                            ApiErrorFactory.getInstance().messageError(ApiErrorFactory.ERROR_NO_PUBLIC_KEY)),
                            Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);

                    //ENABLE
                    this.jButton_ok.setEnabled(true);

                    return false;
                }

                messageBytes = AEScrypto.dataEncrypt(messageBytes, privateKey, publicKey);
            }
        }
        txTitle = this.jTextFieldTXTitle.getText();
        if (txTitle == null)
            txTitle = "";
        if (txTitle.getBytes(StandardCharsets.UTF_8).length > 256) {

            JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Title size exceeded!") + " <= 256", Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
            return false;

        }


        return true;
    }

    private void viewLinkParent() {
        String refStr = exLinkText.getText();
        Transaction parentTx = Controller.getInstance().getTransaction(refStr);
        if (parentTx == null) {
            exLinkDescription.setText(Lang.getInstance().translate("Not Found") + "!");
        } else {
            exLinkDescription.setText(parentTx.toStringShortAsCreator());
        }
    }

    // выполняемая процедура при изменении адреса получателя
    @Override
    public void recipientAddressWorker(String e) {
        refreshReceiverDetails();
    }

    protected abstract BigDecimal getAmount();

    protected abstract Long getAssetKey();

    public void onSendClick() {

        // confirm params
        if (!cheskError()) return;

        // CREATE TX MESSAGE
        Transaction transaction = Controller.getInstance().r_Send((byte) 2, backward ? TransactionAmount.BACKWARD_MASK : 0,
                (byte) 0, Controller.getInstance().getWalletPrivateKeyAccountByAddress(creator.getAddress()), exLink, feePow,
                recipient, getAssetKey(), getAmount(), txTitle, messageBytes, isTextByte, encrypted);

        String Status_text = "";
        IssueConfirmDialog confirmDialog = new IssueConfirmDialog(null, true, transaction,
                Lang.getInstance().translate(asset.viewAssetTypeActionOK(backward, balancePosition,
                        creator != null && creator.equals(asset.getOwner()))),
                (int) (this.getWidth() / 1.2), (int) (this.getHeight() / 1.2), Status_text,
                Lang.getInstance().translate("Confirmation Transaction"), !noReceive);
        Send_RecordDetailsFrame ww = new Send_RecordDetailsFrame((RSend) transaction);

        confirmDialog.jScrollPane1.setViewportView(ww);
        confirmDialog.pack();
        confirmDialog.setLocationRelativeTo(this);
        confirmDialog.setVisible(true);

        // JOptionPane.OK_OPTION
        if (confirmDialog.isConfirm > 0) {
            ResultDialog.make(this, transaction, confirmDialog.isConfirm == IssueConfirmDialog.TRY_FREE);
        }

        // ENABLE
        this.jButton_ok.setEnabled(true);
    }


    private void initComponents(String message) {

        jLabelRecipientDetail = new javax.swing.JLabel(Lang.getInstance().translate("Recipient Details") + ":");
        jLabelCreator = new javax.swing.JLabel(Lang.getInstance().translate("Sender") + ":");
        jLabelRecipient = new javax.swing.JLabel(Lang.getInstance().translate("Recipient") + ":");
        jComboBoxCreator = new javax.swing.JComboBox<>();
        jLabelTXTitle = new javax.swing.JLabel();
        jlabel_RecipientDetail = new javax.swing.JLabel();
        jLabel_Title = new javax.swing.JLabel();
        jTextFieldTXTitle = new javax.swing.JTextField();
        jLabel_Mess = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea_Message = new javax.swing.JTextArea();
        jCheckBox_Encrypt = new javax.swing.JCheckBox();
        jCheckBox_isText = new javax.swing.JCheckBox();
        jLabel_Asset = new javax.swing.JLabel();
        jLabel_AssetType = new javax.swing.JLabel();
        jComboBox_Asset = new javax.swing.JComboBox<>();
        jLabel_Amount = new javax.swing.JLabel();
        jTextField_Amount = new MDecimalFormatedTextField();
        jLabel_Balances = new javax.swing.JLabel();
        jLabel_Fee = new javax.swing.JLabel();
        jComboBox_Fee = new javax.swing.JComboBox<>();
        jButton_ok = new javax.swing.JButton();

        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea_Account_Description = new javax.swing.JTextArea();

        exLinkTextLabel = new JLabel(Lang.getInstance().translate("Append to") + ":");
        exLinkText = new JTextField();
        exLinkDescriptionLabel = new JLabel(Lang.getInstance().translate("Parent") + ":");
        exLinkDescription = new JTextField();

        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[]{0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        layout.rowHeights = new int[]{0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0, 8, 0};
        setLayout(layout);

        java.awt.GridBagConstraints gridBagConstraints;

        java.awt.GridBagConstraints labelGBC;
        labelGBC = new java.awt.GridBagConstraints();
        labelGBC.gridwidth = 3;
        labelGBC.anchor = java.awt.GridBagConstraints.EAST;
        labelGBC.insets = new java.awt.Insets(0, 20, 5, 0);

        java.awt.GridBagConstraints fieldGBC;
        fieldGBC = new java.awt.GridBagConstraints();
        fieldGBC.gridx = 4;
        fieldGBC.gridwidth = 15;
        fieldGBC.fill = java.awt.GridBagConstraints.HORIZONTAL;
        fieldGBC.weightx = 0.4;
        fieldGBC.insets = new java.awt.Insets(0, 5, 5, 8);

        int gridy = 0;

        jLabel_Title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = gridy;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 17;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 6, 15);
        add(jLabel_Title, gridBagConstraints);

        labelGBC.gridy = ++gridy;
        add(jLabelCreator, labelGBC);
        fieldGBC.gridy = gridy;
        add(jComboBoxCreator, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabelRecipient, labelGBC);
        recipientAddress = new RecipientAddress(this, recipient);
        fieldGBC.gridy = gridy;
        add(recipientAddress, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabelRecipientDetail, labelGBC);
        fieldGBC.gridy = gridy;
        add(jlabel_RecipientDetail, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabelTXTitle, labelGBC);
        fieldGBC.gridy = gridy;
        add(jTextFieldTXTitle, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabel_Mess, labelGBC);

        jTextArea_Message.setColumns(20);
        jTextArea_Message.setRows(5);
        jTextArea_Message.setText(message == null ? "" : message);
        jScrollPane1.setViewportView(jTextArea_Message);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = fieldGBC.gridwidth;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = fieldGBC.weightx;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = fieldGBC.insets;
        add(jScrollPane1, gridBagConstraints);

        fieldGBC.gridy = ++gridy;
        add(jCheckBox_Encrypt, fieldGBC);

        fieldGBC.gridy = ++gridy;
        add(jCheckBox_isText, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabel_Asset, labelGBC);
        fieldGBC.gridy = gridy;
        add(jComboBox_Asset, fieldGBC);

        fieldGBC.gridy = ++gridy;
        add(jLabel_AssetType, fieldGBC);

        labelGBC.gridy = ++gridy;
        add(jLabel_Amount, labelGBC);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = fieldGBC.insets;
        add(jTextField_Amount, gridBagConstraints);

        jLabel_Balances.setHorizontalAlignment(SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx + 4;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.insets = fieldGBC.insets;
        add(jLabel_Balances, gridBagConstraints);

        labelGBC.gridy = ++gridy;
        add(jLabel_Fee, labelGBC);
        fieldGBC.gridy = gridy;
        jComboBox_Fee.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        add(jComboBox_Fee, fieldGBC);

        //exLink
        labelGBC.gridy = ++gridy;
        add(exLinkTextLabel, labelGBC);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = fieldGBC.insets;
        add(exLinkText, gridBagConstraints);

        exLinkDescriptionLabel.setText(Lang.getInstance().translate("Parent") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx + 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = fieldGBC.insets;
        add(exLinkDescriptionLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx + 3;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.insets = fieldGBC.insets;
        add(exLinkDescription, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = fieldGBC.gridx + 10;
        gridBagConstraints.gridy = gridy + 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 15, 15);
        add(jButton_ok, gridBagConstraints);

        jTextArea_Account_Description.setColumns(20);
        jTextArea_Account_Description.setRows(5);

        if (showAssetForm) {
            // не показываем теперь его
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 16;
            gridBagConstraints.gridwidth = 16;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 0.4;
            gridBagConstraints.weighty = 0.2;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 15);
            add(jScrollPane2, gridBagConstraints);
        }
    }


    public javax.swing.JButton jButton_ok;
    private javax.swing.JCheckBox jCheckBox_Encrypt;
    private javax.swing.JCheckBox jCheckBox_isText;
    private javax.swing.JComboBox<Account> jComboBoxCreator;
    public javax.swing.JComboBox<ItemCls> jComboBox_Asset;
    private javax.swing.JComboBox<String> jComboBox_Fee;
    private javax.swing.JLabel jLabel_Asset;
    private javax.swing.JLabel jLabel_AssetType;
    private javax.swing.JLabel jLabelCreator;
    private javax.swing.JLabel jLabel_Amount;
    private javax.swing.JLabel jLabel_Balances;
    private javax.swing.JLabel jLabel_Fee;
    private javax.swing.JLabel jLabel_Mess;
    private javax.swing.JLabel jLabelTXTitle;
    public javax.swing.JLabel jLabelRecipientDetail;
    public javax.swing.JLabel jLabel_Title;
    public javax.swing.JLabel jLabelRecipient;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea_Account_Description;
    public javax.swing.JTextArea jTextArea_Message;
    public MDecimalFormatedTextField jTextField_Amount;
    public javax.swing.JTextField jTextFieldTXTitle;
    private javax.swing.JLabel jlabel_RecipientDetail;
    public RecipientAddress recipientAddress;
    public JTextField exLinkText;
    public JTextField exLinkDescription;
    public JLabel exLinkTextLabel;
    public JLabel exLinkDescriptionLabel;


}
