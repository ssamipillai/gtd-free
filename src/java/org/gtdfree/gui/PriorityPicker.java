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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.Messages;
import org.gtdfree.model.Priority;

/**
 * @author ikesan
 *
 */
public class PriorityPicker extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private static final Icon yellow= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_star_yellow);
	private static final Icon orange= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_star_orange);
	private static final Icon red= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_star_red);
	private static final Icon grey= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_star_grey);
	private static final Icon blue= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_star_blue);
	
	private static final int star_size= 12;
	
	public static final String PRIORITY_PROPERTY_NAME = "priority"; //$NON-NLS-1$
	
	public static void main(String[] args) {
		
		JFrame f= new JFrame();
		PriorityPicker p= new PriorityPicker();
		f.setContentPane(p);
		f.pack();
		f.setVisible(true);
		
	}
	
	
	private Priority priority = Priority.None;
	private Priority hover;

	
	public PriorityPicker() {
		
		Dimension d= new Dimension(3*(star_size+3),star_size+3);
		setMinimumSize(d);
		setPreferredSize(d);
		
		MouseAdapter ma= new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				callHover(e.getPoint(), true);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!isEnabled()) {
					return;
				}

				int q= getWidth()/3;
				
				if (e.getX()<q) {
					if (priority==Priority.Low) {
						setPriority(Priority.None);
					} else {
						setPriority(Priority.Low);
					}
				} else if (e.getX()<q*2) {
					if (priority==Priority.Medium) {
						setPriority(Priority.None);
					} else {
						setPriority(Priority.Medium);
					}
				} else {
					if (priority==Priority.High) {
						setPriority(Priority.None);
					} else {
						setPriority(Priority.High);
					}
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				if (!isEnabled()) {
					return;
				}

				setHover(null,true);
			}
		};
		
		addMouseListener(ma);
		addMouseMotionListener(ma);
	}
	
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(Priority priority) {
		if (this.priority==priority) {
			return;
		}
		Priority old= this.priority;
		this.priority = priority;
		firePropertyChange(PRIORITY_PROPERTY_NAME, old, priority);
		setToolTipText(Messages.getString("PriorityPicker.Priority")+" "+priority); //$NON-NLS-1$ //$NON-NLS-2$
		repaint();
	}

	/**
	 * @return the priority
	 */
	public Priority getPriority() {
		return priority;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int space= (getWidth()-3*star_size)/6;
		int top= (getHeight()-star_size)/2;
		
		Icon i1= grey;
		Icon i2= grey;
		Icon i3= grey;

		
		if (hover!=null && isEnabled()) {
			if (hover.ordinal()>0) {
				i1= blue;
			}
			if (hover.ordinal()>1) {
				i2= blue;
			}
			if (hover.ordinal()>2) {
				i3= blue;
			}
		} else if (priority!=null && isEnabled()) {
			if (priority.ordinal()>0) {
				i1= yellow;
			}
			if (priority.ordinal()>1) {
				i2= orange;
			}
			if (priority.ordinal()>2) {
				i3= red;
			}
		}
		
		i1.paintIcon(this, g, space, top);
		i2.paintIcon(this, g, space*3+star_size, top);
		i3.paintIcon(this, g, space*5+star_size*2, top);
		
	}
	
	protected void callHover(Point p, boolean repaint) {
		if (!isEnabled()) {
			return;
		}
		int q= getWidth()/3;
		
		if (p.getX()<q) {
			setHover(Priority.Low, repaint);
		} else if (p.getX()<q*2) {
			setHover(Priority.Medium, repaint);
		} else {
			setHover(Priority.High, repaint);
		}

	}
	private void setHover(Priority hover, boolean repaint) {
		if (this.hover == hover) {
			return;
		}
		this.hover = hover;
		setToolTipText(Messages.getString("PriorityPicker.Priority")+" "+(hover!=null?hover:priority)); //$NON-NLS-1$ //$NON-NLS-2$

		if (repaint) {
			repaint();
		}
	}
}
