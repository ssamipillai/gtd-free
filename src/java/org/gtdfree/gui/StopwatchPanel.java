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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;

/**
 * @author ikesan
 *
 */
public class StopwatchPanel extends JPanel implements Runnable {
	
	private static final long serialVersionUID = 1L;

	private JProgressBar displayLabel;
	private JToggleButton startButton;
	private JButton resetButton;
	private long offset=0;
	private long start=0;
	private boolean running=false;
	private Thread thread=null;

	public static void main(String[] args) {
		
		JFrame f= new JFrame();
		f.setContentPane(new StopwatchPanel());
		f.pack();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		
	}
	
	
	public StopwatchPanel() {
		initialize();
	}

	private void initialize() {
		
		setLayout(new GridBagLayout());
		
		displayLabel= new JProgressBar();
		
		displayLabel.setFont(new Font("SansSerif",Font.BOLD,displayLabel.getFont().getSize())); //$NON-NLS-1$
		displayLabel.setString("2:00"); //$NON-NLS-1$
		displayLabel.setStringPainted(true);
		displayLabel.setMinimum(0);
		displayLabel.setMaximum(120);
		Dimension d= new Dimension(65,displayLabel.getPreferredSize().height);
		displayLabel.setPreferredSize(d);
		displayLabel.setMinimumSize(d);
		add(displayLabel, new GridBagConstraints(0,0,1,1,0,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0));

		startButton= new JToggleButton();
		startButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_start));
		startButton.setMargin(new Insets(2,2,2,2));
		startButton.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				
				if (startButton.isSelected()) {
					start();
				} else {
					stop();
				}
				
			}
		});
		add(startButton, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,4,0,0),0,0));
		
		resetButton= new JButton();
		resetButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_clear));
		resetButton.setMargin(new Insets(2,2,2,2));
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		add(resetButton, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,4,0,0),0,0));

	}
	
	public void updateDisplay() {
		
		if (SwingUtilities.isEventDispatchThread()) {
			_updateDisplay();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
			
				@Override
				public void run() {
					_updateDisplay();
				}
			});
		}
		
	}

	public synchronized void _updateDisplay() {
		
		int time=0;
		if (running) {
			long now= System.currentTimeMillis();
			time= (int)(now - start + offset)/1000;
		} else {
			time=(int)(offset/1000);
		}
		
		displayLabel.setValue(time);
		
		if (time<120) {
			time= 120-time;
		}
		
		StringBuilder sb= new StringBuilder(6);
		
		int t= time/60;
		sb.append(t);
		sb.append(':');
		t= time%60;
		if (t<10) {
			sb.append('0');
		}
		sb.append(t);
		
		displayLabel.setString (sb.toString());
	}

	public synchronized void reset() {
		start=System.currentTimeMillis();
		offset=0;
		updateDisplay();
	}

	public synchronized void stop() {
		
		if (!running) {
			return;
		}
		
		long now= System.currentTimeMillis();
		offset+= now-start;
		running=false;
		thread=null;
		updateDisplay();
		
		if (startButton.isSelected()) {
			startButton.setSelected(false);
		}
	}

	public synchronized void start() {
		
		thread= new Thread(this);
		running=true;
		start=System.currentTimeMillis();
		thread.setDaemon(false);
		thread.start();
		
	}
	
	@Override
	public void run() {
		while (running && thread==Thread.currentThread()) {
			
			updateDisplay();
			
			synchronized (startButton) {
				try {
					startButton.wait(999);
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
	}

}
