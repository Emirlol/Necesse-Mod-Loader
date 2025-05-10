package me.rime.necesse.modloader;

import me.rime.necesse.modloader.patches.EntrypointPatch;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NecesseGameProvider implements GameProvider {
	private Arguments arguments;
	private EnvType envType;
	private String entrypoint;
	private String platformTarget;
	private String abstractPlatform;
	private Path gameJar;
	private Collection<Path> validParentClassPath; // computed parent class path restriction (loader+deps)
	private final GameTransformer transformer = new GameTransformer(new EntrypointPatch());

	@Override
	public String getGameId() {
		return "necesse";
	}

	@Override
	public String getGameName() {
		return "Necesse";
	}

	@Override
	public String getRawGameVersion() {
		return "0.32.1";
	}

	@Override
	public String getNormalizedGameVersion() {
		return "0.32.1";
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		HashMap<String, String> contactMap = new HashMap<>();
		contactMap.put("homepage", "https://necessegame.com/");
		contactMap.put("wiki", "https://necessewiki.com/");
		contactMap.put("discord", "https://discord.gg/YBhNh52dpy");
		contactMap.put("steam", "https://store.steampowered.com/news/app/1169040");
		contactMap.put("x", "https://x.com/NecesseGame");
		contactMap.put("reddit", "https://www.reddit.com/r/Necesse");
		contactMap.put("youtube", "https://www.youtube.com/@Necesse");

		BuiltinModMetadata.Builder modMetadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
														 .setName(getGameName())
														 .addAuthor("Fair", contactMap)
														 .setContact(new ContactInformationImpl(contactMap))
														 .setDescription("Necesse Game");

		HashMap<String, String> contactMapProvider = new HashMap<>();
		BuiltinModMetadata.Builder providerMetadata = new BuiltinModMetadata.Builder("necesse_provider", "1.0.0")
															  .setName("Necesse Game Provider")
															  .addAuthor("Rime", contactMapProvider)
															  .setContact(new ContactInformationImpl(contactMapProvider))
															  .setDescription("The game provider for the Necesse for the Fabric Loader.");

		return List.of(
				new BuiltinMod(Collections.singletonList(gameJar), modMetadata.build()),
				new BuiltinMod(Collections.emptyList(), providerMetadata.build())
		);
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}

		return getLaunchDirectory(arguments);
	}

	private static Path getLaunchDirectory(Arguments arguments) {
		return Paths.get(arguments.getOrDefault("gameDir", "."));
	}

	@Override
	public boolean isObfuscated() {
		return false;
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.envType = launcher.getEnvironmentType();
		this.arguments = new Arguments();
		arguments.parse(args);

		try {
			LibClassifier<NecesseLibrary> classifier = new LibClassifier<>(NecesseLibrary.class, envType, this);
			NecesseLibrary envGameLib = envType == EnvType.CLIENT ? NecesseLibrary.CLIENT : NecesseLibrary.SERVER;
			NecesseLibrary envPlatform = envType == EnvType.CLIENT ? NecesseLibrary.STEAM_PLATFORM : NecesseLibrary.DESKTOP_PLATFORM;
			if (gameJar != null) {
				classifier.process(gameJar);
			} else {
				List<String> gameLocations = new ArrayList<>();

				String fabricGameJarPath = System.getProperty(SystemProperties.GAME_JAR_PATH);
				if (fabricGameJarPath != null) gameLocations.add(fabricGameJarPath);

				gameLocations.add(envType == EnvType.CLIENT ? "./Necesse.jar" : "./Server.jar");

				var gameLocation = gameLocations.stream()
												.map(str -> Paths.get(str).toAbsolutePath().normalize())
												.filter(Files::exists)
												.findFirst();

				if (gameLocation.isPresent()) classifier.process(gameLocation.get());
			}

			classifier.process(launcher.getClassPath());
			gameJar = classifier.getOrigin(envGameLib);
			entrypoint = classifier.getClassName(envGameLib);
			platformTarget = classifier.getClassName(envPlatform);
			abstractPlatform = classifier.getClassName(NecesseLibrary.PLATFORM);
			validParentClassPath = classifier.getSystemLibraries();
		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}
		if (gameJar == null) throw new RuntimeException("Unable to locate game jar!");

		return true;
	}

	@Override
	public void initialize(FabricLauncher launcher) {
		launcher.setValidParentClassPath(validParentClassPath);
		Log.info(LogCategory.GAME_PROVIDER, "Valid classpath: " + validParentClassPath);
		transformer.locateEntrypoints(launcher, List.of(gameJar));
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
	}

	@Override
	public void launch(ClassLoader loader) {
		MethodHandle invoker;
		Class<?> targetClass, platformClass, abstractPlatformClass;

		try {
			targetClass = loader.loadClass(entrypoint);
			platformClass = loader.loadClass(platformTarget);
			abstractPlatformClass = loader.loadClass(abstractPlatform);
			invoker = MethodHandles.lookup().findStatic(targetClass, "start", MethodType.methodType(void.class, String[].class, abstractPlatformClass));
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to find the main class invoker!", e);
		}

		try {
			// SteamPlatform has a parameterless constructor, while DesktopPlatform has an `isServer` boolean parameter.
			Object platformInstance = envType == EnvType.CLIENT ? platformClass.getDeclaredConstructor().newInstance() : platformClass.getDeclaredConstructor(boolean.class).newInstance(true);
			invoker.invoke(arguments.toArray(), platformInstance); // Can't invokeExact because we don't have a platform instance, even with casting with the class object it's still an Object in the end
		} catch (Throwable e) {
			throw new RuntimeException("Failed to invoke main class!", e);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length && arg.startsWith("--")) i++; // skip value
			else ret[writeIdx++] = arg;
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}
}
