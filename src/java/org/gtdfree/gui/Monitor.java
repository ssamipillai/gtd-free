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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * @author ikesan
 *
 */
public class Monitor extends JPanel {

	private static final long serialVersionUID = 1L;

	class Point {
		private long free;
		private long total;

		public Point() {
			free= Runtime.getRuntime().freeMemory();
			total= Runtime.getRuntime().totalMemory();
			if (total>maxTotal) {
				maxTotal=total;
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f= new JFrame();
		f.setContentPane(new Monitor());
		f.setSize(300, 100);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}


	private long max;
	private long maxTotal;
	private LinkedList<Point> points;
	private JComponent graph;
	private JLabel label;
	protected boolean monitoring=true;
	private JFrame frame;
	
	
	public Monitor() {
		initialize();
	}


	private void initialize() {
		
		max= Runtime.getRuntime().maxMemory();
		
		points= new  LinkedList<Point>();
		
		setLayout(new BorderLayout());
		
		graph= new JComponent() {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Rectangle r= g.getClipBounds();
				g.setColor(Color.white);
				g.fillRect(r.x, r.y, r.width, r.height);

				synchronized (points) {
					int i=0;
					for (Point p : points) {
						if (i>=r.width) {
							break;
						}
						g.setColor(Color.green);
						g.drawLine(r.x+i, r.height, r.x+i, r.y + r.height-(int)(r.height*p.total/maxTotal));
						g.setColor(Color.gray);
						g.drawLine(r.x+i, r.height, r.x+i, r.y + r.height-(int)(r.height*(p.total-p.free)/maxTotal));
						i++;
					}
				}
			}
		};
		graph.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				trimPoints();
			}
		});
		
		add(graph, BorderLayout.CENTER);
		
		label= new JLabel();
		add(label, BorderLayout.SOUTH);
		
		new Thread() {
			protected Runnable task;
			@Override
			public synchronized void run() {
				
				while(isMonitoring()) {
					
					updatePoints();
					
					if (task==null) {
						task= new Runnable() {
							@Override
							public void run() {
								if (!isMonitoring()) {
									return;
								}
								updateDisplay();
								task=null;
							}
						};
						SwingUtilities.invokeLater(task);
					}

					try {
						wait(1000);
					} catch (InterruptedException e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
					
				}
			};
		}.start();
		
	}
	
	private void updatePoints() {
		synchronized (points) {
			points.add(new Point());
			trimPoints();
		}
	}


	private void trimPoints() {
		synchronized (points) {
			while(points.size()>graph.getWidth()) {
				points.removeFirst();
			}
		}
	}
	
	private void updateDisplay() {
		if (points.isEmpty()) {
			return;
		}
		Point p= points.getLast();
		label.setText(" "+"Used:"+" "+(int)((p.total-p.free)/1000000)+"MB  "+"Total:"+" "+(int)(p.total/1000000)+"MB  "+"Max:"+" "+(int)(max/1000000)+"MB"+" ");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
		graph.repaint();
	}
	
	public JFrame getDialog() {
		
		if (frame==null) {
			frame= new JFrame("Memory Monitor"); //$NON-NLS-1$
			frame.setContentPane(this);
			frame.setSize(320, 100);
			frame.setLocationRelativeTo(null);
		}
		return frame;
	}

	public void close() {
		monitoring=false;
		if (frame!=null) {
			frame.dispose();
		}
	}
	
	public boolean isMonitoring() {
		return monitoring;
	}
}
