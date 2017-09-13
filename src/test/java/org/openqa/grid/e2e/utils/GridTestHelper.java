// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.grid.e2e.utils;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;
import com.rationaleemotions.servlets.ServerHelper;

public class GridTestHelper {

	public static void waitForGrid() {
		newWait().until(new Function<Object, Boolean>() {
			@Override
			public Boolean apply(Object input) {
				if (ServerHelper.getHubRegistry()==null) {
					return false;
				}
				return true;
			}
		});
	}
	
	private static Wait<Object> newWait() {
		return new FluentWait<Object>("").withTimeout(30, SECONDS);
	}
}