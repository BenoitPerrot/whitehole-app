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

import java.io.StringWriter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.whitehole.app.model.Binary;
import org.whitehole.app.model.FutureBinary;
import org.whitehole.app.model.Json;
import org.whitehole.app.model.Project;
import org.whitehole.app.model.Repository;
import org.whitehole.app.model.Workspace;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.infra.json.JsonGenerator;

@Path("/")
public class ModelServices {
	
	@GET
	@Path("/projects")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProjects(
			@Context ServletContext context,
			@Context HttpServletResponse response) throws Exception {

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);
		
		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final StringWriter w = new StringWriter();
		try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
			Json.write(g, ws);
		}
		return w.toString();
	}
	
	@POST
	@Path("/projects/new")
	@Produces(MediaType.TEXT_PLAIN)
	public String newProject(
			@Context ServletContext context,
			@QueryParam("name") String name) throws Exception {

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);

		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = r.newProject(ws, name);
		if (p == null) throw new WebApplicationException("Project could not be created.", 500);

		final StringWriter w = new StringWriter();
		try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
			Json.write(g, p, false);
		}
		return w.toString();
	}
	
	@GET
	@Path("/projects/{projectId}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProject(
			@Context ServletContext context,
			@PathParam("projectId") String projectId,
			@Context HttpServletResponse response) throws Exception {

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);
		
		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final Project p = ws.getProjectById(UUID.fromString(projectId)).obtain();
		if (p == null) throw new WebApplicationException("No such project.", 404);

		final StringWriter w = new StringWriter();
		try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
			Json.write(g, p, true);
		}
		return w.toString();
	}

	@POST
	@Path("/projects/{projectId}/newBinary")
	@Produces(MediaType.TEXT_PLAIN)
	public String newBinary(
			@Context ServletContext context,
			@PathParam("projectId") String projectId,
			@QueryParam("name") String binaryName) throws Exception {

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);
		
		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = ws.getProjectById(UUID.fromString(projectId)).obtain();
		if (p == null) throw new WebApplicationException("Project could not be created.", 500);

		final FutureBinary b = r.newBinary(p, binaryName);
		if (b == null) throw new WebApplicationException("Binary could not be created.", 500);

		return "\"" + b.getId().toString() + "\"";
	}

	private static final Pattern contentRangePattern = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");
	
	@POST
	@Path("/projects/{projectId}/uploadResource")
	@Produces(MediaType.TEXT_PLAIN)
	public String uploadResource(
			@Context ServletContext context,
			@PathParam("projectId") String projectId,
			@QueryParam("id") String resourceId,
			@HeaderParam("Content-Range") String range,
			@Context HttpServletRequest request) throws Exception {

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);

		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = ws.getProjectById(UUID.fromString(projectId)).obtain();
		if (p == null) throw new WebApplicationException("No such project.", 404);

		final Matcher m = contentRangePattern.matcher(range);
		m.matches();
		final String start = m.group(1);
		// final String end = m.group(2);
		final String length = m.group(3);

		// <<
		r.uploadResource(r.getPath().resolve(projectId).resolve(resourceId),
				Long.parseLong(start), Long.parseLong(length), request.getInputStream());
		// >>
		
		return "";
	}

	@GET
	@Path("/projects/{projectId}/controlFlowGraph")
	@Produces(MediaType.TEXT_PLAIN)
	public String getControlFlowGraph(
			@Context ServletContext context,
			@PathParam("projectId") String projectId,
			@QueryParam("entryPoint") String entryPoint,
			@Context HttpServletResponse response) throws Exception {

		if (entryPoint == null) throw new WebApplicationException("An entryPoint is required.", 400);

		final Repository r = (Repository) context.getAttribute("repository");
		if (r == null) throw new WebApplicationException("No repository.", 500);
		
		final Workspace ws = r.loadWorkspace();
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final Project p = ws.getProjectById(UUID.fromString(projectId)).obtain();
		if (p == null) throw new WebApplicationException("No such project.", 404);
		
		final Binary b = p.getBinaries().values().iterator().next().obtain();
		if (b == null) throw new WebApplicationException("No such binary.", 404);
		
		final StringWriter w = new StringWriter();
		final ControlFlowGraph cfg = b.extractControlFlowGraph(entryPoint.startsWith("0x") ? Long.parseLong(entryPoint.substring(2), 16) : Long.parseLong(entryPoint));
		if (cfg != null) {
			try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
				Json.write(g, cfg);
			}
		}
		return w.toString();
	}

}
