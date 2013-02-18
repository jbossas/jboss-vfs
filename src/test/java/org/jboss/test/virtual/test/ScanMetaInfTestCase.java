/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.test.virtual.test;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.jboss.test.virtual.test.AbstractVFSTest.registerFactories;
import static org.jboss.test.virtual.test.AbstractVFSTest.unregisterFactories;
import static org.junit.Assert.assertEquals;

/**
 * 00678802 - VFSClasspath patch in facelets goes looking directory info META-INF directories of jar files.
 * The jar files might not contain an actual META-INF directory entry, resulting in an exception.
 * Now it returns a ghost directory entry instead.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ScanMetaInfTestCase
{
   @AfterClass
   public static void afterClass()
   {
      unregisterFactories();
   }

   @BeforeClass
   public static void beforeClass()
   {
      registerFactories();
   }

   @Test
   public void testScanMetaInf() throws Exception
   {
      final URL jarURL = ScanMetaInfTestCase.class.getResource("/vfs/test/jar1-filesonly.jar");
      final URL metaInfURL = new URL("vfszip", null, jarURL.getPath() + "/META-INF/");
      final VirtualFile metaInfRoot = VFS.getRoot(metaInfURL);
      final VirtualFile mfFile = metaInfRoot.getChild("MANIFEST.MF");
      InputStream is = mfFile.openStream();
      final Manifest mf = new Manifest(is);
      mfFile.close();
      final String title = mf.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
      assertEquals(Attributes.Name.SPECIFICATION_TITLE.toString(), "jar1-filesonly", title);
   }
}
