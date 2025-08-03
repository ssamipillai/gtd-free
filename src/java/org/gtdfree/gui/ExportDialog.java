/*
 *    Copyright (C) 2008-2010 Igor Kriznar
 *    
 *    This file is part of GTD-Free.
 *    
 *    GTD-Free is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *    
 *    GTD-Free is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    You should have received a copy of the GNU General Public License
 *    along with GTD-Free.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gtdfree.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.addons.ExportAddOn;
import org.gtdfree.addons.PDFExportAddOn;
import org.gtdfree.addons.ExportAddOn.ExportOrder;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.GTDModel;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPrintPage;

/**
 * @author ikesan
 *
 */
public class ExportDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;

	private static final String EXPORT_FORMAT = "ExportDialog.format"; //$NON-NLS-1$
	private static final String EXPORT_ORDER = "ExportDialog.order"; //$NON-NLS-1$

	private SelectionPanel selectionPanel;
	private GTDModel gtdModel;
	private JComboBox formatCombo;
	private GTDFreeEngine engine;
	private ExportAddOn export;
	private JComboBox orderCombo;
	private JFileChooser fileChooser;
	private JCheckBox overwriteCheckBox;
	private JTabbedPane tabs;
	private ButtonGroup selectionButtonGroup;
	private JRadioButton currentViewRadioButton;
	private JRadioButton customRadioButton;
	
	private boolean printing=false;

	private PDFExportAddOn pdfExport;

	private JPanel optionsTab;

	//private HtmlPanel preview;

	public ExportDialog(Frame owner) {
		super(owner,true);
		initialize();
	}

	public ExportDialog(Frame owner, boolean printing) {
		super(owner,true);
		this.printing=printing;
		initialize();
	}

	private void initialize() {
		
		tabs= new JTabbedPane();
		JPanel p;
		int row=0;
		/*
		 * Create general tab
		 */

		p= new JPanel();
		p.setLayout(new GridBagLayout());
		row=0;
		
		JLabel l= new JLabel(Messages.getString("ExportDialog.Sel")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		//l.setBorder(SelectionPanel.createCategoryBorder(getForeground()));
		p.add(l, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(SelectionPanel.BORDER,SelectionPanel.BORDER,2,SelectionPanel.BORDER),0,0));

		selectionButtonGroup= new ButtonGroup();
		
		currentViewRadioButton= new JRadioButton(Messages.getString("ExportDialog.Curr")); //$NON-NLS-1$
		currentViewRadioButton.setSelected(true);
		selectionButtonGroup.add(currentViewRadioButton);
		currentViewRadioButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				tabs.setEnabledAt(1, false);
			}
		});
		p.add(currentViewRadioButton, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,SelectionPanel.INDENT,2,SelectionPanel.BORDER),0,0));
		
		customRadioButton= new JRadioButton(Messages.getString("ExportDialog.Custom")); //$NON-NLS-1$
		selectionButtonGroup.add(customRadioButton);
		customRadioButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				tabs.setEnabledAt(1, true);
			}
		});
		p.add(customRadioButton, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,SelectionPanel.INDENT,2,SelectionPanel.BORDER),0,0));


		JPanel p1= new JPanel();
		p1.setLayout(new GridBagLayout());

		int rrow=-1;
		
		if (!printing) {
			l= new JLabel(Messages.getString("ExportDialog.Format")); //$NON-NLS-1$
			p1.add(l, new GridBagConstraints(0,++rrow,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,4),0,0));

			formatCombo= new JComboBox();
			formatCombo.setEditable(false);
			formatCombo.setRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					if (value instanceof ExportAddOn) {
					value= ((ExportAddOn)value).getName();
					}
					return super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				}
			});
			formatCombo.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					setExport((ExportAddOn)formatCombo.getSelectedItem());
				}
			});
			p1.add(formatCombo, new GridBagConstraints(1,rrow,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
		
		}
		
		l= new JLabel(Messages.getString("ExportDialog.Order")); //$NON-NLS-1$
		p1.add(l, new GridBagConstraints(0,++rrow,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,4),0,0));

		orderCombo= new JComboBox();
		orderCombo.setEditable(false);
		orderCombo.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				if (value instanceof ExportAddOn.ExportOrder) {
					value= ((ExportAddOn.ExportOrder)value).getDisplayName();
				}
				
				return super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
			}
		});
		orderCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (engine!=null && orderCombo!=null && orderCombo.getSelectedItem()!=null) {
					engine.getGlobalProperties().putProperty(EXPORT_ORDER, orderCombo.getSelectedItem().toString());
				}
			}
		});
		p1.add(orderCombo, new GridBagConstraints(1,rrow,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
		p.add(p1, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(11,11,0,11),0,0));


		if (!printing) {
			overwriteCheckBox= new JCheckBox();
			overwriteCheckBox.setText(Messages.getString("ExportDialog.Overwrite")); //$NON-NLS-1$
			
			p.add(overwriteCheckBox, new GridBagConstraints(0,++row,2,1,1,1,GridBagConstraints.NORTH,GridBagConstraints.HORIZONTAL,new Insets(11,11,11,11),0,0));
		} else {
			p.add(new JPanel(), new GridBagConstraints(0,++row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(11,11,11,11),0,0));
		}
		
		tabs.add(Messages.getString("ExportDialog.General"),p); //$NON-NLS-1$
		

		/*
		 * Create main tab pane 
		 */
		
		p= new JPanel();
		p.setLayout(new GridBagLayout());
		row=0;
		
		selectionPanel= new SelectionPanel();
		p.add(selectionPanel, new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
		tabs.addTab(Messages.getString("ExportDialog.Selection"), p); //$NON-NLS-1$
		tabs.setEnabledAt(1, false);

		
		/*
		 * Create options tab pane 
		 */
		
		optionsTab= new JPanel();
		optionsTab.setLayout(new BorderLayout());
		
		tabs.addTab(Messages.getString("ExportDialog.Options"), optionsTab); //$NON-NLS-1$
		tabs.setEnabledAt(2, false);

		/*
		 * Create preview tab
		 */
		
		//preview = new XHTMLPanel();
		//preview= new HtmlPanel();
		
		//preview.setBackground(Color.WHITE);
		//JScrollPane sp= new JScrollPane(preview);
		//p.add(sp, new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4),0,0));
		//tabs.addTab("Preview", sp);
		//tabs.addChangeListener(new ChangeListener() {
		//	@Override
		//	public void stateChanged(ChangeEvent e) {
		//		if (tabs.getSelectedIndex()==1) {
		//			updatePreview();
		//		}
		//	}
		//});
		
		
		/*
		 * Create bottom part common to all tabs
		 */
		
		p= new JPanel();
		p.setLayout(new GridBagLayout());
		row=0;
		
		p.add(tabs, new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));

		JButton b=null;
		
		if (printing) {
			b= new JButton(Messages.getString("ExportDialog.Print")); //$NON-NLS-1$
			b.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					print();
				}
			});
			p.add(b, new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(11,11,11,2),0,0));
		} else {
			b= new JButton(Messages.getString("ExportDialog.Export")); //$NON-NLS-1$
			b.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					export();
				}
			});
			p.add(b, new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(11,11,11,2),0,0));
		}
		
		b= new JButton(Messages.getString("ExportDialog.Close")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(b, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(11,2,11,11),0,0));

		
		setContentPane(p);
		
		setModalityType(DEFAULT_MODALITY_TYPE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		if (printing) {
			setTitle(Messages.getString("ExportDialog.0")+Messages.getString("ExportDialog.Print.title")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			setTitle(Messages.getString("ExportDialog.2")+Messages.getString("ExportDialog.Export.title")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		pack();
		setSize(getSize().width+50, getSize().height);
		
		if (printing) {
			pdfExport= new PDFExportAddOn();
			pdfExport.setPrintMode(true);
			setExport(pdfExport);
		}
	}
	
	@Override
	public void setVisible(boolean b) {
		
		boolean current=engine.getActiveWorkflowPane()!=null && engine.getActiveWorkflowPane().getActionsInView().iterator(ExportOrder.Actions).hasNext();
		if (!current) {
			currentViewRadioButton.setSelected(current);
			customRadioButton.setSelected(!current);
		}
		currentViewRadioButton.setEnabled(current);
		if (tabs.getSelectedIndex()==1&&current) {
			tabs.setSelectedIndex(0);
		}

		
		super.setVisible(b);
	}

	protected void print() {
		try {
			
			dispose();

			final PrinterJob pj= PrinterJob.getPrinterJob();
			
			PageFormat pf= pj.defaultPage();
			
			HashPrintRequestAttributeSet set= new HashPrintRequestAttributeSet();
			
			Integer pn= engine.getGlobalProperties().getInteger(GlobalProperties.PAGE_SIZE_NAME);

			if (pn!=null) {
				Field[] f= MediaSizeName.class.getFields();
				for (int i = 0; i < f.length; i++) {
					if (Modifier.isStatic(f[i].getModifiers()) && f[i].getType() == MediaSizeName.class && ((MediaSizeName)f[i].get(null)).getValue()==pn) {
						MediaSizeName n= (MediaSizeName)f[i].get(null);
						set.add(n);
						break;
					}
				}
			}
			
			pn= engine.getGlobalProperties().getInteger(GlobalProperties.PAGE_ORIENTATION);
			if (pn!=null) {
				Field[] f= OrientationRequested.class.getFields();
				for (int i = 0; i < f.length; i++) {
					if (Modifier.isStatic(f[i].getModifiers()) && f[i].getType() == OrientationRequested.class && ((OrientationRequested)f[i].get(null)).getValue()==pn) {
						OrientationRequested n= (OrientationRequested)f[i].get(null);
						set.add(n);
						break;
					}
				}
			}

			double [] area= engine.getGlobalProperties().getDoubleArray(GlobalProperties.PAGE_PRINTABLE_AREA);
			if (area!=null && area.length==4) {
				MediaPrintableArea a= new MediaPrintableArea((float)area[0], (float)area[1], (float)area[2], (float)area[3], MediaPrintableArea.MM);
				set.add(a);
			}

			if (!pj.printDialog(set)) {
				return;
			}

			pf= pj.getPageFormat(set);
			
			/*Attribute[] a= set.toArray();
			for (int i = 0; i < a.length; i++) {
				System.out.println(i+" "+a[i].getName()+" "+a[i].getCategory()+" "+a[i].toString());
			}*/
			
			ByteArrayOutputStream out= new ByteArrayOutputStream();
			ActionsCollection ac= 
				currentViewRadioButton.isSelected()
				? engine.getActiveWorkflowPane().getActionsInView()
						: selectionPanel.getSelectionModel().getSelection();
			
			Media media= (Media)set.get(Media.class);
			if (media instanceof MediaSizeName) {
				MediaSizeName msn= (MediaSizeName)media;
				//System.out.println("Paper "+msn.toString()+" "+msn.getValue());
				engine.getGlobalProperties().putProperty(GlobalProperties.PAGE_SIZE_NAME,msn.getValue());
			} else {
				MediaSize ms= (MediaSize)set.get(MediaSize.class);
				if (ms!=null) {
					//System.out.println("Paper size "+ms.toString()+" "+ms.getMediaSizeName().getValue());
					engine.getGlobalProperties().putProperty(GlobalProperties.PAGE_SIZE_NAME,ms.getMediaSizeName().getValue());
				}
			}
			
			OrientationRequested ori= (OrientationRequested)set.get(OrientationRequested.class);
			if (ori!=null) {
				engine.getGlobalProperties().putProperty(GlobalProperties.PAGE_ORIENTATION,ori.getValue());
			}
				
			MediaPrintableArea mpa= (MediaPrintableArea)set.get(MediaPrintableArea.class);
			if (mpa!=null) {
				float[] f= mpa.getPrintableArea(MediaPrintableArea.MM);
				engine.getGlobalProperties().putProperty(GlobalProperties.PAGE_PRINTABLE_AREA,new double[]{f[0],f[1],f[2],f[3]});
			}

			//System.out.println("Print page w:"+pf.getWidth()+" h:"+pf.getHeight()+" ix:"+pf.getImageableX()+" iy:"+pf.getImageableY()+" iw:"+pf.getImageableWidth()+" ih:"+pf.getImageableHeight()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
			
			
			
			//engine.getGlobalProperties().putProperty("Page", new double[]{pf.getOrientation(),pf.getWidth(),pf.getHeight(),pf.getImageableX(),pf.getImageableY(),pf.getImageableWidth(),pf.getImageableHeight()});
				
			pdfExport.setPageSize((float)pf.getWidth(), (float)pf.getHeight());
			pdfExport.setMargins(
					(float)pf.getImageableX(), 
					(float)(pf.getWidth()-pf.getImageableX()-pf.getImageableWidth()), 
					(float)(pf.getImageableY()), 
					(float)(pf.getHeight()-pf.getImageableY()-pf.getImageableHeight()));
			pdfExport.export(getGtdModel(), ac, out, (ExportAddOn.ExportOrder)orderCombo.getSelectedItem(), export.getFileFilters()[0], true);

			PDFFile f= new PDFFile(ByteBuffer.wrap(out.toByteArray()));
			final PDFPrintPage p= new PDFPrintPage(f);
			
			Paper pap= new Paper();
			pap.setSize(pf.getWidth(), pf.getHeight());
			pap.setImageableArea(0, 0, pf.getWidth(), pf.getHeight());
			pf.setPaper(pap);
			
			pj.setPrintable(p,pf);
			p.show(pj,this);
			
			new Thread("PrintingThread") { //$NON-NLS-1$
				@Override
				public void run() {
					try {
						pj.print();
					} catch (PrinterException e) {
						dispose();
						e.printStackTrace();
						JOptionPane.showMessageDialog(ExportDialog.this, Messages.getString("ExportDialog.Print.Fail")+" "+e.toString(), Messages.getString("ExportDialog.Print.Tail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					p.hide();
				};
			}.start();
			

		} catch (Exception e) {
			dispose();
			org.apache.log4j.Logger.getLogger(this.getClass()).error("Print error.", e); //$NON-NLS-1$
			JOptionPane.showMessageDialog(this, Messages.getString("ExportDialog.Print.Fail")+" "+e.toString(), Messages.getString("ExportDialog.Print.Tail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
	}

	protected void export() {
		if (export==null) {
			return;
		}
		
		FileFilter[] ff= export.getFileFilters();
		
		getFileChooser().resetChoosableFileFilters();
		
		if (ff.length==1) {
			getFileChooser().setFileFilter(ff[0]);
		} else {
			for (FileFilter f : ff) {
				getFileChooser().addChoosableFileFilter(f);
			}
		}
		
		getFileChooser().rescanCurrentDirectory();

		if (JFileChooser.APPROVE_OPTION==getFileChooser().showSaveDialog(this)) {
			
			File f= getFileChooser().getSelectedFile();
			
			FileFilter fff= getFileChooser().getFileFilter();
			if (f.getName().indexOf('.')<0 && fff instanceof FileNameExtensionFilter) {
				f= new File(f.toString()+'.'+((FileNameExtensionFilter)fff).getExtensions()[0]);
			}
			
			if (f.exists() && !overwriteCheckBox.isSelected()) {
				int i= JOptionPane.showConfirmDialog(this, Messages.getString("ExportDialog.OWR.1")+f.toString()+Messages.getString("ExportDialog.OWR.2"), Messages.getString("ExportDialog.OWR.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (i!=JOptionPane.OK_OPTION) {
					return;
				}
			}
			
			OutputStream out=null;
			try {
				
				Logger.getLogger(this.getClass()).info("Exporting with '"+export.getName()+"' to "+f.toString()); //$NON-NLS-1$ //$NON-NLS-2$

				out = new BufferedOutputStream(new FileOutputStream(f));
				
				ActionsCollection ac= 
					currentViewRadioButton.isSelected()
					? engine.getActiveWorkflowPane().getActionsInView()
							: selectionPanel.getSelectionModel().getSelection();
					
				export.export(getGtdModel(), ac, out, (ExportAddOn.ExportOrder)orderCombo.getSelectedItem(), fff, true);
				
				dispose();
				
				showSuccessDialog(f);
				
			} catch (Exception e) {
				dispose();
				org.apache.log4j.Logger.getLogger(this.getClass()).error("Export error.", e); //$NON-NLS-1$
				JOptionPane.showMessageDialog(this, Messages.getString("ExportDialog.Exp.Fail.1")+f+Messages.getString("ExportDialog.exp.Fail.2")+" "+e.toString(), Messages.getString("ExportDialog.Exp.Fail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			} finally {
				if (out!=null) {
					try {
						out.flush();
						out.close();
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
				}
			}
		}
	}
	
	protected void updatePreview() {
		if (export==null) {
			return;
		}
		
		new ByteArrayOutputStream();
		try {
			//export.export(getGtdModel(), selectionPanel.getSelectionModel().getSelection(), out, (ActionsCollection.ExportOrder)orderCombo.getSelectedItem());

			//ByteArrayInputStream in= new ByteArrayInputStream(out.toByteArray());
			
			//preview.setDocument(preview.getEditorKit().createDefaultDocument());
			//preview.setText(out.toString("UTF-8"));
			//preview.setDocument(in, ".");
			//preview.setCurrentDocumentContent(out.toString("UTF-8"));
			//preview.setHtml(out.toString("UTF-8"), ".", new SimpleHtmlRendererContext(preview, new SimpleUserAgentContext()));
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
		}
	}

	private void showSuccessDialog(File f) {
		int i= JOptionPane.showOptionDialog(this.getOwner(), Messages.getString("ExportDialog.Exp.OK.1")+f.toString()+Messages.getString("ExportDialog.Exp.OK.2"), Messages.getString("ExportDialog.Exp.OK.title"), JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{Messages.getString("ExportDialog.OK"),Messages.getString("ExportDialog.Open")}, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (i==1) {
			try {
				Desktop.getDesktop().open(f);
			} catch (IOException e1) {
				Logger.getLogger(this.getClass()).error("File open error.", e1); //$NON-NLS-1$
			}
		}
	}

	private JFileChooser getFileChooser() {
		if (fileChooser==null) {
			fileChooser= new JFileChooser();
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home", "."))); //$NON-NLS-1$ //$NON-NLS-2$
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileHidingEnabled(false);
			fileChooser.setAcceptAllFileFilterUsed(false);
		}
		return fileChooser;
	}


	protected void setExport(ExportAddOn selectedItem) {
		export= selectedItem;
		ExportAddOn.ExportOrder[] o= export.getSupportedExportOrders();
		
		orderCombo.removeAllItems();
		
		for (ExportAddOn.ExportOrder e : o) {
			orderCombo.addItem(e);
		}
		
		fileChooser=null;
		
		orderCombo.setSelectedItem(export.getDefaultExportOrder());
		
		if (export.getComponent()!=null) {
			optionsTab.add(export.getComponent());
			tabs.setEnabledAt(2, true);
		} else {
			optionsTab.removeAll();
			tabs.setEnabledAt(2, false);
		}
		
		if (engine!=null) {
			engine.getGlobalProperties().putProperty(EXPORT_FORMAT, export.getName());
		}
	}


	/**
	 * @return the gtdModel
	 */
	public GTDModel getGtdModel() {
		return gtdModel;
	}

	/**
	 * @param gtdModel the gtdModel to set
	 */
	private void setGtdModel(GTDModel gtdModel) {
		this.gtdModel = gtdModel;
		selectionPanel.setGtdModel(gtdModel);
	}
	
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		setGtdModel(engine.getGTDModel());
		
		if (printing) {
			pdfExport.initialize(engine);
		} else {
			ExportAddOn[] e= engine.getExportAddOns();
			for (int i = 0; i < e.length; i++) {
				e[i].initialize(engine);
			}
			DefaultComboBoxModel m= new DefaultComboBoxModel(e);
			formatCombo.setModel(m);
			
			Object o= engine.getGlobalProperties().getProperty(EXPORT_FORMAT);
			if (o!=null) {
				for (ExportAddOn ex : e) {
					if (ex.getName().equals(o)) {
						formatCombo.setSelectedItem(ex);
						break;
					}
				}
			}
			setExport((ExportAddOn)formatCombo.getSelectedItem());
		}
		
		Object o= engine.getGlobalProperties().getProperty(EXPORT_ORDER);
		if (o!=null) {
			ExportOrder eo= ExportOrder.valueOf(o.toString());
			if (eo!=null) {
				orderCombo.setSelectedItem(eo);
			}
		}

	}
	
}
