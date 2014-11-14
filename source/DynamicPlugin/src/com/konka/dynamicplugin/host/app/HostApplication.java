package com.konka.dynamicplugin.host.app;

import android.app.Application;
import android.content.Context;

import com.konka.dynamicplugin.core.IPluginManager;
import com.konka.dynamicplugin.core.PluginManager;
import com.konka.dynamicplugin.host.IHost;

public abstract class HostApplication extends Application implements IHost {
	private IPluginManager mPluginManager;

	@Override
	public void onCreate() {
		mPluginManager = PluginManager.getInstance();
		onAppCreate();
		super.onCreate();
	}

	/**
	 * Do Your Own App Init Work Here
	 */
	abstract public void onAppCreate();

	@Override
	public final IPluginManager getPluginManager() {
		return mPluginManager;
	}

	@Override
	public final Context getHostContext() {
		return this;
	}

}
