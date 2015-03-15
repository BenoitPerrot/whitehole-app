package org.whitehole.app.model;

import org.whitehole.infra.json.JsonArray;

public class ProjectRepository {
	public JsonArray getProjectBriefs() {
		return new JsonArray();
	}
	public Project getProjectById(String id) {
		return new Project();
	}
}