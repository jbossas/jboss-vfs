/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs.spi;

import java.io.Closeable;
import java.io.File;

/**
 * This class represents a handle to a mounted FileSystem.  This can
 * be used to close/cleanup the FileSystem as well as to access the
 * mount source.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 */
public interface MountHandle extends Closeable {
   /**
    * Get the source file used for the mount.
    *
    * @return the source file
    */
   File getMountSource();
}
