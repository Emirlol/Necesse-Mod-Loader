package me.rime.necesse.modloader.patches;

import me.rime.necesse.modloader.NecesseHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class EntrypointPatch extends GamePatch {
	private static final Pattern ENTRYPOINT_PATTERN = Pattern.compile("StartPlatform(?:Client|Server)");

	@Override
	public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		String entrypoint = launcher.getEntrypoint();

		if (!ENTRYPOINT_PATTERN.matcher(entrypoint).find()) {
			Log.warn(LogCategory.GAME_PATCH, "Entrypoint " + entrypoint + " does not match expected pattern, skipping patch.");
			return;
		}

		ClassNode mainClass = classSource.apply(entrypoint);
		if (mainClass == null) {
			throw new RuntimeException("Could not load main class " + entrypoint + "!");
		}

		MethodNode startMethod = findMethod(mainClass, methodNode -> methodNode.name.equals("start"));
		if (startMethod == null) {
			throw new RuntimeException("Could not find \"start\" method in main class " + entrypoint + "!");
		}

		AbstractInsnNode startGameMethodCall = findInsn(startMethod,
				node -> node instanceof MethodInsnNode methodInsnNode
								&& methodInsnNode.name.equals("startGame")
								&& methodInsnNode.owner.contains("Loader")
								&& methodInsnNode.desc.equals("()V"), true);
		if (startGameMethodCall == null) {
			throw new RuntimeException("Could not find \"startGame\" method call in start method " + startMethod + "!");
		}

		String methodName = String.format("start%s", launcher.getEnvironmentType() == EnvType.CLIENT ? "Client" : "Server");
		MethodInsnNode newCall = new MethodInsnNode(Opcodes.INVOKESTATIC, NecesseHooks.INTERNAL_NAME, methodName, "()V", false);
		startMethod.instructions.insertBefore(startGameMethodCall, newCall);

		classEmitter.accept(mainClass);
	}
}
