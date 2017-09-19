/*
 ************************************************************************************
 * Copyright (C) 2010-2015 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.modulescript;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.buildvalidation.BuildValidationHandler;

public class ModuleScriptHandler extends Task {

    private static final Logger log4j = Logger.getLogger(ModuleScriptHandler.class);

    private File basedir;
    private String moduleJavaPackage;
    private Map<String, OpenbravoVersion> modulesVersionMap;

    @Override
    public void execute() {
        List<String> classes = new ArrayList<String>();
        if (moduleJavaPackage != null) {
            // We will only be executing the ModuleScripts of a specific module
            File moduleDir = new File(basedir, "modules/" + moduleJavaPackage + "/build/classes");
            BuildValidationHandler.readClassFiles(classes, moduleDir);
        } else {
            File coreBuildFolder = new File(basedir, "src-util/modulescript/build/classes");
            BuildValidationHandler.readClassFiles(classes, coreBuildFolder);
            File moduleFolder = new File(basedir, "modules");
            File modFoldersA[] = moduleFolder.listFiles();
            ArrayList<File> modFolders = new ArrayList<File>();
            for (File f : modFoldersA) {
                modFolders.add(f);
            }
            Collections.sort(modFolders);
            for (File modFolder : modFolders) {
                if (modFolder.isDirectory()) {
                    File validationFolder = new File(modFolder, "build/classes");
                    if (validationFolder.exists()) {
                        BuildValidationHandler.readClassFiles(classes, validationFolder);
                    }
                }
            }
        }
        for (String s : classes) {
            try {
                Class<?> myClass = Class.forName(s);
                if (ModuleScript.class.isAssignableFrom(myClass)) {
                    ModuleScript instance = (ModuleScript) myClass.newInstance();
                    instance.preExecute(getModulesVersionMap());
                }
            } catch (Exception e) {
                log4j.error("Error executing moduleScript: " + s, e);
                throw new BuildException("Execution of moduleScript " + s + " failed.");
            }
        }
    }

    /**
     * Returns a File with the base directory
     *
     * @return a File with the base directory
     */
    public File getBasedir() {
        return basedir;
    }

    /**
     * Sets the base directory
     *
     * @param basedir File used to set the base directory
     */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    /**
     * Returns the java package
     *
     * @return a String with the module java package
     */
    public String getModuleJavaPackage() {
        return moduleJavaPackage;
    }

    /**
     * Sets the java package
     *
     * @param moduleJavaPackage String to set the java package
     */
    public void setModuleJavaPackage(String moduleJavaPackage) {
        this.moduleJavaPackage = moduleJavaPackage;
    }

    /**
     * Creates the OpenbravoVersion map from a map of version strings
     *
     * @param currentVersionsMap A data structure that contains Strings with
     * module versions mapped by module id
     */
    public void setModulesVersionMap(Map<String, String> currentVersionsMap) {
        modulesVersionMap = new HashMap<String, OpenbravoVersion>();
        for (Map.Entry<String, String> entry : currentVersionsMap.entrySet()) {
            try {
                modulesVersionMap.put(entry.getKey(), new OpenbravoVersion(entry.getValue()));
            } catch (Exception ex) {
                log4j.error(
                        "Not possible to recover the current version of module with id: " + entry.getKey(), ex);
            }
        }
    }

    /**
     * Returns a map with the current module versions
     *
     * @return A data structure that contains module versions mapped by module
     * id
     */
    public Map<String, OpenbravoVersion> getModulesVersionMap() {
        return this.modulesVersionMap;
    }
}
