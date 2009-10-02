/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.virtual.spi;

import java.io.File;

import org.jboss.virtual.VirtualFile;

/**
 * The temp store
 *
 * @author ales.justin@jboss.org
 */
public interface TempStore
{
   /**
    * Create temp folder into which contents of this file will be temp copied.
    * This folder should be unique as the file name will remain the same.
    *
    * This method can return null, which means we fall back to default temp dir mechanism.
    *
    * @param file the file to copy
    * @return new temp folder or null if we fall back to default temp dir
    */
   File createTempFolder(VirtualFile file);

   /**
    * Create temp folder for nested zip file.
    * The folder doesn't have to be unique as the nested file's name will be joined with GUID.
    *
    * This method can return null, which means we fall back to default temp dir mechanism.
    *
    * @param outerName outer file's name
    * @param innerName nested file's name
    * @return temp folder or null if we fall back to default temp dir
    */
   File createTempFolder(String outerName, String innerName);

   /**
    * Clear newly created temp folders.
    * This will be invoked once VFSContext is no longer used.
    *
    * But it's probably better to mark newly created temp dir
    * to be deleted on JVM exit (File::deleteOnExit).  
    */
   void clear();
}