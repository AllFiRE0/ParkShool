package ml.allfire.parkshool.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ParticleManager {

    private final boolean enabled;
    private final Particle particle;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double speed;
    private final double extra;
    private final Object data; // для некоторых частиц нужны данные (например DUST)

    public ParticleManager(ConfigurationSection section) {
        this.enabled = section.getBoolean("enabled", false);

        String typeStr = section.getString("type", "CLOUD").toUpperCase();
        Particle parsedParticle = Particle.CLOUD;
        try {
            parsedParticle = Particle.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            // Оставляем CLOUD по умолчанию
        }
        this.particle = parsedParticle;

        this.count = section.getInt("count", 8);
        this.offsetX = section.getDouble("offset_x", 0.3);
        this.offsetY = section.getDouble("offset_y", 0.1);
        this.offsetZ = section.getDouble("offset_z", 0.3);
        this.speed = section.getDouble("speed", 0.05);
        this.extra = section.getDouble("extra", 0);
        this.data = null; // для базовой версии без данных
    }

    /**
     * Создать частицы под игроком.
     */
    public void spawnAtPlayer(Player player) {
        if (!enabled) return;

        Location loc = player.getLocation().clone();
        // Частицы под ногами — опускаем на 1 блок
        loc.setY(loc.getY() - 0.2);

        spawnAtLocation(loc);
    }

    /**
     * Создать частицы в указанной локации.
     */
    public void spawnAtLocation(Location location) {
        if (!enabled) return;

        World world = location.getWorld();
        if (world == null) return;

        if (data != null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
        } else {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, extra);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
