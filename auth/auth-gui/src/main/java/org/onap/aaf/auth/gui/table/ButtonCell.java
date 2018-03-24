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

package org.onap.aaf.auth.gui.table;

import org.onap.aaf.misc.xgen.html.HTMLGen;

public class ButtonCell extends AbsCell {
	private String[] attrs;
	
	public ButtonCell(String value, String ... attributes) {
		attrs = new String[2+attributes.length];
		attrs[0]="type=button";
		attrs[1]="value="+value;
		System.arraycopy(attributes, 0, attrs, 2, attributes.length);
	}
	@Override
	public void write(HTMLGen hgen) {
		hgen.incr("input",true,attrs).end();

	}
	
	@Override
	public String[] attrs() {
		return AbsCell.CENTER;
	}
}
