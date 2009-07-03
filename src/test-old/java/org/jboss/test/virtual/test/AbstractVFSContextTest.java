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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.test.virtual.support.MockVirtualFileHandlerVisitor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * AbstractVFSContextTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVFSContextTest extends AbstractVFSTest
{
   public AbstractVFSContextTest(String name)
   {
      super(name);
   }
   
   protected abstract VFSContext getVFSContext(String name) throws Exception;

   protected abstract VFSContext getParentVFSContext() throws Exception;

   protected abstract String getSuffix();

   protected abstract String getRealProtocol();

   protected abstract String getRealURLEnd();

   protected abstract String transformExpectedEnd(String expecetedEnd);

   protected abstract boolean isRealURLSupported();

   protected abstract boolean isArchive();

   /* TODO URI testing
   public void testRootURI() throws Exception
   {
   }
   */

   public void testRealURL() throws Exception
   {
      try
      {
         assertRealURL("children", null, null);
         assertRealURL("children", "child1", null);
         assertRealURL("complex", null, null);
         assertRealURL("complex", "subfolder", null);
         assertRealURL("complex", "subfolder/subchild", null);
         assertRealURL("complex", "subfolder/subsubfolder", null);
         assertRealURL("complex", "subfolder/subsubfolder/subsubchild", null);
         assertRealURL("nested", null, null);
         assertRealURL("nested", "complex.jar", null);
         assertRealURL("nested", "complex.jar/subfolder", "complex.jar");
         assertRealURL("nested", "complex.jar/subfolder/subchild", "complex.jar");
         assertRealURL("nested", "complex.jar/subfolder/subsubfolder", "complex.jar");
         assertRealURL("nested", "complex.jar/subfolder/subsubfolder/subsubchild", "complex.jar");

         assertTrue(isRealURLSupported());
      }
      catch (Throwable t)
      {
         assertFalse(t.getMessage(), isRealURLSupported());
      }
   }

   @SuppressWarnings("deprecation")
   public void assertRealURL(String name, String path, String expectedEnd) throws Exception
   {
      VFSContext context = getVFSContext(name);
      VirtualFile root = context.getRoot().getVirtualFile();
      VirtualFile file = root;
      if (path != null && path.length() > 0)
         file = root.findChild(path);

      URL realURL = VFSUtils.getRealURL(file);
      String realURLString = realURL.toExternalForm();

      URL rootURL = root.toURL();
      String rootURLString = rootURL.toExternalForm();
      int p = rootURLString.indexOf(":/");
      int l = rootURLString.length() - 1;
      if (rootURLString.charAt(l - 1) == '!')
         l--;
      String middle = rootURLString.substring(p, l);
      String end;
      expectedEnd = transformExpectedEnd(expectedEnd);
      if (expectedEnd == null)
      {
         end = (path != null) ? path : "";
      }
      else
      {
         end = expectedEnd;
      }

      String expectedRealURL = getRealProtocol() + middle + getRealURLEnd() + end;
      if (expectedRealURL.endsWith("/") && realURLString.endsWith("/") == false)
         realURLString += "/";
      if (expectedRealURL.endsWith("/") == false && realURLString.endsWith("/"))
         expectedRealURL += "/";

      assertEquals("Different real URL:", expectedRealURL, realURLString);
   }

   public void testGetVFS() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      URI rootURI = context.getRootURI();
      VFS vfs = context.getVFS();
      VirtualFile rootFile = vfs.getRoot();

      URI uri = new URI("vfs" + rootURI);
      URI rfUri = rootFile.toURI();
      assertEquals(uri.getPath(), rfUri.getPath());
   }
   
   public void testGetRoot() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      URI rootURI = context.getRootURI();
      VirtualFileHandler rootHandler = context.getRoot(); 
      VFS vfs = context.getVFS();
      VirtualFile rootFile = vfs.getRoot();
      
      assertEquals(rootURI, rootHandler.toURI());
      assertEquals(rootHandler.getVirtualFile(), rootFile);
   }
   
   /* TODO getOptions
   public void testGetOptions() throws Exception
   {
   }
   */
   
   public void testGetChildren() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      List<VirtualFileHandler> children = context.getChildren(root, false);
      
      Set<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");

      Set<String> actual = new HashSet<String>();
      for (VirtualFileHandler child : children)
      {
         if (child.getName().startsWith("META-INF") == false && child.getName().equals(".svn") == false)
            actual.add(child.getName());
      }
      
      assertEquals(expected, actual);
   }

   public void testGetChildrenNullFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      try
      {
         context.getChildren(null, false);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildRoot() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "");
      assertEquals(root, found);
   }

   public void testFindChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "child");
      assertEquals("child", found.getPathName());
   }

   public void testFindChildSubFolder() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "subfolder");
      assertEquals("subfolder", found.getPathName());
   }

   public void testFindChildSubChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "subfolder/subchild");
      assertEquals("subfolder/subchild", found.getPathName());
   }

   public void testFindChildDoesNotExist() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         assertNull(context.getChild(root, "doesnotexist"));
      }
      catch (Throwable t)
      {
         checkThrowableTemp(IOException.class, t);
      }
   }

   public void testFindChildNullFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      try
      {
         context.getChild(null, "");
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildNullPath() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         context.getChild(root, null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testIsArchive() throws Exception
   {
      VFSContext context = getVFSContext("nested");

      VirtualFileHandler root = context.getRoot();
      assertEquals(isArchive(), root.isArchive());

      VirtualFileHandler complex = root.getChild("complex.jar");
      assertNotNull(complex);
      assertEquals(isArchive(), complex.isArchive());

      VirtualFileHandler subfolder = complex.getChild("subfolder");
      assertNotNull(subfolder);
      assertFalse(subfolder.isArchive());

      VirtualFileHandler subchild = subfolder.getChild("subchild");
      assertNotNull(subchild);
      assertFalse(subchild.isArchive());

      VirtualFileHandler subsubfolder = subfolder.getChild("subsubfolder");
      assertNotNull(subsubfolder);
      assertFalse(subsubfolder.isArchive());

      VirtualFileHandler subsubchild = subsubfolder.getChild("subsubchild");
      assertNotNull(subsubchild);
      assertFalse(subsubchild.isArchive());
   }

   public void testSpecialTokensOnLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler leaf = root.getChild("child");
      assertTrue(leaf.isLeaf());
      assertNotNull(leaf.getChild(".."));
      assertNotNull(leaf.getChild("."));
      leaf = root.getChild("subfolder/subchild");
      assertTrue(leaf.isLeaf());
      assertNotNull(leaf.getChild(".."));
      assertNotNull(leaf.getChild("."));
   }

   public void testSimpleReversePath() throws Exception
   {
      checkSpecialPath("simple" + getSuffix() + "/../complex" + getSuffix() + "/subfolder/subsubfolder/../subchild", "subchild");
   }

   public void testComplexReversePath() throws Exception
   {
      checkSpecialPath("complex" + getSuffix() + "/../simple" + getSuffix() + "/child", "child");
   }

   public void testDirectOverTheTop() throws Exception
   {
      checkOverTheTop("..");
   }

   public void testMiddleOverTheTop() throws Exception
   {
      checkOverTheTop("complex" + getSuffix() + "/subfolder/../../../complex" + getSuffix() + "/subfolder");
   }

   protected void checkOverTheTop(String path) throws Exception
   {
      try
      {
         checkSpecialPath(path, null);
         fail("Should not be here.");
      }
      catch(Exception e)
      {
         checkThrowable(IOException.class, e);
      }
   }

   public void testCurrentAtTheStart() throws Exception
   {
      checkSpecialPath("./simple" + getSuffix() + "/child", "child");
      checkSpecialPath("./complex" + getSuffix() + "/subfolder/subchild", "subchild");
   }

   public void testCurrentInTheMiddle() throws Exception
   {
      checkSpecialPath("simple" + getSuffix() + "/./child", "child");
      checkSpecialPath("complex" + getSuffix() + "/./subfolder/subchild", "subchild");
   }

   public void testConcurrentCurrent() throws Exception
   {
      checkSpecialPath("././simple" + getSuffix() + "/././child", "child");
      checkSpecialPath("././complex" + getSuffix() + "/././subfolder/subchild", "subchild");
   }

   protected void checkSpecialPath(String path, String fileName) throws Exception
   {
      VFSContext context = getParentVFSContext();
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.getChild(root, path);
      assertNotNull(child);
      assertTrue(child.isLeaf());
      assertEquals(fileName, child.getName());
   }

   public void testVisit() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      MockVirtualFileHandlerVisitor visitor = new MockVirtualFileHandlerVisitor();
      context.visit(root, visitor);
      
      Set<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");

      Set<String> actual = new HashSet<String>();
      for (VirtualFileHandler child : visitor.getVisited())
      {
         if (child.getName().startsWith("META-INF") == false && child.getName().equals(".svn") == false)
            actual.add(child.getName());
      }
      
      assertEquals(expected, actual);
   }

   public void testVisitNullHandler() throws Exception
   {
      VFSContext context = getVFSContext("children");
      MockVirtualFileHandlerVisitor visitor = new MockVirtualFileHandlerVisitor();
      try
      {
         context.visit(null, visitor);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testVisitNullVisitor() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      try
      {
         context.visit(root, null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }
}
