package org.whitehole.app.model;

import java.util.UUID;

public class FutureProject {
	private final Repository _r;
	
	private final UUID _id;
	
	public UUID getId() {
		return _id;
	}
	
	private final String _name;
	
	public String getName() {
		return _name;
	}
	
	private Project _p;

	public Project obtain() throws Exception {
		if (_p == null)
			_p = _r.loadProject(_id);
		return _p;
	}
	
	public FutureProject(Repository r, UUID id, String name) {
		_r = r;
		_id = id;
		_name = name;
	}
	
	public FutureProject(Repository r, Project p) {
		this(r, p.getId(), p.getName());
		_p = p;
	}
	
}