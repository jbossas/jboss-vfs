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
package org.jboss.test.vfs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.vfs.support.ClassPathIterator;
import org.jboss.test.vfs.support.ClassPathIterator.ClassPathEntry;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Tests of the VFS implementation
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision$
 */
public class FileVFSUnitTestCase extends AbstractVFSTest
{
   public FileVFSUnitTestCase(String name)
   {
      super(name);
   }

   public void setUp() throws Exception
   {
      super.setUp();
   }


   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   public static Test suite()
   {
      return new TestSuite(FileVFSUnitTestCase.class);
   }

   /**
    * Test that one can go from a file uri to VirtualFile and obtain the
    * same VirtualFile using VirtualFile vfsfile uri
    * @throws Exception
    */
   public void testVFSFileURIFactory() throws Exception
   {
      URL rootURL = getClass().getProtectionDomain().getCodeSource().getLocation();
      VirtualFile root0 = VFS.getChild(rootURL.getPath());
      VirtualFile root1 = VFS.getChild(root0.toURI().getPath());
      assertEquals(root0, root1);
   }

   /**
    * Test reading the contents of nested jar entries.
    * @throws Exception
    */
   public void testInnerJarFile() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      VirtualFile outerjar = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outerjar);
      try
      {
         assertTrue("outer.jar != null", outerjar != null);
         VirtualFile jar1 = outerjar.getChild("jar1.jar");
         assertTrue("outer.jar/jar1.jar != null", jar1 != null);
         VirtualFile jar2 = outerjar.getChild("jar2.jar");
         assertTrue("outer.jar/jar2.jar != null", jar2 != null);

         VirtualFile jar1MF = jar1.getChild("META-INF/MANIFEST.MF");
         assertNotNull("jar1!/META-INF/MANIFEST.MF", jar1MF);
         InputStream mfIS = jar1MF.openStream();
         Manifest mf1 = new Manifest(mfIS);
         Attributes mainAttrs1 = mf1.getMainAttributes();
         String title1 = mainAttrs1.getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals("jar1", title1);
         mfIS.close();

         VirtualFile jar2MF = jar2.getChild("META-INF/MANIFEST.MF");
         assertNotNull("jar2!/META-INF/MANIFEST.MF", jar2MF);
         InputStream mfIS2 = jar2MF.openStream();
         Manifest mf2 = new Manifest(mfIS2);
         Attributes mainAttrs2 = mf2.getMainAttributes();
         String title2 = mainAttrs2.getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals("jar2", title2);
         mfIS2.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Basic tests of accessing resources in a jar
    * @throws Exception
    */
   public void testFindResource() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile jar = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(jar);
      try
      {
         assertTrue("outer.jar != null", jar != null);

         VirtualFile metaInf = jar.getChild("META-INF/MANIFEST.MF");
         assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
         InputStream mfIS = metaInf.openStream();
         assertTrue("META-INF/MANIFEST.MF.openStream != null", mfIS != null);
         Manifest mf = new Manifest(mfIS);
         Attributes mainAttrs = mf.getMainAttributes();
         String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
         assertEquals("1.0.0.GA", version);
         mfIS.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Basic tests of accessing resources in a jar
    * @throws Exception
    */
   public void testFindResourceUsingURLStream() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile jar = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(jar);
      try
      {
         assertTrue("outer.jar != null", jar != null);

         /*
         ArrayList<String> searchCtx = new ArrayList<String>();
         searchCtx.add("outer.jar");
         VirtualFile metaInf = vfs.resolveFile("META-INF/MANIFEST.MF", searchCtx);
         */
         VirtualFile metaInf = jar.getChild("META-INF/MANIFEST.MF");
         assertTrue("META-INF/MANIFEST.MF != null", metaInf != null);
         System.err.println(metaInf.toURL());
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
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Basic tests of accessing resources in a jar that does not
    * have parent directory entries.
    * @throws Exception
    */
   public void testFindResourceInFilesOnlyJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile jar = testdir.getChild("jar1-filesonly.jar");
      List<Closeable> mounts = recursiveMount(jar);
      try
      {
         assertTrue("jar1-filesonly.jar != null", jar != null);

         VirtualFile metaInf = jar.getChild("META-INF/MANIFEST.MF");
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
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Basic tests of accessing resources in a war that does not
    * have parent directory entries.
    * @throws Exception
    */
   public void testFindResourceInFilesOnlyWar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile war2 = testdir.getChild("WarDeployApp_web.war");
      List<Closeable> mounts = recursiveMount(war2);
      try
      {

         assertTrue("WarDeployApp_web.war != null", war2 != null);

         VirtualFile classes2 = war2.getChild("WEB-INF/classes");
         assertTrue("WEB-INF/classes != null", classes2 != null);
         assertTrue("WEB-INF/classes is not a leaf", classes2.isDirectory());
         assertFalse("WEB-INF/classes is not a leaf", classes2.isFile());
         classes2 = war2.getChild("WEB-INF/classes");
         assertTrue("WEB-INF/classes != null", classes2 != null);
         assertFalse("WEB-INF/classes is not a leaf", classes2.isFile());
         assertTrue("WEB-INF/classes is not a leaf", classes2.isDirectory());

         VirtualFile HelloJavaBean = classes2.getChild("com/sun/ts/tests/webservices/deploy/warDeploy/HelloJavaBean.class");
         assertTrue("HelloJavaBean.class != null", HelloJavaBean != null);
         assertTrue("HelloJavaBean.class is a leaf", HelloJavaBean.isFile());
         assertFalse("HelloJavaBean.class is a leaf", HelloJavaBean.isDirectory());

         VirtualFile war = testdir.getChild("filesonly.war");
         mounts.addAll(recursiveMount(war));

         assertTrue("filesonly.war != null", war != null);

         VirtualFile classes = war.getChild("WEB-INF/classes");
         assertTrue("WEB-INF/classes != null", classes != null);
         assertTrue("WEB-INF/classes is not a directory", classes.isDirectory());

         VirtualFile jar1 = war.getChild("WEB-INF/lib/jar1.jar");
         assertTrue("WEB-INF/lib/jar1.jar != null", jar1 != null);
         assertFalse("WEB-INF/lib/jar1.jar is not a leaf", jar1.isFile());
         assertTrue("WEB-INF/lib/jar1.jar is not a leaf", jar1.isDirectory());
         VirtualFile ClassInJar1 = jar1.getChild("org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         assertTrue("ClassInJar1.class != null", ClassInJar1 != null);
         assertTrue("ClassInJar1.class is a leaf", ClassInJar1.isFile());
         assertFalse("ClassInJar1.class is a leaf", ClassInJar1.isDirectory());

         VirtualFile metaInf = war.getChild("META-INF/MANIFEST.MF");
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

         war.getChild("WEB-INF/classes");
         assertTrue("WEB-INF/classes != null", classes != null);
         assertFalse("WEB-INF/classes is not a leaf", classes.isFile());
         assertTrue("WEB-INF/classes is not a leaf", classes.isDirectory());
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Validate iterating over a vfs url from a files only war.
    *
    * @throws Exception
    */
   public void testFindClassesInFilesOnlyWar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      VirtualFile war = testdir.getChild("filesonly.war");
      List<Closeable> mounts = recursiveMount(war);
      try
      {

         assertTrue("filesonly.war != null", war != null);

         VirtualFile classes = war.getChild("WEB-INF/classes");
         assertTrue("WEB-INF/classes != null", classes != null);
         HashSet<String> names = new HashSet<String>();
         ClassPathIterator iter = new ClassPathIterator(classes.toURL());
         ClassPathEntry entry = null;
         while ((entry = iter.getNextEntry()) != null)
         {
            names.add(entry.name);
         }
         log.debug(names);
         assertTrue("org/jboss/test/vfs/support/jar1", names.contains("org/jboss/test/vfs/support/jar1"));
         assertTrue("ClassInJar1.class", names.contains("org/jboss/test/vfs/support/jar1/ClassInJar1.class"));
         assertTrue("ClassInJar1$InnerClass.class", names.contains("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class"));
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   public void testFindResourceUnpackedJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile jar = testdir.getChild("unpacked-outer.jar");
      assertTrue("unpacked-outer.jar != null", jar != null);

      /**
      ArrayList<String> searchCtx = new ArrayList<String>();
      searchCtx.add("unpacked-outer.jar");
      VirtualFile metaInf = vfs.resolveFile("META-INF/MANIFEST.MF", searchCtx);
      */
      VirtualFile metaInf = jar.getChild("META-INF/MANIFEST.MF");
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
   public void testResolveFile() throws Exception
   {
      log.info("+++ testResolveFile, cwd=" + (new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      // Check resolving the root file
      VirtualFile root = testdir.getChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", rootURL.getPath(), root.getPathName());
      assertFalse("root isFile", root.isFile());
      assertTrue("root isDirectory", root.isDirectory());

      // Find the outer.jar
      VirtualFile outerJar = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outerJar);
      try
      {
         assertNotNull("outer.jar", outerJar);
         assertEquals("outer.jar name", "outer.jar", outerJar.getName());
         assertEquals("outer.jar path", rootURL.getPath() + "/outer.jar", outerJar.getPathName());

         VirtualFile outerJarMF = testdir.getChild("outer.jar/META-INF/MANIFEST.MF");
         assertNotNull("outer.jar/META-INF/MANIFEST.MF", outerJarMF);

         // Test a non-canonical path
         rootURL = getResource("/vfs/sundry/../test");
         // Check resolving the root file
         root = testdir.getChild("");
         assertEquals("root name", "test", root.getName());
         assertEquals("root path", rootURL.getPath(), root.getPathName());
         assertFalse("root isFile", root.isFile());
         assertTrue("root isDirectory", root.isDirectory());
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Validate resolving a .class file given a set of search contexts in the
    * vfs that make up a classpath.
    *
    * @throws Exception
    */
   public void testResolveClassFileInClassPath() throws Exception
   {
      log.info("+++ testResolveFile, cwd=" + (new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      // Find ClassInJar1.class
      VirtualFile vf = testdir.getChild("jar1.jar");
      List<Closeable> mounts = recursiveMount(vf);
      try
      {
         VirtualFile c1 = vf.getChild("org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         assertNotNull("ClassInJar1.class VF", c1);
         log.debug("Found ClassInJar1.class: " + c1);

         // Find ClassInJar1$InnerClass.class
         VirtualFile c1i = vf.getChild("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
         assertNotNull("ClassInJar1$InnerClass.class VF", c1i);
         log.debug("Found ClassInJar1$InnerClass.class: " + c1i);

         // Find ClassInJar2.class
         vf = testdir.getChild("jar2.jar");
         mounts.addAll(recursiveMount(vf));
         VirtualFile c2 = vf.getChild("org/jboss/test/vfs/support/jar2/ClassInJar2.class");
         assertNotNull("ClassInJar2.class VF", c2);
         log.debug("Found ClassInJar2.class: " + c2);
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   public void testResolveFileInUnpackedJar() throws Exception
   {
      log.info("+++ testResolveFileInUnpackedJar, cwd=" + (new File(".").getCanonicalPath()));
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      // Check resolving the root file
      VirtualFile root = testdir.getChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", rootURL.getPath(), root.getPathName());
      assertTrue("root isDirectory", root.isDirectory());
      assertFalse("root isFile", root.isFile());

      // Find the outer.jar
      VirtualFile outerJar = testdir.getChild("unpacked-outer.jar");
      assertNotNull("unpacked-outer.jar", outerJar);
      assertEquals("unpacked-outer.jar name", "unpacked-outer.jar", outerJar.getName());
      assertEquals("unpacked-outer.jar path", rootURL.getPath() + "/unpacked-outer.jar", outerJar.getPathName());

      VirtualFile outerJarMF = testdir.getChild("unpacked-outer.jar/META-INF/MANIFEST.MF");
      assertNotNull("unpacked-outer.jar/META-INF/MANIFEST.MF", outerJarMF);

      // Check resolving the root file
      root = testdir.getChild("");
      assertEquals("root name", "test", root.getName());
      assertEquals("root path", rootURL.getPath(), root.getPathName());
      assertTrue("root isDirectory", root.isDirectory());
      assertFalse("root isFile", root.isFile());
   }

   public void testFileNotFoundInUnpackedJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      // Find the outer.jar
      VirtualFile outerJar = testdir.getChild("unpacked-outer.jar");
      assertNotNull("unpacked-outer.jar", outerJar);
      assertFalse(outerJar.getChild("WEB-INF").exists());
   }

   public void testNestedNestedParent() throws Exception
   {
      // TODO
   }

   public void testCopyNestedStream() throws Exception
   {
      // TODO
   }

   /**
    * Test file resolution with nested jars
    * @throws Exception
    */
   public void testInnerJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outer = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outer);
      try
      {
         VirtualFile inner = testdir.getChild("outer.jar/jar1.jar");
         log.info("IsFile: " + inner.isFile());
         log.info(inner.getLastModified());
         List<VirtualFile> contents = inner.getChildren();
         // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
         assertTrue("jar1.jar children.length(" + contents.size() + ") >= 2", contents.size() >= 2);
         for (VirtualFile vf : contents)
         {
            log.info("  " + vf.getName());
         }
         VirtualFile vf = testdir.getChild("outer.jar/jar1.jar");
         VirtualFile jar1MF = vf.getChild("META-INF/MANIFEST.MF");
         InputStream mfIS = jar1MF.openStream();
         Manifest mf = new Manifest(mfIS);
         Attributes mainAttrs = mf.getMainAttributes();
         String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
         mfIS.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   public void testInnerJarUsingURLStream() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outer = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outer);
      try
      {
         VirtualFile inner = testdir.getChild("outer.jar/jar1.jar");
         log.info("IsFile: " + inner.isFile());
         log.info(inner.getLastModified());
         List<VirtualFile> contents = inner.getChildren();
         // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
         assertTrue("jar1.jar children.length(" + contents.size() + ") >= 2", contents.size() >= 2);
         for (VirtualFile vf : contents)
         {
            log.info("  " + vf.getName());
         }
         VirtualFile vf = testdir.getChild("outer.jar/jar1.jar");
         VirtualFile jar1MF = vf.getChild("META-INF/MANIFEST.MF");
         InputStream mfIS = jar1MF.toURL().openStream();
         Manifest mf = new Manifest(mfIS);
         Attributes mainAttrs = mf.getMainAttributes();
         String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
         mfIS.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test a scan of the outer.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScan() throws Exception
   {
      URL rootURL = getResource("/vfs/test/outer.jar");
      VirtualFile outer = VFS.getChild(rootURL.getPath());
      List<Closeable> mounts = recursiveMount(outer);
      try
      {

         HashSet<String> expectedClasses = new HashSet<String>();
         expectedClasses.add(outer.getPathName() + "/jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         expectedClasses.add(outer.getPathName() + "/jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
         expectedClasses.add(outer.getPathName() + "/jar1-filesonly.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         expectedClasses.add(outer.getPathName() + "/jar1-filesonly.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
         expectedClasses.add(outer.getPathName() + "/jar2.jar/org/jboss/test/vfs/support/jar2/ClassInJar2.class");
         expectedClasses.add(outer.getPathName() + "/org/jboss/test/vfs/support/CommonClass.class");
         super.enableTrace("org.jboss.vfs.util.SuffixMatchFilter");
         SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
         List<VirtualFile> classes = outer.getChildren(classVisitor);
         int count = 0;
         for (VirtualFile cf : classes)
         {
            String path = cf.getPathName();
            if (path.endsWith(".class"))
            {
               assertTrue(path, expectedClasses.contains(path));
               count++;
            }
         }
         assertEquals("There were 6 classes", 6, count);
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test a scan of the unpacked-outer.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScanUnpacked() throws Exception
   {
      URL rootURL = getResource("/vfs/test/unpacked-outer.jar");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      List<Closeable> mounts = recursiveMount(testdir);
      try
      {

         HashSet<String> expectedClasses = new HashSet<String>();
         expectedClasses.add(rootURL.getPath() + "/jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         expectedClasses.add(rootURL.getPath() + "/jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
         expectedClasses.add(rootURL.getPath() + "/jar2.jar/org/jboss/test/vfs/support/jar2/ClassInJar2.class");
         // FIXME: .class files are not being copied from the resources directory
         expectedClasses.add(rootURL.getPath() + "/org/jboss/test/vfs/support/CommonClass.class");
         super.enableTrace("org.jboss.vfs.util.SuffixMatchFilter");
         SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
         List<VirtualFile> classes = testdir.getChildren(classVisitor);
         int count = 0;
         for (VirtualFile cf : classes)
         {
            String path = cf.getPathName();
            if (path.endsWith(".class"))
            {
               assertTrue(path, expectedClasses.contains(path));
               count++;
            }
         }
         assertEquals("There were 4 classes", 4, count);
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test a scan of the jar1-filesonly.jar vfs to locate all .class files
    * @throws Exception
    */
   public void testClassScanFilesonly() throws Exception
   {
      URL rootURL = getResource("/vfs/test/jar1-filesonly.jar");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      List<Closeable> mounts = recursiveMount(testdir);
      try
      {

         HashSet<String> expectedClasses = new HashSet<String>();
         expectedClasses.add(rootURL.getPath() + "/org/jboss/test/vfs/support/jar1/ClassInJar1.class");
         expectedClasses.add(rootURL.getPath() + "/org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class");
         super.enableTrace("org.jboss.vfs.util.SuffixMatchFilter");
         SuffixMatchFilter classVisitor = new SuffixMatchFilter(".class", VisitorAttributes.RECURSE);
         List<VirtualFile> classes = testdir.getChildren(classVisitor);
         int count = 0;
         for (VirtualFile cf : classes)
         {
            String path = cf.getPathName();
            if (path.endsWith(".class"))
            {
               assertTrue(path, expectedClasses.contains(path));
               count++;
            }
         }
         assertEquals("There were 2 classes", 2, count);

         // Make sure we can walk path-wise to the class
         VirtualFile parent = testdir;
         String className = "org/jboss/test/vfs/support/jar1/ClassInJar1.class";
         VirtualFile classInJar1 = testdir.getChild(className);
         String[] paths = className.split("/");
         StringBuilder vfsPath = new StringBuilder();
         for (String path : paths)
         {
            vfsPath.append(path);
            VirtualFile vf = parent.getChild(path);
            if (path.equals("ClassInJar1.class"))
               assertEquals("ClassInJar1.class", classInJar1, vf);
            else
            {
               assertEquals("vfsPath", testdir.getPathName() + "/" + vfsPath.toString(), vf.getPathName());
               // why should this be equal?
               // assertEquals("lastModified", classInJar1.getLastModified(), vf.getLastModified());
               assertTrue("lastModified", classInJar1.getLastModified() <= vf.getLastModified());
            }
            vfsPath.append('/');
            parent = vf;
         }
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test access of directories in a jar that only stores files
    * @throws Exception
    */
   public void testFilesOnlyJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      VirtualFile jar = testdir.getChild("jar1-filesonly.jar");
      List<Closeable> mounts = recursiveMount(jar);
      try
      {

         VirtualFile metadataLocation = jar.getChild("META-INF");
         assertNotNull(metadataLocation);
         VirtualFile mfFile = metadataLocation.getChild("MANIFEST.MF");
         assertNotNull(mfFile);
         InputStream is = mfFile.openStream();
         Manifest mf = new Manifest(is);
         is.close();
         String title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1-filesonly", title);

         // Retry starting from the jar root
         mfFile = jar.getChild("META-INF/MANIFEST.MF");
         is = mfFile.openStream();
         mf = new Manifest(is);
         is.close();
         title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
         assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1-filesonly", title);
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test the serialization of VirtualFiles
    * @throws Exception
    */
   public void testVFSerialization() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      tmpRoot.deleteOnExit();
      File tmp = new File(tmpRoot, "vfs.ser");
      tmp.createNewFile();
      tmp.deleteOnExit();
      log.info("+++ testVFSerialization, tmp=" + tmp.getCanonicalPath());
      URL rootURL = tmpRoot.toURI().toURL();
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tmpVF = testdir.getChild("vfs.ser");
      FileOutputStream fos = new FileOutputStream(tmp);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(tmpVF);
      oos.close();

      // Check the tmpVF attributes against the tmp file
      long lastModified = tmp.lastModified();
      long size = tmp.length();
      String name = tmp.getName();
      String vfsPath = tmp.getPath();
      URL url = tmp.toURI().toURL();
      log.debug("name: " + name);
      log.debug("vfsPath: " + vfsPath);
      log.debug("url: " + url);
      log.debug("lastModified: " + lastModified);
      log.debug("size: " + size);
      assertEquals("name", name, tmpVF.getName());
      assertEquals("pathName", vfsPath, tmpVF.getPathName());
      assertEquals("lastModified", lastModified, tmpVF.getLastModified());
      assertEquals("size", size, tmpVF.getSize());
      assertEquals("url", url, tmpVF.toURL());
      assertTrue("isFile", tmpVF.isFile());
      assertFalse("isDirectory", tmpVF.isDirectory());
      //assertEquals("isHidden", false, tmpVF.isHidden());

      // Read in the VF from the serialized file
      FileInputStream fis = new FileInputStream(tmp);
      ObjectInputStream ois = new ObjectInputStream(fis);
      VirtualFile tmpVF2 = (VirtualFile)ois.readObject();
      ois.close();
      // Validated the deserialized attribtes against the tmp file
      assertEquals("name", name, tmpVF2.getName());
      assertEquals("pathName", vfsPath, tmpVF2.getPathName());
      assertEquals("lastModified", lastModified, tmpVF2.getLastModified());
      assertEquals("size", size, tmpVF2.getSize());
      assertEquals("url", url, tmpVF2.toURL());
      assertTrue("isFile", tmpVF2.isFile());
      assertFalse("isDirectory", tmpVF2.isDirectory());
      //assertEquals("isHidden", false, tmpVF2.isHidden());
   }

   /**
    * Test the serialization of VirtualFiles representing a jar
    * @throws Exception
    */
   public void testVFJarSerialization() throws Exception
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
      log.info("+++ testVFJarSerialization, tmp=" + tmpJar.getCanonicalPath());

      URI rootURI = tmpRoot.toURI();
      VirtualFile tmp = VFS.getChild(rootURI.getPath());
      File vfsSer = new File(tmpRoot, "vfs.ser");
      vfsSer.createNewFile();
      vfsSer.deleteOnExit();

      VirtualFile tmpVF = tmp.getChild("tst.jar");
      // Validate the vf jar against the tmp file attributes
      long lastModified = tmpJar.lastModified();
      long size = tmpJar.length();
      String name = tmpJar.getName();
      String vfsPath = tmpJar.getPath();
      URL url = tmpJar.toURL();
      //url = JarUtils.createJarURL(url);
      log.debug("name: " + name);
      log.debug("vfsPath: " + vfsPath);
      log.debug("url: " + url);
      log.debug("lastModified: " + lastModified);
      log.debug("size: " + size);
      assertEquals("name", name, tmpVF.getName());
      assertEquals("pathName", vfsPath, tmpVF.getPathName());
      assertEquals("lastModified", lastModified, tmpVF.getLastModified());
      assertEquals("size", size, tmpVF.getSize());
      assertEquals("url", url.getPath(), tmpVF.toURL().getPath());
      assertFalse("isDirectory", tmpVF.isDirectory());
      assertTrue("isFile", tmpVF.isFile());
      //assertEquals("isHidden", false, tmpVF.isHidden());
      // Write out the vfs jar file
      fos = new FileOutputStream(vfsSer);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(tmpVF);
      oos.close();

      // Read in the VF from the serialized file
      FileInputStream fis = new FileInputStream(vfsSer);
      ObjectInputStream ois = new ObjectInputStream(fis);
      VirtualFile tmpVF2 = (VirtualFile)ois.readObject();
      ois.close();
      // Validate the vf jar against the tmp file attributes
      assertEquals("name", name, tmpVF2.getName());
      assertEquals("pathName", vfsPath, tmpVF2.getPathName());
      assertEquals("lastModified", lastModified, tmpVF2.getLastModified());
      assertEquals("size", size, tmpVF2.getSize());
      assertEquals("url", url.getPath(), tmpVF2.toURL().getPath());
      assertFalse("isDirectory", tmpVF2.isDirectory());
      assertTrue("isFile", tmpVF2.isFile());
      //assertEquals("isHidden", false, tmpVF2.isHidden());
   }

   /**
    * Test the serialization of VirtualFiles representing a jar
    * @throws Exception
    */
   public void testVFNestedJarSerialization() throws Exception
   {
      // this expects to be run with a working dir of the container root
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outer = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outer);
      try
      {
         VirtualFile inner = outer.getChild("jar1.jar");

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
         inner = (VirtualFile)ois.readObject();
         ois.close();
         List<VirtualFile> contents = inner.getChildren();
         // META-INF/*, org/jboss/test/vfs/support/jar1/* at least
         // TODO - fix this once no_copy serialization is working
         int size = 2;
         assertTrue("jar1.jar children.length(" + contents.size() + ") is not " + size, contents.size() >= size);
         for (VirtualFile vf : contents)
         {
            log.info("  " + vf.getName());
         }
         VirtualFile vf = testdir.getChild("outer.jar/jar1.jar");
         /*
               VirtualFile jar1MF = vf.getChild("META-INF/MANIFEST.MF");
               InputStream mfIS = jar1MF.openStream();
               Manifest mf = new Manifest(mfIS);
               Attributes mainAttrs = mf.getMainAttributes();
               String version = mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
               assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1", version);
               mfIS.close();
         */
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
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
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      VirtualFile outerJar = testdir.getChild("unpacked-outer.jar");
      URL outerURL = outerJar.toURL();
      log.debug("outerURL: " + outerURL);
      assertTrue(outerURL + " ends in '/'", outerURL.getPath().endsWith("/"));
      // Validate that jar1 is under unpacked-outer.jar
      URL jar1URL = new URL(outerURL, "jar1.jar/");
      log.debug("jar1URL: " + jar1URL + ", path=" + jar1URL.getPath());
      assertTrue("jar1URL path ends in unpacked-outer.jar/jar1.jar!/", jar1URL.getPath().endsWith("unpacked-outer.jar/jar1.jar/"));
      VirtualFile jar1 = outerJar.getChild("jar1.jar");
      List<Closeable> mounts = recursiveMount(jar1);
      try
      {
         assertEquals(jar1URL.getPath(), jar1.toURL().getPath());

         VirtualFile packedJar = testdir.getChild("jar1.jar");
         mounts.addAll(recursiveMount(packedJar));
         jar1URL = packedJar.getChild("org/jboss/test/vfs/support").toURL();
         assertTrue("Jar directory entry URLs must end in /: " + jar1URL.toString(), jar1URL.toString().endsWith("/"));
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
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
      VirtualFile testdir = VFS.getChild(rootURL.getPath());

      VirtualFile outerJar = testdir.getChild("unpacked-outer.jar");
      URI outerURI = outerJar.toURI();
      log.debug("outerURI: " + outerURI);
      assertTrue(outerURI + " ends in '/'", outerURI.getPath().endsWith("/"));
      // Validate that jar1 is under unpacked-outer.jar
      URI jar1URI = new URI(outerURI + "jar1.jar/");
      log.debug("jar1URI: " + jar1URI + ", path=" + jar1URI.getPath());
      assertTrue("jar1URI path ends in unpacked-outer.jar/jar1.jar!/", jar1URI.getPath().endsWith("unpacked-outer.jar/jar1.jar/"));
      VirtualFile jar1 = outerJar.getChild("jar1.jar");
      List<Closeable> mounts = recursiveMount(jar1);
      try
      {
         assertEquals(jar1URI.getPath(), jar1.toURI().getPath());

         VirtualFile packedJar = testdir.getChild("jar1.jar");
         mounts.addAll(recursiveMount(packedJar));
         jar1URI = packedJar.getChild("org/jboss/test/vfs/support").toURI();
         assertTrue("Jar directory entry URLs must end in /: " + jar1URI.toString(), jar1URI.toString().endsWith("/"));
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test copying a jar
    *
    * @throws Exception
    */
   public void testCopyJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile jar = testdir.getChild("outer.jar");
      assertTrue("outer.jar != null", jar != null);
      File tmpJar = File.createTempFile("testCopyJar", ".jar");
      tmpJar.deleteOnExit();

      try
      {
         InputStream is = jar.openStream();
         FileOutputStream fos = new FileOutputStream(tmpJar);
         byte[] buffer = new byte[1024];
         int read;
         while ((read = is.read(buffer)) > 0)
         {
            fos.write(buffer, 0, read);
         }
         fos.close();
         log.debug("outer.jar size is: " + jar.getSize());
         log.debug(tmpJar.getAbsolutePath() + " size is: " + tmpJar.length());
         assertTrue("outer.jar > 0", jar.getSize() > 0);
         assertEquals("copy jar size", jar.getSize(), tmpJar.length());
         is.close();
      }
      finally
      {
         try
         {
            tmpJar.delete();
         }
         catch (Exception ignore)
         {
         }
      }
   }

   /**
    * Test copying a jar that is nested in another jar.
    *
    * @throws Exception
    */
   public void testCopyInnerJar() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outerjar = testdir.getChild("outer.jar");
      List<Closeable> mounts = recursiveMount(outerjar);
      try
      {
         assertTrue("outer.jar != null", outerjar != null);
         VirtualFile jar = outerjar.getChild("jar1.jar");
         assertTrue("outer.jar/jar1.jar != null", jar != null);

         File tmpJar = File.createTempFile("testCopyInnerJar", ".jar");
         tmpJar.deleteOnExit();

         try
         {
            InputStream is = jar.openStream();
            
            assertTrue(is instanceof JarInputStream);
         }
         finally
         {
            try
            {
               tmpJar.delete();
            }
            catch (Exception ignore)
            {
            }
         }
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test that the outermf.jar manifest classpath is parsed
    * correctly.
    *
    * @throws Exception
    */
   public void testManifestClasspath() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outerjar = testdir.getChild("outermf.jar");
      List<Closeable> mounts = recursiveMount(outerjar);
      try
      {

         assertNotNull("outermf.jar != null", outerjar);

         ArrayList<VirtualFile> cp = new ArrayList<VirtualFile>();
         VFSUtils.addManifestLocations(outerjar, cp);
         // The p0.jar should be found in the classpath
         assertEquals("cp size 2", 2, cp.size());
         assertEquals("jar1.jar == cp[0]", "jar1.jar", cp.get(0).getName());
         assertEquals("jar2.jar == cp[1]", "jar2.jar", cp.get(1).getName());
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Test that an inner-inner jar that is extracted does not blowup
    * the addManifestLocations routine.
    *
    * @throws Exception
    */
   public void testInnerManifestClasspath() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile outerjar = testdir.getChild("withalong/rootprefix/outermf.jar");
      assertNotNull(outerjar);
      List<Closeable> mounts = recursiveMount(outerjar);
      try
      {
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
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Validate accessing an packed jar vf and its uri when the vfs path
    * contains spaces
    * @throws Exception
    */
   public void testJarWithSpacesInPath() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tstjar = testdir.getChild("path with spaces/tst.jar");
      List<Closeable> mounts = recursiveMount(tstjar);
      try
      {
         assertNotNull("tstjar != null", tstjar);
         URI uri = tstjar.toURI();
         URI expectedURI = new URI("vfs" + rootURL.toString() + "/path%20with%20spaces/tst.jar/");
         assertEquals(expectedURI.getPath(), uri.getPath());

         InputStream is = uri.toURL().openStream();
         is.close();

         tstjar = testdir.getChild("path with spaces/tst%20nospace.jar");
         mounts.addAll(recursiveMount(tstjar));
         assertNotNull("tstjar != null", tstjar);
         uri = tstjar.toURI();
         expectedURI = new URI("vfs" + rootURL.toString() + "/path%20with%20spaces/tst%2520nospace.jar/");
         assertEquals(expectedURI.getPath(), uri.getPath());

         is = uri.toURL().openStream();
         is.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   public void testJarWithSpacesInContext() throws Exception
   {
      URL rootURL = getResource("/vfs/test/path with spaces");
      VirtualFile testdir = VFS.getChild(URLDecoder.decode(rootURL.getPath(), "UTF-8"));
      VirtualFile tstear = testdir.getChild("spaces.ear");
      List<Closeable> mounts = recursiveMount(tstear);
      try
      {
         assertNotNull("spaces.ear != null", tstear);
         assertTrue(tstear.isDirectory());
         URI uri = tstear.toURI();
         URI expectedURI = new URI("vfs" + rootURL.toString() + "/spaces.ear/");
         assertEquals(expectedURI.getPath(), uri.getPath());

         InputStream is = uri.toURL().openStream();
         is.close();

         VirtualFile tstjar = tstear.getChild("spaces-ejb.jar");
         assertNotNull("spaces-ejb.jar != null", tstjar);
         uri = tstjar.toURI();
         expectedURI = new URI("vfs" + rootURL.toString() + "/spaces.ear/spaces-ejb.jar/");
         assertEquals(expectedURI.getPath(), uri.getPath());
         assertFalse(tstjar.isFile());
         assertTrue(tstjar.isDirectory());

         is = uri.toURL().openStream();
         is.close();

         tstjar = tstear.getChild("spaces-lib.jar");
         assertNotNull("spaces-lib.jar != null", tstjar);
         uri = tstjar.toURI();
         expectedURI = new URI("vfs" + rootURL.toString() + "/spaces.ear/spaces-lib.jar/");
         assertEquals(expectedURI.getPath(), uri.getPath());
         assertFalse(tstjar.isFile());
         assertTrue(tstjar.isDirectory());

         is = uri.toURL().openStream();
         is.close();
      }
      finally
      {
         VFSUtils.safeClose(mounts);
      }
   }

   /**
    * Validate accessing an unpacked jar vf and its uri when the vfs path
    * contains spaces
    * @throws Exception
    */
   public void testUnpackedJarWithSpacesInPath() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tstjar = testdir.getChild("path with spaces/unpacked-tst.jar");
      assertNotNull("tstjar != null", tstjar);
      URI uri = tstjar.toURI();
      URI expectedURI = new URI(rootURL.toString() + "/path%20with%20spaces/unpacked-tst.jar/");
      assertEquals(uri, expectedURI);
   }

   //   /**
   //    * Tests that we can find the META-INF/some-data.xml in an unpacked deployment
   //    *
   //    * @throws Exception for any error
   //    */
   //   public void testGetMetaDataUnpackedJar() throws Exception
   //   {
   //      testGetMetaDataFromJar("unpacked-with-metadata.jar");
   //   }
   //
   //   /**
   //    * Tests that we can find the META-INF/some-data.xml in a packed deployment
   //    *
   //    * @throws Exception for any error
   //    */
   //   public void testGetMetaDataPackedJar() throws Exception
   //   {
   //      testGetMetaDataFromJar("with-metadata.jar");
   //   }

   //   private void testGetMetaDataFromJar(String name) throws Exception
   //   {
   //      URL rootURL = getResource("/vfs/test");
   //     VirtualFile testdir = VFS.getChild(rootURL.getPath());
   //
   //      VirtualFile jar = testdir.getChild(name);
   //      assertNotNull(jar);
   //      VirtualFile metadataLocation = jar.getChild("META-INF");
   //      assertNotNull(metadataLocation);
   //
   //      VirtualFile metadataByName = metadataLocation.getChild("some-data.xml");
   //      assertNotNull(metadataByName);
   //
   //      //This is the same code as is called by AbstractDeploymentContext.getMetaDataFiles(String name, String suffix).
   //      //The MetaDataMatchFilter is a copy of the one used there
   //      List<VirtualFile> metaDataList = metadataLocation.getChildren(new MetaDataMatchFilter(null, "-data.xml"));
   //      assertNotNull(metaDataList);
   //      assertEquals("Wrong size", 1, metaDataList.size());
   //   }

   /**
    * Validate that a URLClassLoader.findReource/getResourceAsStream calls for non-existing absolute
    * resources that should fail as expected with null results. Related to JBMICROCONT-139.
    *
    * @throws Exception
    */
   public void testURLClassLoaderFindResourceFailure() throws Exception
   {
      URL rootURL = getResource("/vfs/test");
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      URL[] cp = { testdir.toURL() };
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
   public void testFileExists() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testFileExists", null, tmpRoot);
      log.info("+++ testFileExists, tmp=" + tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tmpVF = testdir.getChild(tmp.getName());
      assertTrue(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue("tmp.delete()", tmpVF.delete());
      assertFalse(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue(tmpRoot + ".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsfile based urls for a directory.
    *
    * @throws Exception
    */
   public void testDirFileExists() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testFileExists", null, tmpRoot);
      assertTrue(tmp + ".delete()", tmp.delete());
      assertTrue(tmp + ".mkdir()", tmp.mkdir());
      log.info("+++ testDirFileExists, tmp=" + tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tmpVF = testdir.getChild(tmp.getName());
      assertTrue(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertFalse(tmpVF.getPathName() + ".isFile()", tmpVF.isFile());
      assertTrue(tmp + ".delete()", tmp.delete());
      assertFalse(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue(tmpRoot + ".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsjar based urls.
    *
    * @throws Exception
    */
   public void testJarExists() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmpJar = File.createTempFile("testJarExists", ".jar", tmpRoot);
      log.info("+++ testJarExists, tmpJar=" + tmpJar.getCanonicalPath());
      Manifest mf = new Manifest();
      mf.getMainAttributes().putValue("Created-By", "FileVFSUnitTestCase.testJarExists");
      FileOutputStream fos = new FileOutputStream(tmpJar);
      JarOutputStream jos = new JarOutputStream(fos, mf);
      jos.setComment("testJarExists");
      jos.setLevel(0);
      jos.close();

      URL rootURL = tmpRoot.toURI().toURL();
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tmpVF = testdir.getChild(tmpJar.getName());
      assertTrue(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue(tmpVF.getPathName() + ".size() > 0", tmpVF.getSize() > 0);
      assertTrue("tmp.delete()", tmpVF.delete());
      assertFalse(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue(tmpRoot + ".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.exists for vfsjar based urls for a directory.
    *
    * @throws Exception
    */
   public void testDirJarExists() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();
      File tmp = File.createTempFile("testDirJarExists", ".jar", tmpRoot);
      assertTrue(tmp + ".delete()", tmp.delete());
      assertTrue(tmp + ".mkdir()", tmp.mkdir());
      log.info("+++ testDirJarExists, tmp=" + tmp.getCanonicalPath());

      URL rootURL = tmpRoot.toURI().toURL();
      VirtualFile testdir = VFS.getChild(rootURL.getPath());
      VirtualFile tmpVF = testdir.getChild(tmp.getName());
      log.info(tmpVF);
      assertTrue(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertFalse(tmpVF.getPathName() + ".isFile()", tmpVF.isFile());
      assertTrue(tmp + ".delete()", tmp.delete());
      assertFalse(tmpVF.getPathName() + ".exists()", tmpVF.exists());
      assertTrue(tmpRoot + ".delete()", tmpRoot.delete());
   }

   /**
    * Test VirtualFile.delete() for file based urls
    *
    * @throws Exception
    */
   public void testFileDelete() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      VirtualFile root = VFS.getChild(tmpRoot.getPath());

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
      VirtualFile tmpDeletable = VFS.getChild(tmp.toURI());
      assertFalse(tmpRoot + ".delete() == false", tmpDeletable.delete());

      // children() exist
      List<VirtualFile> children = root.getChildren();
      assertEquals(tmpRoot + ".getChildren().size() == 1", 1, children.size());

      // specific child exists(), delete(), exists() not
      VirtualFile tmpVF = root.getChild(tmp.getName());
      assertTrue(tmp + ".exists()", tmpVF.exists());
      assertTrue(tmp + ".delete()", tmp2.delete());
      assertTrue(tmp + ".delete()", tmpVF.delete());
      assertFalse(tmp + ".exists() == false", tmpVF.exists());

      // children() don't exist
      children = root.getChildren();
      assertTrue(tmpRoot + ".getChildren().size() == 0", children.size() == 0);

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
   //   public void testCaseSensitive() throws Exception
   //   {
   //      URL rootURL = getResource("/vfs");
   //
   //      FileSystemContext ctx = new FileSystemContext(new URL(rootURL.toString() + "?caseSensitive=true"));
   //      VirtualFileHandler root = ctx.getRoot();
   //
   //      String path = "context/file/simple/child";
   //      VirtualFileHandler child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child != null);
   //
   //      path = "context/file/simple/CHILD";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child == null);
   //
   //      path = "context/jar/archive.jar";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child != null);
   //
   //      path = "context/JAR/archive.jar";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child == null);
   //
   //      path = "context/jar/archive.JAR";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child == null);
   //
   //      path = "context/jar/archive.jar/child";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child != null);
   //
   //      path = "context/jar/archive.jar/CHILD";
   //      child = root.getChild(path);
   //      assertTrue("getChild('" + path + "')", child == null);
   //   }
}
