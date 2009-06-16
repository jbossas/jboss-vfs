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
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * URLHandler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractURLHandler extends AbstractVirtualFileHandler
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The url */
   private final URL url;

   /**
    * Create a newURLHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param url the url
    * @param name the name
    * @throws IllegalArgumentException for a null context, vfsPath or url
    */
   public AbstractURLHandler(VFSContext context, VirtualFileHandler parent, URL url, String name)
   {
      super(context, parent, name);
      if (url == null)
         throw new IllegalArgumentException("Null url");
      this.url = url;
      initCacheLastModified();
   }

   /**
    * Open connection.
    *
    * @return  url's connection
    * @throws IOException for any error
    */
   protected URLConnection openConnection() throws IOException
   {
      return openConnection(url);
   }

   /**
    * Open connection.
    * Set use cacheable to false,
    * due to locking on Windows.
    *
    * @param url the url to open
    * @return  url's connection
    * @throws IOException for any error
    */
   protected static URLConnection openConnection(URL url) throws IOException
   {
      URLConnection conn = url.openConnection();
      conn.setUseCaches(false);
      return conn;
   }

   protected void initCacheLastModified()
   {
      try
      {
         URLConnection c = openConnection();
         try
         {
            this.cachedLastModified = c.getLastModified();
         }
         finally
         {
            closeConnection(c);
         }
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   private void closeConnection(URLConnection c)
   {
      try
      {
         if (c instanceof JarURLConnection == false)
            VFSUtils.safeClose(c.getInputStream());
      }
      catch (Exception ex)
      {
         if (log.isDebugEnabled())
            log.debug("IGNORING: Exception while closing connection", ex);
      }
   }

   /**
    * Get the url
    * 
    * @return the url
    */
   public URL getURL() 
   {
      return url;
   }

   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return getURL();
   }

   public long getLastModified() throws IOException
   {
      checkClosed();
      URLConnection c = openConnection();
      try
      {
         return c.getLastModified();
      }
      finally
      {
         closeConnection(c);
      }
   }

   public long getSize() throws IOException
   {
      checkClosed();
      URLConnection c = openConnection();
      try
      {
         return c.getContentLength();
      }
      finally
      {
         closeConnection(c);
      }
   }

   /**
    * Basis existence on URLConnection.getLastModified() != 0. This may
    * not be true for all url connections.
    * 
    * @see URLConnection#getLastModified()
    * @see org.jboss.test.virtual.test.URLExistsUnitTestCase
    */
   public boolean exists() throws IOException
   {
      URLConnection c = openConnection();
      try
      {
         return c.getLastModified() != 0;
      }
      finally
      {
         closeConnection(c);
      }
   }

   public boolean isHidden() throws IOException
   {
      checkClosed();
      return false;
   }

   public InputStream openStream() throws IOException
   {
      checkClosed();
      URLConnection conn = openConnection();
      return conn.getInputStream();
   }

   public URI toURI() throws URISyntaxException
   {
      return VFSUtils.toURI(url);
   }
}
