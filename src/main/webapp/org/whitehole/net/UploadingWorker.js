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

//
// (Large) file uploading , meant to be run from within a web worker.

function upload(url, data, options) {
	'use strict';
	
	const type = options.type || 'POST';
	const chunkSize = options.chunkSize || 1024 * 1024;
	
	let start = 0, end = chunkSize, size = data.size, hasFailed = false;
	
	function progressed() {
		postMessage({type: 'progressed', detail: { start: start, end: end, size: size }});
	}
	
	function failed() {
		hasFailed = true;
		postMessage({type: 'failed', detail: {}});
	}
	
	while (start < size && !hasFailed) {
		const xhr = new XMLHttpRequest();
		xhr.addEventListener('load', progressed);
		xhr.addEventListener('error', failed);
		xhr.open(type, url, false); // CRUCIAL: be synchronous to progress correctly, it is fine as running in a worker
		xhr.overrideMimeType('application/octet-stream');
		xhr.setRequestHeader('Content-Range', 'bytes ' + start + '-' + end + '/' + size);
		xhr.send(data.slice(start, end));
		
		start = end;
		end = start + chunkSize;
		if (size < end)
			end = size;
	}

	if (!hasFailed)
		postMessage({type: 'uploaded' });
};

self.addEventListener('message', function(e) {
	upload(e.data.url, e.data.data, e.data.options);
});
