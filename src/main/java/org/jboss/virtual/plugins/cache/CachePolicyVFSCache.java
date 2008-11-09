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
package org.jboss.virtual.plugins.cache;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;

import org.jboss.util.CachePolicy;
import org.jboss.virtual.spi.VFSContext;

/**
 * Cache policy vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class CachePolicyVFSCache extends PathMatchingVFSCache
{
   private CachePolicy policy;
   private Map<Object, Object> properties;

   protected CachePolicyVFSCache()
   {
   }

   protected CachePolicyVFSCache(Map<Object, Object> properties)
   {
      this.properties = properties;
   }

   public Iterable<VFSContext> getCachedContexts()
   {
      // cannot pull all cache entries from policy
      return Collections.emptySet();
   }

   public int size()
   {
      return policy != null ? policy.size() : -1;
   }

   /**
    * Get the policy.
    * Run check before.
    *
    * @return the policy
    */
   protected CachePolicy getPolicy()
   {
      check();
      return policy;
   }

   protected void check()
   {
      if (policy == null)
         throw new IllegalArgumentException("Cache needs to be started first.");
   }

   public void start() throws Exception
   {
      policy = createCachePolicy();
      if (policy == null)
         throw new IllegalArgumentException("Policy is null.");

      policy.create();
      try
      {
         policy.start();
      }
      catch (Exception e)
      {
         try
         {
            policy.destroy();
         }
         catch (Exception ignored)
         {
         }
         throw e;
      }
   }

   public void stop()
   {
      if (policy != null)
      {
         try
         {
            policy.stop();
         }
         catch (Exception ignored)
         {
         }
         try
         {
            policy.destroy();
         }
         catch (Exception ignored)
         {
         }
         finally
         {
            policy = null;
         }
      }
   }

   public void flush()
   {
      if (policy != null)
         policy.flush();
   }

   protected VFSContext getContext(String path)
   {
      return VFSContext.class.cast(policy.get(path));
   }

   protected void putContext(String path, VFSContext context)
   {
      Object result = policy.peek(path);
      if (result == null)
         policy.insert(path, context);
   }

   public void removeContext(String key, VFSContext context)
   {
      policy.remove(key);
   }

   /**
    * Create cache policy.
    *
    * @return the cache policy
    */
   protected abstract CachePolicy createCachePolicy();

   /**
    * Read instance properties.
    *
    * @param key the property key
    * @param defaultValue the default value
    * @param useSystemProperties do we fallback to system properties
    * @return property or default value
    */
   protected Object readInstanceProperties(final String key, final Object defaultValue, final boolean useSystemProperties)
   {
      Object result = null;
      if (properties != null && properties.isEmpty() == false)
      {
         result = properties.get(key);
      }
      if (result == null)
      {
         if (useSystemProperties)
         {
            String stringDefaultValue = defaultValue != null ? defaultValue.toString() : null;
            result = readSystemProperty(key, stringDefaultValue);
         }
         else
            result = defaultValue;
      }
      return result;
   }

   /**
    * Read system property.
    *
    * @param key          the property key
    * @param defaultValue the default value
    * @return system property or default value
    */
   protected static String readSystemProperty(final String key, final String defaultValue)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return System.getProperty(key, defaultValue);
      else
         return AccessController.doPrivileged(new PrivilegedAction<String>()
         {
            public String run()
            {
               return System.getProperty(key, defaultValue);
            }
         });
   }

   /**
    * Parse integer.
    *
    * @param value the string int value
    * @return integer value or null
    */
   protected static Integer parseInteger(String value)
   {
      if (value == null)
         return null;

      return Integer.parseInt(value);
   }

   /**
    * Get integer from value.
    *
    * @param value the value
    * @return integer value or null
    */
   protected static Integer getInteger(Object value)
   {
      if (value == null)
         return null;
      else if (value instanceof Number)
         return Number.class.cast(value).intValue();
      else
         return Integer.parseInt(value.toString());
   }
}