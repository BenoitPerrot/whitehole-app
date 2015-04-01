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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.whitehole.apps.JsonBuilder;
import org.whitehole.binary.pe.Image;
import org.whitehole.infra.io.LargeByteBuffer;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonObjectBuilder;

public class Project {
	
	private final String _id;
	private final String _binaryPath;
	
	public Project(String id, String binaryPath) {
		_id = id;
		_binaryPath = binaryPath.replace("\\", "/");
	}
	
	public JsonObject toBriefJson() {
		return new JsonObjectBuilder()
			.add("id", _id)
			.add("binaryPath", _binaryPath)
			.build();
	}
	
	public JsonObject toJson() {
		final JsonObjectBuilder b = new JsonObjectBuilder();
		b.add("id", _id);
		b.add("binaryPath", _binaryPath);
		
		try {
			final JsonObjectBuilder pe = new JsonObjectBuilder();
			pe.add("pe", JsonBuilder.toJson(new JsonObjectBuilder(), loadImage()));
			b.add("content", pe);
		}
		catch (IOException x) {
		}
		
		return b.build();
	}
	
	// FIXME: move to some kind of container for Binaries
	// <<
	private Image _lpe;
	public Image loadImage() throws IOException {
		return loadBinary()._lpe;
	}
	
	private ByteBuffer _b;
	public ByteBuffer loadByteBuffer() throws IOException {
		return loadBinary()._b;
	}
	
	private Project loadBinary() throws IOException {
		if (_lpe == null || _b == null) { // Same
			final File f = new File(_binaryPath);
			final FileInputStream fi = new FileInputStream(f);
			_b = fi.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			_lpe = Image.load(new LargeByteBuffer(_b), 0);
			fi.close();
		}
		return this;
	}
	// >>
}