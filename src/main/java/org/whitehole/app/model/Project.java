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
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.whitehole.apps.JsonBuilder;
import org.whitehole.assembly.ia32_x64.control.CallGraphExplorer;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.assembly.ia32_x64.dis.Disassembler;
import org.whitehole.binary.pe.Image;
import org.whitehole.binary.pe.SectionHeader;
import org.whitehole.infra.io.LargeByteBuffer;
import org.whitehole.infra.json.JsonArray;
import org.whitehole.infra.json.JsonGenerator;
import org.whitehole.infra.json.JsonNumber;
import org.whitehole.infra.json.JsonObject;
import org.whitehole.infra.json.JsonObjectBuilder;
import org.whitehole.infra.json.JsonReader;

public class Project {

	private final String _id;
	
	public String getId() {
		return _id;
	}
	
	private final String _name;
	
	public String getName() {
		return _name;
	}
	
	private String _binaryPath = "";

	public String getBinaryPath() {
		return _binaryPath;
	}
	
	public Project setBinaryPath(String binaryPath) {
		_binaryPath = binaryPath.replace("\\", "/");
		return this;
	}

	public Project(String id, String name) {
		_id = id;
		_name = name;
	}

	public String newBinary(String binaryName) throws IOException {
		return "\"" + UUID.randomUUID().toString() + "\"";
	}

	public JsonObject toBriefJson() {
		return new JsonObjectBuilder()
				.add("id", _id)
				.add("name", _name)
				.add("binaryPath", _binaryPath)
				.build();
	}

	public JsonObject toJson() {
		final JsonObjectBuilder b = new JsonObjectBuilder();
		b.add("id", _id);
		b.add("name", _name);
		b.add("binaryPath", _binaryPath);

		try {
			final JsonObjectBuilder pe = new JsonObjectBuilder();
			pe.add("pe", JsonBuilder.toJson(new JsonObjectBuilder(), loadImage()));

			b.add("content", pe);
			
			final JsonArray entryPoints = new JsonArray();
			extractEntryPoints().stream().forEach(p -> entryPoints.add(new JsonNumber(new BigDecimal(p))));
			b.add("entryPoints", entryPoints);
		}
		catch (Exception x) {
		}

		return b.build();
	}

	// FIXME: move to some kind of container for Binaries
	// <<
	private HashMap<Long, ControlFlowGraph> _entryPointToControlFlowGraph;
	
	private Project exploreBinary() throws Exception {
		if (_entryPointToControlFlowGraph == null) {
			_entryPointToControlFlowGraph = new HashMap<>();

			final Image lpe = loadImage();
			final ByteBuffer buffer = loadByteBuffer();

			final long entryPointRVA = lpe.getAddressOfEntryPoint().longValue();
			// Entry point (relative to image base): Long.toHexString(entryPointRVA))
			// Entry point (absolute): Long.toHexString(entryPointRVA + imageBase))

			// ep relative to imgb
			// h.getVA relative to imgb
			final SectionHeader sh = lpe.findSectionHeaderByRVA(entryPointRVA);
			if (sh != null) {

				// final long vma = imageBase + sh.getVirtualAddress().toBigInteger().longValue();
				// Logger.getAnonymousLogger().info("Entry point in section '" + Explorer.getName(sh) + "' starting at VMA 0x" + Long.toHexString(vma));

				CallGraphExplorer.explore(new Disassembler(lpe.isPE32x() ? Disassembler.WorkingMode._64BIT : Disassembler.WorkingMode._32BIT), buffer, Image.computeRVAToOffset(sh) + entryPointRVA,
						_entryPointToControlFlowGraph);
			}
		}
		return this;
	}
	
	public Set<Long> extractEntryPoints() throws Exception {
		return exploreBinary()._entryPointToControlFlowGraph.keySet();
	}
	
	public ControlFlowGraph extractControlFlowGraph(long entryPoint) throws Exception {
		return exploreBinary()._entryPointToControlFlowGraph.get(entryPoint);
	}

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

	//
	//
	//
	
	public static void write(JsonGenerator g, Project p) {
		g.writeStartObject();
		g.write("id", p._id);
		g.write("name", p._name);
		g.write("binaryPath", p._binaryPath);
		g.writeEnd();
	}

	public static Project load(Path path) throws Exception {
		try (final JsonReader r = new JsonReader(new FileReader(path.toFile()))) {
			final JsonObject o = r.readObject();
			final String id = o.getString("id").toString();
			final String name = o.getString("name").toString();
			final String binaryPath = o.getString("binaryPath").toString();
			return new Project(id, name).setBinaryPath(binaryPath);
		}
	}
}
