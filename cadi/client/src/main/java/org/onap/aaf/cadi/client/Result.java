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

package org.onap.aaf.cadi.client;

public class Result<T> {
	public final int code;
	public final T value;
	public final String error;

	private Result(int code, T value, String error) {
		this.code = code;
		this.value = value;
		this.error = error;
	}

	public static<T> Result<T> ok(int code,T t) {
		return new Result<T>(code,t,null);
	}
	
	public static<T> Result<T> err(int code,String body) {
		return new Result<T>(code,null,body);
	}

	public static<T> Result<T> err(Result<?> r) {
		return new Result<T>(r.code,null,r.error);
	}

	public boolean isOK() {
		return error==null;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("Code: ");
		sb.append(code);
		if(error!=null) {
			sb.append(" = ");
			sb.append(error);
		}
		return sb.toString();
	}
}