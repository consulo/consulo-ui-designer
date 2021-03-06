/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.uiDesigner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.intellij.ProjectTopics;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.util.Pair;
import com.intellij.reference.SoftReference;
import com.intellij.uiDesigner.lw.StringDescriptor;
import com.intellij.uiDesigner.radComponents.RadComponent;
import com.intellij.uiDesigner.radComponents.RadRootContainer;

/**
 * @author yole
 */
@Singleton
public class StringDescriptorManager
{
	public static StringDescriptorManager getInstance(Module module)
	{
		return ServiceManager.getService(module, StringDescriptorManager.class);
	}

	private Module myModule;
	private final Map<Pair<Locale, String>, SoftReference<PropertiesFile>> myPropertiesFileCache = new HashMap<Pair<Locale, String>, SoftReference<PropertiesFile>>();

	@Inject
	public StringDescriptorManager(Module module)
	{
		myModule = module;
		module.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter()
		{
			public void rootsChanged(final ModuleRootEvent event)
			{
				synchronized(myPropertiesFileCache)
				{
					myPropertiesFileCache.clear();
				}
			}
		});
	}

	@Nullable
	public String resolve(@Nonnull RadComponent component, @Nullable StringDescriptor descriptor)
	{
		RadRootContainer root = (RadRootContainer) FormEditingUtil.getRoot(component);
		Locale locale = (root != null) ? root.getStringDescriptorLocale() : null;
		return resolve(descriptor, locale);
	}

	@Nullable
	public String resolve(@Nullable StringDescriptor descriptor, @Nullable Locale locale)
	{
		if(descriptor == null)
		{
			return null;
		}

		if(descriptor.getValue() != null)
		{
			return descriptor.getValue();
		}

		IProperty prop = resolveToProperty(descriptor, locale);
		if(prop != null)
		{
			final String value = prop.getUnescapedValue();
			if(value != null)
			{
				return value;
			}
		}
		// We have to return surrogate string in case if propFile name is invalid or bundle doesn't have specified key
		return "[" + descriptor.getKey() + " / " + descriptor.getBundleName() + "]";
	}

	public IProperty resolveToProperty(@Nonnull StringDescriptor descriptor, @Nullable Locale locale)
	{
		String propFileName = descriptor.getDottedBundleName();
		Pair<Locale, String> cacheKey = new Pair<Locale, String>(locale, propFileName);
		SoftReference<PropertiesFile> propertiesFileRef;
		synchronized(myPropertiesFileCache)
		{
			propertiesFileRef = myPropertiesFileCache.get(cacheKey);
		}
		PropertiesFile propertiesFile = (propertiesFileRef == null) ? null : propertiesFileRef.get();
		if(propertiesFile == null || !propertiesFile.getContainingFile().isValid())
		{
			propertiesFile = PropertiesUtil.getPropertiesFile(propFileName, myModule, locale);
			synchronized(myPropertiesFileCache)
			{
				myPropertiesFileCache.put(cacheKey, new SoftReference<>(propertiesFile));
			}
		}

		if(propertiesFile != null)
		{
			final IProperty propertyByKey = propertiesFile.findPropertyByKey(descriptor.getKey());
			if(propertyByKey != null)
			{
				return propertyByKey;
			}
		}
		return null;
	}
}
