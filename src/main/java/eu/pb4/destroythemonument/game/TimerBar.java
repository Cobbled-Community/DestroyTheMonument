package eu.pb4.destroythemonument.game;

import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public final class TimerBar {
    private final ServerBossEvent bar;

    public TimerBar(Collection<ServerPlayer> players, long ticksUntilEnd) {
        this.bar = new ServerBossEvent(this.getText(ticksUntilEnd), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
        for (ServerPlayer player : players) {
            this.bar.addPlayer(player);
        }
    }

    public void update(long ticksUntilEnd, long totalTicksUntilEnd) {
        if (ticksUntilEnd % 20 == 0) {
            this.bar.setName(this.getText(ticksUntilEnd));
            this.bar.setProgress((float) ticksUntilEnd / totalTicksUntilEnd);
        }
    }

    private Component getText(long ticksUntilEnd) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("%02d:%02d left", minutes, seconds);

        return Component.literal(time);
    }

    public void addPlayer(ServerPlayer player) {
        this.bar.addPlayer(player);
    }

    public void removePlayer(ServerPlayer player) {
        this.bar.removePlayer(player);
    }

    public void remove() {
        this.bar.removeAllPlayers();
    }
}
