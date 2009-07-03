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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.virtual.support.MockVFSContext;
import org.jboss.test.virtual.support.MockVirtualFileFilter;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;

/**
 * VirtualFileUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VirtualFileUnitTestCase extends AbstractMockVFSTest
{
   public VirtualFileUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(VirtualFileUnitTestCase.class);
   }

   public void testGetNameRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      URI uri = context.getRootURI();
      assertGetName(uri, "");
   }

   public void testGetNameChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      URI uri = context.getRootURI();
      assertGetName(uri, "child1");
      assertGetName(uri, "child2");
      assertGetName(uri, "child3");
   }

   public void testGetNameSubChildren() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      
      URI uri = context.getRootURI();
      assertGetName(uri, "child1/child1,1", "child1,1");
      assertGetName(uri, "child2/child2,1", "child2,1");
      assertGetName(uri, "child2/child2,2", "child2,2");
      assertGetName(uri, "child3/child3,1", "child3,1");
      assertGetName(uri, "child3/child3,2", "child3,2");
      assertGetName(uri, "child3/child3,3", "child3,3");
   }

   public void testGetPathNameRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      URI uri = context.getRootURI();
      assertGetPathName(uri, "");
   }

   public void testGetPathNameChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      URI uri = context.getRootURI();
      assertGetPathName(uri, "");
      assertGetPathName(uri, "child1");
      assertGetPathName(uri, "child2");
      assertGetPathName(uri, "child3");
   }

   public void testGetPathNameSubChildren() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      
      URI uri = context.getRootURI();
      assertGetPathName(uri, "");
      assertGetPathName(uri, "child1");
      assertGetPathName(uri, "child1/child1,1");
      assertGetPathName(uri, "child2");
      assertGetPathName(uri, "child2/child2,1");
      assertGetPathName(uri, "child2/child2,2");
      assertGetPathName(uri, "child3");
      assertGetPathName(uri, "child3/child3,1");
      assertGetPathName(uri, "child3/child3,2");
      assertGetPathName(uri, "child3/child3,3");
   }

   public void testToURI() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      URI uri = context.getRootURI();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      
      VirtualFile root = VFS.getRoot(uri);
      assertEquals(uri, root.toURI());

      VirtualFile found1 = root.findChild("child1");
      assertEquals(child1.toURI(), found1.toURI());

      VirtualFile found11 = root.findChild("child1/child1,1");
      assertEquals(child11.toURI(), found11.toURI());
   }

   public void testToURL() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      URL url = context.getRootURL();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child11 = getChildHandler(context, "child1/child1,1").getVirtualFile();
      
      VirtualFile root = VFS.getRoot(url);
      assertEquals(url, root.toURL());

      VirtualFile found1 = root.findChild("child1");
      assertEquals(child1.toURL(), found1.toURL());

      VirtualFile found11 = root.findChild("child1/child1,1");
      assertEquals(child11.toURL(), found11.toURL());
   }

   public void testGetLastModfied() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLastModified(12345l);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(12345l, file.getLastModified());

      context.getMockRoot().setLastModified(67890l);
      assertEquals(67890l, file.getLastModified());
   }

   public void testGetLastModfiedIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("getLastModified");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getLastModified();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetLastModfiedClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.getLastModified();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
   
   public void testGetSize() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setSize(12345l);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(12345l, file.getSize());

      context.getMockRoot().setSize(67890l);
      assertEquals(67890l, file.getSize());
   }

   public void testGetSizeIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("getSize");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getSize();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetSizeClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.getSize();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(false, file.isLeaf());

      context.getMockRoot().setLeaf(true);
      assertEquals(true, file.isLeaf());
   }

   public void testIsLeafIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("isLeaf");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.isLeaf();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testIsLeafClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.isLeaf();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
   
   public void testIsHidden() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setHidden(true);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(true, file.isHidden());

      context.getMockRoot().setHidden(false);
      assertEquals(false, file.isHidden());
   }

   public void testIsHiddenIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("isHidden");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.isHidden();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testIsHiddenClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.isHidden();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testOpenStream() throws Exception
   {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setStream(bytes);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      InputStream stream = file.openStream();
      byte[] buffer = new byte[bytes.length];
      stream.read(buffer);
      
      assertTrue(stream.read() == -1);
      assertTrue(Arrays.equals(bytes, buffer));
   }

   public void testOpenStreamIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("openStream");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.openStream();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testOpenStreamClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.openStream();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testCloseStreams() throws Exception
   {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setStream(bytes);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      InputStream stream = file.openStream();
      assertEquals(1, stream.read());
      
      file.closeStreams();
      assertEquals(-1, stream.read());
   }

   public void testCloseStreamViaClose() throws Exception
   {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setStream(bytes);

      VirtualFile file = VFS.getRoot(context.getRootURI());
      InputStream stream = file.openStream();
      assertEquals(1, stream.read());
      
      file.close();
      assertEquals(-1, stream.read());
   }

   public void testClose() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
   }
   
   public void testCloseDuplicate() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      file.close();
   }

   public void testGetVFS() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(context.getVFS(), file.getVFS());
   }

   public void testGetVFSClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.getVFS();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
   
   public void testGetParentRoot() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertNull(file.getParent());
   }
   
   public void testGetParentSimpleChild() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      VirtualFile root = VFS.getRoot(context.getRootURI());
      VirtualFile child = root.findChild("child1");
      VirtualFile parent = child.getParent();
      assertEquals(root, parent);
   }
   
   public void testGetParentStructuredChild() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();

      VirtualFile root = VFS.getRoot(context.getRootURI());
      VirtualFile child = root.findChild("child1");
      VirtualFile subChild = child.findChild("child1,1");
      VirtualFile parent = child.getParent();
      assertEquals(root, parent);
      parent = subChild.getParent();
      assertEquals(child, parent);
   }

   public void testGetParentIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setIOException("getParent");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getParent();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testGetParentClosed() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      file.close();
      try
      {
         file.getParent();
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetAllChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren();
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertChildrenOnLeaf(file);
   }

   public void testGetAllChildrenIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getChildren();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildren(null);
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenWithNullFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertChildrenOnLeaf(file, null);
   }

   public void testGetAllChildrenWithNullFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getChildren(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildren(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildren(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildren(filter);
      assertNotNull(children);
      
      assertEmpty(children);
      assertEmpty(filter.getVisited());
   }

   public void testGetAllChildrenWithFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      assertChildrenOnLeaf(file, filter);
   }

   public void testGetAllChildrenWithFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         file.getChildren(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively();
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenRecursivelyIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertChildrenOnLeafRecursively(file);
   }

   public void testGetAllChildrenIOExceptionRecursively() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getChildrenRecursively();
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      List<VirtualFile> children = file.getChildrenRecursively(null);
      assertNotNull(children);
      
      assertEmpty(children);
   }

   public void testGetAllChildrenRecursivelyWithNullFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertChildrenOnLeafRecursively(file, null);
   }

   public void testGetAllChildrenRecursivelyWithNullFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.getChildrenRecursively(null);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildrenRecursively(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildrenRecursively(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildrenRecursively(filter);
      assertNotNull(children);
      
      assertEmpty(children);
      assertEmpty(filter.getVisited());
   }

   public void testGetAllChildrenRecursivelyWithFilterIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      assertChildrenOnLeafRecursively(file, filter);
   }

   public void testGetAllChildrenRecursivelyWithFilterIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      try
      {
         file.getChildrenRecursively(filter);
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      List<VirtualFile> children = file.getChildren(filter);
      assertNotNull(children);
      
      List<VirtualFile> expected = new ArrayList<VirtualFile>();
      expected.add(child1);
      expected.add(child2);
      expected.add(child3);
      
      assertEquals(expected, children);
      assertEquals(expected, filter.getVisited());
   }

   public void testVisitAllChildrenStructured() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      file.visit(visitor);
      
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
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      file.visit(visitor);
      
      assertEmpty(filter.getVisited());
   }

   public void testVisitAllChildrenIsLeaf() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      file.visit(visitor);
      List<VirtualFile> matched = visitor.getMatched();
      assertNotNull(matched);
      assertEmpty(matched);
   }

   public void testVisitAllChildrenNullVisitor() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.visit(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testVisitChildrenIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChildren");
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      MockVirtualFileFilter filter = new MockVirtualFileFilter();
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter);
      try
      {
         file.visit(visitor);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }
   
   public void testFindChildSame() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      context.getMockRoot().setLeaf(false);
      
      VirtualFile root = VFS.getRoot(context.getRootURI());
      
      assertFindChild(root, "", root);
   }
   
   public void testFindChildChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      VirtualFile child1 = getChildHandler(context, "child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "child3").getVirtualFile();
      
      VirtualFile root = VFS.getRoot(context.getRootURI());
      
      assertFindChild(root, "", root);
      assertFindChild(root, "child1", child1);
      assertFindChild(root, "child2", child2);
      assertFindChild(root, "child3", child3);
   }

   public void testFindFolderChildren() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildrenAndNonLeafs();
      VirtualFile child1 = getChildHandler(context, "folder1/child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "folder2/child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "folder3/child3").getVirtualFile();

      VirtualFile root = VFS.getRoot(context.getRootURI());

      assertFindChild(root, "", root);
      assertFindChild(root, "folder1/child1", child1);
      assertFindChild(root, "folder2/child2", child2);
      assertFindChild(root, "folder3/child3", child3);
   }

   public void testFindSimpleReverse() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildrenAndNonLeafs();
      VirtualFile child1 = getChildHandler(context, "folder1/child1").getVirtualFile();
      VirtualFile child2 = getChildHandler(context, "folder2/child2").getVirtualFile();
      VirtualFile child3 = getChildHandler(context, "folder3/child3").getVirtualFile();

      VirtualFile root = VFS.getRoot(context.getRootURI());

      assertFindChild(root, "", root);
      assertFindChild(root, "folder2/../folder1/child1", child1);
      assertFindChild(root, "folder3/child1/../../folder2/child2", child2);
      assertFindChild(root, "./folder1/child1", child1);
      assertFindChild(root, "folder2/./child2", child2);
      assertFindChild(root, "././folder2/././child2", child2);
      try
      {
         assertFindChild(root, "../folder3/child3", child3);
      }
      catch (Exception e)
      {
         checkThrowable(IOException.class, e);
      }
      try
      {
         assertFindChild(root, "folder2/../../folder3/child3", child3);
      }
      catch (Exception e)
      {
         checkThrowable(IOException.class, e);
      }
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
      
      VirtualFile root = VFS.getRoot(context.getRootURI());
      
      assertFindChild(root, "", root);
      VirtualFile found1 = assertFindChild(root, "child1", child1);
      assertFindChild(root, "child1/child1,1", child11);
      assertFindChild(found1, "child1,1", child11);
      VirtualFile found2 = assertFindChild(root, "child2", child2);
      assertFindChild(root, "child2/child2,1", child21);
      assertFindChild(found2, "child2,1", child21);
      assertFindChild(root, "child2/child2,2", child22);
      assertFindChild(found2, "child2,2", child22);
      VirtualFile found3 = assertFindChild(root, "child3", child3);
      assertFindChild(root, "child3/child3,1", child31);
      assertFindChild(found3, "child3,1", child31);
      assertFindChild(root, "child3/child3,2", child32);
      assertFindChild(found3, "child3,2", child32);
      assertFindChild(root, "child3/child3,3", child33);
      assertFindChild(found3, "child3,3", child33);
   }

   public void testFindChildNullPath() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.findChild(null);
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

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.findChild("doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testFindChildStructuredDoesNotExist() throws Exception
   {
      MockVFSContext context = registerStructuredVFSContextWithSubChildren();

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.findChild("child1/doesnotexist");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }

   public void testFindChildIOException() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContextWithChildren();
      context.getMockRoot().setIOException("getChild");

      VirtualFile file = VFS.getRoot(context.getRootURI());
      try
      {
         file.findChild("child1");
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         checkThrowable(IOException.class, t);
      }
   }
   
   public void testToString() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      VirtualFileHandler handler = context.getRoot();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(handler.toString(), file.toString());
   }
   
   public void testHashCode() throws Exception
   {
      MockVFSContext context = registerSimpleVFSContext();
      VirtualFileHandler handler = context.getRoot();
      
      VirtualFile file = VFS.getRoot(context.getRootURI());
      assertEquals(handler.hashCode(), file.hashCode());
   }
   
   public void testEquals() throws Exception
   {
      MockVFSContext context1 = createSimpleVFSContext();
      MockVFSContext context2 = createSimpleVFSContext();
      
      VirtualFile file1 = context1.getVFS().getRoot();
      VirtualFile file2 = context2.getVFS().getRoot();

      assertEquals(file1, file2);
      
      MockVFSContext context3 = createSimple2VFSContext();
      VirtualFile file3 = context3.getVFS().getRoot();

      assertFalse(file1.equals(file3));
      assertFalse(file2.equals(file3));

      assertFalse(file1.equals(null));

      assertFalse(file1.equals(new Object()));
   }
}
