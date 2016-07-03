package gui.records;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JScrollPane;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.mapdb.Fun.Tuple4;
import api.ApiErrorFactory;
import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.crypto.Base58;
import core.transaction.R_Vouch;
import core.transaction.Transaction;
import database.DBSet;
//import gui.items.persons.RIPPersonFrame;
import gui.models.AccountsComboBoxModel;
import gui.transaction.OnDealClick;
import lang.Lang;
import ntp.NTP;
import utils.Pair;

public class VouchRecordDialog extends JDialog  {

	private static final long serialVersionUID = 2717571093561259483L;
	
	private static Transaction record;
	private static Record_Info infoPanel;

	public VouchRecordDialog() {
		super();
		
		initComponents();
	
		setSize(400,300);
			this.setTitle(Lang.getInstance().translate("Vouch Record"));
			this.setResizable(true);
			this.setModal(true);

	    setPreferredSize(new Dimension(500, 600));
		//PACK
		this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
	    //MainFrame.this.add(comp, constraints).setFocusable(false);
	}
	
	//private Transaction refreshRecordDetails(JTextField recordTxt, JLabel recordDetails)
	private Transaction refreshRecordDetails(String text)
	{
		
		/*
		if(Controller.getInstance().getStatus() != Controller.STATUS_OK)
		{
			infoPanel.show_mess(Lang.getInstance().translate("Status must be OK to show public key details."));
	        jLabel_RecordInfo.setViewportView(infoPanel);
			return null;
		}
		*/

		Transaction record = null;
		if (text.length() < 40) { 
			//record = R_Vouch.getVouchingRecord(DBSet.getInstance(), jTextField_recordID.getText());
			record = R_Vouch.getVouchingRecord(DBSet.getInstance(), text);
		} else {
			record = Transaction.findByDBRef(DBSet.getInstance(), Base58.decode(text));
		}

		if (record == null) {
			infoPanel.show_mess(Lang.getInstance().translate("Error - use signature of record or blockNo-recNo"));
	        jLabel_RecordInfo.setViewportView(infoPanel);
			return record;
		}
		
		//ENABLE
		jButton_Confirm.setEnabled(true);

		infoPanel.show_001(record);
		//infoPanel.setFocusable(false);
        jLabel_RecordInfo.setViewportView(infoPanel);

        return record;
	}

	public void onGoClick()
			//JComboBox<Account> jComboBox_YourAddress, JTextField feePowTxt)
	{

    	if (!OnDealClick.proccess1(jButton_Confirm)) return;

		Account creator = (Account) jComboBox_YourAddress.getSelectedItem();
    	//String address = pubKey1Txt.getText();
    	int feePow = 0;
    	int parse = 0;
		try {

			//READ FEE POW
			feePow = Integer.parseInt(jFormattedTextField_Fee.getText());
		}				
		catch(Exception e)
		{
			if(parse == 0)
			{
				JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid fee"), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			}
			else
			{
			}

			//ENABLE
			jButton_Confirm.setEnabled(true);

			return;
		}
    	
		//Account authenticator =  new Account(address);
		PrivateKeyAccount authenticator = Controller.getInstance().getPrivateKeyAccountByAddress(creator.getAddress());

		int version = 0; // without user signs
		
		Pair<Transaction, Integer> result = Controller.getInstance().r_Vouch(0, false,
				authenticator, feePow,
				record.getBlockHeight(DBSet.getInstance()), record.getSeqNo(DBSet.getInstance()));
		//Pair<Transaction, Integer> result = new Pair<Transaction, Integer>(null, 0);
		
		//CHECK VALIDATE MESSAGE
		if (result.getB() == Transaction.VALIDATE_OK) {
			JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Record has been authenticated!"), Lang.getInstance().translate("Success"), JOptionPane.INFORMATION_MESSAGE);
			this.dispose();
		} else {
		
			JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate(OnDealClick.resultMess(result.getB())), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
		}
		
		//ENABLE
		jButton_Confirm.setEnabled(true);
		
	}
	
	private void initComponents() {
	        java.awt.GridBagConstraints gridBagConstraints;

	        jLabel_RecordInfo = new javax.swing.JScrollPane();
	        //jLabel_RecordInfo = new javax.swing.JLabel();
	        jLabel_YourAddress = new javax.swing.JLabel();
	        //jComboBox_YourAddress = new javax.swing.JComboBox<>();

	        jLabel_recordID = new javax.swing.JLabel();
	        jTextField_recordID = new javax.swing.JTextField();

	        jLabel_Fee = new javax.swing.JLabel();
	        jFormattedTextField_Fee = new javax.swing.JFormattedTextField();
	        jButton_Cansel = new javax.swing.JButton();
	        jButton_Confirm = new javax.swing.JButton();
	        jLabel_Fee_Check = new javax.swing.JLabel();
	        jLabel_Title = new javax.swing.JLabel();

	        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
	        setMinimumSize(new java.awt.Dimension(650, 23));
	        setModal(true);
	        setPreferredSize(new java.awt.Dimension(700, 600));
	        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
	            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
	                //formAncestorMoved(evt);
	            }
	            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
	            }
	        });
	        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
	        layout.columnWidths = new int[] {0, 9, 0, 9, 0, 9, 0};
	        layout.rowHeights = new int[] {0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0};
	        getContentPane().setLayout(layout);

	        jLabel_recordID.setText(Lang.getInstance().translate("BlocNo-RecNo or signature") +":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 14;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
	        gridBagConstraints.insets = new java.awt.Insets(0, 27, 0, 0);
	        getContentPane().add(jLabel_recordID, gridBagConstraints);

	        /*
	        try {
	            jFormattedTextField_ToDo.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.MaskFormatter("##.##.####")));
	        } catch (java.text.ParseException ex) {
	            ex.printStackTrace();
	        }
	        */
	        jTextField_recordID.setHorizontalAlignment(javax.swing.JTextField.LEFT);
	        jTextField_recordID.setToolTipText("BlockNo-recNo or signature");
	        jTextField_recordID.setMinimumSize(new java.awt.Dimension(300, 20));
	        jTextField_recordID.setText(""); // NOI18N
	        jTextField_recordID.setPreferredSize(new java.awt.Dimension(300, 20));
	        jTextField_recordID.getDocument().addDocumentListener(new DocumentListener() {
	            
				@Override
				public void changedUpdate(DocumentEvent arg0) {
				}
				@Override
				public void insertUpdate(DocumentEvent arg0) {
					VouchRecordDialog.record = refreshRecordDetails(jTextField_recordID.getText());
				}
				@Override
				public void removeUpdate(DocumentEvent arg0) {
					VouchRecordDialog.record = refreshRecordDetails(jTextField_recordID.getText());
				}
	        });
		    
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 14;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        getContentPane().add(jTextField_recordID, gridBagConstraints);
		    
	        
	        jLabel_RecordInfo.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	        infoPanel = new Record_Info(); 
	        //info.show_001(record);
	        //infoPanel.setFocusable(false);
	        //jLabel_RecordInfo.setViewportView(infoPanel);

	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 4;
	        gridBagConstraints.gridwidth = 7;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
	        gridBagConstraints.weightx = 1.0;
	        gridBagConstraints.weighty = 1.0;
	    //    gridBagConstraints.insets = new java.awt.Insets(12, 9, 0, 9);
	        gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 9);
	        getContentPane().add(jLabel_RecordInfo, gridBagConstraints);

	        jLabel_YourAddress.setText(Lang.getInstance().translate("Your Address")+":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
	       // gridBagConstraints.insets = new java.awt.Insets(0, 27, 0, 0);
	        gridBagConstraints.insets = new java.awt.Insets(21, 27, 0, 0);
	        getContentPane().add(jLabel_YourAddress, gridBagConstraints);

	        //AccountsComboBoxModel
	        jComboBox_YourAddress =new JComboBox<Account>(new AccountsComboBoxModel());
	        jComboBox_YourAddress.setMinimumSize(new java.awt.Dimension(500, 22));
	        jComboBox_YourAddress.setPreferredSize(new java.awt.Dimension(500, 22));
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridwidth = 3;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	       // gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 13);
	        gridBagConstraints.insets = new java.awt.Insets(21, 0, 0, 13);
	        getContentPane().add(jComboBox_YourAddress, gridBagConstraints);

	        	        

	        jLabel_Fee.setText(Lang.getInstance().translate("Fee Power") +":");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 17;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
	        gridBagConstraints.insets = new java.awt.Insets(0, 27, 0, 0);
	        getContentPane().add(jLabel_Fee, gridBagConstraints);

	        jFormattedTextField_Fee.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#"))));
	        jFormattedTextField_Fee.setHorizontalAlignment(javax.swing.JTextField.LEFT);
	        jFormattedTextField_Fee.setMinimumSize(new java.awt.Dimension(100, 20));
	        jFormattedTextField_Fee.setText("0");
	        jFormattedTextField_Fee.setPreferredSize(new java.awt.Dimension(100, 20));

	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 17;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
	        gridBagConstraints.insets = new java.awt.Insets(0, 0, 48, 0);
	        getContentPane().add(jFormattedTextField_Fee, gridBagConstraints);

	        jButton_Cansel.setText(Lang.getInstance().translate("Cancel"));
	        jButton_Cansel.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	dispose();	
	            }
	        });
	        
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 19;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
	        gridBagConstraints.insets = new java.awt.Insets(1, 0, 29, 0);
	        getContentPane().add(jButton_Cansel, gridBagConstraints);

	        jButton_Confirm.setText(Lang.getInstance().translate("Confirm"));
	        jButton_Confirm.setToolTipText("");
	        jButton_Confirm.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e)
			    {
			    	onGoClick();
			    }
			});
	        
	        
	        
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 4;
	        gridBagConstraints.gridy = 19;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
	        getContentPane().add(jButton_Confirm, gridBagConstraints);

	        jLabel_Fee_Check.setText("0..6");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 4;
	        gridBagConstraints.gridy = 17;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
	        getContentPane().add(jLabel_Fee_Check, gridBagConstraints);

	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 2;
	        gridBagConstraints.gridwidth = 7;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
	        gridBagConstraints.weightx = 1.0;
	    //    gridBagConstraints.insets = new java.awt.Insets(12, 9, 11, 9);
	        gridBagConstraints.insets = new java.awt.Insets(12, 23, 0, 9);
	        getContentPane().add(jLabel_Title, gridBagConstraints);
	        jLabel_Title.setText(Lang.getInstance().translate("Information about the record"));
	        getContentPane().add(jLabel_Title, gridBagConstraints);

	        pack();
	    }// <
	  

	    // Variables declaration - do not modify                     
	    private javax.swing.JButton jButton_Cansel;
	    private javax.swing.JButton jButton_Confirm;
	    private JComboBox<Account> jComboBox_YourAddress;
	    private javax.swing.JLabel jLabel_Fee;
	    private javax.swing.JFormattedTextField jFormattedTextField_Fee;
	    private javax.swing.JLabel jLabel_Fee_Check;
	    private javax.swing.JScrollPane jLabel_RecordInfo;
	    //private javax.swing.JLabel jLabel_RecordInfo;
	    
	    private javax.swing.JLabel jLabel_recordID;
	    private javax.swing.JTextField jTextField_recordID;

	    private javax.swing.JLabel jLabel_Title;
	    private javax.swing.JLabel jLabel_YourAddress;
	    // End of variables declaration                   
	
}
