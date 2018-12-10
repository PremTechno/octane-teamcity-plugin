/*
 *     2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "octane-config")
@XmlAccessorType(XmlAccessType.NONE)
public class OctaneConfigStructure {

	@XmlElement
	private String identity;
	@XmlElement
	private String identityFrom;
	@XmlElement
	private String uiLocation;
	@XmlElement(name = "api-key")
	private String username;
	@XmlElement(name = "secret")
	private String secretPassword;
	@XmlElement
	private String location;
	@XmlElement
	private String sharedSpace;

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getIdentityFrom() {
		return identityFrom;
	}

	public void setIdentityFrom(String identityFrom) {
		this.identityFrom = identityFrom;
	}

	public long getIdentityFromAsLong() {
		return Long.valueOf(identityFrom);
	}

	public String getUiLocation() {
		return uiLocation;
	}

	public void setUiLocation(String uiLocation) {
		this.uiLocation = uiLocation;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSecretPassword() {
		if(secretPassword != null && !secretPassword.isEmpty()) {
			secretPassword = EncryptUtil.isScrambled(secretPassword) ? EncryptUtil.unscramble(secretPassword) : secretPassword;
		}
		return secretPassword;
	}

	public void setSecretPassword(String secretPassword) {
		if(secretPassword != null && !secretPassword.isEmpty()) {
			secretPassword = EncryptUtil.isScrambled(secretPassword) ? secretPassword : EncryptUtil.scramble(secretPassword);
		}
		this.secretPassword = secretPassword;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSharedSpace() {
		return sharedSpace;
	}

	public void setSharedSpace(String sharedSpace) {
		this.sharedSpace = sharedSpace;
	}

	@Override
	public String toString() {
		return "OctaneConfigStructure { " +
				"identity: " + identity +
				", identityFrom: " + identityFrom +
				", uiLocation: " + uiLocation +
				", apiKey: " + username +
				", secret: " + secretPassword +
				", location: " + location +
				", sharedSpace: " + sharedSpace + '}';
	}
}
