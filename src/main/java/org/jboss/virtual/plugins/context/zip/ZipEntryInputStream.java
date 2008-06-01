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

/**
 * ZipEntryInputStream is part of ZipFileWrapper implementation.
 *
 * It wraps the stream retrieved from ZipFile.getInputStream(entry)
 * and releases the underlying ZipFileWrapper when detecting end of use.
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */

public class ZipEntryInputStream extends InputStream
{
   private InputStream delegate;

   private ZipFileWrapper zipWrapper;

   private boolean closed;

   ZipEntryInputStream(ZipFileWrapper zipWrapper, InputStream is) throws IOException
   {
      if (is == null)
         throw new IllegalArgumentException("Input stream is null");
      
      this.zipWrapper = zipWrapper;
      delegate = is;
   }

   private void streamClosed(boolean doClose)
   {
      if (closed == false && doClose)
      {
         closed = true;
         zipWrapper.release();
      }
   }

   public int read() throws IOException
   {
      int rc = -1;
      try
      {
         rc = delegate.read();
         return rc;
      }
      finally
      {
         streamClosed(rc < 0);
      }
   }

   public int read(byte buf[]) throws IOException
   {
      int rc = -1;
      try
      {
         rc = delegate.read(buf);
         return rc;
      }
      finally
      {
         streamClosed(rc < 0);
      }
   }

   public int read(byte buf[], int off, int len) throws IOException
   {
      int rc = -1;
      try
      {
         rc = delegate.read(buf, off, len);
         return rc;
      }
      finally
      {
         streamClosed(rc < 0);
      }
   }

   public synchronized void reset() throws IOException
   {
      boolean ok = false;
      try
      {
         delegate.reset();
         ok = true;
      }
      finally
      {
         streamClosed(ok == false);
      }
   }

   public synchronized void mark(int readlimit)
   {
      boolean ok = false;
      try
      {
         delegate.mark(readlimit);
         ok = true;
      }
      finally
      {
         streamClosed(ok == false);
      }
   }

   public int available() throws IOException
   {
      boolean ok = false;
      try
      {
         int ret = delegate.available();
         ok = true;
         return ret;
      }
      finally
      {
         streamClosed(ok == false);
      }
   }

   public long skip(long n) throws IOException
   {
      boolean ok = false;
      try
      {
         long ret = delegate.skip(n);
         ok = true;
         return ret;
      }
      finally
      {
         streamClosed(ok == false);
      }
   }

   public void close() throws IOException
   {
      streamClosed(true);
      super.close();
   }

   protected void finalize()
   {
      try
      {
         close();
      }
      catch(IOException ignored)
      {
      }
   }

   boolean isClosed()
   {
      return closed;
   }

}
