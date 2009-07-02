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

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.Test;
import org.jboss.virtual.VirtualFile;

/**
 * comment
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class AssembledContextTestCase extends AbstractVFSTest
{
   public AssembledContextTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(AssembledContextTestCase.class);
   }

   public void testRegex()
   {
      String[] files = {".java", "x.java", "FooBar.java"};
      String expression = "*.java";
      Pattern p = AssembledDirectory.getPattern(expression);
      System.out.println("pattern: " + p.pattern());
      for (String file : files)
      {
         assertTrue(p.matcher(file).matches());
      }
      System.out.println("no matches");
      p  = AssembledDirectory.getPattern("?.java");
      assertTrue(p.matcher("x.java").matches());
      assertFalse(p.matcher("xyz.java").matches());
      assertFalse(p.matcher(".java").matches());

      p = AssembledDirectory.getPattern("x?z*.java");
      assertTrue(p.matcher("xyz.java").matches());
      assertTrue(p.matcher("xyzasdfasdf.java").matches());
      assertFalse(p.matcher("xyzadasdfasdf").matches());
      assertFalse(p.matcher("xzadasdfasdf").matches());
      System.out.println("done it");
   }

   public void testAntMatching()
   {
      String file;
      String exp;
      file = "xabc/foobar/test.java";
      exp = "?abc/*/*.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "abc/foobar/test.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));
      file = "xabc/x/test.xml";
      assertFalse(AssembledDirectory.antMatch(file, exp));
      file = "xabc/test.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));


      exp = "org/jboss/Test.java";
      file = "org/jboss/Test.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));

      exp = "org/jboss/Test.java";
      file = "org/wrong.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));

      exp = "test/**";
      file = "test/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "test/foo/bar/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "x.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));

      exp = "**/CVS/*";
      file = "CVS/Repository";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/CVS/Entries";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/jakarta/tools/ant/CVS/Entries";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/CVS/foo/bar/Entries";
      assertFalse(AssembledDirectory.antMatch(file, exp));

      exp = "org/apache/jakarta/**";
      file ="org/apache/jakarta/tools/ant/docs/index.html";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file ="org/apache/jakarta/test.xml";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/xyz.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));

      exp = "org/apache/**/CVS/*";
      file ="org/apache/CVS/Entries";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file ="org/apache/jakarta/tools/ant/CVS/Entries";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/CVS/foo/bar/Entries";
      assertFalse(AssembledDirectory.antMatch(file, exp));
      file = "org/apache/nada/foo/bar/Entries";
      assertFalse(AssembledDirectory.antMatch(file, exp));

      exp = "**/test/**";
      file = "test/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "test/bar/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "test/bar/foo/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "foo/test/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "foo/bar/test/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "foo/test/bar/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "foo/bar/test/bar/foo/x.java";
      assertTrue(AssembledDirectory.antMatch(file, exp));
      file = "foo/bar/flah.java";
      assertFalse(AssembledDirectory.antMatch(file, exp));
   }

   public void testAddClass() throws Exception
   {
      AssembledDirectory directory = AssembledContextFactory.getInstance().create("foo.jar");
      directory.addClass(VirtualFile.class);


      List<VirtualFile> children = directory.getChildren();
      assertEquals(children.size(), 1);
      VirtualFile curr = children.get(0);
      System.out.println("test org/");
      assertEquals("org", curr.getName());

      System.out.println("test org/jboss");
      children = curr.getChildren();
      assertEquals(children.size(), 1);
      curr = children.get(0);
      assertEquals("jboss", curr.getName());

      System.out.println("test org/jboss/virtual");
      children = curr.getChildren();
      assertEquals(children.size(), 1);
      curr = children.get(0);
      assertEquals("virtual", curr.getName());
      children = curr.getChildren();
      boolean found;
      found = false;
      for (VirtualFile child: children)
      {
         if (child.getName().equals("VirtualFile.class"))
         {
            found = true;
            assertEquals("org/jboss/virtual/VirtualFile.class", child.getPathName());
            break;
         }
      }
      assertTrue("VirtualFile.class was found", found);
   }

   public void testAddResources() throws Exception
   {
      // Find test.classes.url location for vfs/links/war1.vfslink.properties
      URL classesURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      assertNotNull("classesURL", classesURL);
      System.setProperty("test.classes.url", classesURL.toString());
      URL libURL = super.getResource("/vfs/sundry/jar");
      assertNotNull("libURL", libURL);      
      System.setProperty("test.lib.url", libURL.toString());

      AssembledDirectory directory = AssembledContextFactory.getInstance().create("foo.jar");
      String[] includes = {"org/jboss/virtual/*.class", "org/jboss/virtual/**/context/jar/*.class"};
      String[] excludes = {"**/Nested*"};
      directory.addResources("org/jboss/virtual/VirtualFile.class", includes, excludes, Thread.currentThread().getContextClassLoader());
      List<VirtualFile> children = directory.getChildren();
      assertEquals(children.size(), 1);
      VirtualFile curr = children.get(0);
      System.out.println("test org/");
      assertEquals("org", curr.getName());

      System.out.println("test org/jboss");
      children = curr.getChildren();
      assertEquals(children.size(), 1);
      curr = children.get(0);
      assertEquals("jboss", curr.getName());

      System.out.println("test org/jboss/virtual");
      children = curr.getChildren();
      assertEquals(children.size(), 1);
      curr = children.get(0);
      assertEquals("virtual", curr.getName());
      children = curr.getChildren();
      boolean found;
      found = false;
      for (VirtualFile child: children)
      {
         if (child.getName().equals("VFS.class"))
         {
            found = true;
            break;
         }
      }
      assertTrue("VFS.class was found", found);

      found = false;
      for (VirtualFile child: children)
      {
         if (child.getName().equals("VirtualFile.class"))
         {
            found = true;
            assertEquals("org/jboss/virtual/VirtualFile.class", child.getPathName());
            break;
         }
      }
      assertTrue("VirtualFile.class was found", found);

      found = false;
      for (VirtualFile child: children)
      {
         if (child.getName().equals("plugins"))
         {
            found = true;
            break;
         }
      }
      assertTrue("plugins/", found);

      System.out.println("Test org/jboss/virtual/plugins/context/jar");
      VirtualFile jar = directory.findChild("org/jboss/virtual/plugins/context/jar");
      assertNotNull(jar);
      assertEquals("jar", jar.getName());

      children = jar.getChildren();
      for (VirtualFile child: children)
      {
         if (child.getName().startsWith("Nested")) throw new RuntimeException("did not exclude propertly");
      }
      AssembledContextFactory.getInstance().remove(directory);
   }

   public void testMkDir() throws Exception
   {
      AssembledDirectory directory = AssembledContextFactory.getInstance().create("foo.jar");
      directory.mkdir("META-INF");
      assertNotNull(directory.findChild("META-INF"));

   }
}
