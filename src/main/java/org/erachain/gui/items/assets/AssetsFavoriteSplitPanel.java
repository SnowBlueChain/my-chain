package org.erachain.gui.items.assets;

import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.items.ItemSplitPanel;
import org.erachain.gui.records.VouchRecordDialog;
import org.erachain.lang.Lang;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AssetsFavoriteSplitPanel extends ItemSplitPanel {
    private static final long serialVersionUID = 2717571093561259483L;
    //private static FavoriteAssetsTableModel table_Model = ;
    private AssetsFavoriteSplitPanel th;

    public AssetsFavoriteSplitPanel() {
        super(new FavoriteAssetsTableModel(), "AssetsFavoriteSplitPanel");
        this.setName(Lang.getInstance().translate("Favorite Assets"));

        th = this;
        JMenuItem sell = new JMenuItem(Lang.getInstance().translate("To sell"));

        sell.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ExchangeFrame((AssetCls) th.item_Menu, null, "To sell", "");
            }
        });

        JMenuItem excahge = new JMenuItem(Lang.getInstance().translate("Exchange"));
        excahge.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ExchangeFrame((AssetCls) th.item_Menu, null, "", "");
            }
        });

        JMenuItem buy = new JMenuItem(Lang.getInstance().translate("Buy"));
        buy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ExchangeFrame((AssetCls) th.item_Menu, null, "Buy", "");
            }
        });

        JMenuItem vouch_menu = new JMenuItem(Lang.getInstance().translate("Vouch"));
        vouch_menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DCSet db = DCSet.getInstance();
                Transaction trans = db.getTransactionFinalMap()
                        .get(((AssetCls) th.item_Menu).getReference());
                new VouchRecordDialog(trans.getBlockHeight(), trans.getSeqNo());

            }
        });
        th.menu_Table.addSeparator();
        th.menu_Table.add(excahge);
        th.menu_Table.addSeparator();
        th.menu_Table.add(buy);
        th.menu_Table.add(sell);
        th.menu_Table.addSeparator();
        th.menu_Table.add(vouch_menu);
    }

    // show details
    @Override
    public Component get_show(ItemCls item) {
        return new AssetInfo((AssetCls) item);
    }

}