package me.rime.necesse.modloader;

import java.lang.reflect.Field;

// Unused for now, will get around to fixing it and starting to use it later. TODO
public class NecesseMetadataLookup {
	private final String name;
	private int appID = -1;
	private final String version;
	private int hotfix = -1; // These can't be left uninitialized, their value would be 0 and 0 is a valid value for them, so there's no way to distinguish uninitialized vs zero value.

	public NecesseMetadataLookup() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> gameInfoClass = Class.forName("necesse.engine.GameInfo");
		Class<?> versionClass = Class.forName("necesse.engine.GameVersion");

		this.name = (String) getField(gameInfoClass, "name");
		this.appID = (Integer) getField(gameInfoClass, "appID");
		Field versionField = versionClass.getField("versionString");
		versionField.setAccessible(true);
		this.version = (String) versionField.get(versionClass.cast(getField(gameInfoClass, "version")));
		this.hotfix = (Integer) getField(gameInfoClass, "hotfix");

		if (this.name == null || appID == -1 || version == null || hotfix == -1) {
			throw new IllegalStateException("Failed to retrieve game metadata");
		}
	}

	private Object getField(Class<?> clazz, String name) throws IllegalAccessException, NoSuchFieldException {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);

		return field.get(null);
	}

	public String getVersion() {
		return version;
	}

	public int getHotfix() {
		return hotfix;
	}

	public String getName() {
		return name;
	}

	public int getAppID() {
		return appID;
	}
}
