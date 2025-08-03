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

package org.gtdfree.addons;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.model.Action;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Priority;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.FontSelector;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author ikesan
 *
 */
public class PDFExportAddOn implements ExportAddOn {
	
	static class FontModel {
		
		private static String[] availableFontNames= new String[]{};
		
		private String[] selectedFontNames;
		private List<BaseFont> fonts;
		private boolean embedFonts=true;
		private String encoding= BaseFont.IDENTITY_H;
		public BaseFont baseFont;

		public FontModel() {
		}
		
		public void rebuildFonts() throws DocumentException, IOException {
			fonts= new  ArrayList<BaseFont>(3);
			
			if (selectedFontNames!=null && selectedFontNames.length>0) {
				
				for (int i = 0; i < selectedFontNames.length; i++) {
					fonts.add( FontFactory.getFont(selectedFontNames[i],encoding,embedFonts,12,Font.NORMAL, Color.BLACK, true).getBaseFont());
				}
			}

			fonts.add(getBaseFont());
			fonts.add(BaseFont.createFont(BaseFont.HELVETICA,"UTF-8",false));

			/*System.out.print("Fonts rebuilt:"+" ");
			for (BaseFont f : fonts) {
				System.out.print(" ");
				System.out.print(f.getPostscriptFontName());
			}
			System.out.println();*/
		}
		
		public List<BaseFont> getFonts() throws DocumentException, IOException {
			if (fonts==null) {
				rebuildFonts();
			}
			return fonts;
		}
		
		public String[] getAvailableFontNames() {
			
			return availableFontNames;
		}
		
		@SuppressWarnings("unchecked")
		public void loadAvailableFontNames() {
			
			Set<String> s= FontFactory.getRegisteredFamilies();

			//System.out.println(s);
			
			List<String> l= new ArrayList<String>(s.size());
			
			for (String name : s) {
				
				if (embedFonts) {
					try {
						BaseFont f= FontFactory.getFont(name.toString(), encoding, true, 12, Font.NORMAL, Color.BLACK, true).getBaseFont();
						if (f!=null) {
							
							String[][] names= f.getFamilyFontName();
							l.add(names[0][names[0].length-1]);
						}
					} catch (Exception e) {
						// ignore
					}
				} else {
					l.add(name);
				}
				
			}
			
			Collections.sort(l);
			//System.out.println(l);
			availableFontNames= l.toArray(new String[l.size()]);

		}

		/**
		 * @return the selectedFontNames
		 */
		public String[] getSelectedFontNames() {
			return selectedFontNames;
		}

		/**
		 * @param selectedFontNames the selectedFontNames to set
		 */
		public void setSelectedFontNames(String[] selectedFontNames) {
			this.selectedFontNames = selectedFontNames;
			fonts=null;
		}

		/**
		 * @return the embedFonts
		 */
		public boolean isEmbedFonts() {
			return embedFonts;
		}

		/**
		 * @param embedFonts the embedFonts to set
		 */
		public void setEmbedFonts(boolean embedFonts) {
			this.embedFonts = embedFonts;
			fonts=null;
		}
		
		public BaseFont getBaseFont() {
			if (baseFont==null) {
				try {
					baseFont= FontFactory.getFont("SansSerif", encoding, true, 12, Font.NORMAL, Color.BLACK, true).getBaseFont();
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e);
				}
			}
			if (baseFont==null) {
				try {
					baseFont= FontFactory.getFont("DeJaVu Sans", encoding, true, 12, Font.NORMAL, Color.BLACK, true).getBaseFont();
				} catch (Exception e1) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e1);
				}
			}
			if (baseFont==null) {
				try {
					baseFont= FontFactory.getFont("Arial", encoding, true, 12, Font.NORMAL, Color.BLACK, true).getBaseFont();
				} catch (Exception e2) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e2);
				}
			}
			if (baseFont==null) {
				try {
					baseFont= BaseFont.createFont(BaseFont.HELVETICA,"UTF-8",false);
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e);
				}
			}

			return baseFont;
		}
		
	}


	class OptionsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;

		private JComboBox fontsCombo;
		private JCheckBox embedCheckBox;
		private JComboBox sizeComboBox;

		public OptionsPanel() {
			if (getFontModel().getAvailableFontNames().length==0) {
				ApplicationHelper.executeInBackground(new Runnable() {
					@Override
					public void run() {
						Logger.getLogger(this.getClass()).debug("Loading fonts...");
						FontFactory.registerDirectories();
						getFontModel().loadAvailableFontNames();
						if (fontsCombo!=null) {
							rebuildFonts();
							setFromGlobalProperties();
						}
						Logger.getLogger(this.getClass()).debug("Fonts loaded.");
					}
				});
			}
			initialize();
		}
		
		private void initialize() {
			
			setLayout(new GridBagLayout());
			
			int row=0;
			
			add(new JLabel("Font:"),new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(11,11,4,4),0,0));

			fontsCombo= new JComboBox();
			fontsCombo.setRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;

				JTextField field;
				/*@Override
				public void setBackground(Color bg) {
					if (super.getBackground()!=Color.WHITE) {
						super.setBackground(Color.WHITE);
					}
				}*/
				private JTextField getField(String s) {
					if (field == null) {
						field = new JTextField();
						field.setEditable(false);
					}
					field.setText(s);
					return field;
				}
				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					Component c=null;
					String s= value !=null ? value.toString() : "";
					if (index<0) {
						c= getField(s);
					} else {
						c= super.getListCellRendererComponent(list, s, index, isSelected,
									cellHasFocus);
					}
					java.awt.Font f= new java.awt.Font(s, java.awt.Font.PLAIN, getFont().getSize());
					if (f!=null && s.length()>0 && f.canDisplay(s.charAt(0))) {
						c.setFont(f);
					} else {
						c.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, getFont().getSize()));
					}
					return c;
				}
			});
			fontsCombo.setBackground(Color.WHITE);
			fontsCombo.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (fontsCombo.getSelectedIndex()<1) {
						getFontModel().setSelectedFontNames(null);
						if (engine!=null) {
							engine.getGlobalProperties().putProperty(FONT_PROPERTY, null);
						}
					} else {
						getFontModel().setSelectedFontNames(new String[]{fontsCombo.getSelectedItem().toString()});
						if (engine!=null) {
							engine.getGlobalProperties().putProperty(FONT_PROPERTY, fontsCombo.getSelectedItem().toString());
						}
					}
				}
			});
			add(fontsCombo,new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(11,4,4,11),0,0));
			
			embedCheckBox= new JCheckBox();
			embedCheckBox.setVisible(!printMode);
			embedCheckBox.setText("Embed Fonts");
			embedCheckBox.setToolTipText("Fonts embedded in PDF document (preferred) ensures PDF looks same on each computer,\nbut they make PDF document slightly larger.");
			embedCheckBox.setSelected(true);
			embedCheckBox.addPropertyChangeListener("selected", new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					getFontModel().setEmbedFonts(embedCheckBox.isSelected());
					rebuildFonts();
				}
			});
			add(embedCheckBox,new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(4,11,4,11),0,0));

			add(new JLabel("Base font size:"),new GridBagConstraints(0,++row,1,1,0,1,GridBagConstraints.NORTH,GridBagConstraints.NONE,new Insets(4,11,4,4),0,0));

			sizeComboBox= new JComboBox(new Object[]{9f,10f,11f,12f,13f,14f});
			sizeComboBox.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					setBaseFontSize(((Float)sizeComboBox.getSelectedItem()).floatValue());
					if (engine!=null) {
						engine.getGlobalProperties().putProperty(FONT_SIZE_PROPERTY, ((Float)sizeComboBox.getSelectedItem()).intValue());
					}
				}
			});
			add(sizeComboBox,new GridBagConstraints(1,row,1,1,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,new Insets(4,4,4,11),0,0));
			
			rebuildFonts();
		}
		
		private void rebuildFonts() {
			String sel=null;
			if (fontsCombo.getSelectedIndex()>0) {
				sel= fontsCombo.getSelectedItem().toString();
			}
			
			String[] all= getFontModel().getAvailableFontNames();
			
			fontsCombo.removeAllItems();
			
			fontsCombo.addItem(DEFAULT_FONT_NAME);
			
			for (int i = 0; i < all.length; i++) {
				fontsCombo.addItem(all[i]);
			}
			
			if (sel!=null) {
				fontsCombo.setSelectedItem(sel);
			} else {
				fontsCombo.setSelectedIndex(0);
			}

			setFromGlobalProperties();
		}
		
		private void setFromGlobalProperties() {
			if (engine!=null) {
				if (fontsCombo!=null && fontsCombo.getItemCount()>1) {
					Object o= engine.getGlobalProperties().getProperty(FONT_PROPERTY);
					if (o instanceof String && o.toString().length()>0) {
						if (!o.equals(fontsCombo.getSelectedItem())) {
							for (int i = 0; i < fontsCombo.getItemCount(); i++) {
								if (fontsCombo.getItemAt(i).equals(o)) {
									fontsCombo.setSelectedIndex(i);
								}
							}
						}
					} else {
						if (fontsCombo.getSelectedIndex()!=0) {
							fontsCombo.setSelectedIndex(0);
						}
					}
				}
				
				Integer i= engine.getGlobalProperties().getInteger(FONT_SIZE_PROPERTY);
				if (i!=null) {
					if (((Float)sizeComboBox.getSelectedItem()).intValue()!=i) {
						sizeComboBox.setSelectedItem(i.floatValue());
					}
				}
			}
		}

	}
	
	public static final String DEFAULT_FONT_NAME = "<Default>";

	private static final String NONE_DOT = "No actions selected or defined.";
	private static final String EMPTY = "";
	private static final String NONE = "None";
	private static final String REMINDER = "Reminder:"+" ";
	private static final String PRIORITY = "Priority:"+" ";
	private static final String PROJECT = "Project:"+" ";
	private static final String CREATED = "Created:"+" ";
	private static final String ID = "ID:"+" ";
	private static final String OPEN= "OPEN ";
	private static final String RESOLVED= "RESOLVED ";
	private static final String DELETED= "DELETED ";
	private static final String STALLED= "STALLED ";
	
	private static final String CHECK_OPEN= "\u2610";
	private static final String CHECK_RESOLVED= "\u2611";
	private static final String CHECK_DELETED= "\u2612";
	private static final String CHECK_STALLED= "\u2205";
	private static final String FULL_STAR= "\u2605";
	private static final String HOLLOW_STAR= "\u2606";

	
	private static final Color COLOR_PRIORITY_LOW= new Color(0xfff001);
	private static final Color COLOR_PRIORITY_MEDIUM= new Color(0xffc13b);
	private static final Color COLOR_PRIORITY_HIGH= new Color(0xff2b01);
	private static final Color COLOR_OPEN= new Color(0x0000ff);
	private static final Color COLOR_DELETED= new Color(0x808080);
	private static final Color COLOR_RESOLVED= new Color(0x008000);

	private static final String FONT_PROPERTY = "PDFExportAddOn.font";
	private static final String FONT_SIZE_PROPERTY = "PDFExportAddOn.fontSize";
	
	
	private String description= "Exports GTD-Free data as PDF doument.";
	private ExportAddOn.ExportOrder[] orders= new ExportAddOn.ExportOrder[]{ExportAddOn.ExportOrder.FoldersProjectsActions,ExportAddOn.ExportOrder.FoldersActions,ExportAddOn.ExportOrder.ProjectsActions,ExportAddOn.ExportOrder.ProjectsFoldersActions,ExportAddOn.ExportOrder.Actions};
	private String name = "PDF";
	private FileFilter[] fileFilters = new FileFilter[]{new FileNameExtensionFilter("PDF documents","pdf")};
	private float baseFontSize= 9;
	private boolean marginSet=false;
	private float marginBottom;
	private float marginTop;
	private float marginRight;
	private float marginLeft;
	private FontSelector fontSelector;
	private boolean sizeSet;
	private Rectangle pageSize;
	private FontSelector fontSelectorB;
	private FontSelector fontSelectorB2;
	private FontSelector fontSelectorB4;
	private BaseFont baseFont;
	private FontModel fontModel= new FontModel();
	private OptionsPanel component;
	private boolean printMode;
	private GTDFreeEngine engine;
	

	/**
	 * @return the printMode
	 */
	public boolean isPrintMode() {
		return printMode;
	}

	/**
	 * @param printMode the printMode to set
	 */
	public void setPrintMode(boolean printMode) {
		this.printMode = printMode;
	}

	public void setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom) {
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        marginSet=true;
	}
	
	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#export(org.gtdfree.model.GTDModel, org.gtdfree.model.ActionFilter, java.io.OutputStream, org.gtdfree.model.ExportAddOn.ExportOrder)
	 */
	@Override
	public void export(GTDModel model, ActionsCollection collection, OutputStream out,
			ExportAddOn.ExportOrder order, FileFilter ff, boolean compact) throws Exception {

		fontSelector= new FontSelector();
		fontSelectorB= new FontSelector();
		fontSelectorB2= new FontSelector();
		fontSelectorB4= new FontSelector();
    	
		baseFont = fontModel.getBaseFont();

		for (BaseFont bf : fontModel.getFonts()) {
            fontSelector.addFont(new Font(bf,baseFontSize));
            fontSelectorB.addFont(new Font(bf,baseFontSize,Font.BOLD));
            fontSelectorB2.addFont(new Font(bf,baseFontSize+2,Font.BOLD));
            fontSelectorB4.addFont(new Font(bf,baseFontSize+4,Font.BOLD));
		}
            
		boolean emptyH2=false;
		boolean emptyH3=false;
		
		PdfPTable actionTable=null;
		
		Document doc= new Document();

		if (sizeSet) {
			doc.setPageSize(pageSize);
		}
		if (marginSet) {
			doc.setMargins(marginLeft, marginRight, marginTop, marginBottom);
		}
		
		//System.out.println("PDF size "+doc.getPageSize().toString());
		//System.out.println("PDF m "+marginLeft+" "+marginRight+" "+marginTop+" "+marginBottom);
		

		@SuppressWarnings("unused")
		PdfWriter pw= PdfWriter.getInstance(doc, out);
		
		doc.addCreationDate();
		doc.addTitle("GTD-Free PDF");
		doc.addSubject("GTD-Free data exported as PDF");
		
		HeaderFooter footer= new HeaderFooter(newParagraph(), true);
		footer.setAlignment(HeaderFooter.ALIGN_CENTER);
		footer.setBorder(HeaderFooter.TOP);
		doc.setFooter(footer);
		
		doc.open();


		Phrase ch= newTitle("GTD-Free Data");
		Paragraph p= new Paragraph(ch);
		p.setAlignment(Paragraph.ALIGN_CENTER);

		PdfPTable t= new PdfPTable(1);
		t.setWidthPercentage(100f);
		PdfPCell c= newCell(p);
		c.setBorder(Table.BOTTOM);
		c.setBorderWidth(2.5f);
		c.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
		c.setPadding(5f);
		t.addCell(c);
		doc.add(t);
		
		Iterator<Object> it= collection.iterator(order);

		
		while (it.hasNext()) {
			Object o = it.next();
			
			if (o == ActionsCollection.ACTIONS_WITHOUT_PROJECT) {
				
				if (order==ExportAddOn.ExportOrder.FoldersProjectsActions) {
					
					doc.add(newSubSection(ActionsCollection.ACTIONS_WITHOUT_PROJECT));
					
					emptyH2=false;
					emptyH3=true;
				} else {

					doc.add(newSection(ActionsCollection.ACTIONS_WITHOUT_PROJECT));
					
					emptyH2=true;
					emptyH3=false;
				}

				continue;
			}
			
			if (o instanceof Folder) {
				
				Folder f= (Folder)o;

				if (actionTable!=null) {
					doc.add(actionTable);
					actionTable=null;
				}

				if (f.isProject()) {
					
					if ( order== ExportAddOn.ExportOrder.ProjectsActions 
							|| order== ExportAddOn.ExportOrder.ProjectsFoldersActions) {

						if (emptyH2 || emptyH3) {
							p= newParagraph(NONE_DOT);
							doc.add(p);
						}

						doc.add(newSection(f.getName()));
						
						if (compact) {
							if (f.getDescription()!=null && f.getDescription().length()>0) {
								p= newParagraph(f.getDescription());
								p.setIndentationLeft(10f);
								p.setIndentationRight(10f);
								doc.add(p);
							}
						} else {
							t= new PdfPTable(2);
							t.setKeepTogether(true);
							t.setSpacingBefore(5f);
							t.setWidthPercentage(66f);
							t.setWidths(new float[]{0.33f,0.66f});
							
							c= newCell("ID");
							t.addCell(c);
							c= newCell(newStrongParagraph(String.valueOf(f.getId())));
							t.addCell(c);
							c= newCell("Type");
							t.addCell(c);
							c= newCell(newStrongParagraph("Project"));
							t.addCell(c);
							c= newCell("Open");
							t.addCell(c);
							c= newCell(newStrongParagraph(String.valueOf(f.getOpenCount())));
							t.addCell(c);
							c= newCell("All");
							t.addCell(c);
							c= newCell(newStrongParagraph(String.valueOf(f.size())));
							t.addCell(c);
							c= newCell("Description");
							t.addCell(c);
							c= newDescriptionCell(f.getDescription());
							t.addCell(c);
							
							doc.add(t);
						}
						
						emptyH2=true;
						emptyH3=false;

					} else {
						
						if (emptyH3) {
							p= newParagraph(NONE_DOT);
							doc.add(p);
						}

						doc.add(newSubSection("Project:"+" "+f.getName()));
						
						emptyH2=false;
						emptyH3=true;
					}
					
					continue;
 
				}
				if ( order == ExportAddOn.ExportOrder.FoldersActions 
						|| order == ExportAddOn.ExportOrder.FoldersProjectsActions) {
			
					if (emptyH2 || emptyH3) {
						p= newParagraph(NONE_DOT);
						doc.add(p);
					}

					doc.add(newSection(f.getName()));

					if (compact) {
						if (f.getDescription()!=null && f.getDescription().length()>0) {
							p= newParagraph(f.getDescription());
							p.setIndentationLeft(10f);
							p.setIndentationRight(10f);
							doc.add(p);
						}
					} else {

						t= new PdfPTable(2);
						t.setKeepTogether(true);
						t.setSpacingBefore(5f);
						t.setWidthPercentage(66f);
						t.setWidths(new float[]{0.33f,0.66f});
	
						c= newCell("ID");
						t.addCell(c);
						c= newCell(newStrongParagraph(String.valueOf(f.getId())));
						t.addCell(c);
	
						String type="";
						if (f.isAction()) {
							type="Action list";
						} else if (f.isInBucket()) {
							type="In-Bucket";
						} else if (f.isQueue()) {
							type="Next action queue";
						} else if (f.isReference()) {
							type="Reference list";
						} else if (f.isSomeday()) {
							type="Someday/Maybe list";
						} else if (f.isBuildIn()) {
							type="Default list";
						}
	
						c= newCell("Type");
						t.addCell(c);
						c= newCell(newStrongParagraph(type));
						t.addCell(c);
						c= newCell("Open");
						t.addCell(c);
						c= newCell(newStrongParagraph(String.valueOf(f.getOpenCount())));
						t.addCell(c);
						c= newCell("All");
						t.addCell(c);
						c= newCell(newStrongParagraph(String.valueOf(f.size())));
						t.addCell(c);
						c= newCell("Description");
						t.addCell(c);
						c= newDescriptionCell(f.getDescription());
						t.addCell(c);
						
						doc.add(t);
					}
					
					emptyH2=true;
					emptyH3=false;

				} else {
					
					if (emptyH3) {
						p= newParagraph(NONE_DOT);
						doc.add(p);
					}

					doc.add(newSubSection("List:"+" "+f.getName()));

					emptyH2=false;
					emptyH3=true;

				}

				continue;
				
			}
			
			if (o instanceof Action) {
				emptyH2=false;
				emptyH3=false;
				
				Action a= (Action)o;

				if (compact) {
					
					if (actionTable== null) {
						actionTable= new PdfPTable(5);
						actionTable.setWidthPercentage(100f);
						actionTable.setHeaderRows(1);
						actionTable.setSpacingBefore(5f);
						c= newHeaderCell("ID");
						actionTable.addCell(c);
						c= newHeaderCell("Pri.");
						actionTable.addCell(c);
						c= newHeaderCell("Description");
						actionTable.addCell(c);
						c= newHeaderCell("Reminder");
						actionTable.addCell(c);
						c= newHeaderCell(CHECK_RESOLVED);
						actionTable.addCell(c);

						float width= doc.getPageSize().getWidth()-doc.getPageSize().getBorderWidthLeft()-doc.getPageSize().getBorderWidthRight();
						int i= model.getLastActionID();
						float step= baseFontSize-1;
						int steps= (int)Math.floor(Math.log10(i))+1;
						// ID column
						float col1= 8 + steps * step;
						// Priority column
						float col2= 4 + 3 * (baseFontSize+4);
						// Reminder column
						float col4= 10 + step * 11;
						// Resolved column
						float col5= 8 + baseFontSize;
						// Description column
						float col3= width - col1 - col2 - col4 - col5;
						actionTable.setWidths(new float[]{col1,col2,col3,col4,col5});
						
					}
					
					addSingleActionRow(a, actionTable);
					
				} else {
					addSingleActionTable(model, doc, a);
				}
			}
			
		}
		
		if (actionTable!=null) {
			doc.add(actionTable);
			actionTable=null;
		}
		if (emptyH2 || emptyH3) {
			
			p= newParagraph(NONE_DOT);
			doc.add(p);
		}
		
		//w.writeCharacters("Exported: "+ApplicationHelper.toISODateTimeString(new Date()));
		
		doc.close();
	}

	private Phrase newTitle(String string) {
		Phrase p= fontSelectorB4.process(string);
		return p;
	}

	private PdfPCell newHeaderCell(String string) throws DocumentException, IOException {
		Paragraph p= new Paragraph(fontSelectorB.process(string));
		PdfPCell c= newCell(p);
		c.setHorizontalAlignment(Rectangle.ALIGN_CENTER);
		return c;
	}

	/**
	 * @param model
	 * @param doc
	 * @param a
	 * @throws DocumentException
	 * @throws IOException
	 * @throws BadElementException
	 */
	private void addSingleActionTable(GTDModel model, Document doc, Action a)
			throws DocumentException, IOException, BadElementException {
		Chunk ch;
		PdfPTable t;
		PdfPCell c;
		t= new PdfPTable(3);
		t.setSpacingBefore(5f);
		t.setWidthPercentage(100f);
		
		Paragraph ph= newParagraph();
		ch= newChunk(ID);
		ph.add(ch);
		ch= newChunk(String.valueOf(a.getId()));
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);
		c= newCell(ph);
		t.addCell(c);
		
		ph= newParagraph();
		ch= newChunk(CREATED);
		ph.add(ch);
		ch= newChunk(ApplicationHelper.toISODateTimeString(a.getCreated()));
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);
		c= newCell(ph);
		t.addCell(c);
		
		ph= newParagraph();
		if (a.isOpen()) {
			ch= newChunk(OPEN);
			ch.getFont().setStyle(Font.BOLD);
			ch.getFont().setColor(COLOR_OPEN);
			ph.add(ch);
			ch= newOpenChunk();
			ph.add(ch);
		} else if (a.isResolved()) {
			ch= newChunk(RESOLVED);
			ch.getFont().setStyle(Font.BOLD);
			ch.getFont().setColor(COLOR_RESOLVED);
			ph.add(ch);
			ch= newResolvedChunk();
			ph.add(ch);
		} else if (a.isDeleted()) {
			ch= newChunk(DELETED);
			ch.getFont().setStyle(Font.BOLD);
			ch.getFont().setColor(COLOR_DELETED);
			ph.add(ch);
			ch= newDeletedChunk();
			ph.add(ch);
		} else {
			ch= newChunk(STALLED);
			ch.getFont().setStyle(Font.BOLD);
			ch.getFont().setColor(COLOR_DELETED);
			ph.add(ch);
			ch= newStalledChunk();
			ph.add(ch);
		}
		c= newCell(ph);
		c.setHorizontalAlignment(Cell.ALIGN_RIGHT);
		t.addCell(c);

		ph= newParagraph();
		ch= newChunk(PRIORITY);
		ph.add(ch);
		ch= newChunk(a.getPriority()==null ? NONE : a.getPriority().toString());
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);
		ch= newChunk(" ");
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);

		if (a.getPriority()==null || a.getPriority()==Priority.None) {
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.Low) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.Medium) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newMediumStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.High) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newMediumStarChunk();
			ph.add(ch);
			ch= newHighStarChunk();
			ph.add(ch);
		}

		c= newCell(ph);
		t.addCell(c);
		
		ph= newParagraph();
		ch= newChunk(REMINDER);
		ph.add(ch);
		ch= newChunk(a.getRemind()!=null ? ApplicationHelper.toISODateString(a.getRemind()) : NONE);
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);
		c= newCell(ph);
		t.addCell(c);
		

		ph= newParagraph();
		ch= newChunk(PROJECT);
		ph.add(ch);
		if (a.getProject()!=null) {
			ch= newChunk(model.getProject(a.getProject()).getName());
		} else {
			ch= newChunk(NONE);
		}
		ch.getFont().setStyle(Font.BOLD);
		ph.add(ch);
		c= newCell(ph);
		t.addCell(c);

		c= newDescriptionCell(a.getDescription());
		c.setColspan(3);
		t.addCell(c);
		
		
		if (a.getUrl()!=null) {
			
			ch= newChunk(a.getUrl().toString());
			ch.setAnchor(a.getUrl());
			ch.getFont().setColor(Color.BLUE);
			
			c= newCell(new Paragraph(ch));
			c.setColspan(3);
			t.addCell(c);
		}

		doc.add(t);
	}
	
	private void addSingleActionRow(Action a, PdfPTable t)
	throws DocumentException, IOException, BadElementException {
		
		Chunk ch;
		PdfPCell c;
		
		// ID row
		Paragraph ph= newParagraph(String.valueOf(a.getId()));
		c= newCell(ph);
		c.setHorizontalAlignment(Cell.ALIGN_RIGHT);

		t.addCell(c);
		
		// Priority
		ph= newParagraph();
		if (a.getPriority()==null || a.getPriority()==Priority.None) {
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.Low) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.Medium) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newMediumStarChunk();
			ph.add(ch);
			ch= newNoneStarChunk();
			ph.add(ch);
		} else if (a.getPriority()==Priority.High) {
			ch= newLowStarChunk();
			ph.add(ch);
			ch= newMediumStarChunk();
			ph.add(ch);
			ch= newHighStarChunk();
			ph.add(ch);
		}
		c= newCell(ph);
		t.addCell(c);

		// description
		
		c= newDescriptionCell(a.getDescription());
		t.addCell(c);

		// reminder 
		
		ph= newParagraph(a.getRemind()!=null ? ApplicationHelper.toDateString(a.getRemind()) : EMPTY);
		c= newCell(ph);
		c.setHorizontalAlignment(Cell.ALIGN_RIGHT);
		t.addCell(c);

		// status
		
		ph= newParagraph();
		if (a.isOpen()) {
			ch= newOpenChunk();
			ph.add(ch);
		} else if (a.isResolved()) {
			ch= newResolvedChunk();
			ph.add(ch);
		} else if (a.isDeleted()) {
			ch= newDeletedChunk();
			ph.add(ch);
		} else {
			ch= newStalledChunk();
			ph.add(ch);
		}
		c= newCell(ph);
		c.setHorizontalAlignment(Cell.ALIGN_RIGHT);
		t.addCell(c);
		
		/*if (a.getUrl()!=null) {
			
			ch= newChunk(a.getUrl().toString());
			ch.setAnchor(a.getUrl());
			ch.getFont().setColor(Color.BLUE);
			
			c= newCell(new Paragraph(ch));
			c.setColspan(3);
			t.addCell(c);
		}*/
		
	}

	private Chunk newChunk(String s) throws DocumentException, IOException {
		if (s==null) {
			s="";
		}
		Chunk c= new Chunk(s,newFont());
		return c;
	}

	private Paragraph newParagraph() throws DocumentException, IOException {
		Paragraph p= new Paragraph();
		return p;
	}

	private PdfPCell newCell(Paragraph p) throws BadElementException {
		PdfPCell c= new PdfPCell(p);
		c.setBorder(PdfPCell.BOX);
		c.setBorderWidth(0.1f);
		c.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
		c.setPaddingBottom(3f);
		c.setPaddingLeft(3f);
		c.setPaddingRight(3f);
		c.setPaddingTop(0f);
		c.setUseBorderPadding(true);
		return c;
	}

	private PdfPCell newCell(String string) throws DocumentException, IOException {
		PdfPCell c= newCell(newParagraph(string));
		return c;
	}

	private PdfPCell newDescriptionCell(String s) throws DocumentException, IOException {
		PdfPCell c= newCell(s);
		c.setPaddingBottom(5f);
		c.setPaddingLeft(5f);
		c.setPaddingRight(5f);
		c.setPaddingTop(2f);
		return c;
	}

	private Paragraph newParagraph(String string) throws DocumentException, IOException {
		Paragraph p= new Paragraph(fontSelector.process(string));
		return p;
	}

	private Font newFont() throws DocumentException, IOException {
		return new Font(baseFont,baseFontSize);
	}
	
	private Paragraph newStrongParagraph(String s) throws DocumentException, IOException {
		Paragraph p= new Paragraph(fontSelectorB.process(s));
		return p;
	}

	private Chunk newResolvedChunk() throws DocumentException, IOException {
		Chunk c= newChunk(CHECK_RESOLVED);
		c.getFont().setSize(baseFontSize+2);
		c.getFont().setColor(COLOR_RESOLVED);
		return c;
	}

	private Chunk newOpenChunk() throws DocumentException, IOException {
		Chunk c= newChunk(CHECK_OPEN);
		c.getFont().setSize(baseFontSize+2);
		c.getFont().setColor(COLOR_OPEN);
		return c;
	}

	private Chunk newStalledChunk() throws DocumentException, IOException {
		Chunk c= newChunk(CHECK_STALLED);
		c.getFont().setSize(baseFontSize+2);
		c.getFont().setColor(COLOR_DELETED);
		return c;
	}

	private Chunk newDeletedChunk() throws DocumentException, IOException {
		Chunk c= newChunk(CHECK_DELETED);
		c.getFont().setSize(baseFontSize+2);
		c.getFont().setColor(COLOR_DELETED);
		return c;
	}

	private Chunk newHighStarChunk() throws DocumentException, IOException {
		Chunk c= newChunk(FULL_STAR);
		//c.getFont().setSize(defaultFontSize+2);
		c.getFont().setColor(COLOR_PRIORITY_HIGH);
		return c;
	}

	private Chunk newMediumStarChunk() throws DocumentException, IOException {
		Chunk c= newChunk(FULL_STAR);
		//c.getFont().setSize(defaultFontSize+2);
		c.getFont().setColor(COLOR_PRIORITY_MEDIUM);
		return c;
	}

	private Chunk newLowStarChunk() throws DocumentException, IOException {
		Chunk c= newChunk(FULL_STAR);
		//c.getFont().setSize(defaultFontSize+2);
		c.getFont().setColor(COLOR_PRIORITY_LOW);
		return c;
	}

	private Chunk newNoneStarChunk() throws DocumentException, IOException {
		Chunk c= newChunk(HOLLOW_STAR);
		//c.getFont().setSize(defaultFontSize+2);
		c.getFont().setColor(Color.BLACK);
		return c;
	}

	private Element newSection(String s) throws DocumentException, IOException {
		Phrase c= fontSelectorB2.process(s);
		Paragraph p= new Paragraph(c);
		
		PdfPTable t= new PdfPTable(1);
		t.setSpacingBefore(15f);
		t.setSpacingAfter(7f);
		t.setWidthPercentage(100f);
		
		PdfPCell ce= newCell(p);
		ce.setBorder(PdfPCell.BOTTOM);
		ce.setBorderWidth(1f);
		ce.setPaddingLeft(0);
		ce.setPaddingRight(0);
		
		t.addCell(ce);
		
		return t;
	}

	private Element newSubSection(String s) throws DocumentException, IOException {
		Phrase c= fontSelectorB.process(s);
		Paragraph p= new Paragraph(c);
		
		PdfPTable t= new PdfPTable(1);
		t.setSpacingBefore(7f);
		t.setSpacingAfter(5f);
		t.setWidthPercentage(100f);
		
		PdfPCell ce= newCell(p);
		ce.setBorder(PdfPCell.BOTTOM);
		ce.setBorderWidth(0.75f);
		ce.setPaddingLeft(0);
		ce.setPaddingRight(0);
		
		t.addCell(ce);
		
		return t;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getDefaultExportOrder()
	 */
	@Override
	public ExportAddOn.ExportOrder getDefaultExportOrder() {
		return ExportAddOn.ExportOrder.FoldersProjectsActions;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getSupportedExportOrders()
	 */
	@Override
	public ExportAddOn.ExportOrder[] getSupportedExportOrders() {
		return orders;
	}
	
	@Override
	public FileFilter[] getFileFilters() {
		return fileFilters;
	}

	/**
	 * @return the defaultFontSize
	 */
	public float getBaseFontSize() {
		return baseFontSize;
	}

	/**
	 * @param defaultFontSize the defaultFontSize to set
	 */
	public void setBaseFontSize(float defaultFontSize) {
		this.baseFontSize = defaultFontSize;
	}
	
	public void setPageSize(Rectangle pageSize) {
		this.pageSize = pageSize;
	}
	
	public void setPageSize(float width, float height) {
		this.pageSize = new Rectangle(width,height);
	}

	public Rectangle getPageSize() {
		return pageSize;
	}
	
	@Override
	public JComponent getComponent() {
		if (component == null) {
			component = new OptionsPanel();
		}

		return component;
	}
	
	public FontModel getFontModel() {
		return fontModel;
	}
	
	@Override
	public void initialize(GTDFreeEngine engine) {
		this.engine=engine;
		
		if (component!=null) {
			component.setFromGlobalProperties();
		}
		
		PropertyChangeListener l= new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (component!=null) {
					component.setFromGlobalProperties();
				}
			}
		};
		engine.getGlobalProperties().addPropertyChangeListener(FONT_PROPERTY, l);
		engine.getGlobalProperties().addPropertyChangeListener(FONT_SIZE_PROPERTY, l);
	}

}
