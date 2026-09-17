package dev.vvh.mekasuitarcana.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles detection, backup, and value migration for outdated or unversioned server configuration files.
 *
 * <p>Loader-free design so migration behavior and key remapping can be tested in standard JUnit runs.</p>
 */
public final class ArcanaConfigMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArcanaConfigMigrator.class);

    public static final int CURRENT_VERSION = 1;

    private ArcanaConfigMigrator() {}

    /**
     * Reads the integer version from the config. Returns 0 if missing, unparseable, or absent.
     */
    public static int readVersion(CommentedConfig config) {
        if (config == null || !config.contains("config_version")) {
            return 0;
        }
        Object val = config.get("config_version");
        if (val instanceof Number num) {
            return num.intValue();
        }
        return 0;
    }

    /**
     * Returns true if the config version is older than CURRENT_VERSION.
     */
    public static boolean needsMigration(CommentedConfig config) {
        return readVersion(config) < CURRENT_VERSION;
    }

    /**
     * Migrates the config in place, creating a backup of the disk file if present.
     *
     * @param config the active CommentedConfig
     * @param diskPath path to the config file on disk (may be null in test environments)
     * @return true if migration was performed, false if already up-to-date
     */
    public static boolean migrate(CommentedConfig config, Path diskPath) {
        if (!needsMigration(config)) {
            return false;
        }

        int oldVersion = readVersion(config);
        LOGGER.info("Detected outdated or unversioned config (version {}). Migrating to version {}...",
                oldVersion, CURRENT_VERSION);

        if (diskPath != null && Files.exists(diskPath)) {
            backupConfig(diskPath, oldVersion);
        }

        // Copy over values from prior versions that still matter, including legacy key renames
        migrateLegacyKeys(config);

        // Update version to current
        config.set("config_version", CURRENT_VERSION);

        return true;
    }

    public static void backupConfig(Path diskPath, int oldVersion) {
        try {
            String fileName = diskPath.getFileName().toString();
            Path backupPath = diskPath.resolveSibling(fileName + ".v" + oldVersion + ".bak");
            if (Files.exists(backupPath)) {
                backupPath = diskPath.resolveSibling(fileName + ".bak");
            }
            Files.copy(diskPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Created backup of old config at: {}", backupPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to create backup of old config at {}: {}", diskPath, e.getMessage());
        }
    }

    public static void migrateLegacyKeys(CommentedConfig config) {
        // Remap legacy 'cooldown_acceleration' -> 'cooldown_reduction'
        remapSubConfig(config, "balance.cooldown_acceleration", "balance.cooldown_reduction");
        remapValue(config, "disabled_modules.cooldown_acceleration", "disabled_modules.cooldown_reduction");

        // Remap legacy 'cast_time' -> 'casting_stabilization'
        remapSubConfig(config, "balance.cast_time", "balance.casting_stabilization");
        remapValue(config, "disabled_modules.cast_time", "disabled_modules.casting_stabilization");
    }

    private static void remapSubConfig(CommentedConfig config, String oldPath, String newPath) {
        if (config.contains(oldPath)) {
            Object oldObj = config.get(oldPath);
            if (oldObj instanceof Config oldSubConfig) {
                Object existing = config.get(newPath);
                Config newSubConfig;
                if (existing instanceof Config c) {
                    newSubConfig = c;
                } else {
                    newSubConfig = Config.inMemory();
                    config.set(newPath, newSubConfig);
                }
                for (Config.Entry entry : oldSubConfig.entrySet()) {
                    if (!newSubConfig.contains(entry.getKey())) {
                        newSubConfig.set(entry.getKey(), entry.getValue());
                    }
                }
            }
            config.remove(oldPath);
        }
    }

    private static void remapValue(CommentedConfig config, String oldPath, String newPath) {
        if (config.contains(oldPath)) {
            if (!config.contains(newPath)) {
                config.set(newPath, config.get(oldPath));
            }
            config.remove(oldPath);
        }
    }
}