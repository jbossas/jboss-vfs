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
package org.jboss.vfs.util.automount;

import java.io.Closeable;
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

import org.jboss.logging.Logger;
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
   
   /* Map of owners and their references */
   private static final ConcurrentMap<MountOwner, Set<RegistryEntry>> ownerReferences = new ConcurrentHashMap<MountOwner, Set<RegistryEntry>>();

   /* Provider of temp files/directories*/
   private static TempFileProvider tempFileProvider;
   
   private static final Logger log = Logger.getLogger("org.jboss.vfs.util.automount");
   
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
    * @param mountOptions optional configuration to use for mounting
    * @throws IOException when the target can not be mounted.
    */
   public static void mount(VirtualFile target, MountOption... mountOptions) throws IOException
   {
      mount(new VirtualFileOwner(target), target, mountOptions);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
    * 
    * @param owner Object that owns the reference to the mount
    * @param target VirtualFile to mount
    * @param mountOptions optional configuration to use for mounting
    * @throws IOException when the target can not be mounted.
    */
   public static void mount(Object owner, VirtualFile target, MountOption... mountOptions) throws IOException
   {
      mount(new SimpleMountOwner(owner), target, mountOptions);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
    * 
    * @param owner VirtualFile that owns the reference to the mount
    * @param target VirtualFile to mount
    * @param mountOptions optional configuration to use for mounting
    * @throws IOException when the target can not be mounted.
    */
   public static void mount(VirtualFile owner, VirtualFile target, MountOption... mountOptions) throws IOException
   {
      mount(new VirtualFileOwner(owner), target, mountOptions);
   }

   /**
    * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
    * 
    * @param owner MountOwner that owns the reference to the mount
    * @param target VirtualFile to mount
    * @param mountOptions optional configuration to use for mounting
    * @throws IOException when the target can not be mounted
    */
   public static void mount(MountOwner owner, VirtualFile target, MountOption... mountOptions) throws IOException
   {
      final RegistryEntry targetEntry = getEntry(target);
      targetEntry.mount(target, getMountConfig(mountOptions));
      targetEntry.inboundReferences.add(owner);
      ownerReferences.putIfAbsent(owner, new HashSet<RegistryEntry>());
      ownerReferences.get(owner).add(targetEntry);
   }
   
   /**
    * Creates a MountConfig and applies the provided mount options
    * 
    * @param mountOptions options to use for mounting
    * @return a MountConfig
    */
   private static MountConfig getMountConfig(MountOption[] mountOptions) {
      final MountConfig config = new MountConfig();
      for(MountOption option : mountOptions) {
         option.apply(config);
      }
      return config;
   }

   /**
    * Cleanup all references from the owner.  Cleanup any mounted entries that become un-referenced in the process.
    * 
    * @param owner {@link Object} to cleanup references for
    */
   public static void cleanup(Object owner)
   {
      cleanup(new SimpleMountOwner(owner));
   }
   
   /**
    * Cleanup all references from the owner.  Cleanup any mounted entries that become un-referenced in the process.
    * 
    * @param owner {@link Object} to cleanup references for
    */
   public static void cleanup(VirtualFile owner)
   {
      cleanup(new VirtualFileOwner(owner));
   }
   
   /**
    * Cleanup all references from the {@link MountOwner}.  Cleanup any mounted entries that become un-referenced in the process.
    * 
    * @param owner {@link MountOwner} to cleanup references for
    */
   public static void cleanup(MountOwner owner)
   {
      final Set<RegistryEntry> references = ownerReferences.remove(owner);
      if(references != null) 
      {
         for (RegistryEntry entry : references)
         {
            entry.removeInboundReference(owner);
         }
      }
      owner.onCleanup();
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
   static RegistryEntry getEntry(VirtualFile virtualFile)
   {
      if (virtualFile == null)
      {
         throw new IllegalArgumentException("A valid VirtualFile is required.");
      }
      return rootEntry.find(virtualFile);
   }

   private static TempFileProvider getTempFileProvider() throws IOException
   {
      if(tempFileProvider == null)
         tempFileProvider = TempFileProvider.create("automount", Executors.newScheduledThreadPool(2));
      return tempFileProvider;
   }
   
   static class RegistryEntry
   {
      private final ConcurrentMap<String, RegistryEntry> children = new ConcurrentHashMap<String, RegistryEntry>();

      private final Set<MountOwner> inboundReferences = new HashSet<MountOwner>();

      private final List<Closeable> handles = new LinkedList<Closeable>();
      
      private final AtomicBoolean mounted = new AtomicBoolean();
      
      private void mount(VirtualFile target, MountConfig mountConfig) throws IOException
      {
         if (mounted.compareAndSet(false, true))
         {
            if(target.isFile())
            {
               log.debugf("Automounting: %s with options %s", target, mountConfig);
               
               final TempFileProvider provider = getTempFileProvider();
               if(mountConfig.mountExpanded()) 
               {
                  if(mountConfig.copyTarget())
                     handles.add(VFS.mountZipExpanded(target, target, provider));
                  else 
                     handles.add(VFS.mountZipExpanded(target.getPhysicalFile(), target, provider));
               }
               else
               {
                  if(mountConfig.copyTarget())
                     handles.add(VFS.mountZip(target, target, provider));
                  else
                     handles.add(VFS.mountZip(target.getPhysicalFile(), target, provider));
               }
             }
         }
      }
      
      private void removeInboundReference(MountOwner owner)
      {
         inboundReferences.remove(owner);
         if (inboundReferences.isEmpty())
         {
            cleanup();
         }
      }

      void cleanup()
      {
         if(mounted.compareAndSet(true, false)) 
         {
            VFSUtils.safeClose(handles);
            handles.clear();
   
            final Collection<RegistryEntry> entries = getEntriesRecursive();
            for (RegistryEntry entry : entries)
            {
               entry.cleanup();
            }
         }
      }

      private boolean isMounted()
      {
         return mounted.get();
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
         final String current = path.remove(0);
         children.putIfAbsent(current, new RegistryEntry());
         final RegistryEntry childEntry = children.get(current);
         return childEntry.find(path);
      }

      private Collection<RegistryEntry> getEntriesRecursive()
      {
         final List<RegistryEntry> allHandles = new LinkedList<RegistryEntry>();
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
   }
}
