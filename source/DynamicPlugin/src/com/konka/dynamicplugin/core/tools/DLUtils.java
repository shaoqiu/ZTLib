package com.konka.dynamicplugin.core.tools;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class DLUtils {

	public static PackageInfo getPackageInfo(Context context, String apkFilepath)
			throws FileNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = null;
		pkgInfo = pm.getPackageArchiveInfo(apkFilepath,
				PackageManager.GET_ACTIVITIES);
		return pkgInfo;
	}

	public static Drawable getAppIcon(Context context, String apkFilepath)
			throws FileNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return null;
		}
		ApplicationInfo appInfo = addSourceDir(apkFilepath, pkgInfo);
		return pm.getApplicationIcon(appInfo);
	}

	public static CharSequence getAppLabel(Context context, String apkFilepath)
			throws FileNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return "";
		}
		ApplicationInfo appInfo = addSourceDir(apkFilepath, pkgInfo);
		return pm.getApplicationLabel(appInfo);
	}

	public static CharSequence getAppPackageName(Context context, String apkFilepath)
			throws FileNotFoundException {
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return "";
		}
		ApplicationInfo appInfo = addSourceDir(apkFilepath, pkgInfo);
		return appInfo.packageName;
	}

	public static CharSequence getAppDescription(Context context, String apkFilepath)
			throws FileNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return "";
		}
		ApplicationInfo appInfo = addSourceDir(apkFilepath, pkgInfo);
		return appInfo.loadDescription(pm);
	}

	public static int getAppVersion(Context context, String apkFilepath)
			throws FileNotFoundException {
		// PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return 1;
		}
		// ApplicationInfo appInfo = addSourceDir(apkFilepath, pkgInfo);
		return pkgInfo.versionCode;
	}

	// Workaround for http://code.google.com/p/android/issues/detail?id=9151
	private static ApplicationInfo addSourceDir(String apkFilepath,
			PackageInfo pkgInfo) {
		ApplicationInfo appInfo = pkgInfo.applicationInfo;
		if (Build.VERSION.SDK_INT >= 8) {
			appInfo.sourceDir = apkFilepath;
			appInfo.publicSourceDir = apkFilepath;
		}
		return appInfo;
	}

	public static void showDialog(Activity activity, String title, String message) {
		new AlertDialog.Builder(activity).setTitle(title).setMessage(message)
				.setPositiveButton("确定", null).show();
	}

}
