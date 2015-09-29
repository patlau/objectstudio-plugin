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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;

import org.apache.commons.io.FilenameUtils;

/**
 * Runs the ObjectStudio process and reads the logfile.
 * @author patricklauper
 *
 */
public class ObjectStudioRunner {
	PrintStream logger;
	FilePath workdir;
	FilePath tempdir;
	HashMap<String, String> envs = new HashMap<String, String>();
	int buildNr;
	ObjectStudio objectStudio;
	
	FilePath image;
	FilePath preloadScript;
	
	/**
	 * Create a new instance with a logger.
	 * @param logger for logging
	 * @param buildNr for temp files
	 */
	public ObjectStudioRunner(ObjectStudio os, PrintStream logger, int buildNr) {
		this.logger = logger;
		this.objectStudio = os;
	}
	
	private void printBuildInfo(ObjectStudioBuilder builder) {
        try {
			logger.println("[ObjectStudio] - Build Path: " + workdir.absolutize().getRemote());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
        logger.println("[ObjectStudio] - Preload Script: " + builder.getPreloadScript());
        logger.println("[ObjectStudio] - Autoload Script: " + builder.getLoadScript());
        logger.println("[ObjectStudio] - AfterLogon Script: " + builder.getPostloadScript());
        logger.println("[ObjectStudio] - OStudio Exe: " + builder.getObjectStudioExe());
        logger.println("[ObjectStudio] - OStudio Image: " + builder.getObjectStudioImage());
        logger.println("[ObjectStudio] - OStudio Ini: " + builder.getOstudioIni());
        logger.println("[ObjectStudio] - OStudio Log: " + builder.getOstudioLog());
        logger.println("[ObjectStudio] - OStudio Parameter: " + builder.getOstudioParameter());
	}
	
	private void printEnvInfo(EnvVars envVars) {
		logger.println("[ObjectStudio] - Environment Variables");
        for (Map.Entry<String, String> e : envVars.entrySet()) {
        	logger.println("   " + e.getKey() + "=" + e.getValue());
        }
	}

	/**
	 * Run ObjectStudio with Jenkins build instance.
	 * @param build Jenkins build
	 * @param launcher Jenkins launcher
	 * @param listener Jenkins listener
	 * @param builder ObjectStudioBuilder
	 * @throws AbortException abort build on failure
	 * @throws InterruptedException abort build on interrupt
	 */
	public void run(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, ObjectStudioBuilder builder) throws AbortException, InterruptedException {

        logger.println("[ObjectStudio] - Get Workdir");
        // Get absolute workspace directory from Jenkins build
        workdir = build.getWorkspace();

        initTempDirectory();
        initNetworkDrives();
        
        // Change working directory to sub directory if required
        if (builder.getBuildPath() != null && !builder.getBuildPath().isEmpty()) {
        	workdir = workdir.child(builder.getBuildPath());
        }

        logger.println("[ObjectStudio] - Get Environment");
        EnvVars envVars = getEnv(build, listener);
        
        // Get absolute ObjectStudio log file in working directory
        logger.println("[ObjectStudio] - Get Logfile");
        FilePath log = getAbsoluteWorkspacePath(builder.getOstudioLog());

        initLogfile(log);
        
        initPreloadScript(builder);
        initPostloadScript(builder);
        
        initOstudioIni(builder);
        initOstudioImage(builder);
        
        printBuildInfo(builder);
        printEnvInfo(envVars);
        
        logger.println("[ObjectStudio] - Get Commandline");
        ArgumentListBuilder command = objectStudio.getCommandline(build, builder, this);
        
        logger.println("[ObjectStudio] - Starting: " + command);
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        ProcStarter procStarter = launcher.decorateByEnv(envVars).launch()
        		.pwd(workdir)
        		.envs(envs)
        		.cmds(command)
        		.stderr(errorStream)
        		.stdout(listener);
        
        Proc proc = null;		
        try {
	        proc = procStarter.start();
		} catch (IOException e) {
			e.printStackTrace();
            throw new AbortException("Error starting ObjectStudio: " + e.getMessage());
		}

        logger.println("[ObjectStudio] - Reading Log: " + log.getRemote());
		try {
			waitForProcess(log, proc);
			readLog(log, proc);
		} catch (IOException e) {
			e.printStackTrace();
            throw new AbortException("Error waiting ObjectStudio: " + e.getMessage());
		}

		logger.println("[ObjectStudio] - Joining");
        int rc;
		try {
			rc = proc.join();
	        if (rc != 0) {
	        	logger.println("Error running command: " + errorStream.toString());
	            throw new AbortException(errorStream.toString());
	        }   
		} catch (IOException e) {
			e.printStackTrace();
            throw new AbortException("Error joining ObjectStudio: " + e.getMessage());
		} catch (InterruptedException e) {
			try {
				proc.kill();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            throw new InterruptedException();
		}	
    	
	}

	private void readLog(FilePath log, Proc proc) throws InterruptedException {
		
		BufferedReader reader = null;
		int lastLine = 0;
		
		try {
	        String s = null;
	        while (proc.isAlive()) {
	        	
	        	// Seems we need to open it again when running on remote node.
	        	// Which is really bad, should rewrite this to run on remote node.
	        	reader = new BufferedReader(new InputStreamReader(log.read()));
		        int line = 1;
				int timeout = 0;
		        
		        while (proc.isAlive() && timeout < 10) {
			        
					s = reader.readLine();
			        while (s != null) {
			        	if (line > lastLine) {
			        		logger.println(s);
			        		timeout = 0;
			        	}
			        	line++;
			        	s = reader.readLine();
			        }
			        
			        // Sleep here, so we do not create too much CPU overhead while ObjectStudio is working
			        if (proc.isAlive()) {
						Thread.sleep(1000);
						timeout++;
			        }
		        }
		        
		        reader.close();
		        lastLine = line - 1;
	        }
		} catch (IOException e) {
	        logger.println("Failed reading log " + log.getRemote());
			e.printStackTrace();
		} catch (InterruptedException e) {
			try {
				proc.kill();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (reader != null) {
				try { 
					reader.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
			throw new InterruptedException();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	private void waitForProcess(FilePath log, Proc proc)
			throws InterruptedException, IOException {
		// Wait at least 10 seconds
		for (int i = 0; i < 10; i++) {
			if (!log.exists()) {
				try {
			        logger.println("    Waiting for ObjectStudio...");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					try {
						proc.kill();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					throw new InterruptedException();
				}
			}
		}
	}

	private void initOstudioImage(ObjectStudioBuilder builder)
			throws AbortException {
        logger.println("[ObjectStudio] - Create Image File for " + builder.getObjectStudioImage());
        FilePath src = getAbsoluteWorkspacePath(builder.getObjectStudioImage());
        
        if (!builder.getOstudioImageCopy()) {
        	this.image = src;
        	return;
        }
        
        FilePath dst = getTempFilename(builder.getObjectStudioImage());
        try {
			copyTo(src, dst.absolutize());
		} catch (IOException e) {
            throw new AbortException("Can not create temp file: " + e.getMessage());
		} catch (InterruptedException e) {
            throw new AbortException("Can not create temp file: " + e.getMessage());
		}
        
        this.image = dst;
	}

	private void initOstudioIni(ObjectStudioBuilder builder)
			throws AbortException {
        logger.println("[ObjectStudio] - Create ostudio.ini from " + builder.getOstudioIni());
        FilePath src = getAbsoluteWorkspacePath(builder.getOstudioIni());
        FilePath dst = getAbsoluteWorkspacePath("ostudio.ini");
        try {
			copyTo(src, dst.absolutize());
		} catch (IOException e) {
            throw new AbortException("Can not create temp file: " + e.getMessage());
		} catch (InterruptedException e) {
            throw new AbortException("Can not create temp file: " + e.getMessage());
		}
	}

	private void initPostloadScript(ObjectStudioBuilder builder) {
        logger.println("[ObjectStudio] - Setting PostLoad Script to " + builder.getPostloadScript());
        envs.put("AFTERLOGONSCRIPT", builder.getPostloadScript());
        //logger.println("[ObjectStudio] - AFTERLOGONSCRIPT: " + envs.get("AFTERLOGONSCRIPT"));
	}

	/**
	 * Preload-File muss Absolut und ohne Blanks sein.
	 * @param builder Jenkins builder
	 * @throws AbortException abort build on error
	 */
	private void initPreloadScript(ObjectStudioBuilder builder)
			throws AbortException {
		// 
        if (builder.getPreloadScript() == null || builder.getPreloadScript().trim().isEmpty()) {
        	logger.println("[ObjectStudio] - Skipping Preload Script");
        } else {
        	logger.println("[ObjectStudio] - Create Preload File for " + builder.getPreloadScript());
            FilePath src = getAbsoluteWorkspacePath(builder.getPreloadScript());
            FilePath dst = getTempFilename(builder.getPreloadScript());
            try {
    			copyTo(src, dst.absolutize());
    		} catch (IOException e) {
                throw new AbortException("Can not create temp file: " + e.getMessage());
    		} catch (InterruptedException e) {
                throw new AbortException("Can not create temp file: " + e.getMessage());
    		}
            this.preloadScript = dst;
       }

	}

	/**
	 * Delete log file if it exists.
	 * @param log log filename
	 */
	private void initLogfile(FilePath log) {
        try {
			if (log.exists()) {
				logger.println("[ObjectStudio] - Delete Log: " + log.getRemote());
				try {
					log.delete();
				} catch (IOException e) {
					logger.println("Can not delete log: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Init some network drives.
	 * Should be in Jenkins or VSS.
	 */
	private void initNetworkDrives() {
        //WinUtils.checkForUnmappedNetworkDrive(new File("J:\\"));
	}

	/**
	 * TEMP fuer Logging auf Workspace\Temp setzen.
	 * @param workdir
	 * @param envs
	 * @throws AbortException
	 */
	private void initTempDirectory()
			throws AbortException {

        try {
    		this.tempdir = getAbsoluteWorkspacePath("TEMP").absolutize();
            logger.println("[ObjectStudio] - Setting TEMP directory: " + this.tempdir.getRemote());
            envs.put("TEMP", this.tempdir.getRemote());
            envs.put("TMP", this.tempdir.getRemote());
            
	        if (this.tempdir.exists()) {
	        	this.tempdir.deleteContents();
	        }
	        this.tempdir.mkdirs();
		} catch (IOException e) {
            throw new AbortException("Can not create directory TEMP: " + e.getMessage());
		} catch (InterruptedException e) {
            throw new AbortException("Can not create directory TEMP: " + e.getMessage());
		}
	}

	private EnvVars getEnv(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException, AbortException {
		EnvVars envVars = new EnvVars();
		try {
	        envVars = build.getEnvironment(listener);
		} catch (IOException e) {
			e.printStackTrace();
            throw new AbortException("Can not get environment variables");
		}
		return envVars;
	}
	
	/**
	 * Get Absolute Filename for Files which could be in Workspace.
	 * Special method because Workspace could be different than Current directory
	 * @param workdir Jenkins workspace directory
	 * @param filename Absolute or relative filename
	 * @return absolute filename 
	 */
	private FilePath getAbsoluteWorkspacePath(String filename) {
		try {
			FilePath f = this.workdir.child(filename).absolutize();
			//logger.println("[ObjectStudio] File: " + this.workdir.getRemote() + " => " + filename + " => " + f.getRemote());
			return f;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Cleanup copied files.
	 * 
	 * @param build
	 *            Jenkins build
	 * @param launcher
	 *            Jenkins launcer
	 * @param listener
	 *            Jenkins listener
	 * @param builder 
	 * @throws AbortException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	protected void cleanup(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, ObjectStudioBuilder builder) throws AbortException {

		// Temp directory will be cleaned when running and already exists
		
	}
	

	/**
	 * Get a temp filename without blanks.
	 * 
	 * @param filename
	 *            filename
	 * @return temp file
	 */
	public FilePath getTempFilename(String filename) {
		return this.tempdir.child((Integer.toString(buildNr) + FilenameUtils.getName(filename)).replaceAll(" ", "_"));
	}
	
	
	/**
	 * - * Copies a file to destination directory and log to Jenkins build. -
	 * * @param src source filename - * @param dest destination filename
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void copyTo(FilePath src, FilePath dest) throws IOException, InterruptedException {
		logger.println("[ObjectStudio] - Copy File: " + src.getRemote() + " to " + dest.getRemote());
		if (dest.getRemote().equals(src.getRemote())) {
			logger.println("[ObjectStudio] - Source and Dest File is equal: " + dest.getRemote());
			return;
		}
		if (dest.exists()) {
			logger.println("[ObjectStudio] - Dest File exists, overwrite: " + dest.getRemote());
			//dest.delete();
		}
		src.copyTo(dest);
	}

	public Object getObjectStudioImageName() {
		return this.image == null ? null : this.image.getRemote();
	}

	public String getPreloadScriptName() {
		return this.preloadScript == null ? null : this.preloadScript.getRemote();
	}

}