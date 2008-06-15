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

import java.io.InputStream;
import java.io.IOException;

/**
 * SizeLimitedInputStream
 *
 * Signals EOF when the specified number of bytes
 * have been read from the underlying stream
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class SizeLimitedInputStream extends InputStream
{
   /** Underlying input stream */
	private InputStream in;

   /** Number of bytes to read before size limit is reached */
   private long togo;

   /**
    * SizeLimitedInputStream constructor
    *
    * @param ins underlying input stream
    * @param size number of bytes after which EOF will occur
    */
   public SizeLimitedInputStream(InputStream ins, long size)
   {
		this.in = ins;
		this.togo = size;
	}

   /**
    * Read one byte
    *
    * @return -1 if stream has reached its size limit, otherwise whatever the underlying input stream returns
    * @throws IOException for any error
    */
   public int read() throws IOException
   {
		int b = -1;
		if (togo > 0)
      {
			b = in.read();
			if (b != -1)
            togo--;
		}
		return b;
	}

   /**
    * Read a buffer of bytes
    *
    * @param buf read buffer
    * @return -1 if stream has reached its size limit, otherwise whatever the underlying input stream returns
    * @throws IOException for any error
    */
   public int read(byte [] buf) throws IOException
   {
		return read(buf, 0, buf.length);
	}

   /**
    * Read a buffer of bytes
    *
    * @param buf read buffer
    * @param offs offset within buffer to be a start point
    * @param len number of bytes to read
    * @return -1 if stream has reached its size limit, otherwise whatever the underlying input stream returns
    * @throws IOException for any error
    */
   public int read(byte [] buf, int offs, int len) throws IOException
   {
		int rc = -1;

      if (togo > 0)
      {
         int ltogo = (int)togo;
         rc = ltogo < len ? ltogo : len;
			rc = in.read(buf, offs, rc);
			if (rc != -1)
            togo -= rc;
		}

		return rc;
	}

   /**
    * Close the underlying input stream
    *
    * @throws IOException for any error
    */
   public void close() throws IOException
   {
		in.close();
	}
}