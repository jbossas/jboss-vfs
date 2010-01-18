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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * {@inheritDoc}
 * <p/>
 * An implementation {@link FileSystem} that represents a temporary copy of an existing {@link VirtualFile} as its root. 
 * The temporary {@link FileSystem} will be synchronized with changes to the original {@link VirtualFile} root and children.   
 * 
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class OneWaySynchronizedCopyFileSystem implements FileSystem
{
   /* The original root to use as a base for synchronization */
   private final VirtualFile originalRoot;

   /* Temporary file system holding local copies */
   private final RealFileSystem temporaryFileSystem;

   /* Map holding information about the state of a giving temporary file location */ 
   private final ConcurrentMap<String, SynchInfo> synchInfoMap = new ConcurrentHashMap<String, SynchInfo>();

   /**
    * Constructs a new {@link OneWaySynchronizedCopyFileSystem}.
    * 
    * @param originalRoot the root file to base the synchronization
    * @param temporaryRoot the file to use as the root of the temporary file system
    */
   public OneWaySynchronizedCopyFileSystem(VirtualFile originalRoot, File temporaryRoot)
   {
      this.originalRoot = originalRoot;
      this.temporaryFileSystem = new RealFileSystem(temporaryRoot);
   }
   
   /** {@inheritDoc} */
   public boolean delete(VirtualFile mountPoint, VirtualFile target)
   {
      getSynchInfo(mountPoint, target).setState(SynchState.DELETED_LOCAL);
      return temporaryFileSystem.delete(mountPoint, target);
   }

   /** {@inheritDoc} */
   public boolean exists(VirtualFile mountPoint, VirtualFile target)
   {
      return getFile(mountPoint, target).exists();
   }

   /** {@inheritDoc} */
   public File getFile(VirtualFile mountPoint, VirtualFile target)
   {
      File file = temporaryFileSystem.getFile(mountPoint, target);
      VirtualFile originalChild = getOriginalTarget(mountPoint, target);
      synch(originalChild, file, getSynchInfo(mountPoint, target));
      return file;
   }

   /**
    * Synchronize the temporary file with the contents of the original.  
    * 
    * @param original the original {@link VirtualFile} to synchronize with the temporary file
    * @param file the temporary file to synchronize with the original 
    * @param info the synchronization info for this file location
    */
   private void synch(VirtualFile original, File file, SynchInfo info)
   {
      if (file.exists()) {

         if (original.exists()) {
            long origModTime = original.getLastModified();
            long copyModTime = file.lastModified();
            if (origModTime > copyModTime) {
               copyOriginal(original, file, info);
            }
         } else {
            if (info.isInState(SynchState.COPIED)) {
               file.delete();
            } else {
               info.setState(SynchState.ADDED_LOCAL);
            }
         }
      } else if (info.isInState(SynchState.DELETED_LOCAL) == false) {
         if (original.exists() && info.isInState(SynchState.UNKNOWN)) {
            if (original.isDirectory()) {
               file.mkdirs();
               info.setState(SynchState.COPIED);
            } else {
               copyOriginal(original, file, info);
            }
         }
      }

   }
   
   /**
    * Copy the original content over to the temporary location
    * 
    * @param original the original {@link VirtualFile} to copy contents from
    * @param file the temporary file to copy contents to
    * @param info the synchronization info for the file locaiton
    */
   private void copyOriginal(VirtualFile original, File file, SynchInfo info)
   {
      try {
         VFSUtils.copyStreamAndClose(original.openStream(), new FileOutputStream(file));
         file.setLastModified(original.getLastModified());
         info.setState(SynchState.COPIED);
      }
      catch (IOException e) {
         throw new RuntimeException("Failed to create temporary copy of file: " + original, e);
      }
   }

   /** {@inheritDoc} */
   public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target)
   {
      File file = getFile(mountPoint, target);
      if (file.isFile()) {
         return null;
      }

      List<String> directoryEntries = new LinkedList<String>();

      VirtualFile originalTarget = getOriginalTarget(mountPoint, target);
      for (VirtualFile origChild : originalTarget.getChildren()) {
         directoryEntries.add(origChild.getName());
      }

      String[] tempedFiles = file.list();
      for (String name : tempedFiles) {
         SynchInfo info = getSynchInfo(mountPoint, target, name);
         if (directoryEntries.contains(name) == false && info.isInState(SynchState.COPIED) == false) {
            directoryEntries.add(name);
         }
      }
      return directoryEntries;
   }

   /** {@inheritDoc} */
   public long getLastModified(VirtualFile mountPoint, VirtualFile target)
   {
      return getFile(mountPoint, target).lastModified();
   }

   /** {@inheritDoc} */
   public long getSize(VirtualFile mountPoint, VirtualFile target)
   {
      return getFile(mountPoint, target).length();
   }

   /** {@inheritDoc} */
   public boolean isDirectory(VirtualFile mountPoint, VirtualFile target)
   {
      return getFile(mountPoint, target).isDirectory();
   }

   /** {@inheritDoc} */
   public boolean isFile(VirtualFile mountPoint, VirtualFile target)
   {
      return getFile(mountPoint, target).isDirectory();
   }

   /** {@inheritDoc} */
   public boolean isReadOnly()
   {
      return false;
   }

   /** {@inheritDoc} */
   public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      return new FileInputStream(getFile(mountPoint, target));
   }

   /** {@inheritDoc} */
   public void close() throws IOException
   {
      temporaryFileSystem.close();
   }

   /** {@inheritDoc} */
   public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target)
   {
      return null;
   }

   /**
    * Get the synchronization info for a target
    * 
    * @param mountPoint the mount for this {@link FileSystem}
    * @param target the target {@link VirtualFile} to get info for
    * @return the synchronization info
    */
   private SynchInfo getSynchInfo(VirtualFile mountPoint, VirtualFile target)
   {
      return getSynchInfo(mountPoint, target, null);
   }
   
   /**
    * Get the synchronization info for the child of a target
    * 
    * @param mountPoint the mount for this {@link FileSystem}
    * @param target the target {@link VirtualFile} to get info for
    * @param targetChild the name of a child of the target
    * @return the synchronization info
    */
   private SynchInfo getSynchInfo(VirtualFile mountPoint, VirtualFile target, String targetChild)
   {
      String path = getRelativePath(mountPoint, target);
      if (targetChild != null) {
         path = path.equals("") ? targetChild : path + "/" + targetChild;
      }
      return getSynchInfo(path);
   }

   /**
    * Get the synchronization info for a raw file path key
    * 
    * @param path the file path
    * @return the synchronization info
    */
   private SynchInfo getSynchInfo(String path)
   {
      synchInfoMap.putIfAbsent(path, new SynchInfo());
      return synchInfoMap.get(path);
   }

   /**
    * Get the relative file path between the mount point and the target
    *  
    * @param mountPoint the mount point for the {@link FileSystem}
    * @param target the target {@link VirtualFile} 
    * @return the relative path between
    */
   private String getRelativePath(VirtualFile mountPoint, VirtualFile target)
   {
      if (mountPoint.equals(target)) {
         return "";
      }
      return target.getPathNameRelativeTo(mountPoint);
   }

   /**
    * Return the original target {@link VirtualFile} that should be used to back this temporary target.  
    * 
    * @param mountPoint the mount point for the {@link FileSystem}
    * @param target the target {@link VirtualFile}
    * @return the {@link VirtualFile} from the original {@link FileSystem}
    */
   private VirtualFile getOriginalTarget(VirtualFile mountPoint, VirtualFile target)
   {
      VirtualFile originalChild = null;
      if (target.equals(mountPoint)) {
         originalChild = originalRoot;
      } else {
         originalChild = originalRoot.getChild(getRelativePath(mountPoint, target));
      }
      return originalChild;
   }
   
   /**
    * An enumeration of synchronizations states.  
    */
   private enum SynchState {
      UNKNOWN, COPIED, DELETED_LOCAL, ADDED_LOCAL
   };

   /**
    * Holder for synchronization states 
    */
   private class SynchInfo
   {
      private SynchState state = SynchState.UNKNOWN;

      private boolean isInState(SynchState synchState)
      {
         return synchState.equals(state);
      }

      public void setState(SynchState state)
      {
         this.state = state;
      }
   }

}
