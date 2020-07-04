package org.erachain.gui.items.statement;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.exdata.ExData;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.core.transaction.RSignNote;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.PasswordPane;
import org.erachain.gui.library.*;
import org.erachain.gui.transaction.RecDetailsFrame;
import org.erachain.lang.Lang;
import org.erachain.utils.MenuPopupUtil;
import org.json.simple.JSONObject;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple3;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author Саша
 */
@SuppressWarnings("serial")
public class RNoteInfo extends javax.swing.JPanel {

    public javax.swing.JPanel jPanel2;
    /**
     * Creates new form StatementInfo
     *
     * @param statement
     */
    RSignNote statement;
    RSignNote statementEncrypted;
    Transaction transaction;
    private MAttachedFilesPanel file_Panel;
    private VouchLibraryPanel voush_Library_Panel;
    private javax.swing.JLabel jLabel_Title;
    private javax.swing.JPanel jPanel1;
    private MSplitPane jSplitPane1;
    private MTextPane jTextArea_Body;

    public RNoteInfo(Transaction transaction) {
        if (transaction == null)
            return;
        this.transaction = transaction;
        statement = (RSignNote) transaction;
        statement.parseData();

        initComponents();

        viewInfo();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    //// <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel_Title = new javax.swing.JLabel();
        jSplitPane1 = new MSplitPane();
        jPanel1 = new javax.swing.JPanel();
        new javax.swing.JScrollPane();
        jTextArea_Body = new MTextPane();
        jPanel2 = new javax.swing.JPanel();
        file_Panel = new MAttachedFilesPanel();
        new javax.swing.JLabel();

        // jTable_Sign = new javax.swing.JTable();

        setLayout(new java.awt.GridBagLayout());

        JPanel pp = new RecDetailsFrame(transaction, true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 0, 11);
        add(pp, gridBagConstraints);

        jSplitPane1.setBorder(null);
        jSplitPane1.setOrientation(MSplitPane.VERTICAL_SPLIT);

        jPanel1.setLayout(new java.awt.GridBagLayout());
        int y = 0;

        // jTextArea_Body.setColumns(20);
        // jTextArea_Body.setRows(5);
        // jScrollPane3.setViewportView(jTextArea_Body);
        // jScrollPane3.getViewport().add(jTextArea_Body);
        jLabel_Title.setText(Lang.getInstance().translate("Title"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 0, 11);
        jPanel1.add(jLabel_Title, gridBagConstraints);

        //jTextArea_Body.setWrapStyleWord(true);
        //jTextArea_Body.setLineWrap(true);

        MenuPopupUtil.installContextMenu(jTextArea_Body);
        //jTextArea_Body.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 11, 11);

        JScrollPane scrol1 = new JScrollPane();
        scrol1.setViewportView(jTextArea_Body);
        jPanel1.add(scrol1, gridBagConstraints);

        if (statement.isEncrypted()) {
            JCheckBox encrypted = new JCheckBox(Lang.getInstance().translate("Encrypted"));
            encrypted.setSelected(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
            gridBagConstraints.gridy = ++y;
            gridBagConstraints.weightx = 0.1;
            gridBagConstraints.insets = new java.awt.Insets(11, 11, 11, 11);
            jPanel1.add(encrypted, gridBagConstraints);

            encrypted.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!encrypted.isSelected()) {
                        if (!Controller.getInstance().isWalletUnlocked()) {
                            //ASK FOR PASSWORD
                            String password = PasswordPane.showUnlockWalletDialog(null);
                            if (!Controller.getInstance().unlockWallet(password)) {
                                //WRONG PASSWORD
                                JOptionPane.showMessageDialog(null, Lang.getInstance().translate("Invalid password"), Lang.getInstance().translate("Unlock Wallet"), JOptionPane.ERROR_MESSAGE);

                                encrypted.setSelected(!encrypted.isSelected());

                                return;
                            }
                        }

                        statementEncrypted = statement;

                        Account account = Controller.getInstance().getInvolvedAccount(statement);
                        Fun.Tuple3<Integer, String, RSignNote> result = statement.decrypt(account);
                        if (result.a < 0) {
                            JOptionPane.showMessageDialog(null,
                                    Lang.getInstance().translate(result.b == null ? "Not exists Account access" : result.b),
                                    Lang.getInstance().translate("Not decrypted"), JOptionPane.ERROR_MESSAGE);
                            encrypted.setSelected(!encrypted.isSelected());

                            return;

                        } else if (result.b != null) {
                            JOptionPane.showMessageDialog(null,
                                    Lang.getInstance().translate(" In pos: " + result.a + " - " + result.b),
                                    Lang.getInstance().translate("Not decrypted"), JOptionPane.ERROR_MESSAGE);
                            encrypted.setSelected(!encrypted.isSelected());

                            return;

                        }

                        statement = result.c;
                        statement.parseData();
                        viewInfo();

                    } else {
                        // закроем доступ
                        statement = statementEncrypted;
                        viewInfo();
                    }
                }
            });

        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 11, 11);
        jPanel1.add(file_Panel, gridBagConstraints);

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 11, 11, 11);
        voush_Library_Panel = new VouchLibraryPanel(transaction);
        jPanel2.add(voush_Library_Panel, gridBagConstraints);
        //

        jSplitPane1.setRightComponent(jPanel2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(jSplitPane1, gridBagConstraints);
    }// </editor-fold>

    public void delay_on_Close() {
        voush_Library_Panel.delay_on_close();
    }

    @SuppressWarnings("unchecked")
    private void viewInfo() {
        String resultStr = "";
        ExData exData;

        exData = statement.getExData();
        exData.resolveValues(DCSet.getInstance());

        String title = exData.getTitle();
        if (title != null)
            jLabel_Title.setText(Lang.getInstance().translate("Title") + ": " + title);

        // recipients
        if (exData.hasRecipients()) {
            resultStr += "<h2>Recipients</h2>";
            Account[] recipients = exData.getRecipients();
            int i = 1;
            for (Account recipient : recipients) {
                resultStr += i + " " + recipient.getAddress() + "<br>";
            }
            resultStr += "<br>";
        }

        if (exData.isEncrypted()) {
            resultStr += "<h3>Encrypted</h3><br>";
            file_Panel.clear();
        }

        long templateKey = exData.getTemplateKey();
        if (templateKey > 0) {
            TemplateCls template = exData.getTemplate();
            resultStr += "<h2>" + template.getName() + "</h2>";
            String valuedText = exData.getValuedText();
            if (valuedText != null) {
                resultStr += Library.to_HTML(valuedText);
            }
            resultStr += "<hr><br>";
        }

        String message = exData.getMessage();
        if (message != null) {
            resultStr += Library.to_HTML(message) + "<br><br>";
        }

        if (exData.hasHashes()) {
            // hashes
            JSONObject hashes = exData.getHashes();
            resultStr += "<h3>Hashes</h3>";
            int i = 1;
            for (Object s : hashes.keySet()) {
                resultStr += i + " " + s + " " + hashes.get(s) + "<br>";
            }
            resultStr += "<br";
        }

        if (exData.hasFiles()) {
            HashMap<String, Tuple3<byte[], Boolean, byte[]>> files = exData.getFiles();
            Iterator<Entry<String, Tuple3<byte[], Boolean, byte[]>>> it_Files = files.entrySet().iterator();
            while (it_Files.hasNext()) {
                Entry<String, Tuple3<byte[], Boolean, byte[]>> file = it_Files.next();
                boolean zip = new Boolean(file.getValue().b);
                String name_File = file.getKey();
                byte[] file_byte = file.getValue().c;
                file_Panel.insert_Row(name_File, zip, file_byte);
            }
        }

        jTextArea_Body.setText(resultStr);
    }
}
