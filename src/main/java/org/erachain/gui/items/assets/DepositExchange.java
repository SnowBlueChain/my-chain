package org.erachain.gui.items.assets;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.gui.library.MButton;
import org.erachain.gui.models.AccountsComboBoxModel;
import org.erachain.gui.models.FundTokensComboBoxModel;
import org.erachain.lang.Lang;
import org.erachain.utils.StrJSonFine;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//public class PersonConfirm extends JDialog { // InternalFrame  {
public class DepositExchange extends JPanel {

    // private JComboBox<Account> accountLBox;

    private static final Logger LOGGER = LoggerFactory.getLogger(DepositExchange.class);

    private static final long serialVersionUID = 2717571093561259483L;
    private MButton jButtonHistory;
    private MButton jButton_Confirm;
    private JComboBox<Account> jComboBox_YourAddress;
    public JComboBox<AssetCls> cbxAssets;
    private JLabel jLabel_Address;
    private JLabel jLabel_Adress_Check;
    private JLabel jLabel_Asset;
    private JLabel jLabel_Details;
    private JTextField jTextField_Details;
    private JLabel jTextField_Details_Check;
    private JLabel jLabel_YourAddress;
    private JTextField jTextField_Address = new JTextField();

    public DepositExchange(AssetCls asset, Account account) {

        initComponents(asset, account);
        this.setVisible(true);
    }

    private void refreshReceiverDetails(String text, JLabel details) {
        // CHECK IF RECIPIENT IS VALID ADDRESS
        details.setText("<html>" + text + "</html>");

    }

    public void onGoClick() {

        jButton_Confirm.setEnabled(false);
        jTextField_Details.setText("");
        jTextField_Details_Check.setText(Lang.getInstance().translate("wait"));

        // http://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
        //String url_string = "https://api.face2face.cash/apipay/index.json";
        String urlGetRate = "https://api.face2face.cash/apipay/get_rate.json/10/9/1";
        String urlGetHistory = "https://api.face2face.cash/apipay/history.json/ERA/78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5";
        String urlGetDetailsTest = "https://api.face2face.cash/apipay/get_uri_in.json/2/3/12/78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5/1000";

        JSONObject jsonObject;

        String urlGetDetails = "https://api.face2face.cash/apipay/get_uri_in.json/2/";

        AssetCls asset = (AssetCls) cbxAssets.getSelectedItem();
        switch ((int)asset.getKey()) {
            case 12:
                urlGetDetails += "3/12/" + jTextField_Address.getText() + "/0.1"; // BTC -> eBTC
                break;
            case 2:
                urlGetDetails += "3/10/" + jTextField_Address.getText() + "/0.1"; // BTC -> COMPU
                break;
            case 95:
                urlGetDetails += "3/13/" + jTextField_Address.getText() + "/0.1"; // BTC -> eUSD
                break;
            default:
                urlGetDetails += "3/10/" + jTextField_Address.getText() + "/0.1"; // BTC -> COMPU
        }

        String inputText = "";
        try {

            // CREATE CONNECTION
            URL url = new URL(urlGetDetails);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // EXECUTE
            int resCode = connection.getResponseCode();

            //READ RESULT
            InputStream stream;
            if (resCode == 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
            }

            InputStreamReader isReader = new InputStreamReader(stream, "UTF-8");
            //String result = new BufferedReader(isReader).readLine();

            BufferedReader bufferedReader = new BufferedReader(isReader);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null)
                inputText += inputLine;
            bufferedReader.close();

            jsonObject = (JSONObject) JSONValue.parse(inputText);

        } catch (Exception e) {
            jsonObject = null;
            inputText = "";
        }

        if (jsonObject != null && jsonObject.containsKey("addr_in")) {
            if (BlockChain.DEVELOP_USE) {
                jLabel_Adress_Check.setText("<html>" + StrJSonFine.convert(jsonObject) + "</html>");
            }

            String rate = jsonObject.get("rate").toString();
            String bal = jsonObject.get("bal").toString();

            LOGGER.debug(StrJSonFine.convert(jsonObject));
            String help;

            String incomeAssetName = "bitcoins";
            asset = (AssetCls) cbxAssets.getSelectedItem();
            switch ((int)asset.getKey()) {
                case 2:
                    help = Lang.getInstance().translate("Transfer <b>%1</b> to this address for buy")
                            .replace("%1", incomeAssetName) + " <b>COMPU</B>"
                        + " " + Lang.getInstance().translate("by rate") + ": <b>" + rate + "</b>"
                        + ", " + Lang.getInstance().translate("max buy amount") + ": <b>" + bal + "</b> COMPU";
                    break;
                default:
                    help = Lang.getInstance().translate("Transfer <b>%1</B> to this address for deposit your account on Exchange")
                            .replace("%1", incomeAssetName);
            }

            jTextField_Details.setText(jsonObject.get("addr_in").toString());
            jTextField_Details_Check.setText("<html>" + help + "</html>");

        } else {
            jLabel_Adress_Check.setText("<html>" + inputText + "</html>");
            jTextField_Details.setText("");
            jTextField_Details_Check.setText(Lang.getInstance().translate("error"));
        }

        jButton_Confirm.setEnabled(true);

    }

    private void initComponents(AssetCls asset_in, Account account) {

        AssetCls asset;
        if (asset_in == null) {
            asset = Controller.getInstance().getAsset(2l);
        } else {
            asset = asset_in;
        }

        GridBagConstraints gridBagConstraints;

        //paneAssetInfo = new JScrollPane();
        jLabel_YourAddress = new JLabel();
        jComboBox_YourAddress = new JComboBox<>();
        jLabel_Address = new JLabel();
        jLabel_Asset = new JLabel();

        jLabel_Adress_Check = new JLabel();
        jLabel_Details = new JLabel();
        jTextField_Details = new JTextField();
        jTextField_Details_Check = new JLabel();

        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{0, 9, 0, 9, 0, 9, 0};
        layout.rowHeights = new int[]{0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0};
        //getContentPane().setLayout(layout);
        this.setLayout(layout);

        int gridy = 0;
        jLabel_YourAddress.setText(Lang.getInstance().translate("Your account") + ":");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(21, 27, 0, 0);
        add(jLabel_YourAddress, gridBagConstraints);

        jComboBox_YourAddress = new JComboBox<Account>(new AccountsComboBoxModel());
        jTextField_Address.setText(((Account) jComboBox_YourAddress.getSelectedItem()).getAddress());

        jComboBox_YourAddress.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                jTextField_Address.setText(((Account) jComboBox_YourAddress.getSelectedItem()).getAddress());

            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(21, 0, 0, 13);
        add(jComboBox_YourAddress, gridBagConstraints);

        ////////////////
        jLabel_Address.setText(Lang.getInstance().translate("Account to Deposit") + ":");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(0, 27, 0, 0);
        add(jLabel_Address, gridBagConstraints);

        if (account == null) {
            jLabel_Adress_Check.setText(Lang.getInstance().translate("Insert Deposit Account"));
        } else {
            jTextField_Address.setText(account.getAddress());
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        add(jTextField_Address, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        add(jLabel_Adress_Check, gridBagConstraints);

        gridy++;
        /////////////// ASSET
        jLabel_Asset.setText(Lang.getInstance().translate("Asset") + ":");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        // gridBagConstraints.insets = new java.awt.Insets(0, 27, 0, 0);
        gridBagConstraints.insets = new Insets(21, 27, 0, 0);
        add(jLabel_Asset, gridBagConstraints);

        GridBagConstraints favoritesGBC = new GridBagConstraints();
        favoritesGBC.insets = new Insets(21, 0, 0, 13);
        favoritesGBC.fill = GridBagConstraints.HORIZONTAL;
        favoritesGBC.anchor = GridBagConstraints.LINE_END;
        favoritesGBC.gridwidth = 3;
        favoritesGBC.gridx = 2;
        favoritesGBC.gridy = gridy;

        cbxAssets = new JComboBox<AssetCls>(new FundTokensComboBoxModel());
        this.add(cbxAssets, favoritesGBC);

        JLabel detailsHead = new JLabel();

        cbxAssets.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    AssetCls asset = (AssetCls) cbxAssets.getSelectedItem();
                    //paneAssetInfo.setViewportView(new AssetInfo(asset, false));

                    jTextField_Details.setText("");
                    jTextField_Details_Check.setText("");

                    switch ((int)asset.getKey()) {
                        case 2:
                            jLabel_Details.setText(Lang.getInstance().translate("Bitcoin Address for buy") + ":");
                            refreshReceiverDetails(Lang.getInstance().translate("Payment Details") +
                                            " " + Lang.getInstance().translate("for buy") + " COMPU",
                                    detailsHead);
                            break;
                        default:
                            jLabel_Details.setText(Lang.getInstance().translate("Bitcoin Address for deposit") + ":");
                            refreshReceiverDetails(Lang.getInstance().translate("Payment Details") +
                                            " " + Lang.getInstance().translate("for deposit") + " bitcoins",
                                    detailsHead);
                    }
                }
            }
        });

        //////////////// BUTTONS

        gridy += 3;

        jButton_Confirm = new MButton(Lang.getInstance().translate("Get Payment Details"), 2);
        jButton_Confirm.setToolTipText("");
        jButton_Confirm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onGoClick();
            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        //gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new Insets(1, 0, 29, 0);
        add(jButton_Confirm, gridBagConstraints);

        ////////////// DETAILS
        gridy += 3;


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(0, 27, 0, 0);
        add(detailsHead, gridBagConstraints);
        detailsHead.setHorizontalAlignment(JTextField.LEFT);
        detailsHead.setText(Lang.getInstance().translate("Payment Details"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(0, 27, 0, 0);
        add(jLabel_Details, gridBagConstraints);
        jLabel_Details.setHorizontalAlignment(JTextField.LEFT);
        jLabel_Details.setText(Lang.getInstance().translate("Bitcoin Address for deposit") + ":");

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        add(jTextField_Details, gridBagConstraints);
        jTextField_Details.setEditable(false);
        jTextField_Details.setToolTipText("");
        jTextField_Details.setText(""); // NOI18N

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        add(jTextField_Details_Check, gridBagConstraints);

        //////////////////////////
        gridy += 3;

        jButtonHistory = new MButton(Lang.getInstance().translate("See Deposit History"), 2);
        jButtonHistory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                JSONObject jsonObject;

                // [TOKEN]/[ADDRESS]
                String urlGetDetails = "https://api.face2face.cash/apipay/history.json/";

                AssetCls asset = (AssetCls) cbxAssets.getSelectedItem();
                switch ((int)asset.getKey()) {
                    case 12:
                        urlGetDetails += "12/" + jTextField_Address.getText(); // BTC -> eBTC
                        break;
                    case 2:
                        urlGetDetails += "10/" + jTextField_Address.getText(); // BTC -> COMPU
                        break;
                    case 95:
                        urlGetDetails += "13/" + jTextField_Address.getText(); // BTC -> eUSD
                        break;
                    default:
                        urlGetDetails += "10/" + jTextField_Address.getText(); // BTC -> COMPU
                }


                String inputText = "";
                try {

                    // CREATE CONNECTION
                    URL url = new URL(urlGetDetails);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // EXECUTE
                    int resCode = connection.getResponseCode();

                    //READ RESULT
                    InputStream stream;
                    if (resCode == 400) {
                        stream = connection.getErrorStream();
                    } else {
                        stream = connection.getInputStream();
                    }

                    InputStreamReader isReader = new InputStreamReader(stream, "UTF-8");
                    //String result = new BufferedReader(isReader).readLine();

                    BufferedReader bufferedReader = new BufferedReader(isReader);
                    String inputLine;
                    while ((inputLine = bufferedReader.readLine()) != null)
                        inputText += inputLine;
                    bufferedReader.close();

                    jsonObject = (JSONObject) JSONValue.parse(inputText);

                } catch (Exception e) {
                    jsonObject = null;
                    inputText = "";
                }

                if (jsonObject != null) {
                    if (jsonObject.containsKey("deal")) {
                        if (BlockChain.DEVELOP_USE) {
                            jLabel_Adress_Check.setText("<html>" + StrJSonFine.convert(jsonObject) + "</html>");
                        }

                        String rate = jsonObject.get("rate").toString();
                        String bal = jsonObject.get("bal").toString();

                        LOGGER.debug(StrJSonFine.convert(jsonObject));
                        String help;
                    } else {
                        LOGGER.debug(StrJSonFine.convert(jsonObject));
                    }
                } else {
                    jLabel_Adress_Check.setText(inputText);
                }
            }
        });


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        //gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new Insets(1, 0, 29, 0);

        if (BlockChain.DEVELOP_USE)
            add(jButtonHistory, gridBagConstraints);

    }

}
