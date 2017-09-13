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

import com.google.common.base.Function;

import org.openqa.grid.internal.Registry;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

public class RegistryTestHelper {

	private RegistryTestHelper() {
		// Utility class
	}

	/**
	 * Wait for the registry to have nodes registered.
	 */
	public static void waitForNodes(final Registry r) {
		newWait().until(new Function<Object, Integer>() {
			@Override
			public Integer apply(Object input) {
				Integer i = r.getHub().getRegistry().getAllProxies().size();
				if (i == 0) {
					return null;
				}
				return i;
			}
		});
	}
	/**
	 * Wait for the registry to have exactly nodeNumber nodes registered.
	 */
	public static void waitForNode(final Registry r, final int nodeNumber) {
		newWait().until(new Function<Object, Integer>() {
			@Override
			public Integer apply(Object input) {
				Integer i = r.getAllProxies().size();
				if (i != nodeNumber) {
					return null;
				}
				return i;
			}
		});
	}

	private static Wait<Object> newWait() {
		return new FluentWait<Object>("").withTimeout(30, SECONDS);
	}
}