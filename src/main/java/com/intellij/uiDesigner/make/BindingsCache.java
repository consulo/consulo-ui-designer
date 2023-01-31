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

/*
 * @author: Eugene Zhuravlev
 * Date: Jul 4, 2003
 * Time: 7:39:27 PM
 */
package com.intellij.uiDesigner.make;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import consulo.compiler.CompilerPaths;
import consulo.document.Document;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;
import consulo.ide.impl.idea.compiler.impl.StateCache;
import consulo.logging.Logger;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.uiDesigner.compiler.Utils;
import com.intellij.uiDesigner.lw.LwRootContainer;

final class BindingsCache
{
	private static final Logger LOG = Logger.getInstance(BindingsCache.class);
	@NonNls
	private static final String BINDINGS_FILE_NAME = "formbinding.dat";
	private StateCache<MyState> myCache;

	public BindingsCache(final Project project)
	{
		final File cacheStoreDirectory = CompilerPaths.getCacheStoreDirectory(project);
		try
		{
			if(cacheStoreDirectory != null)
			{
				FileUtil.createParentDirs(cacheStoreDirectory);
				myCache = createCache(cacheStoreDirectory);
			}
			else
			{
				myCache = null;
			}
		}
		catch(IOException e)
		{
			LOG.info(e);
			for(File file : cacheStoreDirectory.listFiles())
			{
				if(file.getName().startsWith(BINDINGS_FILE_NAME))
				{
					FileUtil.delete(file);
				}
			}
			try
			{
				myCache = createCache(cacheStoreDirectory);
			}
			catch(IOException e1)
			{
				LOG.info(e1);
				myCache = null;
			}
		}
	}

	private static StateCache<MyState> createCache(final File cacheStoreDirectory) throws IOException
	{
		return new StateCache<MyState>(new File(cacheStoreDirectory, BINDINGS_FILE_NAME))
		{
			public MyState read(final DataInput stream) throws IOException
			{
				return new MyState(stream.readLong(), stream.readUTF());
			}

			public void write(final MyState myState, final DataOutput out) throws IOException
			{
				out.writeLong(myState.getFormTimeStamp());
				out.writeUTF(myState.getClassName());
			}
		};
	}

	public String getBoundClassName(final VirtualFile formFile) throws Exception
	{
		File file = VfsUtil.virtualToIoFile(formFile);
		String classToBind = getSavedBinding(file);
		if(classToBind == null)
		{
			final Document doc = FileDocumentManager.getInstance().getDocument(formFile);
			final LwRootContainer rootContainer = Utils.getRootContainer(doc.getText(), null);
			classToBind = rootContainer.getClassToBind();
		}
		if(classToBind != null)
		{
			updateCache(file, classToBind);
		}
		return classToBind;
	}

	private String getSavedBinding(final File formFile)
	{
		if(myCache != null)
		{
			try
			{
				final MyState state = myCache.getState(formFile);
				if(state != null)
				{
					if(formFile.lastModified() == state.getFormTimeStamp())
					{
						return state.getClassName();
					}
				}
			}
			catch(IOException e)
			{
				myCache.wipe();
			}
		}
		return null;
	}

	private void updateCache(final File formFile, final String classToBind)
	{
		if(myCache != null)
		{
			final MyState state = new MyState(formFile.lastModified(), classToBind);
			try
			{
				myCache.update(formFile, state);
			}
			catch(IOException e)
			{
				LOG.info(e);
				myCache.wipe();
				try
				{
					myCache.update(formFile, state);
				}
				catch(IOException ignored)
				{
				}
			}
		}
	}

	public void close()
	{
		try
		{
			myCache.close();
		}
		catch(IOException e)
		{
			LOG.info(e);
		}
	}

	private static final class MyState implements Serializable
	{
		private final long myFormTimeStamp;
		private final String myClassName;

		public MyState(final long formTimeStamp, final String className)
		{
			myFormTimeStamp = formTimeStamp;
			myClassName = className;
		}

		public long getFormTimeStamp()
		{
			return myFormTimeStamp;
		}

		public String getClassName()
		{
			return myClassName;
		}
	}
}
