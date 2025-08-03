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

import junit.framework.TestCase;

import org.gtdfree.journal.JournalTools;

/**
 * @author ikesan
 *
 */
public class JournalToolsTest extends TestCase {

	public void testTool() throws Exception {
		
		long time0= System.currentTimeMillis();
		
		long day0= JournalTools.today();
		
		long day1= JournalTools.toDay(time0);

		assertEquals(day0, day1);

		long time1= JournalTools.toDate(day0).getTime();
		
		assertTrue(time0-time1<JournalTools.MILLIS_IN_DAY);
		
		day1= JournalTools.toDay(time1);
		
		assertEquals(day0, day1);
		
		day1++;
		
		time1= JournalTools.toDate(day1).getTime();
		
		assertEquals(day1,JournalTools.toDay(time1));
		
		testTime(0);
		
		testTime(JournalTools.MILLIS_IN_DAY);
		
	}
	
	private void testTime(long time) {
		
		long day0= JournalTools.toDay(time);

		long time0= JournalTools.toDate(day0).getTime();

		assertEquals(time, time0);

	}
	
}
