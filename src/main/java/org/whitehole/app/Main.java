package org.whitehole.app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.whitehole.app.model.ProjectRepository;

public class Main {

	static class Arguments {

		public int port = 80;
		
		public Arguments(String[] args) {
			for (int a = 0; a < args.length; ++a) {
				final String opt = args[a];
				
				switch (opt) {
					case "--port":
						++a;
						if (a < args.length)
							port = Integer.parseInt(args[a]);
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
