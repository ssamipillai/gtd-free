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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.gtdfree.model.Action;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.GTDDataODB.ActionProxyODB;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.OID;
import org.neodatis.odb.Objects;
import org.neodatis.odb.OdbConfiguration;
import org.neodatis.odb.core.trigger.SelectTrigger;
import org.neodatis.odb.xml.XMLExporter;
import org.neodatis.odb.xml.XMLImporter;

/**
 * @author ikesan
 * 
 */
public class ODBTest extends TestCase {

	
	static class Bird {
		public String name;
		public int size;

		public Bird() {
		}

		public Bird(String n, int s) {
			name = n;
			size = s;
		}
	}

	static class Flock {
		public List<Bird> birds = new ArrayList<Bird>();

		public void add(Bird b) {
			birds.add(b);
		}

		public int size() {
			return birds.size();
		}

		public Bird get(int i) {
			return birds.get(i);
		}
		
		public List<Bird> getBirds() {
			return birds;
		}
		
		public void setBirds(List<Bird> birds) {
			this.birds = birds;
		}
	}

	File f = new File("odb.data");

	public void testBird() {

		f.delete();
		ODB odb = ODBFactory.open(f.toString());

		Bird b = new Bird("Duck", 10);

		odb.store(b);

		b.size = 11;

		odb.store(b);

		odb.close();

		odb = ODBFactory.open(f.toString());

		Objects<Bird> o = odb.getObjects(Bird.class);

		assertEquals(1, o.size());

		Bird b1 = o.getFirst();
		assertEquals(11, b1.size);

		odb.close();

		f.delete();
		odb = ODBFactory.open(f.toString());

		OID oid = odb.store(b);
		b = (Bird)odb.getObjectFromId(oid);

		b.size = 12;
		odb.store(b);

		odb.close();

		odb = ODBFactory.open(f.toString());
		o = odb.getObjects(Bird.class);
		assertEquals(1, o.size());

		b1 = o.getFirst();
		assertEquals(12, b1.size);

		odb.close();
	}

	public void testFlock() {

		f.delete();
		ODB odb = ODBFactory.open(f.toString());

		Bird b = new Bird("Duck", 10);
		odb.store(b);
		b.size = 11;
		odb.store(b);

		Flock fl = new Flock();
		fl.add(b);

		odb.store(fl);

		fl.add(new Bird("Lame Duck", 3));
		b.size = 12;
		odb.store(fl);

		Objects<Flock> o = odb.getObjects(Flock.class);
		assertEquals(1, o.size());

		odb.close();
		odb = ODBFactory.open(f.toString());

		o = odb.getObjects(com.gtdfree.test.ODBTest.Flock.class);
		assertEquals(1, o.size());

		fl = o.getFirst();

		assertEquals(2, fl.size());
		assertEquals(12, fl.get(0).size);
		assertEquals(3, fl.get(1).size);

		odb.close();

	}

	public void testBulkBirds() {
		f.delete();
		ODB odb = ODBFactory.open(f.toString());

		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			System.currentTimeMillis();
			odb
					.store(new Bird(
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
							100));
			odb.commit();
			// System.out.println(System.currentTimeMillis()-t);
		}
		long t1 = System.currentTimeMillis();
		odb.close();
		t1 = System.currentTimeMillis() - t1;
		time1 = System.currentTimeMillis() - time1;
		System.out.println("TIME 1 (store+commit): "
				+ (System.currentTimeMillis() - time1));

		f.delete();
		odb = ODBFactory.open(f.toString());
		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			System.currentTimeMillis();
			odb
					.store(new Bird(
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
							100));
			// System.out.println(System.currentTimeMillis()-t);
		}
		long t2 = System.currentTimeMillis();
		odb.close();
		t2 = System.currentTimeMillis() - t2;
		time2 = System.currentTimeMillis() - time2;
		System.out.println("close 1: " + t1);
		System.out.println("TIME 1 (store+commit): " + time1);
		System.out.println("close 2: " + t2);
		System.out.println("TIME 2 (store): " + time2);

	}

	public void testBulkActions() {
		OdbConfiguration.setThrowExceptionWhenInconsistencyFound(true);
		// OdbConfiguration.setByteCodeInstrumentationIsOn(false);
		OdbConfiguration.setAutomaticCloseFileOnExit(true);
		try {
			OdbConfiguration.setDatabaseCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		OdbConfiguration.setUseLazyCache(true);

		f.delete();
		ODB odb = ODBFactory.open(f.toString());

		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// long t= System.currentTimeMillis();
			new ActionProxyODB(
					new Action(
							i,
							new Date(),
							new Date(),
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
					odb);
			odb.commit();
			// System.out.println(System.currentTimeMillis()-t);
		}
		long t1 = System.currentTimeMillis();
		odb.close();
		t1 = System.currentTimeMillis() - t1;
		time1 = System.currentTimeMillis() - time1;
		// System.out.println("TIME 1 (store+commit): "+(System.currentTimeMillis()-time1));

		f.delete();
		odb = ODBFactory.open(f.toString());
		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// long t= System.currentTimeMillis();
			new ActionProxyODB(
					new Action(
							i,
							new Date(),
							new Date(),
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
					odb);
			// System.out.println(System.currentTimeMillis()-t);
		}
		long t2 = System.currentTimeMillis();
		odb.close();
		t2 = System.currentTimeMillis() - t2;
		time2 = System.currentTimeMillis() - time2;

		f.delete();
		odb = ODBFactory.open(f.toString());
		long time3 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// long t= System.currentTimeMillis();
			ActionProxyODB ap = new ActionProxyODB(
					new Action(
							i,
							new Date(),
							new Date(),
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
					odb);
			// long tt= System.currentTimeMillis();
			ap.store();
			// System.out.println((System.currentTimeMillis()-t)+" "+(tt-t));
		}
		long t3 = System.currentTimeMillis();
		odb.close();
		t3 = System.currentTimeMillis() - t3;
		time3 = System.currentTimeMillis() - time3;

		f.delete();
		odb = ODBFactory.open(f.toString());
		ActionProxyODB[] ap = new ActionProxyODB[1000];
		for (int i = 0; i < 1000; i++) {
			// long t= System.currentTimeMillis();
			ap[i] = new ActionProxyODB(
					new Action(
							i,
							new Date(),
							new Date(),
							"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
					odb);
			// System.out.println((System.currentTimeMillis()-t)+" "+(tt-t));
		}
		long time4 = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			ap[i]
					.get()
					.setDescription(
							i
									+ "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
			ap[i].store();
		}
		long t4 = System.currentTimeMillis();
		odb.close();
		t4 = System.currentTimeMillis() - t4;
		time4 = System.currentTimeMillis() - time4;

		System.out.println("close 1: " + t1);
		System.out.println("TIME 1 (store+commit): " + time1);
		System.out.println("close 2: " + t2);
		System.out.println("TIME 2 (store): " + time2);
		System.out.println("close 3: " + t3);
		System.out.println("TIME 3 (store+update+commit): " + time3);
		System.out.println("close 4: " + t4);
		System.out.println("TIME 4 (update): " + time4);

	}

	public void testStoring() {

		f.delete();

		ODB odb = ODBFactory.open(f.toString());

		Action a = new Action(10, new Date(100), new Date(1000), "desc1");
		a.setResolution(Resolution.RESOLVED);

		OID i = odb.store(a);

		assertNotNull(i);

		System.out.println("OID " + i.oidToString());

		Action a1 = (Action)odb.getObjectFromId(i);

		assertEquals(a, a1);

		odb.close();

		odb = ODBFactory.open(f.toString());

		a1 = (Action)odb.getObjectFromId(i);

		assertNotNull(a1);
		assertEquals(a.getId(), a1.getId());
		assertEquals(a.getCreated(), a1.getCreated());
		assertEquals(a.getResolved(), a1.getResolved());
		assertEquals(a.getDescription(), a1.getDescription());
		assertTrue(a.getResolution() == a1.getResolution());
		assertTrue(a.getResolution().equals(a1.getResolution()));
		assertEquals(a.getResolution().ordinal(), a1.getResolution().ordinal());

		odb.store(a);

		Objects<Action> o = odb.getObjects(Action.class);

		assertEquals(2, o.size());

		a.setDescription("desc2");

		OID i1 = odb.store(a);

		odb.close();
		odb = ODBFactory.open(f.toString());

		Action a2 = (Action)odb.getObjectFromId(i1);

		assertEquals("desc2", a2.getDescription());

		odb.close();
	}

	public void testTrigger() {
		f.delete();
		ODB odb = ODBFactory.open(f.toString());

		Bird b = new Bird("Duck", 10);
		odb.store(b);
		b.size = 11;
		odb.store(b);

		Flock fl = new Flock();
		fl.add(b);

		odb.store(fl);

		fl.add(new Bird("Lame Duck", 3));
		b.size = 12;
		odb.store(fl);

		Objects<Flock> o = odb.getObjects(Flock.class);
		assertEquals(1, o.size());

		odb.close();
		odb = ODBFactory.open(f.toString());
		odb.addSelectTrigger(Bird.class, new SelectTrigger() {

			@Override
			public void afterSelect(Object object, OID oid) {
				// System.out.println("created "+ooo.getObjectFromId(oid));
			}
		});

		o = odb.getObjects(com.gtdfree.test.ODBTest.Flock.class);
		System.out.println("flock");
		assertEquals(1, o.size());

		fl = o.getFirst();

		assertEquals(2, fl.size());
		assertEquals(12, fl.get(0).size);
		assertEquals(3, fl.get(1).size);

		odb.close();
	}

	public void testExportImport() {
		
		f.delete();
		ODB odb= ODBFactory.open(f.toString());
		
		Bird b= new Bird("Duck",10);
		
		odb.store(b);
		
		b.size=11;
		
		odb.store(b);
		
		Flock fl = new Flock();
		fl.add(b);

		odb.store(fl);

		fl.add(new Bird("Lame Duck", 3));
		b.size = 12;
		odb.store(fl);

		Objects<Flock> of = odb.getObjects(Flock.class);
		assertEquals(1, of.size());

		odb.commit();
		//odb= ODBFactory.open(f.toString());

		File ex= new File("export.xml");
		
		XMLExporter exp= new XMLExporter(odb);
		try {
			System.out.println(ex.toString()+" "+ex.getAbsoluteFile().getParent()+" "+ex.getName());
			exp.export(ex.getAbsoluteFile().getParent(), ex.getName());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		odb.close();
		
		f.delete();
		odb= ODBFactory.open(f.toString());
		
		//odb.store(new Flock());
		
		XMLImporter imp= new XMLImporter(odb);
		try {
			imp.importFile(ex.getAbsoluteFile().getParent(), ex.getName());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
		odb.close();
		
		odb= ODBFactory.open(f.toString());

		Objects<Bird> ob= odb.getObjects(Bird.class);
		assertEquals(2, ob.size());
		
		Bird b1= ob.getFirst();
		assertEquals(12, b1.size);


		of = odb.getObjects(com.gtdfree.test.ODBTest.Flock.class);
		assertEquals(1, of.size());

		fl = of.getFirst();

		assertEquals(2, fl.size());
		assertEquals(12, fl.get(0).size);
		assertEquals(3, fl.get(1).size);

		odb.close();
	}
}
