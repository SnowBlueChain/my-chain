package gui.items;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import controller.Controller;
import core.item.ItemCls;
import gui.Split_Panel;
import gui.library.MTable;
import lang.Lang;
import utils.MenuPopupUtil;
import utils.TableMenuPopupUtil;

public class Item_Search_SplitPanel extends Item_SplitPanel {

	private static final long serialVersionUID = 2717571093561259483L;
	protected TableModelItems search_Table_Model;
	private JTextField key_Item;
//	protected JMenuItem favorite_menu_items;
//	protected JPopupMenu menu_Table;
//	protected ItemCls item_Menu;
//	protected ItemCls item_Table_Selected = null;

	@SuppressWarnings("rawtypes")
	public Item_Search_SplitPanel(TableModelItems search_Table_Model1, String gui_Name, String search_Label_Text) {

		super(search_Table_Model1, gui_Name);
		this.search_Table_Model = search_Table_Model1;
		setName(Lang.getInstance().translate(search_Label_Text));
		searthLabel_SearchToolBar_LeftPanel.setText(Lang.getInstance().translate("Search") + ":  ");
		// not show buttons
		jToolBar_RightPanel.setVisible(false);
		toolBar_LeftPanel.setVisible(true);
		button1_ToolBar_LeftPanel.setVisible(false);
		button2_ToolBar_LeftPanel.setVisible(false);

		this.searchToolBar_LeftPanel.setVisible(true);
		this.toolBar_LeftPanel.add(new JLabel(Lang.getInstance().translate("Find Key") + ":"));
		key_Item = new JTextField();
		key_Item.setToolTipText("");
		key_Item.setAlignmentX(1.0F);
		key_Item.setMinimumSize(new java.awt.Dimension(100, 20));
		key_Item.setName(""); // NOI18N
		key_Item.setPreferredSize(new java.awt.Dimension(100, 20));
		key_Item.setMaximumSize(new java.awt.Dimension(2000, 20));

		MenuPopupUtil.installContextMenu(key_Item);

		this.toolBar_LeftPanel.add(key_Item);
		key_Item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				searchTextField_SearchToolBar_LeftPanel.setText("");
				Label_search_Info_Panel.setText(Lang.getInstance().translate("Waiting..."));
				jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
				new Thread() {
					@Override
					public void run() {
						search_Table_Model.Find_item_from_key(key_Item.getText());
						if (search_Table_Model.getRowCount() < 1) {
							Label_search_Info_Panel.setText(Lang.getInstance().translate("Not Found"));
							jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
							jScrollPane_jPanel_RightPanel.setViewportView(null);
							return;
						}
						jScrollPanel_LeftPanel.setViewportView(jTable_jScrollPanel_LeftPanel);
					}
				}.start();
			}
		});

		
		// UPDATE FILTER ON TEXT CHANGE

		searchTextField_SearchToolBar_LeftPanel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				// GET VALUE
				String search = searchTextField_SearchToolBar_LeftPanel.getText();
				if (search.equals("")) {
					jScrollPane_jPanel_RightPanel.setViewportView(null);
					search_Table_Model.clear();
					Label_search_Info_Panel.setText(Lang.getInstance().translate("Enter more  2 characters"));
					jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
					return;
				}
				if (search.length() < 3) {
					Label_search_Info_Panel.setText(Lang.getInstance().translate("Enter more  2 characters"));
					jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
					return;
				}
				key_Item.setText("");

				Label_search_Info_Panel.setText(Lang.getInstance().translate("Waiting..."));
				jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
				new Thread() {
					@Override
					public void run() {
						search_Table_Model.set_Filter_By_Name(search);
						if (search_Table_Model.getRowCount() < 1) {
							Label_search_Info_Panel.setText(Lang.getInstance().translate("Not Found"));
							jScrollPanel_LeftPanel.setViewportView(search_Info_Panel);
							jScrollPane_jPanel_RightPanel.setViewportView(null);
							return;
						}
						jScrollPanel_LeftPanel.setViewportView(jTable_jScrollPanel_LeftPanel);
					}
				}.start();
			}

		});

	}
}
