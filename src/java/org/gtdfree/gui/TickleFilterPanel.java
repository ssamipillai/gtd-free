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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import org.gtdfree.Messages;
import org.gtdfree.model.RemindFilter;
import org.gtdfree.model.Utils;

/**
 * @author ikesan
 *
 */
public class TickleFilterPanel extends AbstractFilterPanel {

	private static final long serialVersionUID = 1L;

	class WeekdayToggleButton extends JToggleButton {
		
		private static final long serialVersionUID = 1L;
		private String longName;
		private String shortName;
		private long end;
		private long start;

		public WeekdayToggleButton(String name) {
			super(name);
			longName=name;
			shortName=name.substring(0, 3);
			setMargin(new Insets(0,1,0,1));
			setToolTipText(getText());
			setSelected(false);
			addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					fireSearch(false);
				}
			});
		}
		
		public void fireSearch(boolean select) {
			if (select && !isSelected()) {
				setSelected(true);
				return;
			}
			if (isSelected()) {
				for (WeekdayToggleButton b : daysOfWeek.values()) {
					if (b.isSelected() && b!=WeekdayToggleButton.this) {
						b.setSelected(false);
					}
				}
				TickleFilterPanel.this.fireSearch(WeekdayToggleButton.this.start,WeekdayToggleButton.this.end);
			} else {
				for (WeekdayToggleButton b : daysOfWeek.values()) {
					if (b.isSelected()) {
						return;
					}
				}
				TickleFilterPanel.this.fireSearch(null);
			}
		}
		
		public void useShortName() {
			setText(shortName);
		}
		public void useLongName() {
			setText(longName);
		}

		public void setDates(long start, long end) {
			this.start=start;
			this.end=end;
		}
	}

	class MonthToggleButton extends JToggleButton {
		
		private static final long serialVersionUID = 1L;
		private String longName;
		private String shortName;
		private long start;
		private long end;

		public MonthToggleButton(String name) {
			super(name);
			longName=name;
			shortName=name.substring(0, 3);
			setMargin(new Insets(0,1,0,1));
			setToolTipText(getText());
			setSelected(false);
			addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					fireSearch(false);
				}
			});
		}
		
		public void fireSearch(boolean select) {
			if (select && !isSelected()) {
				setSelected(true);
				return;
			}
			if (isSelected()) {
				for (MonthToggleButton b : months.values()) {
					if (b.isSelected() && b!=MonthToggleButton.this) {
						b.setSelected(false);
					}
				}
				TickleFilterPanel.this.fireSearch(MonthToggleButton.this.start,MonthToggleButton.this.end);
			} else {
				for (MonthToggleButton b : months.values()) {
					if (b.isSelected()) {
						return;
					}
				}
				TickleFilterPanel.this.fireSearch(null);
			}
			
		}
		
		public void useShortName() {
			setText(shortName);
		}
		public void useLongName() {
			setText(longName);
		}

		public void setDates(long start, long end) {
			this.start=start;
			this.end=end;
		}
		
	}

	class DayOfMonthToggleButton extends JToggleButton {
		
		private static final long serialVersionUID = 1L;
		private long start;
		private long end;

		public DayOfMonthToggleButton(int code) {
			super(String.valueOf(code));
			setMargin(new Insets(0,1,0,1));
			setToolTipText(getText());
			setSelected(false);
			addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					fireSearch(false);
				}
			});
		}
		
		public void fireSearch(boolean select) {
			if (select && !isSelected()) {
				setSelected(true);
				return;
			}
			if (isSelected()) {
				for (DayOfMonthToggleButton b : daysOfMonth.values()) {
					if (b.isSelected() && b!=DayOfMonthToggleButton.this) {
						b.setSelected(false);
					}
				}
				TickleFilterPanel.this.fireSearch(DayOfMonthToggleButton.this.start,DayOfMonthToggleButton.this.end);
			} else {
				for (DayOfMonthToggleButton b : daysOfMonth.values()) {
					if (b.isSelected()) {
						return;
					}
				}
				TickleFilterPanel.this.fireSearch(null);
			}
		}

		public void setDates(long start, long end) {
			this.start=start;
			this.end=end;
		}
	}
	
	private JRadioButton allRadio;
	private JRadioButton pastRadio;
	private JPanel criterions;
	private JRadioButton weekRadio;
	private JRadioButton monthRadio;
	private JRadioButton yearRadio;
	private CardLayout cards;
	private Map<Integer,WeekdayToggleButton> daysOfWeek= new HashMap<Integer,WeekdayToggleButton>(7); 
	private ButtonGroup viewGroup;
	private DateFormatSymbols symbols= DateFormatSymbols.getInstance(Locale.ENGLISH);
	private Map<Integer, DayOfMonthToggleButton> daysOfMonth= new HashMap<Integer,DayOfMonthToggleButton>(31);
	private Map<Integer, MonthToggleButton> months= new HashMap<Integer, MonthToggleButton>(12);
	private SimpleDateFormat dayFormat= new SimpleDateFormat("EEE, d MMM yyyy"); //$NON-NLS-1$
	private SimpleDateFormat monthFormat= new SimpleDateFormat("MMMM yyyy"); //$NON-NLS-1$
	private JPanel weekPanel;
	private JPanel monthPanel;
	private JPanel yearPanel;
	private Dimension weekPref;
	protected Dimension yearPref;

	public TickleFilterPanel() {
		initialize();
	}
	
	private void initialize() {
		setLayout(new GridBagLayout());
		
		cards= new CardLayout();
		criterions= new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				if (weekRadio.isSelected()) {
					return criterions.getComponent(2).getPreferredSize();
				}
				if (monthRadio.isSelected()) {
					return criterions.getComponent(3).getPreferredSize();
				}
				if (yearRadio.isSelected()) {
					return criterions.getComponent(4).getPreferredSize();
				}
				return new Dimension(0,0);
			}
		};
		criterions.setLayout(cards);
		
		JLabel jl= new JLabel(Messages.getString("TickleFilterPanel.Tickler")+" "); //$NON-NLS-1$ //$NON-NLS-2$
		add(jl, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
		
		viewGroup= new ButtonGroup();
		
		allRadio= new JRadioButton(Messages.getString("TickleFilterPanel.All")); //$NON-NLS-1$
		allRadio.setToolTipText(Messages.getString("TickleFilterPanel.All.desc")); //$NON-NLS-1$
		allRadio.setSelected(true);
		allRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					cards.show(criterions, Messages.getString("TickleFilterPanel.All")); //$NON-NLS-1$
					fireSearch(null);
				}
			}
		});
		viewGroup.add(allRadio);
		add(allRadio, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,8,0,0),0,0));

		pastRadio= new JRadioButton(Messages.getString("TickleFilterPanel.Past")); //$NON-NLS-1$
		pastRadio.setToolTipText(Messages.getString("TickleFilterPanel.Past.desc")); //$NON-NLS-1$
		pastRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					cards.show(criterions, Messages.getString("TickleFilterPanel.Past")); //$NON-NLS-1$
					fireSearchPast(Utils.today());
				}
			}
		});
		viewGroup.add(pastRadio);
		add(pastRadio, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,8,0,0),0,0));
		
		weekRadio= new JRadioButton(Messages.getString("TickleFilterPanel.Week")); //$NON-NLS-1$
		weekRadio.setToolTipText(Messages.getString("TickleFilterPanel.Week.desc")); //$NON-NLS-1$
		weekRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					cards.show(criterions, Messages.getString("TickleFilterPanel.Week")); //$NON-NLS-1$
					updateWeekPanel();
				}
			}
		});
		viewGroup.add(weekRadio);
		add(weekRadio, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,8,0,0),0,0));
		
		monthRadio= new JRadioButton(Messages.getString("TickleFilterPanel.Month")); //$NON-NLS-1$
		monthRadio.setToolTipText(Messages.getString("TickleFilterPanel.Month.desc")); //$NON-NLS-1$
		monthRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					cards.show(criterions, Messages.getString("TickleFilterPanel.Month")); //$NON-NLS-1$
					updateMonthPanel();
					doLayout();
				}
			}
		});
		viewGroup.add(monthRadio);
		add(monthRadio, new GridBagConstraints(3,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,8,0,0),0,0));

		yearRadio= new JRadioButton(Messages.getString("TickleFilterPanel.Year")); //$NON-NLS-1$
		yearRadio.setToolTipText(Messages.getString("TickleFilterPanel.Year.desc")); //$NON-NLS-1$
		yearRadio.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					cards.show(criterions, Messages.getString("TickleFilterPanel.Year")); //$NON-NLS-1$
					updateYearPanel();
				}
			}
		});
		viewGroup.add(yearRadio);
		add(yearRadio, new GridBagConstraints(4,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,8,0,0),0,0));
		
		
		criterions.add(new JPanel(),Messages.getString("TickleFilterPanel.All")); //$NON-NLS-1$
		criterions.add(new JPanel(),Messages.getString("TickleFilterPanel.Past")); //$NON-NLS-1$
		
		weekPanel= new JPanel();
		weekPanel.setLayout(new GridBagLayout());
		String[] names= symbols.getWeekdays();
		for (int i=1; i<names.length; i++) {
			WeekdayToggleButton tb= new WeekdayToggleButton(names[i]);
			daysOfWeek.put(i, tb);
		}
		weekPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (weekPref==null || weekPref.width==0) {
					weekPref= weekPanel.getPreferredSize();
				}
				if (weekPref.width>weekPanel.getSize().width) {
					for (WeekdayToggleButton b : daysOfWeek.values()) {
						b.useShortName();
					}
				} else {
					for (WeekdayToggleButton b : daysOfWeek.values()) {
						b.useLongName();
					}
				}
			}
		});
		criterions.add(weekPanel,Messages.getString("TickleFilterPanel.Week")); //$NON-NLS-1$
		
		monthPanel= new JPanel();
		monthPanel.setLayout(new GridBagLayout());
		for (int i=1; i<32; i++) {
			DayOfMonthToggleButton tb= new DayOfMonthToggleButton(i);
			daysOfMonth.put(i, tb);
		}
		monthPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				updateMonthPanel();
			}
		});
		JScrollPane jsp= new JScrollPane(monthPanel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}
			@Override
			public Dimension getPreferredSize() {
				Dimension d= getViewport().getView().getMinimumSize();
				Dimension r= null;
				if (getHorizontalScrollBar().isVisible()) {
					r= new Dimension(d.width,d.height+getHorizontalScrollBar().getPreferredSize().height);
				} else {
					r= new Dimension(d.width,d.height);
				}
				return r;
			}
		};
		jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		criterions.add(jsp,Messages.getString("TickleFilterPanel.Month")); //$NON-NLS-1$

		yearPanel= new JPanel();
		yearPanel.setLayout(new GridBagLayout());
		yearPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (yearPref==null || yearPref.width==0) {
					yearPref= yearPanel.getPreferredSize();
				}
				if (yearPref.width>yearPanel.getSize().width) {
					for (MonthToggleButton b : months.values()) {
						b.useShortName();
					}
				} else {
					for (MonthToggleButton b : months.values()) {
						b.useLongName();
					}
				}
			}
		});
		names= symbols.getMonths();
		for (int i=0; i<12; i++) {
			MonthToggleButton tb= new MonthToggleButton(names[i]);
			months.put(i, tb);
		}
		criterions.add(yearPanel,Messages.getString("TickleFilterPanel.Year")); //$NON-NLS-1$

		add(criterions, new GridBagConstraints(0,1,5,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
	
	}
	
	protected void updateWeekPanel() {
		GregorianCalendar c=new GregorianCalendar();
		long start= Utils.today(c);
		int day= c.get(Calendar.DAY_OF_WEEK);
		WeekdayToggleButton tb= daysOfWeek.get(day);
		
		if (weekPanel.getComponentCount()!=7 || weekPanel.getComponent(0)!=tb) {
			weekPanel.removeAll();

				
			int index=0;
			for (int i= day; i<8; i++) {
				WeekdayToggleButton dtb= daysOfWeek.get(i);
				long end= start+Utils.MILLISECONDS_IN_DAY;
				dtb.setDates(start,end);
				dtb.setToolTipText(dayFormat.format(new Date(start)));
				weekPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
				start=end;
			}
			for (int i= 1; i<day; i++) {
				WeekdayToggleButton dtb= daysOfWeek.get(i);
				long end= start+Utils.MILLISECONDS_IN_DAY;
				dtb.setDates(start,end);
				dtb.setToolTipText(dayFormat.format(new Date(start)));
				weekPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
				start=end;
			}
		}
		tb.fireSearch(true);
	}

	protected void updateMonthPanel() {
		
		GregorianCalendar c=new GregorianCalendar();
		Utils.today(c);
		int day= c.get(Calendar.DAY_OF_MONTH);
		int max= c.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		DayOfMonthToggleButton tb= daysOfMonth.get(day);
		
		if (monthPanel.getComponentCount()<20 || monthPanel.getComponent(0)!=tb) {
			monthPanel.removeAll();

			int index=0;
			for (int i= day; i<=max; i++) {
				c.set(Calendar.DAY_OF_MONTH, i);
				DayOfMonthToggleButton dtb= daysOfMonth.get(i);
				dtb.setDates(c.getTimeInMillis(),c.getTimeInMillis()+Utils.MILLISECONDS_IN_DAY);
				dtb.setToolTipText(dayFormat.format(new Date(c.getTimeInMillis())));
				monthPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
			}
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
			for (int i= 1; i<day; i++) {
				c.set(Calendar.DAY_OF_MONTH, i);
				DayOfMonthToggleButton dtb= daysOfMonth.get(i);
				dtb.setDates(c.getTimeInMillis(),c.getTimeInMillis()+Utils.MILLISECONDS_IN_DAY);
				dtb.setToolTipText(dayFormat.format(new Date(c.getTimeInMillis())));
				monthPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
			}
		}
		tb.fireSearch(true);
	}

	protected void updateYearPanel() {
		GregorianCalendar c=new GregorianCalendar();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(GregorianCalendar.DAY_OF_MONTH, 1);
		c.set(GregorianCalendar.MILLISECOND, 0);
		c.set(GregorianCalendar.MINUTE, 0);
		c.set(GregorianCalendar.SECOND, 0);
		c.set(GregorianCalendar.HOUR_OF_DAY, 0);
		int month= c.get(Calendar.MONTH);
		MonthToggleButton tb= months.get(month);
		
		if (yearPanel.getComponentCount()!=12 || yearPanel.getComponent(0)!=tb) {
			yearPanel.removeAll();

			int index=0;
			for (int i= month; i<12; i++) {
				c.set(GregorianCalendar.MONTH, i);
				MonthToggleButton dtb= months.get(i);
				dtb.setToolTipText(monthFormat.format(new Date(c.getTimeInMillis())));
				yearPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
				long start= c.getTimeInMillis();
				c.set(Calendar.MONTH,i+1);
				dtb.setDates(start,c.getTimeInMillis());
			}
			for (int i= 0; i<month; i++) {
				c.set(GregorianCalendar.MONTH, i);
				MonthToggleButton dtb= months.get(i);
				dtb.setToolTipText(monthFormat.format(new Date(c.getTimeInMillis())));
				yearPanel.add(dtb,new GridBagConstraints(index++,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
				long start= c.getTimeInMillis();
				c.set(Calendar.MONTH,i+1);
				dtb.setDates(start,c.getTimeInMillis());
			}
		}
		tb.fireSearch(true);
	}

	private void fireSearch(long start, long end) {
		fireSearch(new RemindFilter(start,end));
	}
	private void fireSearchPast(long end) {
		fireSearch(new RemindFilter(end,true));
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.gui.AbstractFilterPanel#clearFilters()
	 */
	@Override
	protected void clearFilters() {
		allRadio.setSelected(true);
		
		for (WeekdayToggleButton b : daysOfWeek.values()) {
			b.setSelected(false);
		}
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public void setVisible(boolean flag) {
		
		if (!flag) {
			clearFilters();
			fireSearch(null);
		}
		
		super.setVisible(flag);
	}

	public void selectPast() {
		pastRadio.setSelected(true);
	}

	public void selectToday() {
		weekRadio.setSelected(true);
	}

}
