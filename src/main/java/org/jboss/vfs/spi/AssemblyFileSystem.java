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
package org.jboss.vfs.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;

/**
 * FileSystem used to mount an Assembly into the VFS.
 *  
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class AssemblyFileSystem implements FileSystem {

   private final VirtualFileAssembly assembly;

   public AssemblyFileSystem(VirtualFileAssembly assembly) {
      this.assembly = assembly;
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#getFile(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
      return getVirtualFileAndRun(mountPoint, target, new VirtualFileTask<File>() {
         public File with(VirtualFile file) throws IOException {
            return file.getPhysicalFile();
         }

         public File without() {
            return null;
         }
      });
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#delete(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public boolean delete(VirtualFile mountPoint, VirtualFile target) {
      VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
      if (assemblyFile == null) {
         return false;
      }
      return assemblyFile.delete();
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#exists(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public boolean exists(VirtualFile mountPoint, VirtualFile target) {
      VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
      if (assemblyFile == null) {
         return false;
      }
      return assemblyFile.exists();
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#getDirectoryEntries(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
      VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
      if (assemblyFile == null) {
         return Collections.<String> emptyList();
      }
      List<String> directoryEntries = new LinkedList<String>();
      for (VirtualFile child : assemblyFile.getChildren()) {
         directoryEntries.add(child.getName());
      }
      return directoryEntries;
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#getLastModified(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public long getLastModified(VirtualFile mountPoint, VirtualFile target) throws IOException {
      return getVirtualFileAndRun(mountPoint, target, new VirtualFileTask<Long>() {
         public Long with(VirtualFile file) throws IOException {
            return file.getLastModified();
         }

         public Long without() {
            return -1L;
         }
      });
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#getSize(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public long getSize(VirtualFile mountPoint, VirtualFile target) throws IOException {
      return getVirtualFileAndRun(mountPoint, target, new VirtualFileTask<Long>() {
         public Long with(VirtualFile file) throws IOException {
            return file.getSize();
         }

         public Long without() {
            return 0L;
         }
      });
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#isDirectory(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
      VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
      if (assemblyFile == null) {
         return false;
      }
      return assemblyFile.isDirectory();
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#isReadOnly()
    */
   public boolean isReadOnly() {
      return false;
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#openInputStream(org.jboss.vfs.VirtualFile, org.jboss.vfs.VirtualFile)
    */
   public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
      return getVirtualFileAndRun(mountPoint, target, new VirtualFileTask<InputStream>() {
         public InputStream with(VirtualFile file) throws IOException {
            return file.openStream();
         }

         public InputStream without() {
            return null;
         }
      });
   }

   /*
    * {@inheritDoc}
    * @see org.jboss.vfs.spi.FileSystem#close()
    */
   public void close() throws IOException {
      assembly.close();
   }

   /**
    * Get the file for the mount/target combination and run the FileTask if the File is found otherwise return the 
    * result of FileStask.getNullReturn.
    * 
    * @param <T>
    * @param mountPoint
    * @param target
    * @param task
    * @return
    */
   private <T> T getVirtualFileAndRun(VirtualFile mountPoint, VirtualFile target, VirtualFileTask<T> task)
         throws IOException {
      VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
      if (assemblyFile != null) {
         return task.with(assemblyFile);
      }
      return task.without();
   }

   /**
    * Task that can be run with a File.
    */
   private static interface VirtualFileTask<T> {
      /** 
       * Method executed it the File is found.
       * 
       * @param file
       * @return
       */
      T with(VirtualFile file) throws IOException;

      /**
       * Method executed if the File is not found.
       * @return
       */
      T without();
   }

}
