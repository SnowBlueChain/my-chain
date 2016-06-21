package gui.items.assets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import lang.Lang;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import controller.Controller;
import core.item.ItemCls;
import core.item.assets.AssetCls;

@SuppressWarnings("serial")
public class AssetPairSelect extends JFrame{
	
	public AssetPairSelectTableModel assetPairSelectTableModel;

	public AssetPairSelect(long key, String action) {
		
		super(Lang.getInstance().translate("DATACHAINS.world") + " - " + Controller.getInstance().getAsset(key).toString() + " - " + Lang.getInstance().translate("Select pair"));
		
		//ICON
		List<Image> icons = new ArrayList<Image>();
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon64.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon128.png"));
		this.setIconImages(icons);
		
		//CLOSE
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		//LAYOUT
		this.setLayout(new GridBagLayout());
		
		//this.setSize(800, 600);
		
		//PADDING
		((JComponent) this.getContentPane()).setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		//Label GBC
				GridBagConstraints labelGBC = new GridBagConstraints();
				labelGBC.insets = new Insets(0, 5, 5, 0);
				labelGBC.fill = GridBagConstraints.BOTH;  
				labelGBC.anchor = GridBagConstraints.NORTHWEST;
				labelGBC.weightx = 1;	
				labelGBC.weighty = 1;	
				labelGBC.gridwidth = 2;
				labelGBC.gridx = 0;	
				labelGBC.gridy = 0;	
		
				JLabel label = new JLabel("Выберите пару");
				
				if (action == "Buy") label.setText("Укажите актив на который хотите купить " +  Controller.getInstance().getAsset(key).toString() );
				if (action == "To sell") label.setText("Укажите актив за который хотите продать " +  Controller.getInstance().getAsset(key).toString());
		//TABLE GBC
		GridBagConstraints tableGBC = new GridBagConstraints();
		tableGBC.insets = new Insets(0, 5, 5, 0);
		tableGBC.fill = GridBagConstraints.BOTH;  
		tableGBC.anchor = GridBagConstraints.NORTHWEST;
		tableGBC.weightx = 1;	
		tableGBC.weighty = 1;	
		tableGBC.gridwidth = 2;
		tableGBC.gridx = 0;	
		tableGBC.gridy = 1;	
		
		assetPairSelectTableModel = new AssetPairSelectTableModel(key, action);
				
		final JTable assetsPairTable = new JTable(assetPairSelectTableModel);
		
		assetsPairTable.setIntercellSpacing(new java.awt.Dimension(2, 2));

		assetsPairTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	    for (int i = 0; i < assetsPairTable.getColumnCount(); i++) {
	      DefaultTableColumnModel colModel = (DefaultTableColumnModel) assetsPairTable.getColumnModel();
	      TableColumn col = colModel.getColumn(i);
	      int width = 0;

	      TableCellRenderer renderer = col.getHeaderRenderer();
	      for (int r = 0; r < assetsPairTable.getRowCount(); r++) {
	        renderer = assetsPairTable.getCellRenderer(r, i);
	        Component comp = renderer.getTableCellRendererComponent(assetsPairTable, assetsPairTable.getValueAt(r, i),
	            false, false, r, i);
	        width = Math.max(width, comp.getPreferredSize().width);
	      }
	      col.setPreferredWidth(width + 2);
	    }

	    for (int row = 0; row < assetsPairTable.getRowCount(); row++)
	    {
	        int rowHeight = assetsPairTable.getRowHeight();

	        for (int column = 0; column < assetsPairTable.getColumnCount(); column++)
	        {
	            Component comp = assetsPairTable.prepareRenderer(assetsPairTable.getCellRenderer(row, column), row, column);
	            rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
	        }

	        assetsPairTable.setRowHeight(row, rowHeight);
	    }

	    assetsPairTable.getTableHeader().setPreferredSize(new Dimension(10, (int)(assetsPairTable.getTableHeader().getPreferredSize().getHeight()+6)));

	    assetsPairTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
	    
	    assetsPairTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					JTable target = (JTable)e.getSource();
					int row = target.getSelectedRow();

					if(row < assetPairSelectTableModel.assets.size())
					{
						new ExchangeFrame(
								(AssetCls)Controller.getInstance().getItem(ItemCls.ASSET_TYPE, assetPairSelectTableModel.key), 
								(AssetCls) assetPairSelectTableModel.assets.get(row), action);
						((JFrame) (assetsPairTable.getTopLevelAncestor())).dispose();
					}
				}
			}
		});
	    
		this.add(new JScrollPane(assetsPairTable), tableGBC);
		this.add(label, labelGBC);
		
		//PACK
		this.pack();
		this.setSize(800, this.getHeight());
		this.setResizable(true);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

}
