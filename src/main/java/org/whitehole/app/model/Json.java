// Copyright (c) 2015, Benoit PERROT.
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
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.package org.whitehole.app.model;
package org.whitehole.app.model;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Map.Entry;

import org.whitehole.apps.objdump.assembly.ia32_x64.IntelStringWriter;
import org.whitehole.assembly.ia32_x64.control.BasicBlock;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph.MotivatedBlock;
import org.whitehole.assembly.ia32_x64.dom.Immediate;
import org.whitehole.assembly.ia32_x64.dom.Instruction;
import org.whitehole.assembly.ia32_x64.dom.Mnemonic;
import org.whitehole.assembly.ia32_x64.dom.Operand;
import org.whitehole.infra.json.JsonException;
import org.whitehole.infra.json.JsonGenerator;

public class Json {

	private Json() {
	}

	public static JsonGenerator write(JsonGenerator g, Binary b) throws Exception {
		g.writeStartObject("content");
		org.whitehole.binary.pe.Json.write(g, "pe", b.getImage());
		g.writeEnd();

		g.writeStartArray("entryPoints");
		b.extractEntryPoints().stream().forEach(p -> g.write(new BigDecimal(p)));
		return g.writeEnd();
	}

	public static JsonGenerator write(JsonGenerator g, ControlFlowGraph cfg) throws JsonException {
		g.writeStartObject();

		g.writeStartObject("basicBlocks");
		for (final BasicBlock bb : cfg.getBasicBlocks()) {

			g.writeStartObject("0x" + Long.toHexString(bb.getEntryPoint()));

			g.writeStartArray("instructions");
			for (final Entry<Long, Instruction> e : bb)
				write(g, e.getValue(), e.getKey());
			g.writeEnd();

			if (cfg.hasOutcomingBlocks(bb)) {
				g.writeStartArray("exits");
				for (final MotivatedBlock m : cfg.getDestinationBlocks(bb)) {
					final BasicBlock obb = m.getBlock();
					g.writeStartObject();
					g.write("point", "0x" + Long.toHexString(obb.getEntryPoint()));
					g.write("reason", m.getReason().toString());
					g.writeEnd();
				}
				g.writeEnd();
			}

			g.writeEnd();
		}
		g.writeEnd();

		return g.writeEnd();
	}

	public static JsonGenerator write(JsonGenerator g, Instruction i, long p) throws JsonException {
		g.writeStartObject();

		boolean written = false;
		if (i.getMnemonic() == Mnemonic.CALL) {
			if (i.getOperands().length == 1) {
				final Operand o = i.getOperands()[0];
				if (o instanceof Immediate) {
					final Immediate imm = (Immediate) o;
					final long addr = 5 /* i.getByteLength() */+ imm.getSignedInteger().intValue() /* FIXME: sign-extended long */+ p;
					g.write("m", "call");
					g.writeStartObject("o");
					// final String name = null; // FIXME: find routine name by address
					// if (name != null)
					// g.write("name", name);
					g.write("rva", "0x" + Long.toHexString(addr));
					g.writeEnd();
					written = true;
				}
				// else if (o instanceof SIBDAddress) {
				// final SIBDAddress a = (SIBDAddress) o;
				// if (a.getBase() == null && a.getIndex() == null && a.getDisplacement() != null) {
				// final long addr = a.getDisplacement().longValue();
				// final String name = null; // FIXME: find routine name by address
				// if (name != null) {
				// g.write("m", "call");
				// g.writeStartArray("o");
				// g.write("dll@" + name);
				// g.writeEnd();
				// written = true;
				// }
				// }
				// }
			}
		}

		if (!written) {
			g.write("m", IntelStringWriter.toString(i.getMnemonic()));
			if (0 < i.getOperands().length) {
				g.writeStartArray("o");
				for (Operand o : i.getOperands()) {
					final StringWriter sw = new StringWriter();
					IntelStringWriter.OperandWriter ow = new IntelStringWriter.OperandWriter(sw, i.getMnemonic());
					o.accept(ow);
					g.write(sw.toString());
				}
				g.writeEnd();
			}
		}

		return g.writeEnd();
	}

	public static JsonGenerator write(JsonGenerator g, Project p, boolean detailFirstBinary) throws Exception {
		g.writeStartObject()
				.write("id", p.getId().toString())
				.write("name", p.getName());
		// <<
		if (detailFirstBinary) {
			final Binary first = p.getBinaries().values().iterator().next().obtain();
			g.write("binaryId", first.getId().toString());
			write(g, first);
		}
		else {
			g.writeStartArray("binaries");
			p.getBinaries().forEach((id, binary) -> {
				g.writeStartObject()
						.write("id", id.toString())
						.write("name", binary.getName())
						.writeEnd();
			});
			g.writeEnd();
		}
		// >>
		return g.writeEnd();
	}

	public static JsonGenerator write(JsonGenerator g, Workspace ws) {
		g.writeStartArray();
		ws.getProjects().forEach(p -> {
			g.writeStartObject()
					.write("id", p.getId().toString())
					.write("name", p.getName())
					.writeEnd();
		});
		g.writeEnd();
		return g;
	}

}
