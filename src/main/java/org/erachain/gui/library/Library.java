package org.erachain.gui.library;

import com.github.rjeschke.txtmark.Processor;
import net.sf.tinylaf.Theme;
import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.transaction.*;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.PlaySound;
import org.erachain.utils.SysTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

//import net.sf.tinylaf.Theme;

// почемуто иногда она не может найти эту библиотеку при запуске JAR - надо закоментить ее и опять вставить здесь
// по Alt-Enter на Класса с вызовом Theme. ниже в коде

/*
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.org.erachain.api.skin.*;
import org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel;
 */

public class Library {

    protected static Logger logger = LoggerFactory.getLogger(Library.class);

    // PLAY SOUND
    public static void notifySysTrayRecord(Transaction transaction) {

        if (transaction.getCreator() == null)
            return;

        if (transaction.noDCSet())
            transaction.setDC(DCSet.getInstance(), false);

        switch ( transaction.getType()) {
            case Transaction.SEND_ASSET_TRANSACTION:
                RSend r_Send = (RSend) transaction;

                // AS RECIPIENT
                Account account = Controller.getInstance().getWalletAccountByAddress(r_Send.getRecipient().getAddress());
                if (account != null) {
                    if (r_Send.hasAmount()) {
                        if (Settings.getInstance().isSoundReceivePaymentEnabled())
                            PlaySound.getInstance().playSound("receivepayment.wav");

                        SysTray.getInstance().sendMessage("Payment received",
                                "From: " + r_Send.getCreator().getPersonAsString() + "\nTo: " + r_Send.getRecipient().getPersonAsString() + "\n"
                                        + "Asset Key" + ": " + r_Send.getAbsKey() + ", " + "Amount" + ": "
                                        + r_Send.getAmount().toPlainString()
                                        + (r_Send.getHead() != null? "\n Title" + ":" + r_Send.getHead() : "")
                                ,
                                MessageType.INFO);

                    } else {
                        if (Settings.getInstance().isSoundReceiveMessageEnabled())
                            PlaySound.getInstance().playSound("receivemessage.wav");

                        SysTray.getInstance().sendMessage("Message received",
                                "From: " + r_Send.getCreator().getPersonAsString() + "\nTo: " + r_Send.getRecipient().getPersonAsString() + "\n"
                                        + (r_Send.getHead() != null? "\n Title" + ":" + r_Send.getHead() : "")
                                ,
                                MessageType.INFO);

                    }

                    return;
                }

                account = Controller.getInstance().getWalletAccountByAddress(r_Send.getCreator().getAddress());
                if (account != null) {
                    if (r_Send.hasAmount()) {

                        if (Settings.getInstance().isSoundNewTransactionEnabled())
                            PlaySound.getInstance().playSound("newtransaction.wav");

                        SysTray.getInstance().sendMessage("Payment send",
                                "From: " + transaction.getCreator().getPersonAsString() + "\nTo: " + r_Send.getRecipient().getPersonAsString() + "\n"
                                        + "Asset Key" + ": " + r_Send.getAbsKey() + ", " + "Amount" + ": "
                                        + r_Send.getAmount().toPlainString()
                                        + (r_Send.getHead() != null? "\n Title" + ":" + r_Send.getHead() : "")
                                ,
                                MessageType.INFO);

                    } else {

                        if (Settings.getInstance().isSoundNewTransactionEnabled())
                            PlaySound.getInstance().playSound("newtransaction.wav");

                        SysTray.getInstance().sendMessage("Message send",
                                "From: " + transaction.getCreator().getPersonAsString() + "\nTo: " + r_Send.getRecipient().getPersonAsString() + "\n"
                                        + (r_Send.getHead() != null? "\n Title" + ":" + r_Send.getHead() : "")
                                ,
                                MessageType.INFO);

                    }
                    return;
                }

            default:
                account = Controller.getInstance().getWalletAccountByAddress(transaction.getCreator().getAddress());
                if (account != null) {
                    if (Settings.getInstance().isSoundNewTransactionEnabled()) {
                        PlaySound.getInstance().playSound("newtransaction.wav");
                    }

                    SysTray.getInstance().sendMessage("Transaction send",
                            "From: " + transaction.getCreator().getPersonAsString() + "\n"
                                    + transaction.toString()
                            ,
                            MessageType.INFO);
                }
        }
    }

    public static void setGuiLookAndFeel() {

        // theme
        String name_Theme = Settings.getInstance().get_LookAndFell();

        if (name_Theme.equals("System")) {

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e1) {
                logger.error(e1.getMessage(), e1);
            }
        }

        if (name_Theme.equals("Metal")) {

            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //UIManager.getLookAndFeel();
            }
        }

        if (name_Theme.equals("Other")) {

            try {
                //UIManager.setLookAndFeel("de.muntjak.tinylookandfeel.TinyLookAndFeel");
                UIManager.setLookAndFeel("net.sf.tinylaf.TinyLookAndFeel");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        /*
         * //USE SYSTEM STYLE try { int a = 1; //
         * UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         * // UIManager.setLookAndFeel(
         * UIManager.getCrossPlatformLookAndFeelClassName()); // С‚РѕР¶Рµ С‡С‚Рѕ
         * UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
         * // work //
         * UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
         * ;; // РЅРµ СѓРІРµР»РёС‡РёРІР°РµС‚ С€СЂРёС„С‚С‹ //
         * UIManager.setLookAndFeel(
         * "com.sun.java.swing.plaf.motif.MotifLookAndFeel");; // work //
         * UIManager.setLookAndFeel(
         * "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); // works //
         * UIManager.setLookAndFeel(
         * "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"); //
         * works // UIManager.setLookAndFeel(
         * UIManager.getCrossPlatformLookAndFeelClassName());
         * //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel")
         * ;// // com.sun.java.swing.plaf.gtk.GTKLookAndFeel //
         * com.sun.java.swing.plaf.motif.MotifLookAndFeel //
         * com.sun.java.swing.plaf.windows.WindowsLookAndFeel
         *
         *
         *
         *
         */

        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        System.setProperty("sun.awt.noerasebackground", "true");
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        int size_font = new Integer(Settings.getInstance().get_Font());
        String name_font = Settings.getInstance().get_Font_Name();

        Font font = new Font(name_font, Font.TRUETYPE_FONT, size_font);
        UIManager.put("Button.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("TableHeader.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("RadioButton.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("CheckBox.font", font);
        UIManager.put("FormattedTextField.font", font);

        UIManager.put("Menu.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("Frame.titleFont", font);
        UIManager.put("InternalFrame.font", font);
        UIManager.put("InternalFrame.titleFont", font);

        UIManager.put("TextPane.font", font);
        // UIManager.put( "ScrollBar.minimumThumbSize", new Dimension(20,30) );
        // UIManager.put("ScrollBar.minimumThumbSize", new Dimension(25,25));
        // UIManager.put("Table.height", size_font*5);
        UIManager.put("TextArea.font", font);

        UIManager.put("InternalFrame.paletteTitleFont", font);
        UIManager.put("InternalFrame.normalTitleFont", font);

        UIManager.put("FileChooser.font", font);

        UIManager.put("CheckBoxMenuItem.acceleratorFont", font);
        UIManager.put("CheckBoxMenuItem.font", font);
        UIManager.put("ColorChooser.font", font);

        UIManager.put("EditorPane.font", font);
        UIManager.put("FormattedTextField.font", font);
        UIManager.put("IconButton.font", font);
        UIManager.put("InternalFrame.optionDialogTitleFont", font);
        UIManager.put("InternalFrame.paletteTitleFont", font);
        UIManager.put("InternalFrame.titleFont", font);
        UIManager.put("Label.font", font);
        UIManager.put("List.font", font);
        UIManager.put("Menu.acceleratorFont", font);
        UIManager.put("Menu.font", font);
        UIManager.put("MenuBar.font", font);
        UIManager.put("MenuItem.acceleratorFont", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("OptionPane.font", font);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("Panel.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("PopupMenu.font", font);
        UIManager.put("ProgressBar.font", font);
        UIManager.put("RadioButton.font", font);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", font);
        UIManager.put("RadioButtonMenuItem.font", font);
        UIManager.put("ScrollPane.fon", font);
        UIManager.put("Slider.font", font);
        UIManager.put("Spinner.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("TabbedPane.smallFont", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("ToggleButton.font", font);
        UIManager.put("ToolBar.font", font);
        UIManager.put("ToolTip.font", font);
        UIManager.put("Tree.font", font);

        UIManager.put("TitledBorder.font", font);
        UIManager.put("Panel.font", font);

        // text to button optionPane
        UIManager.put("OptionPane.yesButtonText", Lang.getInstance().translate("Confirm"));
        UIManager.put("OptionPane.noButtonText", Lang.getInstance().translate("Cancel"));
        UIManager.put("OptionPane.cancelButtonText", Lang.getInstance().translate("Cancel"));
        UIManager.put("OptionPane.okButtonText", Lang.getInstance().translate("OK"));
        UIManager.put("OptionPane.titleFont", font);

        UIManager.put("SplitPane.oneTouchButtonSize", size_font * 2);
        UIManager.put("SplitPane.supportsOneTouchButtons", true);
        UIManager.put("SplitPane.dividerSize", size_font);
        UIManager.put("SplitPaneDivider.oneTouchButtonSize", size_font * 2);
        UIManager.put("SplitPane.centerOneTouchButtons", true);
        UIManager.put("ArrowButton.size", size_font*2);

        if (size_font > 16)
            UIManager.put("ScrollBar.width", size_font);

        // .setUIFont(new
        // javax.swing.plaf.FontUIResource("Tahoma",Font.PLAIN,12));

        // ArrayList<Tuple2<String,Object>> ss = new ArrayList<Tuple2<String,
        // Object>>();

        // UIManager.put("RadioButton.focus", new Color(0, 0, 0, 0));
        // UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        // UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
        // UIManager.put("ComboBox.focus", new Color(0, 0, 0, 0));
        // UIManager.put("TextArea.font", UIManager.get("TextField.font"));

        int scrolH = (int) (size_font * 1.2);
        if (scrolH < 17)
            scrolH = 17;

        UIManager.put("InternalFrame.titlePaneHeight", scrolH);
        UIManager.put("InternalFrame.titleButtonHeight", scrolH);
        UIManager.put("InternalFrame.titleButtonWidth", scrolH);

        Theme.frameTitleFont.setFont(font);
        Theme.scrollSize.setValue(scrolH);

        Theme.internalPaletteTitleFont.setFont(font);
        Theme.toolTipFont.setFont(font);

        @SuppressWarnings("rawtypes")
        java.util.Enumeration keys = UIManager.getDefaults().keys();

        while (keys.hasMoreElements()) {

            String key = keys.nextElement().toString();
            if (key.contains("OptionPane")) {
            }
        }

        // font = font;

    }

    /*
     * public static void setupSubstance() { try { final String fileName =
     * System.getProperty("user.home") + System.getProperty("file.separator") +
     * "insubstantial.txt"; final Properties properties = new Properties();
     * LookAndFeel laf = new SubstanceGeminiLookAndFeel();
     * UIManager.setLookAndFeel(laf);
     * UIManager.put(SubstanceGeminiLookAndFeel.SHOW_EXTRA_WIDGETS,
     * Boolean.TRUE); JFrame.setDefaultLookAndFeelDecorated(true);
     * JDialog.setDefaultLookAndFeelDecorated(true);
     * Runtime.getRuntime().addShutdownHook(new Thread() {
     *
     * @Override public void run() { try { String skinClassName =
     * SubstanceLookAndFeel.getCurrentSkin().getClass().getCanonicalName();
     * properties.setProperty("skinClassName", skinClassName);
     * properties.store(new FileOutputStream(fileName), fileName); } catch
     * (Throwable t) { t.printStackTrace(); } } }); properties.load(new
     * FileInputStream(fileName)); String skinClassName =
     * properties.getProperty("skinClassName"); ((SubstanceLookAndFeel)
     * laf).setSkin(skinClassName); } catch (Throwable t) { t.printStackTrace();
     * } }
     */

    public static String to_HTML(String str) {

        return viewDescriptionHTML(str);

    }

    public static String isNum_And_Length(String str, int length) {
        try {
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            return "Not Namber";
        }
        if (str.length() != length)
            return "Error Size";
        return "ok";
    }

    public static BigDecimal getBlockSegToBigInteger(Transaction transaction) {
        if (transaction == null)
            return new BigDecimal(-2);
        if (transaction.isConfirmed(DCSet.getInstance())) {
            String m = transaction.getBlockHeight() + "";
            String d = transaction.getSeqNo() + "";
            int zz = 5 - d.length();
            for (int z = 0; z < zz; z++) {
                d = "0" + d;
            }
            String bd = m + "." + d;
            return new BigDecimal(bd).setScale(5);
        }
        return new BigDecimal(-1);

    }

    public static String viewDescriptionHTML(String descr) {

        if (descr.startsWith("#"))
            // MARK DOWN
            return Processor.process(descr);

        if (descr.startsWith("["))
            // FORUM CKeditor
            // TODO CK_editor INSERT
            return Processor.process(descr);

        if (descr.startsWith("{"))
            // it is DOCX
            // TODO DOCX insert
            return descr;

        if (descr.startsWith("<"))
            // it is HTML
            return descr;

        // PLAIN TEXT
        return descr.replaceAll(" ", "&ensp;").replaceAll("\t", "&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;")
                .replaceAll("\n", "<br>");

    }
    
    /**
     * Save JSON String to era File
     * @param parent - getParent()
     * @param JSONString - JSON STRING
     */
    public static void saveJSONStringToEraFile(Container parent, String JSONString){
        // String raw = Base58.encode(transaction.toBytes(false, null));
        FileChooser chooser = new FileChooser();
        chooser.setDialogTitle(Lang.getInstance().translate("Save File"));
        // chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("*.era","*.*");
        chooser.setFileFilter(filter);

        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {

            String pp = chooser.getSelectedFile().getPath();

            File ff = new File(pp + ".era");
            // if file
            if (ff.exists() && ff.isFile()) {
                int aaa = JOptionPane.showConfirmDialog(chooser,
                        Lang.getInstance().translate("File") + Lang.getInstance().translate("Exists") + "! "
                                + Lang.getInstance().translate("Overwrite") + "?",
                        Lang.getInstance().translate("Message"), JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                System.out.print("\n gggg " + aaa);
                if (aaa != 0) {
                    return;
                }
                ff.delete();

            }

            try (FileWriter fw = new FileWriter(ff)) {
                fw.write(JSONString);
            } catch (IOException e) {
                System.out.println(e);
            }

            
        }

       
    }
    
    public static void saveTransactionJSONtoFileSystem(Container parent,Transaction trans){
        String jsonString ="";
        switch (trans.getType()) {

        case Transaction.SIGN_NOTE_TRANSACTION:

            jsonString = ((RSignNote) trans).toJson().toJSONString();
            break;

        case Transaction.VOTE_ON_ITEM_POLL_TRANSACTION:

            jsonString = ((VoteOnItemPollTransaction) trans).toJson().toJSONString();
            break;

        case Transaction.ARBITRARY_TRANSACTION:

            jsonString = ((ArbitraryTransaction) trans).toJson().toJSONString();

            break;

        case Transaction.ISSUE_ASSET_TRANSACTION:

            jsonString = ((IssueAssetTransaction) trans).toJson().toJSONString();
            break;

        case Transaction.ISSUE_PERSON_TRANSACTION:

            jsonString = ((IssuePersonRecord) trans).toJson().toJSONString();

            break;

        case Transaction.ISSUE_POLL_TRANSACTION:

            jsonString = ((IssuePollRecord) trans).toJson().toJSONString();
            break;

        case Transaction.SET_STATUS_TO_ITEM_TRANSACTION:

            jsonString = ((RSetStatusToItem) trans).toJson().toJSONString();
            break;
           
        case Transaction.CREATE_ORDER_TRANSACTION:

            jsonString = ((CreateOrderTransaction) trans).toJson().toJSONString();
            break;

        case Transaction.CANCEL_ORDER_TRANSACTION:

            jsonString = ((CancelOrderTransaction) trans).toJson().toJSONString();
            break;

        case Transaction.MULTI_PAYMENT_TRANSACTION:

            jsonString = ((MultiPaymentTransaction) trans).toJson().toJSONString();
            break;

        case Transaction.SEND_ASSET_TRANSACTION:
            jsonString = ((RSend) trans).toJson().toJSONString();
            break;

        case Transaction.VOUCH_TRANSACTION:
            jsonString = ((RVouch) trans).toJson().toJSONString();
            break;

        case Transaction.CERTIFY_PUB_KEYS_TRANSACTION:
            jsonString = ((RSertifyPubKeys) trans).toJson().toJSONString();
            break;

        case Transaction.HASHES_RECORD:
            jsonString = ((RHashes) trans).toJson().toJSONString();
            break;

        case Transaction.ISSUE_IMPRINT_TRANSACTION:

            jsonString = ((IssueImprintRecord) trans).toJson().toJSONString();
            break;

        case Transaction.ISSUE_TEMPLATE_TRANSACTION:

            jsonString = ((IssueTemplateRecord) trans).toJson().toJSONString();
            break;
            
        case Transaction.ISSUE_UNION_TRANSACTION:

            jsonString = ((IssueUnionRecord) trans).toJson().toJSONString();
            break;

        case Transaction.ISSUE_STATUS_TRANSACTION:

            jsonString = ((IssueStatusRecord) trans).toJson().toJSONString();
            break;

        case Transaction.GENESIS_SEND_ASSET_TRANSACTION:

            jsonString = ((GenesisTransferAssetTransaction) trans).toJson().toJSONString();

            break;

        case Transaction.GENESIS_ISSUE_TEMPLATE_TRANSACTION:

            jsonString = ((GenesisIssueTemplateRecord) trans).toJson().toJSONString();
            break;

        case Transaction.GENESIS_ISSUE_ASSET_TRANSACTION:

            jsonString = ((GenesisIssueAssetTransaction) trans).toJson().toJSONString();

            break;

        case Transaction.GENESIS_ISSUE_PERSON_TRANSACTION:

            jsonString = ((GenesisCertifyPersonRecord) trans).toJson().toJSONString();

            break;
         default:
             jsonString = (trans).toJson().toJSONString();
        
        }
        if (jsonString.equals("")) return;
        Library.saveJSONStringToEraFile(parent, jsonString);
    }

    //добавляем в конец стандартные меню копировать, вырезать
    //
    public static void addStandartMenuItems(JPopupMenu menu, JTextField component){
        JMenuItem item;
        item = new JMenuItem(new DefaultEditorKit.CopyAction());
        item.setText(Lang.getInstance().translate("Copy"));
        item.setEnabled(true);
 //       item.setEnabled(component.getSelectionStart() != component
 //               .getSelectionEnd());
        menu.add(item);
        item = new JMenuItem(new DefaultEditorKit.CutAction());
        item.setText(Lang.getInstance().translate("Cut"));
        item.setEnabled(true);
   //     item.setEnabled(component.isEditable()
   //             && component.getSelectionStart() != component
   //             .getSelectionEnd());
        menu.add(item);
        item = new JMenuItem(new DefaultEditorKit.PasteAction());
        item.setText(Lang.getInstance().translate("Paste"));
        item.setEnabled(component.isEditable());
        menu.add(item);

    }
}
