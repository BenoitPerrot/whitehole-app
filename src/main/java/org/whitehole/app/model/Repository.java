package org.whitehole.app.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

import org.whitehole.infra.json.JsonException;
import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonReader;
import org.whitehole.infra.json.JsonValue;


public class Repository {

	public static void save(Path path, Project p) throws JsonException, IOException {
		try (final JsonGenerator g = new JsonGenerator.Writer(new FileWriter(path.resolve("description.json").toFile()))) {
			g.writeStartObject()
			.  write("id", p.getId())
			.  write("name", p.getName())
			.  writeStartObject("binary")
			.    write("id", p.getBinaryId().toString())
			.    write("name", p.getBinaryName())
			.  writeEnd()
			.writeEnd();
		}
	}

	public static Project loadProject(Path path) throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(path.resolve("description.json").toFile()))) {
			final JsonObject o = r.readObject();
			final String id = o.getString("id").toString();
			final String name = o.getString("name").toString();
			
			final JsonObject bin = o.getObject("binary");
			final UUID binaryId = UUID.fromString(bin.getString("id").toString());
			final String binaryName = bin.getString("name").toString();
			
			return new Project(path, id, name)
				.setBinaryId(binaryId)
				.setBinaryName(binaryName);
		}
	}
	
	public static Workspace save(Path path, Workspace workspace) throws JsonException, IOException {
		try (final JsonGenerator g = new JsonGenerator.Writer(new FileWriter(path.resolve("index.json").toFile()))) {
			g.writeStartObject();
			g.writeStartArray("projects");
			workspace.getProjects().forEach(e -> {
				g.write(e.getKey());
			});
			g.writeEnd();
			g.writeEnd();
		}
		return workspace;
	}

	public static Workspace loadWorkspace(Path path) throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(path.resolve("index.json").toFile()))) {
			final JsonObject o = r.readObject();
			
			final HashMap<String, Project> nameToProject = new HashMap<>();
			for (final JsonValue id : o.getArray("projects"))
				nameToProject.put(id.toString(), loadProject(path.resolve(id.toString())));
			
			return new Workspace(path, nameToProject);
		}
	}
}
