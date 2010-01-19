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
package org.jboss.vfs.util.automount;

/**
 * Configuration used to control the auto-mount behavior.
 * 
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
class MountConfig
{
   private boolean mountExpanded;

   private boolean copyTarget;

   /**
    * Should the archive be mounted as an expanded zip filesystem.  Defaults to false.
    * 
    * @return true if it should be expanded
    */
   boolean mountExpanded()
   {
      return mountExpanded;
   }
   
   /**
    * Set whether the mount should be an expanded zip filesystem.
    * 
    * @param mountExpanded the boolean value to set it to
    */
   void setMountExpanded(boolean mountExpanded)
   {
      this.mountExpanded = mountExpanded;
   }

   /**
    * Should the archive be copied to a temporary location before being mounted.
    * Defaults to false.
    * 
    * @return true if the archive should be copied before being mounted
    */
   boolean copyTarget()
   {
      return copyTarget;
   }

   /**
    * Set whether the archive should be copied before being mounted.
    * 
    * @param copyTarget the boolean value to set it to
    */
   void setCopyTarget(boolean copyTarget)
   {
      this.copyTarget = copyTarget;
   }

   @Override
   public String toString()
   {
      return new StringBuilder().append("MountConfig[Expanded: ").append(mountExpanded).append(", Copy: ").append(
            copyTarget).append("]").toString();
   }

}
