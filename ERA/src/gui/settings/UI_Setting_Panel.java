package gui.settings;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.base.Charsets;

import lang.Lang;
import lang.LangFile;
import settings.Settings;
import utils.DateTimeFormat;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author Саша
 */
public class UI_Setting_Panel extends javax.swing.JPanel {

    public ButtonGroup group;
    public javax.swing.JComboBox<String> font_Name;
    public javax.swing.JComboBox<String> size_Font;
    public javax.swing.JComboBox<LangFile> jComboBox_Lang;
    public javax.swing.JComboBox<String> jComboBox_Thems;
    public JCheckBox chckbxSoundReceivePayment;
    public JCheckBox chckbxSoundReceiveMessage;
    public JCheckBox chckbxSoundNewTransaction;
    public JCheckBox chckbxSystemLookFeel;
    public JCheckBox chckbxMetallLookFeel;
    public JCheckBox chckbxOtherTemes;
    public JPanel Theme_Select_Panel;
    public JRadioButton other_Themes;
    public JRadioButton system_Theme;
    public JRadioButton metal_Theme;
    // Variables declaration - do not modify
    private javax.swing.JLabel Label_Titlt;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton_Download_Lang;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel_Font;
    private javax.swing.JLabel jLabel_Lang;
    private javax.swing.JLabel jLabel_Thems;
    private javax.swing.JLabel jLabel_sounds;
    /**
     * Creates new form UISetting_Panel
     */
    public UI_Setting_Panel() {
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

        Label_Titlt = new javax.swing.JLabel();
        jLabel_Thems = new javax.swing.JLabel();
        jComboBox_Thems = new javax.swing.JComboBox<String>();
        jLabel_Font = new javax.swing.JLabel();
        // jComboBox_Font_Name = new javax.swing.JComboBox<>();
        // jComboBox_Font_Size = new javax.swing.JComboBox<>();
        jLabel_Lang = new javax.swing.JLabel();
        //  jComboBox_Lang = new javax.swing.JComboBox<LangFile>();
        jButton_Download_Lang = new javax.swing.JButton();
        //   jCheckBoxSend_Asset = new javax.swing.JCheckBox();
        //    jCheckBox_Send_message = new javax.swing.JCheckBox();
        //    javax.swing.JCheckBox jCheckBox3_Other_Trans = new javax.swing.JCheckBox();
        jLabel_sounds = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        Label_Titlt.setText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 0, 0);
        //add(Label_Titlt, gridBagConstraints);


        group = new ButtonGroup();

        other_Themes = new JRadioButton(Lang.getInstance().translate("Other Themes"), false);
        group.add(other_Themes);
        system_Theme = new JRadioButton(Lang.getInstance().translate("System Theme"), true);
        group.add(system_Theme);
        metal_Theme = new JRadioButton(Lang.getInstance().translate("Metal Theme"), true);
        group.add(metal_Theme);
        other_Themes.isSelected();


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(other_Themes, gridBagConstraints);


        if (Settings.getInstance().get_LookAndFell().equals("Other")) other_Themes.setSelected(true);


        other_Themes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // Размер соответствует последнему параметру метода addRadioButton
                jComboBox_Thems.setEnabled(other_Themes.isSelected());
            }
        });


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(system_Theme, gridBagConstraints);
        system_Theme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // Размер соответствует последнему параметру метода addRadioButton
                jComboBox_Thems.setEnabled(!system_Theme.isSelected());
            }
        });

        if (Settings.getInstance().get_LookAndFell().equals("System")) {


            jComboBox_Thems.setEnabled(false);
            system_Theme.setSelected(true);
        }


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(metal_Theme, gridBagConstraints);

        metal_Theme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // Размер соответствует последнему параметру метода addRadioButton
                jComboBox_Thems.setEnabled(!metal_Theme.isSelected());
            }
        });

        if (Settings.getInstance().get_LookAndFell().equals("Metal")) {


            jComboBox_Thems.setEnabled(false);
            metal_Theme.setSelected(true);
        }


        jLabel_Thems.setText(Lang.getInstance().translate("Select Theme") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jLabel_Thems, gridBagConstraints);


        jComboBox_Thems.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[]{"YQ Theme", "Unicode", "Silver", "Plastic", "Nightly", "Golden", "Forest"}));

        jComboBox_Thems.setSelectedItem(Settings.getInstance().get_Theme());


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 9, 11);
        add(jComboBox_Thems, gridBagConstraints);


        jLabel_Font.setText(Lang.getInstance().translate("Font") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jLabel_Font, gridBagConstraints);

        font_Name = new javax.swing.JComboBox<String>();


        font_Name.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[]{"Arial", "Courier", "Tahoma", "Times New Roman"}));
        font_Name.setSelectedItem(Settings.getInstance().get_Font_Name());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(font_Name, gridBagConstraints);

        size_Font = new javax.swing.JComboBox<String>();
        size_Font.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[]{"11", "12", "14", "16", "18", "20", "24"}));
        size_Font.setSelectedItem(Settings.getInstance().get_Font());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 8, 10);
        add(size_Font, gridBagConstraints);

        jLabel_Lang.setText(Lang.getInstance().translate("Interface language") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jLabel_Lang, gridBagConstraints);


        jComboBox_Lang = new JComboBox<LangFile>();

        for (LangFile langFile : Lang.getInstance().getLangListAvailable()) {
            jComboBox_Lang.addItem(langFile);

            if (langFile.getFileName().equals(Settings.getInstance().getLangFileName())) {
                jComboBox_Lang.setSelectedItem(langFile);
            }
        }


        //       jComboBox_Lang.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(jComboBox_Lang, gridBagConstraints);

        jButton_Download_Lang.setText(Lang.getInstance().translate("Download"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 21, 0, 12);
        add(jButton_Download_Lang, gridBagConstraints);


        jButton_Download_Lang.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton_Download_Lang.setText("...");
                jButton_Download_Lang.repaint(0);

                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final JPopupMenu menu = new JPopupMenu();

                            String stringFromInternet = "";
                            try {
                                String url = Lang.translationsUrl + "available.json";

                                URL u = new URL(url);

                                InputStream in = u.openStream();
                                stringFromInternet = IOUtils.toString(in, Charsets.UTF_8);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            JSONObject inernetLangsJSON = (JSONObject) JSONValue.parse(stringFromInternet);


                            for (Object internetKey : inernetLangsJSON.keySet()) {

                                JSONObject internetValue = (JSONObject) inernetLangsJSON.get(internetKey);

                                String itemText = null;
                                final String langFileName = (String) internetValue.get("_file_");

                                //long time_of_translation = ((Long) internetValue.get("_timestamp_of_translation_")).longValue();
                                long time_of_translation = Long.parseLong(((Object) internetValue.get("_timestamp_of_translation_")).toString());

                                try {
                                    //LOGGER.error("try lang file: " + langFileName);
                                    JSONObject oldLangFile = Lang.openLangFile(langFileName);

                                    if (oldLangFile == null) {
                                        itemText = (String) internetValue.get("download lang_name translation");

                                    } else if (time_of_translation >
                                            //(Long) oldLangFile.get("_timestamp_of_translation_")
                                            Long.parseLong(((Object) oldLangFile.get("_timestamp_of_translation_")).toString())
                                            ) {
                                        itemText = ((String) internetValue.get("download update of lang_name translation from %date%")).replace("%date%", DateTimeFormat.timestamptoString(time_of_translation, "yyyy-MM-dd", ""));
                                    }
                                } catch (Exception e2) {
                                    itemText = (String) internetValue.get("download lang_name translation");
                                }

                                if (itemText != null) {

                                    JMenuItem item = new JMenuItem();
                                    item.setText("[" + (String) internetKey + "] " + itemText);

                                    item.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                String url = Lang.translationsUrl + langFileName;

                                                FileUtils.copyURLToFile(new URL(url), new File(Settings.getInstance().getLangDir(), langFileName));

                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }

                                            jComboBox_Lang.removeAllItems();

                                            for (LangFile langFile : Lang.getInstance().getLangListAvailable()) {
                                                jComboBox_Lang.addItem(langFile);

                                                if (langFile.getFileName().equals(langFileName)) {
                                                    jComboBox_Lang.setSelectedItem(langFile);
                                                }
                                            }
                                        }
                                    });

                                    menu.add(item);
                                }

                            }
                            if (menu.getComponentCount() == 0) {
                                JMenuItem item = new JMenuItem();
                                item.setText(Lang.getInstance().translate("No new translations"));
                                item.setEnabled(false);
                                menu.add(item);
                            }

                            menu.show(jComboBox_Lang, 0, jComboBox_Lang.getHeight());
                        } finally {
                            jButton_Download_Lang.setText(Lang.getInstance().translate("Download"));
                        }
                    }
                });
            }
        });


        chckbxSoundReceivePayment = new JCheckBox(Lang.getInstance().translate("Receive payment"));
        chckbxSoundReceivePayment.setHorizontalAlignment(SwingConstants.LEFT);
        chckbxSoundReceivePayment.setSelected(Settings.getInstance().isSoundReceivePaymentEnabled());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(chckbxSoundReceivePayment, gridBagConstraints);


        chckbxSoundReceiveMessage = new JCheckBox(Lang.getInstance().translate("Receive message"));
        chckbxSoundReceiveMessage.setHorizontalAlignment(SwingConstants.LEFT);
        chckbxSoundReceiveMessage.setSelected(Settings.getInstance().isSoundReceiveMessageEnabled());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(chckbxSoundReceiveMessage, gridBagConstraints);

        chckbxSoundNewTransaction = new JCheckBox(Lang.getInstance().translate("Other transactions"));
        chckbxSoundNewTransaction.setHorizontalAlignment(SwingConstants.LEFT);
        chckbxSoundNewTransaction.setSelected(Settings.getInstance().isSoundNewTransactionEnabled());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 10);
        add(chckbxSoundNewTransaction, gridBagConstraints);

        jLabel_sounds.setText(Lang.getInstance().translate("Sounds") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        add(jLabel_sounds, gridBagConstraints);


        JLabel jLabel_UI = new JLabel(Lang.getInstance().translate("UI") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        //     add(jLabel_UI, gridBagConstraints);


        jLabel1.setText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel1, gridBagConstraints);


    }// </editor-fold>
    // End of variables declaration                   
}
