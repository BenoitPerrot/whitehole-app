package org.whitehole.app.model;

import java.util.UUID;

public class FutureBinary {

	private final Repository _r;

	private final UUID _id;

	public UUID getId() {
		return _id;
	}

	private final String _name;

	public String getName() {
		return _name;
	}

	private Binary _b;

	public Binary obtain() throws Exception {
		if (_b == null)
			_b = _r.loadBinary(_id, _name);
		return _b;
	}
	
	public FutureBinary(Repository r, UUID id, String name) {
		_r = r;
		_id = id;
		_name = name;
	}

	public FutureBinary(Repository r, Binary b) {
		this(r, b.getId(), b.getName());
		_b = b;
	}
}
