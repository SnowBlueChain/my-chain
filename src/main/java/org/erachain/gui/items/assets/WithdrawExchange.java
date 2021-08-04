package org.erachain.gui.items.assets;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.gui.IconPanel;
import org.erachain.gui.items.accounts.AccountAssetSendPanel;
import org.erachain.gui.library.MButton;
import org.erachain.gui.models.FundTokensComboBoxModel;
import org.erachain.gui2.MainPanel;
import org.erachain.lang.Lang;
import org.erachain.utils.StrJSonFine;
import org.erachain.utils.URLViewer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WithdrawExchange extends IconPanel {

    public static String NAME = "WithdrawExchange";
    public static String TITLE = "Withdraw Exchange";

    private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawExchange.class);

    private static final long serialVersionUID = 2717571093561259483L;
    private MButton buttonShowForm;
    private MButton jButton_Confirm;
    public JComboBox<AssetCls> cbxInAssets;
    public JComboBox<AssetCls> cbxOutAssets;
    private JLabel labelOutAddress;
    private JLabel labelAddressCheck;
    private JLabel labelInAsset;
    private JLabel labelOutAsset;
    private JLabel labelDetails;
    private JTextField textFieldAddress = new JTextField();
    JLabel jText_Help = new JLabel();

    private AssetCls assetIn;
    private AssetCls assetOut;

    public WithdrawExchange(AssetCls assetIn, Account account) {
        super(NAME, TITLE);
        initComponents(assetIn, account);
        this.setVisible(true);
    }

    public void onGoClick() {


        // http://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
        //String url_string = "https://api.face2face.cash/apipay/index.json";
        //String urlGetRate = "https://api.face2face.cash/apipay/get_rate.json/10/9/1";
        //String urlGetHistory = "https://api.face2face.cash/apipay/history.json/ERA/78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5";
        //String urlGetDetails2 = "https://api.face2face.cash/apipay/get_uri_in.json/2/10/9/78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5/1000";

        JSONObject jsonObject = null;
        String inputText = "";
        String accountTo;
        String message = "";
        String rate = null;
        String address = textFieldAddress.getText().trim();
        try {

            // GET RATE
            String urlGetDetails = "https://api.face2face.cash/apipay/get_uri_in.json/2/";
            String abbrevIN = assetIn.getName();

            String abbrevOut = assetOut.getName();
            String outURL = "/" + abbrevOut + "/" + address + "/";

            switch ((int) assetIn.getKey()) {
                case (int) AssetCls.ERA_KEY:
                    urlGetDetails += abbrevIN + outURL + "1000";
                    message += abbrevOut;
                    break;
                case (int) AssetCls.FEE_KEY:
                    urlGetDetails += abbrevIN + outURL + "1";
                    message += abbrevOut;
                    break;
                case (int) AssetCls.USD_KEY:
                    urlGetDetails += "@" + abbrevIN + outURL + "1000";
                    message += abbrevIN;
                    break;
                case (int) AssetCls.BTC_KEY:
                    urlGetDetails += "@" + abbrevIN + outURL + "0.1";
                    message += abbrevIN;
                    break;
                case (int) DepositExchange.TEST_ASSET:
                    urlGetDetails = "http://185.195.26.197/7pay_in/apipay/get_uri_in.json/2/";
                    urlGetDetails += "fZEN/ZEN/" + address + "/30"; // eZEN -> ZEN
                    message += "ZEN";
                    break;
                default:
                    urlGetDetails += "@" + abbrevIN + outURL + "100";
                    message += abbrevIN;
            }

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

            if (BlockChain.TEST_MODE) {
                labelAddressCheck.setText("<html>" + StrJSonFine.convert(jsonObject) + "</html>");
            }

            LOGGER.debug(StrJSonFine.convert(jsonObject));

            accountTo = jsonObject.get("addr_in").toString();
            rate = jsonObject.get("rate").toString();

        } catch (Exception e) {
            if (jsonObject != null && jsonObject.containsKey("wrong")) {
                labelAddressCheck.setText(jsonObject.get("wrong").toString());
            } else {
                labelAddressCheck.setText(inputText + " " + e.getMessage());
            }
            jsonObject = null;
            accountTo = null;
        }

        if (assetIn != null && accountTo != null && rate != null) {

            rate = jsonObject.get("rate").toString();
            String bal = jsonObject.get("bal").toString();

            String formTitle;
            String incomeAssetName = assetIn.getName();
            switch ((int) assetIn.getKey()) {
                case 1:
                case 2:
                    formTitle = Lang.T("Transfer <b>%1</b> to this address for buy")
                            .replace("%1", incomeAssetName) + " <b>BTC</B>"
                            + " " + Lang.T("by rate") + ": <b>" + rate + "</b>"
                            + ", " + Lang.T("max buy amount") + ": <b>" + bal + "</b> BTC";
                    break;
                case (int) DepositExchange.TEST_ASSET:
                    incomeAssetName = "ZEN";
                    formTitle = Lang.T("Withdraw %1 to").replace("%1", incomeAssetName) + " " + address;
                    break;
                default:
                    formTitle = Lang.T("Withdraw %1 to").replace("%1", incomeAssetName) + " " + address;
            }

            if (jsonObject.containsKey("may_pay")) {
                formTitle += "<br>" + Lang.T("You may pay maximum") + ": " + jsonObject.get("may_pay").toString()
                        + incomeAssetName;
            }

            formTitle = "<html><h2>" + formTitle + "</h2></html>";

            message += ":" + textFieldAddress.getText();
            AccountAssetSendPanel panel = new AccountAssetSendPanel(formTitle, assetIn,
                    null, new Account(accountTo), null, message, false);

            panel.jTextFieldTXTitle.setEnabled(false);
            panel.jComboBox_Asset.setEnabled(false);
            panel.recipientAddress.setEnabled(false);
            panel.jTextArea_Message.setEnabled(false);
            labelAddressCheck.setText("");

            panel.setName(Lang.T("Withdraw"));

            MainPanel.getInstance().removeTab(panel.getName());
            MainPanel.getInstance().insertNewTab(Lang.T("Send asset"), panel);

        }

        jButton_Confirm.setEnabled(true);

    }

    private void initComponents(AssetCls assetIn, Account account) {

        if (assetIn == null) {
            this.assetIn = Controller.getInstance().getAsset(2L);
        } else {
            this.assetIn = assetIn;
        }

        GridBagConstraints gridBagConstraints;

        labelOutAddress = new JLabel();
        labelInAsset = new JLabel();
        labelOutAsset = new JLabel();

        labelAddressCheck = new JLabel();
        labelDetails = new JLabel();

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        GridBagConstraints labelGBC = new GridBagConstraints();
        labelGBC.gridx = 0;
        labelGBC.anchor = GridBagConstraints.LINE_END;
        labelGBC.insets = new Insets(10, 15, 0, 0);

        GridBagConstraints textGBC = new GridBagConstraints();
        textGBC.gridx = 1;
        textGBC.gridwidth = 4;
        textGBC.fill = GridBagConstraints.HORIZONTAL;
        textGBC.insets = new Insets(10, 5, 0, 15);

        GridBagConstraints fieldGBC = new GridBagConstraints();
        fieldGBC.gridx = 1;
        fieldGBC.gridwidth = 3;
        fieldGBC.weightx = 0.1;
        fieldGBC.fill = GridBagConstraints.HORIZONTAL;
        fieldGBC.insets = new Insets(10, 5, 0, 15);

        JLabel jText_Title = new JLabel("<html><h2>" + Lang.T("Withdraw from the Exchange") + "</h2></html>");
        textGBC.gridy = 0;
        add(jText_Title, textGBC);

        ++textGBC.gridy;
        add(jText_Help, textGBC);

        /////////////// ASSET IN
        labelGBC.gridy = ++textGBC.gridy;
        add(labelInAsset, labelGBC);

        fieldGBC.gridy = labelGBC.gridy;
        cbxInAssets = new JComboBox<>(new FundTokensComboBoxModel(false));
        this.add(cbxInAssets, fieldGBC);

        /////////////// ASSET OUT
        ++labelGBC.gridy;
        add(labelOutAsset, labelGBC);

        cbxOutAssets = new JComboBox<>(new FundTokensComboBoxModel(new long[]{AssetCls.BTC_KEY}));
        fieldGBC.gridy = labelGBC.gridy;
        this.add(cbxOutAssets, fieldGBC);

        cbxInAssets.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    reset();
                }
            }
        });
        cbxOutAssets.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    reset();
                }
            }
        });

        ////////////////
        labelOutAddress.setText(Lang.T("Address to Withdraw") + ":");
        ++labelGBC.gridy;
        add(labelOutAddress, labelGBC);

        if (account == null) {
            labelAddressCheck.setText(Lang.T("Insert Withdraw Address"));
        } else {
            textFieldAddress.setText(account.getAddress());
        }

        fieldGBC.gridy = labelGBC.gridy;
        add(textFieldAddress, fieldGBC);

        // GO NEXT
        jButton_Confirm = new MButton(Lang.T("Next"), 1);
        jButton_Confirm.setToolTipText("");
        jButton_Confirm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onGoClick();
            }
        });

        fieldGBC.gridy = ++labelGBC.gridy;
        add(jButton_Confirm, fieldGBC);

        // TIP
        textGBC.gridy = ++labelGBC.gridy;
        labelAddressCheck.setText("");
        add(labelAddressCheck, textGBC);

        //////////////////////////
        JEditorPane jText_History = new JEditorPane();
        jText_History.setContentType("text/html");
        jText_History.setEditable(false);

        jText_History.setBackground(UIManager.getColor("Panel.background"));
        // не пашет - надо внутри ручками в тексте jText_History.setFont(UIManager.getFont("Label.font"));

        buttonShowForm = new MButton(Lang.T("See Withdraw Transactions"), 2);
        buttonShowForm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jText_History.setText(DepositExchange.showHistory(null,
                        textFieldAddress.getText(), labelAddressCheck));
            }
        });

        fieldGBC.gridy = ++labelGBC.gridy;
        add(buttonShowForm, fieldGBC);


        jText_History.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent arg0) {
                // TODO Auto-generated method stub
                HyperlinkEvent.EventType type = arg0.getEventType();
                if (type != HyperlinkEvent.EventType.ACTIVATED)
                    return;

                try {
                    URLViewer.openWebpage(new URL(arg0.getDescription()));
                } catch (MalformedURLException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }

            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = ++labelGBC.gridy;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        add(jText_History, gridBagConstraints);

        reset();
    }

    private void reset() {
        assetIn = (AssetCls) cbxInAssets.getSelectedItem();

        boolean stableCoin = assetIn.getKey() > 10 && assetIn.getKey() < 24;
        cbxOutAssets.setVisible(!stableCoin);
        labelOutAsset.setVisible(!stableCoin);

        String crytoOUT;
        String help = "<html><h3>2. ";
        if (stableCoin) {
            labelInAsset.setText(Lang.T("Withdraw") + ":");
            assetOut = assetIn;
            switch ((int) assetIn.getKey()) {
                case (int) DepositExchange.TEST_ASSET:
                    crytoOUT = "ZEN";
                    break;
                default:
                    crytoOUT = assetOut.getName();
            }
            help += Lang.T("Set the address to which you want to withdraw") + " " + crytoOUT;
        } else {
            labelInAsset.setText(Lang.T("Sell") + ":");
            assetOut = (AssetCls) cbxOutAssets.getSelectedItem();
            crytoOUT = assetOut.getName();
            help += Lang.T("Set the address to which you want to send") + " " + crytoOUT;
        }

        help += ". " + Lang.T("And click button '%1' to open the panel for payment").replace("%1",
                Lang.T("Next"))
                + ". " + Lang.T("Where You need to set only amount of withdraw asset in the panel for payment")
                + ".</h3>"
                + Lang.T("Minimal payment in equivalent")
                + " <b>" + 5 + " USD</b>" + "<br>"
                + Lang.T("Service will have some commission");

        jText_Help.setText(help);

    }
}
