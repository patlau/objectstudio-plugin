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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import net.sf.json.JSONObject;

import org.apache.commons.io.FilenameUtils;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * ObjectStudioBuilder.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link ObjectStudioDescriptor#newInstance(StaplerRequest)} is invoked and a
 * new {@link ObjectStudioBuilder} is created. The created instance is persisted
 * to the project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #buildPath}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Patrick Lauper
 */
public class ObjectStudioBuilder extends Builder {

	String buildPath = "";
	String loadScript = "load.txt";
	String ostudioIni = "ostudio.ini";
	String ostudioImage = "ostudio.img";
	String ostudioLog = "ostudio.log";
	String ostudioParameter = "-E50";
	String postloadScript = "postload.txt";
	String preloadScript = "preload.txt";
	Boolean imageCopy = true;

	/**
	 * Fields in config.jelly must match the parameter names in the
	 * "DataBoundConstructor".
	 *
	 * @param buildPath
	 *            Run build in this subdirectory
	 * @param preloadScript
	 *            Preload Script
	 * @param loadScript
	 *            Load Script
	 * @param postloadScript
	 *            Postload Script
	 * @param ostudioIni
	 *            Ostudio.ini File to be used
	 * @param ostudioLog
	 *            Name of logfile passed to -o
	 * @param ostudioParameter
	 *            More ObjectStudio parameters
	 * @param ostudioImage
	 *            ObjectStudio image to be used
	 */
	@DataBoundConstructor
	public ObjectStudioBuilder(String buildPath, String preloadScript,
			String loadScript, String postloadScript, String ostudioIni,
			String ostudioLog, String ostudioParameter, String ostudioImage) {
		this.buildPath = buildPath;
		this.preloadScript = preloadScript;
		this.loadScript = loadScript;
		this.postloadScript = postloadScript;
		this.ostudioIni = ostudioIni;
		this.ostudioLog = ostudioLog;
		this.ostudioParameter = ostudioParameter;
		this.ostudioImage = (ostudioImage == null || ostudioImage.trim()
				.isEmpty()) ? this.ostudioImage : ostudioImage;
	}

	/**
	 * Get ObjectStudio image filename.
	 * 
	 * @return name of ObjectStudio image file
	 */
	public String getOstudioImage() {
		return ostudioImage == null ? "ostudio.img" : ostudioImage;
	}

	/**
	 * Set ObjectStudio image filename.
	 * 
	 * @param ostudioImage
	 *            image filename or null for default image
	 */
	public void setOstudioImage(String ostudioImage) {
		this.ostudioImage = ostudioImage;
	}

	/**
	 * Get build path relative to working directory.
	 * 
	 * @return relative directory to start the build in
	 */
	public String getBuildPath() {
		return buildPath;
	}

	/**
	 * Set build path relative to working directory.
	 * 
	 * @param buildPath
	 *            null, empty string or sub directory of working directory
	 */
	public void setBuildPath(String buildPath) {
		this.buildPath = buildPath;
	}

	/**
	 * Get ObjectStudio INI filename.
	 * 
	 * @return name of INI-filename
	 */
	public String getOstudioIni() {
		return ostudioIni;
	}

	/**
	 * Set ObjectStudio INI filename. This INI file will be copied to the
	 * working directory before starting ObjectStudio
	 * 
	 * @param ostudioIni
	 *            null, empty string or relative path to the file
	 */
	public void setOstudioIni(String ostudioIni) {
		this.ostudioIni = ostudioIni;
	}

	/**
	 * Get ObjectStudio log (-o) filename.
	 * 
	 * @return null or filename
	 */
	public String getOstudioLog() {
		return ostudioLog;
	}

	/**
	 * Set ObjectStudio log (-o) filename. If filename is null or empty, no log
	 * will be written.
	 * 
	 * @param ostudioLog
	 *            null, empty string or filename.
	 */
	public void setOstudioLog(String ostudioLog) {
		this.ostudioLog = ostudioLog;
	}

	/**
	 * Set additional ObjectStudio Parameter.
	 * 
	 * @return null or string containing parameter
	 */
	public String getOstudioParameter() {
		return ostudioParameter;
	}

	/**
	 * Get additional ObjectStudio Parameter.
	 * 
	 * @param ostudioParameter
	 *            null or
	 */
	public void setOstudioParameter(String ostudioParameter) {
		this.ostudioParameter = ostudioParameter;
	}

	/**
	 * Get preload script filename.
	 * 
	 * @return preload script
	 */
	public String getPreloadScript() {
		return preloadScript;
	}

	/**
	 * Set preload script filename (-l).
	 * 
	 * @param preloadScript
	 *            null or filename
	 */
	public void setPreloadScript(String preloadScript) {
		this.preloadScript = preloadScript;
	}

	/**
	 * Get load script filename.
	 * 
	 * @return load script filename
	 */
	public String getLoadScript() {
		return loadScript;
	}

	/**
	 * Set load script filename (-A).
	 * 
	 * You can use this to load Application txt files. But because this is not a
	 * ObjectStudio commandline parameter, you will have to implement loading of
	 * this script yourself, e.g. by calling:
	 * ApplicationDefinitionStreamClass>>loadFilename: (System
	 * commandLineOptionAt: $A)
	 * 
	 * @param loadScript
	 *            load script filename
	 */
	public void setLoadScript(String loadScript) {
		this.loadScript = loadScript;
	}

	/**
	 * Get post load script.
	 * 
	 * @return post load script filename
	 */
	public String getPostloadScript() {
		return postloadScript;
	}

	/**
	 * Set post load script.
	 * 
	 * @param postloadScript
	 *            null or filename
	 */
	public void setPostloadScript(String postloadScript) {
		this.postloadScript = postloadScript;
	}

	/**
	 * Get absolute ostudio.exe filename.
	 * 
	 * @return absolute path to ostudio.exe
	 */
	public String getObjectStudioExe() {
		return getDescriptor().getPathAbsolute() + "ostudio.exe";
	}

	/**
	 * Get absolute image filename.
	 * 
	 * @return absolute path to image
	 */
	public String getObjectStudioImage() {
		if ((new File(getOstudioImage())).isAbsolute()) {
			return getOstudioImage();
		}
		return getDescriptor().getPathAbsolute() + getOstudioImage();
	}


	/**
	 * Jenkins build perform.
	 * 
	 * @param build
	 *            Jenkins build
	 * @param launcher
	 *            Jenkins launcher
	 * @param listener
	 *            Jenkins listener
	 * @throws AbortException
	 *             abort build on error
	 * @return always true
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws AbortException, InterruptedException {
		
		ObjectStudioRunner runner = new ObjectStudioRunner(this.getObjectStudio(), listener.getLogger(), build.getNumber());
		try {
			runner.run(build, launcher, listener, this);
		} finally {
			runner.cleanup(build, launcher, listener, this);
		}
		return true;
	}
	
	protected ObjectStudio getObjectStudio() {
		return new ObjectStudio7();
	}

	public Boolean getOstudioImageCopy() {
		return imageCopy;
	}

	public void setOstudioImageCopy(Boolean ostudioImageCopy) {
		this.imageCopy = ostudioImageCopy;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public ObjectStudioDescriptor getDescriptor() {
		return (ObjectStudioDescriptor) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link ObjectStudioBuilder}. Used as a singleton. The
	 * class is marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	public static class ObjectStudioDescriptor extends
			BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information, simply store it in a
		 * field and call save().
		 *
		 * <p>
		 * If you don't want fields to be persisted, use <tt>transient</tt>.
		 */
		protected String path = getDefaultPath();

		/**
		 * In order to load the persisted global configuration, you have to call
		 * load() in the constructor.
		 */
		public ObjectStudioDescriptor() {
			load();
		}

		protected String getDefaultPath() {
			return "C:\\Program Files (x86)\\ObjectStudio711\\";
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 *         <p>
		 *         Note that returning {@link FormValidation#error(String)} does
		 *         not prevent the form from being saved. It just means that a
		 *         message will be displayed to the user.
		 */
		public FormValidation doCheckPath(@QueryParameter String value)
				throws IOException, ServletException {
			File file = new File(value);
			if (!(value == null) && file.exists() && file.isDirectory()) {
				return FormValidation.ok();
			} else {
				return FormValidation.error("Invalid Directory Name");
			}
		}

		/**
		 * Indicates that this builder can be used with all kinds of project
		 * types.
		 * 
		 * @param aClass
		 *            project class
		 * @return always true
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 * 
		 * @return plugin name
		 */
		@Override
		public String getDisplayName() {
			return "ObjectStudio 7 Builder";
		}

		/**
		 * Read configuration from JSON object.
		 * 
		 * @param req
		 *            Request
		 * @param formData
		 *            Form data
		 * @return if configuration succeeded
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			req.bindJSON(this, formData.getJSONObject("objectStudio7"));
			save();
			return super.configure(req, formData);
		}

		/**
		 * Get installation path of ObjectStudio.
		 * 
		 * @return path of installation directory
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Set installation path of ObjectStudio.
		 * 
		 * @param objectStudioPath
		 *            path of installation directory
		 */
		public void setPath(String objectStudioPath) {
			this.path = objectStudioPath;
		}

		/**
		 * Get absolute installation path of ObjectStudio.
		 * 
		 * @return absolute path of installation directory
		 */
		public String getPathAbsolute() {
			return FilenameUtils.getFullPath(getPath());
		}
	}
}