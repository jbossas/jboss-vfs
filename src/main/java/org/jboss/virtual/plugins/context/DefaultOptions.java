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
package org.jboss.virtual.plugins.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.jboss.virtual.spi.Options;

/**
 * Options impl.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class DefaultOptions implements Options
{
   /** The options */
   private transient Map<String, Object> options;

   public int size()
   {
      return options == null ? 0 : options.size();
   }

   public void merge(Options other)
   {
      if (other == null)
         throw new IllegalArgumentException("Null other options");

      if (other.size() == 0)
         return;

      // get all from other
      Map<String, Object> map = other.getOptions(Object.class);

      synchronized (this)
      {
         if (options == null)
            options = new HashMap<String, Object>();

         options.putAll(map);
      }
   }

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

   public void addOptions(Map<String, ?> map)
   {
      if (map == null)
         throw new IllegalArgumentException("Null map");

      for (Map.Entry<String, ?> entry : map.entrySet())
      {
         addOption(entry.getKey(), entry.getValue());
      }
   }

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

   public <T> T getOption(Class<T> expectedType)
   {
      if (expectedType == null)
         throw new IllegalArgumentException("Null expectedType");

      return getOption(expectedType.getName(), expectedType);
   }

   public <T> T getOption(String name, Class<T> expectedType)
   {
      if (expectedType == null)
         throw new IllegalArgumentException("Null expectedType");

      Object result = getOption(name);
      if (result == null)
         return null;
      return expectedType.cast(result);      
   }

   public boolean getBooleanOption(String name)
   {
      Boolean result = getOption(name, Boolean.class);
      return result != null && result;
   }
}
