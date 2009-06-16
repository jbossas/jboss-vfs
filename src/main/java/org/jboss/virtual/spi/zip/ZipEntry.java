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

/**
 * Zip entry abstraction.
 * 
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ZipEntry
{

   /**
    * Get the full name of the entry.
    *
    * @return the full name
    */
   String getName();

   /**
    * Determine whether this entry is a directory.
    *
    * @return {@code true} if the entry is a directory
    */
   boolean isDirectory();

   /**
    * Get the modification time of this entry in milliseconds as per {@link System#currentTimeMillis()}.
    *
    * @return the modification time
    */
   long getTime();

   /**
    * Set the modification time.
    *
    * @param time the modification time
    */
   void setTime(long time);

   /**
    * Get the uncompressed size of the data referred to by this entry object.
    *
    * @return the size
    */
   long getSize();

   /**
    * Set the uncompressed size.
    *
    * @param size the size
    */
   void setSize(long size);

   /**
    * Get the zip file entry comment.  May not be available if this object was acquired from a {@link ZipEntryProvider},
    * since the comment information is present only in the Zip directory, and the {@code ZipEntryProvider} uses only
    * local file headers to gather its information.
    *
    * @return the comment string
    */
   String getComment();

   /**
    * Set the zip file entry comment.
    *
    * @param comment the comment string
    */
   void setComment(String comment);

   /**
    * Get the 32-bit unsigned CRC value.
    *
    * @return the CRC value
    */
   long getCrc();

   /**
    * Set the CRC value.
    *
    * @param crc the CRC value
    */
   void setCrc(long crc);

   /**
    * Get the implementation object.
    *
    * @return the implementation object
    */
   Object unwrap();
}
