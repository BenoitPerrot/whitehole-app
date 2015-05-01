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
package org.whitehole.app.model;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import org.whitehole.infra.json.JsonArray;
import org.whitehole.infra.json.JsonArrayBuilder;
import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonObject;

public class ProjectRepository {
	
	private final HashMap<String, Project> _projects = new HashMap<>();
	
	public Stream<Entry<String, Project>> getProjects() {
		return _projects.entrySet().stream();
	}
	
	public Project newProject(String name) {
		final String id = UUID.randomUUID().toString();
		final Project p = new Project(id, name);
		_projects.put(id, p);
		return p;
	}
	
	public JsonArray getProjectBriefs() {
		final JsonArrayBuilder a = new JsonArrayBuilder();
		_projects.forEach((id, p) -> {
			a.add(p.toBriefJson());
		});
		return a.build();
	}
	
	public Project getProjectById(String id) {
		return _projects.get(id);
	}
	
	public static JsonGenerator write(JsonGenerator g, ProjectRepository r) {
		g.writeStartObject();
		g.writeStartArray("projects");
		r.getProjects().forEach(e -> {
			Project.write(g, e.getValue());
		});
		g.writeEnd();
		g.writeEnd();
		return g;
	}
	
	public static ProjectRepository fromJson(JsonObject pseudoProjectRepository) throws Exception {
		final ProjectRepository pr = new ProjectRepository();
		pseudoProjectRepository.getArray("projects").forEach(pseudoProject -> {
			final Project p = Project.fromJson((JsonObject) pseudoProject);
			pr._projects.put(p.getId(), p);
		});
		return pr;
	}
}