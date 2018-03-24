/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */

package org.onap.aaf.auth.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

public class InputIterator implements Iterable<String> {
	private BufferedReader in;
	private final PrintStream out;
	private final String prompt, instructions;
	
	public InputIterator(BufferedReader in, PrintStream out, String prompt, String instructions) {
		this.in = in;
		this.out = out;
		this.prompt = prompt;
		this.instructions = instructions;
	}
	
	@Override
	public Iterator<String> iterator() {
		out.println(instructions);
		return new Iterator<String>() {
			String input;
			@Override
			public boolean hasNext() {
				out.append(prompt);
				try {
					input = in.readLine();
				} catch (IOException e) {
					input = null;
					return false;
				}
				return input.length()>0;
			}

			@Override
			public String next() {
				return input;
			}

			@Override
			public void remove() {
			}
		};
	}
}

