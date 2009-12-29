/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.tools.ant.filters.StringInputStream;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.spi.OneWaySynchronizedCopyFileSystem;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

/**
 * TestCase to verify the functionality of a {@link OneWaySynchronizedCopyFileSystem}.
 * 
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class OneWaySynchronizedCopyFileSystemTestCase extends AbstractVFSTest
{

   private TempFileProvider tempFileProvider;

   public OneWaySynchronizedCopyFileSystemTestCase(String name)
   {
      super(name);
   }
   
   /**
    * Verify files are correctly copied from the original root
    * 
    * @throws Exception
    */
   @Test
   public void testExistingFiles() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         
         TempDir dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
       
         assertFalse(new File(dirRoot, "META-INF").exists());
         assertTrue(newVirtualFile.getChild("META-INF").exists());
         assertTrue(new File(dirRoot, "META-INF").exists());
         assertTrue(newVirtualFile.getChild("META-INF").isDirectory());
         
         assertFalse(newVirtualFile.getChild("missing.txt").exists());
         
         File manifest = newVirtualFile.getChild("META-INF/MANIFEST.MF").getPhysicalFile();
         assertTrue(manifest.exists());
         
         
         assertCoppied(existing.getChild("META-INF/MANIFEST.MF"), newVirtualFile.getChild("META-INF/MANIFEST.MF"));
         
         List<VirtualFile> children = newVirtualFile.getChildren();
         assertEquals(2, children.size());
         assertTrue(children.contains(newVirtualFile.getChild("org")));
         assertTrue(children.contains(newVirtualFile.getChild("META-INF")));
         
         assertFalse(new File(dirRoot, "org").exists());
         assertTrue(newVirtualFile.getChild("org").exists());
         assertTrue(new File(dirRoot, "org").exists());
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   /**
    * Verify that copies that are added to the original are copied to the temporary location
    * 
    * @throws Exception
    */
   @Test
   public void testAddFilesToOriginal() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         TempDir dir = getTempFileProvider().createTempDir("existing-jar1");
         File existingDir = dir.getRoot();
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         mounts.add(VFS.mountReal(existingDir, existing));
         
         dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
   
         assertEmpty(newVirtualFile.getChildren());
         
         File addedDir = new File(existingDir, "META-INF");
         addedDir.mkdir();
         
         assertTrue(newVirtualFile.getChild("META-INF").exists());
         assertTrue(new File(dirRoot, "META-INF").exists());
         assertTrue(newVirtualFile.getChild("META-INF").isDirectory());
         
         assertFalse(newVirtualFile.getChild("test.txt").exists());
         
         File addedFile = new File(existingDir, "test.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertTrue(existing.getChild("test.txt").exists());
         
         assertFalse(new File(dirRoot, "test.txt").exists());
         
         List<VirtualFile> children = newVirtualFile.getChildren();
         assertEquals(2, children.size());
         assertTrue(children.contains(newVirtualFile.getChild("test.txt")));
         assertTrue(children.contains(newVirtualFile.getChild("META-INF")));
         
         assertFalse(new File(dirRoot, "test.txt").exists());
         
         assertTrue(newVirtualFile.getChild("test.txt").exists());
         assertTrue(new File(dirRoot, "test.txt").exists());
         
         assertCoppied(existing.getChild("test.txt"), newVirtualFile.getChild("test.txt"));
         
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   /**
    * Verify that files added to the temporary copy are not delete because they are missing in the original
    * 
    * @throws Exception
    */
   @Test
   public void testAddFilesToCopy() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         TempDir dir = getTempFileProvider().createTempDir("existing-jar1");
         File existingDir = dir.getRoot();
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         mounts.add(VFS.mountReal(existingDir, existing));
         
         dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
   
         assertEmpty(newVirtualFile.getChildren());
         
         File addedDir = new File(dirRoot, "META-INF");
         addedDir.mkdir();
         assertTrue(addedDir.exists());
         
         assertTrue(newVirtualFile.getChild("META-INF").exists());
         
         File addedFile = new File(dirRoot, "test.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertFalse(existing.getChild("test.txt").exists());
         
         assertTrue(newVirtualFile.getChild("test.txt").exists());
         
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   /**
    * Verify that changes made to the original are correctly made to the copy
    * 
    * @throws Exception
    */
   @Test
   public void testUpdateFilesFromOriginal() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         TempDir dir = getTempFileProvider().createTempDir("existing-jar1");
         File existingDir = dir.getRoot();
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         mounts.add(VFS.mountReal(existingDir, existing));
         
         dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
   
         
         File addedFile = new File(existingDir, "test.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertTrue(newVirtualFile.getChild("test.txt").exists());
         assertTrue(new File(dirRoot, "test.txt").exists());
         assertCoppied(existing.getChild("test.txt"), newVirtualFile.getChild("test.txt"));
         
         Thread.sleep(1000);
         
         VFSUtils.copyStreamAndClose(new StringInputStream("Some other text"), new FileOutputStream(addedFile));
         
         assertCoppied(existing.getChild("test.txt"), newVirtualFile.getChild("test.txt"));
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   /**
    * Verify that files deleted from the original are deleted from the copy
    * 
    * @throws Exception
    */
   @Test
   public void testDeleteFilesFromOriginal() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         TempDir dir = getTempFileProvider().createTempDir("existing-jar1");
         File existingDir = dir.getRoot();
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         mounts.add(VFS.mountReal(existingDir, existing));
         
         dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
   
         File addedFile = new File(existingDir, "test.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertTrue(existing.getChild("test.txt").exists());
         
         assertTrue(newVirtualFile.getChild("test.txt").exists());
         assertTrue(new File(dirRoot, "test.txt").exists());
         
         addedFile.delete();
         assertFalse(addedFile.exists());
         assertFalse(existing.getChild("test.txt").exists());
         
         // Still in local temp.
         assertTrue(new File(dirRoot, "test.txt").exists());
         
         List<VirtualFile> children = newVirtualFile.getChildren();
         assertEquals(0, children.size());
         
         assertFalse(newVirtualFile.getChild("test.txt").exists());
         assertFalse(new File(dirRoot, "test.txt").exists());
         
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   /**
    * Verify that files deleted from the copy are not re-copied from the original 
    * @throws Exception
    */
   @Test
   public void testDeleteFilesFromCopy() throws Exception
   {
      List<Closeable> mounts = new LinkedList<Closeable>();
      try {
         TempDir dir = getTempFileProvider().createTempDir("existing-jar1");
         File existingDir = dir.getRoot();
         VirtualFile existing = getVirtualFile("/vfs/test/jar1");
         mounts.add(VFS.mountReal(existingDir, existing));
         
         dir = getTempFileProvider().createTempDir("new-jar1");
         File dirRoot = dir.getRoot();
         OneWaySynchronizedCopyFileSystem fs = new OneWaySynchronizedCopyFileSystem(existing, dirRoot);
         
         VirtualFile newVirtualFile = VFS.getChild("/vfs/new-jar");
         mounts.add(VFS.mount(newVirtualFile, fs));
         
         File addedFile = new File(existingDir, "test.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertTrue(existing.getChild("test.txt").exists());
         
         assertTrue(newVirtualFile.getChild("test.txt").exists());
         assertTrue(new File(dirRoot, "test.txt").exists());
         
         newVirtualFile.getChild("test.txt").delete();
         assertFalse(newVirtualFile.getChild("test.txt").exists());
         assertTrue(existing.getChild("test.txt").exists());
         
         addedFile = new File(existingDir, "test2.txt");
         VFSUtils.copyStreamAndClose(new StringInputStream("Some text"), new FileOutputStream(addedFile));
         assertTrue(addedFile.exists());
         assertTrue(existing.getChild("test2.txt").exists());
         
         assertTrue(newVirtualFile.getChild("test2.txt").exists());
         
         File copiedFIle = new File(dirRoot, "test2.txt");
         copiedFIle.delete();
         
         assertFalse(newVirtualFile.getChild("test2.txt").exists());
      } finally {
         VFSUtils.safeClose(mounts);
      }
   }
   
   private void assertCoppied(VirtualFile expected, VirtualFile actual) throws ArrayComparisonFailure, IOException
   {
      assertContentEqual(expected, actual);
      assertEquals(expected.getLastModified(), actual.getLastModified());
   }
   
   private TempFileProvider getTempFileProvider() throws IOException {
      if(tempFileProvider == null)
         tempFileProvider = TempFileProvider.create("Test", Executors.newSingleThreadScheduledExecutor());
      return tempFileProvider;
   }
}
