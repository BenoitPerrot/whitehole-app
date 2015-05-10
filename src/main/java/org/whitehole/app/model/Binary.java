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
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package org.whitehole.app.model;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.whitehole.assembly.ia32_x64.control.CallGraphExplorer;
import org.whitehole.assembly.ia32_x64.control.ControlFlowGraph;
import org.whitehole.assembly.ia32_x64.dis.Disassembler;
import org.whitehole.binary.pe.Image;
import org.whitehole.binary.pe.SectionHeader;

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
	
	public Image getImage() {
		return _lpe;
	}

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

}
