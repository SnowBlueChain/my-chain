package org.erachain.gui.items.templates;

import org.erachain.core.item.ItemCls;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.gui.items.ItemSplitPanel;
import org.erachain.gui.records.VouchRecordDialog;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.URLViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

public class TemplatesFavoriteSplitPanel extends ItemSplitPanel {
    private static final long serialVersionUID = 2717571093561259483L;
    private TemplatesFavoriteSplitPanel th;

    public TemplatesFavoriteSplitPanel() {
        super(new FavoriteTemplatesTableModel(), "TemplatesFavoriteSplitPanel");
        this.setName(Lang.getInstance().translate("Favorite Templates"));
        th = this;

        JMenuItem vouch_menu = new JMenuItem(Lang.getInstance().translate("Vouch"));
        vouch_menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DCSet db = DCSet.getInstance();
                Transaction trans = db.getTransactionFinalMap().get(((TemplateCls) th.itemMenu).getReference());
                new VouchRecordDialog(trans.getBlockHeight(), trans.getSeqNo());

            }
        });
        th.menuTable.add(vouch_menu);

        menuTable.addSeparator();

        JMenuItem setSeeInBlockexplorer = new JMenuItem(Lang.getInstance().translate("Check in Blockexplorer"));

        setSeeInBlockexplorer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    URLViewer.openWebpage(new URL("http://" + Settings.getInstance().getBlockexplorerURL()
                            + ":" + Settings.getInstance().getWebPort() + "/index/blockexplorer.html"
                            + "?template=" + itemMenu.getKey()));
                } catch (MalformedURLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        menuTable.add(setSeeInBlockexplorer);

    }

    // show details
    @Override
    public Component getShow(ItemCls item) {
        return new InfoTemplates((TemplateCls) item);
    }

}
