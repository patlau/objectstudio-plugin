/**
 * The MIT License
 * Copyright (c) 2015 Patrick Lauper
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.objectstudio;

import hudson.Extension;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * ObjectStudio8Builder {@link ObjectStudioBuilder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link ObjectStudio8Builder#newInstance(StaplerRequest)} is invoked
 * and a new {@link ObjectStudio8Builder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream.
 *
 * @author Patrick Lauper
 */
public class ObjectStudio8Builder extends ObjectStudioBuilder {
    
	/**
	 * Parameters passed to VisualWorks.
	 */
	String visualWorksParameter = "";
	
	/**
	 * Make VisualWorks write a memory usage report (-xq).
	 */
	Boolean reportMemoryUsage = true;
	
	
	/**
	 * Fields in config.jelly must match the parameter names in the "DataBoundConstructor".
	 * 
	 * @param buildPath 		Run build in this subdirectory
	 * @param preloadScript 	Preload Script
	 * @param loadScript		Load Script
	 * @param postloadScript	Postload Script
	 * @param ostudioIni		Ostudio.ini File to be used
	 * @param ostudioLog		Name of logfile passed to -o
	 * @param ostudioParameter	More ObjectStudio parameters
	 * @param ostudioImage		ObjectStudio image to be used
	 * @param visualWorksParameter	VisualWorks parameters
	 * @param reportMemoryUsage		Write memory usage report
	 */
    @DataBoundConstructor
    public ObjectStudio8Builder(String buildPath, String preloadScript, 
    		String loadScript, String postloadScript, String ostudioIni, 
    		String ostudioLog, String ostudioParameter, String ostudioImage,
    		String visualWorksParameter, Boolean reportMemoryUsage) {
        super(buildPath, preloadScript, loadScript, postloadScript, ostudioIni, ostudioLog, ostudioParameter, ostudioImage);
        this.visualWorksParameter = visualWorksParameter;
        this.reportMemoryUsage = reportMemoryUsage;
    }
	/**
	 * Is memory usage report enabled.
	 * @return true if memory usage report is enabled
	 */
	public Boolean getReportMemoryUsage() {
		return reportMemoryUsage == null ? false : reportMemoryUsage;
	}

	/**
	 * Enable or disable memory usage report.
	 * @param reportMemoryUsage true if memory usage is enabled, otherwise false
	 */
	public void setReportMemoryUsage(Boolean reportMemoryUsage) {
		this.reportMemoryUsage = reportMemoryUsage;
	}

	/**
	 * Get VisualWorks command line parameter.
	 * @return String containing command line parameter
	 */
	public String getVisualWorksParameter() {
		return visualWorksParameter == null ? "" : visualWorksParameter;
	}

	/**
	 * Set VisualWorks command line parameter.
	 * @param visualWorksParameter	String containing command line parameter
	 */
	public void setVisualWorksParameter(String visualWorksParameter) {
		this.visualWorksParameter = visualWorksParameter;
	}

	/**
	 * Get absolute path to ObjectStudio.exe.
	 * @return absolute path to ObjectStudio.exe
	 */
	@Override
	public String getObjectStudioExe() {
		return getDescriptor().getPathAbsolute() + "ObjectStudio.exe";
	}


	@Override
	protected ObjectStudio getObjectStudio() {
		return new ObjectStudio8();
	}
    
    /**
     * Return plugin descriptor for Jenkins.
     * @return ObjectStudio8 descriptor instance
     */
    @Override
    public ObjectStudioDescriptor8 getDescriptor() {
        return (ObjectStudioDescriptor8) super.getDescriptor();
    }

    /**
     * Implement plugin descriptor for Jenkins.
     * This indicates to Jenkins that this is an implementation of an extension point.
     */
    @Extension 
    public static final class ObjectStudioDescriptor8 extends ObjectStudioBuilder.ObjectStudioDescriptor {
        
    	/**
    	 * Get plugin display name.
    	 * @return display name
    	 */
    	@Override
		public String getDisplayName() {
            return "ObjectStudio 8 Builder";
        }

    	/** 
    	 * Configure the plugin from UI.
    	 * @param req Stapler request
    	 * @param formData JSON object containing form data
    	 * @return true when successfully configured
    	 */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        	req.bindJSON(this, formData.getJSONObject("objectStudio8"));
            save();
            return super.configure(req, formData);
        }
        
        /**
         * Default Path for ObjectStudio 8. Should be taken from Windows Registry.
         * @return absolute path to ObjectStudio8 installation directory
         */
        @Override
        protected String getDefaultPath() {
			return "C:\\Program Files (x86)\\Cincom\\ObjectStudio8.6\\";
		}
    }
}