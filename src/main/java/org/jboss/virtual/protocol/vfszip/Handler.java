package org.jboss.virtual.protocol.vfszip;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.plugins.context.zip.ZipEntryContext;
import org.jboss.virtual.plugins.context.zip.ZipEntryContextFactory;
import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URLStreamHandler for VFS
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */

public class Handler extends URLStreamHandler
{
   protected URLConnection openConnection(URL u) throws IOException
   {
      String url = u.toString();
      ZipEntryContext ctx = (ZipEntryContext) ZipEntryContextFactory.getInstance().getVFS(u);
      if (ctx == null)
         throw new IOException("No VFS context found for URL: " + url);

      String rootPath = ctx.getRootURI().getPath();
      String entryPath = u.getFile().substring(rootPath.length());
      
      VirtualFileHandler child = ctx.getChild(ctx.getRoot(), entryPath);
      VirtualFile vf = child == null ? null : child.getVirtualFile();
      if (vf == null)
         throw new IOException("No VFS file found for URL: " + url);

      return new VirtualFileURLConnection(u, vf);
   }
}
