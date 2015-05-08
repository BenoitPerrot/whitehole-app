package org.whitehole.app.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

import org.whitehole.infra.json.JsonException;
import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonReader;
import org.whitehole.infra.json.JsonValue;


public class Repository {

	private final Path _path;
	
	public Path getPath() {
		return _path;
	}
	
	public Repository(Path path) {
		_path = path;
	}
	
	public void save(Project p) throws JsonException, IOException {
		try (final JsonGenerator g = new JsonGenerator.Writer(new FileWriter(_path.resolve(p.getId()).resolve("description.json").toFile()))) {
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

	public Project loadProject(Path path) throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(path.resolve("description.json").toFile()))) {
			final JsonObject o = r.readObject();
			final String id = o.getString("id").toString();
			final String name = o.getString("name").toString();
			
			final JsonObject bin = o.getObject("binary");
			final UUID binaryId = UUID.fromString(bin.getString("id").toString());
			final String binaryName = bin.getString("name").toString();
			
			return new Project(id, name)
				.setBinaryId(binaryId)
				.setBinaryName(binaryName);
		}
	}
	
	public void uploadResource(Path path, long offset, Long totalLength, InputStream input) throws IOException {

		// Dump content at specified range
		try (final RandomAccessFile output = new RandomAccessFile(path.toFile(), "rw")) {

			// if (totalLength != null)
			//     // Reserve area
			//     output.setLength(totalLength);

			// Dump stream at specified offset
			{
				final byte[] buffer = new byte[8192];
				int n;
				while ((n = input.read(buffer)) != -1) {
					output.seek(offset);
					output.write(buffer, 0, n);
					offset += n;
				}
			}

			// <<
			System.err.println("TODO: check range, remove sleep()");
			try {
				Thread.sleep(1000);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			// >>
		}
		
		// Check end
		if (totalLength == null || offset == totalLength) {// FIXME: check for file upload completion more correctly
		}
	}
	
	public Workspace save(Workspace workspace) throws JsonException, IOException {
		try (final JsonGenerator g = new JsonGenerator.Writer(new FileWriter(_path.resolve("index.json").toFile()))) {
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

	public Workspace loadWorkspace() throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(_path.resolve("index.json").toFile()))) {
			final JsonObject o = r.readObject();
			
			final HashMap<String, Project> nameToProject = new HashMap<>();
			for (final JsonValue id : o.getArray("projects"))
				nameToProject.put(id.toString(), loadProject(_path.resolve(id.toString())));
			
			return new Workspace(nameToProject);
		}
	}
}
