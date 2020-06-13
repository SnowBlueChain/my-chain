package org.erachain.gui.transaction;

import org.erachain.core.transaction.CreateOrderTransaction;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.erachain.utils.MenuPopupUtil;

import javax.swing.*;

@SuppressWarnings("serial")
public class CreateOrderDetailsFrame extends RecDetailsFrame {
    public CreateOrderDetailsFrame(CreateOrderTransaction orderCreation) {
        super(orderCreation);

        orderCreation.setDC(DCSet.getInstance(), true);

        //LABEL HAVE
        ++labelGBC.gridy;
        JLabel haveLabel = new JLabel(Lang.getInstance().translate("Have") + ":");
        this.add(haveLabel, labelGBC);

        //HAVE
        ++detailGBC.gridy;
        JTextField have = new JTextField(
                orderCreation.getAmountHave().toPlainString()
                        + " x "
                        + String.valueOf(orderCreation.getHaveAsset().toString()));
        have.setEditable(false);
        MenuPopupUtil.installContextMenu(have);
        this.add(have, detailGBC);

        //LABEL WANT
        ++labelGBC.gridy;
        JLabel wantLabel = new JLabel(Lang.getInstance().translate("Want") + ":");
        this.add(wantLabel, labelGBC);

        //HAVE
        ++detailGBC.gridy;
        JTextField want = new JTextField(
                orderCreation.getAmountWant().toPlainString() + " x "
                        + String.valueOf(orderCreation.getWantAsset().toString()));
        want.setEditable(false);
        MenuPopupUtil.installContextMenu(want);
        this.add(want, detailGBC);

        //LABEL PRICE
        ++labelGBC.gridy;
        JLabel priceLabel = new JLabel(Lang.getInstance().translate("Price") + ":");
        this.add(priceLabel, labelGBC);

        //PRICE
        ++detailGBC.gridy;
        JTextField price = new JTextField(orderCreation.getPriceCalc().toPlainString()
                + " / " + orderCreation.getPriceCalcReverse().toPlainString());
        price.setEditable(false);
        MenuPopupUtil.installContextMenu(price);
        this.add(price, detailGBC);

        //PACK
        //		this.pack();
        //        this.setResizable(false);
        //       this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
