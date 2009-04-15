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

import junit.framework.Test;

/**
 * Test jar file override.
 *
 * @author ales.justin@jboss.org
 */
public class JarOverrideTestCase extends AbstractVFSTest
{
   public JarOverrideTestCase(String name)
   {
      super(name, false, false);
   }

   protected JarOverrideTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy, false);
   }

   public static Test suite()
   {
      return suite(JarOverrideTestCase.class);
   }

   public void testOverride() throws Exception
   {
      // TODO - cleanup, override code
   }
}