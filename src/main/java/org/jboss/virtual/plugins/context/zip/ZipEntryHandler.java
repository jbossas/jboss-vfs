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
package org.jboss.virtual.plugins.context.zip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Handler representing an individual file (ZipEntry) within ZipEntryContext
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipEntryHandler extends AbstractVirtualFileHandler implements StructuredVirtualFileHandler
{
   /** The url */
   private URL url;

   /** isLeaf */
   private boolean isLeaf;

   /** has it been initialized yet */
   transient private boolean initialized;

   /**
    * Create a new ZipEntryHandler.
    *
    * @param context ZipEntryContext
    * @param parent  parent within the same context
    * @param name    name of this file within context
    * @param isLeaf  true if this file should have a URL not ending with '/', false otherwise
    * @throws IOException for any error
    */
   public ZipEntryHandler(ZipEntryContext context, AbstractVirtualFileHandler parent, String name, boolean isLeaf) throws IOException
   {
      super(context, parent, name);

      this.isLeaf = isLeaf;
      if(parent != null)
      {
         context.addChild(parent, this);
      }
   }

   private synchronized void init()
   {
      if (initialized)
         return;

      StringBuilder vfsUrl = new StringBuilder();
      try
      {
         url = getZipEntryContext().getChildURL(getParent(), getName());
         String currentUrl = url.toString();
         int pos = currentUrl.indexOf(":/");
         vfsUrl.append("vfszip:").append(currentUrl.substring(pos+1));

         if (isLeaf == false && vfsUrl.charAt(vfsUrl.length()-1) != '/')
            vfsUrl.append("/");
         setVfsUrl(new URL(vfsUrl.toString()));
      }
      catch(Exception ex)
      {
         throw new RuntimeException("ASSERTION ERROR - failed to set vfsUrl: " + vfsUrl, ex );
      }
      initialized = true;
   }

   public URI toURI() throws URISyntaxException
   {
      init();
      return VFSUtils.toURI(url);
   }

   public long getLastModified() throws IOException
   {
      checkClosed();
      return getZipEntryContext().getLastModified(this);
   }

   public long getSize() throws IOException
   {
      checkClosed();
      return getZipEntryContext().getSize(this);
   }

   public boolean exists() throws IOException
   {
      checkClosed();
      return getZipEntryContext().exists(this);
   }

   public boolean isLeaf() throws IOException
   {
      checkClosed();
      return getZipEntryContext().isLeaf(this);
   }

   public boolean isHidden() throws IOException
   {
      checkClosed();
      return false;
   }

   public boolean isNested() throws IOException
   {
      return ZipEntryContext.isNested(this);
   }

   public InputStream openStream() throws IOException
   {
      checkClosed();
      return getZipEntryContext().openStream(this);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      return getZipEntryContext().getChildren(this, ignoreErrors);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      checkClosed();
      return structuredFindChild(path);
   }

   public boolean removeChild(String name) throws IOException
   {
      return false;
   }

   public boolean delete(int gracePeriod) throws IOException
   {
      checkClosed();
      boolean deleted = getZipEntryContext().delete(this, gracePeriod);
      if (deleted)
         super.delete(gracePeriod);

      return deleted;
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      return getZipEntryContext().getChild(this, name);
   }

   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      if (original instanceof AbstractVirtualFileHandler == false)
         throw new IllegalArgumentException("Original file handler not found in this context: " + original);

      getZipEntryContext().replaceChild(this, (AbstractVirtualFileHandler) original, replacement);
   }

   private ZipEntryContext getZipEntryContext()
   {
      return ((ZipEntryContext) getLocalVFSContext());
   }

   public URL toVfsUrl()
   {
      init();
      return super.getVfsUrl();
   }

   public URL getRealURL() throws IOException, URISyntaxException
   {
      // see how far the parent contexts go
      // if there is no parent context or it is of type FileSystemContext
      // ZipEntryContext -> jar!/
      // ZipEntryContext / ZipEntryContext -> jar!/jar
      // ZipEntryConteyt / ZipEntryContext / ZipEntryContext ... -> jar!/jar
      VFSContext ctx = getLocalVFSContext();
      VirtualFileHandler peer = getLocalVFSContext().getRootPeer();

      if (peer == null)
         return getZipEntryContext().getRealURL();

      if (peer instanceof AbstractVirtualFileHandler
         && ((AbstractVirtualFileHandler)peer).getLocalVFSContext() instanceof FileSystemContext)
      {
         String lpath = getLocalPathName();
         if (lpath.startsWith("/") == false)
            lpath = "/" + lpath;
         return new URL("jar:file:" + ctx.getRootURI().getPath() + "!" + lpath);
      }

      if (peer instanceof AbstractVirtualFileHandler)
      {
         AbstractVirtualFileHandler aPeer =(AbstractVirtualFileHandler) peer;
         URL realUrl = aPeer.getLocalVFSContext().getRoot().getRealURL();
         String urlStr = realUrl.toExternalForm();
         if (urlStr.endsWith("!/"))
            return new URL(urlStr + aPeer.getLocalPathName());
         else
            return realUrl;
      }
       
      throw new RuntimeException("Operation not supported for handler: " + this);
   }
}
