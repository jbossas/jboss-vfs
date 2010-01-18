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
 * Abstract MountOwner used to wrap a real object as an owner. 
 * 
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 *
 * @param <T> the type of the actual owner
 */
public abstract class AbstractMountOwner<T> implements MountOwner
{
   private final T owner;
   
   /**
    * Construct with an object.
    * 
    * @param owner the actual owner
    */
   protected AbstractMountOwner(T owner)
   {
      this.owner = owner;
   }
   
   /**
    * Get the owner object
    * 
    * @return the actual owner
    */
   protected T getOwner() {
      return owner;
   }
   
   @SuppressWarnings("unchecked")
   @Override
   public boolean equals(Object other)
   {
      if(!(other instanceof AbstractMountOwner))
         return false;
      return getOwner().equals(AbstractMountOwner.class.cast(other).getOwner());
   }

   @Override
   public int hashCode()
   {
      return getOwner().hashCode();
   }
}
