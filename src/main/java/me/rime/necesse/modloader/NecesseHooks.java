package me.rime.necesse.modloader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.File;

public class NecesseHooks {
	public static final String INTERNAL_NAME = NecesseHooks.class.getName().replace('.', '/');

	public static void startClient() {
		File runDir = new File(".");

		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
		loader.prepareModInit(runDir.toPath(), loader.getGameInstance());
		loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
		loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
	}

	public static void startServer() {
		Log.info(LogCategory.LOG, "Starting server...");
		File runDir = new File(".");

		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
		loader.prepareModInit(runDir.toPath(), loader.getGameInstance());
		loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
		loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
	}
}
