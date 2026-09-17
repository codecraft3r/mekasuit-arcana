package dev.vvh.mekasuitarcana.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArcanaConfigMigratorTest {

    @Test
    void testVersionDetection() {
        CommentedConfig empty = TomlFormat.instance().createConfig();
        assertEquals(0, ArcanaConfigMigrator.readVersion(empty));
        assertTrue(ArcanaConfigMigrator.needsMigration(empty));

        empty.set("config_version", 1);
        assertEquals(1, ArcanaConfigMigrator.readVersion(empty));
        assertFalse(ArcanaConfigMigrator.needsMigration(empty));
    }

    @Test
    void testMigrateLegacyKeys() {
        CommentedConfig config = TomlFormat.instance().createConfig();
        config.set("balance.fe_per_mana", 10.0);
        config.set("balance.cooldown_acceleration.max_units", 4);
        config.set("balance.cooldown_acceleration.percent_per_unit", 0.5);
        config.set("disabled_modules.cooldown_acceleration", true);

        config.set("balance.cast_time.max_units", 3);
        config.set("balance.cast_time.percent_per_unit", 0.25);
        config.set("disabled_modules.cast_time", false);

        ArcanaConfigMigrator.migrateLegacyKeys(config);

        // Check remapped cooldown keys
        assertFalse(config.contains("balance.cooldown_acceleration"));
        assertTrue(config.contains("balance.cooldown_reduction"));
        assertEquals(4, (int) config.get("balance.cooldown_reduction.max_units"));
        assertEquals(0.5, (double) config.get("balance.cooldown_reduction.percent_per_unit"));
        assertEquals(true, config.get("disabled_modules.cooldown_reduction"));

        // Check remapped cast_time keys
        assertFalse(config.contains("balance.cast_time"));
        assertTrue(config.contains("balance.casting_stabilization"));
        assertEquals(3, (int) config.get("balance.casting_stabilization.max_units"));
        assertEquals(0.25, (double) config.get("balance.casting_stabilization.percent_per_unit"));
        assertEquals(false, config.get("disabled_modules.casting_stabilization"));

        // Ensure untouched values remain
        assertEquals(10.0, (double) config.get("balance.fe_per_mana"));
    }

    @Test
    void testMigrateWithBackupAndFile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("mekasuitarcana-server.toml");

        String legacyToml = "balance:\n  fe_per_mana: 15.0\n";
        String legacyTomlContent = "[balance]\nfe_per_mana = 15.0\n[balance.cooldown_acceleration]\nmax_units = 2\npercent_per_unit = 0.3\n[balance.cast_time]\nmax_units = 2\npercent_per_unit = 0.2\n[disabled_modules]\ncooldown_acceleration = true\n";

        Files.writeString(configFile, legacyTomlContent);

        CommentedConfig config = new TomlParser().parse(Files.readString(configFile));
        assertTrue(ArcanaConfigMigrator.needsMigration(config));

        boolean migrated = ArcanaConfigMigrator.migrate(config, configFile);
        assertTrue(migrated);

        // Check backup was created
        Path backupFile = tempDir.resolve("mekasuitarcana-server.toml.v0.bak");
        assertTrue(Files.exists(backupFile), "Backup file should exist");
        assertEquals(legacyTomlContent, Files.readString(backupFile));

        // Check migration in memory
        assertEquals(ArcanaConfigMigrator.CURRENT_VERSION, ArcanaConfigMigrator.readVersion(config));
        assertEquals(15.0, (double) config.get("balance.fe_per_mana"));
        assertEquals(2, (int) config.get("balance.cooldown_reduction.max_units"));
        assertEquals(0.3, (double) config.get("balance.cooldown_reduction.percent_per_unit"));
        assertEquals(2, (int) config.get("balance.casting_stabilization.max_units"));
        assertEquals(0.2, (double) config.get("balance.casting_stabilization.percent_per_unit"));
        assertEquals(true, config.get("disabled_modules.cooldown_reduction"));

        // Second run should be no-op
        assertFalse(ArcanaConfigMigrator.needsMigration(config));
        boolean secondRun = ArcanaConfigMigrator.migrate(config, configFile);
        assertFalse(secondRun);
    }
}