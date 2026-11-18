package com.coldfriend.interaction;

import com.coldfriend.player.PlayerId;

/**
 * A player asking to use something. The simulation decides whether it works; presentation code only sends it.
 *
 * @param player who is asking
 * @param target which thing they want to use
 */
public record InteractionRequest(PlayerId player, InteractableRef target) {
}
