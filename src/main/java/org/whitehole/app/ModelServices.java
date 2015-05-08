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
import java.nio.file.Files;
import java.util.Map.Entry;
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
import org.whitehole.app.model.Project;
import org.whitehole.app.model.Repository;
import org.whitehole.app.model.Workspace;
import org.whitehole.apps.objdump.assembly.ia32_x64.IntelStringWriter;
import org.whitehole.assembly.ia32_x64.control.BasicBlock;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph.MotivatedBlock;
import org.whitehole.assembly.ia32_x64.dom.Immediate;
import org.whitehole.assembly.ia32_x64.dom.Instruction;
import org.whitehole.assembly.ia32_x64.dom.Mnemonic;
import org.whitehole.assembly.ia32_x64.dom.Operand;
import org.whitehole.infra.json.JsonException;
import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonString;
import org.whitehole.infra.json.JsonWriter;

@Path("/")
public class ModelServices {
	
	@GET
	@Path("/projects")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProjects(
			@Context ServletContext context,
			@Context HttpServletResponse response) throws IOException {

		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final StringWriter w = new StringWriter();
		try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
			g.writeStartArray();
			ws.getProjects().forEach(e -> {
				Repository.write(g, e.getValue());
			});
			g.writeEnd();
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

		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = ws.newProject(name);
		if (p == null) throw new WebApplicationException("Project could not be created.", 500);

		// <<
		Files.createDirectory(r.getPath().resolve(p.getId()));
		r.save(ws);
		// >>

		final StringWriter w = new StringWriter();
		try (final JsonGenerator.Writer g = new JsonGenerator.Writer(w)) {
			Repository.write(g, p);
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
		
		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final Project p = ws.getProjectById(projectId);
		if (p == null) throw new WebApplicationException("No such project.", 404);

		final StringWriter w = new StringWriter();
		final JsonWriter jw = new JsonWriter(w);
		
		final java.nio.file.Path path = r.getPath().resolve(p.getId());
		JsonObject x = new JsonObject();
		x.put("id", new JsonString(p.getId()));
		x.put("name", new JsonString(p.getName()));

		final Binary first = p.getBinaries().values().iterator().next();
		x.put("binaryId", new JsonString(first.getId().toString()));
		first.load(path.resolve(first.getId().toString()));
		first.toJson(x);

		jw.writeObject(x);
		jw.close();
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
		
		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = ws.getProjectById(projectId);
		if (p == null) throw new WebApplicationException("Binary could not be created.", 500);

		final Binary b = p.newBinary(UUID.randomUUID(), binaryName);
		final String binaryId = "\"" + b.getId().toString() + "\"";
		
		// <<
		r.save(p);
		// >>
		
		return binaryId;
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

		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);
		
		final Project p = ws.getProjectById(projectId);
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
		
		final Workspace ws = (Workspace) context.getAttribute("workspace");
		if (ws == null) throw new WebApplicationException("No workspace.", 500);

		final Project p = ws.getProjectById(projectId);
		if (p == null) throw new WebApplicationException("No such project.", 404);
		
		final Binary b = p.getBinaries().values().iterator().next();
		if (b == null) throw new WebApplicationException("No such binary.", 404);
		
		final StringWriter w = new StringWriter();

		b.load(r.getPath().resolve(p.getId()).resolve(b.getId().toString()));
		final ControlFlowGraph cfg = b.extractControlFlowGraph(entryPoint.startsWith("0x") ? Long.parseLong(entryPoint.substring(2), 16) : Long.parseLong(entryPoint));
		if (cfg != null) {
			final JsonGenerator.Builder g = new JsonGenerator.Builder();
			write(g, cfg);

			final JsonWriter jw = new JsonWriter(w);
			jw.write(g.get());
			jw.close();
		}
		return w.toString();
	}
	
	static void write(JsonGenerator g, Instruction i, long p) throws JsonException {
		g.writeStartObject();

		boolean written = false;
		if (i.getMnemonic() == Mnemonic.CALL) {
			if (i.getOperands().length == 1) {
				final Operand o = i.getOperands()[0];
				if (o instanceof Immediate) {
					final Immediate imm = (Immediate) o;
					final long addr = 5 /* i.getByteLength() */+ imm.getSignedInteger().intValue() /* FIXME: sign-extended long */+ p;
					g.write("m", "call");
					g.writeStartObject("o");
//					final String name = null; // FIXME: find routine name by address
//					if (name != null)
//						g.write("name", name);
					g.write("rva", "0x" + Long.toHexString(addr));
					g.writeEnd();
					written = true;
				}
//				else if (o instanceof SIBDAddress) {
//					final SIBDAddress a = (SIBDAddress) o;
//					if (a.getBase() == null && a.getIndex() == null && a.getDisplacement() != null) {
//						final long addr = a.getDisplacement().longValue();
//						final String name = null; // FIXME: find routine name by address
//						if (name != null) {
//							g.write("m", "call");
//							g.writeStartArray("o");
//							g.write("dll@" + name);
//							g.writeEnd();
//							written = true;
//						}
//					}
//				}
			}
		}
		
		if (!written) {
			g.write("m", IntelStringWriter.toString(i.getMnemonic()));
			if (0 < i.getOperands().length) {
				g.writeStartArray("o");
				for (Operand o : i.getOperands()) {
					final StringWriter sw = new StringWriter();
					IntelStringWriter.OperandWriter ow = new IntelStringWriter.OperandWriter(sw, i.getMnemonic());
					o.accept(ow);
					g.write(sw.toString());
				}
				g.writeEnd();
			}
		}

		g.writeEnd();
	}

	static JsonGenerator write(JsonGenerator g, ControlFlowGraph cfg) throws JsonException {

		g.writeStartObject();

		g.writeStartObject("basicBlocks");
		for (final BasicBlock bb : cfg.getBasicBlocks()) {

			g.writeStartObject("0x" + Long.toHexString(bb.getEntryPoint()));

			g.writeStartArray("instructions");
			for (final Entry<Long, Instruction> e : bb)
				write(g, e.getValue(), e.getKey());
			g.writeEnd();

			if (cfg.hasOutcomingBlocks(bb)) {
				g.writeStartArray("exits");
				for (final MotivatedBlock m : cfg.getDestinationBlocks(bb)) {
					final BasicBlock obb = m.getBlock();
					g.writeStartObject();
					g.write("point", "0x" + Long.toHexString(obb.getEntryPoint()));
					g.write("reason", m.getReason().toString());
					g.writeEnd();
				}
				g.writeEnd();
			}

			g.writeEnd();
		}
		g.writeEnd();

		g.writeEnd();
		
		return g;
	}

}
