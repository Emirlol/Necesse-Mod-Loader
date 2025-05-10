package me.rime.necesse.modloader;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier.LibraryType;

enum NecesseLibrary implements LibraryType {
	CLIENT(EnvType.CLIENT, "necesse/StartPlatformClient.class"),
	PLATFORM("necesse/engine/platforms/Platform.class"),
	STEAM_PLATFORM(EnvType.CLIENT, "necesse/engine/platforms/steam/SteamPlatform.class"),
	SERVER(EnvType.SERVER, "necesse/StartPlatformServer.class"),
	DESKTOP_PLATFORM(EnvType.SERVER, "necesse/engine/platforms/desktop/DesktopPlatform.class");

	private final EnvType env;
	private final String[] paths;

	NecesseLibrary(String path) {
		this(null, new String[]{path});
	}

	NecesseLibrary(String... paths) {
		this(null, paths);
	}

	NecesseLibrary(EnvType env, String... paths) {
		this.paths = paths;
		this.env = env;
	}

	@Override
	public boolean isApplicable(EnvType env) {
		return this.env == null || this.env == env;
	}

	@Override

	public String[] getPaths() {
		return paths;
	}
}
