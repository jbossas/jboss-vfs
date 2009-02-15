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
package org.jboss.virtual.spi;

import java.util.Map;

/**
 * Options interface.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface Options
{
   /**
    * Get options size.
    *
    * @return the size
    */
   int size();

   /**
    * Merge options.
    *
    * @param other the other options
    */
   void merge(Options other);

   /**
    * Get all options that match type.
    *
    * @param exactType the exact type
    * @param <T> the exact type
    * @return matching options
    */
   <T> Map<String, T> getOptions(Class<T> exactType);

   /**
    * Set an option against the type.
    * This is useful for caching information against a type.<p>
    *
    * If you add a future object, subsequent gets will wait for the result<p>
    *
    * WARNING: Be careful about what you put in here. Don't create
    * references across classloaders, if you are not sure add a WeakReference
    * to the information.
    *
    * @param name the name
    * @param option the option, pass null to remove an option
    * @throws IllegalArgumentException for a null name
    */
   void addOption(String name, Object option);

   /**
    * Add options.
    *
    * @param map the options map
    * @throws IllegalArgumentException for a null map
    */
   void addOptions(Map<String, ?> map);

   /**
    * Remove an option
    *
    * @param name the name
    * @throws IllegalArgumentException for a null name
    */
   void removeOption(String name);

   /**
    * Get an option from the type
    *
    * @param name the name
    * @return the option
    */
   Object getOption(String name);

   /**
    * Get option.
    *
    * @param expectedType the expected type.
    * @param <T> the expectedType
    * @return the option
    */
   <T> T getOption(Class<T> expectedType);

   /**
    * Get the option.
    *
    * @param name the name
    * @param expectedType the expected type
    * @param <T> the expected type
    * @return the option or null if no such matching exists
    */
   <T> T getOption(String name, Class<T> expectedType);

   /**
    * Get boolean option.
    *
    * @param name the name
    * @return boolean option value
    */
   boolean getBooleanOption(String name);
}
