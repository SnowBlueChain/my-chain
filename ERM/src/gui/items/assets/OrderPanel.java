package gui.items.assets;

import gui.AccountRenderer;
import gui.DebugTabPane;
import gui.PasswordPane;
import gui.models.AccountsComboBoxModel;
import gui.transaction.OnDealClick;
import lang.Lang;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

//import settings.Settings;
import utils.DateTimeFormat;
import utils.Pair;
import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.item.assets.AssetCls;
import core.transaction.Transaction;

@SuppressWarnings("serial")
public class OrderPanel extends JPanel
{
	static Logger LOGGER = Logger.getLogger(OrderPanel.class.getName());

	private AssetCls have;
	private AssetCls want;
	private JButton sellButton;
	private JComboBox<Account> cbxAccount;
	public JTextField txtAmount;
	public JTextField txtPrice;
	private JTextField txtFeePow;
	private JTextField txtBuyingPrice;
	private JTextField txtBuyingAmount;
	private JTextPane superHintText;
	
	public OrderPanel(AssetCls have, AssetCls want, boolean buying)
	{
		this.setLayout(new GridBagLayout());
		
		this.have = have;
		this.want = want;
		
		//PADDING
		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		//LABEL GBC
		GridBagConstraints superhintGBC = new GridBagConstraints();
		superhintGBC.insets = new Insets(0, 5, 5, 0);
		superhintGBC.fill = GridBagConstraints.BOTH;   
		superhintGBC.anchor = GridBagConstraints.SOUTHWEST;
		superhintGBC.gridx = 0;
		superhintGBC.gridwidth = 3;
		superhintGBC.weightx = superhintGBC.weighty = 1.0;
		superhintGBC.weighty = 1.0;
		
		//LABEL GBC
		GridBagConstraints labelGBC = new GridBagConstraints();
		labelGBC.insets = new Insets(0, 5, 5, 0);
		labelGBC.fill = GridBagConstraints.HORIZONTAL;   
		labelGBC.anchor = GridBagConstraints.NORTHWEST;
		labelGBC.gridx = 0;
		
		//DETAIL GBC
		GridBagConstraints detailGBC = new GridBagConstraints();
		detailGBC.insets = new Insets(0, 5, 5, 0);
		detailGBC.fill = GridBagConstraints.HORIZONTAL;  
		detailGBC.anchor = GridBagConstraints.NORTHWEST;
		detailGBC.gridx = 1;	
		
		//DETAIL GBC
		GridBagConstraints assetHintGBC = new GridBagConstraints();
		assetHintGBC.insets = new Insets(0, 5, 5, 0);
		assetHintGBC.fill = GridBagConstraints.HORIZONTAL;  
		assetHintGBC.anchor = GridBagConstraints.NORTHWEST;
		assetHintGBC.gridx = 2;	
		
		//LABEL FROM
		labelGBC.gridy = 0;
		JLabel fromLabel = new JLabel(Lang.getInstance().translate("Account") + ":");
		this.add(fromLabel, labelGBC);
		
		//COMBOBOX FROM
		detailGBC.gridy = 0;
		this.cbxAccount = new JComboBox<Account>(new AccountsComboBoxModel());
		this.cbxAccount.setRenderer(new AccountRenderer(this.have.getKey()));
        this.add(this.cbxAccount, detailGBC);
		
		//ASSET HINT
		assetHintGBC.gridy = detailGBC.gridy;
		JLabel accountHintLabel = new JLabel( have.getShort() );
		this.add(accountHintLabel, assetHintGBC);
        
		//LABEL PRICE
		labelGBC.gridy++;
		JLabel priceLabel = new JLabel(Lang.getInstance().translate("Price") + ":");
		this.add(priceLabel, labelGBC);
		
		//PRICE
		detailGBC.gridy++;
		txtPrice = new JTextField();
		this.add(txtPrice, detailGBC);	
		
		//ASSET HINT
		assetHintGBC.gridy = detailGBC.gridy;
		JLabel priceHintLabel = new JLabel( buying?have.getShort():want.getShort() );
		this.add(priceHintLabel, assetHintGBC);
				
		if(false & buying)
		{
			/*
			//LABEL BUYING PRICE
			labelGBC.gridy++;
			JLabel buyingPriceLabel = new JLabel(Lang.getInstance().translate("Buying price") + ":");
			this.add(buyingPriceLabel, labelGBC);
			
			//BUYING PRICE
			detailGBC.gridy++;
			txtBuyingPrice = new JTextField();
			txtBuyingPrice.setEnabled(false);
			this.add(txtBuyingPrice, detailGBC);
			
			//ASSET HINT
			assetHintGBC.gridy = detailGBC.gridy;
			JLabel buyingPriceHintLabel = new JLabel( have.getShort() );
			this.add(buyingPriceHintLabel, assetHintGBC);
			
			//ON PRICE CHANGE
			txtPrice.getDocument().addDocumentListener(new DocumentListener() 
			{
				public void changedUpdate(DocumentEvent e) 
				{
					calculateBuyingPrice(txtBuyingPrice);
				}
				
				public void removeUpdate(DocumentEvent e) 
				{
					calculateBuyingPrice(txtBuyingPrice);
				}
				  
				public void insertUpdate(DocumentEvent e) 
				{
					calculateBuyingPrice(txtBuyingPrice);
				}
			});
			*/
		}
		else
		{
			//ON PRICE CHANGE
			txtPrice.getDocument().addDocumentListener(new DocumentListener() 
			{
				public void changedUpdate(DocumentEvent e) 
				{
					calculateBuyingAmount(txtBuyingAmount, buying);
				}
				
				public void removeUpdate(DocumentEvent e) 
				{
					calculateBuyingAmount(txtBuyingAmount, buying);
				}
				  
				public void insertUpdate(DocumentEvent e) 
				{
					calculateBuyingAmount(txtBuyingAmount, buying);
				}
			});
		}
		
		//LABEL AMOUNT
		labelGBC.gridy++;
		JLabel amountLabel = new JLabel(Lang.getInstance().translate("Amount") + ":");
		this.add(amountLabel, labelGBC);
				
		//AMOUNT
		detailGBC.gridy++;
		this.txtAmount = new JTextField();
		this.add(this.txtAmount, detailGBC);	
		
		//ASSET HINT
		assetHintGBC.gridy = detailGBC.gridy;
		JLabel amountHintLabel = new JLabel( buying?want.getShort():have.getShort() );

		this.add(amountHintLabel, assetHintGBC);
		
		//LABEL AMOUNT
		labelGBC.gridy++;
		JLabel buyingAmountLabel = new JLabel(Lang.getInstance().translate("Total") + ":");
		this.add(buyingAmountLabel, labelGBC);
					
		//AMOUNT
		detailGBC.gridy++;
		txtBuyingAmount = new JTextField();
		txtBuyingAmount.setEnabled(false);
		this.add(txtBuyingAmount, detailGBC);
			
		//ASSET HINT
		assetHintGBC.gridy = detailGBC.gridy;
		JLabel buyingAmountHintLabel = new JLabel( buying?have.getShort():want.getShort() );
		this.add(buyingAmountHintLabel, assetHintGBC);
		
		//ON PRICE CHANGE
		txtAmount.getDocument().addDocumentListener(new DocumentListener() 
		{
			public void changedUpdate(DocumentEvent e) 
			{
				calculateBuyingAmount(txtBuyingAmount, buying);
			}
			
			public void removeUpdate(DocumentEvent e) 
			{
				calculateBuyingAmount(txtBuyingAmount, buying);
			}
				  
			public void insertUpdate(DocumentEvent e) 
			{
				calculateBuyingAmount(txtBuyingAmount, buying);
			}
		});	
		
		//LABEL FEE
		labelGBC.gridy++;
		JLabel feeLabel = new JLabel(Lang.getInstance().translate("Fee Power") + ":");
		this.add(feeLabel, labelGBC);
		           
		//FEE
		detailGBC.gridy++;
		txtFeePow = new JTextField("0");
		this.add(txtFeePow, detailGBC);		
		
		//ASSET HINT
		assetHintGBC.gridy = detailGBC.gridy;
		JLabel feeHintLabel = new JLabel( Controller.getInstance().getAsset(0).getShort());
		this.add(feeHintLabel, assetHintGBC);
		
		//ADD SELL BUTTON
		labelGBC.gridy++;
		labelGBC.gridwidth = 3;
		
		superHintText = new JTextPane();
		superHintText.setEditable(false);
		superHintText.setBackground(this.getBackground());
		superHintText.setContentType("text/html");
		
		superHintText.setFont(txtBuyingAmount.getFont());
		superHintText.setText( "<html><body style='font-size: 100%'>&nbsp;<br>&nbsp;<br></body></html>" );
		
		superHintText.setPreferredSize(new Dimension(125, 40));
		
		JPanel scrollPaneSuperHintText = new JPanel(new BorderLayout());
		
		scrollPaneSuperHintText.add(superHintText, BorderLayout.SOUTH);
		
		this.add(scrollPaneSuperHintText, superhintGBC);
		
		labelGBC.gridy++;
		
		if(buying)
			this.sellButton = new JButton(Lang.getInstance().translate("Buy"));	
		else
			this.sellButton = new JButton(Lang.getInstance().translate("Sell"));	
		
		this.sellButton.setPreferredSize(new Dimension(125, 25));
		this.sellButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				onSellClick(buying);
			}
		});	
		this.add(this.sellButton, labelGBC);
	}
	
	public void calculateHint() 
	{
		if(!isDigit(this.txtPrice.getText()))
			superHintText.setText( "<html><body style='font-size: 100%'>&nbsp;<br>" + Lang.getInstance().translate("Enter correct price.") + "</body></html>" );
		else if(!isDigit(this.txtAmount.getText()))
			superHintText.setText( "<html><body style='font-size: 100%'>&nbsp;<br>" + Lang.getInstance().translate("Enter correct amount.") + "</body></html>" );
		else
			superHintText.setText( "<html><body style='font-size: 100%'>" + 
					Lang.getInstance().translate("Give <b>%amount% %have%</b>" + 
					" at the price of <b>%price%&nbsp;%want%</b> per <b>1% %have%</b> that would get " + 
					"<b>%buyingamount%&nbsp;%want%</b>.")
					.replace("%amount%", this.txtAmount.getText())
					.replace("%have%", have.getShort())
					.replace("%price%", this.txtPrice.getText())
					.replace("%want%", want.getShort())
					.replace("%buyingamount%", this.txtBuyingAmount.getText())
					+"</body></html>");
	}
	
	private static boolean isDigit(String s) throws NumberFormatException {
	    try {
	        new BigDecimal(s);
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}
	
	/*
	public void calculateBuyingPrice(JTextField target, boolean buying) 
	{
	    try
	    {
	    	BigDecimal price = new BigDecimal(txtPrice.getText());		    	
	    	target.setText(BigDecimal.ONE.setScale(8).divide(price, RoundingMode.DOWN).toPlainString());	
	    }
	    catch(Exception e)
	    {
	    	target.setText("0");
	    }
	    
	    calculateBuyingAmount(txtBuyingAmount, buying);
	}
	*/
	
	public void calculateBuyingAmount(JTextField target, boolean buying) 
	{
	    try
	    {
	    	BigDecimal price = new BigDecimal(txtPrice.getText());		    	
	    	BigDecimal amount = new BigDecimal(txtAmount.getText());
	    	if (buying) {
	    		if (this.have.isDivisible()) {
	    	    	target.setText(price.multiply(amount).setScale(8, RoundingMode.HALF_UP).toPlainString());
	    		} else {
	    	    	target.setText(price.multiply(amount).setScale(3, RoundingMode.HALF_UP).toPlainString());
	    		}
	    	} else {
	    		if (this.want.isDivisible()) {
	    	    	target.setText(price.multiply(amount).setScale(8, RoundingMode.HALF_UP).toPlainString());
	    		} else {
	    	    	target.setText(price.multiply(amount).setScale(3, RoundingMode.HALF_UP).toPlainString());
	    		}
	    	}
	    }
	    catch(Exception e)
	    {
	    	target.setText("0");
	    }
	    
	    //calculateHint();
	}
	
	public void onSellClick(boolean buying)
	{
		//DISABLE
		this.sellButton.setEnabled(false);
	
		//CHECK IF NETWORK OK
		if(Controller.getInstance().getStatus() != Controller.STATUS_OK)
		{
			//NETWORK NOT OK
			JOptionPane.showMessageDialog(null, Lang.getInstance().translate("You are unable to send a transaction while synchronizing or while having no connections!"), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			
			//ENABLE
			this.sellButton.setEnabled(true);
			
			return;
		}
		
		//CHECK IF WALLET UNLOCKED
		if(!Controller.getInstance().isWalletUnlocked())
		{
			//ASK FOR PASSWORD
			String password = PasswordPane.showUnlockWalletDialog(); 
			if(!Controller.getInstance().unlockWallet(password))
			{
				//WRONG PASSWORD
				JOptionPane.showMessageDialog(null, Lang.getInstance().translate("Invalid password"), Lang.getInstance().translate("Unlock Wallet"), JOptionPane.ERROR_MESSAGE);
				
				//ENABLE
				this.sellButton.setEnabled(true);
				
				return;
			}
		}
		
		//READ CREATOR
		Account sender = (Account) this.cbxAccount.getSelectedItem();
		
		int feePow;
		BigDecimal amountHave;
		BigDecimal price;
		long parse = 0;
		try
		{
			//READ FEE
			feePow = Integer.parseInt(this.txtFeePow.getText());
			
			//READ AMOUNT
			parse = 1;
			amountHave = new BigDecimal(this.txtAmount.getText()).setScale(8);
			
			//READ PRICE
			parse = 2;
			price = new BigDecimal(this.txtPrice.getText()).setScale(8);			

		}
		catch(Exception e)
		{
			LOGGER.error(e.getMessage(),e);
			
			if(parse == 0)
			{
				JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid fee") + "!", Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			}
			if(parse == 1)
			{
				JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid amount") + "!", Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			}
			if(parse == 2)
			{
				JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Invalid price") + "!", Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			}
			this.sellButton.setEnabled(true);
			return;
		}

		//CREATE ORDER
		
		BigDecimal amountWant = amountHave.multiply(price);
		if (buying) {
			price = amountWant;
			amountWant = amountHave;
			amountHave = price;
		}
		
		if (false) {
			// for develop
			JOptionPane.showMessageDialog(new JFrame(),
					amountHave.toPlainString() + " - " + amountWant.toPlainString(),
					Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
			this.sellButton.setEnabled(true);
			return;
		}

		PrivateKeyAccount creator = Controller.getInstance().getPrivateKeyAccountByAddress(sender.getAddress());
		Pair<Transaction, Integer> result = Controller.getInstance().createOrder(creator, this.have, this.want, amountHave.setScale(8, RoundingMode.HALF_DOWN), amountWant.setScale(8, RoundingMode.HALF_DOWN), feePow);
		
		//CHECK VALIDATE MESSAGE
		if (result.getB() == Transaction.VALIDATE_OK) {
			
			JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate("Order has been sent") + "!", Lang.getInstance().translate("Success"), JOptionPane.INFORMATION_MESSAGE);
			
			this.txtFeePow.setText("0");
			this.txtAmount.setText("");
			this.txtPrice.setText("");
			
		} else {		
			JOptionPane.showMessageDialog(new JFrame(), Lang.getInstance().translate(OnDealClick.resultMess(result.getB())), Lang.getInstance().translate("Error"), JOptionPane.ERROR_MESSAGE);
		}
		
		//ENABLE
		this.sellButton.setEnabled(true);
	}
	
}
