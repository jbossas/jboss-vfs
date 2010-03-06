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

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract URLStreamHandler that can be used as a base for other URLStreamHandlers that
 * require the URL to be local.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 * @version $Revision$
 */
public abstract class AbstractLocalURLStreamHandler extends URLStreamHandler {

    private static final Set<String> locals;

    static {
        Set<String> set = new HashSet<String>();
        set.add(null);
        set.add("");
        set.add("~");
        set.add("localhost");
        locals = set;
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        return openConnection(u);
    }

    @Override
    protected boolean hostsEqual(URL url1, URL url2) {
        return locals.contains(toLower(url1.getHost())) && locals.contains(toLower(url2.getHost())) || super.hostsEqual(url1, url2);
    }

    private static String toLower(String str) {
        return str == null ? null : str.toLowerCase();
    }

    protected void ensureLocal(URL url) throws IOException {
       if (!locals.contains(toLower(url.getHost())))   
          throw new IOException("Remote host access not supported for URLs of type \"" + url.getProtocol() + "\"");
    }

}
