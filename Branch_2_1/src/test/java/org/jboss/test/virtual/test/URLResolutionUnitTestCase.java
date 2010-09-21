/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.virtual.test;

import java.net.URL;

import junit.framework.Test;
import org.jboss.test.BaseTestCase;

/**
 * Basic tests of URL resolution
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class URLResolutionUnitTestCase extends BaseTestCase
{
   public URLResolutionUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(URLResolutionUnitTestCase.class);
   }

   /**
    * Test resolution when the URL against which relative paths are resolved
    * is NOT a directory (ends in '/').
    * @throws Exception
    */
   public void testNonDirRelativeURLs()
      throws Exception
   {
      URL root = new URL("file:/root");
      URL rootPeer = new URL(root, "peer");
      URL expected = new URL("file:/peer");
      assertEquals(expected, rootPeer);
   }
   /**
    * Test resolution of a relative path with a leading .. against
    * a NON directory URL.
    * @throws Exception
    */
   public void testNonDirDotDotRelativeURLs()
      throws Exception
   {
      URL root = new URL("file:/root/sub1");
      URL rootPeer = new URL(root, "../peer");
      URL expected = new URL("file:/peer");
      assertEquals(expected, rootPeer);

      root = new URL("file:/root/sub1/subsub1");
      rootPeer = new URL(root, "../peer");
      expected = new URL("file:/root/peer");
      assertEquals(expected, rootPeer);
   }

   /**
    * Test resolution when the URL against which relative paths are resolved
    * is a directory (ends in '/').
    * @throws Exception
    */
   public void testDirRelativeURLs()
      throws Exception
   {
      URL root = new URL("file:/root/");
      URL rootPeer = new URL(root, "peer");
      URL expected = new URL("file:/root/peer");
      assertEquals(expected, rootPeer);
   }

   /**
    * Test resolution of a relative path with a leading .. against
    * a directory URL.
    * @throws Exception
    */
   public void testDirDotDotRelativeURLs()
      throws Exception
   {
      URL root = new URL("file:/root/sub1/");
      URL rootPeer = new URL(root, "../peer");
      URL expected = new URL("file:/root/peer");
      assertEquals(expected, rootPeer);

      root = new URL("file:/root/sub1/subsub1/");
      rootPeer = new URL(root, "../peer");
      expected = new URL("file:/root/sub1/peer");
      assertEquals(expected, rootPeer);
   }
}
