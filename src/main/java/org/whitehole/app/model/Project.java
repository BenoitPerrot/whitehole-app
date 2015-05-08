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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonObjectBuilder;

public class Project {

	private final String _id;
	
	public String getId() {
		return _id;
	}
	
	private final String _name;
	
	public String getName() {
		return _name;
	}
	
	private HashMap<UUID, Binary> _binaries;
	
	public HashMap<UUID, Binary> getBinaries() {
		return _binaries;
	}
	
	public Project(String id, String name) {
		this(id, name, new HashMap<>());
	}

	public Project(String id, String name, HashMap<UUID, Binary> binaries) {
		_id = id;
		_name = name;
		_binaries = binaries;
	}

	public JsonObject toBriefJson() {
		try (final JsonGenerator.Builder g = new JsonGenerator.Builder()) {
			g
			.writeStartObject()
			.  write("id", _id)
			.  write("name", _name);
			
			if (!_binaries.isEmpty()) {
				final Binary first = _binaries.values().iterator().next();
				g.write("binaryId", first.getId().toString());
			}
			
			g
			.writeEnd();
			return (JsonObject) g.get();
		}
	}

	public JsonObject toJson(Path path) {
		final JsonObjectBuilder b = new JsonObjectBuilder();
		b.add("id", _id);
		b.add("name", _name);
		
		final Binary first = _binaries.values().iterator().next();
		b.add("binaryId", first.getId().toString());
		first.toJson(b, path.resolve(first.getId().toString()));

		return b.build();
	}
	
	public Binary newBinary(UUID id, String name) {
		final Binary b = new Binary(id, name);
		_binaries.put(id, b);
		return b;
	}
	
}
