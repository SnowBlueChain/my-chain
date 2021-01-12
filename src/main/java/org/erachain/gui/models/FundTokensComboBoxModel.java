package org.erachain.gui.models;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.item.assets.AssetCls;

import javax.swing.*;

@SuppressWarnings("serial")
public class FundTokensComboBoxModel extends DefaultComboBoxModel<AssetCls> {

    public FundTokensComboBoxModel(boolean deposit) {

        AssetCls asset;
        if (BlockChain.TEST_MODE) {
            //this.addElement(Controller.getInstance().getAsset(1031));

            if (deposit) {
                this.addElement(Controller.getInstance().getAsset(AssetCls.ERA_KEY));
            } else {
            }

            for (Long key : new Long[]{AssetCls.FEE_KEY, 1048583L, 1078L, 1079L}) {
                asset = Controller.getInstance().getAsset(key);
                if (asset == null)
                    continue;
                this.addElement(asset);
            }

        } else {

            if (deposit) {
                this.addElement(Controller.getInstance().getAsset(AssetCls.ERA_KEY));
                this.addElement(Controller.getInstance().getAsset(AssetCls.FEE_KEY));
            } else {
            }

            for (Long key : new Long[]{
                    //AssetCls.FEE_KEY,
                    AssetCls.BTC_KEY // BTC
                    //1114L
            }) {
                asset = Controller.getInstance().getAsset(key);
                if (asset == null)
                    continue;

                this.addElement(asset);
            }

            //this.addElement(Controller.getInstance().getAsset(14)); // ETH
            //this.addElement(Controller.getInstance().getAsset(92)); // RUB
            //this.addElement(Controller.getInstance().getAsset(95)); // USD
        }

    }

    public FundTokensComboBoxModel(long[] assetKets) {

        AssetCls asset;
        for (Long key : assetKets) {
            asset = Controller.getInstance().getAsset(key);
            if (asset == null)
                continue;
            this.addElement(asset);
        }
    }
}
