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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * TypeInfoOptions.
 *
 * TODO add some security on who can add options
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="les.justin@jboss.com">Ales Justin</a>
 */
public class Options
{
   /** The options */
   private transient Map<String, Object> options;

   /**
    * Get options size.
    *
    * @return the size
    */
   public int size()
   {
      return options == null ? 0 : options.size();
   }

   /**
    * Merge options.
    *
    * @param other the other options
    */
   public void merge(Options other)
   {
      if (other == null)
         throw new IllegalArgumentException("Null other options");

      Map<String, Object> map = other.options;
      if (map == null || map.isEmpty())
         return;

      synchronized (this)
      {
         if (options == null)
            options = new HashMap<String, Object>();

         options.putAll(map);
      }
   }

   /**
    * Get all options that match type.
    *
    * @param exactType the exact type
    * @param <T> the exact type
    * @return matching options
    */
   public <T> Map<String, T> getOptions(Class<T> exactType)
   {
      if (exactType == null)
         throw new IllegalArgumentException("Null exact type");

      Map<String, T> result = new HashMap<String,T>();
      if (options != null && options.isEmpty() == false)
      {
         for (Map.Entry<String, Object> entry : options.entrySet())
         {
            Object value = entry.getValue();
            if (exactType.isInstance(value))
            {
               result.put(entry.getKey(), exactType.cast(value));
            }
         }
      }
      return result;
   }

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
   public void addOption(String name, Object option)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      if (option == null)
         return;

      synchronized (this)
      {
         if (options == null)
            options = new HashMap<String, Object>();
         options.put(name, option);
      }
   }

   /**
    * Add options.
    *
    * @param map the options map
    * @throws IllegalArgumentException for a null map
    */
   public void addOptions(Map<String, ?> map)
   {
      if (map == null)
         throw new IllegalArgumentException("Null map");

      for (Map.Entry<String, ?> entry : map.entrySet())
      {
         addOption(entry.getKey(), entry.getValue());
      }
   }

   /**
    * Remove an option
    *
    * @param name the name
    * @throws IllegalArgumentException for a null name
    */
   public void removeOption(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      synchronized (this)
      {
         if (options == null)
            return;
         options.remove(name);
      }
   }

   /**
    * Get an option from the type
    *
    * @param name the name
    * @return the option
    */
   public Object getOption(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      Object result;
      synchronized (this)
      {
         if (options == null)
            return null;
         result = options.get(name);
      }
      if (result == null)
         return null;

      // Special case if the option is a future object
      if (result instanceof Future)
      {
         try
         {
            return ((Future<?>) result).get();
         }
         catch (RuntimeException e)
         {
            throw e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("Error getting option from future " + result, e);
         }
      }
      return result;
   }

   /**
    * Get the option.
    *
    * @param name the name
    * @param expectedType the expected type
    * @param <T> the expected type
    * @return the option or null if no such matching exists
    */
   public <T> T getOption(String name, Class<T> expectedType)
   {
      if (expectedType == null)
         throw new IllegalArgumentException("Null expectedType");

      Object result = getOption(name);
      if (result == null)
         return null;
      return expectedType.cast(result);      
   }
}
