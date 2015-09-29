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
 * ObjectStudio 7 specific methods.
 * 
 * @author zhlpc
 *
 */
public class ObjectStudio7 implements ObjectStudio {

	/**
	 * Create ObjectStudio 7 Commandline.
	 * 
	 * @param build Jenkins build instance
	 * @param builder ObjectStudio Builder
	 * @param runner ObjectStudio Runner
	 * @return ArgumentListBuilder
	 */
	@Override
	public ArgumentListBuilder getCommandline(
			AbstractBuild<?, ?> build, Builder builder, ObjectStudioRunner runner) {
		
		ObjectStudioBuilder config = (ObjectStudioBuilder) builder;
		
		ArgumentListBuilder argList = new ArgumentListBuilder();
	
		argList.addQuoted("C:\\Windows\\System32\\CMD.EXE");
		argList.add("/Q");
		argList.add("/C");
	
		argList.add("start");
		argList.addQuoted("ObjectStudio");
		argList.add("/WAIT");
		argList.add(config.getObjectStudioExe());
		argList.add("-i"
				+ runner.getObjectStudioImageName()
				+ "");

		if (runner.getPreloadScriptName() != null) {
			argList.add("-l"
					+ runner.getPreloadScriptName());
		}
		argList.add("-A" + config.getLoadScript());
		argList.add("-o" + config.getOstudioLog());
		for (String param : config.getOstudioParameter().split(" ")) {
			argList.add(param);
		}
	
		// -i"%OBJECTSTUDIO7%ostudio.img" -l%_PRELOAD% -A%_AUTOLOAD% -oOS7.log
		// -E50 -x2 -cSRV:DB:%VE2000_DB_TEST%
		return argList;
	}
	
}
