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

import org.gtdfree.GlobalProperties;
import org.gtdfree.model.GTDDataXML;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class ModelTestXML extends ModelTest {
	
	@Override
	public GTDModel setUpGTDModel(int i) throws IOException {
		
		File xmlFile= new File(testDir,"gtd-free-data."+i+".xml");

		GTDDataXML xmlData= new GTDDataXML();
		GlobalProperties gp= new GlobalProperties();
		gp.putProperty(GlobalProperties.AUTO_SAVE, true);
		xmlData.initialize(xmlFile, gp);
		
		GTDModel m= xmlData.restore();
		return m;
	}
	
	@Override
	public void tearDownGTDModel(int i, GTDModel m) {
		super.tearDownGTDModel(i, m);
		File xmlFile= new File(testDir,"gtd-free-data."+i+".xml");
		xmlFile.delete();
		
		for (int j = 0; j < 10; j++) {
			File f= new File(testDir,"gtd-free-data."+i+"backup"+j+".xml");
			f.delete();
		}
	}
	
}
