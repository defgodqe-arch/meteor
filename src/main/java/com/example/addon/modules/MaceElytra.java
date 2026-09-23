package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/**
 * After attacking a living entity with a mace, equips an Elytra and switches
 * to the first Firework Rocket found in the hotbar.
 */
public class MaceElytra extends Module {
    private int ticksUntilElytra;
    private int ticksUntilFirework;
    private boolean pending;

    public MaceElytra() {
        super(AddonTemplate.CATEGORY, "mace-elytra", "Equips an Elytra after a mace attack and switches to a Firework Rocket.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || !(event.entity instanceof LivingEntity)) return;
        if (!mc.player.getMainHandItem().is(Items.MACE)) return;

        // The attack event is fired before the actual game-mode attack finishes.
        // Delay the inventory change by one client tick so the mace attack is
        // sent first.
        pending = true;
        ticksUntilElytra = 1;
        ticksUntilFirework = 2;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!pending || mc.player == null) return;

        if (ticksUntilElytra > 0) {
            ticksUntilElytra--;
            if (ticksUntilElytra == 0) equipElytra();
        }

        if (ticksUntilFirework > 0) {
            ticksUntilFirework--;
            if (ticksUntilFirework == 0) selectFirework();
        }

        if (ticksUntilElytra == 0 && ticksUntilFirework == 0) pending = false;
    }

    private void equipElytra() {
        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;

        FindItemResult elytra = InvUtils.find(stack -> stack.is(Items.ELYTRA));
        if (!elytra.found() || elytra.isArmor()) return;

        InvUtils.quickSwap()
            .from(elytra.slot())
            .toArmor(EquipmentSlot.CHEST.getIndex());
    }

    private void selectFirework() {
        // Search only inventory indices 0-8 so an offhand firework is never
        // selected when the goal is specifically to switch to a hotbar slot.
        FindItemResult firework = InvUtils.find(stack -> stack.is(Items.FIREWORK_ROCKET), 0, 8);
        if (!firework.found() || !firework.isHotbar()) return;

        InvUtils.swap(firework.slot(), false);
    }

    private void reset() {
        ticksUntilElytra = 0;
        ticksUntilFirework = 0;
        pending = false;
    }
}
