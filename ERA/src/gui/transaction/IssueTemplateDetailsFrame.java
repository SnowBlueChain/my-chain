package gui.transaction;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.github.rjeschke.txtmark.Processor;

import core.crypto.Base58;
import core.item.templates.TemplateCls;
import core.transaction.IssueTemplateRecord;
import gui.library.library;
import lang.Lang;
import utils.DateTimeFormat;
import utils.MenuPopupUtil;

@SuppressWarnings("serial")
public class IssueTemplateDetailsFrame extends Rec_DetailsFrame
{
	public IssueTemplateDetailsFrame(IssueTemplateRecord templateIssue)
	{
		super(templateIssue);
				
		//LABEL NAME
		++labelGBC.gridy;
		JLabel nameLabel = new JLabel(Lang.getInstance().translate("Name") + ":");
		this.add(nameLabel, labelGBC);
		
		//NAME
		++detailGBC.gridy;
		JTextField name = new JTextField(templateIssue.getItem().getName());
		name.setEditable(false);
		MenuPopupUtil.installContextMenu(name);
		this.add(name, detailGBC);		
		
		//LABEL DESCRIPTION
		++labelGBC.gridy;
		JLabel descriptionLabel = new JLabel(Lang.getInstance().translate("Description") + ":");
		this.add(descriptionLabel, labelGBC);
				
		//DESCRIPTION
		++detailGBC.gridy;
		String txt = "<HTML>"+ library.to_HTML(templateIssue.getItem().getDescription());
		JLabel txtAreaDescription = new JLabel(txt);
		//txtAreaDescription.setRows(4);
		txtAreaDescription.setBorder(name.getBorder());
		//txtAreaDescription.setEditable(false);
		//MenuPopupUtil.installContextMenu(txtAreaDescription);
		this.add(txtAreaDescription, detailGBC);		
						           
        //PACK
//		this.pack();
  //      this.setResizable(false);
  //      this.setLocationRelativeTo(null);
        this.setVisible(true);
        
	}
}
