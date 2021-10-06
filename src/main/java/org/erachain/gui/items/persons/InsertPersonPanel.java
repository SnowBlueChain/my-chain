package org.erachain.gui.items.persons;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.crypto.Base58;
import org.erachain.core.exdata.exLink.ExLinkAppendix;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.persons.PersonFactory;
import org.erachain.core.item.persons.PersonHuman;
import org.erachain.core.transaction.IssuePersonRecord;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.MainFrame;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.library.MButton;
import org.erachain.gui.transaction.IssuePersonDetailsFrame;
import org.erachain.gui.transaction.OnDealClick;
import org.erachain.lang.Lang;
import org.erachain.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import static org.erachain.gui.items.utils.GUIUtils.checkWalletUnlock;

public class InsertPersonPanel extends IssuePersonPanel {

    public static String NAME = "InsertPersonPanel";
    public static String TITLE = "Insert Person";

    private static final Logger logger = LoggerFactory.getLogger(InsertPersonPanel.class);
    private static final long serialVersionUID = 1L;

    private JTextField txtSign = new JTextField();
    private JTextField txtPublicKey = new JTextField();

    private MButton insertButton;
    private MButton insertAndCertifyButton;

    private JLabel labelSign = new JLabel();
    private JLabel labelPublicKey = new JLabel();
    protected MButton pasteButton;

    protected PersonHuman person;

    public InsertPersonPanel() {
        super(NAME, TITLE, "Person issue has been sent!");
        init();

        // нужно чтобы нельзя было случайно вставить по Ctrl-C в это поле чего угодно
        txtBirthLatitude.removeAll();
        txtBirthLatitude.setComponentPopupMenu(null);

        issueJButton.setVisible(false);
        aliveCheckBox.setVisible(false);
    }

    public String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                result = result.trim().replaceAll("\n", "");
            } catch (Exception e) {
                logger.error("Clipboard error", e);
            }
        }
        return result;
    }

    private void init() {

        int gridy = super.initComponents(false);
        initLabels();

        exLinkTextLabel.setVisible(true);
        exLinkText.setVisible(true);

        txtBirthLatitude.setText("");
        txtBirthLongitudeLatitude.setText("");
        txtHeight.setText("");
        textFeePow.setSelectedItem("0");
        nameField.setEditable(false);
        textAreaDescription.setEditable(false);
        txtBirthday.setEnabled(false);
        txtDeathDay.setEnabled(false);

        registrarAddress.setEnabled(false);
        registrarAddressDesc.setEnabled(false);

        addImageLabel.setEnabled(false);
        comboBoxGender.setEnabled(false);
        txtBirthLatitude.setEditable(false);
        txtBirthLongitudeLatitude.setEditable(false);
        txtSkinColor.setEditable(false);
        txtEyeColor.setEditable(false);
        txtHairColor.setEditable(false);
        txtHeight.setEditable(false);

        txtPublicKey.setEditable(false);

        labelSign.setText(Lang.T("Signature") + ":");
        labelGBC.gridy = ++gridy;
        jPanelAdd.add(labelSign, labelGBC);

        fieldGBC.gridy = gridy++;
        txtSign.setEditable(false);
        jPanelAdd.add(txtSign, fieldGBC);

        labelPublicKey.setText(Lang.T("Public key") + ":");
        labelGBC.gridy = gridy;
        jPanelAdd.add(labelPublicKey, labelGBC);

        fieldGBC.gridy = gridy++;
        txtPublicKey.setEditable(false);
        jPanelAdd.add(txtPublicKey, fieldGBC);

        pasteButton = new MButton(Lang.T("Paste Person from clipboard"), 2);
        pasteButton.addActionListener(arg0 -> {
            String baseXXstr = getClipboardContents();
            try {
                byte[] dataPerson = Base58.decode(baseXXstr);
                setByteCode(dataPerson);
            } catch (Exception ee) {
                try {
                    byte[] dataPerson = Base64.getDecoder().decode(baseXXstr);
                    setByteCode(dataPerson);
                } catch (Exception eee) {
                    JOptionPane.showMessageDialog(null, ee.getMessage(), Lang.T("Error"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

        });

        fieldGBC.gridy = gridy++;
        jPanelAdd.add(pasteButton, fieldGBC);

        insertButton = new MButton(Lang.T("Check person and insert"), 2);
        insertButton.addActionListener(arg0 -> {
            insert(false);
        });

        insertAndCertifyButton = new MButton(Lang.T("Check person, insert and certify pubkey"), 2);
        insertAndCertifyButton.addActionListener(arg0 -> {
            insert(true);
        });

        fieldGBC.gridy = gridy++;
        jPanelAdd.add(insertButton, fieldGBC);

        fieldGBC.gridy = gridy++;
        jPanelAdd.add(insertAndCertifyButton, fieldGBC);

        super.initBottom(gridy);

    }

    private void insert(boolean andCertify) {
        if (person == null) {
            return;
        }
        if (checkWalletUnlock()) {
            return;
        }

        // READ CREATOR
        Account creatorAccount = (Account) fromJComboBox.getSelectedItem();

        int feePow;
        try {
            // READ FEE POW
            feePow = Integer.parseInt((String) textFeePow.getSelectedItem());
        } catch (Exception e) {
            String mess = "Invalid fee power 0..6";
            JOptionPane.showMessageDialog(new JFrame(), Lang.T(mess),
                    Lang.T("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrivateKeyAccount creator = Controller.getInstance()
                .getWalletPrivateKeyAccountByAddress(creatorAccount.getAddress());

        if (creator == null) {
            JOptionPane.showMessageDialog(new JFrame(),
                    Lang.T(OnDealClick.resultMess(Transaction.PRIVATE_KEY_NOT_FOUND)),
                    Lang.T("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Long linkRef = Transaction.parseDBRef(exLinkText.getText());
        if (linkRef != null) {
            exLink = new ExLinkAppendix(linkRef);
        }

        Pair<Transaction, Integer> result = Controller.getInstance().issuePersonHuman(creator, exLink, feePow, person, andCertify);

        // CHECK VALIDATE MESSAGE
        if (result.getB() == Transaction.VALIDATE_OK) {
            String statusText = "";

            IssueConfirmDialog confirmDialog = new IssueConfirmDialog(MainFrame.getInstance(), true, result.getA(),
                    " ",
                    (int) (getWidth() / 1.2), (int) (getHeight() / 1.2), statusText,
                    Lang.T("Confirmation transaction issue person"));

            IssuePersonDetailsFrame ww = new IssuePersonDetailsFrame((IssuePersonRecord) result.getA());
            confirmDialog.jScrollPane1.setViewportView(ww);
            confirmDialog.setLocationRelativeTo(this);
            confirmDialog.setVisible(true);
            if (confirmDialog.isConfirm > 0) {
                // VALIDATE AND PROCESS
                Integer result1 = Controller.getInstance().getTransactionCreator().afterCreate(result.getA(),
                        Transaction.FOR_NETWORK, confirmDialog.isConfirm == IssueConfirmDialog.TRY_FREE, false);
                if (result1 != Transaction.VALIDATE_OK) {
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.T(OnDealClick.resultMess(result1)),
                            Lang.T("Error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.T("Person issue has been sent!"),
                            Lang.T("Success"), JOptionPane.INFORMATION_MESSAGE);
                    person = null;
                    eraseFields();

                }
            }
        } else {
            JOptionPane.showMessageDialog(new JFrame(),
                    Lang.T(OnDealClick.resultMess(result.getB())),
                    Lang.T("Error"), JOptionPane.ERROR_MESSAGE);
        }

    }

    public void setByteCode(byte[] dataPerson) {
        person = null;
        reset();
        try {
            person = (PersonHuman) PersonFactory.getInstance().parse(Transaction.FOR_NETWORK, dataPerson, false);
        } catch (Exception ee) {
            JOptionPane.showMessageDialog(null, ee.getMessage(), Lang.T("Error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        nameField.setText(person.viewName());
        addImageLabel.set(person.getImage());
        addIconLabel.set(person.getIcon());

        // SET ONE TIME ZONE for Birthday
        TimeZone tz = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            txtBirthday.setDate(new Date(person.getBirthday()));
            if (!person.isAlive(0L)) {
                txtDeathDay.setDate(new Date(person.getDeathday()));
                txtDeathDay.setVisible(true);
                jLabelDead.setVisible(true);
            } else {
                txtDeathDay.setVisible(false);
                jLabelDead.setVisible(false);
            }
        } finally {
            TimeZone.setDefault(tz);
        }

        textAreaDescription.setText(person.getDescription() == null ? "" : person.getDescription());

        comboBoxGender.setSelectedIndex(person.getGender());

        txtBirthLatitude.setText("" + person.getBirthLatitude() + ", " + person.getBirthLongitude());
        if (person.getSkinColor() != null) {
            txtSkinColor.setText(person.getSkinColor());
        }
        if (person.getEyeColor() != null) {
            txtEyeColor.setText(person.getEyeColor());
        }
        if (person.getHairColor() != null) {
            txtHairColor.setText(person.getHairColor());
        }
        txtHeight.setText("" + person.getHeight());

        txtSign.setText(
                person.isSignatureValid(DCSet.getInstance()) ? Base58.encode(person.getMakerSignature())
                        : Lang.T("Wrong signature for data maker"));
        txtPublicKey.setText(Base58.encode(person.getMaker().getPublicKey()));

    }

    public PersonCls getPerson() {
        return person;
    }

    private void eraseFields() {
        textFeePow.setSelectedItem("0");
        nameField.setText("");
        textAreaDescription.setText("");
        txtBirthLatitude.setText("");
        txtBirthLongitudeLatitude.setText("");
        txtSkinColor.setText("");
        txtEyeColor.setText("");
        txtHairColor.setText("");
        txtHeight.setText("");
        addImageLabel.reset();
        txtSign.setText("");
        txtPublicKey.setText("");

    }

}
