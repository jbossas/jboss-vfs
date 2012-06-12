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
package org.jboss.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.util.collection.CollectionsFactory;
import org.jboss.virtual.plugins.copy.CopyMechanism;
import org.jboss.virtual.plugins.copy.ExplodedCopyMechanism;
import org.jboss.virtual.plugins.copy.TempCopyMechanism;
import org.jboss.virtual.plugins.copy.UnjarCopyMechanism;
import org.jboss.virtual.plugins.copy.UnpackCopyMechanism;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.LinkInfo;
import org.jboss.virtual.spi.Options;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.cache.VFSCacheFactory;

/**
 * VFS Utilities
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class VFSUtils
{
   /** The log */
   private static final Logger log = Logger.getLogger(VFSUtils.class);

   /** The link */
   public static final String VFS_LINK_INFIX = ".vfslink";

   /** The link properties */
   public static final String VFS_LINK_PROPERTIES_SUFFIX = ".vfslink.properties";

   /** The link name */
   public static final String VFS_LINK_NAME = "vfs.link.name";
   /** The link target */
   public static final String VFS_LINK_TARGET = "vfs.link.target";

   /**
    * The system no force copy key / query
    */
   public static final String FORCE_COPY_KEY = "jboss.vfs.forceCopy";
   public static final String USE_COPY_QUERY = "useCopyJarHandler";

   /**
    * Key used to force fallback from vfszip (default) to vfsjar
    */
   public static final String FORCE_VFS_JAR_KEY = "jboss.vfs.forceVfsJar";

   /**
    * Key used to turn off reaper mode in vfszip - forcing synchronous (slower) handling of files
    */
   public static final String FORCE_NO_REAPER_KEY = "jboss.vfs.forceNoReaper";
   public static final String NO_REAPER_QUERY = "noReaper";

   /**
    * Key used to force case sensitive path checking in vfsfile
    */
   public static final String FORCE_CASE_SENSITIVE_KEY = "jboss.vfs.forceCaseSensitive";
   public static final String CASE_SENSITIVE_QUERY = "caseSensitive";

   /**
    * Key used to force canonical lookup
    */
   public static final String FORCE_CANONICAL = "jboss.vfs.forceCanonical";

   /**
    * Key used to turn on memory optimizations - less cache use at the expense of performance
    */
   public static final String OPTIMIZE_FOR_MEMORY_KEY = "jboss.vfs.optimizeForMemory";

   /**
    * Key used to determine VFS Cache impl
    */
   public static final String VFS_CACHE_KEY = "jboss.vfs.cache";

   /**
    * Constant representing the URL file protocol
    */
   public static final String FILE_PROTOCOL = "file";
   
   /** Standard separator for JAR URL */
   public static final String JAR_URL_SEPARATOR = "!/";

   /** The temp marker flag */
   public static final String IS_TEMP_FILE = "IS_TEMP_FILE";

   /**
    * Stop cache.
    */
   public static void stopCache()
   {
      VFSCacheFactory.getInstance().stop();
   }

   /**
    * Get the paths string for a collection of virtual files
    *
    * @param paths the paths
    * @return the string
    * @throws IllegalArgumentException for null paths
    */
   public static String getPathsString(Collection<VirtualFile> paths)
   {
      if (paths == null)
         throw new IllegalArgumentException("Null paths");

      StringBuilder buffer = new StringBuilder();
      boolean first = true;
      for (VirtualFile path : paths)
      {
         if (path == null)
            throw new IllegalArgumentException("Null path in " + paths);
         if (first == false)
            buffer.append(':');
         else
            first = false;
         buffer.append(path.getPathName());
      }

      if (first == true)
         buffer.append("<empty>");

      return buffer.toString();
   }

   /**
    * Add manifest paths
    *
    * @param file the file
    * @param paths the paths to add to
    * @throws IOException if there is an error reading the manifest or the
    *         virtual file is closed
    * @throws IllegalStateException if the file has no parent
    * @throws IllegalArgumentException for a null file or paths
    */
   public static void addManifestLocations(VirtualFile file, List<VirtualFile> paths) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (paths == null)
         throw new IllegalArgumentException("Null paths");

      boolean trace = log.isTraceEnabled();
      
      Manifest manifest = getManifest(file);
      if (manifest == null)
         return;

      Attributes mainAttributes = manifest.getMainAttributes();
      String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);

      if (classPath == null)
      {
         if (trace)
            log.trace("Manifest has no Class-Path for " + file.getPathName());
         return;
      }

      VirtualFile parent = file.getParent();
      if (parent == null)
      {
         log.debug(file + " has no parent.");
         return;
      }

      if (trace)
         log.trace("Parsing Class-Path: " + classPath + " for " + file.getName() + " parent=" + parent.getName());
      
      StringTokenizer tokenizer = new StringTokenizer(classPath);
      while (tokenizer.hasMoreTokens())
      {
         String path = tokenizer.nextToken();
         try
         {
            VirtualFile vf = parent.getChild(path);
            if(vf != null)
            {
               if(paths.contains(vf) == false)
               {
                  paths.add(vf);
                  // Recursively process the jar
                  addManifestLocations(vf, paths);
               }
               else if (trace)
                  log.trace(vf.getName() + " from manifiest is already in the classpath " + paths);
            }
            else if (trace)
               log.trace("Unable to find " + path + " from " + parent.getName());
         }
         catch (IOException e)
         {
            log.debug("Manifest Class-Path entry " + path + " ignored for " + file.getPathName() + " reason=" + e);
         }
      }
   }

   /**
    * Get a manifest from a virtual file,
    * assuming the virtual file is the root of an archive
    *
    * @param archive the root the archive
    * @return the manifest or null if not found
    * @throws IOException if there is an error reading the manifest or the
    *         virtual file is closed
    * @throws IllegalArgumentException for a null archive
    */
   public static Manifest getManifest(VirtualFile archive) throws IOException
   {
      if (archive == null)
         throw new IllegalArgumentException("Null archive");

      VirtualFile manifest = archive.getChild(JarFile.MANIFEST_NAME);
      if (manifest == null)
      {
         if (log.isTraceEnabled())
            log.trace("Can't find manifest for " + archive.getPathName());
         return null;
      }
      return readManifest(manifest);
   }

   /**
    * Read the manifest from given manifest VirtualFile.
    *
    * @param manifest the VF to read from
    * @return JAR's manifest
    * @throws IOException if problems while opening VF stream occur
    */
   public static Manifest readManifest(VirtualFile manifest) throws IOException
   {
      if (manifest == null)
         throw new IllegalArgumentException("Null manifest file");

      InputStream stream = manifest.openStream();
      try
      {
         return new Manifest(stream);
      }
      finally
      {
         try
         {
            stream.close();
         }
         catch (IOException ignored)
         {
         }
      }
   }

   /**
     * Get a manifest from a virtual file system,
     * assuming the root of the VFS is the root of an archive
     *
     * @param archive the vfs
     * @return the manifest or null if not found
     * @throws IOException if there is an error reading the manifest
     * @throws IllegalArgumentException for a null archive
     */
    public static Manifest getManifest(VFS archive) throws IOException
    {
       if (archive == null)
         throw new IllegalArgumentException("Null vfs archive");

       VirtualFile root = archive.getRoot();
       return getManifest(root);
    }

   /**
    * Fix a name (removes any trailing slash)
    *
    * @param name the name to fix
    * @return the fixed name
    * @throws IllegalArgumentException for a null name
    */
   public static String fixName(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      int length = name.length();
      if (length <= 1)
         return name;
      if (name.charAt(length-1) == '/')
         return name.substring(0, length-1);

      return name;
   }

   /**
    * Get the name.
    *
    * @param uri the uri
    * @return name from uri's path
    */
   public static String getName(URI uri)
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      String name = uri.getPath();
      if( name != null )
      {
         // TODO: Not correct for certain uris like jar:...!/
         int lastSlash = name.lastIndexOf('/');
         if( lastSlash > 0 )
            name = name.substring(lastSlash+1);
      }
      return name;
   }

   /**
    * Take a URL.getQuery string and parse it into name=value pairs
    *
    * @param query Possibly empty/null url query string
    * @return String[] for the name/value pairs in the query. May be empty but never null.
    */
   public static Map<String, String> parseURLQuery(String query)
   {
	   Map<String, String> pairsMap = CollectionsFactory.createLazyMap();
      if(query != null)
      {
   	   StringTokenizer tokenizer = new StringTokenizer(query, "=&");
   	   while(tokenizer.hasMoreTokens())
   	   {
   		   String name = tokenizer.nextToken();
   		   String value = tokenizer.nextToken();
   		   pairsMap.put(name, value);
   	   }
      }
	   return pairsMap;
   }

   /**
    * Does a vf name contain the VFS link prefix
    * @param name - the name portion of a virtual file
    * @return true if the name starts with VFS_LINK_PREFIX, false otherwise
    */
   public static boolean isLink(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      return name.indexOf(VFS_LINK_INFIX) >= 0;
   }

   /**
    * Read the link information from the stream based on the type as determined
    * from the name suffix.
    *
    * @param is - input stream to the link file contents
    * @param name - the name of the virtual file representing the link
    * @param props the propertes
    * @return a list of the links read from the stream
    * @throws IOException on failure to read/parse the stream
    * @throws URISyntaxException for an error parsing a URI
    */
   public static List<LinkInfo> readLinkInfo(InputStream is, String name, Properties props)
      throws IOException, URISyntaxException
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      if(name.endsWith(VFS_LINK_PROPERTIES_SUFFIX))
      {
         List<LinkInfo> info = new ArrayList<LinkInfo>();
         parseLinkProperties(is, info, props);
         return info;
      }
      else
         throw new UnsupportedEncodingException("Unknown link format: " + name);
   }

   /**
    * Parse a properties link file
    *
    * @param is - input stream to the link file contents
    * @param info the link infos
    * @param props the propertes
    * @throws IOException on failure to read/parse the stream
    * @throws URISyntaxException for an error parsing a URI
    */
   public static void parseLinkProperties(InputStream is, List<LinkInfo> info, Properties props)
      throws IOException, URISyntaxException
   {
      if (is == null)
         throw new IllegalArgumentException("Null input stream");
      if (info == null)
         throw new IllegalArgumentException("Null info");
      if (props == null)
         throw new IllegalArgumentException("Null properties");

      props.load(is);
      // Iterate over the property tuples
      for(int n = 0; ; n ++)
      {
         String nameKey = VFS_LINK_NAME + "." + n;
         String name = props.getProperty(nameKey);
         String uriKey = VFS_LINK_TARGET + "." + n;
         String uri = props.getProperty(uriKey);
         // End when the value is null since a link may not have a name
         if (uri == null)
         {
            break;
         }
         // Replace any system property references
         uri = StringPropertyReplacer.replaceProperties(uri);
         LinkInfo link = new LinkInfo(name, new URI(uri));
         info.add(link);
      }
   }

   /**
    * Deal with urls that may include spaces.
    *
    * @param url the url
    * @return uri the uri
    * @throws URISyntaxException for any error
    */
   public static URI toURI(URL url) throws URISyntaxException
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");

      try
      {
         return url.toURI();
      }
      catch (URISyntaxException e)
      {
         String urispec = url.toExternalForm();
         // Escape percent sign and spaces
         urispec = urispec.replaceAll("%", "%25");
         urispec = urispec.replaceAll(" ", "%20");
         return new URI(urispec);
      }
   }

   /**
    * Ensure the url is convertible to URI by encoding spaces and percent characters if necessary
    *
    * @param url to be sanitized
    * @return sanitized URL
    * @throws URISyntaxException if URI conversion can't be fixed
    * @throws MalformedURLException if an error occurs
    */
   public static URL sanitizeURL(URL url) throws URISyntaxException, MalformedURLException
   {
      return toURI(url).toURL();
   }

   /**
    * Get the options for this file.
    *
    * @param file the file
    * @return options map
    */
   private static Options getOptions(VirtualFile file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      VirtualFileHandler handler = file.getHandler();
      VFSContext context = handler.getVFSContext();
      return context.getOptions();
   }

   /**
    * Get the options for this vfs.
    *
    * @param vfs the vfs
    * @return options map
    */
   private static Options getOptions(VFS vfs)
   {
      if (vfs == null)
         throw new IllegalArgumentException("Null vfs");

      VFSContext context = vfs.getContext();
      return context.getOptions();
   }

   /**
    * Get the option.
    *
    * @param file the file
    * @param key the option key
    * @return key's option
    */
   public static String getOption(VirtualFile file, String key)
   {
      Object option = getOption(file, key, Object.class);
      return option != null ? option.toString() : null;
   }

   /**
    * Get the option.
    *
    * @param vfs the vfs
    * @param key the option key
    * @return key's option
    */
   public static String getOption(VFS vfs, String key)
   {
      Object option = getOption(vfs, key, Object.class);
      return option != null ? option.toString() : null;
   }

   /**
    * Get the option.
    *
    * @param <T> exact type
    * @param file the file
    * @param key the option key
    * @param exactType the exact type
    * @return key's option
    */
   public static <T> T getOption(VirtualFile file, String key, Class<T> exactType)
   {
      Options options = getOptions(file);
      return options != null ? options.getOption(key, exactType) : null;
   }

   /**
    * Get the option.
    *
    * @param <T> exact type
    * @param vfs the vfs
    * @param key the option key
    * @param exactType the exact type
    * @return key's option
    */
   public static <T> T getOption(VFS vfs, String key, Class<T> exactType)
   {
      Options options = getOptions(vfs);
      return options != null ? options.getOption(key, exactType) : null;
   }

   /**
    * Enable option.
    *
    * @param file the file
    * @param optionName option name
    */
   protected static void enableOption(VirtualFile file, String optionName)
   {
      Options options = getOptions(file);
      if (options == null)
         throw new IllegalArgumentException("Cannot enable " + optionName + " on null options: " + file);

      options.addOption(optionName, Boolean.TRUE);
   }

   /**
    * Disable option.
    *
    * @param file the file
    * @param optionName option name
    */
   protected static void disableOption(VirtualFile file, String optionName)
   {
      Options options = getOptions(file);
      if (options == null)
         throw new IllegalArgumentException("Cannot disable " + optionName + " on null options: " + file);

      options.removeOption(optionName);
   }

   /**
    * Enable option.
    *
    * @param vfs the vfs
    * @param optionName option name
    */
   protected static void enableOption(VFS vfs, String optionName)
   {
      Options options = getOptions(vfs);
      if (options == null)
         throw new IllegalArgumentException("Cannot enable " + optionName + " on null options: " + vfs);

      options.addOption(optionName, Boolean.TRUE);
   }

   /**
    * Disable option.
    *
    * @param vfs the vfs
    * @param optionName option name
    */
   protected static void disableOption(VFS vfs, String optionName)
   {
      Options options = getOptions(vfs);
      if (options == null)
         throw new IllegalArgumentException("Cannot disable " + optionName + " on null options: " + vfs);

      options.removeOption(optionName);
   }

   /**
    * Enable copy for file param.
    *
    * @param file the file
    */
   public static void enableCopy(VirtualFile file)
   {
      enableOption(file, USE_COPY_QUERY);
   }

   /**
    * Disable copy for file param.
    *
    * @param file the file
    */
   public static void disableCopy(VirtualFile file)
   {
      disableOption(file, USE_COPY_QUERY);
   }

   /**
    * Enable copy for vfs param.
    *
    * @param vfs the vfs
    */
   public static void enableCopy(VFS vfs)
   {
      enableOption(vfs, USE_COPY_QUERY);
   }

   /**
    * Disable copy for vfs param.
    *
    * @param vfs the vfs
    */
   public static void disableCopy(VFS vfs)
   {
      disableOption(vfs, USE_COPY_QUERY);
   }

   /**
    * Enable reaper for file param.
    *
    * @param file the file
    */
   public static void enableNoReaper(VirtualFile file)
   {
      enableOption(file, NO_REAPER_QUERY);
   }

   /**
    * Disable reaper for file param.
    *
    * @param file the file
    */
   public static void disableNoReaper(VirtualFile file)
   {
      disableOption(file, NO_REAPER_QUERY);
   }

   /**
    * Enable reaper for vfs param.
    *
    * @param vfs the vfs
    */
   public static void enableNoReaper(VFS vfs)
   {
      enableOption(vfs, NO_REAPER_QUERY);
   }

   /**
    * Disable reaper for vfs param.
    *
    * @param vfs the vfs
    */
   public static void disableNoReaper(VFS vfs)
   {
      disableOption(vfs, NO_REAPER_QUERY);
   }

   /**
    * Enable case sensitive for file param.
    *
    * @param file the file
    */
   public static void enableCaseSensitive(VirtualFile file)
   {
      enableOption(file, CASE_SENSITIVE_QUERY);
   }

   /**
    * Disable case sensitive for file param.
    *
    * @param file the file
    */
   public static void disableCaseSensitive(VirtualFile file)
   {
      disableOption(file, CASE_SENSITIVE_QUERY);
   }

   /**
    * Enable case sensitive for vfs param.
    *
    * @param vfs the vfs
    */
   public static void enableCaseSensitive(VFS vfs)
   {
      enableOption(vfs, CASE_SENSITIVE_QUERY);
   }

   /**
    * Disable case sensitive for vfs param.
    *
    * @param vfs the vfs
    */
   public static void disableCaseSensitive(VFS vfs)
   {
      disableOption(vfs, CASE_SENSITIVE_QUERY);
   }

   /**
    * Is the virtual file temporary.
    *
    * @param file the file
    * @return true if temporary, false otherwise
    */
   public static boolean isTemporaryFile(VirtualFile file)
   {
      Options options = getOptions(file);
      return options.getBooleanOption(IS_TEMP_FILE);
   }

   /**
    * Unpack the nested artifact under file param.
    *
    * @param file the file to unpack
    * @return unpacked file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile unpack(VirtualFile file) throws IOException, URISyntaxException
   {
      log.warn("Using unpack modification is not yet fully supported - rewire-ing issues.");
      return copy(file, UnpackCopyMechanism.INSTANCE);
   }

   /**
    * Force explode.
    * Explode archives or nested entries.
    *
    * @param file the file to explode
    * @return exploded file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile explode(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, ExplodedCopyMechanism.INSTANCE);
   }

   /**
    * Create temp.
    *
    * @param file the file to temp
    * @return temp file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile temp(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, TempCopyMechanism.INSTANCE);
   }

   /**
    * Unjar.
    *
    * @param file the file to unjar
    * @return temp file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile unjar(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, UnjarCopyMechanism.INSTANCE);
   }

   /**
    * Create temp.
    *
    * @param file the file to unpack/explode
    * @param mechanism the copy mechanism
    * @return temp file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   protected static VirtualFile copy(VirtualFile file, CopyMechanism mechanism) throws IOException, URISyntaxException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (mechanism == null)
         throw new IllegalArgumentException("Null copy mechanism");

      return mechanism.copy(file, file.getHandler());
   }

   /**
    * Is file handle nested.
    *
    * @param file the file handle to check
    * @return true if file/dir is nested otherwise false
    * @throws IOException for any error
    */
   public static boolean isNestedFile(VirtualFile file) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      VirtualFileHandler handler = file.getHandler();
      return handler.isNested();
   }

   /**
    * Copy input stream to output stream and close them both
    *
    * @param is input stream
    * @param os output stream
    * @throws IOException for any error
    */
   public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException
   {
      try
      {
         copyStream(is, os);
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch(IOException ignored)
            {
            }
         }
         if (os != null)
            os.close();
      }
   }

   /**
    * Copy input stream to output stream without closing streams.
    * Flushes output stream when done.
    *
    * @param is input stream
    * @param os output stream
    * @throws IOException for any error
    */
   public static void copyStream(InputStream is, OutputStream os) throws IOException
   {
      if (is == null)
         throw new IllegalArgumentException("input stream is null");
      if (os == null)
         throw new IllegalArgumentException("output stream is null");

      try
      {
         byte [] buff = new byte[65536];
         int rc = is.read(buff);
         while (rc != -1)
         {
            os.write(buff, 0, rc);
            rc = is.read(buff);
         }
      }
      finally
      {
         os.flush();
      }
   }

   /**
    * Get spec compatilbe url from virtual file.
    *
    * @param file the virtual file
    * @return spec compatible url
    * @throws Exception for any error
    */
   public static URL getCompatibleURL(VirtualFile file) throws Exception
   {
      return getCompatibleResource(file, URL_CREATOR);
   }

   /**
    * Get spec compatilbe uri from virtual file.
    *
    * @param file the virtual file
    * @return spec compatible uri
    * @throws Exception for any error
    */
   public static URI getCompatibleURI(VirtualFile file) throws Exception
   {
      return getCompatibleResource(file, URI_CREATOR);
   }

   /**
    * Create new compatible resource.
    *
    * @param file the virtual file
    * @param creator resoruce creator
    * @return new resource
    * @throws Exception for any error
    * @param <T> exact resource type
    */
   private static <T> T getCompatibleResource(VirtualFile file, ResourceCreator<T> creator) throws Exception
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (creator == null)
         throw new IllegalArgumentException("Null creator");

      if (isNestedFile(file))
      {
         return creator.getNestedResource(file);
      }
      else
      {
         // is not nested, so direct VFS resource is not an option
         return creator.getRealResource(file);
      }
   }

   /**
    * @param <T> exact resource type
    */
   private static interface ResourceCreator<T>
   {
      /**
       * Get resource from virtual file.
       *
       * @param file the virtual file
       * @return resource instance from file
       * @throws Exception for any error
       */
      T getNestedResource(VirtualFile file) throws Exception;

      /**
       * Get real resource from virtual file.
       *
       * @param file the virtual file
       * @return resource instance from file
       * @throws Exception for any error
       */
      T getRealResource(VirtualFile file) throws Exception;
   }

   private static final ResourceCreator<URL> URL_CREATOR = new ResourceCreator<URL>()
   {
      public URL getNestedResource(VirtualFile file) throws Exception
      {
         return file.toURL();
      }

      public URL getRealResource(VirtualFile file) throws Exception
      {
         return getRealURL(file);
      }
   };

   private static final ResourceCreator<URI> URI_CREATOR = new ResourceCreator<URI>()
   {
      public URI getNestedResource(VirtualFile file) throws Exception
      {
         return file.toURI();
      }

      public URI getRealResource(VirtualFile file) throws Exception
      {
         return toURI(getRealURL(file));
      }
   };

   /**
    * Get real url.
    * The closest thing that doesn't need the vfs url handlers.
    *
    * @param file the virtual file
    * @return real url
    * @throws IOException for any error
    * @throws URISyntaxException for any uri syntac error
    */
   public static URL getRealURL(VirtualFile file) throws IOException, URISyntaxException
   {
      VirtualFileHandler handler = file.getHandler();
      return handler.getRealURL();
   }

   /**
    * Get relative path.
    *
    * @param context the vfs context
    * @param uri the uri
    * @return uri's relative path to context's root
    */
   public static String getRelativePath(VFSContext context, URI uri)
   {
      String uriPath = stripProtocol(uri);
      String contextKey = getKey(context);
      return uriPath.substring(contextKey.length());
   }

   /**
    * Get path.
    * Apply Carlo's fix as well.
    *
    * @param uri the uri
    * @return uri's path
    */
   public static String getPath(URI uri)
   {
      String path = uri.getPath();
      if(path == null)
      {
         String s = uri.toString();
         if(s.startsWith("jar:file:"))
            path = s.substring("jar:file:".length()).replaceFirst("!/", "/") + "/";
      }
      return path;
   }

   /**
    * Strip protocol from url string, return tokens.
    *
    * @param uri the uri
    * @return uri's path string tokens
    */
   public static List<String> stripProtocolToTokens(URI uri)
   {
      String path = getPath(uri);
      try
      {
         return PathTokenizer.applySpecialPathsToTokens(path);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Strip protocol from url string.
    *
    * @param uri the uri
    * @return uri's path string
    */
   public static String stripProtocol(URI uri)
   {
      List<String> tokens = stripProtocolToTokens(uri);
      if (tokens.isEmpty() == false)
      {
         StringBuilder builder = new StringBuilder();
         for (String token : tokens)
         {
            builder.append("/").append(token);
         }
         builder.append("/");
         return builder.toString();
      }
      else
      {
         return "/";
      }
   }

   /**
    * Get path key.
    *
    * @param context the vfs context
    * @return contex's root path w/o protocol
    */
   public static String getKey(VFSContext context)
   {
      URI uri = context.getRootURI();
      return stripProtocol(uri);
   }
   
   /**
    * Returns a string of the contents of the virtual file, showing the names of all 
    * the nested directories and names, useful for debugging
    * 
    * @param file the virtual file to display the contents of
    * @return A string containing the contents of the virtual file
    * @throws RuntimeException if an error happened
    */
   public static String outputContents(VirtualFile file)
   {
      StringWriter writer = new StringWriter();
      outputContents(file, writer);
      return writer.toString();
   }

   /**
    * Output contents to writer.
    *
    * @param file the file
    * @param writer the writer
    */
   public static void outputContents(VirtualFile file, Writer writer)
   {
      if (writer == null)
         throw new IllegalArgumentException("Null writer.");

      try
      {
         //writer.write("Contents of " + file.toURI() + "\n");
         VirtualFileOutputter.visit(file, writer);
      }
      catch (Exception e)
      {
         throw new RuntimeException("ERROR displaying the contents of " + file.getName(), e);
      }
   }

   /**
    * Output visitor.
    */
   private static class VirtualFileOutputter implements VirtualFileVisitor
   {
      private Writer writer;
      private Map<VirtualFile, Integer> levels;

      private VirtualFileOutputter(Writer writer, Map<VirtualFile, Integer> levels)
      {
         this.writer = writer;
         // prepare levels
         this.levels = levels;
      }

      static void visit(VirtualFile file, Writer writer) throws IOException
      {
         Map<VirtualFile, Integer> levels = new HashMap<VirtualFile, Integer>();
         levels.put(file.getParent(), 0);
         VirtualFileOutputter visitor = new VirtualFileOutputter(writer, levels);
         file.visit(visitor);
      }

      public VisitorAttributes getAttributes()
      {
         VisitorAttributes attributes = new VisitorAttributes();
         attributes.setIncludeRoot(true);
         attributes.setRecurseFilter(VisitorAttributes.RECURSE_ALL);
         return attributes;
      }

      public void visit(VirtualFile file)
      {
         try
         {
            VirtualFile parent = file.getParent();
            int level = levels.get(parent);
            String suffix = "";
            if (file.isLeaf() == false)
            {
               suffix = "/";
               levels.put(file, level + 1);
            }
            String string = file.getName() + suffix;
            writeToBuffer(level, string);
         }
         catch (IOException e)
         {
            throw new RuntimeException("Cannot handle file: " + file, e);
         }
      }
      
      private void writeToBuffer(int level, String string) throws IOException
      {
         for (int i = 0 ; i < level ; i++)
            writer.append("  ");
         
         writer.append(string);
         writer.append("\n");
      }
   }
}
