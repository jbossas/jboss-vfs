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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

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

   static class RegistryEntry
   {
      private final ConcurrentMap<String, RegistryEntry> children = new ConcurrentHashMap<String, RegistryEntry>();

      private final Set<RegistryEntry> inboundReferences = new HashSet<RegistryEntry>();

      private final Set<RegistryEntry> outboundReferences = new HashSet<RegistryEntry>();

      private Closeable handle;

      Collection<RegistryEntry> getChildren()
      {
         return Collections.unmodifiableCollection(children.values());
      }

      void mount(RegistryEntry owner, VirtualFile target, MountType mountType) throws IOException
      {
         if (!isMounted() && target.isFile())
         {
            if (MountType.ZIP.equals(mountType))
               handle = VFS.mountZip(target, target, getTempFileProvider(target.getName()));
            else
               handle = VFS.mountZipExpanded(target, target, getTempFileProvider(target.getName()));
         }
         if (owner.equals(this) == false)
         {
            inboundReferences.add(owner);
            owner.outboundReferences.add(this);
         }
      }

      void removeInboundReference(RegistryEntry owner)
      {
         inboundReferences.remove(owner);
         if (inboundReferences.isEmpty())
         {
            cleanup();
         }
      }

      void cleanup()
      {
         VFSUtils.safeClose(handle);
         handle = null;

         Collection<RegistryEntry> entries = getEntriesRecursive();
         for (RegistryEntry entry : entries)
         {
            entry.cleanup();
         }
         for (RegistryEntry entry : outboundReferences)
         {
            entry.removeInboundReference(this);
         }
      }

      boolean isMounted()
      {
         return handle != null;
      }

      RegistryEntry find(VirtualFile file)
      {
         return find(PathTokenizer.getTokens(file.getPathName()));
      }

      RegistryEntry find(List<String> path)
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

      Collection<RegistryEntry> getEntriesRecursive()
      {
         List<RegistryEntry> allHandles = new LinkedList<RegistryEntry>();
         collectEntries(this, allHandles);
         return allHandles;
      }

      void collectEntries(RegistryEntry registryEntry, List<RegistryEntry> entries)
      {
         for (RegistryEntry childEntry : registryEntry.getChildren())
         {
            collectEntries(childEntry, entries);
            entries.add(childEntry);
         }

      }

   }
}
