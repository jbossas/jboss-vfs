/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test.virtual.test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.virtual.support.MockURLStreamHandler;
import org.jboss.test.virtual.support.MockVFSContext;
import org.jboss.test.virtual.support.MockVirtualFileFilter;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;

/**
 * VFSUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VFSUnitTestCase extends AbstractMockVFSTest
{
   public VFSUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(VFSUnitTestCase.class);
   }

   public void testGetVFSURI() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      MockVFSContext context2 = registerSimple2VFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      assertNotNull(vfs);
      assertEquals(context.getVFS(), vfs);
      
      VFS vfs2 = VFS.getVFS(context2.getRootURI());
      assertNotNull(vfs2);
      assertEquals(context2.getVFS(), vfs2);
   }

   public void testGetVFSURINull() throws Exception
   {
      try
      {
         VFS.getVFS((URI) null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVFSURINoFactory() throws Exception
   {
      try
      {
         URI uri = new URI("doesnotexist:///");
         VFS.getVFS(uri);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVFSURIIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      mockVFSContextFactory.setIOException("getVFSURI");

      try
      {
         VFS.getVFS(context.getRootURI());
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVFSURL() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      MockVFSContext context2 = registerSimple2VFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURL());
      assertNotNull(vfs);
      assertEquals(context.getVFS(), vfs);
      
      VFS vfs2 = VFS.getVFS(context2.getRootURL());
      assertNotNull(vfs2);
      assertEquals(context2.getVFS(), vfs2);
   }

   public void testGetVFSURLNull() throws Exception
   {
      try
      {
         VFS.getVFS((URL) null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVFSURLNoFactory() throws Exception
   {
      try
      {
         URL url = new URL("doesnotexist", "", 0, "", MockURLStreamHandler.INSTANCE);
         VFS.getVFS(url);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVFSURLIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      mockVFSContextFactory.setIOException("getVFSURL");

      try
      {
         VFS.getVFS(context.getRootURL());
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURI() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertNotNull(file);
      assertEquals(context.getRoot().getVirtualFile(), file);
   }

   public void testGetRootURINullURI() throws Exception
   {
      try
      {
         VFS.getRoot((URI) null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetRootURINoFactory() throws Exception
   {
      try
      {
         URI uri = new URI("doesnotexist:///");
         VFS.getRoot(uri);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURIIOExceptionGetVFS() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      mockVFSContextFactory.setIOException("getVFSURI");

      try
      {
         VFS.getRoot(context.getRootURI());
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURIIOExceptionGetRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.setIOException("getRoot");

      try
      {
         VFS.getRoot(context.getRootURI());
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURIRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VirtualFile file = VFS.getVirtualFile(context.getRootURI(), "");
      assertNotNull(file);
      assertEquals(context.getRoot().getVirtualFile(), file);
   }

   public void testGetVirtualFileURIChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VirtualFile file1 = VFS.getVirtualFile(context.getRootURI(), "child1");
      assertNotNull(file1);
      assertEquals(child1, file1);
      
      VirtualFile file2 = VFS.getVirtualFile(context.getRootURI(), "child2");
      assertNotNull(file2);
      assertEquals(child2, file2);
      
      VirtualFile file3 = VFS.getVirtualFile(context.getRootURI(), "child3");
      assertNotNull(file3);
      assertEquals(child3, file3);
   }

   public void testGetVirtualFileURINullURI() throws Exception
   {
      try
      {
         VFS.getVirtualFile((URI) null, "");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVirtualFileURINullPath() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS.getVirtualFile(context.getRootURI(), null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVirtualFileURINoFactory() throws Exception
   {
      try
      {
         URI uri = new URI("doesnotexist:///");
         VFS.getVirtualFile(uri, "");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURIDoesNotExist() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS.getVirtualFile(context.getRootURI(), "doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURIIOExceptionGetVFS() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      mockVFSContextFactory.setIOException("getVFSURI");

      try
      {
         VFS.getVirtualFile(context.getRootURI(), "child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURIIOExceptionFindChild() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("findChild");

      try
      {
         VFS.getVirtualFile(context.getRootURI(), "child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURL() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VirtualFile file = VFS.getRoot(context.getRootURL());
      assertNotNull(file);
      assertEquals(context.getRoot().getVirtualFile(), file);
   }

   public void testGetRootURLNullURL() throws Exception
   {
      try
      {
         VFS.getRoot((URL) null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetRootURLNoFactory() throws Exception
   {
      URL url = new URL("doesnotexist", "", 0, "", MockURLStreamHandler.INSTANCE);
      try
      {
         VFS.getRoot(url);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURLIOExceptionGetVFS() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      mockVFSContextFactory.setIOException("getVFSURL");

      URL url = context.getRootURL();
      try
      {
         VFS.getRoot(url);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRootURLIOExceptionGetRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.setIOException("getRoot");

      URL url = context.getRootURL();
      try
      {
         VFS.getRoot(url);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURLRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VirtualFile file = VFS.getVirtualFile(context.getRootURL(), "");
      assertNotNull(file);
      assertEquals(context.getRoot().getVirtualFile(), file);
   }

   public void testGetVirtualFileURLChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VirtualFile file1 = VFS.getVirtualFile(context.getRootURL(), "child1");
      assertNotNull(file1);
      assertEquals(child1, file1);
      
      VirtualFile file2 = VFS.getVirtualFile(context.getRootURL(), "child2");
      assertNotNull(file2);
      assertEquals(child2, file2);
      
      VirtualFile file3 = VFS.getVirtualFile(context.getRootURL(), "child3");
      assertNotNull(file3);
      assertEquals(child3, file3);
   }

   public void testGetVirtualFileURLNullURL() throws Exception
   {
      try
      {
         VFS.getVirtualFile((URL) null, "");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVirtualFileURLNullPath() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS.getVirtualFile(context.getRootURL(), null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetVirtualFileURLNoFactory() throws Exception
   {
      try
      {
         URL url = new URL("doesnotexist", "", 0, "", MockURLStreamHandler.INSTANCE);
         VFS.getVirtualFile(url, "");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURLIOExceptionGetVFS() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      mockVFSContextFactory.setIOException("getVFSURL");

      try
      {
         VFS.getVirtualFile(context.getRootURL(), "child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURLDoesNotExist() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS.getVirtualFile(context.getRootURL(), "doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetVirtualFileURLIOExceptionFindChild() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("findChild");

      try
      {
         VFS.getVirtualFile(context.getRootURL(), "child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      assertNotNull(vfs);

      VirtualFile root = vfs.getRoot();
      assertNotNull(root);
      
      assertEquals(context.getRoot().getVirtualFile(), root);
   }

   public void testGetRootIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.setIOException("getRoot");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      assertNotNull(vfs);

      try
      {
         vfs.getRoot();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testFindChildRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      
      assertFindChild(vfs, "", vfs.getRoot());
   }

   public void testFindChildChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      assertFindChild(vfs, "child1", child1);
      assertFindChild(vfs, "child2", child2);
      assertFindChild(vfs, "child3", child3);
   }

   public void testFindChildSubChildren() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child21 = getChildHandler(context, "child2/child2,1").getVirtualFile();
      VirtualFile child22 = getChildHandler(context, "child2/child2,2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      VirtualFile child31 = getChildHandler(context, "child3/child3,1").getVirtualFile();
      VirtualFile child32 = getChildHandler(context, "child3/child3,2").getVirtualFile();
      VirtualFile child33 = getChildHandler(context, "child3/child3,3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      assertFindChild(vfs, "child1", child1);
      assertFindChild(vfs, "child1/child1,1", child11);
      assertFindChild(vfs, "child2", child2);
      assertFindChild(vfs, "child2/child2,1", child21);
      assertFindChild(vfs, "child2/child2,2", child22);
      assertFindChild(vfs, "child3", child3);
      assertFindChild(vfs, "child3/child3,1", child31);
      assertFindChild(vfs, "child3/child3,2", child32);
      assertFindChild(vfs, "child3/child3,3", child33);
   }

   public void testFindChildNullPath() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS vfs = VFS.getVFS(context.getRootURI());
         vfs.findChild(null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testGetChildNullPath() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      try
      {
         VFS vfs = VFS.getVFS(context.getRootURI());
         vfs.getChild(null);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildSimpleDoesNotExist() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.findChild("doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
         assertNull(vfs.getChild("doesnotexist"));
      }
   }

   public void testFindChildStructuredDoesNotExist() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();

      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.findChild("child1/doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
         assertNull(vfs.getChild("child1/doesnotexist"));
      }
   }

   public void testFindChildIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("findChild");

      try
      {
         VFS vfs = VFS.getVFS(context.getRootURI());
         vfs.findChild("child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetChildIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChild");

      try
      {
         VFS vfs = VFS.getVFS(context.getRootURI());
         vfs.getChild("child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren();
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren();
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren();
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildren();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildren();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildrenWithNullFilter() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren(null);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenWithNullFilterStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren(null);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenWithNullFilterNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildren(null);
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenWithNullFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildren(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenWithNullFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildren(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildrenWithFilter() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildren(filter);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
      assertEquals(expected, filter.getVisited());
   }

   public void testGetAllChildrenWithFilterStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildren(filter);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
      assertEquals(expected, filter.getVisited());
   }

   public void testGetAllChildrenWithFilterNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildren(filter);
      assertNotNull(children);
      
      assertEmpty(children);
      assertEmpty(filter.getVisited());
   }

   public void testGetAllChildrenWithFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         vfs.getChildren(filter);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenWithFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         vfs.getChildren(filter);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildrenRecursively() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively();
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenRecursivelyStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child21 = getChildHandler(context, "child2/child2,1").getVirtualFile();
      VirtualFile child22 = getChildHandler(context, "child2/child2,2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      VirtualFile child31 = getChildHandler(context, "child3/child3,1").getVirtualFile();
      VirtualFile child32 = getChildHandler(context, "child3/child3,2").getVirtualFile();
      VirtualFile child33 = getChildHandler(context, "child3/child3,3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively();
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child11);
      expected.add(child2);
      expected.add(child21);
      expected.add(child22);
      expected.add(child3);
      expected.add(child31);
      expected.add(child32);
      expected.add(child33);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenRecursivelyNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively();
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenRecursivelyIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildrenRecursively();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenRecursivelyIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildrenRecursively();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildrenRecursivelyWithNullFilter() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively(null);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenRecursivelyWithNullFilterStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child21 = getChildHandler(context, "child2/child2,1").getVirtualFile();
      VirtualFile child22 = getChildHandler(context, "child2/child2,2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      VirtualFile child31 = getChildHandler(context, "child3/child3,1").getVirtualFile();
      VirtualFile child32 = getChildHandler(context, "child3/child3,2").getVirtualFile();
      VirtualFile child33 = getChildHandler(context, "child3/child3,3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively(null);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child11);
      expected.add(child2);
      expected.add(child21);
      expected.add(child22);
      expected.add(child3);
      expected.add(child31);
      expected.add(child32);
      expected.add(child33);
      
      assertEquals(expected, children);
   }

   public void testGetAllChildrenRecursivelyWithNullFilterNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      List<VirtualFile> children = vfs.getChildrenRecursively(null);
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenRecursivelyWithNullFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildrenRecursively(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenRecursivelyWithNullFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.getChildrenRecursively(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetAllChildrenRecursivelyWithFilter() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildrenRecursively(filter);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
      assertEquals(expected, filter.getVisited());
   }

   public void testGetAllChildrenRecursivelyWithFilterStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child21 = getChildHandler(context, "child2/child2,1").getVirtualFile();
      VirtualFile child22 = getChildHandler(context, "child2/child2,2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      VirtualFile child31 = getChildHandler(context, "child3/child3,1").getVirtualFile();
      VirtualFile child32 = getChildHandler(context, "child3/child3,2").getVirtualFile();
      VirtualFile child33 = getChildHandler(context, "child3/child3,3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildrenRecursively(filter);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child11);
      expected.add(child2);
      expected.add(child21);
      expected.add(child22);
      expected.add(child3);
      expected.add(child31);
      expected.add(child32);
      expected.add(child33);
      
      assertEquals(expected, children);
      assertEquals(expected, filter.getVisited());
   }

   public void testGetAllChildrenRecursivelyWithFilterNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = vfs.getChildrenRecursively(filter);
      assertNotNull(children);
      
      assertEmpty(children);
      assertEmpty(filter.getVisited());
   }

   public void testGetAllChildrenRecursivelyWithFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         vfs.getChildren(filter);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildrenRecursivelyWithFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         vfs.getChildrenRecursively(filter);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testVisitAllChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      vfs.visit(visitor);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, filter.getVisited());
   }

   public void testVisitAllChildrenStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      vfs.visit(visitor);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, filter.getVisited());
   }

   public void testVisitAllChildrenNoChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      vfs.visit(visitor);

      assertEmpty(filter.getVisited());
   }

   public void testVisitAllChildrenIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      try
      {
         vfs.visit(visitor);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testVisitAllChildrenNullVisitor() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      try
      {
         vfs.visit(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testVisitAllChildrenIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VFS vfs = VFS.getVFS(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      try
      {
         vfs.visit(visitor);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }
   
   public void testToString() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      VFS vfs = context.getVFS();
      
      assertEquals(context.toString(), vfs.toString());
   }
   
   public void testHashCode() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      VFS vfs = context.getVFS();
      
      assertEquals(context.hashCode(), vfs.hashCode());
   }
   
   public void testEquals() throws Exception
   {
      MockVFSContext context1 = createSimpleVFSContext();
      MockVFSContext context2 = createSimpleVFSContext();
      
      VFS vfs1 = context1.getVFS();
      VFS vfs2 = context2.getVFS();

      assertEquals(vfs1, vfs2);
      
      MockVFSContext context3 = createSimple2VFSContext();
      VFS vfs3 = context3.getVFS();

      assertFalse(vfs1.equals(vfs3));
      assertFalse(vfs2.equals(vfs3));

      assertFalse(vfs1.equals(null));

      assertFalse(vfs1.equals(new Object()));
   }
}
