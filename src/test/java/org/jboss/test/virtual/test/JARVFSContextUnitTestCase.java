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
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.jar.JarContext;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * JARVFSContextUnitTestCase.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class JARVFSContextUnitTestCase extends AbstractVFSContextTest
{
   public JARVFSContextUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      VFS.init();
      System.out.println("java.protocol.handler.pkgs: " + System.getProperty("java.protocol.handler.pkgs"));
      return new TestSuite(JARVFSContextUnitTestCase.class);
   }

   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getResource("/vfs/context/jar/" + name + ".jar");
      url = JarUtils.createJarURL(url);
      return new JarContext(url);
   }

   protected VFSContext getParentVFSContext() throws Exception
   {
      URL url = getResource("/vfs/context/jar/");
      return new FileSystemContext(url);
   }

   /**
    * Create vfs context from url.
    *
    * @param url the url
    * @return new vfs context
    * @throws Exception for any error
    */
   protected VFSContext createVSFContext(URL url) throws Exception
   {
      if (url.toExternalForm().startsWith("jar") == false)
         url = JarUtils.createJarURL(url);
      return new JarContext(url);
   }

   protected String getSuffix()
   {
      return ".jar";
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    *
    * @throws Exception for any error
    */
   public void testJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      VFSContext context = createVSFContext(entry);
      assertEquals("child", context.getRoot().getName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = createVSFContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    * A JarEntry that is the root of the VFS should have a VFS Path of ""
    *
    * @throws Exception for any error
    */
   public void testPathIsEmptryForJarEntryAsRoot() throws Exception
   {
      VFS.init();
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      VFSContext context = createVSFContext(entry);
      assertEquals("child", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = createVSFContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());
   }

   protected static void safeClose(InputStream is)
   {
      try
      {
         is.close();
      }
      catch (Throwable ignore)
      {
      }
   }

   /**
    * Handler representing a directory must return a zero length stream
    *
    * @throws Exception for any error
    */
   public void testDirectoryZipEntryOpenStream() throws Exception
   {
      URL url = getResource("/vfs/context/jar/complex.jar");
      VFSContext ctx = createVSFContext(url);

      VirtualFileHandler sub = ctx.getRoot().getChild("subfolder");
      InputStream is = sub.openStream();
      try
      {
         assertTrue("input stream closed", is.read() == -1);
      }
      finally
      {
         safeClose(is);
      }
   }

   /**
    * There was a problem with noCopy inner jars returning empty streams
    *
    * @throws Exception for any error
    */
   public void testInnerJarFileEntryOpenStream() throws Exception
   {
      URL url = getResource("/vfs/context/jar/nested.jar");
      VFSContext ctx = createVSFContext(url);

      VirtualFileHandler nested = ctx.getRoot().getChild("complex.jar");
      VirtualFileHandler target = nested.getChild("META-INF/MANIFEST.MF");

      InputStream is = target.openStream();
      try
      {
         assertFalse("input stream closed", is.read() == -1);
      }
      finally
      {
         safeClose(is);
      }
   }

   public void testInnerJarOverURL() throws Exception
   {
      URL url = getResource("/vfs/test/nested/" + getNestedName() + ".jar");
      String urlString = url.toExternalForm();
      testInnerEntryOverURL(urlString, "/complex.jar", false);
      // test children
      testInnerEntryOverURL(urlString, "/complex.jar/child", false);
      testInnerEntryOverURL(urlString, "/complex.jar/subfolder/subchild", false);
      // test folder
      testInnerEntryOverURL(urlString, "/complex.jar/subfolder", false);
      testInnerEntryOverURL(urlString, "/complex.jar/subfolder/subsubfolder", false);
      // 3 level zips
      url = getResource("/vfs/test/level1.zip");
      urlString = url.toExternalForm();
      testInnerEntryOverURL(urlString, "/level2.zip/level3.zip/test3.txt", false);
   }

   protected void testInnerEntryOverURL(String urlString, String entry, boolean result) throws IOException
   {
      URL vfsURL = new URL(getProtocol() + urlString.substring(4) + entry);
      InputStream is = vfsURL.openStream();
      try
      {
         assertEquals("cannot read input stream", result, is.read() == -1);
      }
      finally
      {
         safeClose(is);
      }
   }

   @SuppressWarnings("deprecation")
   public void testEqualsOnEmptyPath() throws Exception
   {
      URL rootURL = getResource("/vfs/test/interop_W2JREMarshallTest_appclient_vehicle.ear");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile file = vfs.findChild("interop_W2JREMarshallTest_appclient_vehicle_client.jar");
      VirtualFile same = file.findChild("");
      assertEquals(file, same);
   }

   /** All of the following entries should be seen
[551][valkyrie: web_pkg_scope]$ jar -tvf war/target/web_pkg_scope_web.war 
     0 Fri Sep 05 12:47:30 PDT 2008 META-INF/
   126 Fri Sep 05 12:47:28 PDT 2008 META-INF/MANIFEST.MF
     0 Fri Sep 05 12:07:32 PDT 2008 WEB-INF/
     0 Fri Sep 05 12:07:32 PDT 2008 WEB-INF/classes/
     0 Fri Sep 05 12:07:32 PDT 2008 WEB-INF/classes/web_pkg_scope/
     0 Fri Sep 05 12:07:32 PDT 2008 WEB-INF/classes/web_pkg_scope/entity/
     0 Fri Sep 05 12:07:32 PDT 2008 WEB-INF/classes/web_pkg_scope/servlet/
   288 Fri Sep 05 12:07:30 PDT 2008 WEB-INF/classes/web_pkg_scope/entity/Account.class
   299 Fri Sep 05 12:07:30 PDT 2008 WEB-INF/classes/web_pkg_scope/servlet/JpaServlet.class
  1055 Fri Sep 05 12:25:02 PDT 2008 WEB-INF/web.xml
     0 Fri Sep 05 12:47:30 PDT 2008 META-INF/maven/
     0 Fri Sep 05 12:47:30 PDT 2008 META-INF/maven/org.jboss.vfs/
     0 Fri Sep 05 12:47:30 PDT 2008 META-INF/maven/org.jboss.vfs/web_pkg_scope_web/
   735 Fri Sep 05 12:47:20 PDT 2008 META-INF/maven/org.jboss.vfs/web_pkg_scope_web/pom.xml
   122 Fri Sep 05 12:47:28 PDT 2008 META-INF/maven/org.jboss.vfs/web_pkg_scope_web/pom.properties
    * @throws Exception
    */
   public void testWarClassesJarInputStream() throws Exception
   {
      URL rootURL = getResource("/vfs/test/web_pkg_scope.ear");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile file = vfs.getChild("web_pkg_scope_web.war/WEB-INF/classes/META-INF/persistence.xml");
      assertNotNull(file);
      VirtualFile classes = file.getParent().getParent();
      // Access the classes contents as a jar file
      URL classesURL = classes.toURL();
      String[] entryNames = {
            "web_pkg_scope/",
            "web_pkg_scope/entity/",
            "web_pkg_scope/servlet/",
            "web_pkg_scope/entity/Account.class",
            "web_pkg_scope/servlet/JpaServlet.class"
      };
      JarInputStream jis = new JarInputStream( classesURL.openStream() );
      HashSet<String> missingEntries = new HashSet<String>(Arrays.asList(entryNames));
      int count = 0;
      JarEntry jarEntry;
      while((jarEntry = jis.getNextJarEntry()) != null)
      {
         String name = jarEntry.getName();
         missingEntries.remove(name);
         count ++;
      }
      assertEquals("No missing entries: "+missingEntries, 0, missingEntries.size());
      classes.closeStreams();
   }

   // we need to make sure this doesn't get touched before
   protected String getNestedName()
   {
      return "nested";
   }

   protected String getProtocol()
   {
      return "vfsfile";
   }
}
