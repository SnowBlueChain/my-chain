package org.erachain.gui.items.assets;

import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.items.Search_Item_SplitPanel;
import org.erachain.gui.records.VouchRecordDialog;
import org.erachain.lang.Lang;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Search_Assets_SplitPanel extends Search_Item_SplitPanel {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    //private static ItemAssetsTableModel tableModelItemAssets = ;
    private Search_Assets_SplitPanel th;


    public Search_Assets_SplitPanel(boolean search_and_exchange) {
        super(new ItemAssetsTableModel(), "Search_Assets_SplitPanel", "Search_Assets_SplitPanel");
        th = this;
        setName(Lang.getInstance().translate("Search Assets"));


        // MENU


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
                Transaction trans = db.getTransactionFinalMap().get(th.item_Menu.getReference());

                new VouchRecordDialog(trans.getBlockHeight(), trans.getSeqNo());

            }
        });


//	nameSalesMenu.add(favorite);
        if (search_and_exchange) {
            this.menu_Table.add(excahge);
            this.menu_Table.addSeparator();
            this.menu_Table.add(buy);

            this.menu_Table.add(sell);
            this.menu_Table.addSeparator();

            this.menu_Table.addSeparator();

            this.menu_Table.add(vouch_menu);
        } else {
            this.menu_Table.remove(this.favorite_menu_items);
        }


    }


    //show details
    @Override
    protected Component get_show(ItemCls item) {

        return new Asset_Info((AssetCls) item);

    }

    // mouse 2 click
    @Override
    protected void table_mouse_2_Click(ItemCls item) {

        new ExchangeFrame((AssetCls) item, null, "", "");
    }

}