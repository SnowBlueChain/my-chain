package org.erachain.gui.transaction;

import org.erachain.core.transaction.IssueTemplateRecord;
import org.erachain.gui.library.Library;
import org.erachain.lang.Lang;
import org.erachain.utils.MenuPopupUtil;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class IssueTemplateDetailsFrame extends RecDetailsFrame {
    public IssueTemplateDetailsFrame(IssueTemplateRecord templateIssue) {
        super(templateIssue);

        //LABEL NAME
        ++labelGBC.gridy;
        JLabel nameLabel = new JLabel(Lang.getInstance().translate("Name") + ":");
        this.add(nameLabel, labelGBC);

        //NAME
        ++detailGBC.gridy;
        JTextField name = new JTextField(templateIssue.getItem().viewName());
        name.setEditable(false);
        MenuPopupUtil.installContextMenu(name);
        this.add(name, detailGBC);

        //LABEL DESCRIPTION
        ++labelGBC.gridy;
        JLabel descriptionLabel = new JLabel(Lang.getInstance().translate("Description") + ":");
        this.add(descriptionLabel, labelGBC);

        //DESCRIPTION
        ++detailGBC.gridy;
        String txt = "<html>" + Library.to_HTML(templateIssue.getItem().getDescription());

        JLabel txtAreaDescription = new JLabel(txt);
        JScrollPane scroller = new JScrollPane(txtAreaDescription, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(0,400));
        //txtAreaDescription.setRows(4);
        txtAreaDescription.setBorder(name.getBorder());
        //txtAreaDescription.setEditable(false);
        //MenuPopupUtil.installContextMenu(txtAreaDescription);
        this.add(scroller, detailGBC);


        //PACK
//		this.pack();
        //      this.setResizable(false);
        //      this.setLocationRelativeTo(null);
        this.setVisible(true);

    }
}
