package gui.items.persons;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.mapdb.Fun.Tuple2;

import controller.Controller;
import core.account.Account;
import core.account.PublicKeyAccount;
import core.crypto.Base58;
import core.item.persons.PersonCls;
import core.item.persons.PersonHuman;
import core.transaction.Transaction;
import database.DBSet;
import gui.items.accounts.Account_Send_Dialog;
import gui.items.mails.Mail_Send_Dialog;
import gui.models.PersonAccountsModel;
import gui.models.PersonStatusesModel;
import gui.models.Renderer_Left;
import lang.Lang;
import utils.DateTimeFormat;
import utils.MenuPopupUtil;
import utils.TableMenuPopupUtil;

/**
 *
 * @author Саша
 */
public class Person_Info_002 extends javax.swing.JPanel {

    /**
     * Creates new form Person_Info_002
     */
	
	private PersonHuman human;
	private JPanel jPanel_Tab_Status;
	private JScrollPane jScrollPane_Tab_Status;
	private JTable jTable_Tab_Status;
	private JLabel jLabel_Tab_Status;
	private JPanel jPanel_Tab_Accounts;
	private JTable jTable_Tab_Accounts;
	private JLabel jLabel_Tab_Accounts;
	
    public Person_Info_002(PersonCls person, boolean full) {
    	
        initComponents(person, full);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
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
        jTextField_Creator = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jPanel_Image = new javax.swing.JPanel();
        jLabel_Title = new javax.swing.JLabel();
        jLabel_Statuses = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable_Statuses = new javax.swing.JTable();
        jLabel_Sign = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable_Sign = new javax.swing.JTable();
        jLabel_Owner = new javax.swing.JLabel();
        jLabel_Seg_No = new javax.swing.JLabel();
        jTextField_Owner = new javax.swing.JTextField();
        jTextField_Seg_No = new javax.swing.JTextField();
        jLabel_Owner_Sign = new javax.swing.JLabel();
        jTextField_Owner_Sign = new javax.swing.JTextField();
        

        
        
        SimpleDateFormat formatDate = new SimpleDateFormat("dd.MM.yyyy"); // HH:mm");
        
        setMinimumSize(new java.awt.Dimension(100, 100));
        setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        add(jLabel_Title, gridBagConstraints);
        
        if (person == null){
        	jLabel_Title.setText(Lang.getInstance().translate("Person not found"));
        	return;
        	
        }
        
        jLabel_Title.setText(Lang.getInstance().translate(""));
        
        
        
        
        
        jLabel_Name.setText(Lang.getInstance().translate("Name")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        jPanel3.add(jLabel_Name, gridBagConstraints);

        jTextField_Name.setEditable(false);
        jTextField_Name.setText(person.getName());
        MenuPopupUtil.installContextMenu(jTextField_Name);
        jTextField_Name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_NameActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        jPanel3.add(jTextField_Name, gridBagConstraints);

        jLabel_Description.setText(Lang.getInstance().translate("Description")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel3.add(jLabel_Description, gridBagConstraints);

        jLabel_Date_Born.setText(Lang.getInstance().translate("Birthday")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel3.add(jLabel_Date_Born, gridBagConstraints);

        
        //////////////////////////////
        /*
        Date ddd = new Date(person.getBirthday());
        DateFormat timeFormat = new SimpleDateFormat("YYYY.MM.DD");
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dataStr = timeFormat.format(ddd);
        
    	Locale local = new Locale("ru","RU"); // формат даты
    	DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, local); // для формата даты
    	String dataStrLoc = df.format(ddd).toString();
    	String dataStrLoc1 = df.format(new Date(person.getBirthday())).toString();
    	*/
        
        jTextField_Date_Born.setEditable(false);
        jTextField_Date_Born.setText(new Date(person.getBirthday())+ "");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        jPanel3.add(jTextField_Date_Born, gridBagConstraints);

		///TimeZone.setDefault(tz);
		////////////////////////

		
        jLabel_Gender.setText(Lang.getInstance().translate("Gender")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel3.add(jLabel_Gender, gridBagConstraints);

        jTextField_Gender.setEditable(false);
        if(person.getGender() == 0) jTextField_Gender.setText(Lang.getInstance().translate("Male"));
        if(person.getGender() ==1) jTextField_Gender.setText(Lang.getInstance().translate("Female"));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        jPanel3.add(jTextField_Gender, gridBagConstraints);

    //    jTextArea_Description.setColumns(20);
        jTextArea_Description.setRows(5);
        
        MenuPopupUtil.installContextMenu(jTextArea_Description);
        jTextArea_Description.setEditable(false);
        
        jTextArea_Description.setText(person.getDescription());
                
        jScrollPane1.setViewportView(jTextArea_Description);

        human = null;
        PublicKeyAccount owner = null;
        byte[] recordReference = person.getReference();
        Transaction issue_record = Transaction.findByDBRef(DBSet.getInstance(), recordReference);
        PublicKeyAccount creator = issue_record.getCreator();
        if (person instanceof PersonHuman) {
        	human = (PersonHuman) person;
        	if (human.isMustBeSigned()) {
        		owner = person.getOwner();
        	}
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
   //     gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.6;
        jPanel3.add(jScrollPane1, gridBagConstraints);

        jLabel_Creator.setText(Lang.getInstance().translate("Publisher")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
        jPanel3.add(jLabel_Creator, gridBagConstraints);

        jTextField_Creator.setEditable(false);
        jTextField_Creator.setText(creator.toString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        jPanel3.add(jTextField_Creator, gridBagConstraints);
                
        
        if (human.isMustBeSigned() && owner != null
        		&& !owner.equals(creator)) {
        
	        jLabel_Owner.setText(Lang.getInstance().translate("Owner")+":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 10;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
	        jPanel3.add(jLabel_Owner, gridBagConstraints);
	
	        jTextField_Owner.setEditable(false);
	        
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 11;
	        gridBagConstraints.gridwidth = 3;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        gridBagConstraints.weightx = 0.2;
	        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
	        
	        jTextField_Owner.setText(owner.toString());
	        jPanel3.add(jTextField_Owner, gridBagConstraints);
	
        
	        jLabel_Owner_Sign.setText(Lang.getInstance().translate("Owner Sign")+":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 12;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
	        jPanel3.add(jLabel_Owner_Sign, gridBagConstraints);
	
	        jTextField_Owner_Sign.setEditable(false);
	        
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 13;
	        gridBagConstraints.gridwidth = 3;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        gridBagConstraints.weightx = 0.2;
	        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
	        
	        jTextField_Owner_Sign.setText(human.isSignatureValid()?
	        		Base58.encode(human.getOwnerSignature()):
        			Lang.getInstance().translate("Wrong signaryte for data owner"));
	        jPanel3.add(jTextField_Owner_Sign, gridBagConstraints);
        }
        
        
        JPopupMenu creator_Meny = new JPopupMenu();
        JMenuItem copy_Creator_Address1 = new JMenuItem(Lang.getInstance().translate("Copy Address"));
  		copy_Creator_Address1.addActionListener(new ActionListener()
  		{
  			public void actionPerformed(ActionEvent e) 
  			{
  				
  				      				
  				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				StringSelection value = new StringSelection(person.getOwner().getAddress().toString());
  			    clipboard.setContents(value, null);
  			}
  		});
  		 creator_Meny.add(copy_Creator_Address1);
  		 
  		JMenuItem copyPublicKey = new JMenuItem(Lang.getInstance().translate("Copy Public Key"));
		copyPublicKey.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				//StringSelection value = new StringSelection(person.getCreator().getAddress().toString());
  				byte[] publick_Key = Controller.getInstance().getPublicKeyByAddress(person.getOwner().getAddress());
  				PublicKeyAccount public_Account = new PublicKeyAccount(publick_Key);
  				StringSelection value = new StringSelection(public_Account.getBase58());
  				 clipboard.setContents(value, null);
			}
		});
		creator_Meny.add(copyPublicKey);
  		 
  		 

   		JMenuItem Send_Coins_Crator = new JMenuItem(Lang.getInstance().translate("Send Coins"));
   		Send_Coins_Crator.addActionListener(new ActionListener()
   		{
   			public void actionPerformed(ActionEvent e) 
   			{
   				new Account_Send_Dialog(null, null, new Account(person.getOwner().getAddress().toString()),null);
   			}
   		});
   		creator_Meny.add(Send_Coins_Crator);

   		JMenuItem Send_Mail_Creator = new JMenuItem(Lang.getInstance().translate("Send Mail"));
   		Send_Mail_Creator.addActionListener(new ActionListener()
   		{
   			public void actionPerformed(ActionEvent e) 
   			{
   			
   				new Mail_Send_Dialog(null, null, new Account(person.getOwner().getAddress().toString()),null);
   			}
   		});
   		creator_Meny.add(Send_Mail_Creator);
   		
   	 jTextField_Creator.add(creator_Meny);
   	 jTextField_Creator.setComponentPopupMenu(creator_Meny);
  		 
        
        

        jLabel1.setText(Lang.getInstance().translate("Deathday")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 2);
        jPanel3.add(jLabel1, gridBagConstraints);
        
        jTextField1.setEditable(false);
        Long end = person.getDeathday();
        if (end == null || end <= person.getBirthday()){
        	jTextField1.setText( "-");
        	jTextField1.setVisible(false);
        	jLabel1.setVisible(false);
        }
        else
        
        	jTextField1.setText( new Date (end)+"");
        
        
        
      
        
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        jPanel3.add(jTextField1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
       gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
   //     gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty =0.05;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        add(jPanel3, gridBagConstraints);

   //     jPanel_Image.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel_Image.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
      //  gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
      //  gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
     //   gridBagConstraints.weightx = 1.0;
     //   gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 0);
        add(jPanel_Image, gridBagConstraints);

        
     //   jLabel2.setText("jLabel2");
        ImageIcon image = new ImageIcon(person.getImage());
        int x = image.getIconWidth();
        int y = image.getIconHeight();
        
        double k =  ((double)x/(double)150);
        y = (int) ((double)y/ k);
        x = 150;
                
        if (y != 0){
			Image Im = image.getImage().getScaledInstance(x,    y,
			        1);
		        
        jLabel2.setIcon(new ImageIcon( Im));
        }
        
 //       jLabel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx =1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 0.2;
        jPanel_Image.add(jLabel2, gridBagConstraints);
        
        
        
        


             
        
        PersonStatusesModel statusModel = new PersonStatusesModel (person.getKey());
        jTable_Statuses.setModel(statusModel);
        
        jTable_Statuses.setDefaultRenderer(String.class, new Renderer_Left(jTable_Statuses.getFontMetrics(jTable_Statuses.getFont()),statusModel.get_Column_AutoHeight())); // set renderer
        //CHECKBOX FOR FAVORITE
        		TableColumn to_Date_Column1 = jTable_Statuses.getColumnModel().getColumn( PersonStatusesModel.COLUMN_PERIOD);	
        		//favoriteColumn.setCellRenderer(new Renderer_Boolean()); //personsTable.getDefaultRenderer(Boolean.class));
        		to_Date_Column1.setMinWidth(80);
        		to_Date_Column1.setMaxWidth(200);
        		to_Date_Column1.setPreferredWidth(120);//.setWidth(30);
        
       // jTable1.setPreferredSize(new java.awt.Dimension(100, 64));
        
               
        
     //   jScrollPane2.setViewportView(jTable_Statuses);

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
        
        
        
        
       
        jPanel_Tab_Status = new javax.swing.JPanel();
        jScrollPane_Tab_Status = new javax.swing.JScrollPane();
       
        jLabel_Tab_Status = new javax.swing.JLabel();
        jPanel_Tab_Accounts = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane_Tab_Accounts = new javax.swing.JScrollPane();
        jTable_Tab_Accounts = new javax.swing.JTable();
        jLabel_Tab_Accounts = new javax.swing.JLabel();

     //   setLayout(new java.awt.GridBagLayout());

        jPanel_Tab_Status.setLayout(new java.awt.GridBagLayout());

       
        jScrollPane_Tab_Status.setViewportView(jTable_Statuses);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel_Tab_Status.add(jScrollPane_Tab_Status, gridBagConstraints);

        jLabel_Tab_Status.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
      //  jPanel_Tab_Status.add(jLabel_Tab_Status, gridBagConstraints);

        jTabbedPane1.addTab(Lang.getInstance().translate("Statuses"), jPanel_Tab_Status);

        jPanel_Tab_Accounts.setLayout(new java.awt.GridBagLayout());

       
        
        
        PersonAccountsModel personModel = new PersonAccountsModel(person.getKey());
        jTable_Sign.setModel(personModel);
        
        jTable_Sign.setDefaultRenderer(String.class, new Renderer_Left(jTable_Sign.getFontMetrics(jTable_Sign.getFont()),personModel.get_Column_AutoHeight())); // set renderer
      //CHECKBOX FOR FAVORITE
      		TableColumn to_Date_Column = jTable_Sign.getColumnModel().getColumn( PersonAccountsModel.COLUMN_TO_DATE);	
      		//favoriteColumn.setCellRenderer(new Renderer_Boolean()); //personsTable.getDefaultRenderer(Boolean.class));
      		to_Date_Column.setMinWidth(50);
      		to_Date_Column.setMaxWidth(200);
      		to_Date_Column.setPreferredWidth(80);//.setWidth(30);
        
        
        
        jScrollPane_Tab_Accounts.setViewportView(jTable_Sign);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel_Tab_Accounts.add(jScrollPane_Tab_Accounts, gridBagConstraints);

        jLabel_Tab_Accounts.setText("jLabel2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
  //      jPanel_Tab_Accounts.add(jLabel_Tab_Accounts, gridBagConstraints);
        
       

        jTabbedPane1.addTab( Lang.getInstance().translate("Accounts"), jPanel_Tab_Accounts);
        
        
        
        
        
        
        
        
        

     
      
        
       
      

        
        
        
        
        

     
   
    
        JPopupMenu menu = new JPopupMenu();	
  		
  		JMenuItem copyAddress = new JMenuItem(Lang.getInstance().translate("Copy Address"));
  		copyAddress.addActionListener(new ActionListener()
  		{
  			public void actionPerformed(ActionEvent e) 
  			{
  				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				      				
  				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				StringSelection value = new StringSelection(personModel.getAccount_String(row));
  			    clipboard.setContents(value, null);
  			}
  		});
  		menu.add(copyAddress);
  		
  		JMenuItem menu_copyPublicKey = new JMenuItem(Lang.getInstance().translate("Copy Public Key"));
  		menu_copyPublicKey.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				//StringSelection value = new StringSelection(person.getCreator().getAddress().toString());
				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				      				
  				byte[] publick_Key = Controller.getInstance().getPublicKeyByAddress(personModel.getAccount_String(row));
  				PublicKeyAccount public_Account = new PublicKeyAccount(publick_Key);
  				StringSelection value = new StringSelection(public_Account.getBase58());
  				 clipboard.setContents(value, null);
			}
		});
		menu.add(menu_copyPublicKey);
  		
  		JMenuItem copy_Creator_Address = new JMenuItem(Lang.getInstance().translate("Copy Creator Address"));
  		copy_Creator_Address.addActionListener(new ActionListener()
  		{
  			public void actionPerformed(ActionEvent e) 
  			{
  				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				      				
  				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				StringSelection value = new StringSelection(personModel.get_Creator_Account(row));
  			    clipboard.setContents(value, null);
  			}
  		});
  		menu.add(copy_Creator_Address);
  		
  		
  		JMenuItem menu_copy_Creator_PublicKey = new JMenuItem(Lang.getInstance().translate("Copy Creator Public Key"));
  		menu_copy_Creator_PublicKey.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  				//StringSelection value = new StringSelection(person.getCreator().getAddress().toString());
				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				      				
  				byte[] publick_Key = Controller.getInstance().getPublicKeyByAddress(personModel.get_Creator_Account(row));
  				PublicKeyAccount public_Account = new PublicKeyAccount(publick_Key);
  				StringSelection value = new StringSelection(public_Account.getBase58());
  				 clipboard.setContents(value, null);
			}
		});
		menu.add(menu_copy_Creator_PublicKey);
  		
  		
  		
  		JMenuItem Send_Coins_item_Menu = new JMenuItem(Lang.getInstance().translate("Send Coins"));
  		Send_Coins_item_Menu.addActionListener(new ActionListener()
  		{
  			public void actionPerformed(ActionEvent e) 
  			{
  				
  				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				Account account = personModel.getAccount(row);
  				
  				new Account_Send_Dialog(null, null,account, null);
  				
  				
  				
  			}
  		});
  		menu.add(Send_Coins_item_Menu);

  		JMenuItem Send_Mail_item_Menu = new JMenuItem(Lang.getInstance().translate("Send Mail"));
  		Send_Mail_item_Menu.addActionListener(new ActionListener()
  		{
  			public void actionPerformed(ActionEvent e) 
  			{
  				
  				int row = jTable_Sign.getSelectedRow();
  				row = jTable_Sign.convertRowIndexToModel(row);
  				Account account = personModel.getAccount(row);
  				
  				new Mail_Send_Dialog(null,null,account,null);
  				
  				
  			
  				
  			}
  		});
  		menu.add(Send_Mail_item_Menu);
  		
    	
  		
  		////////////////////
  		TableMenuPopupUtil.installContextMenu(jTable_Sign, menu);  // SELECT ROW ON WHICH CLICKED RIGHT BUTTON
  	
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
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
    private javax.swing.JLabel jLabel_Sign;
    private javax.swing.JLabel jLabel_Statuses;
    private javax.swing.JLabel jLabel_Title;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel_Image;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable_Sign;
    private javax.swing.JTable jTable_Statuses;
    private javax.swing.JTextArea jTextArea_Description;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField_Creator;
    private javax.swing.JTextField jTextField_Date_Born;
    private javax.swing.JTextField jTextField_Gender;
    private javax.swing.JTextField jTextField_Name;
    private javax.swing.JLabel jLabel_Owner;
    private javax.swing.JLabel jLabel_Seg_No;
    private javax.swing.JTextField jTextField_Owner;
    private javax.swing.JTextField jTextField_Seg_No;
    
    private javax.swing.JLabel jLabel_Owner_Sign;
    private javax.swing.JTextField jTextField_Owner_Sign;
    
    // End of variables declaration                   
}
