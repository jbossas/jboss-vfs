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
package org.jboss.virtual.plugins.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.util.id.GUID;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Copy mechanism to be used in VFSUtils.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractCopyMechanism implements CopyMechanism
{
   private static Logger log = Logger.getLogger(CopyMechanism.class);

   private static File tempDir;

   private static class GetTempDir implements PrivilegedAction<File>
   {
      public File run()
      {
         String tempDirKey = System.getProperty("vfs.temp.dir", "jboss.server.temp.dir");
         return new File(System.getProperty(tempDirKey, System.getProperty("java.io.tmpdir")));
      }
   }

   /**
    * Get temp directory.
    *
    * @return the temp directory
    */
   public synchronized static File getTempDirectory()
   {
      if (tempDir == null)
      {
         tempDir = AccessController.doPrivileged(new GetTempDir());
         log.info("VFS temp dir: " + tempDir);
      }
      return tempDir;
   }

   /**
    * Get mechanism type.
    *
    * @return the type
    */
   protected abstract String getType();

   /**
    * Is handler already modified.
    *
    * @param handler the handler
    * @return true if already modified
    * @throws IOException for any error
    */
   protected abstract boolean isAlreadyModified(VirtualFileHandler handler) throws IOException;

   /**
    * Should we replace old handler with new.
    *
    * @param parent the parent handler
    * @param oldHandler the old handler
    * @param newHandler the new handler
    * @return true if needs replacement
    * @throws IOException for any error
    */
   protected abstract boolean replaceOldHandler(VirtualFileHandler parent, VirtualFileHandler oldHandler, VirtualFileHandler newHandler) throws IOException;

   /**
    * Unwrap the handler from possible delegate handler.
    *
    * @param handler the handler to unwrap
    * @return unwrapped handler
    */
   protected VirtualFileHandler unwrap(VirtualFileHandler handler)
   {
      if (handler instanceof DelegatingHandler)
         handler = ((DelegatingHandler)handler).getDelegate();
      return handler;
   }

   public VirtualFile copy(VirtualFile file, VirtualFileHandler handler) throws IOException, URISyntaxException
   {
      VirtualFileHandler unwrapped = unwrap(handler);
      // check modification on unwrapped
      if (isAlreadyModified(unwrapped))
      {
         if (log.isTraceEnabled())
            log.trace("Should already be " + getType() + ": " + unwrapped);
         return file;
      }

      //create guid dir
      File guidDir = createTempDirectory(getTempDirectory(), GUID.asString());
      // unpack handler
      File copy = copy(guidDir, handler);
      // create new handler
      FileSystemContext fileSystemContext = new FileSystemContext(copy);
      VirtualFileHandler newHandler = fileSystemContext.getRoot();
      VirtualFileHandler parent = handler.getParent();
      if (parent != null && replaceOldHandler(parent, handler, newHandler))
         parent.replaceChild(handler, newHandler);

      return newHandler.getVirtualFile();
   }

   /**
    * Copy handler.
    *
    * @param guidDir the guid directory
    * @param handler the handler to copy
    * @return handler's copy as file
    * @throws IOException for any error
    */
   protected File copy(File guidDir, VirtualFileHandler handler) throws IOException
   {
      File copy = createTempDirectory(guidDir, handler.getName());
      unpack(handler, copy, false);
      return copy;
   }

   /**
    * Create the temp directory.
    *
    * @param parent the parent
    * @param name the dir name
    * @return new directory
    */
   protected static File createTempDirectory(File parent, String name)
   {
      File file = new File(parent, name);
      if (file.mkdir() == false)
         throw new IllegalArgumentException("Cannot create directory: " + file);
      file.deleteOnExit();
      return file;
   }

   /**
    * Unpack the root into file.
    * Repeat this on the root's children.
    *
    * @param root the root
    * @param file the file
    * @param writeRoot do we write root
    * @throws IOException for any error
    */
   protected static void unpack(VirtualFileHandler root, File file, boolean writeRoot) throws IOException
   {
      // should we write the root
      if (writeRoot)
         rewrite(root, file);

      if (root.isLeaf() == false)
      {
         List<VirtualFileHandler> children = root.getChildren(true);
         if (children != null && children.isEmpty() == false)
         {
            for (VirtualFileHandler handler : children)
            {
               File next = new File(file, handler.getName());
               if (handler.isLeaf() == false && next.mkdir() == false)
                  throw new IllegalArgumentException("Problems creating new directory: " + next);
               next.deleteOnExit();

               unpack(handler, next, handler.isLeaf());
            }
         }
      }
   }

   /**
    * Rewrite contents of handler into file.
    *
    * @param handler the handler
    * @param file the file
    * @throws IOException for any error
    */
   protected static void rewrite(VirtualFileHandler handler, File file) throws IOException
   {
      OutputStream out = new FileOutputStream(file);
      InputStream in = handler.openStream();
      try
      {
         byte[] bytes = new byte[1024];
         while (in.available() > 0)
         {
            int length = in.read(bytes);
            if (length > 0)
               out.write(bytes, 0, length);
         }
      }
      finally
      {
         try
         {
            in.close();
         }
         catch (IOException ignored)
         {
         }
         try
         {
            out.close();
         }
         catch (IOException ignored)
         {
         }
      }
   }
}