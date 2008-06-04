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

package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * A delegating VirtualFileHandler that allows for overriding the delegate
 * parent and name. One usecase is a link which roots another VFSContext
 * under a different parent and name.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class DelegatingHandler extends AbstractVirtualFileHandler
{
   /** Serialization */
   private static final long serialVersionUID = 1;
   
   /** The delegate */
   private VirtualFileHandler delegate;

   /**
    * Create a DelegatingHandler without a delegate - which will have to be set afterwards
    *
    * @param context - the context for the parent
    * @param parent - the parent of the delegate in this VFS
    * @param name - the name of the delegate in this VFS
    */
   public DelegatingHandler(VFSContext context, VirtualFileHandler parent, String name)
   {
      this(context, parent, name, null);
   }

   /**
    * Create a DelegatingHandler
    * 
    * @param context - the context for the parent
    * @param parent - the parent of the delegate in this VFS
    * @param name - the name of the delegate in this VFS
    * @param delegate - the handler delegate
    */
   public DelegatingHandler(VFSContext context, VirtualFileHandler parent, String name, VirtualFileHandler delegate)
   {
      super(context, parent, name);
      //if (delegate == null)
      //   throw new IllegalArgumentException("Null delegate");
      this.delegate = delegate;
   }

   public void setDelegate(VirtualFileHandler handler)
   {
      this.delegate = handler;
   }

   public VirtualFileHandler getDelegate()
   {
      return delegate;
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return delegate.getChild(path);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return delegate.getChildren(ignoreErrors);
   }

   public long getLastModified() throws IOException
   {
      return delegate.getLastModified();
   }

   public long getSize() throws IOException
   {
      return delegate.getSize();
   }

   public boolean isLeaf() throws IOException
   {
      return delegate.isLeaf();
   }

   public boolean exists() throws IOException
   {
      return delegate.exists();
   }

   public boolean isHidden() throws IOException
   {
      return delegate.isHidden();
   }

   public boolean isNested() throws IOException
   {
      return delegate.isNested();
   }

   public InputStream openStream() throws IOException
   {
      return delegate.openStream();
   }

   public URI toURI() throws URISyntaxException
   {
      return delegate.toURI();
   }

   public URL toURL() throws URISyntaxException, MalformedURLException
   {
      return delegate.toURL();
   }

   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      delegate.replaceChild(original, replacement);
   }

   public URL toVfsUrl() throws MalformedURLException, URISyntaxException
   {
      return delegate.toVfsUrl();
   }

   public int hashCode()
   {
      if (delegate != null)
         return delegate.hashCode();

      return super.hashCode();
   }

   public boolean equals(Object o)
   {
      if (o == this)
         return true;
      
      if (o instanceof DelegatingHandler)
      {
         DelegatingHandler handler = (DelegatingHandler) o;
         if (delegate != null)
            return delegate.equals(handler.delegate);
         else if (handler.delegate != null)
            return false;   // one is null
         else
            return true;    // both are null
      }

      return false;
   }
}
