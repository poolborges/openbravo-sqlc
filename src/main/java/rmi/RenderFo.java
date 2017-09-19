/*
 ************************************************************************************
 * Copyright (C) 2001-2011 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package rmi;


import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.rmi.registry.*;


import java.lang.*;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
//import org.apache.fop.apps.Driver;
//import org.apache.fop.apps.Version;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.xml.sax.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.FileAppender;

public class RenderFo extends UnicastRemoteObject implements RenderFoI {
    
    private static String BASEPATH = "."; //@basepath@
    private static String RENDERFO_ADDR = "localhost"; //@RenderFoAddress@

    private String strFo;
    static Logger logger = Logger.getLogger(RenderFo.class);

    public RenderFo() throws RemoteException {
    }

    public RenderFo(String strFo) throws RemoteException {
        this.strFo = strFo;
    }

    public Object execute() throws RemoteException {
        return computeRenderFo(strFo);
    }

    public byte[] computeRenderFo(String strFo) throws RemoteException {
        byte[] content = null;
        try {
            StringReader sr = new StringReader(strFo);
            InputSource inputFo = new InputSource(sr);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
//      Driver driver = new Driver(inputFo, out);
//      driver.run();
            content = out.toByteArray();
            out.close();
        } catch (Exception e) {
            Throwable t = (Throwable) e;
            logger.error("computeRenderFo exception: " + e.getMessage());
            e.printStackTrace();
        }
        return content;
    }

    // Registration for RMI serving:
    public static void main(String[] args) {
        SimpleLayout layout = new SimpleLayout();

        FileAppender appender = null;

        Date today = new Date();

        // format the date in the form "Wed 27 Aug, 2003"
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        String strToday = formatter.format(today);

        String strFileName = "renderFo" + strToday + ".log";

        try {
            appender = new FileAppender(layout, BASEPATH+"\\logs\\" + strFileName, false); // eg.
            // @basepath@
            // = c:\\rmi
        } catch (Exception e) {
        }

        logger.addAppender(appender);
        logger.setLevel((Level) Level.WARN);

        logger.warn("Try setSecurityManager ");
        System.setProperty("java.security.policy","file://./java.policy");
        // java.net.SocketPermission, java.net.NetPermission,
        // java.security.AccessControlException: access denied 
        //("java.net.SocketPermission" "127.0.0.1:4160" "connect,resolve")
        // p1 = new SocketPermission("127.0.0.1:1099", "connect,accept");
        // is granted to some code, it allows that code to accept connections on, 
        // connect to, or listen on any port between 1024 and 65535 on the local host.
        // p2 = new SocketPermission("localhost:1024-", "accept,connect,listen");
        if (System.getSecurityManager() == null) {
            logger.warn("getSecurityManager is null");
            SecurityManager sm = new SecurityManager();
            //sm.
            System.setSecurityManager(sm);
            // install suitable security manager
            //System.setSecurityManager(new RMISecurityManager());
        }
        

        try {
            RenderFo render = new RenderFo();
            Naming.bind("//"+RENDERFO_ADDR+"/RenderFo", render);
            logger.warn("RenderFo ready");
        } catch (Exception e) {
            logger.error("Error binding:" + e.getMessage());
            e.printStackTrace();
        }
    }
}
