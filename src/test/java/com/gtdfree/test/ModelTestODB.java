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

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.gtdfree.GlobalProperties;
import org.gtdfree.model.GTDDataODB;
import org.gtdfree.model.GTDModel;
import org.neodatis.odb.ODB;

/**
 * @author ikesan
 *
 */
public class ModelTestODB extends ModelTest {
	
	@Override
	public GTDModel setUpGTDModel(int i) throws IOException {
		
		File f= new File(testDir,"gtd-free-data."+i+".odb");

		GTDDataODB data= new GTDDataODB();
		GlobalProperties gp= new GlobalProperties();
		gp.putProperty(GlobalProperties.AUTO_SAVE, true);
		data.initialize(f, gp);
		
		GTDModel m= data.restore();
		return m;
	}
	
	@Override
	public void tearDownGTDModel(int i, GTDModel m) {
		super.tearDownGTDModel(i, m);
		File f= new File(testDir,"gtd-free-data."+i+".odb");
		f.delete();
		
	}
	
	public void testVerifyConsistancy() {
		
		int i= 3;
		
		try {
			GTDModel m1= setUpGTDModel(i);
			
			m1.importData(gtdModel);
			
			checkConsistency(m1);
			
			assertContentEquals(gtdModel, m1);
			
			ODB odb= ((GTDDataODB)m1.getDataRepository()).getODB();
			
			odb.delete(m1);
			odb.close();
			
			GTDModel m2= setUpGTDModel(i);
			
			assertEquals(6, m2.size());
			
			Logger l= Logger.getAnonymousLogger();
			l.setUseParentHandlers(false);
			l.addHandler(new Handler() {
			
				@Override
				public void publish(LogRecord record) {
					System.out.println("> "+record.getMessage());
				}
			
				@Override
				public void flush() {
					//
				}
			
				@Override
				public void close() throws SecurityException {
					//
				}
			});
			
			((GTDDataODB)m2.getDataRepository()).checkConsistency(l, false, true);
			
			assertContentEquals(gtdModel, m2, false, false);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			tearDownGTDModel(i, null);
		}
		
		
	}
	
}
