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
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.gtdfree.model.Action;
import org.gtdfree.model.ActionEvent;
import org.gtdfree.model.ConsistencyException;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDDataODB;
import org.gtdfree.model.GTDDataXMLTools;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelListener;
import org.gtdfree.model.Priority;
import org.gtdfree.model.Project;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class ModelTest extends TestCase {
	
	class TestModelListener implements GTDModelListener {
		FolderEvent actionAdded;
		ActionEvent actionModified;
		FolderEvent actionRemoved;
		Folder folderAdded;
		FolderEvent folderModified;
		Folder folderRemoved;
		Folder orderChanges;
		
		public void elementAdded(FolderEvent a) {
			actionAdded= a;
		}
		public void elementModified(ActionEvent a) {
			actionModified=a;
		}
		public void elementRemoved(FolderEvent a) {
			actionRemoved=a;
		}
		public void folderAdded(Folder folder) {
			folderAdded=folder;
		}
		public void folderModified(FolderEvent folder) {
			folderModified=folder;
		}
		public void folderRemoved(Folder folder) {
			folderRemoved=folder;
		}
		public void orderChanged(Folder f) {
			orderChanges=f;
		}
	}

	private static final String BAD_DESC = "\"\"\"'''@2!#$%^&*(())\\||||}}{{P:;:::?.>M<nN.b.nbZ#u45g[]@@@///:$$$#@@@////–—‘’‚“”„†‡•…‰€™&#8211;&#8212;&#8216;&#8217;&#8218;&#8220;&#8221;&#8222;&#8224;&#8225;&#8226;&#8230;&#8240;&#8364;&#8482;";
	
	GTDModel gtdModel;
	Folder f1;
	Project p1;
	File testRoot= new File("src/test/resources");
	File testDir= new File(testRoot,"tmp");

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		
		tearDown();
		
		gtdModel= setUpGTDModel(0);
		
		f1= gtdModel.createFolder("F1", FolderType.ACTION);
		
		assertNotNull(f1);
		assertEquals(gtdModel, f1.getParent());
		assertEquals(f1, gtdModel.getFolder(f1.getId()));
		
		p1= (Project)gtdModel.createFolder("P1", FolderType.PROJECT);

		assertNotNull(p1);
		assertEquals(gtdModel, p1.getParent());
		assertEquals(p1, gtdModel.getProject(p1.getId()));
		assertEquals(p1, gtdModel.getFolder(p1.getId()));
		
		assertEquals(8, gtdModel.size());
		
		Action a1= gtdModel.createAction(f1, "A1");
		Action a2= gtdModel.createAction(f1, "A2");
		Action a3= gtdModel.createAction(f1, "A3");
		Action a4= gtdModel.createAction(f1, "A4");
		Action a5= gtdModel.createAction(f1, "A5");
		
		assertEquals(5, f1.size());
		assertEquals(0, p1.size());
		assertEquals(a5, f1.get(0));
		assertEquals(a4, f1.get(1));
		assertEquals(a3, f1.get(2));
		assertEquals(a2, f1.get(3));
		assertEquals(a1, f1.get(4));
		
		a1.setProject(p1.getId());
		a2.setProject(p1.getId());
		a3.setProject(p1.getId());

		assertEquals(p1.getId(), (int)a1.getProject());
		assertEquals(p1.getId(), (int)a2.getProject());
		assertEquals(p1.getId(), (int)a3.getProject());
		assertEquals(3, p1.size());
		assertEquals(a1, p1.get(0));
		assertEquals(a2, p1.get(1));
		assertEquals(a3, p1.get(2));
		
		a2.setQueued(true);
		a3.setQueued(true);
		a4.setQueued(true);

		assertTrue(a2.isQueued());
		assertTrue(a3.isQueued());
		assertTrue(a4.isQueued());
		assertEquals(3, gtdModel.getQueue().size());
		assertEquals(a2, gtdModel.getQueue().get(0));
		assertEquals(a3, gtdModel.getQueue().get(1));
		assertEquals(a4, gtdModel.getQueue().get(2));
		
		URL url= new URL("http://gtd-free.sourceforge.net/");
		a1.setUrl(url);
		
		assertNotNull(a1.getUrl());
		assertEquals(url.toString(), a1.getUrl().toString());
		
		a2.setDescription(BAD_DESC);
		
		checkConsistency(gtdModel);
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		tearDownGTDModel(0, gtdModel);
		tearDownGTDModel(1, null);
		tearDownGTDModel(2, null);
		
		File[] f= testDir.listFiles();
		for (int i = 0; i < f.length; i++) {
			if (f[i].isFile()) {
				f[i].delete();
			}
		}
	}
	
	public GTDModel setUpGTDModel(@SuppressWarnings("unused") int i) throws IOException {
		return new GTDModel(null);
	}

	public void tearDownGTDModel(@SuppressWarnings("unused") int i, GTDModel m) {
		if (m!=null) {
			try {
				m.getDataRepository().close(true);
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}
	}

	public void testProjectChange() {
		
		TestModelListener ml= new TestModelListener();
		
		gtdModel.addGTDModelListener(ml);
		
		Action a1= f1.get(0);
		
		assertNotNull(a1);
		
		a1.setProject(p1.getId());
		
		assertNotNull(a1.getProject());
		assertEquals(p1.getId(), (int)a1.getProject());
		assertEquals(4, p1.size());
		assertEquals(a1, p1.get(3));
		
		assertNotNull(ml.actionModified);
		assertEquals(a1,ml.actionModified.getAction());
		assertEquals("project",ml.actionModified.getProperty());
		assertEquals(null, ml.actionModified.getOldValue());
		assertEquals(a1.getProject(), ml.actionModified.getNewValue());
	
		assertNotNull(ml.actionAdded);
		assertEquals(a1,ml.actionAdded.getAction());
		assertEquals(p1,ml.actionAdded.getFolder());
		
		checkConsistency(gtdModel);
	}
	
	public void testRemindBuildIn() {
		
		TestModelListener ml= new TestModelListener();
		
		gtdModel.addGTDModelListener(ml);
		
		Date d= new Date();
		
		Action a1= f1.get(0);
		
		assertNotNull(a1);
		
		a1.setRemind(d);
		
		assertEquals(d, a1.getRemind());
		
		assertNotNull(ml.actionModified);
		assertEquals(a1,ml.actionModified.getAction());
		assertEquals("remind",ml.actionModified.getProperty());
		assertEquals(null, ml.actionModified.getOldValue());
		assertEquals(d, ml.actionModified.getNewValue());
	
		assertNotNull(ml.actionAdded);
		assertEquals(a1,ml.actionAdded.getAction());
		assertEquals(gtdModel.getRemindFolder(),ml.actionAdded.getFolder());
		
		assertEquals(1, gtdModel.getRemindFolder().size());
		assertEquals(1, gtdModel.getRemindFolder().getOpenCount());
		
		a1.setResolution(Resolution.RESOLVED);
		
		assertEquals(1, gtdModel.getRemindFolder().size());
		assertEquals(0, gtdModel.getRemindFolder().getOpenCount());
		
		checkConsistency(gtdModel);
	}

	public void testPriorityBuildIn() {
		
		TestModelListener ml= new TestModelListener();
		
		gtdModel.addGTDModelListener(ml);
		
		Priority pr= Priority.Medium;
		
		Action a1= f1.get(0);
		
		assertNotNull(a1);
		assertNotNull(a1.getPriority());
		assertEquals(Priority.None, a1.getPriority());
		
		a1.setPriority(pr);
		
		assertEquals(pr, a1.getPriority());
		
		assertNotNull(ml.actionModified);
		assertEquals(a1,ml.actionModified.getAction());
		assertEquals("priority",ml.actionModified.getProperty());
		assertEquals(Priority.None, ml.actionModified.getOldValue());
		assertEquals(pr, ml.actionModified.getNewValue());
	
		assertNotNull(ml.actionAdded);
		assertEquals(a1,ml.actionAdded.getAction());
		assertEquals(gtdModel.getPriorityFolder(),ml.actionAdded.getFolder());
		
		assertEquals(1, gtdModel.getPriorityFolder().size());
		assertEquals(1, gtdModel.getPriorityFolder().getOpenCount());
		
		a1.setResolution(Resolution.RESOLVED);
		
		assertEquals(1, gtdModel.getPriorityFolder().size());
		assertEquals(0, gtdModel.getPriorityFolder().getOpenCount());


		ml= new TestModelListener();
		gtdModel.addGTDModelListener(ml);
		pr= Priority.High;
		a1= f1.get(1);
		
		assertNotNull(a1);
		assertNotNull(a1.getPriority());
		assertEquals(Priority.None, a1.getPriority());
		
		a1.setPriority(pr);
		
		assertEquals(pr, a1.getPriority());
		
		assertNotNull(ml.actionModified);
		assertEquals(a1,ml.actionModified.getAction());
		assertEquals("priority",ml.actionModified.getProperty());
		assertEquals(Priority.None, ml.actionModified.getOldValue());
		assertEquals(pr, ml.actionModified.getNewValue());
	
		assertNotNull(ml.actionAdded);
		assertEquals(a1,ml.actionAdded.getAction());
		assertEquals(gtdModel.getPriorityFolder(),ml.actionAdded.getFolder());
		
		assertEquals(2, gtdModel.getPriorityFolder().size());
		assertEquals(1, gtdModel.getPriorityFolder().getOpenCount());

		ml= new TestModelListener();
		gtdModel.addGTDModelListener(ml);

		a1.setPriority(Priority.None);
		
		assertEquals(Priority.None, a1.getPriority());
		
		assertNotNull(ml.actionModified);
		assertEquals(a1,ml.actionModified.getAction());
		assertEquals("priority",ml.actionModified.getProperty());
		assertEquals(Priority.None, ml.actionModified.getNewValue());
		assertEquals(pr, ml.actionModified.getOldValue());
	
		assertNotNull(ml.actionRemoved);
		assertEquals(a1,ml.actionRemoved.getAction());
		assertEquals(gtdModel.getPriorityFolder(),ml.actionRemoved.getFolder());

		assertEquals(1, gtdModel.getPriorityFolder().size());
		assertEquals(0, gtdModel.getPriorityFolder().getOpenCount());

		checkConsistency(gtdModel);
	}

	public void testImport() {
		File f=null;
		try {
		
			GTDModel m= setUpGTDModel(1);
			Folder f1= m.createFolder("F1", FolderType.ACTION);
			f1.setClosed(true);
	
			f= new File(testDir,"test.xml");
			gtdModel.exportXML(f);
			GTDDataXMLTools.importFile(m,f);
			checkConsistency(m);
			assertContentEquals(gtdModel,m);
			
			if (f!=null) {
				f.delete();
			}
			
			tearDownGTDModel(1, m);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testLoad() {
		File f=null;
		try {
		
			GTDModel m= setUpGTDModel(1);
	
			f= new File(testDir,"test.xml");
			
			gtdModel.exportXML(f);
			
			GTDDataXMLTools.importFile(m,f);
			
			checkConsistency(m);

			assertContentEquals(gtdModel,m);
			

			tearDownGTDModel(1, m);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (f!=null) {
				f.delete();
			}
		}
	}

	public void testLoadODB() {
		File f=null;
		File fodb= new File(testDir,"test.odb");
		File fodbx= new File(testDir,"test.odb-xml");
		try {
		
			fodb.delete();
			fodbx.delete();
			
			GTDDataODB odb= new GTDDataODB(fodb);
			GTDModel m= odb.restore();
			odb.store();
			odb.close(true);
	
			odb= new GTDDataODB(fodb);
			m= odb.restore();
			checkConsistency(m);
			odb.store();
			odb.close(true);

			f= new File(testDir,"test.xml");
			f.delete();

			odb= new GTDDataODB(fodb);
			m= odb.restore();
			gtdModel.exportXML(f);
			GTDDataXMLTools.load(m,f);
			
			checkConsistency(m);
			assertContentEquals(gtdModel,m);
			
			odb.store();
			odb.exportODB(fodbx);
			odb.close(true);
			
			odb= new GTDDataODB(fodb);
			m= odb.restore();
			
			checkConsistency(m);
			assertContentEquals(gtdModel,m);

			odb.close(false);
			fodb.delete();
			
			odb= new GTDDataODB(fodb);
			m= odb.restore();
			
			assertEquals(gtdModel.size()-2, m.size());
			odb.importODB(fodbx);
			
			odb= new GTDDataODB(fodb);
			m= odb.restore();
			checkConsistency(m);
			assertContentEquals(gtdModel,m);
			
			odb.close(false);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		} finally {
			if (f!=null) {
				f.delete();
			}
		}
	}

	public void testLoadWIN1250() {
		File f= new File(testRoot,"gtd-free-data_WIN1250_2.1.xml");
		File f1= new File(testRoot,"gtd-free-data1.xml");

		try {
			
		
			GTDModel m1= setUpGTDModel(1);
			GTDModel m2= setUpGTDModel(2);
	
			GTDDataXMLTools.load(m1,f);
			
			checkConsistency(m1);
			
			m1.exportXML(f1);
			
			GTDDataXMLTools.load(m2,f1);
			
			checkConsistency(m2);

			assertContentEquals(m1,m2);
			
			tearDownGTDModel(1, m1);
			tearDownGTDModel(2, m2);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			f1.delete();
		}
	}

	public void testLoadVsImport() {

		File f= new File(testDir,"gtd-free-data.testLoadVsImport.xml");
		
		try {
			
			gtdModel.exportXML(f);
		
			GTDModel m1= setUpGTDModel(1);
			GTDModel m2= setUpGTDModel(2);
	
			GTDDataXMLTools.load(m1,f);
			GTDDataXMLTools.importFile(m2,f);
			
			checkConsistency(m1);
			checkConsistency(m2);

			assertContentEquals(gtdModel, m1);
			assertContentEquals(gtdModel, m2);
			assertContentEquals(m1,m2);
			
			tearDownGTDModel(1, m1);
			tearDownGTDModel(2, m2);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			f.delete();
		}
	}

	public void testLoadSaveLoad() {
		
		File f= new File(testDir,"gtd-free-data.testLoadSaveLoad.xml");
		File f1= new File(testDir,"gtd-free-data.testLoadSaveLoad1.xml");

		try {
			
			gtdModel.exportXML(f);
		
			GTDModel m1= setUpGTDModel(1);
			GTDModel m2= setUpGTDModel(2);
	
			GTDDataXMLTools.load(m1,f);
			checkConsistency(m1);
			assertContentEquals(gtdModel, m1);
			
			m1.exportXML(f1);
			GTDDataXMLTools.load(m2,f1);
			
			checkConsistency(m2);

			assertContentEquals(m1,m2);
			
			tearDownGTDModel(1, m1);
			tearDownGTDModel(2, m2);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			f.delete();
			f1.delete();
			
		}
	}

	public void testSaveLoad() {
		File f= new File(testDir,"gtd-free-data-s.xml");

		try {

			gtdModel.exportXML(f);
		
			GTDModel m1= setUpGTDModel(1);
	
			GTDDataXMLTools.load(m1,f);
			checkConsistency(m1);
			assertContentEquals(gtdModel, m1);
	
			assertEquals(BAD_DESC, m1.getFolder(f1.getId()).get(3).getDescription());
			
			assertContentEquals(gtdModel,m1);
			
			tearDownGTDModel(1, m1);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			f.delete();
		}
	}

	public void test21vs20() {
		File f= new File(testRoot,"gtd-free-data_2.0.xml");
		File f1= new File(testRoot,"test.xml");

		try {
			
			GTDModel m1= setUpGTDModel(1);
			GTDModel m2= setUpGTDModel(2);
	
			GTDDataXMLTools.load(m1,f);
			checkConsistency(m1);
			
			m1.exportXML(f1);
			
			GTDDataXMLTools.load(m2,f1);
			
			checkConsistency(m2);

			assertContentEquals(m1,m2);
			
			tearDownGTDModel(1, m1);
			tearDownGTDModel(2, m2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			f1.delete();
		}
	}

	protected void assertContentEquals(GTDModel m1, GTDModel m2) {
		assertContentEquals(m1, m2, true, true);
	}
	protected void assertContentEquals(GTDModel m1, GTDModel m2, boolean matchActions, boolean matchProject) {
		assertEquals(m1.size(), m2.size());
		assertEquals(m1.toProjectsArray().length,m2.toProjectsArray().length);
		assertEquals(m1.toFoldersArray().length,m2.toFoldersArray().length);
		
		Folder[] ff2= m2.toFoldersArray();
		Folder[] ff1= m1.toFoldersArray();
		
		Map<String, Folder> m= new HashMap<String, Folder>();
		
		for (int i = 0; i < ff2.length; i++) {
			m.put(ff2[i].getName(), ff2[i]);
		}
		
		for (int j = 0; j < ff1.length; j++) {
			Folder f1= ff1[j];
			Folder f2= m.get(f1.getName());
			
			assertEquals(f1.getName(), f2.getName());
/*			if (f1.getName().equals("ANKA 2008")) {
				System.out.println(f1);
				for (Action action : f1) {
					System.out.println(" >"+action.getId()+" "+action.getDescription());
				}
				System.out.println(f2);
				for (Action action : f2) {
					System.out.println(" >"+action.getId()+" "+action.getDescription());
				}
			}
*/			
			assertEquals(f1.getType(), f2.getType());
			if (matchProject || !f1.isProject()) {
				assertEquals(f1.getName(),f1.size(), f2.size());
			}

			if (matchActions) {
				if (!f1.isProject() && f1.getId()!=m1.getResolvedFolder().getId() && f1.getId()!=m1.getDeletedFolder().getId()) {
					for (int i = 0; i < f1.size(); i++) {
						Action a1= f1.get(i);
						Action a2= f2.get(i);
						assertEquals(f1.getName(),a1.getDescription(), a2.getDescription());
						assertEquals(f1.getName(),a1.getUrl(), a2.getUrl());
						assertEquals(f1.getName(),a1.getPriority(), a2.getPriority());
						assertEquals(f1.getName(),a1.getRemind(), a2.getRemind());
						if (a1.getProject()!=null) {
							assertNotNull(a2.getProject());
							assertEquals(m1.getProject(a1.getProject()).getName(), m2.getProject(a2.getProject()).getName());
						}
					}
				}
			}
		}
	}
	
	public void testMove() {
		try {
			
			Folder f= f1;
			
			assertNotNull(f);
			
			assertEquals(5, f.size());
			assertEquals("A5", f.get(0).getDescription());
			assertEquals("A4", f.get(1).getDescription());
			assertEquals("A3", f.get(2).getDescription());
			assertEquals(BAD_DESC, f.get(3).getDescription());
			assertEquals("A1", f.get(4).getDescription());

			assertEquals(false, f.get(0).canMoveUp());
			assertEquals(true, f.get(0).canMoveDown());
			
			f.get(0).moveDown();
			
			assertEquals("A4", f.get(0).getDescription());
			assertEquals("A5", f.get(1).getDescription());
			assertEquals("A3", f.get(2).getDescription());
			assertEquals(BAD_DESC, f.get(3).getDescription());
			assertEquals("A1", f.get(4).getDescription());
			
			f.get(1).moveDown();
			
			assertEquals("A4", f.get(0).getDescription());
			assertEquals("A3", f.get(1).getDescription());
			assertEquals("A5", f.get(2).getDescription());
			assertEquals(BAD_DESC, f.get(3).getDescription());
			assertEquals("A1", f.get(4).getDescription());

			f.get(2).moveDown();
			f.get(3).moveDown();

			assertEquals("A4", f.get(0).getDescription());
			assertEquals("A3", f.get(1).getDescription());
			assertEquals(BAD_DESC, f.get(2).getDescription());
			assertEquals("A1", f.get(3).getDescription());
			assertEquals("A5", f.get(4).getDescription());

			assertEquals(true, f.get(4).canMoveUp());
			assertEquals(false, f.get(4).canMoveDown());

			f.get(4).moveUp();
			
			assertEquals("A4", f.get(0).getDescription());
			assertEquals("A3", f.get(1).getDescription());
			assertEquals(BAD_DESC, f.get(2).getDescription());
			assertEquals("A5", f.get(3).getDescription());
			assertEquals("A1", f.get(4).getDescription());

			f.get(3).moveUp();
			
			assertEquals("A4", f.get(0).getDescription());
			assertEquals("A3", f.get(1).getDescription());
			assertEquals("A5", f.get(2).getDescription());
			assertEquals(BAD_DESC, f.get(3).getDescription());
			assertEquals("A1", f.get(4).getDescription());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	protected void checkConsistency(GTDModel m) {
		
		try {
			GTDModel.checkConsistency(m,null,true,false);
		} catch (ConsistencyException e) {
			e.printStackTrace();
			//System.out.println(e.getActions()[0]);
			fail(e.toString());
		}
		
		try {
			m.getDataRepository().checkConsistency(null, true, false);
		} catch (ConsistencyException e) {
			e.printStackTrace();
			//System.out.println(e.getActions()[0]);
			fail(e.toString());
		}
	}

}
