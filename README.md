Jenkins ObjectStudio Plugin
===========================

Provides Jenkins integration with [Cincom ObjectStudio](http://cincomsmalltalk.com/) Smalltalk. 

Runs ObjectStudio (Version 7 or 8) and reads the Transcript log file and displays it in Jenkins console output. 

Configuration of the build step includes currently some specific parameters for my own build process, but it should be possible to use it with only the generic configuration parameters. I will remove this parameters later and replace them with a more generic approach. These specific configuration parameters are:
- Load script: Passed as -A commandline parameter
- Postload script: Passed as AFTERLOGONSCRIPT environment variable

Some files are copied to a temporary file before running ObjectStudio, because ObjectStudio may change these files:
- OStudio Ini file
- OStudio Image file

Cincom and ObjectStudio are registered trademarks of Cincom Systems Inc.


License
-------

	(The MIT License)

	Copyright (c) 2015 Patrick Lauper

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	'Software'), to deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be
	included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
	EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
