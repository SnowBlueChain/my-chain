package gui.items.persons;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Date;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import controller.Controller;
import core.account.Account;
import core.account.PublicKeyAccount;
import core.crypto.Base58;
import core.item.persons.PersonCls;
import core.item.persons.PersonHuman;
import core.transaction.Transaction;
import datachain.DCSet;
import gui.library.Accounts_Library_Panel;
import gui.library.MButton;
import gui.library.M_Accoutn_Text_Field;
import gui.library.Statuses_Library_Panel;
import gui.library.Voush_Library_Panel;
import lang.Lang;
import utils.MenuPopupUtil;

/**
 *
 * @author РЎР°С€Р°
 */
public class Person_Info_002 extends javax.swing.JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Creates new form Person_Info_002
	 */

	private PersonHuman human;
	private PublicKeyAccount publisher;
	public Statuses_Library_Panel statuses_Library_Panel;
	public Accounts_Library_Panel accounts_Library_Panel;
	public Voush_Library_Panel voush_Library_Panel;
	public Person_Owner_Panel person_Owner_Panel;
	public Person_Vouched_Panel person_Vouched_Panel;

	public Person_Info_002(PersonCls person, boolean full) {
		if (person != null)	initComponents(person, full);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents(PersonCls person, boolean full) {
		java.awt.GridBagConstraints gridBagConstraints;

		jPanel3 = new javax.swing.JPanel();
		jLabel_Name = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jTextField_Name = new javax.swing.JTextField();
		jLabel_Description = new javax.swing.JLabel();
		jLabel_Date_Born = new javax.swing.JLabel();
		jTextField_Date_Born = new javax.swing.JTextField();
		jLabel_Gender = new javax.swing.JLabel();
		jTextField_Gender = new javax.swing.JTextField();
		jScrollPane1 = new javax.swing.JScrollPane();
		jTextArea_Description = new javax.swing.JTextArea();
		jLabel_Creator = new javax.swing.JLabel();
//		jTextField_Creator = new javax.swing.JTextField();
		jLabel1 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jPanel_Image = new javax.swing.JPanel();
		
		jLabel_Owner = new javax.swing.JLabel();
		
	//	jTextField_Owner = new javax.swing.JTextField();
		new javax.swing.JTextField();
		jLabel_Owner_Sign = new javax.swing.JLabel();
		jTextField_Owner_Sign = new javax.swing.JTextField();

		human = null;
		PublicKeyAccount owner = null;
		byte[] recordReference = person.getReference();
		Transaction issue_record = Transaction.findByDBRef(DCSet.getInstance(), recordReference);
		publisher = issue_record.getCreator();
		if (person instanceof PersonHuman) {
			human = (PersonHuman) person;
			if (human.isMustBeSigned()) {
				owner = person.getOwner();
			}
		}

		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new java.awt.GridBagLayout());

		jPanel3.setLayout(new java.awt.GridBagLayout());
		  JPanel jPanel1 = new javax.swing.JPanel();
	//	 jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	        jPanel1.setLayout(new java.awt.GridBagLayout());
	        JLabel jLabel1N = new JLabel();
	        jLabel1N.setText(Lang.getInstance().translate("Name") + ":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
	        gridBagConstraints.insets = new java.awt.Insets(8, 8, 3, 0);
	        jPanel1.add(jLabel1N, gridBagConstraints);

	        javax.swing.JTextField  jTextField1N;
 	        jTextField1N = new JTextField ();
	        jTextField1N.setEditable(false);
	        jTextField1N.setText(person.getName());
			MenuPopupUtil.installContextMenu(jTextField1N);
			jTextField1N.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					jTextField_NameActionPerformed(evt);
				}
			});
	        
	        
	     //   jTextField1N.setMinimumSize(new java.awt.Dimension(120, 20));
	        jTextField1N.setName(""); // NOI18N
	     //   jTextField1N.setPreferredSize(new java.awt.Dimension(120, 20));
	    //    jTextField1N.setRequestFocusEnabled(false);
	          gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
	        gridBagConstraints.weightx = 0.5;
	        gridBagConstraints.insets = new java.awt.Insets(8, 4, 3, 0);
	        jPanel1.add(jTextField1N, gridBagConstraints);

		
	        JLabel lbl_Block = new JLabel(Lang.getInstance().translate("Block") + ":");
	        
			gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 3;
	        gridBagConstraints.gridy = 0;
	      //  gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
	     //   gridBagConstraints.weightx = 0.1;
	        gridBagConstraints.insets = new java.awt.Insets(8, 8, 3, 8);
	        
	        
	        jPanel1.add(lbl_Block, gridBagConstraints);    
	        
	        
	    
		JTextField txt_Block = new JTextField(issue_record.getBlockHeight(DCSet.getInstance())+"-"+issue_record.getSeqNo(DCSet.getInstance()));
		
		txt_Block.setEditable(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 3, 8);
        
        jPanel1.add(txt_Block, gridBagConstraints);
        MenuPopupUtil.installContextMenu(txt_Block);
        txt_Block.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField_NameActionPerformed(evt);
			}
		});
        
        MButton btn_Block = new MButton(Lang.getInstance().translate("Deals"), 2.0);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 3, 8);
        
        if (person.isConfirmed()) {
        	jPanel1.add(btn_Block, gridBagConstraints);
        	btn_Block.addMouseListener(new MouseListener(){
	
				@Override
				public void mouseClicked(MouseEvent arg0) {
					// TODO Auto-generated method stub
					Person_Work_Dialog pWD = new Person_Work_Dialog(person);
					
					pWD.setLocation(arg0.getXOnScreen()-pWD.getWidth(), arg0.getYOnScreen());
					pWD.setVisible(true);
				}
	
				@Override
				public void mouseEntered(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}
	
				@Override
				public void mouseExited(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}
	
				@Override
				public void mousePressed(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}
	
				@Override
				public void mouseReleased(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}
	        	
	        });
       }
        
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 0.1;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		
		add(jPanel1, gridBagConstraints);

		jLabel_Name.setText(Lang.getInstance().translate("Name") + ":");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
	//	add(jLabel_Name, gridBagConstraints);

		jTextField_Name.setEditable(false);
		jTextField_Name.setText(person.getName());
		MenuPopupUtil.installContextMenu(jTextField_Name);
		jTextField_Name.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField_NameActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.1;
	//	add(jTextField_Name, gridBagConstraints);

		jLabel_Description.setText(Lang.getInstance().translate("Description") + ":");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
		jPanel3.add(jLabel_Description, gridBagConstraints);

		jTextArea_Description.setRows(5);

		MenuPopupUtil.installContextMenu(jTextArea_Description);
		jTextArea_Description.setEditable(false);
		jTextArea_Description.setWrapStyleWord(true);
		jTextArea_Description.setLineWrap(true);
		

		String descript = Lang.getInstance().translate("Gender") + ":";
		if (person.getGender() == 0)
			descript =descript+Lang.getInstance().translate("Male");
		if (person.getGender() == 1)
			descript =descript+Lang.getInstance().translate("Female");
		long bi = person.getBirthday();
		long de = person.getDeathday();
		String biStr = person.getBirthdayStr();
		if (de/10 > bi/10){
			//descript =descript+"\n"+ new Date(person.getBirthday()).toString() + " - "+ new Date(person.getDeathday()).toString();
			descript =descript+"\n"+ biStr + " - "+ person.getDeathdayStr();			
			
		}else{
			
			//descript = descript+"\n" + Lang.getInstance().translate("Birthday") + ":" + new Date(person.getBirthday()) + "";
			descript = descript+ "\n" + Lang.getInstance().translate("Birthday") + ":" + biStr;
			
			
		}
		
		descript = descript + "\n" + Lang.getInstance().translate("Coordinates Birth") + ": " + ((Float)person.getBirthLatitude()).toString() + "," +	((Float)person.getBirthLongitude()).toString();
		descript = descript + "\n" + Lang.getInstance().translate("Height") + ": " + person.getHeight();
		
		descript = descript+"\n" + person.getDescription();
		jTextArea_Description.setText(descript);

		jScrollPane1.setViewportView(jTextArea_Description);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		// gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		// gridBagConstraints.weightx = 0.1;
		gridBagConstraints.weighty = 0.6;
		jPanel3.add(jScrollPane1, gridBagConstraints);

		int gridy = 8;
		if (human.isMustBeSigned() && owner != null && !owner.equals(publisher)) {

			jLabel_Owner.setText(Lang.getInstance().translate("Data Creator") + ":");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridy++;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
			jPanel3.add(jLabel_Owner, gridBagConstraints);

			jTextField_Owner = new M_Accoutn_Text_Field(owner);
			jTextField_Owner.setEditable(false);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridy++;
			gridBagConstraints.gridwidth = 3;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
			jPanel3.add(jTextField_Owner, gridBagConstraints);

			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridy++;
			gridBagConstraints.gridwidth = 3;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);

			
			jLabel_Owner_Sign.setText(Lang.getInstance().translate("Owner Sign") + ":");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridy++;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
		//	jPanel3.add(jLabel_Owner_Sign, gridBagConstraints);

			jTextField_Owner_Sign.setEditable(false);

			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridy++;
			gridBagConstraints.gridwidth = 3;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);

			jTextField_Owner_Sign.setText(human.isSignatureValid() ? Base58.encode(human.getOwnerSignature())
					: Lang.getInstance().translate("Wrong signaryte for data owner"));
		//	jPanel3.add(jTextField_Owner_Sign, gridBagConstraints);
		}
	
		jLabel_Creator.setText(Lang.getInstance().translate("Registrator") + ":");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridy++;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
		jPanel3.add(jLabel_Creator, gridBagConstraints);

		jTextField_Creator = new M_Accoutn_Text_Field(publisher);
		jTextField_Creator.setEditable(false);
	//	jTextField_Creator.setText(publisher.toString());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridy;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
		gridBagConstraints.weightx = 0.2;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		jPanel3.add(jTextField_Creator, gridBagConstraints);
		

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		// gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 8, 8, 8);
		add(jPanel3, gridBagConstraints);

		// jPanel_Image.setBorder(javax.swing.BorderFactory.createLineBorder(new
		// java.awt.Color(0, 0, 0)));
		jPanel_Image.setLayout(new java.awt.GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		// gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		// gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		// gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.6;
		gridBagConstraints.insets = new java.awt.Insets(0, 8, 8, 0);
		add(jPanel_Image, gridBagConstraints);

		// jLabel2.setText("jLabel2");
		ImageIcon image = new ImageIcon(person.getImage());
		int x = image.getIconWidth();
		int y = image.getIconHeight();

		int x1 = 250;
		double k = ((double) x / (double) x1);
		y = (int) ((double) y / k);
		

		if (y != 0) {
			Image Im = image.getImage().getScaledInstance(x1, y, 1);

			jLabel2.setIcon(new ImageIcon(Im));
		}
		
		jLabel2.addMouseListener(new Image_mouse_Clikl());

			

		// jLabel2.setBorder(javax.swing.BorderFactory.createLineBorder(new
		// java.awt.Color(0, 0, 0)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.weightx = 0.2;
		gridBagConstraints.weighty = 0.2;
		jPanel_Image.add(jLabel2, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 0.1;
		gridBagConstraints.weighty = 0.4;
		gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
		// add(jScrollPane2, gridBagConstraints);

		javax.swing.JTabbedPane jTabbedPane1 = new javax.swing.JTabbedPane();

		add(jTabbedPane1, gridBagConstraints);

		// statuses panel
		
		statuses_Library_Panel = new Statuses_Library_Panel(person);

		jTabbedPane1.add(statuses_Library_Panel);

		// Accounts panel
		accounts_Library_Panel = new Accounts_Library_Panel(person);
		jTabbedPane1.add(accounts_Library_Panel);

		// vouch panel
		voush_Library_Panel = new Voush_Library_Panel(issue_record);
		jTabbedPane1.add(voush_Library_Panel);
		
		
		// created person panel
		person_Owner_Panel = new Person_Owner_Panel(person);
		jTabbedPane1.add(person_Owner_Panel);
		// vouched person
		person_Vouched_Panel = new Person_Vouched_Panel(person);
		jTabbedPane1.add(person_Vouched_Panel);

	}// </editor-fold>

	private void jTextField_NameActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
	}

	// Variables declaration - do not modify
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel_Creator;
	private javax.swing.JLabel jLabel_Date_Born;
	private javax.swing.JLabel jLabel_Description;
	private javax.swing.JLabel jLabel_Gender;
	private javax.swing.JLabel jLabel_Name;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel_Image;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTextArea jTextArea_Description;
	private javax.swing.JTextField jTextField1;
	private M_Accoutn_Text_Field jTextField_Creator;
	private javax.swing.JTextField jTextField_Date_Born;
	private javax.swing.JTextField jTextField_Gender;
	private javax.swing.JTextField jTextField_Name;
	private javax.swing.JLabel jLabel_Owner;
	private M_Accoutn_Text_Field jTextField_Owner;
	private javax.swing.JLabel jLabel_Owner_Sign;
	private javax.swing.JTextField jTextField_Owner_Sign;

	// End of variables declaration
	class  Image_mouse_Clikl extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
	//		Point p = e.getPoint();
	//		int row = search_Table.rowAtPoint(p);
			if(e.getClickCount() == 2)
			{
	//			row = personsTable.convertRowIndexToModel(row);
	//			PersonCls person = tableModelPersons.getPerson(row);
	//			new PersonFrame(person);
				
			}
		
		//	if(e.getClickCount() == 1 & e.getButton() == e.BUTTON1)
				if( e.getButton() == MouseEvent.BUTTON1)
			{
				
		/*		
				row = search_Table.convertRowIndexToModel(row);
				PersonCls person = search_Table_Model.getPerson(row);	
//выводим меню всплывающее
				if(Controller.getInstance().isItemFavorite(person))
				{
					Search_run_menu.jButton3.setText(Lang.getInstance().translate("Remove Favorite"));
				}
				else
				{
					Search_run_menu.jButton3.setText(Lang.getInstance().translate("Add Favorite"));
				}
	//			alpha = 255;
				alpha_int = 5;
				Search_run_menu.setBackground(new Color(1,204,102,255));		
			    Search_run_menu.setLocation(e.getXOnScreen(), e.getYOnScreen());
			    Search_run_menu.repaint();
		        Search_run_menu.setVisible(true);		
	    
		    */
		
			}
			
		}
		}
	
	public void  delay_on_Close(){
		statuses_Library_Panel.delay_on_close();
		accounts_Library_Panel.delay_on_close();
		voush_Library_Panel.delay_on_close();
		person_Owner_Panel.delay_on_close();
		person_Vouched_Panel.delay_on_close();
		
		
		
	}
	
	
}
