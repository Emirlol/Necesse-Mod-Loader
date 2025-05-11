package me.rime.necesse.modloader;

import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NecesseMetadataLookup {
	private String name;
	private int appID = -1;
	private String version;
	private int hotfix = -1; // These can't be left uninitialized, their value would be 0 and 0 is a valid value for them, so there's no way to distinguish uninitialized vs zero value.
	private String discordInviteUrl;
	private String steamNewsUrl;
	private String twitterUrl;
	private String xUrl;
	private String redditUrl;
	private String youtubeUrl;
	private String websiteUrl;
	private String wikiUrl;

	public NecesseMetadataLookup(Path gameJar) {
		try (ZipFile zipFile = new ZipFile(gameJar.toFile())) {
			ZipEntry entry = zipFile.getEntry("necesse/engine/GameInfo.class");
			if (entry == null) throw new IOException("Game jar does not contain necesse/engine/GameInfo.class");

			try (InputStream inputStream = zipFile.getInputStream(entry)) {
				ClassReader classReader = new ClassReader(inputStream);
				classReader.accept(new ClassVisitor(Opcodes.ASM9) {
					@Override
					public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
						if ((access & Opcodes.ACC_STATIC) != 0 && value != null) {
							switch (name) {
								case "name" -> NecesseMetadataLookup.this.name = (String) value;
								case "appID" -> NecesseMetadataLookup.this.appID = (int) value;
								case "version" -> NecesseMetadataLookup.this.version = (String) value;
								case "hotfix" -> NecesseMetadataLookup.this.hotfix = (int) value;
								case "discord_invite_url" -> NecesseMetadataLookup.this.discordInviteUrl = (String) value;
								case "steam_news_url" -> NecesseMetadataLookup.this.steamNewsUrl = (String) value;
								case "twitter_url" -> NecesseMetadataLookup.this.twitterUrl = (String) value;
								case "x_url" -> NecesseMetadataLookup.this.xUrl = (String) value;
								case "reddit_url" -> NecesseMetadataLookup.this.redditUrl = (String) value;
								case "youtube_url" -> NecesseMetadataLookup.this.youtubeUrl = (String) value;
								case "website_url" -> NecesseMetadataLookup.this.websiteUrl = (String) value;
								case "wiki_url" -> NecesseMetadataLookup.this.wikiUrl = (String) value;
								default -> {
									Log.warn(LogCategory.GAME_PROVIDER, "Unknown field: " + name + " = " + value);
								}
							}
						}
						return super.visitField(access, name, descriptor, signature, value);
					}
				}, 0);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (this.name == null || appID == -1 || version == null || hotfix == -1) {
			throw new IllegalStateException("Failed to retrieve game metadata");
		}
	}

	private Object getField(Class<?> clazz, String name) throws IllegalAccessException, NoSuchFieldException {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);

		return field.get(null);
	}

	public String getName() {
		return name;
	}

	public int getAppID() {
		return appID;
	}

	public String getVersion() {
		return version;
	}

	public int getHotfix() {
		return hotfix;
	}

	public String getWikiUrl() {
		return wikiUrl;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public String getYoutubeUrl() {
		return youtubeUrl;
	}

	public String getRedditUrl() {
		return redditUrl;
	}

	public String getxUrl() {
		return xUrl;
	}

	public String getTwitterUrl() {
		return twitterUrl;
	}

	public String getSteamNewsUrl() {
		return steamNewsUrl;
	}

	public String getDiscordInviteUrl() {
		return discordInviteUrl;
	}
}
