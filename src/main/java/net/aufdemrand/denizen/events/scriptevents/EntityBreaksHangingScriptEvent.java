package net.aufdemrand.denizen.events.scriptevents;

import net.aufdemrand.denizen.objects.dCuboid;
import net.aufdemrand.denizen.objects.dEllipsoid;
import net.aufdemrand.denizen.objects.dEntity;
import net.aufdemrand.denizen.objects.dLocation;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

import java.util.HashMap;

public class EntityBreaksHangingScriptEvent extends ScriptEvent implements Listener {

    // <--[event]
    // @Events
    // entity breaks hanging (in <notable cuboid>)
    // entity breaks hanging because <cause> (in <notable cuboid>)
    // <entity> breaks hanging (in <notable cuboid>)
    // <entity> breaks hanging because <cause> (in <notable cuboid>)
    //
    // @Cancellable true
    //
    // @Triggers when a hanging entity (painting or itemframe) is broken.
    //
    // @Context
    // <context.cause> returns the cause of the entity breaking.
    // <context.entity> returns the dEntity that broke the hanging entity, if any.
    // <context.hanging> returns the dEntity of the hanging.
    // <context.cuboids> returns a dList of the cuboids the hanging is in.
    // <context.location> returns a dLocation of the hanging.
    // Causes list: <@link url http://bit.ly/1BeqxPX>
    //
    // -->

    public EntityBreaksHangingScriptEvent() {
        instance = this;
    }
    public static EntityBreaksHangingScriptEvent instance;
    public Element cause;
    public dEntity entity;
    public dEntity hanging;
    public dList cuboids;
    public dLocation location;
    public HangingBreakByEntityEvent event;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        String entName = CoreUtilities.getXthArg(0, lower);
        return lower.contains("breaks hanging")
                && (entName.equals("entity") || dEntity.matches(entName));
    }

    @Override
    public boolean matches(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        String entName = CoreUtilities.getXthArg(0, lower);
        if (!entity.matchesEntity(entName)){
            return false;
        }

        String notable = null;
        if (CoreUtilities.xthArgEquals(3, lower, "in")) {
            notable = CoreUtilities.getXthArg(4, lower);
        }
        else if (CoreUtilities.xthArgEquals(5, lower, "in")) {
            notable = CoreUtilities.getXthArg(6, lower);
        }
        if (notable != null) {
            if (dCuboid.matches(notable)) {
                dCuboid cuboid = dCuboid.valueOf(notable);
                if (!cuboid.isInsideCuboid(location)) {
                    return false;
                }
            }
            else if (dEllipsoid.matches(notable)) {
                dEllipsoid ellipsoid = dEllipsoid.valueOf(notable);
                if (!ellipsoid.contains(location)) {
                    return false;
                }
            }
            else {
                dB.echoError("Invalid event 'IN ...' check [" + getName() + "]: '" + s + "' for " + scriptContainer.getName());
                return false;
            }
        }

        if (CoreUtilities.xthArgEquals(3, lower, "because")){
            if (!CoreUtilities.getXthArg(4, lower).equals(CoreUtilities.toLowerCase(cause.asString()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "EntityBreaksHanging";
    }

    @Override
    public void init() {
        Bukkit.getServer().getPluginManager().registerEvents(this, DenizenAPI.getCurrentInstance());
    }

    @Override
    public void destroy() {
        HangingBreakByEntityEvent.getHandlerList().unregister(this);
    }

    @Override
    public boolean applyDetermination(ScriptContainer container, String determination) {
        return super.applyDetermination(container, determination);
    }

    @Override
    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = super.getContext();
        context.put("cause", cause);
        context.put("entity", entity);
        context.put("hanging", hanging);
        context.put("cuboids", cuboids);
        context.put("location", location);
        return context;
    }

    @EventHandler
    public void onHangingBreaks(HangingBreakByEntityEvent event) {
        hanging = new dEntity(event.getEntity());
        cause =  new Element(event.getCause().name());
        location = new dLocation(hanging.getLocation());
        entity = new dEntity(event.getRemover());
        cuboids = new dList();
        for (dCuboid cuboid: dCuboid.getNotableCuboidsContaining(location)) {
            cuboids.add(cuboid.identifySimple());
        }
        cancelled = event.isCancelled();
        this.event = event;
        fire();
        event.setCancelled(cancelled);
    }
}
