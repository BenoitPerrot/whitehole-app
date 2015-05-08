package org.whitehole.app.model;

import java.io.File;
import java.io.FileInputStream;
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
import org.whitehole.infra.json.JsonNumber;
import org.whitehole.infra.json.JsonObjectBuilder;

public class Binary {

	private UUID _id;

	public UUID getId() {
		return _id;
	}

	private String _name;

	public String getName() {
		return _name;
	}

	public Binary(UUID id, String name) {
		_id = id;
		_name = name;
	}

	// FIXME: move to some kind of container for Binaries
	// <<
	private HashMap<Long, ControlFlowGraph> _entryPointToControlFlowGraph;

	private Binary explore(Path path) throws Exception {
		if (_entryPointToControlFlowGraph == null) {
			_entryPointToControlFlowGraph = new HashMap<>();

			final Image lpe = loadImage(path);
			final ByteBuffer buffer = loadByteBuffer(path);

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

	public Set<Long> extractEntryPoints(Path path) throws Exception {
		return explore(path)._entryPointToControlFlowGraph.keySet();
	}

	public ControlFlowGraph extractControlFlowGraph(Path path, long entryPoint) throws Exception {
		return explore(path)._entryPointToControlFlowGraph.get(entryPoint);
	}

	private Image _lpe;

	public Image loadImage(Path path) throws IOException {
		return load(path)._lpe;
	}

	private ByteBuffer _b;

	public ByteBuffer loadByteBuffer(Path path) throws IOException {
		return load(path)._b;
	}

	private Binary load(Path path) throws IOException {
		if (_lpe == null || _b == null) { // Same
			final File f = path.toFile();
			final FileInputStream fi = new FileInputStream(f);
			//
			_b = fi.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			_lpe = Image.load(new LargeByteBuffer(_b), 0);
			//
			fi.close();
		}
		return this;
	}

	public JsonObjectBuilder toJson(JsonObjectBuilder b, Path path) {
		try {
			final JsonObjectBuilder pe = new JsonObjectBuilder();
			pe.add("pe", JsonBuilder.toJson(new JsonObjectBuilder(), loadImage(path)));
			b.add("content", pe);

			final JsonArray entryPoints = new JsonArray();
			extractEntryPoints(path).stream().forEach(p -> entryPoints.add(new JsonNumber(new BigDecimal(p))));
			b.add("entryPoints", entryPoints);
		}
		catch (Exception x) {
		}

		return b;
	}

}
