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
package org.jboss.virtual.plugins.vfs.helpers;

import java.util.StringTokenizer;
import java.io.IOException;

/**
 * PathTokenizer.
 * 
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class PathTokenizer
{
   /** The reverse path const */
   private static final String CURRENT_PATH = ".";

   /** The reverse path const */
   private static final String REVERSE_PATH = "..";

   /**
    * Utility class
    */
   private PathTokenizer()
   {
   }

   /**
    * Get the remaining path from some tokens
    *
    * @param tokens the tokens
    * @param i the current location
    * @param end the end index
    * @return the remaining path
    * @throws IllegalArgumentException for null tokens or i is out of range
    */
   protected static String getRemainingPath(String[] tokens, int i, int end)
   {
      if (tokens == null)
         throw new IllegalArgumentException("Null tokens");
      if (i < 0 || i >= end)
         throw new IllegalArgumentException("i is not in the range of tokens: 0-" + (end-1));

      if (i == end-1)
         return tokens[end-1];

      StringBuilder buffer = new StringBuilder();
      for (; i < end-1; ++i)
      {
         buffer.append(tokens[i]);
         buffer.append("/");
      }
      buffer.append(tokens[end-1]);
      return buffer.toString();
   }

   /**
    * Get the tokens
    * 
    * @param path the path
    * @return the tokens or null if the path is empty
    * @throws IllegalArgumentException if the path is null, it is empty or it is a relative path
    */
   public static String[] getTokens(String path)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      StringTokenizer tokenizer = new StringTokenizer(path, "/");
      int count = tokenizer.countTokens();
      if (count == 0)
         return null;

      String[] tokens = new String[count];
      int i = 0;
      while (tokenizer.hasMoreTokens())
      {
         String token = tokenizer.nextToken();

         if ("".equals(token))
            throw new IllegalArgumentException("A path element is empty: " + path);

         tokens[i++] = token;
      }
      return tokens;
   }
   
   /**
    * Get the remaining path from some tokens
    * 
    * @param tokens the tokens
    * @param i the current location
    * @return the remaining path
    * @throws IllegalArgumentException for null tokens or i is out of range
    */
   public static String getRemainingPath(String[] tokens, int i)
   {
      if (tokens == null)
         throw new IllegalArgumentException("Null tokens");

      return getRemainingPath(tokens, i, tokens.length);
   }

   /**
    * Apply any . or .. paths in the path param.
    *
    * @param path the path
    * @return simple path, containing no . or .. paths
    * @throws IOException if reverse path goes over the top path
    */
   public static String applySpecialPaths(String path) throws IOException
   {
      String[] tokens = getTokens(path);
      if (tokens == null)
         return null;

      int i = 0;
      for(int j = 0; j < tokens.length; j++)
      {
         String token = tokens[j];

         if (isCurrentToken(token))
            continue;
         else if (isReverseToken(token))
            i--;
         else
            tokens[i++] = token;

         if (i < 0)
            throw new IOException("Using reverse path on top path: " + path);
      }
      return getRemainingPath(tokens, 0, i);
   }

   /**
    * Is current token.
    *
    * @param token the token to check
    * @return true if token matches current path token
    */
   public static boolean isCurrentToken(String token)
   {
      return CURRENT_PATH.equals(token);
   }

   /**
    * Is reverse token.
    *
    * @param token the token to check
    * @return true if token matches reverse path token
    */
   public static boolean isReverseToken(String token)
   {
      return REVERSE_PATH.equals(token);
   }
}
