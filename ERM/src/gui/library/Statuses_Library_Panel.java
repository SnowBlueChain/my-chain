package gui.library;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;

import controller.Controller;
import core.account.Account;
import core.account.PublicKeyAccount;
import core.item.persons.PersonCls;
import gui.items.accounts.Account_Send_Dialog;
import gui.items.mails.Mail_Send_Dialog;
import gui.models.PersonStatusesModel;
import gui.models.Renderer_Left;
import lang.Lang;
import utils.TableMenuPopupUtil;

public class Statuses_Library_Panel extends JPanel {



/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private MTable jTable_Statuses;
private JScrollPane jScrollPane_Tab_Status;
private GridBagConstraints gridBagConstraints;

public Statuses_Library_Panel(PersonCls person){

	
    this.setName(Lang.getInstance().translate("Statuses"));
    this.setLayout(new java.awt.GridBagLayout());


	   PersonStatusesModel statusModel = new PersonStatusesModel (person.getKey());
       jTable_Statuses = new MTable(statusModel);
       
       //CHECKBOX FOR FAVORITE
       		TableColumn to_Date_Column1 = jTable_Statuses.getColumnModel().getColumn( PersonStatusesModel.COLUMN_PERIOD);	
       		//favoriteColumn.setCellRenderer(new Renderer_Boolean()); //personsTable.getDefaultRenderer(Boolean.class));
    //   		int rr = (int) (getFontMetrics( UIManager.getFont("Table.font")).stringWidth("0022-22-2222"));	
       		to_Date_Column1.setMinWidth(80);
       		to_Date_Column1.setMaxWidth(200);
       		to_Date_Column1.setPreferredWidth(120);//.setWidth(30);
      
       		
       		
    
       jScrollPane_Tab_Status = new javax.swing.JScrollPane();
      
      
    

      
       jScrollPane_Tab_Status.setViewportView(jTable_Statuses);

       gridBagConstraints = new java.awt.GridBagConstraints();
       gridBagConstraints.gridx = 0;
       gridBagConstraints.gridy = 0;
       gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
       gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
       gridBagConstraints.weightx = 0.1;
       gridBagConstraints.weighty = 0.1;
       gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
       this.add(jScrollPane_Tab_Status, gridBagConstraints);

    
   	JPopupMenu menu = new JPopupMenu();

	JMenuItem copy_Creator_Address = new JMenuItem(Lang.getInstance().translate("Copy Creator Address"));
	copy_Creator_Address.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int row = jTable_Statuses.getSelectedRow();
			row = jTable_Statuses.convertRowIndexToModel(row);

			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection value = new StringSelection( statusModel.get_Creator_Account(row).getAddress());
			clipboard.setContents(value, null);
		}
	});
	menu.add(copy_Creator_Address);

	JMenuItem menu_copy_Creator_PublicKey = new JMenuItem(Lang.getInstance().translate("Copy Creator Public Key"));
	menu_copy_Creator_PublicKey.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			// StringSelection value = new
			// StringSelection(person.getCreator().getAddress().toString());
			int row = jTable_Statuses.getSelectedRow();
			row = jTable_Statuses.convertRowIndexToModel(row);

			byte[] publick_Key = Controller.getInstance()
					.getPublicKeyByAddress(statusModel.get_Creator_Account(row).getAddress());
			PublicKeyAccount public_Account = new PublicKeyAccount(publick_Key);
			StringSelection value = new StringSelection(public_Account.getBase58());
			clipboard.setContents(value, null);
		}
	});
	menu.add(menu_copy_Creator_PublicKey);


	
	JMenuItem menu_copy_Block_PublicKey = new JMenuItem(Lang.getInstance().translate("Copy No.Transaction"));
	menu_copy_Block_PublicKey.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			// StringSelection value = new
			// StringSelection(person.getCreator().getAddress().toString());
			int row = jTable_Statuses.getSelectedRow();
			row = jTable_Statuses.convertRowIndexToModel(row);

			
			StringSelection value = new StringSelection(statusModel.get_No_Trancaction(row));
			clipboard.setContents(value, null);
		}
	});
	menu.add(menu_copy_Block_PublicKey);

	
	
	
	JMenuItem Send_Coins_item_Menu = new JMenuItem(Lang.getInstance().translate("Send Coins to Creator"));
	Send_Coins_item_Menu.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {

			int row = jTable_Statuses.getSelectedRow();
			row = jTable_Statuses.convertRowIndexToModel(row);
			Account account = statusModel.get_Creator_Account(row);

			new Account_Send_Dialog(null, null, account, null);

		}
	});
	menu.add(Send_Coins_item_Menu);

	JMenuItem Send_Mail_item_Menu = new JMenuItem(Lang.getInstance().translate("Send Mail to Creator"));
	Send_Mail_item_Menu.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {

			int row = jTable_Statuses.getSelectedRow();
			row = jTable_Statuses.convertRowIndexToModel(row);
			Account account = statusModel.get_Creator_Account(row);

		new Mail_Send_Dialog(null, null, account, null);

		}
	});
	menu.add(Send_Mail_item_Menu);

	
	
	
	////////////////////
	TableMenuPopupUtil.installContextMenu(jTable_Statuses, menu); // SELECT














}



}
