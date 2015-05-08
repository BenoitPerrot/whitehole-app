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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import org.whitehole.infra.json.JsonArray;
import org.whitehole.infra.json.JsonArrayBuilder;

public class Workspace {
	
	private final Path _path;
	
	private final HashMap<String, Project> _projects;
	
	public Stream<Entry<String, Project>> getProjects() {
		return _projects.entrySet().stream();
	}
	
	public Workspace(Path path, HashMap<String, Project> projects) throws Exception {
		_path = path;
		_projects = projects;
	}
	
	//
	//
	//
	
	public Project newProject(String name) throws IOException {

		final String id = UUID.randomUUID().toString();
		
		final Path path = _path.resolve(id.toString());
		Files.createDirectory(path);
		
		final Project p = new Project(path, id, name);
		_projects.put(id, p);

		// Save index <<
		Repository.save(_path, this);
		// >>
		
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
	
}