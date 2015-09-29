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

import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

/**
 * ObjectStudio 8 specific methods.
 * 
 * @author zhlpc
 *
 */
public class ObjectStudio8 implements ObjectStudio {
	
	/**
     * Build the ObjectStudio8 commandline.
	 * @param build Jenkins build instance
	 * @param builder ObjectStudio Builder
	 * @param runner ObjectStudio Runner
     * @return Instance of ArgumentListBuilder
     */
    @Override
	public ArgumentListBuilder getCommandline(AbstractBuild build, Builder builder, ObjectStudioRunner runner) {
		
    	ObjectStudio8Builder config = (ObjectStudio8Builder) builder;
    	
    	ArgumentListBuilder argList = new ArgumentListBuilder();
		
		argList.addQuoted("C:\\Windows\\System32\\CMD.EXE");
		argList.add("/Q");
		argList.add("/C");
		
		argList.add("start");
		argList.addQuoted("ObjectStudio8");
		argList.add("/WAIT");
		argList.add(config.getObjectStudioExe());
		
		if (config.getReportMemoryUsage()) {
			argList.add("-xq");
		}
		for (String param : config.getVisualWorksParameter().split(" ")) {
			argList.add(param);
		}
		
		argList.add(runner.getObjectStudioImageName());
		
		StringBuilder osCmdline = new StringBuilder();
		if (runner.getPreloadScriptName() != null) {
			osCmdline.append(" -l" + runner.getPreloadScriptName());
		}
		if (config.getLoadScript() != null && !config.getLoadScript().trim().isEmpty()) {
			osCmdline.append(" -A" + config.getLoadScript());
		}
		if (config.getOstudioLog() != null && !config.getOstudioLog().trim().isEmpty()) {
			osCmdline.append(" -o'" + config.getOstudioLog() + "'");
		}
		if (config.getOstudioParameter() != null && !config.getOstudioParameter().trim().isEmpty()) {
			osCmdline.append(" " + config.getOstudioParameter());
		}
		argList.add("-ostudio");
		argList.add(osCmdline.toString());
		
		return argList;
	}
}
