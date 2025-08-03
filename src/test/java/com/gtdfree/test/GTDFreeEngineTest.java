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

package com.gtdfree.test;

import java.util.Properties;

import junit.framework.TestCase;

import org.gtdfree.GTDFreeEngine;

/**
 * @author ikesan
 *
 */
public class GTDFreeEngineTest extends TestCase {
	
	GTDFreeEngine engine;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {

		engine= new GTDFreeEngine();

	}
	
	public void testInitialization() {
		
		Properties p= engine.getConfiguration();
		
		assertNotNull(p);
		assertNotNull(p.getProperty("user.home"));
		assertNotNull(p.getProperty("build.version"));
		assertNotNull(p.getProperty("build.type"));
		
	}

}
