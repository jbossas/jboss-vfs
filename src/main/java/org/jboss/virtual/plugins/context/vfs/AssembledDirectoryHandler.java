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
package org.jboss.virtual.plugins.context.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.AssembledDirectory;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * The assembled directory handler.
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
@Assembled
public class AssembledDirectoryHandler extends AbstractVirtualFileHandler implements StructuredVirtualFileHandler
{
   private long lastModified = System.currentTimeMillis();
   private List<VirtualFileHandler> children = new ArrayList<VirtualFileHandler>();
   private Map<String, VirtualFileHandler> childrenMap = new HashMap<String, VirtualFileHandler>();

   public AssembledDirectoryHandler(VFSContext context, AssembledDirectoryHandler parent, String name) throws IOException
   {
      super(context, parent, name);
      String path = getPathName();
      if (path.endsWith("/") == false)
         path += "/";
      setVfsUrl(new URL("vfs", context.getName(), -1, path, new AssembledUrlStreamHandler(context)));
   }

   public VirtualFileHandler addChild(VirtualFileHandler handler)
   {
      if (handler.getClass().isAnnotationPresent(Assembled.class) == false)
      {
         try
         {
            handler = new AssembledFileHandler(getVFSContext(), this, handler.getName(), handler);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      children.add(handler);
      childrenMap.put(handler.getName(), handler);
      lastModified = System.currentTimeMillis();
      return handler;
   }

   public VirtualFileHandler findChild(String name)
   {
      return childrenMap.get(name);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }

   public URI toURI() throws URISyntaxException
   {
      return getVfsUrl().toURI();
   }

   public long getLastModified() throws IOException
   {
      return lastModified;
   }

   public long getSize() throws IOException
   {
      return 0;
   }

   public boolean exists() throws IOException
   {
      return true;
   }

   public boolean isLeaf() throws IOException
   {
      return false;
   }

   public boolean isHidden() throws IOException
   {
      return false;
   }

   public boolean isNested() throws IOException
   {
      return false;
   }

   public InputStream openStream() throws IOException
   {
      throw new RuntimeException("Cannot open stream");
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return children;
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      return childrenMap.get(name);
   }

   @Override
   public VirtualFile getVirtualFile()
   {
      checkClosed();
      increment();
      return new AssembledDirectory(this);
   }

   @Override
   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return getVfsUrl();
   }

   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      children.remove(original);
      children.add(replacement);
      childrenMap.put(original.getName(), replacement);
   }
}
