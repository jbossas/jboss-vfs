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
package org.jboss.virtual;

import java.io.IOException;
import java.io.Closeable;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import org.jboss.logging.Logger;
import org.jboss.virtual.spi.RealFileSystem;
import org.jboss.virtual.spi.FileSystem;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;

/**
 * Virtual File System
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="ales.justin@jboss.com">Ales Justin</a> 
 * @version $Revision: 1.1 $
 */
public class VFS
{
   /** The log */
   private static final Logger log = Logger.getLogger(VFS.class);

   private final MountNode rootMountNode = new MountNode(null);
   private final VirtualFile rootVirtualFile;
   private static VFS instance = new VFS();

   static
   {
      init();
   }

   /**
    * Get the "global" instance.
    *
    * @return the VFS instance
    */
   public static VFS getInstance()
   {
      return instance;
   }

   /**
    * Create a new instance.
    */
   public VFS()
   {
      // By default, there's a root mount which points to the "real" FS
      final List<String> emptyList = Collections.<String>emptyList();
      rootMountNode.mount = new Mount(rootMountNode, RealFileSystem.ROOT_INSTANCE, emptyList);
      //noinspection ThisEscapedInObjectConstruction
      rootVirtualFile = new VirtualFile(this, emptyList, "");
   }

   /**
    * Initialize VFS protocol handlers package property. 
    */
   @SuppressWarnings({"deprecation", "unchecked"})
   public static void init()
   {
      String pkgs = System.getProperty("java.protocol.handler.pkgs");
      if (pkgs == null || pkgs.trim().length() == 0)
      {
         pkgs = "org.jboss.virtual.protocol";
         System.setProperty("java.protocol.handler.pkgs", pkgs);
      }
      else if (pkgs.contains("org.jboss.virtual.protocol") == false)
      {
         pkgs = "org.jboss.virtual.protocol|" + pkgs;
         System.setProperty("java.protocol.handler.pkgs", pkgs);
      }
//      org.jboss.virtual.plugins.context.VfsArchiveBrowserFactory factory = org.jboss.virtual.plugins.context.VfsArchiveBrowserFactory.INSTANCE;
//      // keep this until AOP and HEM uses VFS internally instead of the stupid ArchiveBrowser crap.
//      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfsfile", factory);
//      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfszip", factory);
//      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfsjar", factory);
//      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfs", factory);
   }

   /**
    * Mount a filesystem on a mount point in the VFS.  The mount point is any valid file name, existant or non-existant.
    * If a relative path is given, it will be treated as relative to the VFS root.
    *
    * @param mountPoint the mount point
    * @param fileSystem the file system to mount
    * @return a handle which can be used to unmount the filesystem
    * @throws IOException if an I/O error occurs, such as a filesystem already being mounted at the given mount point
    */
   public Closeable mount(String mountPoint, FileSystem fileSystem) throws IOException {
      final List<String> realMountPoint = PathTokenizer.applySpecialPaths(PathTokenizer.getTokens(mountPoint));
      MountNode mountNode = rootMountNode;
      for (String seg : realMountPoint)
      {
         synchronized (mountNode) {
            Map<String, MountNode> childMap = mountNode.nodeMap;
            MountNode subNode;
            if (childMap == null) {
               childMap = new HashMap<String, MountNode>();
               subNode = new MountNode(mountNode);
               childMap.put(seg, subNode);
               mountNode.nodeMap = childMap;
               mountNode = subNode;
            } else {
               subNode = childMap.get(seg);
               if (subNode != null) {
                  mountNode = subNode;
               } else {
                  childMap = new HashMap<String, MountNode>(childMap);
                  subNode = new MountNode(mountNode);
                  childMap.put(seg, subNode);
                  mountNode.nodeMap = childMap;
                  mountNode = subNode;
               }
            }
         }
      }
      synchronized (mountNode) {
         if (mountNode.mount != null) {
            throw new IOException("Filsystem already mounted at mount point \"" + mountPoint + "\"");
         }
         final Mount mount = new Mount(mountNode, fileSystem, realMountPoint);
         mountNode.mount = mount;
         log.debugf("Created mount %s for %s on %s at mount point '%s'", mount, fileSystem, this, mountPoint);
         return mount;
      }
   }

   /**
    * Find a virtual file.
    *
    * @param path the child path
    * @return the child
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the path is null
    */
   public VirtualFile getChild(String path)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");
      final List<String> realPath = PathTokenizer.applySpecialPaths(PathTokenizer.getTokens(path));
      final String realPathString = PathTokenizer.getRemainingPath(realPath, 0);
      return new VirtualFile(this, realPath, realPathString);
   }

   /**
    * Get the root virtual file for this VFS instance.
    *
    * @return the root virtual file
    */
   public VirtualFile getRootVirtualFile()
   {
      return rootVirtualFile;
   }

   /**
    * Get the children
    * 
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildren() throws IOException
   {
      return getRootVirtualFile().getChildren(null);
   }

   /**
    * Get the children
    * 
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildren(VirtualFileFilter filter) throws IOException
   {
      return getRootVirtualFile().getChildren(filter);
   }
   
   /**
    * Get all the children recursively<p>
    * 
    * This always uses {@link VisitorAttributes#RECURSE}
    * 
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      return getRootVirtualFile().getChildrenRecursively(null);
   }
   
   /**
    * Get all the children recursively<p>
    * 
    * This always uses {@link VisitorAttributes#RECURSE}
    * 
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildrenRecursively(VirtualFileFilter filter) throws IOException
   {
      return getRootVirtualFile().getChildrenRecursively(filter);
   }
   
   /**
    * Visit the virtual file system from the root
    * 
    * @param visitor the visitor
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the visitor is null
    */
   public void visit(VirtualFileVisitor visitor) throws IOException
   {
      visitor.visit(rootVirtualFile);
   }

   /**
    * Visit the virtual file system
    * 
    * @param file the file
    * @param visitor the visitor
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the file or visitor is null
    */
   protected void visit(VirtualFile file, VirtualFileVisitor visitor) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      if (file.getVFS() != this)
         throw new IllegalArgumentException("Virtual file from foreign VFS");

      visitor.visit(file);
   }

   /**
    * Get the enclosing mounted FileSystem for the given path.
    *
    * @param pathTokens the path tokens
    * @return the filesystem
    */
   Mount getMount(List<String> pathTokens)
   {
      MountNode mountNode = rootMountNode;
      Mount mount = mountNode.mount;
      for (String pathToken : pathTokens)
      {
         final Map<String, MountNode> childMap = mountNode.nodeMap;
         if (childMap != null) {
            mountNode = childMap.get(pathToken);
            final Mount subMount = mountNode.mount;
            if (subMount != null) {
               mount = subMount;
            }
         } else {
            break;
         }
      }
      return mount;
   }

   /**
    * Get all immediate submounts for a path.
    *
    * @param tokens the path tokens
    * @return the collection of present mount (simple) names
    */
   Iterator<String> getSubmounts(List<String> tokens)
   {
      MountNode mountNode = rootMountNode;
      for (String pathToken : tokens)
      {
         final Map<String, MountNode> childMap = mountNode.nodeMap;
         if (childMap != null) {
            mountNode = childMap.get(pathToken);
         } else {
            return Collections.<String>emptyList().iterator();
         }
      }
      final List<String> list = new ArrayList<String>();
      for (Map.Entry<String, MountNode> entry : mountNode.nodeMap.entrySet())
      {
         final MountNode subNode = entry.getValue();
         if (subNode.mount != null) {
            list.add(entry.getKey());
         }
      }
      return list.iterator();
   }

   /**
    * The mount representation.  This instance represents a binding between a position in the virtual filesystem and
    * the backing filesystem implementation; the same {@code FileSystem} may be mounted in more than one place, however
    * only one {@code FileSystem} may be bound to a specific path at a time.
    */
   final class Mount implements Closeable {
      private final MountNode mountNode;
      private final FileSystem fileSystem;
      private final List<String> realMountPoint;

      private Mount(MountNode mountNode, FileSystem fileSystem, List<String> realMountPoint)
      {
         this.mountNode = mountNode;
         this.fileSystem = fileSystem;
         this.realMountPoint = realMountPoint;
      }

      public void close() throws IOException
      {
         unmountFrom(rootMountNode, realMountPoint.iterator());
      }

      private boolean unmountFrom(MountNode node, Iterator<String> iter)
      {
         synchronized (node) {
            final Map<String, MountNode> nodeMap = node.nodeMap;
            if (iter.hasNext()) {
               if (nodeMap != null) {
                  final String key = iter.next();
                  final MountNode nextNode = nodeMap.get(key);
                  if (nextNode == null) {
                     return nodeMap.isEmpty();
                  }
                  final boolean emptySubNode = unmountFrom(nextNode, iter);
                  if (emptySubNode) {
                     final boolean otherChildren = nodeMap.size() > 1;
                     // subnode is dead; remove it from our map
                     if (otherChildren) {
                        // there's other children; not dead yet
                        final HashMap<String, MountNode> newMap = new HashMap<String, MountNode>(nodeMap);
                        newMap.remove(key);
                        node.nodeMap = newMap;
                        return false;
                     } else {
                        // no other children; dead if there's no mount here
                        node.nodeMap = null;
                        return node.mount == null;
                     }
                  }
                  // subnode isn't empty; not dead
                  return false;
               } else {
                  // dead node if there's no mount here
                  return node.mount == null;
               }
            } else {
               if (node.mount == this) {
                  node.mount = null;
                  log.debugf("Unmounted %s for %s on %s", this, fileSystem, this);
                  // the node is dead if there are no children
                  return nodeMap == null;
               } else {
                  // Node must be already unmounted; do cleanup work anyway.
                  return node.mount == null && nodeMap == null;
               }
            }
         }
      }

      FileSystem getFileSystem()
      {
         return fileSystem;
      }

      List<String> getRealMountPoint()
      {
         return realMountPoint;
      }
   }

   /**
    * A mount point node.  These nodes form a tree of possible mount points.
    */
   private static final class MountNode {

      private final MountNode parent;
      /**
       * The immutable node map.  Since the map is immutable, changes to this field must be accomplished by replacing
       * the field value with a new map (copy on write).  Modifications to this field are protected by {@code this}.
       */
      private volatile Map<String, MountNode> nodeMap;
      /**
       * The current mount at this point.  Modifications to this field are protected by {@code this}.
       */
      private volatile Mount mount;

      private MountNode(MountNode parent)
      {
         this.parent = parent;
      }
   }
}
