package org.erachain.gui.items.persons;

import com.toedter.calendar.JDateChooser;
import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.crypto.AEScrypto;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.persons.PersonHuman;
import org.erachain.core.transaction.IssuePersonRecord;
import org.erachain.core.transaction.Transaction;
import org.erachain.gui.MainFrame;
import org.erachain.gui.items.IssueItemPanel;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.library.MButton;
import org.erachain.gui.library.RecipientAddress;
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
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.TimeZone;

import static org.erachain.gui.items.utils.GUIUtils.checkWalletUnlock;

@SuppressWarnings("serial")
public class IssuePersonPanel extends IssueItemPanel implements RecipientAddress.RecipientAddressInterface {

    public static String NAME = "IssuePersonPanel";
    public static String TITLE = "Issue Person";

    private static final Logger logger = LoggerFactory.getLogger(IssuePersonPanel.class);
    protected JDateChooser txtBirthday;
    protected RecipientAddress registrarAddress;
    protected JLabel registrarAddressDesc = new JLabel();
    protected JDateChooser txtDeathDay;
    protected JComboBox<String> comboBoxGender = new JComboBox<>();
    //protected JTextField textPersonNumber = new JTextField();
    protected JTextField txtBirthLatitude = new JTextField("0.0, 0.0");
    protected JTextField txtBirthLongitudeLatitude = new JTextField("0");
    protected JTextField txtSkinColor = new JTextField();
    protected JTextField txtEyeColor = new JTextField();
    protected JTextField txtHairColor = new JTextField();
    protected JTextField txtHeight = new JTextField("170");
    protected MButton copyButton;
    protected JLabel jLabelRegistrarAddress = new JLabel(Lang.getInstance().translate("Registrar") + ":");
    private JLabel jLabelBirthLatitudeLongtitude = new JLabel(Lang.getInstance().translate("Coordinates of Birth") + ":");
    private JLabel jLabelBirthday = new JLabel(Lang.getInstance().translate("Birthday") + ":");
    protected JLabel jLabelDead = new JLabel(Lang.getInstance().translate("Deathday") + ":");
    private JLabel jLabelEyeColor = new JLabel(Lang.getInstance().translate("Eye color") + ":");
    private JLabel jLabelGender = new JLabel(Lang.getInstance().translate("Gender") + ":");
    private JLabel jlabelhairColor = new JLabel(Lang.getInstance().translate("Hair color") + ":");
    private JLabel jLabelHeight = new JLabel(Lang.getInstance().translate("Growth") + ":");
    //private JLabel jLabelPersonNumber = new JLabel(Lang.getInstance().translate("Person number") + ":");
    protected JPanel jPanelHead = new JPanel();
    protected JCheckBox aliveCheckBox = new JCheckBox(Lang.getInstance().translate("Alive"), true);

    public IssuePersonPanel() {
        this(NAME, TITLE);
    }

    public IssuePersonPanel(String name, String title) {
        super(name, title);
        initComponents();
        initLabels();

    }

    private void initLabels() {
        txtDeathDay.setVisible(false);
        jLabelDead.setVisible(false);
        aliveCheckBox.addActionListener(arg0 -> {
            if (aliveCheckBox.isSelected()) {
                txtDeathDay.setVisible(false);
                jLabelDead.setVisible(false);
            } else {
                txtDeathDay.setVisible(true);
                jLabelDead.setVisible(true);
            }
        });

        String[] items = PersonCls.GENDERS_LIST;
        items = Lang.getInstance().translate(items);
        comboBoxGender.setModel(new DefaultComboBoxModel<>(items));
        comboBoxGender.setSelectedIndex(2);
        setVisible(true);

    }

    protected void initComponents() {
        super.initComponents();

        // вывод верхней панели
        int yPos = super.initTopArea();

        GridBagConstraints gridBagConstraints;

        copyButton = new MButton(Lang.getInstance().translate("Create and copy to clipboard"), 2);
        copyButton.addActionListener(e -> onIssueClick());


        // SET ONE TIME ZONE for Birthday
        TimeZone tz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        txtBirthday = new JDateChooser("yyyy-MM-dd HH:mm 'UTC'", "####-##-## ##:##", '_');
        Calendar calendar = Calendar.getInstance(tz);
        calendar.set(1990, Calendar.NOVEMBER, 11, 12, 13, 1);
        txtBirthday.setCalendar(calendar);
        txtDeathDay = new JDateChooser("yyyy-MM-dd HH:mm 'UTC'", "####-##-## ##:##", '_');
        TimeZone.setDefault(tz);

        int y = yPos;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 8;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanelMain.add(titleJLabel, gridBagConstraints);

        y++;
        txtBirthday.setFont(UIManager.getFont("TextField.font"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        jPanelMain.add(txtBirthday, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 15;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new Insets(10, 18, 0, 16);
        jPanelMain.add(jPanelHead, gridBagConstraints);

        // gender
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPos + 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        jPanelMain.add(comboBoxGender, gridBagConstraints);

        // born
        y++;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 18, 0, 0);
        jPanelMain.add(jLabelBirthday, gridBagConstraints);

        y++;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 18, 0, 0);
        jPanelMain.add(jLabelGender, gridBagConstraints);


        //HairСolor
        y++;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 18, 0, 0);
        jPanelMain.add(jlabelhairColor, gridBagConstraints);

        //BirthLatitude
        y++;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 18, 0, 0);
        jPanelMain.add(jLabelBirthLatitudeLongtitude, gridBagConstraints);

        // label registrar address
        y += 2;
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 18, 0, 0);
        jPanelMain.add(jLabelRegistrarAddress, gridBagConstraints);

        registrarAddress = new RecipientAddress(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        //      gridBagConstraints.insets = new Insets(5, 5, 5, 8);
        jPanelMain.add(registrarAddress, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        jPanelMain.add(registrarAddressDesc, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = ++y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 16);
        jPanelMain.add(copyButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPos + 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        jPanelMain.add(txtHairColor, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPos + 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        jPanelMain.add(txtBirthLatitude, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPos + 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 1);
        jPanelMain.add(textFeePow, gridBagConstraints);

        // dead
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = yPos + 6;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        jPanelMain.add(aliveCheckBox, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = yPos + 6;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        jPanelMain.add(jLabelDead, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = yPos + 8;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        //mainPanel.add(jLabelPersonNumber, gridBagConstraints);

        // EyeColor
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = yPos + 10;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        jPanelMain.add(jLabelEyeColor, gridBagConstraints);

        // Height
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = yPos + 12;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        jPanelMain.add(jLabelHeight, gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = yPos + 6;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 16);
        jPanelMain.add(txtDeathDay, gridBagConstraints);
        txtDeathDay.setFont(UIManager.getFont("TextField.font"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = yPos + 8;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 16);
        //mainPanel.add(textPersonNumber, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = yPos + 10;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 16);
        jPanelMain.add(txtEyeColor, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = yPos + 12;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 16);
        jPanelMain.add(txtHeight, gridBagConstraints);

        /* Added Copy, Paste in GEO (by Samartsev. 18.03.2019) */
        JPopupMenu popup = new JPopupMenu();
        txtBirthLatitude.add(popup);
        txtBirthLatitude.setComponentPopupMenu(popup);

        JMenuItem jMenuItemCopy = new JMenuItem(Lang.getInstance().translate("Копировать"), KeyEvent.VK_C);
        jMenuItemCopy.setMnemonic(KeyEvent.VK_C);
        jMenuItemCopy.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, InputEvent.CTRL_MASK));

        JMenuItem jMenuItemPaste = new JMenuItem(Lang.getInstance().translate("Вставить"), KeyEvent.VK_V);
        jMenuItemPaste.setMnemonic(KeyEvent.VK_V);
        jMenuItemPaste.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_V, InputEvent.CTRL_MASK));

        popup.add(jMenuItemCopy);
        popup.add(jMenuItemPaste);
        jMenuItemCopy.addActionListener(e -> {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = toolkit.getSystemClipboard();
            StringSelection coordString = new StringSelection(txtBirthLatitude.getText());
            clipboard.setContents(coordString, null);
        });
        jMenuItemPaste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable transferable = clipboard.getContents(this);
                if (transferable == null) {
                    return;
                }
                try {
                    String dataBase58 = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    dataBase58 = dataBase58.trim();
                    dataBase58 = dataBase58.replaceAll("\n", "");
                    txtBirthLatitude.setText(dataBase58);
                } catch (Exception exception) {
                    logger.error("Error menu paste", exception);
                }
            }
        });

       /* // set acoount TO
        this.registrarAddress.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshReceiverDetails();
            }
        });*/

        // вывод подвала
        super.initBottom(y);

    }


    protected void reset() {
        textName.setText("");
        textAreaDescription.setText("");
        addImageLabel.reset();
    }

    public void onIssueClick() {
        boolean forIssue = false;

        // DISABLE
        copyButton.setEnabled(false);

        if (checkWalletUnlock(copyButton)) {
            return;
        }

        // READ CREATOR
        Account sender = (Account) this.fromJComboBox.getSelectedItem();

        int parse = 0;
        int feePow;
        byte gender;
        long birthday;
        long deathday;
        float birthLatitude;
        float birthLongitude;
        int height;
        try {
            // READ FEE POW
            feePow = Integer.parseInt((String) textFeePow.getSelectedItem());
            // READ GENDER
            parse++;
            gender = (byte) (comboBoxGender.getSelectedIndex());
            parse++;
            // SET TIMEZONE to UTC-0
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            birthday = txtBirthday.getCalendar().getTimeInMillis();
            parse++;
            // END DATE
            try {
                deathday = txtDeathDay.getCalendar().getTimeInMillis();
            } catch (Exception ed1) {
                deathday = birthday - 1;
            }
            if (aliveCheckBox.isSelected()) {
                deathday = birthday - 1;
            }
            parse++;
            String[] latitudeLongitude = txtBirthLatitude.getText().split(",");
            birthLatitude = Float.parseFloat(latitudeLongitude[0]);
            parse++;
            birthLongitude = Float.parseFloat(latitudeLongitude[1]);
            parse++;
            height = Integer.parseInt(txtHeight.getText());

        } catch (Exception e) {
            String mess = "Invalid pars... " + parse;
            switch (parse) {
                case 0:
                    mess = "Invalid fee power 0..6";
                    break;
                case 1:
                    mess = "Invalid gender";
                    break;
                case 2:
                    mess = "Invalid birthday [YYYY-MM-DD] or [YYYY-MM-DD hh:mm:ss]";
                    break;
                case 3:
                    mess = "Invalid deathday [YYYY-MM-DD] or [YYYY-MM-DD hh:mm:ss]";
                    break;
                case 4:
                    mess = "Invalid Coordinates of Birth, example: 43.123032, 131.917828";
                    break;
                case 5:
                    mess = "Invalid Coordinates of Birth, example: 43.123032, 131.917828";
                    break;
                case 6:
                    mess = "Invalid growth 10..255";
                    break;
            }
            JOptionPane.showMessageDialog(null, Lang.getInstance().translate(mess),
                    Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
            copyButton.setEnabled(true);
            return;
        }
        PrivateKeyAccount creator = Controller.getInstance().getWalletPrivateKeyAccountByAddress(sender.getAddress());
        if (creator == null) {
            JOptionPane.showMessageDialog(new JFrame(),
                    Lang.getInstance().translate(OnDealClick.resultMess(Transaction.PRIVATE_KEY_NOT_FOUND)),
                    Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Pair<Transaction, Integer> result = Controller.getInstance().issuePerson(forIssue, creator,
                linkTo, textName.getText(), feePow, birthday, deathday, gender,
                "", //textPersonNumber.getText(),
                birthLatitude,
                birthLongitude, txtSkinColor.getText(), txtEyeColor.getText(), txtHairColor.getText(),
                height, null, addImageLabel.getImgBytes(), textAreaDescription.getText(),
                creator, null);

        IssuePersonRecord issuePersonRecord = (IssuePersonRecord) result.getA();

        // CHECK VALIDATE MESSAGE
        if (result.getB() == Transaction.VALIDATE_OK) {
            if (!forIssue) {
                PersonHuman personHuman = (PersonHuman) issuePersonRecord.getItem();
                // SIGN
                personHuman.sign(creator);
                byte[] issueBytes = personHuman.toBytes(false, false);
                String base58str = Base58.encode(issueBytes);
                if (registrar == null) {
                    // copy to clipBoard

                    // This method writes a string to the system clipboard.
                    // otherwise it returns null.
                    StringSelection stringSelection = new StringSelection(base58str);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.getInstance().translate("Person bytecode has been copy to buffer") + "!",
                            Lang.getInstance().translate("Success"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // send telegram
                    byte[] encryptedBytes = AEScrypto.dataEncrypt(issueBytes, creator.getPrivateKey(), registrar.getPublicKey());

                    Transaction transaction = Controller.getInstance().r_Send(
                            creator, null, feePow, registrar, 0L,
                            null, "Person bytecode", encryptedBytes,
                            new byte[1], new byte[]{1}, 0);

                    Controller.getInstance().broadcastTelegram(transaction, true);
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.getInstance().translate("Person bytecode has been send to Registrar") + "!",
                            Lang.getInstance().translate("Success"), JOptionPane.INFORMATION_MESSAGE);

                }

                // ENABLE
                copyButton.setEnabled(true);
                return;
            }
            String statusText = "";
            IssueConfirmDialog issueConfirmDialog = new IssueConfirmDialog(MainFrame.getInstance(), true, issuePersonRecord,
                    " ",
                    (int) (getWidth() / 1.2), (int) (getHeight() / 1.2), statusText,
                    Lang.getInstance().translate("Confirmation transaction issue person"));

            IssuePersonDetailsFrame issuePersonDetailsFrame = new IssuePersonDetailsFrame(issuePersonRecord);
            issueConfirmDialog.jScrollPane1.setViewportView(issuePersonDetailsFrame);
            issueConfirmDialog.setLocationRelativeTo(this);
            issueConfirmDialog.setVisible(true);
            if (issueConfirmDialog.isConfirm) {
                // VALIDATE AND PROCESS
                Integer afterCreateResult = Controller.getInstance().getTransactionCreator().afterCreate(result.getA(), Transaction.FOR_NETWORK);
                if (afterCreateResult != Transaction.VALIDATE_OK) {
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.getInstance().translate(OnDealClick.resultMess(afterCreateResult)),
                            Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(new JFrame(),
                            Lang.getInstance().translate("Person issue has been sent!"),
                            Lang.getInstance().translate("Success"), JOptionPane.INFORMATION_MESSAGE);
                }

            }
        } else if (result.getB() == Transaction.INVALID_NAME_LENGTH_MIN) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                    Lang.getInstance().translate("Name must be more then %val characters!")
                            .replace("%val", "" + issuePersonRecord.getItem().getMinNameLen()),
                    Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (result.getB() == Transaction.INVALID_NAME_LENGTH_MAX) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                    Lang.getInstance().translate("Name must be less then %val characters!")
                            .replace("%val", "" + ItemCls.MAX_NAME_LENGTH),
                    Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(new JFrame(),
                    Lang.getInstance().translate(OnDealClick.resultMess(result.getB())),
                    Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
        }


        // ENABLE
        copyButton.setEnabled(true);
    }

    PublicKeyAccount registrar;

    private void refreshReceiverDetails(String registrarStr) {

        //Account
        this.registrarAddressDesc.setText(Lang.getInstance().translate(
                Account.getDetailsForEncrypt(registrarStr, AssetCls.FEE_KEY, true)));

        registrar = null;
        if (registrarStr != null && !registrarStr.isEmpty()) {
            if (Crypto.getInstance().isValidAddress(registrarStr)) {
                byte[] pubKey = Controller.getInstance().getPublicKeyByAddress(registrarStr);
                if (pubKey == null) {
                    registrar = null;
                } else {
                    registrar = new PublicKeyAccount(pubKey);
                }
            } else {
                if (PublicKeyAccount.isValidPublicKey(registrarStr)) {
                    registrar = new PublicKeyAccount(registrarStr);
                }
            }
        }

        if (registrar == null) {
            copyButton.setText(Lang.getInstance().translate("Create and copy to clipboard"));
        } else {
            copyButton.setText(Lang.getInstance().translate("Create and send to Registrar"));
        }
    }

    // выполняемая процедура при изменении адреса получателя
    @Override
    public void recipientAddressWorker(String e) {
        refreshReceiverDetails(e);
    }
}