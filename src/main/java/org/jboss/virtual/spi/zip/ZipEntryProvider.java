/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.virtual.spi.zip;

import java.io.InputStream;
import java.io.IOException;
import java.io.Closeable;

/**
 * Zip entry provider abstraction.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ZipEntryProvider extends Closeable
{

   /**
    * Get the next entry in the stream.  This method may render the results of {@link #currentStream()} useless;
    * such streams should be closed before invoking this method.
    *
    * @return the next entry in the stream
    * @throws IOException if an I/O error occurs
    */
   ZipEntry getNextEntry() throws IOException;

   /**
    * Get the current stream for this entry iterator.  The stream <b>must</b> be closed or file locking or cleanup
    * issues may ensue.
    *
    * @return the input stream
    * @throws IOException if an I/O error occurs
    */
   InputStream currentStream() throws IOException;

   /**
    * Close the iterator.  This <b>may</b> close any outstanding streams returned by this object; however it may not
    * so callers must be sure to close such streams before calling this method.
    *
    * @throws IOException if an I/O error occurs
    */
   void close() throws IOException;
}