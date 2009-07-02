/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.virtual.support.ClassPathIterator;
import org.jboss.test.virtual.support.MetaDataMatchFilter;
import org.jboss.test.virtual.support.ClassPathIterator.ClassPathEntry;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.vfs.helpers.SuffixMatchFilter;

/**
 * Tests of the VFS implementation
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 55523 $
 */
public class FileVFSUnitTestCase extends AbstractVFSTest
{
   public FileVFSUnitTestCase(String name)
   {
      super(name);
   }
   
   protected FileVFSUnitTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy);
   }

   public static Test suite()
   {
      return new TestSuite(FileVFSUnitTestCase.class);
   }

   /**
    * Test that a VFSContextFactory can be created from the testcase CodeSource url
    * @throws Exception
    */
   public void testVFSContextFactory()
      throws Exception
   {
      URL root = getClass().getProtectionDomain().getCodeSource().getLocation();
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(root);
      assertTrue("VFSContextFactory(CodeSource.Location) != null", factory != null);
   }

   /**
    * Test that one can go from a file uri to VirtualFile and obtain the
    * same VirtualFile using VirtualFile vfsfile uri
    * @throws Exception
    */
   public void testVFSFileURIFactory()
      throws Exception
   {
      URL rootURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      VFS rootVFS0 = VFS.getVFS(rootURL.toURI());
      VirtualFile root0 = rootVFS0.getRoot();
      VFS rootVFS1 = VFS.getVFS(root0.toURI());
      VirtualFile root1 = rootVFS1.getRoot();
      assertEquals(root0, root1);
   }

   /**
    * Test reading the contents of nested jar entries.
    * @throws Exception
    */
   public void testInnerJarFile()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile outerjar = vfs.findChild("outer.jar");
      assertTrue("outer.jar != null", outerjar != null);
      VirtualFile jar1 = outerjar.findChild("jar1.jar");
      assertTrue("outer.jar/jar1.jar != null", jar1 != null);
      VirtualFile jar2 = outerjar.findChild("jar2.jar");
      assertTrue("outer.jar/jar2.jar != null", jar2 != null);

      VirtualFile jar1MF = jar1.findChild("META-INF/MANIFEST.MF");
      assertNotNull("jar1!/META-INF/MANIFEST.MF", jar1MF);
      InputStream mfIS = jar1MF.openStream();
      Manifest mf1 = new Manifest(mfIS);
      Attributes mainAttrs1 = mf1.getMainAttributes();
      String title1 = mainAttrs1.getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals("jar1", title1);
      jar1MF.close();

      VirtualFile jar2MF = jar2.findChild("META-INF/MANIFEST.MF");
      assertNotNull("jar2!/META-INF/MANIFEST.MF", jar2MF);
      InputStream mfIS2 = jar2MF.openStream();
      Manifest mf2 = new Manifest(mfIS2);
      Attributes mainAttrs2 = mf2.getMainAttributes();
      String title2 = mainAttrs2.getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals("jar2", title2);
      jar2MF.close();
   }

   /**
    * Basic tests of accessing resources in a jar
    * @throws Exception
    */
   public void testFindResource()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile jar = vfs.findChild("outer.jar");
      assertTrue("outer.jar != null", jar != null);

      /*
      ArrayList<String> searchCtx = new ArrayList<String>();
      searchCtx.add("outer.jar");
      VirtualFile metaInf = vfs.resolveFile("META-INF/MANIFEST.MF", searchCtx);
      */
      VirtualFile metaInf = jar.findChild("META-INF/MANIFEST.MF");
      assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
      InputStream mfIS = metaInf.openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      mfIS.close();
   }

   /**
    * Basic tests of accessing resources in a jar
    * @throws Exception
    */
   public void testFindResourceUsingURLStream()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile jar = vfs.findChild("outer.jar");
      assertTrue("outer.jar != null", jar != null);

      /*
      ArrayList<String> searchCtx = new ArrayList<String>();
      searchCtx.add("outer.jar");
      VirtualFile metaInf = vfs.resolveFile("META-INF/MANIFEST.MF", searchCtx);
      */
      VirtualFile metaInf = jar.findChild("META-INF/MANIFEST.MF");
      assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
      InputStream mfIS = metaInf.toURL().openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      mfIS.close();

      String urlString = metaInf.toURL().toString();
      URL mfURL = new URL(urlString);
      mfIS = mfURL.openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      mf = new Manifest(mfIS);
      mainAttrs = mf.getMainAttributes();
      version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      mfIS.close();
   }

   /**
    * Basic tests of accessing resources in a jar that does not
    * have parent directory entries.
    * @throws Exception
    */
   public void testFindResourceInFilesOnlyJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile jar = vfs.findChild("jar1-filesonly.jar");
      assertTrue("jar1-filesonly.jar != null", jar != null);

      VirtualFile metaInf = jar.findChild("META-INF/MANIFEST.MF");
      assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
      InputStream mfIS = metaInf.toURL().openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      String title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals("jar1-filesonly", title);
      mfIS.close();

      String urlString = metaInf.toURL().toString();
      URL mfURL = new URL(urlString);
      mfIS = mfURL.openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      mf = new Manifest(mfIS);
      mainAttrs = mf.getMainAttributes();
      version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals("jar1-filesonly", title);
      mfIS.close();
   }

   /**
    * Basic tests of accessing resources in a war that does not
    * have parent directory entries.
    * @throws Exception
    */
   public void testFindResourceInFilesOnlyWar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile war2 = vfs.findChild("WarDeployApp_web.war");
      assertTrue("WarDeployApp_web.war != null", war2 != null);

      VirtualFile classes2 = war2.findChild("WEB-INF/classes");
      assertTrue("WEB-INF/classes != null", classes2 != null);
      assertTrue("WEB-INF/classes is not a leaf", classes2.isLeaf()==false);
      classes2 = war2.findChild("WEB-INF/classes");
      assertTrue("WEB-INF/classes != null", classes2 != null);
      assertTrue("WEB-INF/classes is not a leaf", classes2.isLeaf()==false);

      VirtualFile HelloJavaBean = classes2.findChild("com/sun/ts/tests/webservices/deploy/warDeploy/HelloJavaBean.class");
      assertTrue("HelloJavaBean.class != null", HelloJavaBean != null);
      assertTrue("HelloJavaBean.class is a leaf", HelloJavaBean.isLeaf());

      VirtualFile war = vfs.findChild("filesonly.war");
      assertTrue("filesonly.war != null", war != null);

      VirtualFile classes = war.findChild("WEB-INF/classes");
      assertTrue("WEB-INF/classes != null", classes != null);
      assertTrue("WEB-INF/classes is not a leaf", classes.isLeaf()==false);

      VirtualFile jar1 = war.findChild("WEB-INF/lib/jar1.jar");
      assertTrue("WEB-INF/lib/jar1.jar != null", jar1 != null);
      assertTrue("WEB-INF/lib/jar1.jar is not a leaf", jar1.isLeaf()==false);
      VirtualFile ClassInJar1 = jar1.findChild("org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      assertTrue("ClassInJar1.class != null", ClassInJar1 != null);
      assertTrue("ClassInJar1.class is a leaf", ClassInJar1.isLeaf());

      VirtualFile metaInf = war.findChild("META-INF/MANIFEST.MF");
      assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
      InputStream mfIS = metaInf.toURL().openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      String title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals("filesonly-war", title);
      mfIS.close();

      war.findChild("WEB-INF/classes");
      assertTrue("WEB-INF/classes != null", classes != null);
      assertTrue("WEB-INF/classes is not a leaf", classes.isLeaf()==false);
   }

   /**
    * Validate iterating over a vfs url from a files only war.
    * 
    * @throws Exception
    */
   public void testFindClassesInFilesOnlyWar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile war = vfs.findChild("filesonly.war");
      assertTrue("filesonly.war != null", war != null);

      VirtualFile classes = war.findChild("WEB-INF/classes");
      assertTrue("WEB-INF/classes != null", classes != null);
      HashSet<String> names = new HashSet<String>();
      ClassPathIterator iter = new ClassPathIterator(classes.toURL());
      ClassPathEntry entry = null;
      while( (entry = iter.getNextEntry()) != null )
      {
         names.add(entry.name);
      }
      log.debug(names);
      assertTrue("org/jboss/test/vfs/support/jar1", names.contains("org/jboss/test/vfs/support/jar1"));
      assertTrue("ClassInJar1.class", names.contains("org/jboss/test/vfs/support/jar1/ClassInJar1.class"));
      assertTrue("ClassInJar1$InnerClass.class", names.contains("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class"));
   }

   public void testFindResourceUnpackedJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile jar = vfs.findChild("unpacked-outer.jar");
      assertTrue("unpacked-outer.jar != null", jar != null);

      /**
      ArrayList<String> searchCtx = new ArrayList<String>();
      searchCtx.add("unpacked-outer.jar");
      VirtualFile metaInf = vfs.resolveFile("META-INF/MANIFEST.MF", searchCtx);
      */
      VirtualFile metaInf = jar.findChild("META-INF/MANIFEST.MF");
      assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
      InputStream mfIS = metaInf.openStream();
      assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
      assertEquals("1.0.0.GA", version);
      mfIS.close();
   }

   /**
    * Test simple file resolution without search contexts
    * @throws Exception
    */
   public void testResolveFile()
      throws Exception
   {
      log.info("+++ testResolveFile, cwd="+(new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      // Check resolving the root file
      VirtualFile root = vfs.findChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", "", root.getPathName());
      assertFalse("root isDirectory", root.isLeaf());

      // Find the outer.jar
      VirtualFile outerJar = vfs.findChild("outer.jar");
      assertNotNull("outer.jar", outerJar);
      assertEquals("outer.jar name", "outer.jar", outerJar.getName());
      assertEquals("outer.jar path", "outer.jar", outerJar.getPathName());
      
      VirtualFile outerJarMF = vfs.findChild("outer.jar/META-INF/MANIFEST.MF");
      assertNotNull("outer.jar/META-INF/MANIFEST.MF", outerJarMF);

      // Test a non-canonical path
      rootURL = getResource("/vfs/sundry/../test");
      // Check resolving the root file
      root = vfs.findChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", "", root.getPathName());
      assertFalse("root isDirectory", root.isLeaf());
   }

   /**
    * Validate resolving a .class file given a set of search contexts in the
    * vfs that make up a classpath.
    * 
    * @throws Exception
    */
   public void testResolveClassFileInClassPath()
      throws Exception
   {
      log.info("+++ testResolveFile, cwd="+(new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      
      // Find ClassInJar1.class
      VirtualFile vf = vfs.findChild("jar1.jar");
      VirtualFile c1 = vf.findChild("org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      assertNotNull("ClassInJar1.class VF", c1);
      log.debug("Found ClassInJar1.class: "+c1);

      // Find ClassInJar1$InnerClass.class
      VirtualFile c1i = vf.findChild("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
      assertNotNull("ClassInJar1$InnerClass.class VF", c1i);
      log.debug("Found ClassInJar1$InnerClass.class: "+c1i);

      // Find ClassInJar2.class
      vf = vfs.findChild("jar2.jar");
      VirtualFile c2 = vf.findChild("org/jboss/test/vfs/support/jar2/ClassInJar2.class");
      assertNotNull("ClassInJar2.class VF", c2);
      log.debug("Found ClassInJar2.class: "+c2);
   }

   public void testResolveFileInUnpackedJar()
      throws Exception
   {
      log.info("+++ testResolveFileInUnpackedJar, cwd="+(new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      // Check resolving the root file
      VirtualFile root = vfs.findChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", "", root.getPathName());
      assertFalse("root isDirectory", root.isLeaf());

      // Find the outer.jar
      VirtualFile outerJar = vfs.findChild("unpacked-outer.jar");
      assertNotNull("unpacked-outer.jar", outerJar);
      assertEquals("unpacked-outer.jar name", "unpacked-outer.jar", outerJar.getName());
      assertEquals("unpacked-outer.jar path", "unpacked-outer.jar", outerJar.getPathName());
      
      VirtualFile outerJarMF = vfs.findChild("unpacked-outer.jar/META-INF/MANIFEST.MF");
      assertNotNull("unpacked-outer.jar/META-INF/MANIFEST.MF", outerJarMF);

      // Check resolving the root file
      root = vfs.findChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", "", root.getPathName());
      assertFalse("root isDirectory", root.isLeaf());
   }

   public void testFileNotFoundInUnpackedJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      // Find the outer.jar
      VirtualFile outerJar = vfs.findChild("unpacked-outer.jar");
      assertNotNull("unpacked-outer.jar", outerJar);
      assertNull(outerJar.getChild("WEB-INF"));
   }

   public void testNestedNestedParent() throws Exception
   {
      // TODO
   }

   public void testCopyNestedStream() throws Exception
   {
      // TODO
   }

/*
   public void testNoCopyNestedStream()
      throws Exception
   {
      URL rootURL = getResource("/vfs/seam/jboss-seam-booking.ear");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile clazz = vfs.getChild("lib/commons-beanutils.jar/org/apache/commons/beanutils/BeanComparator.class");
      assertNotNull(clazz);
      URL url = clazz.toURL();
      InputStream is = url.openStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] tmp = new byte[1024];
      int read = 0;
      while ( (read = is.read(tmp)) >= 0 )
         baos.write(tmp, 0, read);
      byte[] bytes = baos.toByteArray();
      int size = bytes.length;
      System.out.println("size = " + size);
   }
*/

   /**
    * Test file resolution with nested jars
    * @throws Exception
    */
   public void testInnerJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile inner = vfs.findChild("outer.jar/jar1.jar");
      log.info("IsFile: "+inner.isLeaf());
      log.info(inner.getLastModified());
      List<VirtualFile> contents = inner.getChildren();
      // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
      assertTrue("jar1.jar children.length("+contents.size()+") >= 2", contents.size() >= 2);
      for(VirtualFile vf : contents)
      {
         log.info("  "+vf.getName());
      }
      VirtualFile vf = vfs.findChild("outer.jar/jar1.jar");
      VirtualFile jar1MF = vf.findChild("META-INF/MANIFEST.MF");
      InputStream mfIS = jar1MF.openStream();
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
      mfIS.close();
   }

   public void testInnerJarUsingURLStream()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile inner = vfs.findChild("outer.jar/jar1.jar");
      log.info("IsFile: "+inner.isLeaf());
      log.info(inner.getLastModified());
      List<VirtualFile> contents = inner.getChildren();
      // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
      assertTrue("jar1.jar children.length("+contents.size()+") >= 2", contents.size() >= 2);
      for(VirtualFile vf : contents)
      {
         log.info("  "+vf.getName());
      }
      VirtualFile vf = vfs.findChild("outer.jar/jar1.jar");
      VirtualFile jar1MF = vf.findChild("META-INF/MANIFEST.MF");
      InputStream mfIS = jar1MF.toURL().openStream();
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
      mfIS.close();
   }

   /**
    * Test a scan of the outer.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScan()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test/outer.jar");
      VFS vfs = VFS.getVFS(rootURL);

      HashSet<String> expectedClasses = new HashSet<String>();
      expectedClasses.add("jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      expectedClasses.add("jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
      expectedClasses.add("jar1-filesonly.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      expectedClasses.add("jar1-filesonly.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
      expectedClasses.add("jar2.jar/org/jboss/test/vfs/support/jar2/ClassInJar2.class");
      expectedClasses.add("org/jboss/test/vfs/support/CommonClass.class");
      super.enableTrace("org.jboss.virtual.plugins.vfs.helpers.SuffixMatchFilter");
      SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
      List<VirtualFile> classes = vfs.getChildren(classVisitor);
      int count = 0;
      for (VirtualFile cf : classes)
      {
         String path = cf.getPathName();
         if( path.endsWith(".class") )
         {
            assertTrue(path, expectedClasses.contains(path));
            count ++;
         }
      }
      assertEquals("There were 6 classes", 6, count);
   }

   /**
    * Test a scan of the unpacked-outer.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScanUnpacked()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test/unpacked-outer.jar");
      VFS vfs = VFS.getVFS(rootURL);
   
      HashSet<String> expectedClasses = new HashSet<String>();
      expectedClasses.add("jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      expectedClasses.add("jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
      expectedClasses.add("jar2.jar/org/jboss/test/vfs/support/jar2/ClassInJar2.class");
      // FIXME: .class files are not being copied from the resources directory
      expectedClasses.add("org/jboss/test/vfs/support/CommonClass.class");
      super.enableTrace("org.jboss.virtual.plugins.vfs.helpers.SuffixMatchFilter");
      SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
      List<VirtualFile> classes = vfs.getChildren(classVisitor);
      int count = 0;
      for (VirtualFile cf : classes)
      {
         String path = cf.getPathName();
         if( path.endsWith(".class") )
         {
            assertTrue(path, expectedClasses.contains(path));
            count ++;
         }
      }
      assertEquals("There were 4 classes", 4, count);
   }

   /**
    * Test a scan of the jar1-filesonly.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScanFilesonly()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test/jar1-filesonly.jar");
      VFS vfs = VFS.getVFS(rootURL);
   
      HashSet<String> expectedClasses = new HashSet<String>();
      expectedClasses.add("org/jboss/test/vfs/support/jar1/ClassInJar1.class");
      expectedClasses.add("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
      super.enableTrace("org.jboss.virtual.plugins.vfs.helpers.SuffixMatchFilter");
      SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
      List<VirtualFile> classes = vfs.getChildren(classVisitor);
      int count = 0;
      for (VirtualFile cf : classes)
      {
         String path = cf.getPathName();
         if( path.endsWith(".class") )
         {
            assertTrue(path, expectedClasses.contains(path));
            count ++;
         }
      }
      assertEquals("There were 2 classes", 2, count);

      // Make sure we can walk path-wise to the class
      VirtualFile parent = vfs.getRoot();
      String className = "org/jboss/test/vfs/support/jar1/ClassInJar1.class";
      VirtualFile classInJar1 = vfs.findChild(className);
      String[] paths = className.split("/");
      StringBuilder vfsPath = new StringBuilder();
      for(String path : paths)
      {
         vfsPath.append(path);
         VirtualFile vf = parent.findChild(path);
         if( path.equals("ClassInJar1.class") )
            assertEquals("ClassInJar1.class", classInJar1, vf);
         else
         {
            assertEquals("vfsPath", vfsPath.toString(), vf.getPathName());
            // why should this be equal?
            // assertEquals("lastModified", classInJar1.getLastModified(), vf.getLastModified());
            assertTrue("lastModified", classInJar1.getLastModified() <= vf.getLastModified());
         }
         vfsPath.append('/');
         parent = vf;
      }
   }

   /**
    * Test access of directories in a jar that only stores files
    * @throws Exception
    */
   public void testFilesOnlyJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile jar = vfs.findChild("jar1-filesonly.jar");
      VirtualFile metadataLocation = jar.findChild("META-INF");
      assertNotNull(metadataLocation);
      VirtualFile mfFile = metadataLocation.findChild("MANIFEST.MF");
      assertNotNull(mfFile);
      InputStream is = mfFile.openStream();
      Manifest mf = new Manifest(is);
      mfFile.close();
      String title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1-filesonly", title);

      // Retry starting from the jar root
      mfFile = jar.findChild("META-INF/MANIFEST.MF");
      is = mfFile.openStream();
      mf = new Manifest(is);
      mfFile.close();
      title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1-filesonly", title);
   }

   /**
    * Test the serialization of VirtualFiles
    * @throws Exception
    */
   public void testVFSerialization()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      tmpRoot.deleteOnExit();
      File tmp = new File(tmpRoot, "vfs.ser");
      tmp.createNewFile();
      tmp.deleteOnExit();
      log.info("+++ testVFSerialization, tmp="+tmp.getCanonicalPath());
      URL rootURL = tmpRoot.toURI().toURL();
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tmpVF = vfs.findChild("vfs.ser");
      FileOutputStream fos = new FileOutputStream(tmp);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(tmpVF);
      oos.close();

      // Check the tmpVF attributes against the tmp file
      long lastModified = tmp.lastModified();
      long size = tmp.length();
      String name = tmp.getName();
      String vfsPath = tmp.getPath();
      vfsPath = vfsPath.substring(tmpRoot.getPath().length()+1);
      URL url = new URL("vfs" + tmp.toURI().toURL());
      log.debug("name: "+name);
      log.debug("vfsPath: "+vfsPath);
      log.debug("url: "+url);
      log.debug("lastModified: "+lastModified);
      log.debug("size: "+size);
      assertEquals("name", name, tmpVF.getName());
      assertEquals("pathName", vfsPath, tmpVF.getPathName());
      assertEquals("lastModified", lastModified, tmpVF.getLastModified());
      assertEquals("size", size, tmpVF.getSize());
      assertEquals("url", url, tmpVF.toURL());
      assertEquals("isLeaf", true, tmpVF.isLeaf());
      assertEquals("isHidden", false, tmpVF.isHidden());

      // Read in the VF from the serialized file
      FileInputStream fis = new FileInputStream(tmp);
      ObjectInputStream ois = new ObjectInputStream(fis);
      VirtualFile tmpVF2 = (VirtualFile) ois.readObject();
      ois.close();
      // Validated the deserialized attribtes against the tmp file
      assertEquals("name", name, tmpVF2.getName());
      assertEquals("pathName", vfsPath, tmpVF2.getPathName());
      assertEquals("lastModified", lastModified, tmpVF2.getLastModified());
      assertEquals("size", size, tmpVF2.getSize());
      assertEquals("url", url, tmpVF2.toURL());
      assertEquals("isLeaf", true, tmpVF2.isLeaf());
      assertEquals("isHidden", false, tmpVF2.isHidden());
   }

   /**
    * Test the serialization of VirtualFiles representing a jar
    * @throws Exception
    */
   public void testVFJarSerialization()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      tmpRoot.deleteOnExit();
      // Create a test jar containing a txt file
      File tmpJar = new File(tmpRoot, "tst.jar");
      tmpJar.createNewFile();
      tmpJar.deleteOnExit();
      FileOutputStream fos = new FileOutputStream(tmpJar);
      JarOutputStream jos = new JarOutputStream(fos);
      // Write a text file to include in a test jar
      JarEntry txtEntry = new JarEntry("tst.txt");
      jos.putNextEntry(txtEntry);
      txtEntry.setSize("testVFJarSerialization".length());
      txtEntry.setTime(System.currentTimeMillis());
      jos.write("testVFJarSerialization".getBytes());
      jos.close();
      log.info("+++ testVFJarSerialization, tmp="+tmpJar.getCanonicalPath());

      URI rootURI = tmpRoot.toURI();
      VFS vfs = VFS.getVFS(rootURI);
      File vfsSer = new File(tmpRoot, "vfs.ser");
      vfsSer.createNewFile();
      vfsSer.deleteOnExit();

      VirtualFile tmpVF = vfs.findChild("tst.jar");
      // Validate the vf jar against the tmp file attributes
      long lastModified = tmpJar.lastModified();
      long size = tmpJar.length();
      String name = tmpJar.getName();
      String vfsPath = tmpJar.getPath();
      vfsPath = vfsPath.substring(tmpRoot.getPath().length()+1);
      URL url = new URL("vfs" + tmpJar.toURI().toURL() + "/");
      //url = JarUtils.createJarURL(url);
      log.debug("name: "+name);
      log.debug("vfsPath: "+vfsPath);
      log.debug("url: "+url);
      log.debug("lastModified: "+lastModified);
      log.debug("size: "+size);
      assertEquals("name", name, tmpVF.getName());
      assertEquals("pathName", vfsPath, tmpVF.getPathName());
      assertEquals("lastModified", lastModified, tmpVF.getLastModified());
      assertEquals("size", size, tmpVF.getSize());
      assertEquals("url", url.getPath(), tmpVF.toURL().getPath());
      // TODO: these should pass
      //assertEquals("isFile", true, tmpVF.isFile());
      //assertEquals("isDirectory", false, tmpVF.isDirectory());
      assertEquals("isHidden", false, tmpVF.isHidden());
      // Write out the vfs jar file
      fos = new FileOutputStream(vfsSer);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(tmpVF);
      oos.close();

      // Read in the VF from the serialized file
      FileInputStream fis = new FileInputStream(vfsSer);
      ObjectInputStream ois = new ObjectInputStream(fis);
      VirtualFile tmpVF2 = (VirtualFile) ois.readObject();
      ois.close();
      // Validate the vf jar against the tmp file attributes
      assertEquals("name", name, tmpVF2.getName());
      assertEquals("pathName", vfsPath, tmpVF2.getPathName());
      assertEquals("lastModified", lastModified, tmpVF2.getLastModified());
      assertEquals("size", size, tmpVF2.getSize());
      assertEquals("url", url.getPath(), tmpVF2.toURL().getPath());
      // TODO: these should pass
      //assertEquals("isFile", true, tmpVF2.isFile());
      //assertEquals("isDirectory", false, tmpVF2.isDirectory());
      assertEquals("isHidden", false, tmpVF2.isHidden());
   }

   /**
    * Test the serialization of VirtualFiles representing a jar
    * @throws Exception
    */
   public void testVFNestedJarSerialization()
      throws Exception
   {
      // this expects to be run with a working dir of the container root
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile inner = vfs.findChild("outer.jar/jar1.jar");

      File vfsSer = File.createTempFile("testVFNestedJarSerialization", ".ser");
      vfsSer.deleteOnExit();
      // Write out the vfs inner jar file
      FileOutputStream fos = new FileOutputStream(vfsSer);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(inner);
      oos.close();
      
      // Read in the VF from the serialized file
      FileInputStream fis = new FileInputStream(vfsSer);
      ObjectInputStream ois = new ObjectInputStream(fis);
      inner = (VirtualFile) ois.readObject();
      ois.close();
      List<VirtualFile> contents = inner.getChildren();
      // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
      // TODO - fix this once no_copy serialization is working
      int size = isForceCopyEnabled(inner) ? 2 : 0;
      assertTrue("jar1.jar children.length("+contents.size()+") is not " + size, contents.size() >= size);
      for(VirtualFile vf : contents)
      {
         log.info("  "+vf.getName());
      }
      VirtualFile vf = vfs.findChild("outer.jar/jar1.jar");
/*
      VirtualFile jar1MF = vf.findChild("META-INF/MANIFEST.MF");
      InputStream mfIS = jar1MF.openStream();
      Manifest mf = new Manifest(mfIS);
      Attributes mainAttrs = mf.getMainAttributes();
      String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
      mfIS.close();
*/
   }

   /**
    * Test parsing of a vfs link properties file. It contains test.classes.url
    * and test.lib.url system property references that are configured to
    * point to the CodeSource location of this class and /vfs/sundry/jar/
    * respectively.
    * 
    * @throws Exception
    */
   public void testVfsLinkProperties()
      throws Exception
   {
      URL linkURL = super.getResource("/vfs/links/test-link.war.vfslink.properties");
      assertNotNull("vfs/links/test-link.war.vfslink.properties", linkURL);
      // Find resources to use as the WEB-INF/{classes,lib} link targets
      URL classesURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      assertNotNull("classesURL", classesURL);
      System.setProperty("test.classes.url", classesURL.toString());
      URL libURL = super.getResource("/vfs/sundry/jar");
      assertNotNull("libURL", libURL);      
      System.setProperty("test.lib.url", libURL.toString());

      assertTrue("isLink", VFSUtils.isLink(linkURL.getPath()));
      Properties props = new Properties();
      InputStream linkIS = linkURL.openStream();
      List<LinkInfo> infos = VFSUtils.readLinkInfo(linkIS, linkURL.getPath(), props);
      assertEquals("LinkInfo count", 2, infos.size());
      LinkInfo classesInfo = null;
      LinkInfo libInfo = null;
      for(LinkInfo info :infos)
      {
         if( info.getName().equals("WEB-INF/classes") )
            classesInfo = info;
         else if(info.getName().equals("WEB-INF/lib") )
            libInfo = info;
      }
      assertNotNull("classesInfo", classesInfo);
      assertEquals("classesInfo.target", classesURL.toURI(), classesInfo.getLinkTarget());
      assertNotNull("libInfo", libInfo);
      assertEquals("libInfo.target", libURL.toURI(), libInfo.getLinkTarget());
   }

   /**
    * Test the test-link.war link
    * @throws Exception
    */
   public void testWarLink()
      throws Exception
   {
      // Find resources to use as the WEB-INF/{classes,lib} link targets
      URL classesURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      assertNotNull("classesURL", classesURL);
      System.setProperty("test.classes.url", classesURL.toString());
      URL libURL = super.getResource("/vfs/sundry/jar");
      assertNotNull("libURL", libURL);      
      System.setProperty("test.lib.url", libURL.toString());

      // Root the vfs at the link file parent directory
      URL linkURL = super.getResource("/vfs/links/test-link.war.vfslink.properties");
      File linkFile = new File(linkURL.toURI());
      File vfsRoot = linkFile.getParentFile();
      assertNotNull("vfs/links/test-link.war.vfslink.properties", linkURL);
      VFS vfs = VFS.getVFS(vfsRoot.toURI());

      // We should find the test-link.war the link represents
      VirtualFile war = vfs.findChild("test-link.war");
      assertNotNull("war", war);

      // Validate the WEB-INF/classes child link
      VirtualFile classes = war.findChild("WEB-INF/classes");
      String classesName = classes.getName();
      String classesPathName = classes.getPathName();
      boolean classesIsDirectory = classes.isLeaf() == false;
      assertEquals("classes.name", "classes", classesName);
      assertEquals("classes.pathName", "test-link.war/WEB-INF/classes", classesPathName);
      assertEquals("classes.isDirectory", true, classesIsDirectory);
      // Should be able to find this class since classes points to out codesource
      VirtualFile thisClass = classes.findChild("org/jboss/test/virtual/test/FileVFSUnitTestCase.class");
      assertEquals("FileVFSUnitTestCase.class", thisClass.getName());

      // Validate the WEB-INF/lib child link
      VirtualFile lib = war.findChild("WEB-INF/lib");
      String libName = lib.getName();
      String libPathName = lib.getPathName();
      boolean libIsDirectory = lib.isLeaf() == false;
      assertEquals("lib.name", "lib", libName);
      assertEquals("lib.pathName", "test-link.war/WEB-INF/lib", libPathName);
      assertEquals("lib.isDirectory", true, libIsDirectory);
      // Should be able to find archive.jar under lib
      VirtualFile archiveJar = lib.findChild("archive.jar");
      assertEquals("archive.jar", archiveJar.getName());
   }

   /**
    * Test configuration change detection on test-link.war link
    * @throws Exception
    */
   public void testWarLinkUpdate()
      throws Exception
   {
      // Setup the system properties used in test-link.war.vfslink.properties 
      URL classesURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      assertNotNull("classesURL", classesURL);
      System.setProperty("test.classes.url", classesURL.toString());
      URL libURL = super.getResource("/vfs/sundry/jar");
      assertNotNull("libURL", libURL);
      System.setProperty("test.lib.url", libURL.toString());

      // Root the vfs at the link file parent directory
      URL linkURL = super.getResource("/vfs/links/test-link.war.vfslink.properties");
      File linkFile = new File(linkURL.toURI());
      assertNotNull("vfs/links/test-link.war.vfslink.properties", linkURL);

      // Setup VFS root in a temp directory
      File root = File.createTempFile("jboss-vfs-testWarLinkUpdate", ".tmp");
      root.delete();
      root.mkdir();
      log.info("Using root: "+root);

      // There should be no test-link.war under the new tmp root
      VFS vfs = VFS.getVFS(root.toURI());
      VirtualFile link = vfs.getChild("test-link.war");
      assertNull("test-link.war", link);

      // Add the link properties, now test-link.war should exist
      File propsFile = new File(root, "test-link.war.vfslink.properties");
      VFSUtils.copyStreamAndClose(new FileInputStream(linkFile), new FileOutputStream(propsFile));
      link = vfs.getChild("test-link.war");
      assertNotNull("test-link.war", link);

      List<VirtualFile> children = link.getChildren();
      assertEquals("test-link.war has 1 child", 1, children.size());
      assertEquals("WEB-INF has 2 children", 2, children.get(0).getChildren().size());

      // Sleep 1sec+ to allow timestamp changes in files to be > 1000ms, JBVFS-59
      Thread.sleep(1005);

      // modify properties file - add more children
      URL dynamicClassRoot = new URL("vfsmemory", ".vfslink-test", "");
      MemoryFileFactory.createRoot(dynamicClassRoot);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintWriter webOut = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"));
      webOut.println("<?xml version=\"1.0\" ?>");
      webOut.println("<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
         "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
         "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\n" +
         "         version=\"2.5\">");
      webOut.println("</web-app>");
      webOut.close();

      MemoryFileFactory.putFile(dynamicClassRoot, baos.toByteArray());

      PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(propsFile, true)));
      out.println("vfs.link.name.2=WEB-INF/web.xml");
      out.println("vfs.link.target.2=" + dynamicClassRoot.toExternalForm());
      out.close();

      Properties linkProps = new Properties();
      InputStream propsIn = new FileInputStream(propsFile);
      linkProps.load(propsIn);
      assertEquals(1+3*2, linkProps.size());
      assertEquals("vfs.link.name.0", "WEB-INF/classes", linkProps.getProperty("vfs.link.name.0"));
      assertEquals("vfs.link.name.1", "WEB-INF/lib", linkProps.getProperty("vfs.link.name.1"));
      assertEquals("vfs.link.name.2", "WEB-INF/web.xml", linkProps.getProperty("vfs.link.name.2"));
      assertEquals("vfs.link.target.2", dynamicClassRoot.toExternalForm(), linkProps.getProperty("vfs.link.target.2"));
      propsIn.close();

      // You need to get a new reference to LinkHandler - to get up-to-date configuration
      children = link.getChildren();
      assertEquals("test-link.war has 1 child", 1, children.size());
      log.info("WEB-INF children after update: "+children.get(0).getChildren());
      assertEquals("WEB-INF has 3 children", 3, children.get(0).getChildren().size());

      // Sleep 1sec+ to allow timestamp changes in files to be > 1000ms, JBVFS-59
      Thread.sleep(1005);
      // modify properties file - remove all but first
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(propsFile)));
      baos = new ByteArrayOutputStream();
      out = new PrintWriter(new OutputStreamWriter(baos));
      String line = in.readLine();
      while(line != null)
      {
         if (line.indexOf(".0=") != -1)
            out.println(line);
         line = in.readLine();
      }
      out.close();
      in.close();

      FileOutputStream fos = new FileOutputStream(propsFile);
      fos.write(baos.toByteArray());
      fos.close();

      children = link.getChildren();
      assertEquals("test-link.war has 1 child", 1, children.size());
      assertEquals("WEB-INF has 1 child", 1, children.get(0).getChildren().size());

      // Sleep 1sec+ to allow timestamp changes in files to be > 1000ms, JBVFS-59
      Thread.sleep(1005);
      // modify properties file - remove all
      fos = new FileOutputStream(propsFile);
      fos.write(' ');
      fos.close();

      assertNotNull(link.getName() + " not null", link);
      assertTrue(link.getName() + " exists()", link.exists());

      children = link.getChildren();
      assertTrue("Wrong number of children", children.size() == 0);

      // remove properties file
      assertTrue(propsFile.getName() + " delete()", propsFile.delete());

      assertFalse(link.getName() + " exists() == false", link.exists());
      VirtualFile oldLink = link;
      link = vfs.getChild("test-link.war");
      assertNull(oldLink.getName() + " is null", link);

      children = vfs.getChildren();
      assertTrue("Wrong number of children", children.size() == 0);

      // put back .vfslink.properties
      VFSUtils.copyStreamAndClose(new FileInputStream(linkFile), new FileOutputStream(propsFile));

      assertTrue(oldLink.getName() + " exists()", oldLink.exists());
      link = vfs.getChild("test-link.war");
      assertNotNull("test-link.war", link);

      children = link.getChildren();
      assertTrue("Wrong number of children", children.size() == 1);
      assertTrue("Wrong number of WEB-INF link children", children.get(0).getChildren().size() == 2);
   }

  /**
    * Test that the URL of a VFS corresponding to a directory ends in '/' so that
    * URLs created relative to it are under the directory. This requires that
    * build-test.xml artifacts exist.
    * 
    * @throws Exception
    */
   public void testDirURLs() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile outerJar = vfs.findChild("unpacked-outer.jar");
      URL outerURL = outerJar.toURL();
      log.debug("outerURL: "+outerURL);
      assertTrue(outerURL+" ends in '/'", outerURL.getPath().endsWith("/"));
      // Validate that jar1 is under unpacked-outer.jar
      URL jar1URL = new URL(outerURL, "jar1.jar/");
      log.debug("jar1URL: "+jar1URL+", path="+jar1URL.getPath());
      assertTrue("jar1URL path ends in unpacked-outer.jar/jar1.jar!/", jar1URL.getPath().endsWith("unpacked-outer.jar/jar1.jar/"));
      VirtualFile jar1 = outerJar.findChild("jar1.jar");
      assertEquals(jar1URL.getPath(), jar1.toURL().getPath());

      VirtualFile packedJar = vfs.findChild("jar1.jar");
      jar1URL = packedJar.findChild("org/jboss/test/vfs/support").toURL();
      assertTrue("Jar directory entry URLs must end in /: " + jar1URL.toString(), jar1URL.toString().endsWith("/"));
   }

   /**
    * Test that the URI of a VFS corresponding to a directory ends in '/' so that
    * URIs created relative to it are under the directory. This requires that
    * build-test.xml artifacts exist.
    * 
    * @throws Exception
    */
   public void testDirURIs() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      VirtualFile outerJar = vfs.findChild("unpacked-outer.jar");
      URI outerURI = outerJar.toURI();
      log.debug("outerURI: "+outerURI);
      assertTrue(outerURI+" ends in '/'", outerURI.getPath().endsWith("/"));
      // Validate that jar1 is under unpacked-outer.jar
      URI jar1URI = new URI(outerURI+"jar1.jar/");
      log.debug("jar1URI: "+jar1URI+", path="+jar1URI.getPath());
      assertTrue("jar1URI path ends in unpacked-outer.jar/jar1.jar!/", jar1URI.getPath().endsWith("unpacked-outer.jar/jar1.jar/"));
      VirtualFile jar1 = outerJar.findChild("jar1.jar");
      assertEquals(jar1URI.getPath(), jar1.toURI().getPath());

      VirtualFile packedJar = vfs.findChild("jar1.jar");
      jar1URI = packedJar.findChild("org/jboss/test/vfs/support").toURI();
      assertTrue("Jar directory entry URLs must end in /: " + jar1URI.toString(), jar1URI.toString().endsWith("/"));
   }

   /**
    * Test copying a jar
    * 
    * @throws Exception
    */
   public void testCopyJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile jar = vfs.findChild("outer.jar");
      assertTrue("outer.jar != null", jar != null);
      File tmpJar = File.createTempFile("testCopyJar", ".jar");
      tmpJar.deleteOnExit();

      try
      {
         InputStream is = jar.openStream();
         FileOutputStream fos = new FileOutputStream(tmpJar);
         byte[] buffer = new byte[1024];
         int read;
         while( (read = is.read(buffer)) > 0 )
         {
            fos.write(buffer, 0, read);
         }
         fos.close();
         log.debug("outer.jar size is: "+jar.getSize());
         log.debug(tmpJar.getAbsolutePath()+" size is: "+tmpJar.length());
         assertTrue("outer.jar > 0", jar.getSize() > 0);
         assertEquals("copy jar size", jar.getSize(), tmpJar.length());
         jar.close();
      }
      finally
      {
         try
         {
            tmpJar.delete();
         }
         catch(Exception ignore)
         {
         }
      }
   }

   /**
    * Test copying a jar that is nested in another jar.
    * 
    * @throws Exception
    */
   public void testCopyInnerJar()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile outerjar = vfs.findChild("outer.jar");
      assertTrue("outer.jar != null", outerjar != null);
      VirtualFile jar = outerjar.findChild("jar1.jar");
      assertTrue("outer.jar/jar1.jar != null", jar != null);

      File tmpJar = File.createTempFile("testCopyInnerJar", ".jar");
      tmpJar.deleteOnExit();

      try
      {
         InputStream is = jar.openStream();
         FileOutputStream fos = new FileOutputStream(tmpJar);
         byte[] buffer = new byte[1024];
         int read;
         while( (read = is.read(buffer)) > 0 )
         {
            fos.write(buffer, 0, read);
         }
         fos.close();
         log.debug("outer.jar/jar1.jar size is: "+jar.getSize());
         log.debug(tmpJar.getAbsolutePath()+" size is: "+tmpJar.length());
         assertTrue("outer.jar > 0", jar.getSize() > 0);
         assertEquals("copy jar size", jar.getSize(), tmpJar.length());
         jar.close();
      }
      finally
      {
         try
         {
            tmpJar.delete();
         }
         catch(Exception ignore)
         {
         }
      }
   }

   /**
    * Test that the outermf.jar manifest classpath is parsed
    * correctly.
    * 
    * @throws Exception
    */
   public void testManifestClasspath()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile outerjar = vfs.findChild("outermf.jar");
      assertNotNull("outermf.jar != null", outerjar);

      ArrayList<VirtualFile> cp = new ArrayList<VirtualFile>();
      VFSUtils.addManifestLocations(outerjar, cp);
      // The p0.jar should be found in the classpath
      assertEquals("cp size 2", 2, cp.size());
      assertEquals("jar1.jar == cp[0]", "jar1.jar", cp.get(0).getName());
      assertEquals("jar2.jar == cp[1]", "jar2.jar", cp.get(1).getName());
   }
   /**
    * Test that an inner-inner jar that is extracted does not blowup
    * the addManifestLocations routine.
    * 
    * @throws Exception
    */
   public void testInnerManifestClasspath()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile outerjar = vfs.getChild("withalong/rootprefix/outermf.jar");
      assertNotNull(outerjar);
      VirtualFile jar1 = outerjar.getChild("jar1.jar");
      assertNotNull(jar1);
      VirtualFile jar2 = outerjar.getChild("jar2.jar");
      assertNotNull(jar2);
      VirtualFile innerjar = outerjar.getChild("innermf.jar");
      assertNotNull("innermf.jar != null", innerjar);
   
      ArrayList<VirtualFile> cp = new ArrayList<VirtualFile>();
      VFSUtils.addManifestLocations(innerjar, cp);
      assertEquals(2, cp.size());
      VirtualFile cp0 = cp.get(0);
      assertEquals(jar1, cp0);
      VirtualFile cp1 = cp.get(1);
      assertEquals(jar2, cp1);
   }

   /**
    * Validate accessing an packed jar vf and its uri when the vfs path
    * contains spaces
    * @throws Exception
    */
   public void testJarWithSpacesInPath()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tstjar = vfs.findChild("path with spaces/tst.jar");
      assertNotNull("tstjar != null", tstjar);
      URI uri = tstjar.toURI();
      URI expectedURI = new URI("vfs"+rootURL.toString()+"/path%20with%20spaces/tst.jar/");
      assertEquals(expectedURI.getPath(), uri.getPath());

      InputStream is = uri.toURL().openStream();
      is.close();

      tstjar = vfs.findChild("path with spaces/tst%20nospace.jar");
      assertNotNull("tstjar != null", tstjar);
      uri = tstjar.toURI();
      expectedURI = new URI("vfs"+rootURL.toString()+"/path%20with%20spaces/tst%2520nospace.jar/");
      assertEquals(expectedURI.getPath(), uri.getPath());

      is = uri.toURL().openStream();
      is.close();
   }

   public void testJarWithSpacesInContext() throws Exception
   {
      URL rootURL = getResource("/vfs/test/path with spaces");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tstear = vfs.getChild("spaces.ear");
      assertNotNull("spaces.ear != null", tstear);
      URI uri = tstear.toURI();
      URI expectedURI = new URI("vfs"+rootURL.toString()+"/spaces.ear/");
      assertEquals(expectedURI.getPath(), uri.getPath());
      assertFalse(tstear.isLeaf());

      InputStream is = uri.toURL().openStream();
      is.close();

      VirtualFile tstjar = tstear.getChild("spaces-ejb.jar");
      assertNotNull("spaces-ejb.jar != null", tstjar);
      uri = tstjar.toURI();
      expectedURI = new URI("vfs"+rootURL.toString()+"/spaces.ear/spaces-ejb.jar/");
      assertEquals(expectedURI.getPath(), uri.getPath());
      assertFalse(tstjar.isLeaf());

      is = uri.toURL().openStream();
      is.close();

      tstjar = tstear.getChild("spaces-lib.jar");
      assertNotNull("spaces-lib.jar != null", tstjar);
      uri = tstjar.toURI();
      expectedURI = new URI("vfs"+rootURL.toString()+"/spaces.ear/spaces-lib.jar/");
      assertEquals(expectedURI.getPath(), uri.getPath());
      assertFalse(tstjar.isLeaf());

      is = uri.toURL().openStream();
      is.close();
   }

   /**
    * Validate accessing an unpacked jar vf and its uri when the vfs path
    * contains spaces
    * @throws Exception
    */
   public void testUnpackedJarWithSpacesInPath()
      throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tstjar = vfs.findChild("path with spaces/unpacked-tst.jar");
      assertNotNull("tstjar != null", tstjar);
      URI uri = tstjar.toURI();
      URI expectedURI = new URI("vfs" + rootURL.toString()+"/path%20with%20spaces/unpacked-tst.jar/");
      assertEquals(uri, expectedURI);
   }

   /**
    * Tests that we can find the META-INF/some-data.xml in an unpacked deployment
    * 
    * @throws Exception for any error
    */
   public void testGetMetaDataUnpackedJar() throws Exception
   {
      testGetMetaDataFromJar("unpacked-with-metadata.jar");
   }
   
   /**
    * Tests that we can find the META-INF/some-data.xml in a packed deployment
    * 
    * @throws Exception for any error
    */
   public void testGetMetaDataPackedJar() throws Exception
   {
      testGetMetaDataFromJar("with-metadata.jar");
   }
   
   private void testGetMetaDataFromJar(String name) throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      
      VirtualFile jar = vfs.findChild(name);
      assertNotNull(jar);
      VirtualFile metadataLocation = jar.findChild("META-INF");
      assertNotNull(metadataLocation);

      VirtualFile metadataByName = metadataLocation.findChild("some-data.xml");
      assertNotNull(metadataByName);
      
      //This is the same code as is called by AbstractDeploymentContext.getMetaDataFiles(String name, String suffix). 
      //The MetaDataMatchFilter is a copy of the one used there
      List<VirtualFile> metaDataList = metadataLocation.getChildren(new MetaDataMatchFilter(null, "-data.xml"));
      assertNotNull(metaDataList);
      assertEquals("Wrong size", 1, metaDataList.size());
   }

   /**
    * Validate that a URLClassLoader.findReource/getResourceAsStream calls for non-existing absolute
    * resources that should fail as expected with null results. Related to JBMICROCONT-139.
    * 
    * @throws Exception
    */
   public void testURLClassLoaderFindResourceFailure() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      URL[] cp = {vfs.getRoot().toURL()};
      URLClassLoader ucl = new URLClassLoader(cp);
      // Search for a non-existent resource
      URL qp = ucl.findResource("nosuch-quartz.props");
      assertNull("findResource(nosuch-quartz.props)", qp);
      InputStream is = ucl.getResourceAsStream("nosuch-quartz.props");
      assertNull("getResourceAsStream(nosuch-quartz.props)", is);
   }

   /**
    * Test VirtualFile.exists for vfsfile based urls.
    * 
    * @throws Exception
    */
   public void testFileExists()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testFileExists", null, tmpRoot);
      log.info("+++ testFileExists, tmp="+tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tmpVF = vfs.findChild(tmp.getName());
      assertTrue(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue("tmp.delete()", tmpVF.delete());
      assertFalse(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue(tmpRoot+".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsfile based urls for a directory.
    * 
    * @throws Exception
    */
   public void testDirFileExists()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testFileExists", null, tmpRoot);
      assertTrue(tmp+".delete()", tmp.delete());
      assertTrue(tmp+".mkdir()", tmp.mkdir());
      log.info("+++ testDirFileExists, tmp="+tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tmpVF = vfs.findChild(tmp.getName());
      assertTrue(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertFalse(tmpVF.getPathName()+".isLeaf()", tmpVF.isLeaf());
      assertTrue(tmp+".delete()", tmp.delete());
      assertFalse(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue(tmpRoot+".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsjar based urls.
    * 
    * @throws Exception
    */
   public void testJarExists()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmpJar = File.createTempFile("testJarExists", ".jar", tmpRoot);
      log.info("+++ testJarExists, tmpJar="+tmpJar.getCanonicalPath());
      Manifest mf = new Manifest();
      mf.getMainAttributes().putValue("Created-By", "FileVFSUnitTestCase.testJarExists");
      FileOutputStream fos = new FileOutputStream(tmpJar);
      JarOutputStream jos = new JarOutputStream(fos, mf);
      jos.setComment("testJarExists");
      jos.setLevel(0);
      jos.close();

      URL rootURL = tmpRoot.toURI().toURL();
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tmpVF = vfs.findChild(tmpJar.getName());
      assertTrue(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue(tmpVF.getPathName()+".size() > 0", tmpVF.getSize() > 0);
      assertTrue("tmp.delete()", tmpVF.delete());
      assertFalse(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue(tmpRoot+".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsjar based urls for a directory.
    * 
    * @throws Exception
    */
   public void testDirJarExists()
      throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testDirJarExists", ".jar", tmpRoot);
      assertTrue(tmp+".delete()", tmp.delete());
      assertTrue(tmp+".mkdir()", tmp.mkdir());
      log.info("+++ testDirJarExists, tmp="+tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile tmpVF = vfs.findChild(tmp.getName());
      log.info(tmpVF);
      assertTrue(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertFalse(tmpVF.getPathName()+".isLeaf()", tmpVF.isLeaf());
      assertTrue(tmp+".delete()", tmp.delete());
      assertFalse(tmpVF.getPathName()+".exists()", tmpVF.exists());
      assertTrue(tmpRoot+".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.delete() for file based urls
    *
    * @throws Exception
    */
   public void testFileDelete() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      VFS vfs = VFS.getVFS(tmpRoot.toURI().toURL());
      VirtualFile root = vfs.getRoot();

      // non-existent directory - exists() not
      tmpRoot.delete();
      assertFalse(tmpRoot + ".exits() == false", root.exists());

      // existing directory - exists(), delete()
      tmpRoot.mkdir();
      assertTrue(tmpRoot + ".exits()", root.exists());
      assertTrue(tmpRoot + ".delete()", root.delete());
      tmpRoot.mkdir();

      // non-empty directory - delete()
      File tmp = new File(tmpRoot, "testFileDelete.jar");
      assertTrue(tmp.mkdir());
      File tmp2 = File.createTempFile("testFileDelete2", ".jar", tmp);
      assertTrue(tmp2.exists());
      VirtualFile tmpDeletable = VFS.getRoot(tmp.toURI());
      assertTrue(tmpRoot + ".delete() == false", tmpDeletable.delete());

      // create child to vfs
      assertTrue(tmp.mkdir());
      // children() exist
      List<VirtualFile> children = vfs.getChildren();
      assertTrue(tmpRoot + ".getChildren().size() == 1", children.size() == 1);

      // specific child exists(), delete(), exists() not
      VirtualFile tmpVF = vfs.getChild(tmp.getName());
      assertTrue(tmp + ".exists()", tmpVF.exists());
      assertTrue(tmp + ".delete()", tmpVF.delete());
      assertFalse(tmp + ".exists() == false", tmpVF.exists());

      // children() don't exist
      children = vfs.getChildren();
      assertTrue(tmpRoot + ".getChildren().size() == 0", children.size() == 0);

      // getChild() returns null
      tmpVF = vfs.getChild(tmp.getName());
      assertNull(tmpRoot + ".getChild('" + tmp.getName() + "') == null", tmpVF);

      // directory delete()
      assertTrue(tmpRoot + ".delete()", root.delete());
   }

   /**
    * Test for <em>caseSensitive=true</em>
    *
    * If this test passes on unixes, it doesn't mean much, because there it should pass without
    * case sensitivity turned on as well.
    *
    * If it passes on windows, it means the functionality works as expected.
    *
    * @throws Exception for any error
    */
   public void testCaseSensitive() throws Exception
   {
      URL rootURL = getResource("/vfs");

      FileSystemContext ctx = new FileSystemContext(new URL(rootURL.toString() + "?caseSensitive=true"));
      VirtualFileHandler root = ctx.getRoot();

      String path = "context/file/simple/child";
      VirtualFileHandler child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child != null);

      path = "context/file/simple/CHILD";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child == null);

      path = "context/jar/archive.jar";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child != null);

      path = "context/JAR/archive.jar";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child == null);

      path = "context/jar/archive.JAR";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child == null);

      path = "context/jar/archive.jar/child";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child != null);

      path = "context/jar/archive.jar/CHILD";
      child = root.getChild(path);
      assertTrue("getChild('" + path + "')", child == null);
   }
}
