/**
 * This file is part of Eclipse Steady.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company. All rights reserved.
 */
/**
 * 
 */
package com.sap.psr.vulas.shared.json.model;

import java.util.HashSet;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

/**
 * A set of {@link IExemption}s.
 */
public class ExemptionSet extends HashSet<IExemption> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Loops over the exemptions to find one that exempts the given {@link VulnerableDependency}.
	 * If such an exemption is found, it is returned. Otherwise, the method return null.
	 * 
	 * @param _s
	 * @param _vd
	 * @return
	 */
	public IExemption getApplicableExemption(VulnerableDependency _vd) {
		if(!this.isEmpty()) {
			for(IExemption e: this) {
				if(e.isExempted(_vd))
					return e;
			}
		}
		return null;
	}

	/**
	 * Creates a set of {@link IExemption}s by reading the settings from the given {@link Configuration}.
	 * 
	 * @param _cfg
	 * @return
	 */
	public static ExemptionSet createFromConfiguration(Configuration _cfg) {
		final ExemptionSet set = new ExemptionSet();
		set.addAll(ExemptionBug.readFromConfiguration(_cfg));
		set.addAll(ExemptionScope.readFromConfiguration(_cfg));
		set.addAll(ExemptionUnassessed.readFromConfiguration(_cfg));
		return set;
	}
	
	/**
	 * Creates a set of {@link IExemption}s by reading the settings from the given {@link Map<String,String>}.
	 * 
	 * @param _cfg
	 * @return
	 */
	public static ExemptionSet createFromMap(Map<String, String> _map) {
		final ExemptionSet set = new ExemptionSet();
		set.addAll(ExemptionBug.readFromConfiguration(_map));
		set.addAll(ExemptionScope.readFromConfiguration(_map));
		set.addAll(ExemptionUnassessed.readFromConfiguration(_map));
		return set;
	}
}
