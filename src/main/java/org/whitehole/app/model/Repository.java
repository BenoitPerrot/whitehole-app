package org.whitehole.app.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
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
	
	public static JsonGenerator write(JsonGenerator g, Project p) {
		g
		.writeStartObject()
		.  write("id", p.getId().toString())
		.  write("name", p.getName())
		.  writeStartArray("binaries");
		p.getBinaries().forEach((id, binary) -> {
			g
			.writeStartObject()
			.  write("id", id.toString())
			.  write("name", binary.getName())
			.writeEnd();
		});
		g
		.  writeEnd()
		.writeEnd();
		return g;
	}
	
	public void save(Project p) throws JsonException, IOException {
		try (final JsonGenerator g = new JsonGenerator.Writer(new FileWriter(_path.resolve(p.getId().toString()).resolve("description.json").toFile()))) {
			write(g, p);
		}
	}

	public Project loadProject(UUID id) throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(_path.resolve(id.toString()).resolve("description.json").toFile()))) {
			final JsonObject o = r.readObject();
			// assert (id == UUID.fromString(o.getString("id").toString()));
			final String name = o.getString("name").toString();
			final Project p = new Project(id, name);
			
			for (final JsonValue x : o.getArray("binaries")) {
				final JsonObject bin = (JsonObject) x;
				
				final UUID binaryId = UUID.fromString(bin.getString("id").toString());
				final String binaryName = bin.getString("name").toString();
				final Binary b = new Binary(binaryId, binaryName);
				
				p.addBinary(b);
			}
			
			return p;
		}
	}
	
	public Project newProject(Workspace ws, String name) throws IOException {
		final Project p = new Project(UUID.randomUUID(), name);
		ws.addProject(new FutureProject(this, p));

		Files.createDirectory(getPath().resolve(p.getId().toString()));
		save(ws);

		return p;
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
			workspace.getProjects().forEach(p -> {
				g.write(p.getId().toString());
			});
			g.writeEnd();
			g.writeEnd();
		}
		return workspace;
	}

	private Workspace _workspace;

	public Workspace loadWorkspace() throws Exception {
		if (_workspace == null)
			try (final JsonReader r = new JsonReader(new FileReader(_path.resolve("index.json").toFile()))) {
				final JsonObject o = r.readObject();

				_workspace = new Workspace();

				for (final JsonValue v : o.getArray("projects")) {
					final JsonObject p = (JsonObject) v;
					final UUID id = UUID.fromString(p.get("id").toString());
					final String name = p.get("name").toString();
					_workspace.addProject(new FutureProject(this, id, name));
				}
			}
		return _workspace;
	}
}
