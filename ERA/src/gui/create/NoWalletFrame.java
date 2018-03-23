package gui.create;

import gui.Gui;
import lang.Lang;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import controller.Controller;
import core.item.templates.TemplateCls;
import datachain.DCSet;

@SuppressWarnings("serial")
public class NoWalletFrame extends JFrame {
	
	private JRadioButton createButton;
	private JRadioButton recoverButton;
	private Gui parent;
	private NoWalletFrame th;
	
	public NoWalletFrame(Gui parent) throws Exception
	{
		super(Lang.getInstance().translate("Erachain.org") + " - " + Lang.getInstance().translate("No Wallet"));
		
		th = this;
		//ICON
		List<Image> icons = new ArrayList<Image>();
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon64.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon128.png"));
		this.setIconImages(icons);
		
		//PARENT
		this.parent = parent;
		
		//LAYOUT
		this.setLayout(new GridBagLayout());
		
		//PADDING
		((JComponent) this.getContentPane()).setBorder(new EmptyBorder(5, 5, 5, 5));
		
		//LABEL GBC
		GridBagConstraints labelGBC = new GridBagConstraints();
		labelGBC.insets = new Insets(5,5,5,5);
		labelGBC.fill = GridBagConstraints.HORIZONTAL;   
		labelGBC.anchor = GridBagConstraints.NORTHWEST;
		labelGBC.weightx = 1;	
		labelGBC.gridwidth = 2;
		labelGBC.gridx = 0;
		
		//OPTIONS GBC
		GridBagConstraints optionsGBC = new GridBagConstraints();
		optionsGBC.insets = new Insets(5,5,5,5);
		optionsGBC.fill = GridBagConstraints.NONE;  
		optionsGBC.anchor = GridBagConstraints.NORTHWEST;
		optionsGBC.weightx = 1;	
		optionsGBC.gridwidth = 2;
		optionsGBC.gridx = 0;	
		optionsGBC.gridy = 2;	
		
		//BUTTON GBC
		GridBagConstraints buttonGBC = new GridBagConstraints();
		buttonGBC.insets = new Insets(5,5,0,5);
		buttonGBC.fill = GridBagConstraints.NONE;  
		buttonGBC.anchor = GridBagConstraints.NORTHWEST;
		buttonGBC.gridwidth = 1;
		buttonGBC.gridx = 0;		
		
		//LABEL
		labelGBC.gridy = 0;
		JLabel label1 = new JLabel(Lang.getInstance().translate("No existing wallet was found."));
		this.add(label1, labelGBC);
		
		//LABEL
      	labelGBC.gridy = 1;
      	JLabel label2 = new JLabel(Lang.getInstance().translate("What would you like to do?"));
      	this.add(label2, labelGBC);
        
      	//ADD OPTIONS
      //	this.createButton = new JRadioButton(Lang.getInstance().translate("Create a new wallet."));
      //	this.createButton.setSelected(true);
      //	this.add(this.createButton, optionsGBC);
      	// CREATE WALLET LABEL
      	JLabel create_Label = new JLabel("<HTML><a href =''>"+ Lang.getInstance().translate("Create a new wallet.") +" </a>");
      	create_Label.setCursor(new Cursor(Cursor.HAND_CURSOR));
      	this.add(create_Label, optionsGBC);
      	create_Label.addMouseListener(new MouseListener(){

    		@Override
    		public void mouseClicked(MouseEvent arg0) {
    			// TODO Auto-generated method stub
    			
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
    			
    			//OPEN CREATE WALLET FRAME
    			TemplateCls template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(Controller.LICENSE_KEY);
    	        if (template == null) {
    	        	// USE default LICENSE
    		        template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(2l);
    	        }

    	       
    				//OPEN CREATE WALLET FRAME
    				th.setVisible(false);
    	        	new License_JFrame(template, true, th, true);
    			

    	             		
    		}
    		
    		
    	});
      	
      	
      	optionsGBC.gridy = 3;
      //	this.recoverButton = new JRadioButton(Lang.getInstance().translate("Recover a wallet using an existing seed"));
     // 	this.add(this.recoverButton, optionsGBC);
     // 		
     // 	ButtonGroup group = new ButtonGroup();
     // 	group.add(this.createButton);
     // 	group.add(this.recoverButton);
      	
    	// CREATE WALLET LABEL
      	JLabel recover_Label = new JLabel("<HTML><a href =''>"+ Lang.getInstance().translate("Recover a wallet using an existing seed") +" </a>");
      	recover_Label.setCursor(new Cursor(Cursor.HAND_CURSOR));
      	this.add(recover_Label, optionsGBC);
      	recover_Label.addMouseListener(new MouseListener(){

    		@Override
    		public void mouseClicked(MouseEvent arg0) {
    			// TODO Auto-generated method stub
    			
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
    			
    			//OPEN CREATE WALLET FRAME
    			TemplateCls template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(Controller.LICENSE_KEY);
    	        if (template == null) {
    	        	// USE default LICENSE
    		        template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(2l);
    	        }

    	       
    				//OPEN CREATE WALLET FRAME
    				th.setVisible(false);
    	        	new License_JFrame(template, true, th, false);
    			

    	             		
    		}
    		
    		
    	});
      	
      	
      	
      	
      	
      	 //BUTTON NEXT
        buttonGBC.gridy = 4;
        buttonGBC.gridx = 1;
        JButton nextButton = new JButton(Lang.getInstance().translate("Next"));
        nextButton.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		        onNextClick();
		    }
		});	
 //       nextButton.setPreferredSize(new Dimension(80, 25));
 //   	this.add(nextButton, buttonGBC);
    	
    	//BUTTON CANCEL
    	buttonGBC.gridx = 0;
    	buttonGBC.gridy = 4;
        JButton cancelButton = new JButton(Lang.getInstance().translate("Cancel"));
        cancelButton.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		        onCancelClick();
		    }
		});
 //       cancelButton.setPreferredSize(new Dimension(80, 25));
 //   	this.add(cancelButton, buttonGBC);
        
    	//CLOSE NICELY
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
            	Controller.getInstance().stopAll(0);
          //  	System.exit(0);
            }
        });
    	
        //CALCULATE HEIGHT WIDTH
      	this.pack();
  //    	this.setSize(500, this.getHeight());
      	
      	this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	public void goAfterLicence(boolean createWallet)
	{
		if (createWallet)
			new CreateWalletFrame(this);
		else
			new RecoverWalletFrame(this);
	}

	public void onNextClick()
	{
        
		TemplateCls template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(Controller.LICENSE_KEY);
        if (template == null) {
        	// USE default LICENSE
	        template = (TemplateCls)DCSet.getInstance().getItemTemplateMap().get(2l);
        }

        if(createButton.isSelected())
		{
			//OPEN CREATE WALLET FRAME
			this.setVisible(false);
        	new License_JFrame(template, true, this, true);
		}
		
		if(recoverButton.isSelected())
		{
			//OPEN RECOVER WALLET FRAME
			this.setVisible(false);
			new License_JFrame(template, true, this, false);	
		}
	}
	
	public void onCancelClick()
	{
		this.parent.onCancelCreateWallet();
		
		this.dispose();
	}
	
	public void onWalletCreated()
	{
		Controller.getInstance().forgingStatusChanged(Controller.getInstance().getForgingStatus());
		
		this.parent.onWalletCreated();
		
		this.dispose();
	}
}
