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
package org.jboss.vfs.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.util.id.GUID;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.PathTokenizer;

/**
 * Utility used to manage mounting Virtual FileSystems.
 * 
 * TODO - Make this thread safe.............
 *  
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class Automounter
{
   /* Root entry in the tree. */
   private static final RegistryEntry rootEntry = new RegistryEntry();

   /* VirutalFile used as a base mount for 'hidden' original copies of mounted files */
   private static final VirtualFile originalsRoot = VFS.getChild("/.vfs/backups");
   
   /* VirutalFile used as a base for all temporary filesytems created */
   private static final VirtualFile tempRoot = VFS.getChild("/.vfs/tmp");
   
   /* Possible mount types */
   private static enum MountType {
      ZIP, EXPANDED
   };

   /**
    * Private constructor
    */
   private Automounter()
   {
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) and set the owner to be the provided target.  (Self owned mount)
    * 
    * @param target VirtualFile to mount
    * @throws IOException when the target can not be mounted.
    */
   public static void mount(VirtualFile target) throws IOException
   {
      mount(target, target);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
    * 
    * @param owner Virtual file that owns the reference to the mount
    * @param target VirtualFile to mount
    * @throws IOException when the target can not be mounted
    */
   public static void mount(VirtualFile owner, VirtualFile target) throws IOException
   {
      RegistryEntry targetEntry = getEntry(target);
      RegistryEntry ownerEntry = getEntry(owner);
      targetEntry.mount(ownerEntry, target, MountType.ZIP);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) as an expanded Zip mount and add an owner entry.  
    * Also creates a back-reference to from the owner to the target. (Self owned mount)
    * 
    * @param owner Virtual file that owns the reference to the mount
    * @param target VirtualFile to mount
    * @throws IOException when the target can not be mounted 
    */
   public static void mountExpanded(VirtualFile target) throws IOException
   {
      mountExpanded(target, target);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) as an expanded Zip mount and add an owner entry.  
    * Also creates a back-reference to from the owner to the target.
    * 
    * @param owner Virtual file that owns the reference to the mount
    * @param target VirtualFile to mount
    * @throws IOException when the target can not be mounted 
    */
   public static void mountExpanded(VirtualFile owner, VirtualFile target) throws IOException
   {
      RegistryEntry targetEntry = getEntry(target);
      RegistryEntry ownerEntry = getEntry(owner);
      targetEntry.mount(ownerEntry, target, MountType.EXPANDED);
   }

   /**
    * Recursively cleanup all mounted handles starting at the provided {@link VirtualFile} location
    * and remove all references to other mounts.
    * 
    * @param owner VirtualFile to cleanup
    */
   public static void cleanup(VirtualFile owner)
   {
      getEntry(owner).cleanup();
   }

   /**
    * Determines whether a target {@link VirtualFile} is mounted.
    * 
    * @param target
    * @return
    */
   public static boolean isMounted(VirtualFile target)
   {
      return getEntry(target).isMounted();
   }
   
   /**
    * Create a backup of the provided root.  Only one backup is allowed for a location.   
    * 
    * @param original the original VirtualFile to backup
    * @return a reference to the backup location
    * @throws IOException if any problems occur during the backup process
    */
   public static VirtualFile backup(VirtualFile original) throws IOException
   {
      RegistryEntry entry = getEntry(original); 
      entry.backup(original);
      return entry.backupFile;
   }
   
   /**
    * Get the backup for the provided target
    * 
    * @param target the location to get the backup for
    * @return the backup
    */
   public static VirtualFile getBackup(VirtualFile target) 
   {
      RegistryEntry entry = getEntry(target);
      return entry.backupFile != null ? entry.backupFile : null;
   }
   
   /**
    * Check to see if a backup exists for the provided location.
    * 
    * @param target the location to check for a backup.
    * @return true if the backup exists.
    */
   public static boolean hasBackup(VirtualFile target) 
   {
      return getEntry(target).backupFile != null;
   }
   
   
   /**
    * Get the entry from the tree creating the entry if not present.
    * 
    * @param virtualFile
    * @return
    */
   private static RegistryEntry getEntry(VirtualFile virtualFile)
   {
      if (virtualFile == null)
      {
         throw new IllegalArgumentException("A valid VirtualFile is required.");
      }
      return rootEntry.find(virtualFile);
   }

   private static TempFileProvider getTempFileProvider(String name) throws IOException
   {
      return TempFileProvider.create(name, Executors.newSingleThreadScheduledExecutor());
   }
   
   private static class RegistryEntry
   {
      private final ConcurrentMap<String, RegistryEntry> children = new ConcurrentHashMap<String, RegistryEntry>();

      private final Set<RegistryEntry> inboundReferences = new HashSet<RegistryEntry>();

      private final Set<RegistryEntry> outboundReferences = new HashSet<RegistryEntry>();

      private final List<Closeable> handles = new LinkedList<Closeable>();
      
      private final AtomicBoolean mounted = new AtomicBoolean();
      
      private VirtualFile backupFile;
      
      private void mount(RegistryEntry owner, VirtualFile target, MountType mountType) throws IOException
      {
         if (mounted.compareAndSet(false, true))
         {
            if(target.isFile())
            {
               final TempFileProvider provider = getTempFileProvider(target.getName());
               // Make sure we can get to the original
               backup(target);
               // Copy archive to temporary location to avoid file locking issues
               File copy = copyArchiveFile(target, provider);
               
               if (MountType.ZIP.equals(mountType))
                  handles.add(VFS.mountZip(copy, target, provider));
               else
                  handles.add(VFS.mountZipExpanded(copy, target, provider));
            }
         }
         if (owner.equals(this) == false)
         {
            inboundReferences.add(owner);
            owner.outboundReferences.add(this);
         }
      }

      
      private File copyArchiveFile(VirtualFile target, final TempFileProvider provider) throws IOException,
            FileNotFoundException
      {
         VirtualFile tempLocation = tempRoot.getChild(GUID.asString());
         handles.add(VFS.mountTemp(tempLocation, provider));
         File copy = tempLocation.getChild(target.getName()).getPhysicalFile();
         VFSUtils.copyStreamAndClose(target.openStream(), new FileOutputStream(copy));
         return copy;
      }

      private void removeInboundReference(RegistryEntry owner)
      {
         inboundReferences.remove(owner);
         if (inboundReferences.isEmpty())
         {
            cleanup();
         }
      }

      private void cleanup()
      {
         VFSUtils.safeClose(handles);
         handles.clear();

         Collection<RegistryEntry> entries = getEntriesRecursive();
         for (RegistryEntry entry : entries)
         {
            entry.cleanup();
         }
         for (RegistryEntry entry : outboundReferences)
         {
            entry.removeInboundReference(this);
         }
         mounted.set(false);
      }

      private boolean isMounted()
      {
         return !handles.isEmpty();
      }

      private RegistryEntry find(VirtualFile file)
      {
         return find(PathTokenizer.getTokens(file.getPathName()));
      }

      private RegistryEntry find(List<String> path)
      {
         if (path.isEmpty())
         {
            return this;
         }
         String current = path.remove(0);
         children.putIfAbsent(current, new RegistryEntry());
         RegistryEntry childEntry = children.get(current);
         return childEntry.find(path);
      }

      private Collection<RegistryEntry> getEntriesRecursive()
      {
         List<RegistryEntry> allHandles = new LinkedList<RegistryEntry>();
         collectEntries(this, allHandles);
         return allHandles;
      }

      private void collectEntries(RegistryEntry registryEntry, List<RegistryEntry> entries)
      {
         for (RegistryEntry childEntry : registryEntry.children.values())
         {
            collectEntries(childEntry, entries);
            entries.add(childEntry);
         }
      }
      
      private void backup(VirtualFile target) throws IOException
      {
         backupFile = originalsRoot.getChild(GUID.asString() + target.getName());
         handles.add(VFS.mountReal(target.getPhysicalFile(), backupFile));
      }
   }
}
