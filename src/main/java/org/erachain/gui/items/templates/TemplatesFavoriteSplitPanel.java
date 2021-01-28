package org.erachain.gui.items.templates;

import org.erachain.core.item.ItemCls;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.items.ItemSplitPanel;
import org.erachain.gui.records.toSignRecordDialog;
import org.erachain.lang.Lang;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TemplatesFavoriteSplitPanel extends ItemSplitPanel {

    public static String NAME = "TemplatesFavoriteSplitPanel";
    public static String TITLE = "Favorite Templates";

    private static final long serialVersionUID = 2717571093561259483L;

    public TemplatesFavoriteSplitPanel() {
        super(new FavoriteTemplatesTableModel(), NAME, TITLE);
        iconName = "favorite.png";

        JMenuItem vouch_menu = new JMenuItem(Lang.T("Sign / Vouch"));
        vouch_menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DCSet db = DCSet.getInstance();
                Transaction trans = db.getTransactionFinalMap().get(itemTableSelected.getReference());
                new toSignRecordDialog(trans.getBlockHeight(), trans.getSeqNo());

            }
        });
        menuTable.add(vouch_menu);

    }

    // show details
    @Override
    public Component getShow(ItemCls item) {
        return new InfoTemplates((TemplateCls) item);
    }


}
