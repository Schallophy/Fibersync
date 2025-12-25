package com.hadroncfy.fibersync.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.hadroncfy.fibersync.interfaces.IServerScoreboard;
import com.hadroncfy.fibersync.interfaces.Unit;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;

@Mixin(ServerScoreboard.class)
public abstract class MixinServerScoreboard extends Scoreboard implements IServerScoreboard {
    // From ServerScoreboard
    @Shadow @Final private Set<ScoreboardObjective> syncableObjectives;
    
    // From parent Scoreboard class - accessed via super
    // objectives: Object2ObjectMap<String, ScoreboardObjective>
    // objectivesByCriterion: Reference2ObjectMap<ScoreboardCriterion, List<ScoreboardObjective>>
    // scores: Map<String, Map<ScoreboardObjective, ScoreboardScore>>
    // objectiveSlots: Map<ScoreboardDisplaySlot, ScoreboardObjective>
    // teams: Object2ObjectMap<String, Team>
    // teamsByScoreHolder: Object2ObjectMap<String, Team>

    @Override
    public void reset(Unit u) {
        // Clear ServerScoreboard's syncable objectives
        this.syncableObjectives.clear();
        
        // Clear parent Scoreboard's data via getters and clear methods
        // Note: In 1.21.11, the parent Scoreboard class manages objectives, scores, teams etc.
        // We need to clear objectives which will cascade to other data
        this.getObjectives().clear();
    }
}
