/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs.protocol;

import org.jboss.vfs.VFSUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Abstract base class for VFS URLConection impls.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 * @version $Revision$
 */
public abstract class AbstractURLConnection extends URLConnection {

   private String contentType;

   protected AbstractURLConnection(final URL url) {
      super(url);
   }

   public String getHeaderField(String name) {
      String headerField = null;
      if(name.equals("content-type")) {
         headerField = getContentType();
      } else if(name.equals("content-length")) {
         headerField = String.valueOf(getContentLength());
      } else if(name.equals("last-modified")) {
         long lastModified = getLastModified();
         if (lastModified != 0) {
            // return the last modified date formatted according to RFC 1123
            Date modifiedDate = new Date(lastModified);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            headerField = sdf.format(modifiedDate);
         }
      } else {
         headerField = super.getHeaderField(name);
      }
      return headerField;
   }

   public String getContentType() {
      if(contentType != null)
         return contentType;
      contentType = getFileNameMap().getContentTypeFor(getName());
      if(contentType == null) {
         try {
            InputStream is = getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            contentType = java.net.URLConnection.guessContentTypeFromStream(bis);
            bis.close();
         } catch(IOException e) { /* ignore */ }
      }
      return contentType;
   }

   protected static URI toURI(URL url) throws IOException {
      try {
         return VFSUtils.toURI(url);
      }
      catch(URISyntaxException e) {
         IOException ioe = new IOException();
         ioe.initCause(e);
         throw ioe;
      }
   }

   protected abstract String getName();
}
