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
