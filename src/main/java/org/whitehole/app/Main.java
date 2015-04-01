// Copyright (c) 2014-2015, Benoit PERROT.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//
//     * Neither the name of the White Hole Project nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package org.whitehole.app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.whitehole.app.model.ProjectRepository;

public class Main {

	static class Arguments {

		public int port = 80;
		public String binaryPath = null;
		
		public Arguments(String[] args) {
			for (int a = 0; a < args.length; ++a) {
				final String opt = args[a];
				
				switch (opt) {
					case "--port":
						++a;
						if (a < args.length)
							port = Integer.parseInt(args[a]);
						break;
					case "--binary-path":
						++a;
						if (a < args.length)
							binaryPath = args[a];
						break;
				}
			}
		}
	}

	public static void main(String[] l) throws Exception {
		final Arguments args = new Arguments(l);
		
		final Server server = new Server(args.port);

		final WebAppContext wac = new WebAppContext();
		final String webappDirLocation = "src/main/webapp/";
		wac.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
		wac.setResourceBase(webappDirLocation);
		wac.setContextPath("/");
		wac.setParentLoaderPriority(true);

		final ProjectRepository r = new ProjectRepository();
		
		if (args.binaryPath != null)
			r.newProject(args.binaryPath);
		
		wac.setAttribute("repository", r);

		server.setHandler(wac);

		// Start
        try {
            server.start();
            server.join();
        } finally {
            server.destroy();
        }
	}
}
