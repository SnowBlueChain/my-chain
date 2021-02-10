package org.erachain.gui.items.polls;

import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.polls.PollCls;
import org.erachain.datachain.DCSet;
import org.erachain.gui.items.SearchItemSplitPanel;
import org.erachain.lang.Lang;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SearchPollsSplitPanel extends SearchItemSplitPanel {

    public static String NAME = "SearchPollsSplitPanel";
    public static String TITLE = "Search Polls";

    private static final long serialVersionUID = 1L;

    public SearchPollsSplitPanel() {
        super(new PollsItemsTableModel(), NAME, TITLE);

        jTableJScrollPanelLeftPanel.getColumnModel().getColumn(3).setMaxWidth(200);
        jTableJScrollPanelLeftPanel.getColumnModel().getColumn(3).setPreferredWidth(100);
        jTableJScrollPanelLeftPanel.getColumnModel().getColumn(4).setMaxWidth(200);
        jTableJScrollPanelLeftPanel.getColumnModel().getColumn(4).setPreferredWidth(100);

        JMenuItem setVote_Menu = new JMenuItem(Lang.T("To Vote"));
        setVote_Menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//			new UnionSetStatusDialog(th, (UnionCls) itemMenu);

                PollCls poll = (PollCls) (itemTableSelected);
                AssetCls AssetCls = DCSet.getInstance().getItemAssetMap().get((long) (1));
                new PollsDialog(poll, 0, AssetCls);
            }
        });
        this.menuTable.add(setVote_Menu);

        menuTable.addSeparator();

        JMenuItem setStatus_Menu = new JMenuItem(Lang.T("Set status"));
        setStatus_Menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //			new UnionSetStatusDialog(th, (UnionCls) itemMenu);
            }
        });
        this.menuTable.add(setStatus_Menu);

    }
	
	
	

	/*
	// show details
	public void onVoteClick() {
		// GET SELECTED OPTION
		int option = votingDetailsPanel.pollTabPane.pollDetailPanel.optionsTable.getSelectedRow();
		if (option >= 0) {
			option = votingDetailsPanel.pollTabPane.pollDetailPanel.optionsTable.convertRowIndexToModel(option);
		}
		
		//this.pollOptionsTableModel;

		PollCls poll = null;
		if (allVotingsPanel.pollsTable.getSelectedRow() >= 0)
			poll = allVotingsPanel.pollsTableModel.getPoll(
					allVotingsPanel.pollsTable.convertRowIndexToModel(allVotingsPanel.pollsTable.getSelectedRow()));
		
		new PollsDialog(poll, option, (AssetCls) allVotingsPanel.cbxAssets.getSelectedItem());
	}
	*/

    @Override
    public Component getShow(ItemCls item) {
        AssetCls AssetCls = DCSet.getInstance().getItemAssetMap().get((long) (1));
        PollsDetailPanel pollInfo = new PollsDetailPanel((PollCls) item, AssetCls);

        return pollInfo;

    }
}