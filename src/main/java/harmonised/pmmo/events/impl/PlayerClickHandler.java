package harmonised.pmmo.events.impl;

import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.api.enums.EventType;
import harmonised.pmmo.api.enums.ReqType;
import harmonised.pmmo.config.Config;
import harmonised.pmmo.core.Core;
import harmonised.pmmo.features.party.PartyUtils;
import harmonised.pmmo.util.Messenger;
import harmonised.pmmo.util.RegistryUtil;
import harmonised.pmmo.util.TagUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Map;

public class PlayerClickHandler {

	@SuppressWarnings("resource")
	public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event ) {
		Player player = event.getEntity();
		Core core = Core.get(player.level());
		boolean serverSide = !player.level().isClientSide;
		
		if (!core.isActionPermitted(ReqType.BREAK, event.getPos(), player)) {
			event.setUseBlock(TriState.FALSE);
			Messenger.sendDenialMsg(ReqType.BREAK, player, new ItemStack(player.level().getBlockState(event.getPos()).getBlock().asItem()).getDisplayName());

		}
		if (!core.isActionPermitted(ReqType.INTERACT, event.getItemStack(), player)) {
			event.setUseItem(TriState.FALSE);
			Messenger.sendDenialMsg(ReqType.INTERACT, player, player.getMainHandItem().getDisplayName());
		}
		if (event.getUseBlock().equals(TriState.FALSE)) return;
		
		CompoundTag hookOutput = new CompoundTag();
		if (serverSide) {
			hookOutput = core.getEventTriggerRegistry().executeEventListeners(EventType.HIT_BLOCK, event, hookOutput);
			if (hookOutput.getBoolean(APIUtils.IS_CANCELLED)) {
				event.setCanceled(true);
				return;
			}
			if (hookOutput.getBoolean(APIUtils.DENY_BLOCK_USE))
				event.setUseBlock(TriState.FALSE);
			if (hookOutput.getBoolean(APIUtils.DENY_ITEM_USE))
				event.setUseItem(TriState.FALSE);
		}
		
		hookOutput = TagUtils.mergeTags(hookOutput, core.getPerkRegistry().executePerk(EventType.HIT_BLOCK, player, new CompoundTag()));
		if (serverSide) {
			Map<String, Long> xpAward = core.getExperienceAwards(EventType.HIT_BLOCK, event.getPos(), player.level(), event.getEntity(), hookOutput);
			List<ServerPlayer> partyMembersInRange = PartyUtils.getPartyMembersInRange((ServerPlayer) event.getEntity());
			core.awardXP(partyMembersInRange, xpAward);	
		}
	}
	
	@SuppressWarnings("resource")
	public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Core core = Core.get(player.level());
		boolean serverSide = !player.level().isClientSide;
		
		if (!core.isActionPermitted(ReqType.INTERACT, event.getPos(), player)) {
			event.setUseBlock(TriState.FALSE);
		}
		if (!core.isActionPermitted(ReqType.INTERACT, event.getItemStack(), player)) {
			event.setUseItem(TriState.FALSE);
		}
		if (event.getUseBlock().equals(TriState.FALSE) && !serverSide && event.getHand().equals(InteractionHand.MAIN_HAND)) {
			Messenger.sendDenialMsg(ReqType.INTERACT, player, event.getLevel().getBlockState(event.getPos()).getBlock().getName());
			return;
		}
		
		CompoundTag hookOutput = new CompoundTag();
		boolean isSalvage = player.isCrouching()
				&& RegistryUtil.getId(event.getLevel().getBlockState(event.getPos()).getBlock()).equals(Config.server().general().salvageBlock());
		if (serverSide) {
			hookOutput = core.getEventTriggerRegistry().executeEventListeners(EventType.ACTIVATE_BLOCK, event, hookOutput);
			if (hookOutput.getBoolean(APIUtils.IS_CANCELLED)) {
				event.setCanceled(true);
				return;
			}
			//======================SALVAGE DROP LOGIC=======================================
			if (isSalvage && event.getHand().equals(InteractionHand.MAIN_HAND))
				core.getSalvage((ServerPlayer) player);
			//=======================END SALVAGE============================================
		}
		if (isSalvage)
			event.setCanceled(true);
		//prevent salvage blocks from opening their gui
		
		hookOutput = TagUtils.mergeTags(hookOutput, core.getPerkRegistry().executePerk(EventType.ACTIVATE_BLOCK, player, new CompoundTag()));
		if (serverSide) {
			Map<String, Long> xpAward = core.getExperienceAwards(EventType.ACTIVATE_BLOCK, event.getPos(), player.level(), event.getEntity(), hookOutput);
			List<ServerPlayer> partyMembersInRange = PartyUtils.getPartyMembersInRange((ServerPlayer) event.getEntity());
			core.awardXP(partyMembersInRange, xpAward);	
		}
	}
	
	@SuppressWarnings("resource")
	public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		Core core = Core.get(player.level());
		boolean serverSide = !player.level().isClientSide;
		
		if (!core.isActionPermitted(ReqType.USE, event.getItemStack(), player)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			Messenger.sendDenialMsg(ReqType.USE, player, event.getItemStack().getDisplayName());
			return;
		}
		CompoundTag hookOutput = new CompoundTag();
		if (serverSide) {
			hookOutput = core.getEventTriggerRegistry().executeEventListeners(EventType.ACTIVATE_ITEM, event, hookOutput);
			if (hookOutput.getBoolean(APIUtils.IS_CANCELLED)) {
				event.setCanceled(true);
				return;
			}
		}
		
		hookOutput = TagUtils.mergeTags(hookOutput, core.getPerkRegistry().executePerk(EventType.ACTIVATE_ITEM, player, new CompoundTag()));
		if (serverSide) {
			Map<String, Long> xpAward = core.getExperienceAwards(EventType.ACTIVATE_ITEM, event.getItemStack(), event.getEntity(), hookOutput);
			List<ServerPlayer> partyMembersInRange = PartyUtils.getPartyMembersInRange((ServerPlayer) event.getEntity());
			core.awardXP(partyMembersInRange, xpAward);	
		}
	}
}
