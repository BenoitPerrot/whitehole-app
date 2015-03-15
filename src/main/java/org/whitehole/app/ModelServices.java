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

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.whitehole.app.model.Project;
import org.whitehole.app.model.ProjectRepository;
import org.whitehole.infra.json.JsonWriter;
//import org.whitehole.webapp.model.Project;
//import org.whitehole.webapp.model.ProjectRepository;

@Path("/")
public class ModelServices {
	
	@GET
	@Path("/projects")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProjectsBriefs(
			@Context ServletContext context,
			@Context HttpServletResponse response) throws IOException {

		final ProjectRepository r = (ProjectRepository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No project repository.", 500);

		final StringWriter w = new StringWriter();
		final JsonWriter jw = new JsonWriter(w);
		jw.write(r.getProjectBriefs());
		jw.close();
		return w.toString();
	}

	@GET
	@Path("/projects/{projectId}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProjectStructure(
			@Context ServletContext context,
			@PathParam("projectId") String projectId,
			@Context HttpServletResponse response) throws Exception {
		
		final ProjectRepository r = (ProjectRepository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No project repository.", 500);

		final Project p = r.getProjectById(projectId);
		if (p == null) throw new WebApplicationException("No such project.", 404);

		final StringWriter w = new StringWriter();
		final JsonWriter jw = new JsonWriter(w);
		jw.writeObject(p.toJson());
		jw.close();
		return w.toString();
	}

}
