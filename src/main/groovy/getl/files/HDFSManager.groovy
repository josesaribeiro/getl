/*
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for "Groovy ETL".

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.

 Copyright (C) EasyData Company LTD

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License and
 GNU Lesser General Public License along with this program.
 If not, see <http://www.gnu.org/licenses/>.
*/

package getl.files

import getl.exception.ExceptionGETL
import getl.files.sub.FileManagerList
import getl.utils.FileUtils
import getl.utils.StringUtils
import groovy.transform.CompileStatic
import org.apache.hadoop.fs.*
import org.apache.hadoop.conf.*
import getl.utils.Logs
import org.apache.hadoop.security.UserGroupInformation
import java.security.PrivilegedExceptionAction

/**
 * HDFS manager
 * @author Alexsey Konstantinov
 */
class HDFSManager extends Manager {
    @Override
    protected void initMethods () {
        super.initMethods()
        methodParams.register("super", ["server", "port", "login"])
    }

    /** Server address */
    String getServer () { params.server }
    /** Server address */
    void setServer (String value) { params.server = value }

    /** Server port */
    Integer getPort () { (params.port != null)?(params.port as Integer):8022 }
    /** Server port */
    void setPort (Integer value) { params.port = value }

    /** Login user */
    String getLogin () { params.login }
    /** Login user */
    void setLogin (String value) { params.login = value }

    /*
     * Password user
     */
    /*
    public String getPassword () { params.password }
    public void setPassword (String value) { params.password = value }
    */

    /** File system driver */
    private FileSystem client

    /** Home directory by user */
    private String homeDirectory
    /** Home directory by user */
    String getHomeDirectory() { this.homeDirectory }

    @Override
    boolean isCaseSensitiveName() {
        return true
    }

    @Override
    boolean isConnected() { client != null }

    @Override
    void connect() {
        if (connected)
            throw new ExceptionGETL('Manager already connected!')

        if (server == null || port == null) throw new ExceptionGETL("Required server host and port for connect")
        if (login == null) throw new ExceptionGETL("Required login for connect")

        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(login)
        ugi.doAs(
            new PrivilegedExceptionAction<Void>() {
                Void run() {
                    Configuration conf = new Configuration()
                    conf.set("fs.defaultFS", "hdfs://$server:$port")
                    conf.set("hadoop.job.ugi", login)


//                    conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.getClass().name)
//                    conf.set("fs.file.impl",org.apache.hadoop.fs.LocalFileSystem.getClass().name)
                    try {
                        client = FileSystem.get(conf)
                    }
                    catch (Exception e) {
                        if (writeErrorsToLog) Logs.Severe("Can not connect to $server:$port")
                        throw e
                    }
                    homeDirectory = client.homeDirectory
                    setCurrentPath(rootPath)

                    return null
                }
            }
        )
    }

    @Override
    void disconnect() {
        if (!connected)
            throw new ExceptionGETL('Manager already disconnected!')

        try {
            if (client != null) client.close()
        }
        finally {
            client = null
            homeDirectory = null
            curPath = null
        }
    }

    /**
     * Current path name
     */
    private String curPath

    @Override
    String getCurrentPath() {
        return curPath
    }

    @Override
    void setCurrentPath(String path) {
        validConnect()

        if (path == curPath) return

        if (path == null || path == '/') {
            if (writeErrorsToLog) Logs.Severe('Invalid path: \"$path\"')
            throw new ExceptionGETL('Invalid null path')
        }
//        if (path[0] == '/' && StringUtils.LeftStr(path, 6) != '/user/') path = path.substring(1)
        path = fullName(path, null)
        def p = new Path(path)
        if (!client.exists(p)) {
            if (writeErrorsToLog) Logs.Severe("Path \"$path\" not found")
            throw new ExceptionGETL("Path \"$path\" not found")
        }
        if (!client.exists(p) || !client.isDirectory(p)) { /* todo: rewrite */
            if (writeErrorsToLog) Logs.Severe("Path \"$path\" non directory")
            throw new ExceptionGETL("Path \"$path\" non directory")
        }
        curPath = path
    }

    private fullName(String dir, String file) {
        if (dir != null && dir[0] == '/' && StringUtils.LeftStr(dir, 6) != '/user/') dir = dir.substring(1)
        if (!((dir + '/').matches(rootPath + '/.*'))) dir = rootPath + '/' + dir
        return ((dir != null)?dir:'') + ((file != null)?"/$file":'')
    }

    private Path fullPath(String dir, String file) {
        new Path(fullName(dir, file))
    }

    class HDFSList extends FileManagerList {
        FileStatus[] listFiles

        @CompileStatic
        @Override
        Integer size() {
            listFiles.length
        }

        @CompileStatic
        @Override
        Map item(int index) {
            FileStatus f = listFiles[index]

            Map<String, Object> m = new HashMap<String, Object>()
            m.filename = f.path.name
            m.filedate = new Date(f.modificationTime)
            m.filesize = f.len
            if  (f.isSymlink()) m.link = f.symlink.name

            if (f.directory) {
                m.type = Manager.TypeFile.DIRECTORY
            }
            else if (f.file) {
                m.type = Manager.TypeFile.FILE
            }
            else if (f.symlink) {
                m.type = Manager.TypeFile.LINK
            }
            else {
                throw new ExceptionGETL("Unnknown type object ${m.filename}")
            }

            m
        }

        @CompileStatic
        @Override
        void clear() {
            listFiles = []
        }
    }

    @Override
    FileManagerList listDir(String maskFiles) {
        validConnect()

        HDFSList res = new HDFSList()
        res.listFiles = client.listStatus(fullPath(currentPath, null))

        return res
    }

    @Override
    void changeDirectoryUp() {
        validConnect()

        if (currentPath == rootPath) {
            if (writeErrorsToLog) Logs.Severe("Can not change directory to up with root directory \"$rootPath\"")
            throw new ExceptionGETL("Can not change directory to up with root directory \"$rootPath\"")
        }

        String[] l = currentPath.split('/')
        def n = []
        for (int i = 0; i < l.length - 1; i++) {
            n << l[i]
        }
        def c = n.join('/')

        try {
            currentPath = c
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not change directory to up")
            throw e
        }
    }

    @Override
    void download(String fileName, String path, String localFileName) {
        validConnect()

        def fn = ((path != null)?path + '/':'') + localFileName
        try {
            def p = fullPath(currentPath, fileName)
            client.copyToLocalFile(false, p, new Path(fn), true)
            def f = new File(fn)
            setLocalLastModified(f, getLastModified(fileName))
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not download file \"${fullName(currentPath, fileName)}\" to \"$fn\"")
            throw e
        }
    }

    @Override
    void upload(String path, String fileName) {
        validConnect()

        def fn = ((path != null)?path + "/":"") + fileName
        try {
            def p = fullPath(currentPath, fileName)
            client.copyFromLocalFile(new Path(fn), p)
            def f = new File(fn)
            setLastModified(fileName, f.lastModified())
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not upload file \"$fn\" to \"${fullName(currentPath, fileName)}\"")
            throw e
        }
    }

    @Override
    void removeFile(String fileName) {
        validConnect()

        try {
            client.delete(fullPath(currentPath, fileName), false)
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not remove file \"${fullName(currentPath, fileName)}\"")
            throw e
        }
    }

    @Override
    void createDir(String dirName) {
        validConnect()

        try {
            client.mkdirs(fullPath(currentPath, dirName))
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not create dir \"${fullName(currentPath, dirName)}\"")
            throw e
        }
    }

    @Override
    void removeDir(String dirName, Boolean recursive) {
        validConnect()

        try {
            client.delete(fullPath(currentPath, dirName), recursive)
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not remove dir \"${fullName(currentPath, dirName)}\"")
            throw e
        }
    }

    @Override
    void rename(String fileName, String path) {
        validConnect()

        try {
            if (FileUtils.RelativePathFromFile(path, '/') == '.')
                path = fullPath(currentPath, path)
            client.rename(fullPath(currentPath, fileName), new Path(path))
        }
        catch (Exception e) {
            if (writeErrorsToLog) Logs.Severe("Can not rename file \"${fullName(currentPath, fileName)}\" to \"$path\"")
            throw e
        }
    }

    @Override
    boolean existsDirectory(String dirName) {
        validConnect()

        return client.exists(fullPath(dirName, null))
    }

    @Override
    long getLastModified(String fileName) {
        validConnect()

        def s = client.getFileStatus(fullPath(currentPath, fileName))
        return s.modificationTime
    }

    @Override
    void setLastModified(String fileName, long time) {
        validConnect()

        if (saveOriginalDate)
            client.setTimes(fullPath(currentPath, fileName), time, -1)
    }

    @Override
    String toString() {
        if (server == null) return 'hdfs'
        String res
        if (rootPath == null || rootPath.length() == 0)
            res = "hdfs://$server"
        else if (rootPath[0] == '/')
            res = "hdfs://$server$rootPath"
        else
            res = "hdfs://$server/$rootPath"

        return res
    }

    @Override
    void noop () {
        super.noop()
        client.getStatus()
    }
}