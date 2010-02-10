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

package org.jboss.vfs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.jboss.vfs.util.PathTokenizer;

/**
 * Assembly of VirtualFiles that can be mounted into the VFS in a structure
 * that is not required to match a real filesystem structure. 
 *  
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class VirtualFileAssembly implements Closeable {

   private static final Random RANDOM_NUM_GEN = new SecureRandom();

   private final AssemblyNode rootNode = new AssemblyNode("");

   private final List<Closeable> mountHandles = new CopyOnWriteArrayList<Closeable>();

   private final VirtualFile mountRoot = VFS.getChild("assembly-mounts").getChild(getAssemblyId());

   private TempFileProvider tempFileProvider;

   /** 
    * Add a {@link VirtualFile} to the assembly.
    *  
    * @param virtualFile
    */
   public void add(VirtualFile virtualFile) {
      String path = virtualFile.getName();
      AssemblyNode assemblyNode = rootNode.findOrBuild(path);
      assemblyNode.setTarget(virtualFile);
   }

   /** 
    * Add a {@link VirtualFile} to the assembly in a given path.
    *  
    * @param path
    * @param virtualFile
    */
   public void add(String path, VirtualFile virtualFile) {
      AssemblyNode assemblyNode = rootNode.findOrBuild(path);
      assemblyNode.setTarget(virtualFile);
   }

   public void add(final String path, final File root) throws IOException {
      VirtualFile mountPoint = mountRoot.getChild(path);
      Closeable handle = VFS.mountReal(root, mountPoint);
      mountHandles.add(handle);
      add(path, mountPoint);
   }

   public void addZip(final String path, final File zipFile) throws IOException {
      VirtualFile mountPoint = mountRoot.getChild(path);
      Closeable handle = VFS.mountZip(zipFile, mountPoint, getTempFileProvider());
      mountHandles.add(handle);
      add(path, mountPoint);
   }

   /**
    * Get the VirtualFile from the assembly.  This will traverse VirtualFiles in assembly 
    * to find children if needed. 
    * 
    * @param mountPoint
    * @param target
    * @return
    * @throws IOException 
    */
   public VirtualFile getFile(VirtualFile mountPoint, VirtualFile target) {
      final String path = target.getPathNameRelativeTo(mountPoint);
      return rootNode.getFile(new Path(path), mountPoint);
   }
   
   /**
    * Returns a list of all the names of the children in the assembly.  
    * @return
    */
   public List<String> getChildNames(VirtualFile mountPoint, VirtualFile target) {
      List<String> names = new LinkedList<String>();
      AssemblyNode targetNode = null;
      if(mountPoint.equals(target)) {
         targetNode = rootNode;
      } else {
         targetNode = rootNode.find(target.getPathNameRelativeTo(mountPoint));
      }
      if(targetNode != null) {
         for(AssemblyNode childNode : targetNode.children.values()) {
            names.add(childNode.realName);
         }
      }
      return names;
   }
   
   public boolean contains(VirtualFile mountPoint, VirtualFile target) {
      final String path = target.getPathNameRelativeTo(mountPoint);
      return rootNode.find(path) != null;
   }

   /**
    * Close the assembly and nested resources.
    */
   public void close() {
      VFSUtils.safeClose(mountHandles);
   }

   /**
    * 
    * @return
    * @throws IOException
    */
   private TempFileProvider getTempFileProvider() throws IOException {
      if (tempFileProvider == null) {
         tempFileProvider = TempFileProvider.create("temp", Executors.newSingleThreadScheduledExecutor());
      }
      return tempFileProvider;
   }

   private String getAssemblyId() {
      return Long.toHexString(RANDOM_NUM_GEN.nextLong());
   }

   /**
    * Path representation to hold onto the elements of the path.
    */
   private static class Path {
      private final Queue<String> parts;

      private Path(String path) {
         parts = new LinkedList<String>();
         List<String> tokens = PathTokenizer.getTokens(path);
         parts.addAll(tokens);
      }

      private boolean isEndOfPath() {
         return parts.isEmpty();
      }

      private String getCurrent() {
         return parts.poll();
      }
   }

   /**
    * Node located within the assembly. 
    */
   private static class AssemblyNode {
      private final Map<String, AssemblyNode> children = new ConcurrentHashMap<String, AssemblyNode>();
      
      private final String realName;

      private VirtualFile target;

      public AssemblyNode(String realName) {
         this.realName = realName;
      }

      /**
       * Find an AssemblyNode staring with this node and return null if not found.
       * 
       * @param path
       * @return
       */
      public AssemblyNode find(String path) {
         return find(new Path(path), false);
      }

      /**
       * Find an AssemblyNode starting with this node and build the required nodes if not found.
       * 
       * @param path
       * @return
       */
      public AssemblyNode findOrBuild(String path) {
         return find(new Path(path), true);
      }

      /**
       * Find an AssemblyNode starting with this node.
       * 
       * @param path
       * @param createIfMissing
       * @return
       */
      public AssemblyNode find(Path path, boolean createIfMissing) {
         if (path.isEndOfPath()) {
            return this;
         }
         String current = path.getCurrent();
         AssemblyNode childNode = getChild(current);
         if (childNode == null) {
            if (!createIfMissing) {
               return null;
            }
            childNode = new AssemblyNode(current);
            addChild(current, childNode);
         }
         return childNode.find(path, createIfMissing);
      }

      /**
       * Get the VirtualFile for a given path.  Will traverse VirtualFile links if not 
       * found in the assembly. 
       * 
       * @param path
       * @return
       * @throws IOException 
       */
      public VirtualFile getFile(Path path, VirtualFile assemblyMountPoint) {

         if (path.isEndOfPath()) {
            return target;
         }
         String current = path.getCurrent();
         AssemblyNode childNode = getChild(current);
         if (childNode != null) {
            return childNode.getFile(path, assemblyMountPoint);
         }
         if (target != null) {
            VirtualFile currentFile = target != null ? target.getChild(current) : null;
            if (currentFile != null) {
               while (!path.isEndOfPath()) {
                  current = path.getCurrent();
                  currentFile = currentFile.getChild(current);
                  if (currentFile == null) {
                     return null;
                  }
               }
               return currentFile;
            }
         }
         return null;

      }

      private void addChild(String name, AssemblyNode child) {
         children.put(name.toLowerCase(), child);
      }

      private AssemblyNode getChild(String name) {
         return children.get(name.toLowerCase());
      }

      private void setTarget(VirtualFile target) {
         this.target = target;
      }
   }
}
