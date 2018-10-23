package org.erachain.gui.items.accounts;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.crypto.AEScrypto;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.transaction.R_Send;
import org.erachain.core.transaction.Transaction;
import org.erachain.gui.AccountRenderer;
import org.erachain.gui.MainFrame;
import org.erachain.gui.PasswordPane;
import org.erachain.gui.items.assets.AssetsComboBoxModel;
import org.erachain.gui.library.Issue_Confirm_Dialog;
import org.erachain.gui.models.AccountsComboBoxModel;
import org.erachain.gui.transaction.OnDealClick;
import org.erachain.gui.transaction.Send_RecordDetailsFrame;
import org.erachain.lang.Lang;
import org.erachain.utils.Converter;
import org.erachain.utils.NameUtils;
import org.erachain.utils.NameUtils.NameResult;
import org.erachain.utils.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

//import org.erachain.settings.Settings;

@SuppressWarnings("serial")

public class Account_Lend_Panel extends AssetSendPanel {

    public Account_Lend_Panel(AssetCls asset, Account account, Account account_To, PersonCls person) {
        super(asset, account, account_To, person);

        this.jLabel_Title.setText(Lang.getInstance().translate("If You want to give a loan asset %asset%, fill in this form")
                .replace("%asset%", asset.viewName()));

        //	icon.setIcon(null);
        this.jButton_ok.setText(Lang.getInstance().translate("Lend"));
        this.jLabel_To.setText(Lang.getInstance().translate("Debtor account") + ":");
        this.jLabel_Recive_Detail.setText(Lang.getInstance().translate("Debtor details") + ":");

    }

    @Override
    public void onSendClick() {
        // confirm params
        if (!cheskError()) return;

        //CREATE TX MESSAGE
        Transaction transaction = Controller.getInstance()
                .r_Send(Controller.getInstance().getPrivateKeyAccountByAddress(sender.getAddress()), feePow, recipient,
                        -key, amount,
                        head, messageBytes, isTextByte, encrypted);

        String Status_text = "";
        Issue_Confirm_Dialog dd = new Issue_Confirm_Dialog(null, true, transaction,
                Lang.getInstance().translate("Lend"), (int) (this.getWidth() / 1.2), (int) (this.getHeight() / 1.2),
                Status_text, Lang.getInstance().translate("Confirmation Transaction"));
        Send_RecordDetailsFrame ww = new Send_RecordDetailsFrame((R_Send) transaction);

        dd.jScrollPane1.setViewportView(ww);
        dd.pack();
        dd.setLocationRelativeTo(this);
        dd.setVisible(true);

        //	JOptionPane.OK_OPTION
        if (dd.isConfirm) {


            result = Controller.getInstance().getTransactionCreator().afterCreate(transaction, Transaction.FOR_NETWORK);

            confirmaftecreatetransaction();
        }

        //ENABLE
        this.jButton_ok.setEnabled(true);
    }

}


