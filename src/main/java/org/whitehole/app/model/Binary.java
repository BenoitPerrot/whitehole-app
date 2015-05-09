package org.whitehole.app.model;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.whitehole.apps.JsonBuilder;
import org.whitehole.assembly.ia32_x64.control.CallGraphExplorer;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.assembly.ia32_x64.dis.Disassembler;
import org.whitehole.binary.pe.Image;
import org.whitehole.binary.pe.SectionHeader;
import org.whitehole.infra.json.JsonArray;
import org.whitehole.infra.json.JsonNumber;
import org.whitehole.infra.json.JsonObject;
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

	private ByteBuffer _b;

	private Image _lpe;

	public Binary(UUID id, String name, ByteBuffer b, Image lpe) {
		_id = id;
		_name = name;
		_b = b;
		_lpe = lpe;
	}

	// FIXME: move to some kind of container for Binaries
	// <<
	private HashMap<Long, ControlFlowGraph> _entryPointToControlFlowGraph;

	private Binary explore() throws Exception {
		if (_entryPointToControlFlowGraph == null) {
			_entryPointToControlFlowGraph = new HashMap<>();

			final long entryPointRVA = _lpe.getAddressOfEntryPoint().longValue();
			// Entry point (relative to image base): Long.toHexString(entryPointRVA))
			// Entry point (absolute): Long.toHexString(entryPointRVA + imageBase))

			// ep relative to imgb
			// h.getVA relative to imgb
			final SectionHeader sh = _lpe.findSectionHeaderByRVA(entryPointRVA);
			if (sh != null) {

				// final long vma = imageBase + sh.getVirtualAddress().toBigInteger().longValue();
				// Logger.getAnonymousLogger().info("Entry point in section '" + Explorer.getName(sh) + "' starting at VMA 0x" + Long.toHexString(vma));

				CallGraphExplorer.explore(new Disassembler(_lpe.isPE32x() ? Disassembler.WorkingMode._64BIT : Disassembler.WorkingMode._32BIT), _b, Image.computeRVAToOffset(sh) + entryPointRVA,
						_entryPointToControlFlowGraph);
			}
		}
		return this;
	}

	public Set<Long> extractEntryPoints() throws Exception {
		return explore()._entryPointToControlFlowGraph.keySet();
	}

	public ControlFlowGraph extractControlFlowGraph(long entryPoint) throws Exception {
		return explore()._entryPointToControlFlowGraph.get(entryPoint);
	}

	public JsonObject toJson(JsonObject o) {
		try {
			final JsonObjectBuilder pe = new JsonObjectBuilder();
			pe.add("pe", JsonBuilder.toJson(new JsonObjectBuilder(), _lpe));
			o.put("content", pe.build());

			final JsonArray entryPoints = new JsonArray();
			extractEntryPoints().stream().forEach(p -> entryPoints.add(new JsonNumber(new BigDecimal(p))));
			o.put("entryPoints", entryPoints);
		}
		catch (Exception x) {
		}
		return o;
	}

}
