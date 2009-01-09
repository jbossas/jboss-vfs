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
import java.net.URL;
import java.util.List;
import java.util.Arrays;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;

/**
 * Test path tokens.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class PathTokensTestCase extends AbstractVFSTest
{
   public PathTokensTestCase(String s)
   {
      super(s);
   }

   public static Test suite()
   {
      return suite(PathTokensTestCase.class);
   }

   protected VirtualFile testPath(String path) throws IOException
   {
      return testPath("/vfs", path);
   }

   protected VirtualFile testPath(String context, String path) throws IOException
   {
      URL url = getResource(context);
      VirtualFile vf = VFS.getRoot(url);
      return vf.getChild(path);
   }

   protected void testValidPath(String path) throws Throwable
   {
      assertNotNull("No such path: " + path, testPath(path));
   }

   protected void testBrokenPath(String path) throws Throwable
   {
      try
      {
         testPath(path);
         fail("Should not be here");
      }
      catch (Throwable t)
      {
         assertInstanceOf(t, IllegalArgumentException.class, false);
      }
   }

   public void testSpecialTokens() throws Throwable
   {
      PathTokenizer.setErrorOnSuspiciousTokens(true);
      try
      {
         testBrokenPath("/.../");
         testBrokenPath(".../");
         testBrokenPath("/...");
         testBrokenPath("...");
         testBrokenPath("/..somemorepath/");
         testBrokenPath("..somemorepath/");
         testBrokenPath("/..somemorepath");
         testBrokenPath("..somemorepath");
      }
      finally
      {
         PathTokenizer.setErrorOnSuspiciousTokens(false);
      }
   }

   public void testRepeatedSlashes() throws Throwable
   {
      testValidPath("/");
      testValidPath("//");
      testValidPath("///");
      testValidPath("////");
      testValidPath("//context");
      testValidPath("//context//");
      testValidPath("context//file");
      testValidPath("context///file");
      testValidPath("//context//file");
      testValidPath("//context///file");
      testValidPath("//context////file");
      testValidPath("//context///jar//");
      testValidPath("//context///jar///");
   }

   public void testSuspiciousTokens() throws Throwable
   {
      testSuspiciousTokens("/.hudson/..hudson/...hudson/./../.../.*foo/foo.bar", ".hudson", "..hudson", "...hudson", ".", "..", "...", ".*foo", "foo.bar");     
      testSuspiciousTokens("jpa/.svn", "jpa", ".svn");
   }

   protected void testSuspiciousTokens(String path, String... expected) throws Throwable
   {
      testSuspiciousTokens(true, path, expected);
      testSuspiciousTokens(false, path, expected);
   }

   protected void testSuspiciousTokens(boolean flag, String path, String... expected) throws Throwable
   {
      PathTokenizer.setErrorOnSuspiciousTokens(flag);
      try
      {
         List<String> tokens = PathTokenizer.getTokens(path);
         List<String> expectedTokens = Arrays.asList(expected);
         assertEquals(expectedTokens, tokens);
         if (flag)
            fail("Should not be here.");
      }
      catch (Throwable t)
      {
         if (!flag)
            throw t;
      }
      finally
      {
         PathTokenizer.setErrorOnSuspiciousTokens(!flag);
      }
   }
}